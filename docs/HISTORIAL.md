# Aurora Store Jetour — Historial del proyecto

Registro de todo lo que se hizo, por qué, y cómo seguir. Última actualización: 26/11/2026.

---

## 1. Qué es

**Aurora Store Jetour** es una versión de Aurora Store para la pantalla de la **Jetour T1**. Desde la 1.0.7 está basada en **GStore** de kangrio (el nuevo nombre de [AuroraStore-BYD](https://github.com/kangrio/AuroraStore-BYD)). Hasta la 1.0.6 se basaba en AuroraStore-BYD 1.0.3.

- **Repo:** https://github.com/MainthorCar/AuroraJetour (es un fork, con los créditos visibles).
- **Paquete:** `com.mainthorcar.aurorajetour`
- **Link de instalación** (siempre baja la última versión):
  `https://github.com/MainthorCar/AuroraJetour/releases/latest/download/AuroraStoreJetour.apk`
- **Licencia:** GPL-3.0. El código tiene que ser público y hay que mantener los créditos (Aurora OSS / whyorean, kangrio, microG, ARSCLib, LSPosed, Morphe).

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
2. **Las apps ven una tablet, no un auto** (`CarEnvironment.java` + `PackageManagerProxy.java`). Tiene 3 partes:
   - **features:** oculta `type.automotive`. **Imprescindible:** sin esto Waze no arranca.
   - **uimode:** `UiModeManager.getCurrentModeType()` responde "normal".
   - **config:** cambiaba `Configuration.uiMode` de "auto" a "normal". **Apagada desde la 1.1.2**, porque hacía que el sistema reiniciara sin parar las pantallas que no se acomodan solas (parpadeo en el cluster: Spotify, resumen de ruta de Waze 5.28).
   - **Llaves de prueba** (propiedad `debug.jetour.carenv`, se borra al reiniciar el auto): `setprop debug.jetour.carenv config_on` vuelve a prender config; `uimode`, `features` o `all` apagan esas partes. Después, `am force-stop paquete` y abrir la app. `logcat -d -s CarEnvironment` muestra qué quedó apagado.
3. **Vibrador falso** (`NoVibratorService.java`). Si el equipo no tiene vibrador, responde "no hay vibración" en vez de dejar que la app se cierre.
4. **Sin App Zygote** (`Patcher.kt` → `disableAppZygote`). Al parchear se quitan `useAppZygote` y `zygotePreloadName`, que en esta pantalla hacen que el sistema cierre Chrome a la fuerza.
5. **Bienvenida:** los botones Atrás / Continuar suben 149 px para que no los tape la barra del aire.
6. **Marca propia:** nombre, paquete, ícono propio (también en la pantalla de inicio de la app), bienvenida en español con créditos, reporte de errores apagado (el servidor era de kangrio) y actualizaciones desde el repo propio.
8. **Apps excluidas del modo tablet** (`CarEnvironment.java` → `EXCLUDED_PACKAGES`). Algunas apps no se bancan el cambio de modo "auto" a "normal". A esas no se les aplica nada del punto 2 (ni UiModeManager, ni `uiMode`, ni funciones ocultas); siguen con microG y el vibrador falso. Hoy: **Spotify** (`com.spotify.music`). En el registro aparece `CarEnvironment: modo tablet desactivado para <paquete>`.
7. **App en español** (`res/values-es/strings.xml`). GStore había quitado las traducciones; se recuperó el español y se tradujeron los textos nuevos. Los demás idiomas quedan en inglés.

**Lo que viene de GStore (kangrio):** microG, parcheo de apps, instaladores y la **preparación de YouTube y YouTube Music en la propia pantalla con Morphe** (baja la versión correcta de APKMirror y la parchea). Por eso, desde la 1.0.7, YouTube volvió a funcionar.

## 4. Historial de versiones

| Versión | Qué cambió |
|---|---|
| 1.0.0 | Primera versión: perfil tablet, apps ven tablet, todo Jetour |
| 1.0.1 | Chrome sin parchear. **Lo empeoró** (falta de microG). Revertido. |
| 1.0.2 | Igual a 1.0.0 (vuelta atrás) |
| 1.0.3 | Botones de la bienvenida por encima de la barra del aire |
| 1.0.4 | Vibrador falso: arregla Google, que se cerraba al tocar el buscador (parche 5) |
| 1.0.5 | Sin App Zygote: arregla Chrome, que se cerraba al abrir (parche 6) |
| 1.0.6 | Intento de arreglar YouTube con las versiones preparadas de kangrio. **No sirvió**: kangrio dejó de publicarlas. |
| 1.0.7 | **Pasa a la base GStore** (kangrio), con YouTube preparado en la pantalla con Morphe. Se mantiene todo lo de Jetour (parche 8). Publicada primero como prueba (`jetour-1.0.7-prueba-11`); YouTube, Chrome, Maps y microG andan. |
| 1.0.8 | Logo propio en la pantalla de inicio y vuelve el español. Primera versión normal con la base GStore. |
| 1.0.9 | Spotify fuera del modo tablet: deja de parpadear en el cluster del T1 Launcher (parche 9). Probada primero como `jetour-1.0.9-prueba-13`. |
| 1.1.0 (prueba) | No tocar la configuración solo en las pantallas del cluster. **No alcanzó**: Waze siguió parpadeando. Descartada. |
| 1.1.1 (prueba) | Llaves `debug.jetour.carenv` para apagar partes del modo tablet. Resultado con Waze 5.28: all = no anda, features = no arranca, uimode = igual, **config = bastante mejor**. |
| 1.1.2 | Parte **config** apagada de fábrica (parche 12). Arregla el parpadeo de Waze nuevo en el cluster. Publicada para todos el 26/11 para probar entre varios. |

## 5. Problemas resueltos (y cómo se encontraron)

| Problema | Causa real | Solución |
|---|---|---|
| Waze pide Android Auto, apps en modo teléfono | La pantalla declara `type.automotive` | Perfil tablet + `CarEnvironment` |
| Aurora "nativa" no bajaba nada | Por `type.automotive`, Google Play muestra el catálogo de autos | Perfil tablet |
| Botón Continuar tapado | La barra del aire no se informa como barra de navegación | +149 px solo en la bienvenida |
| Google se cierra al tocar el buscador | Google nuevo usa vibración y Android 11 sin vibrador lanza `NullPointerException` | Vibrador falso (1.0.4) |
| Google se cierra al cargar sugerencias (`SIGTRAP` en `libwebviewchromium.so`) | WebView **153** instalada a mano el 23/10, choca con Google 17.70 | `pm uninstall com.android.webview`: vuelve a la de fábrica (83). Waze anda igual. |
| Chrome se cierra al abrir | `Failure starting process ... sandboxed_process0` y `ChildZygoteProcess` nulo: el sistema no crea el App Zygote | Quitar App Zygote al parchear (1.0.5) |
| YouTube queda en "Actualizar" y no abre | kangrio dejó de publicar las APK de YouTube ya preparadas | Base GStore: prepara YouTube en la pantalla con Morphe (1.0.7) |
| Actualizaciones de microG quedan en "Instalando" para siempre | Descargas viejas trabadas en Aurora → Descargas | Aurora → Descargas → **Cancel all** y borrar las descargas viejas. Después se instalan solas. |
| Íconos distintos después de la 1.0.7 | GStore trajo su propio logo para la pantalla de inicio | Logo propio otra vez (1.0.8). Los iconitos de menús quedan con el estilo de GStore. |
| App en inglés después de la 1.0.7 | GStore quitó las traducciones | Español recuperado (1.0.8) |
| Spotify parpadea en el cluster del T1 Launcher (de lento a rapidísimo); en la pantalla principal anda bien | Nuestro modo tablet (`CarEnvironment`) le cambia el modo "auto" por "normal". Dentro de la pantalla escondida del cluster eso hace que el sistema reinicie la pantalla de Spotify cada ~3 s (`wm_relaunch_resume_activity` en `logcat -b events`). Maps, Waze y YouTube se bancan el cambio; Spotify no. | Spotify en `EXCLUDED_PACKAGES` (1.0.9) y reinstalarlo desde Aurora. |
| Waze 5.28 parpadea en el cluster al buscar o cargar una ruta (resumen de ruta), micrófono y "iniciar rápido" fallan | La parte **config** del modo tablet: `TripOverviewActivity` se reiniciaba ~3 veces por segundo (`wm_relaunch_resume_activity`) en la pantalla escondida del cluster (`displayId=3`). Waze 5.27.0.1 (código `1030748`) no tenía esa pantalla. | Config apagada de fábrica (1.1.2). Volver atrás: Descarga manual `1030748`. |
| Spotify 5.6.0 "no abre" en el auto de un amigo | Tenía la **Aurora oficial 4.8.4** (`com.aurora.store`), que no hace el modo tablet: Google Play le dio el **Spotify para autos** (Android Automotive). | Desinstalar la Aurora oficial, instalar Aurora Store Jetour, iniciar sesión e instalar Spotify (9.x) de nuevo. |
| Netflix / Disney+ no reproducen | La pantalla no tiene Widevine | No tiene arreglo legal. Opciones: firmware de Jetour con Widevine, bajar en el celular, archivos propios con VLC o Kodi. |

**Lección:** no adivinar. Primero el registro del error, después el arreglo.

**Lección del caso Spotify (18/11):** Aurora **modifica todas las apps que instala** (les mete `extension/`). Si una app instalada desde Aurora falla en algo que en otro lado anda bien, **lo primero es sospechar de nuestro parche** y probarlo desde Aurora Jetour, antes de probar versiones de la app o cosas de afuera. Por qué se descartaron otras causas: tamaño de ventana, orientación, versión de Spotify (9.1.94 y 9.2.0 fallaban igual) y el T1 Launcher.

**Si otra app falla parecido:**
1. Ver si se reinicia sola: `logcat -b events -c`, reproducir, `logcat -b events -d -e "nombre.del.paquete"`. Muchos `wm_relaunch_resume_activity` seguidos = se reinicia.
2. Probar sacarla del modo tablet: agregar su paquete a `EXCLUDED_PACKAGES` en `extension/.../CarEnvironment.java`, subir `jetourVersionCode` y `patchVersionCode`, publicar como **prueba**, reinstalar la app desde Aurora.
3. Si se arregla, publicar normal. Si no, sacarla de la lista.

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

| ¿La app tiene nuestro modo tablet? | `logcat -d -s CarEnvironment` (si está excluida dice "modo tablet desactivado") |
| Reinicios de pantalla de una app | `logcat -b events -d -e "nombre.del.paquete"` |
| Ventanas, pantallas y tamaños | `am stack list` |
| Lo que anota una sola app | `logcat -d --pid=$(pidof nombre.del.paquete)` |
| Versión de una app | `dumpsys package nombre.del.paquete \| grep versionName` |
| Registros internos de ventanas (en este auto existen `WM_DEBUG_RESIZE` y `WM_DEBUG_ORIENTATION`; **no** existen `WM_DEBUG_STATES` ni `WM_DEBUG_CONFIGURATION`) | `wm logging enable-text WM_DEBUG_RESIZE` (de a uno; apagar con `disable-text`) |

Notas del puente: los comandos van de a uno (si uno falla, lo que sigue en la misma línea no corre). "Instalar app" acepta solo `.apk`; un `.xapk` o `.apkm` son varios APK juntos. En Aurora, **Descarga manual** necesita el código exacto de la variante de Google Play (arm64-v8a, Android 11 / API 30, densidad 213); un código de otra variante da "App not supported". "Consultar versiones" depende de Exodus Privacy y puede venir vacío.

Orden para capturar un cierre: `logcat -G 16M` → `logcat -c` → reproducir el cierre → comando de lectura.

## 7. Publicar una actualización

1. Subir `jetourVersionCode` en `app/build.gradle.kts`: 107 es 1.0.7, 108 es 1.0.8, etc.
2. Si cambia lo que se inyecta en las apps (`extension/` o `Patcher.kt`), subir también `patchVersionCode` (hoy en 12). Así las apps instaladas aparecen para re-parchearse.
3. GitHub → **Actions → Publicar Aurora Store Jetour → Run workflow** (o pedírselo a Claude, que puede lanzarlo desde la sesión), elegir la rama **master**, escribir las novedades y apretar el botón.
   - Con **Versión de prueba** tildada se publica como pre-release (`jetour-X-prueba-N`). **No** le llega a nadie como actualización: la bajás vos a mano y la probás.
   - Sin tildar, es la versión normal y les llega a todos.
4. Esperar el ✅ (unos 5 min; "Queued" es normal). Tus amigos ven la actualización en Aurora → Actualizaciones → Buscar actualizaciones.

## 8. Firma y seguridad

- **Llave:** `C:\aurora store jetour\llave\aurorajetour.jks`, alias `aurorajetour`. Hay respaldo en pendrive o Drive, con la contraseña anotada en papel.
- **Nunca subirla a GitHub.** Está en GitHub como secretos: `KEYSTORE_BASE64`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Si se pierde, no se pueden publicar más actualizaciones y todos tendrían que reinstalar de cero.
- Los builds `vanillaDebug` de la PC son de prueba (`.debug`, firma de prueba). No van al auto.

## 9. Trabajar en la PC

- Proyecto: `C:\aurora store jetour\AuroraJetour-github` (clon del repo).
- Traer cambios: Android Studio → menú **master** → **Update Project** (Ctrl+T).
- Variante: **vanillaDebug** (antes de la 1.0.7 era jetourDebug). No aceptar "AGP Upgrade Assistant" ni actualizar Gradle.
- Java 21 se baja solo (plugin foojay en `settings.gradle.kts`).
- Las librerías de Morphe vienen de GitHub Packages: para compilar en la PC hace falta `gpr.user` (tu usuario) y `gpr.key` (un token de GitHub con permiso `read:packages`) en `C:\Users\<vos>\.gradle\gradle.properties`. En GitHub Actions no hace falta nada.
- `C:\aurora store jetour\viejo`: respaldo de la versión vieja.

## 10. Instalación limpia (para amigos o para el video)

1. Desinstalar las apps de Aurora, MicroG (`com.google.android.gms`, `com.google.android.gsf`, `com.android.vending`) y cualquier Aurora vieja.
2. Instalar `AuroraStoreJetour.apk` desde el link.
3. Asistente → iniciar sesión.
4. Instalar una app y aceptar el paquete MicroG.
5. Inicio automático de MicroG Service y MicroG Companion.

## 11. Pendientes e ideas

- YouTube se vuelve a abrir y pausa la música al volver desde el T1 Launcher: lo más probable es que el launcher abra la app con `CLEAR_TASK`/`CLEAR_TOP`. Hay que usar `getLaunchIntentForPackage` con solo `FLAG_ACTIVITY_NEW_TASK`. Se ve en el chat del launcher.
- Confirmar con varios usuarios que la 1.1.2 anda bien: Waze nuevo en el cluster (parpadeo, micrófono, iniciar rápido) y que en la pantalla principal **no pida Android Auto**. Si con config apagada todo anda, probar sacar a Spotify de `EXCLUDED_PACKAGES`.
- Paquetes de Auroras incompatibles, para avisar desde el T1 Launcher: `com.aurora.store`, `com.aurora.store.nightly`, `com.aurora.store.byd`, `com.kangrio.gstore`. La correcta es `com.mainthorcar.aurorajetour`.
- Traer novedades de kangrio: `git fetch kangrio` y unirlas a master (el remoto `kangrio` es solo para leer, **nunca** se sube nada ahí).
- WebView 153: quitada. Waze anda con la de fábrica (83). Si en el futuro se actualiza, que sea a una versión **más nueva que la de Chrome** y de la misma fuente; probar Google después.
- Releases viejas para borrar en GitHub si molestan: `jetour-1.0.0-5`, `jetour-1.0.1-4`.
- Posible tutorial o video de instalación para el README.
