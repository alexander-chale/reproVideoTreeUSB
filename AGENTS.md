# AGENTS.md - Guía de Arquitectura y Desarrollo para Agentes AI

Bienvenido al repositorio de **ReproVideoTreeUSB**. Este archivo sirve como la guía técnica principal para agentes de inteligencia artificial y desarrolladores que contribuyan o realicen modificaciones en esta base de código.

---

## 1. Visión General del Proyecto

**ReproVideoTreeUSB** es un reproductor de video y música de alto rendimiento diseñado y optimizado específicamente para unidades de cabecera Android de automóviles (radios chinas) y dispositivos móviles Android.

Su propósito fundamental es brindar una experiencia fluida, inmersiva y de baja distracción para disfrutar de colecciones de **videos musicales** organizadas en estructuras de carpetas en pendrives USB o almacenamiento interno.

### Características Clave del Sistema:
- **Navegación Jerárquica e Integración USB**: Escaneo automático de unidades USB montadas (`UsbReceiver`) y exploración intuitiva de directorios.
- **Reproducción Continua Global de Carpetas**: Al terminar los videos de una carpeta, el reproductor pasa de forma transparente a la siguiente carpeta del almacenamiento.
- **Reanudación Automática (Resume)**: Memoriza el último video reproducido y la posición exacta en segundos, reanudando la reproducción al abrir la app.
- **Controles Táctiles y Gestuales en Pantalla**:
  - 3 Zonas Táctiles (Izquierda / Centro / Derecha) para salto de pista, play/pause y avance/retroceso rápido por pulsación prolongada.
  - Gestos de deslizamiento vertical para control de **Volumen** (borde izquierdo) y **Brillo** (borde derecho) con feedback visual flotante.
- **Integración con Automóvil (AudioFocus y Botones de Volante)**: Manejo inteligente de `AudioFocus` (atenuación/pausa automática durante instrucciones de GPS de Maps/Waze o llamadas telefónicas). Captura de botones físicos mediante `dispatchKeyEvent`.
- **Modo Pantalla Apagada (Screen Off / Audio Mode)**: Apaga el panel de video manteniendo el audio activo, mostrando una pantalla con ecualizador gráfico animado y metadatos ID3 (artista, álbum, bitrate, año).
- **Personalización Avanzada & Modos de Usuario**: Modo Piloto (invierte gestos laterales), modo noche/oscuro, dimensiones de fila y tamaño de fuente configurables, selector de idioma (Español e Inglés) y atajos de carpeta predeterminada por modo de usuario.

---

## 2. Tech Stack y Entorno de Desarrollo

> [!IMPORTANT]
> **REGLA ESTRICTA DE ENTORNO Y TECH STACK (INVIOLABLE)**:
> Bajo NINGUNA circunstancia ni por ninguna razón se debe modificar el **Tech Stack**, las versiones de librerías (ExoPlayer `1.5.1`, AGP `9.2.1`, Java `11`, `minSdk 21`, `targetSdk 34`, `compileSdk 35`) ni la configuración del entorno de desarrollo.
> 
> Esta combinación específica de tecnologías y versiones ha sido elegida y validada rigurosamente para garantizar la compatibilidad con **radios Android de automóviles (unidades chinas)**. Cualquier cambio en dependencias, versiones de SDK o configuración de Gradle romperá el funcionamiento en el hardware embebido del vehículo y requerirá pruebas manuales innecesarias.

| Componente | Tecnología / Versión |
| :--- | :--- |
| **Lenguaje** | Java 11 |
| **Plataforma Android** | Android SDK (`compileSdk 35`, `targetSdk 34`, `minSdk 21`) |
| **Motor de Video y Audio** | AndroidX Media3 ExoPlayer `1.5.1` (`androidx.media3:media3-exoplayer`, `androidx.media3:media3-ui`) |
| **Componentes UI** | AndroidX AppCompat `1.6.1`, Material Components `1.10.0`, ConstraintLayout `2.1.4` |
| **Build System** | Gradle con Android Gradle Plugin (AGP) `9.2.1` |
| **Pruebas** | JUnit `4.13.2`, Espresso `3.5.1`, AndroidX Test Ext `1.1.5` |

---

## 3. Estructura del Repositorio

```
reproVideoTreeUSB/
├── app/
│   ├── build.gradle                  # Configuración del módulo de la app (SDKs, dependencias)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # Permisos de almacenamiento, orientación landscape, receiver USB
│       │   ├── java/com/echaleleon/reprovideotreeusb/
│       │   │   ├── MainActivity.java # Controlador principal (UI, ExoPlayer, Gestos, AudioFocus, Modos)
│       │   │   └── UsbReceiver.java  # BroadcastReceiver para eventos de montaje USB (MEDIA_MOUNTED)
│       │   └── res/
│       │       ├── layout/           # Vistas XML: activity_main.xml, dialog_settings.xml, item_carpeta.xml
│       │       ├── values/           # Recurso de cadenas (Español por defecto), colores, temas
│       │       └── values-en/        # Recurso de cadenas en Inglés
│       ├── androidTest/             # Pruebas de instrumentación
│       └── test/                    # Pruebas unitarias
├── AGENTS.md                         # Esta guía
├── ReproVideoTreeUSB.txt            # Manual de usuario y especificación funcional
├── build.gradle                     # Configuración Gradle raíz
├── gradle.properties
└── settings.gradle
```

