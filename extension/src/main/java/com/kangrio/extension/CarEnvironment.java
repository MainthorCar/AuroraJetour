/*
 * Aurora Store Jetour — MainthorCar
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.extension;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Jetour T1: hace que cada app parcheada vea una tablet y no un auto.
 *
 * 1. Oculta las funciones de auto (android.hardware.type.automotive, radio del auto)
 *    -> se usa desde PackageManagerProxy (hasSystemFeature / getSystemAvailableFeatures).
 * 2. UiModeManager.getCurrentModeType() devuelve "normal" en vez de "auto".
 * 3. Configuration.uiMode: si el sistema dice "auto", se corrige a "normal".
 *    APAGADO de fábrica desde la 1.1.2 (ver partOff / debug.jetour.carenv).
 *
 * Si la pantalla no está en modo auto, los puntos 2 y 3 no cambian nada.
 */
@SuppressLint({"SoonBlockedPrivateApi", "BlockedPrivateApi", "DiscouragedPrivateApi", "PrivateApi"})
public final class CarEnvironment {
    private static final String TAG = "CarEnvironment";

    private static final Set<String> HIDDEN_FEATURES = new HashSet<>(Arrays.asList(
            "android.hardware.type.automotive",
            "android.hardware.broadcastradio"
    ));

    /**
     * Apps a las que NO se les aplica el "modo tablet" (ni ocultar el auto, ni cambiar uiMode).
     * Spotify: dentro de las pantallas escondidas del cluster se reinicia sin parar (parpadea).
     */
    private static final Set<String> EXCLUDED_PACKAGES = new HashSet<>(Arrays.asList(
            "com.spotify.music"
    ));

    private static volatile boolean enabled = true;

    private CarEnvironment() {
    }

