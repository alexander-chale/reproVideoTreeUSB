package com.echaleleon.reprovideotreeusb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import androidx.appcompat.app.AlertDialog;


import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;



import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    private TextView txtRutaActual;
    private ListView listViewArchivos;
    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private FrameLayout contenedorVideo; // Declara la variable

    private TextView txtNombreVideo; // Declara la variable
    private LinearLayout capaZonasToque;
    private View zonaIzquierda;
    private View zonaCentro;
    private View zonaDerecha;

    private LinearLayout layoutProgreso;
    private SeekBar seekBarVideo;
    private TextView txtTiempoActual;
    private TextView txtTiempoRestante;
    private ImageView imgFeedback;
    private final android.os.Handler handlerProgreso = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable runnableProgreso;

    // NUEVAS VARIABLES:

    private LinearLayout contenedorExplorador;

    private Stack<File> pilaDeRutas = new Stack<>();
    private List<File> archivosEnCarpetaActual = new ArrayList<>();
    private List<File> videosEnReproduccionActual = new ArrayList<>();
    private File carpetaReproduciendoActualmente;

    private final android.os.Handler handlerSeek = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable runnableSeek;
    private long pressStartTime = 0;

    private int configItemHeight = 100; // Default en dp
    private int configTextSize = 32;   // Default en sp
    private String configDefaultFolder = "Videos Musicales";
    private boolean configTitleTop = true;
    private int configSeekSeconds = 10;
    private ImageButton btnConfiguracion;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Inicializar todas las vistas (NUEVO ORDEN Y VISTAS)
        txtRutaActual = findViewById(R.id.txtRutaActual);
        listViewArchivos = findViewById(R.id.listViewArchivos);
        contenedorExplorador = findViewById(R.id.contenedorExplorador);
        btnConfiguracion = findViewById(R.id.btnConfiguracion);

        SharedPreferences prefs = getSharedPreferences("config_repro", MODE_PRIVATE);
        configItemHeight = prefs.getInt("item_height", 100);
        configTextSize = prefs.getInt("text_size", 32);
        configDefaultFolder = prefs.getString("default_folder", "Videos Musicales");
        configTitleTop = prefs.getBoolean("title_top", true);
        configSeekSeconds = prefs.getInt("seek_seconds", 10);

        btnConfiguracion.setOnClickListener(v -> mostrarDialogoConfiguracion());

        // VINCULAR LAS NUEVAS VISTAS DEL VIDEO
        contenedorVideo = findViewById(R.id.contenedorVideo); // <-- ¡OBLIGATORIO!
        playerView = findViewById(R.id.playerView);
        txtNombreVideo = findViewById(R.id.txtNombreVideo);   // <-- ¡OBLIGATORIO!

        layoutProgreso = findViewById(R.id.layoutProgreso);
        seekBarVideo = findViewById(R.id.seekBarVideo);
        txtTiempoActual = findViewById(R.id.txtTiempoActual);
        txtTiempoRestante = findViewById(R.id.txtTiempoRestante);
        imgFeedback = findViewById(R.id.imgFeedback);

        capaZonasToque = findViewById(R.id.capaZonasToque);
        zonaIzquierda = findViewById(R.id.zonaIzquierda);
        zonaCentro = findViewById(R.id.zonaCentro);
        zonaDerecha = findViewById(R.id.zonaDerecha);

        // 2. Configurar Clics de las zonas táctiles sobre el video
        // ZONA IZQUIERDA: Video anterior O rebobinar si se mantiene presionado
        zonaIzquierda.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                pressStartTime = System.currentTimeMillis();
                handlerSeek.postDelayed(() -> {
                    iniciarSeekLoop(-configSeekSeconds * 1000);
                    mostrarFeedback(android.R.drawable.ic_media_rew);
                }, 3000);
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                handlerSeek.removeCallbacksAndMessages(null);
                if (System.currentTimeMillis() - pressStartTime < 3000) {
                    irAlVideoAnterior();
                    mostrarFeedback(android.R.drawable.ic_media_previous);
                }
                return true;
            }
            return true;
        });

        // ZONA CENTRO: Alternar entre Reproducir (Play) y Pausar (Pause)
        zonaCentro.setOnClickListener(v -> {
            if (exoPlayer.isPlaying()) {
                exoPlayer.pause();
                mostrarFeedback(android.R.drawable.ic_media_pause);
            } else {
                exoPlayer.play();
                mostrarFeedback(android.R.drawable.ic_media_play);
            }
        });

        // ZONA DERECHA: Siguiente video O adelantar si se mantiene presionado
        zonaDerecha.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                pressStartTime = System.currentTimeMillis();
                handlerSeek.postDelayed(() -> {
                    iniciarSeekLoop(configSeekSeconds * 1000);
                    mostrarFeedback(android.R.drawable.ic_media_ff);
                }, 3000);
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                handlerSeek.removeCallbacksAndMessages(null);
                if (System.currentTimeMillis() - pressStartTime < 3000) {
                    irAlSiguienteVideo();
                    mostrarFeedback(android.R.drawable.ic_media_next);
                }
                return true;
            }
            return true;
        });

        seekBarVideo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    exoPlayer.seekTo(progress);
                    txtTiempoActual.setText(formatearTiempo(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handlerProgreso.removeCallbacks(runnableProgreso);
                layoutProgreso.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                iniciarHilosProgreso();
                layoutProgreso.postDelayed(() -> layoutProgreso.setVisibility(View.GONE), 3000);
            }
        });

        // 1. Definimos el receptor con lógica mejorada
        BroadcastReceiver usbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Al detectar cambio, escaneamos las rutas comunes
                escanearYRefrescar();
                Toast.makeText(context, "Dispositivo detectado, actualizando...", Toast.LENGTH_SHORT).show();
            }
        };

        // 2. Registramos el filtro con más acciones para mayor compatibilidad
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addDataScheme("file");
        registerReceiver(usbReceiver, filter);

        // 3. ESCANEO INICIAL (La clave para radios chinos)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            escanearYRefrescar();
            Toast.makeText(this, "Buscando dispositivos externos...", Toast.LENGTH_SHORT).show();
        }, 1000); // 1000 milisegundos de espera

        // Configurar clics del explorador de archivos (ListView)
        listViewArchivos.setOnItemClickListener((parent, view, position, id) -> {
            File archivoSeleccionado = archivosEnCarpetaActual.get(position);

            if (archivoSeleccionado.isDirectory()) {
                navegarACarpeta(archivoSeleccionado);
            } else if (esVideo(archivoSeleccionado.getName())) {
                reproducirVideosDeCarpeta(archivoSeleccionado, carpetaReproduciendoActualmente);
            }
        });
        // 3. Inicializar Estados de Visibilidad Iniciales
        contenedorExplorador.setVisibility(View.VISIBLE);
        contenedorVideo.setVisibility(View.GONE); // <-- MANEJAR EL CONTENEDOR, NO EL PLAYER
        capaZonasToque.setVisibility(View.GONE);
        mostrarBarrasSistema();

        // 4. Inicializar ExoPlayer y su Listener Integrado

        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);

        exoPlayer.addListener(new Player.Listener() {

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                // 1. Verificación de seguridad: si es nulo, no hacemos nada
                if (mediaItem == null) return;

                // 2. Verificación de configuración local
                if (mediaItem.localConfiguration != null) {
                    String uri = mediaItem.localConfiguration.uri.toString();

                    // Solo mostramos si NO es uno de los archivos dummy
                    if (!uri.contains("dummy")) {
                        try {
                            // 3. Decodificar la URI para quitar los %20 y obtener el nombre limpio
                            String nombreArchivo = Uri.decode(new File(uri).getName());
                            mostrarNombreVideo(nombreArchivo);
                            mostrarFeedback(0); // Para mostrar la barra de progreso al cambiar
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }


            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    reproducirSiguienteCarpetaGlobal();
                } else if (playbackState == Player.STATE_READY) {
                    iniciarHilosProgreso();
                }
            }

            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                Toast.makeText(MainActivity.this, "Error de video: " + error.getMessage(), Toast.LENGTH_LONG).show();
                error.printStackTrace();
            }




            @Override
            public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
                int indiceActual = exoPlayer.getCurrentMediaItemIndex();
                int totalItems = exoPlayer.getMediaItemCount();

                // Intercepta si el reproductor automático cae en los límites de los archivos "dummy"
                if (indiceActual == 0 && tieneAnteriorCarpetaGlobal() && reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    reproducirCarpetaAnteriorGlobal();
                } else if (indiceActual == totalItems - 1 && tieneSiguienteCarpetaGlobal() && reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    reproducirSiguienteCarpetaGlobal();
                }
            }
        });

        // 5. Cargar almacenamiento raíz al iniciar la App
        File rutaRaiz = Environment.getExternalStorageDirectory();
        navegarACarpeta(rutaRaiz);
        // Pide permiso para gestionar todos los archivos (necesario en Android 10+ para leer USBs)
        //solicitarPermisoEspecial();
        // Dentro de onCreate:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                mostrarDialogoPermisos();
            } else {
                // Ya tiene permiso, escaneamos
                escanearYRefrescar();
            }
        }

    }

    private void mostrarDialogoPermisos() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Acceso al USB requerido")
                .setMessage("Para reproducir tus videos, necesito permiso para acceder a los archivos del USB.")
                .setPositiveButton("Dar Permiso", (dialog, which) -> {
                    solicitarPermisoEspecial();
                })
                .setCancelable(false) // Obliga al usuario a interactuar
                .show();
    }

    private void navegarACarpeta(File nuevaCarpeta) {
        if (nuevaCarpeta == null || !nuevaCarpeta.exists()) return;

        if (pilaDeRutas.isEmpty() || !pilaDeRutas.peek().getAbsolutePath().equals(nuevaCarpeta.getAbsolutePath())) {
            pilaDeRutas.push(nuevaCarpeta);
        }

        carpetaReproduciendoActualmente = nuevaCarpeta;
        txtRutaActual.setText("Ruta: " + nuevaCarpeta.getAbsolutePath());

        // Asegurar que el explorador sea visible al navegar
        listViewArchivos.setVisibility(View.VISIBLE);
        txtRutaActual.setVisibility(View.VISIBLE);
        contenedorExplorador.setVisibility(View.VISIBLE);
        contenedorVideo.setVisibility(View.GONE);
        capaZonasToque.setVisibility(View.GONE); // <-- Ocultar zonas táctiles
        mostrarBarrasSistema();

        archivosEnCarpetaActual.clear();
        List<File> carpetas = new ArrayList<>();
        List<File> videos = new ArrayList<>();

        File[] lista = nuevaCarpeta.listFiles();
        if (lista != null) {
            for (File f : lista) {
                if (f.isDirectory()) {
                    carpetas.add(f);
                } else if (esVideo(f.getName())) {
                    videos.add(f);
                }
            }
        }

        Collections.sort(carpetas, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        Collections.sort(videos, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

        archivosEnCarpetaActual.addAll(carpetas);
        archivosEnCarpetaActual.addAll(videos);

        List<String> nombresParaMostrar = new ArrayList<>();
        for (File f : archivosEnCarpetaActual) {
            nombresParaMostrar.add(f.isDirectory() ? "📁 " + f.getName() : "🎬 " + f.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.item_carpeta, R.id.txtNombreCarpeta, nombresParaMostrar) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);

                TextView text = view.findViewById(R.id.txtNombreCarpeta);
                text.setTextColor(android.graphics.Color.WHITE);

                // Aplicar configuraciones personalizadas
                text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, configTextSize);
                android.view.ViewGroup.LayoutParams params = text.getLayoutParams();
                params.height = (int) android.util.TypedValue.applyDimension(
                        android.util.TypedValue.COMPLEX_UNIT_DIP, configItemHeight, getResources().getDisplayMetrics());
                text.setLayoutParams(params);

                return view;
            }
        };
        listViewArchivos.setAdapter(adapter);
    }

    private boolean esVideo(String nombre) {
        String n = nombre.toLowerCase();
        return n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".avi");
    }

    private void reproducirVideosDeCarpeta(File videoInicial, File carpeta) {

        txtNombreVideo.setText(videoInicial.getName());
        txtNombreVideo.setVisibility(View.VISIBLE);

// Ocultar tras 3 segundos
        txtNombreVideo.postDelayed(() -> txtNombreVideo.setVisibility(View.GONE), 3000);
        // Cambio visual de pantallas
        contenedorExplorador.setVisibility(View.GONE);
        contenedorVideo.setVisibility(View.VISIBLE); // <-- ¡Cambio aquí!
        capaZonasToque.setVisibility(View.VISIBLE);
        ocultarBarrasSistema();
        mostrarFeedback(0); // Mostrar barra de progreso al iniciar

        exoPlayer.stop();
        exoPlayer.clearMediaItems();

        // === CORRECCIÓN AQUÍ: Limpiar y re-escanear la carpeta de forma segura ===
        videosEnReproduccionActual.clear();

        File[] archivos = carpeta.listFiles();
        if (archivos != null) {
            for (File f : archivos) {
                // Aseguramos que sea un archivo de video válido y no una carpeta
                if (f.isFile() && esVideo(f.getName())) {
                    videosEnReproduccionActual.add(f);
                }
            }
        }

        // Ordenar alfabéticamente para mantener la secuencia correcta
        Collections.sort(videosEnReproduccionActual, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

        // === VERIFICACIÓN DE SEGURIDAD ===
        if (videosEnReproduccionActual.isEmpty()) {
            Toast.makeText(this, "No se encontraron videos en esta carpeta", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- PASO A: CALCULAR ÍNDICE REAL ---
        int indiceReal = 0;
        for (int i = 0; i < videosEnReproduccionActual.size(); i++) {
            if (videosEnReproduccionActual.get(i).getAbsolutePath().equals(videoInicial.getAbsolutePath())) {
                indiceReal = i;
                break;
            }
        }

        // --- PASO B: INYECTAR DUMMY AL INICIO ---
        boolean tieneAnterior = tieneAnteriorCarpetaGlobal();
        if (tieneAnterior) {
            exoPlayer.addMediaItem(MediaItem.fromUri(Uri.parse("asset:///dummy_prev.mp4")));
            indiceReal++; // Desplazar el índice real
        }

        // --- PASO C: CARGAR LOS VIDEOS REALES ---
        for (File v : videosEnReproduccionActual) {
            exoPlayer.addMediaItem(MediaItem.fromUri(Uri.fromFile(v)));
        }

        // --- PASO D: INYECTAR DUMMY AL FINAL ---
        if (tieneSiguienteCarpetaGlobal()) {
            exoPlayer.addMediaItem(MediaItem.fromUri(Uri.parse("asset:///dummy_next.mp4")));
        }

        // Arrancar la reproducción en el video correcto
        exoPlayer.seekTo(indiceReal, 0);
        exoPlayer.prepare();
        exoPlayer.play();

        // Nueva línea para mostrar el nombre
        //mostrarNombreVideo(videoInicial.getName());
    }


    private void reproducirSiguienteCarpetaGlobal() {
        if (carpetaReproduciendoActualmente == null) return;
        File padre = carpetaReproduciendoActualmente.getParentFile();
        if (padre == null) return;

        File[] hermanos = padre.listFiles(File::isDirectory);
        if (hermanos != null) {
            List<File> listaCarpetas = new ArrayList<>();
            for (File h : hermanos) listaCarpetas.add(h);
            Collections.sort(listaCarpetas, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

            int indiceActual = listaCarpetas.indexOf(carpetaReproduciendoActualmente);
            if (indiceActual != -1 && indiceActual + 1 < listaCarpetas.size()) {
                File siguienteCarpeta = listaCarpetas.get(indiceActual + 1);

                // Forzar el cambio visual del explorador a esa carpeta tras bambalinas
                navegarACarpeta(siguienteCarpeta);

                contenedorExplorador.setVisibility(View.GONE);
                playerView.setVisibility(View.VISIBLE);
                capaZonasToque.setVisibility(View.VISIBLE); // <-- Asegurar que siga visible

                // Pero como seguimos reproduciendo, volvemos a ocultar el explorador
                txtRutaActual.setVisibility(View.GONE);
                listViewArchivos.setVisibility(View.GONE);

                // Buscar el primer video de esa nueva carpeta y dispararlo
                for (File f : archivosEnCarpetaActual) {
                    if (!f.isDirectory() && esVideo(f.getName())) {
                        reproducirVideosDeCarpeta(f, siguienteCarpeta);
                        break;
                    }
                }
            }
        }
    }

    // Interceptar el botón "Atrás" físico del sistema
    @Override
    public void onBackPressed() {
        // CASO A: Si el reproductor está visible, lo cerramos y regresamos al explorador
        if (contenedorVideo.getVisibility() == View.VISIBLE) {
            exoPlayer.stop();
            contenedorVideo.setVisibility(View.GONE);
            capaZonasToque.setVisibility(View.GONE); // <-- Ocultar zonas táctiles
            contenedorExplorador.setVisibility(View.VISIBLE);
            mostrarBarrasSistema();
        }
        // CASO B: Si ya estábamos viendo el explorador, usamos tu sistema de carpetas con la pila
        else if (pilaDeRutas.size() > 1) {
            pilaDeRutas.pop(); // Sacar carpeta actual
            File carpetaPadre = pilaDeRutas.peek(); // Ver la carpeta anterior
            navegarACarpeta(carpetaPadre); // Redibujar lista
        } else {
            super.onBackPressed(); // Si ya está en la raíz de la SD/USB, cierra la app de forma nativa
        }
    }

    private void solicitarPermisoEspecial() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                    Toast.makeText(this, "Por favor, activa el acceso a archivos para escanear el USB", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        }
    }

    private boolean tieneSiguienteCarpetaGlobal() {
        if (carpetaReproduciendoActualmente == null) return false;
        File padre = carpetaReproduciendoActualmente.getParentFile();
        if (padre == null) return false;

        File[] hermanos = padre.listFiles(File::isDirectory);
        if (hermanos != null) {
            List<File> listaCarpetas = new ArrayList<>();
            for (File h : hermanos) listaCarpetas.add(h);
            Collections.sort(listaCarpetas, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

            int indiceActual = listaCarpetas.indexOf(carpetaReproduciendoActualmente);
            return (indiceActual != -1 && indiceActual + 1 < listaCarpetas.size());
        }
        return false;
    }

    private boolean tieneAnteriorCarpetaGlobal() {
        if (carpetaReproduciendoActualmente == null) return false;
        File padre = carpetaReproduciendoActualmente.getParentFile();
        if (padre == null) return false;

        File[] hermanos = padre.listFiles(File::isDirectory);
        if (hermanos != null) {
            List<File> listaCarpetas = new ArrayList<>();
            for (File h : hermanos) listaCarpetas.add(h);
            Collections.sort(listaCarpetas, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

            int indiceActual = listaCarpetas.indexOf(carpetaReproduciendoActualmente);
            return (indiceActual > 0); // Hay una carpeta antes de la actual
        }
        return false;
    }

    private void reproducirCarpetaAnteriorGlobal() {
        if (carpetaReproduciendoActualmente == null) return;
        File padre = carpetaReproduciendoActualmente.getParentFile();
        if (padre == null) return;

        File[] hermanos = padre.listFiles(File::isDirectory);
        if (hermanos != null) {
            List<File> listaCarpetas = new ArrayList<>();
            for (File h : hermanos) listaCarpetas.add(h);
            Collections.sort(listaCarpetas, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

            int indiceActual = listaCarpetas.indexOf(carpetaReproduciendoActualmente);
            if (indiceActual > 0) {
                File carpetaAnterior = listaCarpetas.get(indiceActual - 1);

                // 1. Actualizamos el explorador de archivos con la carpeta correcta
                navegarACarpeta(carpetaAnterior);

                // 2. Forzamos los estados de visibilidad de la interfaz de reproducción
                contenedorExplorador.setVisibility(View.GONE);
                playerView.setVisibility(View.VISIBLE);
                capaZonasToque.setVisibility(View.VISIBLE);

                // 3. Buscamos el ÚLTIMO video de esa carpeta (archivosEnCarpetaActual ya se actualizó en navegarACarpeta)
                File ultimoVideo = null;
                for (int i = archivosEnCarpetaActual.size() - 1; i >= 0; i--) {
                    File f = archivosEnCarpetaActual.get(i);
                    if (!f.isDirectory() && esVideo(f.getName())) {
                        ultimoVideo = f;
                        break;
                    }
                }

                // 4. Si encontramos un video, lo reproducimos
                if (ultimoVideo != null) {
                    reproducirVideosDeCarpeta(ultimoVideo, carpetaAnterior);
                }
            }
        }
    }

    private void mostrarNombreVideo(String nombre) {
        txtNombreVideo.setText(nombre);
        
        // Ajustar posición según configuración
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) txtNombreVideo.getLayoutParams();
        if (configTitleTop) {
            params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
            params.topMargin = (int) (20 * getResources().getDisplayMetrics().density);
            params.bottomMargin = 0;
        } else {
            params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
            params.bottomMargin = (int) (100 * getResources().getDisplayMetrics().density); // Un poco arriba de la barra de progreso
            params.topMargin = 0;
        }
        txtNombreVideo.setLayoutParams(params);
        
        txtNombreVideo.setVisibility(View.VISIBLE);

        // Oculta el nombre automáticamente después de 3 segundos
        txtNombreVideo.postDelayed(() -> txtNombreVideo.setVisibility(View.GONE), 3000);
    }

    private void mostrarFeedback(int resId) {
        if (resId != 0) {
            imgFeedback.setImageResource(resId);
            imgFeedback.setVisibility(View.VISIBLE);
        }
        layoutProgreso.setVisibility(View.VISIBLE);

        imgFeedback.removeCallbacks(null);
        layoutProgreso.removeCallbacks(null);

        Runnable ocultar = () -> {
            imgFeedback.setVisibility(View.GONE);
            layoutProgreso.setVisibility(View.GONE);
        };

        imgFeedback.postDelayed(ocultar, 3000);
    }

    private void iniciarHilosProgreso() {
        if (runnableProgreso != null) handlerProgreso.removeCallbacks(runnableProgreso);

        runnableProgreso = new Runnable() {
            @Override
            public void run() {
                if (exoPlayer != null && exoPlayer.getPlaybackState() != Player.STATE_IDLE) {
                    long currentPos = exoPlayer.getCurrentPosition();
                    long duration = exoPlayer.getDuration();

                    if (duration > 0) {
                        seekBarVideo.setMax((int) duration);
                        seekBarVideo.setProgress((int) currentPos);
                        txtTiempoActual.setText(formatearTiempo(currentPos));
                        txtTiempoRestante.setText("-" + formatearTiempo(duration - currentPos));
                    }
                }
                handlerProgreso.postDelayed(this, 1000);
            }
        };
        handlerProgreso.post(runnableProgreso);
    }

    private String formatearTiempo(long ms) {
        int totalSegundos = (int) (ms / 1000);
        int minutos = totalSegundos / 60;
        int segundos = totalSegundos % 60;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutos, segundos);
    }

    private void ocultarBarrasSistema() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // Para versiones anteriores a Android 11
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    private void mostrarBarrasSistema() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        } else {
            // Para versiones anteriores a Android 11
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && contenedorVideo != null && contenedorVideo.getVisibility() == View.VISIBLE) {
            ocultarBarrasSistema();
        }
    }

    private void mostrarDialogoConfiguracion() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputHeight = new EditText(this);
        inputHeight.setHint("Altura del item (dp) - Actual: " + configItemHeight);
        inputHeight.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputHeight);

        final EditText inputTextSize = new EditText(this);
        inputTextSize.setHint("Tamaño de letra (sp) - Actual: " + configTextSize);
        inputTextSize.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputTextSize);

        final EditText inputDefaultFolder = new EditText(this);
        inputDefaultFolder.setHint("Carpeta inicial - Actual: " + configDefaultFolder);
        layout.addView(inputDefaultFolder);

        final EditText inputSeekSeconds = new EditText(this);
        inputSeekSeconds.setHint("Segundos salto - Actual: " + configSeekSeconds);
        inputSeekSeconds.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputSeekSeconds);

        final android.widget.CheckBox checkTitleTop = new android.widget.CheckBox(this);
        checkTitleTop.setText("Título arriba (desmarcar para abajo)");
        checkTitleTop.setChecked(configTitleTop);
        layout.addView(checkTitleTop);

        new AlertDialog.Builder(this)
                .setTitle("Ajustar Apariencia")
                .setView(layout)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String h = inputHeight.getText().toString();
                    String t = inputTextSize.getText().toString();
                    String f = inputDefaultFolder.getText().toString();
                    String s = inputSeekSeconds.getText().toString();

                    if (!h.isEmpty()) configItemHeight = Integer.parseInt(h);
                    if (!t.isEmpty()) configTextSize = Integer.parseInt(t);
                    if (!f.isEmpty()) configDefaultFolder = f;
                    if (!s.isEmpty()) configSeekSeconds = Integer.parseInt(s);
                    configTitleTop = checkTitleTop.isChecked();

                    SharedPreferences.Editor editor = getSharedPreferences("config_repro", MODE_PRIVATE).edit();
                    editor.putInt("item_height", configItemHeight);
                    editor.putInt("text_size", configTextSize);
                    editor.putString("default_folder", configDefaultFolder);
                    editor.putInt("seek_seconds", configSeekSeconds);
                    editor.putBoolean("title_top", configTitleTop);
                    editor.apply();

                    // Refrescar la carpeta actual para aplicar cambios
                    if (!pilaDeRutas.isEmpty()) {
                        navegarACarpeta(pilaDeRutas.peek());
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }


    private void irAlVideoAnterior() {
        if (exoPlayer.hasPreviousMediaItem() && exoPlayer.getCurrentMediaItemIndex() > 0) {
            if (exoPlayer.getCurrentMediaItemIndex() == 1 && tieneAnteriorCarpetaGlobal()) {
                reproducirCarpetaAnteriorGlobal();
            } else {
                exoPlayer.seekToPreviousMediaItem();
            }
        } else {
            reproducirCarpetaAnteriorGlobal();
        }
    }

    private void irAlSiguienteVideo() {
        int indiceActual = exoPlayer.getCurrentMediaItemIndex();
        int totalItems = exoPlayer.getMediaItemCount();

        if (indiceActual == totalItems - 2 && tieneSiguienteCarpetaGlobal()) {
            reproducirSiguienteCarpetaGlobal();
        } else if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem();
        } else {
            reproducirSiguienteCarpetaGlobal();
        }
    }

    private void iniciarSeekLoop(long cantidadMs) {
        runnableSeek = new Runnable() {
            @Override
            public void run() {
                if (exoPlayer != null) {
                    long nuevaPosicion = exoPlayer.getCurrentPosition() + cantidadMs;
                    if (nuevaPosicion < 0) nuevaPosicion = 0;
                    if (nuevaPosicion > exoPlayer.getDuration()) nuevaPosicion = exoPlayer.getDuration();
                    exoPlayer.seekTo(nuevaPosicion);
                    handlerSeek.postDelayed(this, 1000);
                }
            }
        };
        handlerSeek.post(runnableSeek);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (runnableProgreso != null) handlerProgreso.removeCallbacks(runnableProgreso);
        if (exoPlayer != null) {
            exoPlayer.release();
        }
    }
    private void escanearYRefrescar() {
        // 1. Buscamos en /storage/ (donde Android monta los USB/SD externos)
        File storage = new File("/storage/");
        if (storage.exists() && storage.listFiles() != null) {
            for (File f : storage.listFiles()) {
                // El emulador usa un ID como '0000-0000', los radios suelen usar 'sda1' o 'usb'
                // Filtramos la memoria interna ('emulated') y la propia carpeta 'self'
                if (f.isDirectory() && !f.getName().equals("emulated") && !f.getName().equals("self")) {
                    
                    // INTENTO DE ENTRAR A LA CARPETA CONFIGURADA
                    if (configDefaultFolder != null && !configDefaultFolder.isEmpty()) {
                        File carpetaFav = new File(f, configDefaultFolder);
                        if (carpetaFav.exists() && carpetaFav.isDirectory()) {
                            navegarACarpeta(carpetaFav);
                            return;
                        }
                    }

                    navegarACarpeta(f);
                    return; // Si encontramos algo en /storage/ que no es la memoria interna, entramos ahí.
                }
            }
        }

        // 2. Si no encontró nada en /storage/, buscamos en /mnt/ (por si acaso)
        File mnt = new File("/mnt/");
        if (mnt.exists() && mnt.listFiles() != null) {
            for (File f : mnt.listFiles()) {
                if (f.isDirectory() && (f.getName().toLowerCase().contains("usb") || f.getName().toLowerCase().contains("sd"))) {
                    
                    if (configDefaultFolder != null && !configDefaultFolder.isEmpty()) {
                        File carpetaFav = new File(f, configDefaultFolder);
                        if (carpetaFav.exists() && carpetaFav.isDirectory()) {
                            navegarACarpeta(carpetaFav);
                            return;
                        }
                    }

                    navegarACarpeta(f);
                    return;
                }
            }
        }

        // 3. Último recurso: memoria interna
        File interna = Environment.getExternalStorageDirectory();
        if (configDefaultFolder != null && !configDefaultFolder.isEmpty()) {
            File carpetaFav = new File(interna, configDefaultFolder);
            if (carpetaFav.exists() && carpetaFav.isDirectory()) {
                navegarACarpeta(carpetaFav);
                return;
            }
        }
        navegarACarpeta(interna);
    }


}