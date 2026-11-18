# Mis enlaces (Android, Kotlin)

App sencilla para guardar tus propios enlaces (AceStream, magnet y web) y abrirlos con un toque.
Los datos se guardan solo en el dispositivo. La app no usa internet ni pide permisos.

## Cómo compilarla

1. Instala Android Studio (Hedgehog 2023.1 o más reciente).
2. `File > Open` y elige esta carpeta (`MisLinks`).
3. Espera a que termine "Gradle sync". Si pide instalar el SDK 34, acepta.
4. Para probarla: conecta el móvil con depuración USB y pulsa `Run`.
   Para Android TV: crea un emulador "Television (1080p)" en Device Manager, o conecta tu TV por ADB
   (`adb connect IP_DE_LA_TV:5555`, tras activar "Depuración por red" en Opciones de desarrollador) y pulsa `Run`.
5. Para generar el APK: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
   El fichero queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Qué hace

- Añadir, editar y borrar enlaces con nombre y grupo; buscador por nombre o grupo.
- Historial de reproducción: justo debajo de la lista se guardan los últimos 20 enlaces de AceStream que abres (sin duplicados, el más reciente arriba). Toque corto vuelve a abrirlos; pulsación larga permite reproducir con la URL del motor, guardarlos en tus enlaces o quitarlos. "Borrar historial" está en el menú. El historial no se incluye en las exportaciones.
- Toque corto: abre el enlace. `acestream://` pasa a Ace Stream; `magnet:` y `https://` a la app que los gestione.
- Pulsación larga: abrir con un reproductor (URL del motor), copiar la URL del motor, copiar el enlace, editar o borrar.
- Compartir desde otra app: al compartir texto con "Mis enlaces" se abre el editor con el enlace ya relleno.
- Menú: importar desde el portapapeles (`Nombre | enlace | grupo`, M3U o enlaces sueltos), exportar M3U, exportar copia de seguridad, ajustes del motor y vaciar la lista.
- Enlaces admitidos: `acestream://ID`, un ID suelto de 40 caracteres hexadecimales, `magnet:?…` y `http(s)://…`.

## Ajustes

La URL del motor por defecto es `http://127.0.0.1:6878/ace/getstream?id={id}`.
Cámbiala en el menú si el motor está en otro equipo (por ejemplo `http://192.168.1.50:6878/ace/getstream?id={id}`).

## Estructura

- `Link.kt`: modelo y reconocimiento del tipo de enlace.
- `LinkStore.kt`: guardado de enlaces, historial y ajustes en SharedPreferences.
- `TextFormat.kt`: importación y exportación de texto.
- `LinkAdapter.kt`: filas de la lista.
- `MainActivity.kt`: pantalla principal, diálogos y acciones.

## Notas

- Este código no se ha compilado ni probado en un dispositivo. Si Android Studio marca algún error de versión, ajusta las versiones en `build.gradle.kts` y `app/build.gradle.kts`.
- Este proyecto sí está preparado para Android TV (ver abajo), pero sigue sin haberse probado en un televisor real.

## Android TV

- Aparece en el menú de aplicaciones de la TV con un banner de un balón de fútbol blanco y negro sobre fondo verde
  (`res/drawable-xhdpi/banner.png`, 320×180). En móviles y tablets el icono es el mismo balón sobre fondo verde.
- Manifiesto: categoría `LEANBACK_LAUNCHER`, banner, y pantalla táctil y `leanback` declarados como no obligatorios,
  de modo que la misma APK sirve para móvil y TV.
- Interfaz de TV (`layout-television`, `values-television`): tema oscuro, textos más grandes, márgenes de seguridad
  de 48dp/27dp y botones "Añadir enlace" y "Opciones" en la cabecera (sin botón flotante ni menú de tres puntos).
- Mando: las flechas mueven el foco (la fila seleccionada se resalta), OK abre el enlace, **OK mantenido** o la
  tecla Menú abren las opciones de la fila.
