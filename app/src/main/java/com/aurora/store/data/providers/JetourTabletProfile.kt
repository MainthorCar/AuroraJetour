/*
 * Perfil de dispositivo "Jetour T1 Tablet" — Aurora Store Jetour (MainthorCar)
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Hace que Google Play vea la pantalla de la Jetour T1 (Snapdragon 8155, Android 11)
 * como una Samsung Galaxy Tab S7+ Wi-Fi (SM-T970, Android 11), sin nada de "auto".
 *
 *  - Identidad: Galaxy Tab S7+ real, con su firma (fingerprint) certificada por Google.
 *  - Hardware: se lee en vivo de la pantalla (procesador, OpenGL, idiomas, versión de
 *    servicios), así coincide siempre con lo que la pantalla puede ejecutar.
 *  - Pantalla: 1920x1080 horizontal, densidad real, categoría XLARGE (tablet).
 *  - Funciones: las reales MENOS las que delatan un auto.
 */

package com.aurora.store.data.providers

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import java.util.Properties

object JetourTabletProfile {

    const val CONFIG_NAME = "jetour_t1_tablet.properties"
    const val USER_READABLE_NAME = "Jetour T1 · Galaxy Tab S7+ (Tablet)"

    // Identidad: Samsung Galaxy Tab S7+ Wi-Fi SM-T970, Android 11 (firma certificada)
    private const val BRAND = "samsung"
    private const val MANUFACTURER = "samsung"
    private const val MODEL = "SM-T970"
    private const val DEVICE = "gts7xlwifi"
    private const val PRODUCT = "gts7xlwifixx"
    private const val HARDWARE = "qcom"
    private const val BUILD_ID = "RP1A.200720.012"
    private const val BOOTLOADER = "T970XXU1BUAA"
    private const val FINGERPRINT =
        "samsung/gts7xlwifixx/gts7xlwifi:11/RP1A.200720.012/T970XXU1BUAA:user/release-keys"
    private const val SDK_INT = "30"
    private const val RELEASE = "11"

    // Pantalla principal de 15.6" (la del tablero de 10" no se toca)
    private const val SCREEN_WIDTH = "1920"
    private const val SCREEN_HEIGHT = "1080"

    /** Funciones que delatan un auto: se le ocultan a Google Play. */
    val CAR_FEATURES = setOf(
        "android.hardware.type.automotive",
        "android.hardware.broadcastradio"
    )

    /** Librerías propias del sistema del auto (Autolink). */
    private val CAR_LIBRARIES = setOf(
        "autolink.shared.sdk"
    )

    /**
     * true si Aurora está corriendo en la pantalla de la Jetour (Chery "cheryidc")
     * o en cualquier pantalla Android que se declare como auto.
     */
    fun isSupported(context: Context): Boolean {
        val isCheryIdc = Build.DEVICE.equals("cheryidc", true) ||
            Build.PRODUCT.equals("cheryidc", true)
        val isAutomotive = context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
        return isCheryIdc || isAutomotive
    }

    fun build(context: Context): Properties {
        // Base: datos reales de la pantalla (GL, ABI, idiomas, versión de GSF/Vending, etc.)
        val properties = NativeDeviceInfoProvider.getNativeDeviceProperties(context)

        properties.apply {
            // Identidad de tablet
            setProperty("UserReadableName", USER_READABLE_NAME)
            setProperty("Build.HARDWARE", HARDWARE)
            setProperty("Build.RADIO", "unknown") // tablet Wi-Fi, sin módem
            setProperty("Build.FINGERPRINT", FINGERPRINT)
            setProperty("Build.BRAND", BRAND)
            setProperty("Build.DEVICE", DEVICE)
            setProperty("Build.VERSION.SDK_INT", SDK_INT)
            setProperty("Build.VERSION.RELEASE", RELEASE)
            setProperty("Build.MODEL", MODEL)
            setProperty("Build.MANUFACTURER", MANUFACTURER)
            setProperty("Build.PRODUCT", PRODUCT)
            setProperty("Build.ID", BUILD_ID)
            setProperty("Build.BOOTLOADER", BOOTLOADER)

            // Pantalla de tablet grande, siempre horizontal
            setProperty("TouchScreen", "${Configuration.TOUCHSCREEN_FINGER}")
            setProperty("Keyboard", "${Configuration.KEYBOARD_NOKEYS}")
            setProperty("Navigation", "${Configuration.NAVIGATION_NONAV}")
            setProperty("ScreenLayout", "${Configuration.SCREENLAYOUT_SIZE_XLARGE}")
            setProperty("HasHardKeyboard", "false")
            setProperty("HasFiveWayNavigation", "false")
            setProperty("Screen.Width", SCREEN_WIDTH)
            setProperty("Screen.Height", SCREEN_HEIGHT)
            // Screen.Density queda la real (213): así Play manda los gráficos correctos

            // Funciones reales menos las de auto
            setProperty(
                "Features",
                getProperty("Features", "")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it !in CAR_FEATURES }
                    .distinct()
                    .joinToString(",")
            )

            // Librerías reales menos las del auto
            setProperty(
                "SharedLibraries",
                getProperty("SharedLibraries", "")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it !in CAR_LIBRARIES }
                    .distinct()
                    .joinToString(",")
            )

            // Varios
            setProperty("TimeZone", "America/Argentina/Buenos_Aires")
            setProperty("Roaming", "mobile-notroaming")

            setProperty("CONFIG_NAME", CONFIG_NAME)
        }

        return properties
    }
}
