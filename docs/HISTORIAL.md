# Aurora Store Jetour — Historial del proyecto

Registro de todo lo que se hizo, por qué, y cómo seguir. Última actualización: 30/10/2026.

---

## 1. Qué es

**Aurora Store Jetour** es una versión de Aurora Store para la pantalla de la **Jetour T1**. Está basada en [AuroraStore-BYD](https://github.com/kangrio/AuroraStore-BYD) de kangrio, versión 1.0.3.

- **Repo:** https://github.com/MainthorCar/AuroraJetour (es un fork, con los créditos visibles).
- **Paquete:** `com.mainthorcar.aurorajetour`
- **Link de instalación** (siempre baja la última versión):
  `https://github.com/MainthorCar/AuroraJetour/releases/latest/download/AuroraStoreJetour.apk`
- **Licencia:** GPL-3.0. El código tiene que ser público y hay que mantener los créditos (Aurora OSS / whyorean, kangrio, microG, ARSCLib, LSPosed).

## 2. La pantalla del auto (datos reales)

| Dato | Valor |
|---|---|
| Procesador | Qualcomm Snapdragon 8155 (`msmnile`) |
| Android | 11 (API 30), build `qti/cheryidc/cheryidc:11/RQ3A.211001.001/80:user/test-keys` |
| Arquitectura | arm64-v8a (+ armeabi-v7a, armeabi) |
| Pantalla principal | 15.6", 1920×1080, densidad 213, horizontal |
| Tablero del conductor | 10", 1920×720 (otra pantalla, no se toca) |
| Barra del aire acondicionado | ~149 px de alto, abajo |
| Declara | `android.hardware.type.automotive` (por eso las apps creen que es un auto) |
| Vibrador | **No tiene** (no existe el servicio `vibrator`) |
| Widevine (DRM) | **No tiene**, solo ClearKey. Netflix, Disney+, etc. no reproducen contenido pago, y no hay arreglo legal por software. |
| WebView de fábrica | `com.android.webview` **83.0.4103.120** |
| Teclado | `com.kika.car.inputmethod` |

## 3. Qué agrega Aurora Store Jetour

1. **Perfil de tablet para Google Play** (`JetourTabletProfile.kt`), activo por defecto y con el gestor de suplantación oculto.
   - Identidad: Samsung Galaxy Tab S7+ Wi-Fi, SM-T970, Android 11, con firma certificada: `samsung/gts7xlwifixx/gts7xlwifi:11/RP1A.200720.012/T970XXU1BUAA:user/release-keys`. La fuente es la lista de MagiskHidePropsConf.
   - Hardware real leído en vivo (OpenGL, ABI, idiomas), pantalla 1920×1080, XLARGE.
   - Sin `type.automotive` ni `broadcastradio`, y sin la librería `autolink.shared.sdk`.
2. **Las apps ven una tablet, no un auto** (`CarEnvironment.java` + `PackageManagerProxy.java`). Dentro de cada app parcheada se oculta `type.automotive` y el modo "car" pasa a "normal". Así Waze dejó de pedir Android Auto.
3. **Vibrador falso** (`NoVibratorService.java`). Si el equipo no tiene vibrador, responde "no hay vibración" en vez de dejar que la app se cierre.
4. **Sin App Zygote** (`Patcher.kt` → `disableAppZygote`). Al parchear se quitan `useAppZygote` y `zygotePreloadName`, que en esta pantalla hacen que el sistema cierre Chrome a la fuerza.
5. **Bienvenida:** los botones Atrás / Continuar suben 149 px para que no los tape la barra del aire.
6. **Marca propia:** nombre, paquete, ícono propio, bienvenida en español con créditos, reporte de errores apagado (el servidor era de kangrio) y actualizaciones desde el repo propio.

## 4. Historial de versiones

| Versión | Qué cambió |
|---|---|
| 1.0.0 | Primera versión: perfil tablet, apps ven tablet, todo Jetour |
| 1.0.1 | Chrome sin parchear. **Lo empeoró** (falta de microG). Revertido. |
| 1.0.2 | Igual a 1.0.0 (vuelta atrás) |
| 1.0.3 | Botones de la bienvenida por encima de la barra del aire |
| 1.0.4 | Vibrador falso: arregla Google, que se cerraba al tocar el buscador (parche 5) |
| 1.0.5 | Sin App Zygote: arregla Chrome, que se cerraba al abrir (parche 6) |

## 5. Problemas resueltos (y cómo se encontraron)

| Problema | Causa real | Solución |
|---|---|---|
| Waze pide Android Auto, apps en modo teléfono | La pantalla declara `type.automotive` | Perfil tablet + `CarEnvironment` |
| Aurora "nativa" no bajaba nada | Por `type.automotive`, Google Play muestra el catálogo de autos | Perfil tablet |
| Botón Continuar tapado | La barra del aire no se informa como barra de navegación | +149 px solo en la bienvenida |
| Google se cierra al tocar el buscador | Google nuevo usa vibración y Android 11 sin vibrador lanza `NullPointerException` | Vibrador falso (1.0.4) |
| Google se cierra al cargar sugerencias (`SIGTRAP` en `libwebviewchromium.so`) | WebView **153** instalada a mano el 23/10, choca con Google 17.70 | `pm uninstall com.android.webview`: vuelve a la de fábrica (83). Waze anda igual. |
| Chrome se cierra al abrir | `Failure starting process ... sandboxed_process0` y `ChildZygoteProcess` nulo: el sistema no crea el App Zygote | Quitar App Zygote al parchear (1.0.5) |
| Netflix / Disney+ no reproducen | La pantalla no tiene Widevine | No tiene arreglo legal. Opciones: firmware de Jetour con Widevine, bajar en el celular, archivos propios con VLC o Kodi. |

**Lección:** no adivinar. Primero el registro del error, después el arreglo.

## 6. Comandos útiles por el puente

El puente corre los comandos **dentro del auto**: sin `adb` y sin `shell` adelante.

| Para qué | Comando |
|---|---|
| Agrandar el registro (el auto lo llena en segundos) | `logcat -G 16M` |
| Limpiar el registro | `logcat -c` |
| Errores de apps (Java) | `logcat -d -e "FATAL|AndroidRuntime|has died|Process: com"` |
| Cierres nativos (detalle `#00 pc ...`) | `logcat -d -s DEBUG` |
| Todo de un proceso | `logcat -d --pid=NÚMERO` |
| Qué hace Chrome o el sistema al abrir | `logcat -d -e "com.android.chrome|cr_|ActivityTaskManager|ActivityManager"` |
| Desinstalar (`uninstall` solo **no** existe) | `pm uninstall nombre.del.paquete` |
| Volver una app de sistema a la de fábrica | `pm uninstall com.android.webview` (solo borra la actualización) |
| Ver WebView activa | `dumpsys webviewupdate` |
| Datos de una app | `dumpsys package nombre.del.paquete` |
| Apps instaladas | `pm list packages -3` |
| Funciones y librerías del equipo | `pm list features` / `pm list libraries` |

Orden para capturar un cierre: `logcat -G 16M` → `logcat -c` → reproducir el cierre → comando de lectura.

## 7. Publicar una actualización

1. Subir `jetourVersionCode` en `app/build.gradle.kts`: 105 es 1.0.5, 106 es 1.0.6, etc.
2. Si cambia lo que se inyecta en las apps (`extension/` o `Patcher.kt`), subir también `patchVersionCode` (hoy en 6). Así las apps instaladas aparecen para re-parchearse.
3. GitHub → **Actions → Publicar Aurora Store Jetour → Run workflow**, escribir las novedades y apretar el botón.
4. Esperar el ✅ (unos 5 min; "Queued" es normal). Tus amigos ven la actualización en Aurora → Actualizaciones → Buscar actualizaciones.

## 8. Firma y seguridad

- **Llave:** `C:\aurora store jetour\llave\aurorajetour.jks`, alias `aurorajetour`. Hay respaldo en pendrive o Drive, con la contraseña anotada en papel.
- **Nunca subirla a GitHub.** Está en GitHub como secretos: `KEYSTORE_BASE64`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Si se pierde, no se pueden publicar más actualizaciones y todos tendrían que reinstalar de cero.
- Los builds `jetourDebug` de la PC son de prueba (`.debug`, firma de prueba). No van al auto.

## 9. Trabajar en la PC

- Proyecto: `C:\aurora store jetour\AuroraJetour-github` (clon del repo).
- Traer cambios: Android Studio → menú **master** → **Update Project** (Ctrl+T).
- Variante: **jetourDebug**. No aceptar "AGP Upgrade Assistant" ni actualizar Gradle.
- Java 21 se baja solo (plugin foojay en `settings.gradle.kts`).
- `C:\aurora store jetour\viejo`: respaldo de la versión vieja.

## 10. Instalación limpia (para amigos o para el video)

1. Desinstalar las apps de Aurora, MicroG (`com.google.android.gms`, `com.google.android.gsf`, `com.android.vending`) y cualquier Aurora vieja.
2. Instalar `AuroraStoreJetour.apk` desde el link.
3. Asistente → iniciar sesión.
4. Instalar una app y aceptar el paquete MicroG.
5. Inicio automático de MicroG Service y MicroG Companion.

## 11. Pendientes e ideas

- Confirmar que Chrome anda con la 1.0.5 (iniciar sesión y elegirlo como navegador).
- WebView 153: quitada. Waze anda con la de fábrica (83). Si en el futuro se actualiza, que sea a una versión **más nueva que la de Chrome** y de la misma fuente; probar Google después.
- Releases viejas para borrar en GitHub si molestan: `jetour-1.0.0-5`, `jetour-1.0.1-4`.
- Posible tutorial o video de instalación para el README.
