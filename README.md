# 🎬 ReproVideoTreeUSB

**ReproVideoTreeUSB** es un reproductor de video y música de alto rendimiento diseñado y optimizado específicamente para unidades de cabecera Android de automóviles (radios Android chinas) y dispositivos móviles Android.

Su objetivo principal es ofrecer una experiencia fluida, inmersiva y de baja distracción para disfrutar de colecciones de **videos musicales y archivos de audio** organizados en estructuras de carpetas en pendrives USB o almacenamiento interno.

---

## 🌟 Características Destacadas

### 📁 1. Gestión de Archivos y Navegación USB
- **Escaneo Automático de USB**: Detecta automáticamente unidades pendrive o discos duros externos USB al conectarse (`ACTION_MEDIA_MOUNTED`).
- **Exploración Jerárquica**: Navegación intuitiva por árbol de carpetas en almacenamiento interno (`/storage/emulated/0`) y unidades USB montadas.
- **Botón Inicio (Casita)**: Salto directo a la carpeta predeterminada configurada (ej. `Videos Musicales`).
- **Cierre Seguro**: Protección contra cierres accidentales al conducir (requiere presionar "Atrás" dos veces para salir).

### 🎥 2. Motor de Reproducción & Transición Continua
- **AndroidX Media3 ExoPlayer (v1.5.1)**: Decodificación fluida de alto rendimiento.
- **Amplio Soporte de Formatos**:
  - **Video**: `.mp4`, `.mkv`, `.avi`, `.webm`, `.3gp`, `.ts`, `.mov`, `.mpg`, `.mpeg`.
  - **Audio**: `.mp3`, `.m4a`, `.wav`, `.flac`, `.aac`, `.ogg`.
- **Reanudación Automática al Encender (Vehicle Resume)**: Memoriza el último archivo reproducido y la posición exacta en segundos, reanudando automáticamente al abrir la app o encender el vehículo.
- **Reproducción Continua Global entre Carpetas**: Al terminar los videos de un directorio, la app pasa de forma transparente a la siguiente carpeta de la colección.

### 🔀 3. Sistema de 4 Modos de Aleatorio (3-Level Shuffle)
- ⏹️ **Modo 0 (Ordenado)**: Reproducción estricta por orden alfabético.
- 📁 **Modo 1 (Carpeta Actual)**: Mezcla y reproduce exclusivamente los archivos de la carpeta actual sin salirse de ella.
- 📂 **Modo 2 (Categoría Padre)**: Mezcla todas las subcarpetas dentro de la categoría principal (ej. `Salsa`).
- 🌐 **Modo 3 (General / Colección Completa)**: Mezcla toda la colección a partir de la carpeta raíz de inicio.
- **Persistencia de Modo**: Memoriza el último modo aleatorio seleccionado al reiniciar la app.
- **Cambio en Tiempo Real**: Cambia de modo aleatorio sin pausar ni reiniciar el video en reproducción.
- **Control Unificado Cíclico**: Botón amplio ($54\times54\text{ dp}$) en el reproductor de video que rota secuencialmente mostrando su color temático (Blanco, Morado, Verde, Naranja).

### ❤️ 4. Categoría de Favoritos y Más Vistos (Top Played)
- **Favoritos Corazón ❤️**: Botón en el reproductor y explorador para marcar/desmarcar canciones y videos favoritos.
- **Filtro de Favoritos por Categoría**: El botón "❤️ Favoritos" reproduce únicamente los archivos favoritos pertenecientes a la carpeta raíz o categoría actual.
- **Sección Más Vistos**: Contabiliza automáticamente las reproducciones y genera listas especiales ordenadas por frecuencia de reproducción (umbral configurable de 1 a 50 reproducciones).

### 🌙 5. Modo Pantalla Apagada (Screen Off / Audio Mode)
- **Ahorro de Energía & Conducción Nocturna**: Apaga el panel de video manteniendo el reproductor de audio activo.
- **Reloj Digital Gigante (`116sp` Bold)**: Ubicado en la esquina superior derecha para una lectura instantánea sin apartar la vista del camino.
- **Indicador de Fecha Proporcional (`22sp` Bold)**: Muestra el día de la semana y fecha (ej. `Lunes, 25 de Octubre`) en Morado Neón (`#BB86FC`).
- **Metadatos ID3 & Carátulas**: Extrae título, artista, álbum, año, género, bitrate (KBPS) y portada de álbum (`MediaMetadataRetriever`).
- **Ecualizador Dinámico Animado**: Barras gráficas de espectro en tiempo real al ritmo de la música.

### 🕹️ 6. Controles Gestuales e Integración con Automóvil
- **3 Zonas Táctiles en Pantalla (Izquierda / Centro / Derecha)**:
  - **Izquierda**: Pista Anterior / Retroceso rápido (pulsación prolongada).
  - **Centro**: Play / Pausa / Bloqueo de pantalla (mantener 3s).
  - **Derecha**: Pista Siguiente / Avance rápido (pulsación prolongada).
- **Gestos de Deslizamiento Vertical**:
  - Borde izquierdo: Ajuste de volumen del sistema (`AudioManager`).
  - Borde derecho: Ajuste de brillo de pantalla (`screenBrightness`).
- **Feedback Visual Flotante**: Iconos translúcidos ($100\times100\text{ dp}$) con fondo circular para confirmación visual instantánea.
- **Inteligencia de Audio (AudioFocus)**: Atenúa el volumen automáticamente durante instrucciones del GPS (Google Maps, Waze) y pausa la música en llamadas entrantes.
- **Mandos de Volante**: Compatible con botones físicos y mandos multimedia del volante (`dispatchKeyEvent`).

### 🛠️ 7. Personalización Avanzada y Modos de Usuario
- **Perfiles Multi-Usuario**: Creación y conmutación rápida entre modos de usuario (ej. *Conductor*, *Niño 1*, *Niño 2*, *Esposa*, *Madre*, *Invitado*), cada uno con sus atajos de carpeta predeterminada.
- **Ajustes Configurables**:
  - Modo Noche / Día (Adaptación de alto contraste para sol intenso o conducción nocturna).
  - Modo Piloto / Copiloto (Invierte los controles de navegación lateral).
  - Ocultar extensiones de archivo (`.mp4`, `.avi`).
  - Posición del título del video (Arriba o Abajo).
  - Segundos de salto personalizables (1 a 60 seg).
  - Tamaño de fuente y altura de renglones del explorador.
  - Idioma de la interfaz (Español e Inglés).

---

## 🛠️ Especificaciones Técnicas

| Componente | Especificación |
| :--- | :--- |
| **Lenguaje** | Java 11 |
| **Plataforma Android** | Android SDK (`compileSdk 35`, `targetSdk 34`, `minSdk 21`) |
| **Motor de Reproducción** | AndroidX Media3 ExoPlayer `1.5.1` |
| **Componentes de UI** | AndroidX AppCompat `1.6.1`, Material Components `1.10.0`, ConstraintLayout `2.1.4` |
| **Build System** | Gradle con Android Gradle Plugin (AGP) `9.2.1` |

---

## 📱 Requisitos de Instalación
- **Dispositivo**: Radio Android para automóviles (Android Head Unit) o celular/tablet Android.
- **Versión de Android**: Android 5.0 (API level 21) o superior.
- **Orientación de Pantalla**: Horizontal (`landscape`).