    /** Se llama apenas se conoce el paquete de la app (antes de crear la Application). */
    public static void setPackage(@Nullable String packageName) {
        if (packageName != null && EXCLUDED_PACKAGES.contains(packageName)) {
            enabled = false;
            Log.i(TAG, "modo tablet desactivado para " + packageName);
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Llaves de prueba, para desactivar partes del modo tablet sin compilar de nuevo.
     * Desde el T1 Puente:  setprop debug.jetour.carenv uimode,features   (y reabrir la app)
     *   config_on = SÍ cambiar Configuration.uiMode (auto -> normal), como hasta la 1.1.1.
     *              Desde la 1.1.2 viene apagado: hacía parpadear apps en el cluster.
     *   uimode   = no tocar UiModeManager.getCurrentModeType()
     *   features = no ocultar android.hardware.type.automotive
     *   all      = nada de lo anterior
     * Vacío (lo normal): todo activo, menos config.
     */
    private static Set<String> disabledParts;

    private static boolean partOff(String part) {
        if (disabledParts == null) {
            Set<String> parts = new HashSet<>();
            try {
                Class<?> sp = Class.forName("android.os.SystemProperties");
                String value = (String) sp.getMethod("get", String.class, String.class)
                        .invoke(null, "debug.jetour.carenv", "");
                if (value != null) {
                    for (String s : value.split(",")) {
                        if (!s.trim().isEmpty()) {
                            parts.add(s.trim());
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            disabledParts = parts;
            Log.i(TAG, "llaves de prueba, apagado: " + (parts.isEmpty() ? "nada" : parts.toString()));
        }
        if (disabledParts.contains("all")) {
            return true;
        }
        if ("config".equals(part)) {
            // Desde la 1.1.2 viene APAGADO de fábrica: cambiar uiMode hacía parpadear
            // a Waze (resumen de ruta) y a Spotify en el cluster. Se prende con config_on.
            return !disabledParts.contains("config_on");
        }
        return disabledParts.contains(part);
    }

    public static boolean isHiddenFeature(@Nullable String name) {
        return enabled && !partOff("features") && name != null && HIDDEN_FEATURES.contains(name);
    }

    // ---------------------------------------------------------------------
    // UiModeManager
    // ---------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static void initUiModeProxy() {
        if (!enabled || partOff("uimode")) {
            return;
        }
        try {
            IBinder original = ServiceManager.getService("uimode");
            if (original == null) {
                return;
            }
            ArrayMap<String, IBinder> sCache = (ArrayMap<String, IBinder>)
                    Utils.getFieldValue(ServiceManager.class, "sCache", null);
            if (sCache != null) {
                sCache.put("uimode", new UiModeManagerProxy(original));
                Log.d(TAG, "uimode proxy ok");
            }
        } catch (Throwable e) {
            Log.e(TAG, "uimode proxy fail: ", e);
        }
    }

    private static final class UiModeManagerProxy extends Binder {
        private static final String DESCRIPTOR = "android.app.IUiModeManager";
        private final IBinder original;
        private final int getCurrentModeTypeCode;

        UiModeManagerProxy(IBinder original) {
            this.original = original;
            int code = -1;
            try {
                Class<?> stub = Class.forName(DESCRIPTOR + "$Stub");
                java.lang.reflect.Field f = stub.getDeclaredField("TRANSACTION_getCurrentModeType");
                f.setAccessible(true);
                code = f.getInt(null);
            } catch (Throwable ignored) {
            }
            this.getCurrentModeTypeCode = code;
        }

        @Override
        public String getInterfaceDescriptor() {
            return DESCRIPTOR;
        }

        @Override
        protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
            if (code == getCurrentModeTypeCode && reply != null) {
                int realMode = Configuration.UI_MODE_TYPE_NORMAL;
                Parcel realReply = Parcel.obtain();
                try {
                    if (original.transact(code, data, realReply, flags)) {
                        realReply.setDataPosition(0);
                        realReply.readException();
                        realMode = realReply.readInt();
                    }
                } catch (Throwable ignored) {
                } finally {
                    realReply.recycle();
                }

                reply.writeNoException();
                reply.writeInt(realMode == Configuration.UI_MODE_TYPE_CAR
                        ? Configuration.UI_MODE_TYPE_NORMAL
                        : realMode);
                reply.setDataPosition(0);
                return true;
            }
            return original.transact(code, data, reply, flags);
        }
    }

    // ---------------------------------------------------------------------
    // Configuration.uiMode
    // ---------------------------------------------------------------------

    public static void fixConfiguration(@Nullable Configuration config) {
        if (config == null) {
            return;
        }
        if ((config.uiMode & Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_CAR) {
            config.uiMode = (config.uiMode & ~Configuration.UI_MODE_TYPE_MASK)
                    | Configuration.UI_MODE_TYPE_NORMAL;
        }
    }

    private static void fix(@Nullable android.content.Context context) {
        try {
            if (context != null) {
                fixConfiguration(context.getResources().getConfiguration());
            }
        } catch (Throwable ignored) {
        }
    }

    public static void attach(@NonNull final Application application) {
        if (!enabled || partOff("config")) {
            return;
        }
        try {
            fix(application);
            application.registerComponentCallbacks(new ComponentCallbacks() {
                @Override
                public void onConfigurationChanged(@NonNull Configuration newConfig) {
                    fixConfiguration(newConfig);
                    fix(application);
                }

                @Override
                public void onLowMemory() {
                }
            });
            application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityPreCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                    fix(application);
                    fix(activity);
                }

                @Override
                public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                    fix(activity);
                }

                @Override
                public void onActivityStarted(@NonNull Activity activity) {
                }

                @Override
                public void onActivityResumed(@NonNull Activity activity) {
                    fix(activity);
                }

                @Override
                public void onActivityPaused(@NonNull Activity activity) {
                }

                @Override
                public void onActivityStopped(@NonNull Activity activity) {
                }

                @Override
                public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
                }

                @Override
                public void onActivityDestroyed(@NonNull Activity activity) {
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "attach fail: ", e);
        }
    }
}
