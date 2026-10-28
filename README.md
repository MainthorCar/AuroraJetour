# Aurora Store Jetour

Versión comunitaria y **no oficial** de [Aurora Store](https://github.com/whyorean/AuroraStore), adaptada a la pantalla de la **Jetour T1** (Snapdragon 8155, Android 11).

Está basada en [AuroraStore-BYD](https://github.com/kangrio/AuroraStore-BYD) de **kangrio**, que hizo posible usar microG y las apps de Google en pantallas de auto sin root.

> [!NOTE]
> Este proyecto no está relacionado con Aurora OSS, Jetour, Chery ni BYD. "Jetour" se usa solo para indicar con qué pantalla es compatible.

---

## ✨ Qué agrega esta versión

* 📱 **Se presenta como tablet ante Google Play.** Perfil fijo "Jetour T1 · Galaxy Tab S7+": identidad de una tablet real (Samsung SM-T970, Android 11), con el hardware real de la pantalla (procesador, OpenGL, idiomas), pantalla 1920×1080 horizontal y sin las funciones de auto.
* 🚗➡️📱 **Las apps instaladas ven una tablet, no un auto.** Dentro de cada app parcheada se oculta `android.hardware.type.automotive` y el modo de interfaz "auto". Así, por ejemplo, Waze deja de pedir Android Auto.
* 🔄 **Actualizaciones desde este repositorio.** La app avisa cuando hay una versión nueva y se actualiza con un toque.
* 🔇 **Sin reporte de errores a servidores externos.**

Todo lo demás (microG, parcheo de apps, instaladores) viene de AuroraStore-BYD.

## 🚀 Instalación

1. Desinstalá cualquier otra Aurora Store y microG que tengas.
2. Bajá `AuroraStoreJetour.apk` del último [Release](https://github.com/MainthorCar/AuroraJetour/releases/latest) e instalalo.
3. Abrí la app, completá el asistente e iniciá sesión.
4. Instalá tu primera app. Cuando te lo pida, **instalá el paquete microG**.
5. Activá el **inicio automático** de *MicroG Service* y *MicroG Companion* en los ajustes de la pantalla.

## ⚠️ Limitaciones conocidas

* La pantalla de la Jetour T1 **no tiene Widevine**: Netflix, Disney+, Prime Video y similares se instalan pero no reproducen contenido protegido. YouTube, Spotify, Twitch, Kodi y VLC funcionan normal.
* El "Ok Google" con la pantalla en reposo no funciona con microG.
* Algunas apps con controles de integridad fuertes pueden no funcionar.

## 🛠 Compilar

* Android Studio → **Build Variants** → `app`: **jetourDebug** (pruebas, firma de prueba) o **jetourRelease** (requiere `app/signing.properties`).
* Publicar una versión: pestaña **Actions** → **Publicar Aurora Store Jetour** → **Run workflow**. Necesita los secretos `KEYSTORE_BASE64`, `KEY_ALIAS` y `KEY_PASSWORD`.
* Antes de publicar, subí `jetourVersionCode` en `app/build.gradle.kts`.

## 📄 Licencia y modificaciones

Este programa es software libre bajo la [GNU GPL v3 o posterior](LICENSE), igual que el proyecto original.

Desde octubre de 2026, MainthorCar modificó la versión 1.0.3 de AuroraStore-BYD así:
- se agregó el perfil Jetour T1 Tablet y la ocultación del entorno de auto;
- se cambiaron el nombre, el paquete y el ícono;
- las actualizaciones apuntan a este repositorio;
- se desactivó el reporte de errores.

El detalle está en el historial de commits.

## 🙏 Créditos

* [Aurora Store](https://github.com/whyorean/AuroraStore): **Rahul Kumar Patel (whyorean)**, **AuroraOSS** y The Calyx Institute. Es el cliente libre de Google Play en el que se basa todo.
* [AuroraStore-BYD](https://github.com/kangrio/AuroraStore-BYD): **kangrio**. Adaptó Aurora a pantallas de auto con microG y parcheo de apps sin root. Si te sirve este proyecto, apoyá también el suyo: [ko-fi.com/kangrio](https://ko-fi.com/kangrio).
* [microG / GmsCore](https://github.com/microg/GmsCore): implementación libre de los servicios de Google.
* [ARSCLib](https://github.com/REAndroid/ARSCLib): lectura y escritura de recursos binarios de Android.
* [AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) de **LSPosed**.

El aviso legal original de Aurora Store está en [DISCLAIMER.md](DISCLAIMER.md).