---

## 4. Arquitectura y Componentes Clave

### 4.1. `MainActivity.java`
Clase central que concentra la lógica de control y presentación:
- **Gestión de ExoPlayer**: Instanciación de `ExoPlayer`, vinculación con `PlayerView`, escuchadores de eventos de reproducción y liberación oportuna de recursos.
- **Navegación en Árbol de Carpetas**: Filtrado por extensiones soportadas (`mp4`, `mkv`, `avi`, `webm`, `mp3`, `wav`, etc.), ordenación alfabética y resolución de saltos globales entre directorios.
- **Procesamiento de Gestos y Zonas Táctiles**:
  - Tres áreas de toque en pantalla (`zonaIzquierda`, `zonaCentro`, `zonaDerecha`).
  - Pulsaciones cortas para reproducción/pausa y navegación de pistas.
  - Pulsaciones largas para búsqueda rápida (seek loop) y retorno al explorador.
  - Gestos de arrastre vertical para ajustar el volumen del sistema (`AudioManager`) y el brillo de pantalla (`WindowManager.LayoutParams.screenBrightness`).
- **Manejo de AudioFocus**: Registro de `AudioFocusRequest` con `USAGE_MEDIA` para interactuar limpiamente con el sistema de sonido del vehículo y aplicaciones de navegación GPS.
- **Modo Pantalla Apagada (Screen Off)**: Activación de la vista `capaPantallaApagada`, extracción de metadatos de canciones/videos mediante `MediaMetadataRetriever` y animación dinámica de barras mediante `visualizerRunnable`.
- **Persistencia de Estado**: Guardado continuo de la carpeta actual, posición de reproducción, modo aleatorio (Shuffle) y ajustes de personalización en `SharedPreferences` ("config_repro").

### 4.2. `UsbReceiver.java`
- Escucha los eventos de broadcast del sistema `ACTION_MEDIA_MOUNTED`.
- Notifica al usuario mediante `Toast` y emite un broadcast interno (`REFRESCAR_USB`) para re-escanear `/storage/`.

---

## 5. Comandos de Construcción y Verificación

### Compilar APK en modo Debug:
```bash
./gradlew assembleDebug
```
*(En Windows PowerShell/CMD: `gradlew.bat assembleDebug`)*

### Ejecutar Pruebas Unitarias:
```bash
./gradlew test
```

### Ejecutar Pruebas de Instrumentación:
```bash
./gradlew connectedAndroidTest
```

---

## 6. Guía y Convenciones para Agentes AI

Cuando realices modificaciones o agregues código a este proyecto, sigue estas reglas:

1. **REGLA ABSOLUTA - PROHIBIDO MODIFICAR TECH STACK O ENTORNO**:
   - Está **estrictamente prohibido** alterar, actualizar o degradar dependencias en `build.gradle` o `gradle/libs.versions.toml`, cambiar versiones del SDK (`minSdk 21`, `targetSdk 34`, `compileSdk 35`), modificar la versión de Java (Java 11) o cambiar AGP/Gradle.
   - El entorno actual es el único calibrado y garantizado para funcionar en las radios Android chinas del vehículo.
2. **Compatibilidad Java 11**: Mantener el código en Java 11 neutro y limpio, respetando las directrices de compatibilidad de Android (`minSdk 21`).
3. **Diseño Orientado a Conducción (Landscape)**:
   - La aplicación opera exclusivamente en orientación horizontal (`landscape`).
   - Los elementos interactivos deben ser de gran tamaño y fácilmente accesibles.
4. **Manejo Correcto del Hilo de UI**:
   - Toda interacción con la interfaz debe realizarse en el hilo principal.
   - Utilizar `Handler(Looper.getMainLooper())` para temporizadores de UI, como la ocultación automática de barras y la animación del ecualizador.
5. **Ciclo de Vida de Reproducción de Medios**:
   - Pausar/liberar `ExoPlayer` e inactivar `visualizerHandler` adecuadamente en los métodos de ciclo de vida (`onPause`, `onStop`, `onDestroy`).
   - Desregistrar o solicitar la pérdida de `AudioFocus` al salir.
6. **Preservación de la Función Resume**:
   - Al modificar la lógica de listas o reproducción, verificar que no se rompa el guardado y restauración de posición en `SharedPreferences`.
7. **Localización y Cadenas de Texto**:
   - No hardcodear cadenas de texto en el código Java o en los layouts.
   - Definir siempre nuevas cadenas en `res/values/strings.xml` (Español) y `res/values-en/strings.xml` (Inglés).
8. **Robustez en Sistemas Embebidos / Automotrices**:
   - Tener en cuenta que las radios Android de automóviles suelen ejecutar versiones modificadas del sistema operativo y pueden tener permisos o rutas de almacenamiento heterogéneas. Incluir manejo adecuado de excepciones (`Exception`) al acceder a almacenamiento externo y configuraciones de pantalla.
