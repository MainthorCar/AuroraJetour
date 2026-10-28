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

    private CarEnvironment() {
    }

    public static boolean isHiddenFeature(@Nullable String name) {
        return name != null && HIDDEN_FEATURES.contains(name);
    }

    // ---------------------------------------------------------------------
    // UiModeManager
    // ---------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static void initUiModeProxy() {
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
