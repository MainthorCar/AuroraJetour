/*
 * Aurora Store Jetour — MainthorCar
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kangrio.extension;

import android.annotation.SuppressLint;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Pantallas sin motor de vibración (como la de la Jetour T1) no tienen el servicio "vibrator".
 * En Android 11, SystemVibrator no lo controla: llamadas como
 * Vibrator.areAllPrimitivesSupported() terminan en NullPointerException y la app se cierra
 * (por ejemplo, Google al tocar el cuadro de búsqueda).
 *
 * Si el equipo NO tiene servicio "vibrator", se registra uno falso que responde
 * "no hay vibración" a todo. Si el equipo tiene vibrador real, no se toca nada.
 */
@SuppressLint({"SoonBlockedPrivateApi", "BlockedPrivateApi", "DiscouragedPrivateApi", "PrivateApi"})
public final class NoVibratorService extends Binder {
    private static final String TAG = "NoVibratorService";
    private static final String SERVICE_NAME = "vibrator";
    private static final String DESCRIPTOR = "android.os.IVibratorService";

    // Vibrator.VIBRATION_EFFECT_SUPPORT_NO
    private static final int EFFECT_SUPPORT_NO = 2;

    private final Map<Integer, String> transactionNames = new HashMap<>();
    private final Map<String, Class<?>> returnTypes = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static void init() {
        try {
            IBinder real = ServiceManager.getService(SERVICE_NAME);
            if (real != null) {
                return; // hay vibrador real: no hacer nada
            }
            ArrayMap<String, IBinder> sCache = (ArrayMap<String, IBinder>)
                    Utils.getFieldValue(ServiceManager.class, "sCache", null);
            if (sCache != null) {
                sCache.put(SERVICE_NAME, new NoVibratorService());
                Log.d(TAG, "fake vibrator service installed");
            }
        } catch (Throwable e) {
            Log.e(TAG, "init fail: ", e);
        }
    }

    private NoVibratorService() {
        try {
            Class<?> stub = Class.forName(DESCRIPTOR + "$Stub");
            for (Field field : stub.getDeclaredFields()) {
                if (!field.getName().startsWith("TRANSACTION_") || field.getType() != int.class) {
                    continue;
                }
                field.setAccessible(true);
                transactionNames.put(field.getInt(null), field.getName().substring("TRANSACTION_".length()));
            }
            Class<?> iface = Class.forName(DESCRIPTOR);
            for (Method method : iface.getDeclaredMethods()) {
                returnTypes.put(method.getName(), method.getReturnType());
            }
        } catch (Throwable e) {
            Log.w(TAG, "could not read IVibratorService", e);
        }
    }

    @Override
    public String getInterfaceDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
        if (code == INTERFACE_TRANSACTION) {
            if (reply != null) {
                reply.writeString(DESCRIPTOR);
            }
            return true;
        }
        if (reply == null) {
            return true; // llamada sin respuesta (oneway): ignorar
        }

        String name = transactionNames.get(code);
        Class<?> returnType = name != null ? returnTypes.get(name) : null;

        try {
            if ("arePrimitivesSupported".equals(name)) {
                int[] ids = readIntArray(data);
                reply.writeNoException();
                reply.writeBooleanArray(new boolean[ids != null ? ids.length : 0]);
            } else if ("areEffectsSupported".equals(name)) {
                int[] ids = readIntArray(data);
                int[] result = new int[ids != null ? ids.length : 0];
                java.util.Arrays.fill(result, EFFECT_SUPPORT_NO);
                reply.writeNoException();
                reply.writeIntArray(result);
            } else {
                reply.writeNoException();
                writeDefault(reply, returnType);
            }
        } catch (Throwable e) {
            Log.w(TAG, "transaction " + name + " failed", e);
            reply.setDataPosition(0);
            reply.writeNoException();
            writeDefault(reply, returnType);
        }
        return true;
    }

    private static int[] readIntArray(Parcel data) {
        try {
            data.enforceInterface(DESCRIPTOR);
            return data.createIntArray();
        } catch (Throwable e) {
            return new int[0];
        }
    }

    private static void writeDefault(Parcel reply, @Nullable Class<?> type) {
        if (type == null || type == void.class) {
            return;
        }
        if (type == boolean.class || type == int.class || type == byte.class
                || type == char.class || type == short.class) {
            reply.writeInt(0);
        } else if (type == long.class) {
            reply.writeLong(0L);
        } else if (type == float.class) {
            reply.writeFloat(0f);
        } else if (type == double.class) {
            reply.writeDouble(0d);
        } else if (type == boolean[].class) {
            reply.writeBooleanArray(new boolean[0]);
        } else if (type == int[].class) {
            reply.writeIntArray(new int[0]);
        } else {
            // objetos (Parcelable, String, etc.): "null"
            reply.writeInt(0);
        }
    }
}
