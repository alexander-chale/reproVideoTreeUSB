package com.echaleleon.reprovideotreeusb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.app.AlertDialog;


import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
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
    private ProgressBar pbVolumen;
    private ProgressBar pbBrillo;
    private ImageButton btnShuffle;
    private boolean isShuffleMode = false;
    private boolean isUserHolding = false;
    private float startY;
    private int initialVolume;
    private float initialBrightness;
    private boolean isSwipingVolume = false;
    private boolean isSwipingBrightness = false;
    private final android.os.Handler handlerProgreso = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable runnableProgreso;
    private final android.os.Handler handlerUI = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable runnableOcultarUI = () -> {
        if (!isUserHolding) {
            if (imgFeedback != null) imgFeedback.setVisibility(View.GONE);
            if (layoutProgreso != null) layoutProgreso.setVisibility(View.GONE);
        }
    };

    // NUEVAS VARIABLES:

    private LinearLayout contenedorExplorador;
    private LinearLayout layoutBarraInferior;

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
    private boolean configNightMode = true;
    private boolean configPilotMode = false;
    private boolean configHideExtension = false;
    
    private ImageButton btnInicio;
    private ImageButton btnScreenOff;
    private RelativeLayout capaPantallaApagada;
    private TextView txtMusicTitle;
    private ImageView imgAlbumArt;
    private boolean isScreenOffMode = false;
    private long tiempoPrimerClickAtras = 0;

    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            if (exoPlayer != null) exoPlayer.pause();
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            if (exoPlayer != null) {
                exoPlayer.setVolume(1.0f);
                exoPlayer.play();
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            if (exoPlayer != null) exoPlayer.pause();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            if (exoPlayer != null) exoPlayer.setVolume(0.2f);
        }
    };

    private ImageButton btnConfiguracion;


    @androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Mantener la pantalla encendida siempre que la app esté en primer plano
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        // 1. Inicializar todas las vistas (NUEVO ORDEN Y VISTAS)
        txtRutaActual = findViewById(R.id.txtRutaActual);
        listViewArchivos = findViewById(R.id.listViewArchivos);
        contenedorExplorador = findViewById(R.id.contenedorExplorador);
        layoutBarraInferior = findViewById(R.id.layoutBarraInferior);
        btnConfiguracion = findViewById(R.id.btnConfiguracion);
        btnInicio = findViewById(R.id.btnInicio);
        btnScreenOff = findViewById(R.id.btnScreenOff);
        capaPantallaApagada = findViewById(R.id.capaPantallaApagada);
        txtMusicTitle = findViewById(R.id.txtMusicTitle);
        imgAlbumArt = findViewById(R.id.imgAlbumArt);

        SharedPreferences prefs = getSharedPreferences("config_repro", MODE_PRIVATE);
        configItemHeight = prefs.getInt("item_height", 100);
        configTextSize = prefs.getInt("text_size", 32);
        configDefaultFolder = prefs.getString("default_folder", "Videos Musicales");
        configTitleTop = prefs.getBoolean("title_top", true);
        configSeekSeconds = prefs.getInt("seek_seconds", 10);
        configNightMode = prefs.getBoolean("night_mode", true);
        configPilotMode = prefs.getBoolean("pilot_mode", false);
        configHideExtension = prefs.getBoolean("hide_extension", false);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        aplicarTemaFondo();

        btnConfiguracion.setOnClickListener(v -> mostrarDialogoConfiguracion());
        btnInicio.setOnClickListener(v -> {
            escanearYRefrescar();
            Toast.makeText(this, "Volviendo a carpeta inicial", Toast.LENGTH_SHORT).show();
        });

        btnScreenOff.setOnClickListener(v -> {
            isScreenOffMode = !isScreenOffMode;
            capaPantallaApagada.setVisibility(isScreenOffMode ? View.VISIBLE : View.GONE);
            btnScreenOff.setColorFilter(isScreenOffMode ? android.graphics.Color.parseColor("#BB86FC") : 
                (configNightMode ? android.graphics.Color.WHITE : android.graphics.Color.BLACK));
            
            if (isScreenOffMode) {
                iniciarAnimacionBarras();
                Toast.makeText(this, "Modo Audio Activado", Toast.LENGTH_SHORT).show();
            }
        });

        // VINCULAR LAS NUEVAS VISTAS DEL VIDEO
        contenedorVideo = findViewById(R.id.contenedorVideo); // <-- ¡OBLIGATORIO!
        playerView = findViewById(R.id.playerView);
        txtNombreVideo = findViewById(R.id.txtNombreVideo);   // <-- ¡OBLIGATORIO!

        layoutProgreso = findViewById(R.id.layoutProgreso);
        seekBarVideo = findViewById(R.id.seekBarVideo);
        txtTiempoActual = findViewById(R.id.txtTiempoActual);
        txtTiempoRestante = findViewById(R.id.txtTiempoRestante);
        imgFeedback = findViewById(R.id.imgFeedback);
        pbVolumen = findViewById(R.id.pbVolumen);
        pbBrillo = findViewById(R.id.pbBrillo);
        btnShuffle = findViewById(R.id.btnShuffle);

        capaZonasToque = findViewById(R.id.capaZonasToque);
        zonaIzquierda = findViewById(R.id.zonaIzquierda);
        zonaCentro = findViewById(R.id.zonaCentro);
        zonaDerecha = findViewById(R.id.zonaDerecha);

        // 2. Configurar Clics de las zonas táctiles sobre el video
        // ZONA IZQUIERDA: Video anterior O rebobinar si se mantiene presionado O VOLUMEN (Deslizar)
        zonaIzquierda.setOnTouchListener((v, event) -> {
            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isUserHolding = true;
                isSwipingVolume = false;
                startY = event.getY();
                initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                
                pressStartTime = System.currentTimeMillis();
                mostrarFeedback(0, android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                
                handlerSeek.postDelayed(() -> {
                    if (isUserHolding && !isSwipingVolume) {
                        int skipDirection = configPilotMode ? 1 : -1;
                        int feedbackIcon = configPilotMode ? android.R.drawable.ic_media_ff : android.R.drawable.ic_media_rew;
                        
                        iniciarSeekLoop(skipDirection * configSeekSeconds * 1000);
                        mostrarFeedback(feedbackIcon, android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                    }
                }, 3000);
                return true;

            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float deltaY = startY - event.getY();
                if (Math.abs(deltaY) > 30) {
                    isSwipingVolume = true;
                    handlerSeek.removeCallbacksAndMessages(null);

                    int volumeChange = (int) (deltaY / (v.getHeight() / maxVol));
                    int newVol = initialVolume + volumeChange;
                    if (newVol < 0) newVol = 0;
                    if (newVol > maxVol) newVol = maxVol;

                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0);

                    pbVolumen.setVisibility(View.VISIBLE);
                    pbVolumen.setMax(maxVol);
                    pbVolumen.setProgress(newVol);
                    
                    pbVolumen.removeCallbacks(null);
                    pbVolumen.postDelayed(() -> pbVolumen.setVisibility(View.GONE), 2000);
                }
                return true;

            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                isUserHolding = false;
                handlerSeek.removeCallbacksAndMessages(null);
                
                if (!isSwipingVolume) {
                    if (System.currentTimeMillis() - pressStartTime < 3000) {
                        if (configPilotMode) {
                            irAlSiguienteVideo();
                            mostrarFeedback(android.R.drawable.ic_media_next, android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                        } else {
                            irAlVideoAnterior();
                            mostrarFeedback(android.R.drawable.ic_media_previous, android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                        }
                    } else {
                        mostrarFeedback(0, android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL); 
                    }
                }
                isSwipingVolume = false;
                return true;
            }
            return true;
        });

        // ZONA CENTRO: Alternar entre Reproducir (Play) y Pausar (Pause)
        zonaCentro.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isUserHolding = true;
                pressStartTime = System.currentTimeMillis();
                mostrarFeedback(0, android.view.Gravity.CENTER);

                handlerSeek.postDelayed(() -> {
                    if (isUserHolding) {
                        exoPlayer.stop();
                        escanearYRefrescar();
                        Toast.makeText(this, "Regresando al inicio...", Toast.LENGTH_SHORT).show();
                    }
                }, 3000);
                
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                isUserHolding = false;
                handlerSeek.removeCallbacksAndMessages(null);
                if (System.currentTimeMillis() - pressStartTime < 3000) {
                    if (exoPlayer.isPlaying()) {
                        exoPlayer.pause();
                        mostrarFeedback(android.R.drawable.ic_media_pause, android.view.Gravity.CENTER);
                    } else {
                        exoPlayer.play();
                        mostrarFeedback(android.R.drawable.ic_media_play, android.view.Gravity.CENTER);
                    }
                } else {
                    mostrarFeedback(0, android.view.Gravity.CENTER);
                }
                return true;
            }
            return true;
        });

        // ZONA DERECHA: Siguiente video O adelantar si se mantiene presionado O BRILLO (Deslizar)
        zonaDerecha.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isUserHolding = true;
                isSwipingBrightness = false;
                startY = event.getY();
                
                float currentBrightness = getWindow().getAttributes().screenBrightness;
                if (currentBrightness < 0) currentBrightness = 0.5f;
                initialBrightness = currentBrightness;

                pressStartTime = System.currentTimeMillis();
                mostrarFeedback(0, android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);

                handlerSeek.postDelayed(() -> {
                    if (isUserHolding && !isSwipingBrightness) {
                        int skipDirection = configPilotMode ? -1 : 1;
                        int feedbackIcon = configPilotMode ? android.R.drawable.ic_media_rew : android.R.drawable.ic_media_ff;
                        
                        iniciarSeekLoop(skipDirection * configSeekSeconds * 1000);
                        mostrarFeedback(feedbackIcon, android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
                    }
                }, 3000);
                return true;

            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float deltaY = startY - event.getY();
                if (Math.abs(deltaY) > 30) {
                    isSwipingBrightness = true;
                    handlerSeek.removeCallbacksAndMessages(null);

                    float brightnessChange = deltaY / v.getHeight();
                    float newBrightness = initialBrightness + brightnessChange;
                    if (newBrightness < 0.01f) newBrightness = 0.01f;
                    if (newBrightness > 1.0f) newBrightness = 1.0f;

                    android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.screenBrightness = newBrightness;
                    getWindow().setAttributes(lp);

                    pbBrillo.setVisibility(View.VISIBLE);
                    pbBrillo.setMax(100);
                    pbBrillo.setProgress((int) (newBrightness * 100));
                    
                    pbBrillo.removeCallbacks(null);
                    pbBrillo.postDelayed(() -> pbBrillo.setVisibility(View.GONE), 2000);
                }
                return true;

            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                isUserHolding = false;
                handlerSeek.removeCallbacksAndMessages(null);
                
                if (!isSwipingBrightness) {
                    if (System.currentTimeMillis() - pressStartTime < 3000) {
                        if (configPilotMode) {
                            irAlVideoAnterior();
                            mostrarFeedback(android.R.drawable.ic_media_previous, android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
                        } else {
                            irAlSiguienteVideo();
                            mostrarFeedback(android.R.drawable.ic_media_next, android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
                        }
                    } else {
                        mostrarFeedback(0, android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
                    }
                }
                isSwipingBrightness = false;
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
                    
                    // Actualizar tiempo restante en tiempo real mientras desliza
                    long duration = exoPlayer.getDuration();
                    if (duration > 0) {
                        txtTiempoRestante.setText("-" + formatearTiempo(duration - progress));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserHolding = true;
                handlerUI.removeCallbacks(runnableOcultarUI);
                handlerProgreso.removeCallbacks(runnableProgreso);
                layoutProgreso.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserHolding = false;
                iniciarHilosProgreso();
                // Al soltar, programamos el ocultamiento
                handlerUI.postDelayed(runnableOcultarUI, 3000);
            }
        });

        btnShuffle.setOnClickListener(v -> {
            isShuffleMode = !isShuffleMode;
            btnShuffle.setColorFilter(isShuffleMode ? android.graphics.Color.parseColor("#BB86FC") : android.graphics.Color.parseColor("#888888"));
            Toast.makeText(this, isShuffleMode ? "Modo Aleatorio Activo" : "Modo Ordenado", Toast.LENGTH_SHORT).show();
            
            // Si el usuario cambia a aleatorio mientras ve un video, re-mezclamos la lista actual
            if (contenedorVideo.getVisibility() == View.VISIBLE && !videosEnReproduccionActual.isEmpty()) {
                long currentPos = exoPlayer.getCurrentPosition();
                int currentIndex = exoPlayer.getCurrentMediaItemIndex();
                // Ajuste por dummy
                int realIdx = currentIndex - (tieneAnteriorCarpetaGlobal() ? 1 : 0);
                if (realIdx >= 0 && realIdx < videosEnReproduccionActual.size()) {
                    File videoActual = videosEnReproduccionActual.get(realIdx);
                    reproducirVideosDeCarpeta(videoActual, carpetaReproduciendoActualmente);
                    exoPlayer.seekTo(exoPlayer.getCurrentMediaItemIndex(), currentPos);
                }
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
        exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC); // Búsqueda de cuadros fluida
        playerView.setPlayer(exoPlayer);

        exoPlayer.addListener(new Player.Listener() {

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                // 1. Verificación de seguridad: si es nulo, no hacemos nada
                if (mediaItem == null) return;

                // 2. Verificación de configuración local
                if (mediaItem.localConfiguration != null) {
                    String uriStr = mediaItem.localConfiguration.uri.toString();

                    // Solo mostramos si NO es uno de los archivos dummy
                    if (!uriStr.contains("dummy")) {
                        try {
                            String nombreArchivo = Uri.decode(new File(uriStr).getName());
                            
                            // LOGICA DE CAMBIO AUTO AUDIO/VIDEO
                            boolean isAudio = isAudioFile(uriStr);
                            if (isAudio) {
                                capaPantallaApagada.setVisibility(View.VISIBLE);
                                iniciarAnimacionBarras();
                                cargarMetadatosAudio(new File(Uri.parse(uriStr).getPath()));
                            } else {
                                // Si es video, resetear carátula a predeterminada por si está activa la capa
                                if (imgAlbumArt != null) {
                                    imgAlbumArt.setImageResource(R.drawable.ic_minimal_player); // O ic_home_custom
                                    imgAlbumArt.setPadding(40, 40, 40, 40);
                                    imgAlbumArt.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                                }
                                
                                // Si es video, volver a video a menos que el modo manual esté activo
                                capaPantallaApagada.setVisibility(isScreenOffMode ? View.VISIBLE : View.GONE);
                                if (!isScreenOffMode) {
                                    mostrarNombreVideo(nombreArchivo);
                                } else {
                                    iniciarAnimacionBarras();
                                }
                            }
                            
                            mostrarFeedback(0, android.view.Gravity.CENTER);
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
                    solicitarAudioFocus();
                }
            }

            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                Toast.makeText(MainActivity.this, "Formato no compatible, saltando...", Toast.LENGTH_SHORT).show();
                error.printStackTrace();
                // Saltar automáticamente al siguiente video
                irAlSiguienteVideo();
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

    private void iniciarAnimacionBarras() {
        if (capaPantallaApagada.getVisibility() == View.GONE) return;

        View[] bars = {
            findViewById(R.id.bar1), findViewById(R.id.bar2),
            findViewById(R.id.bar3), findViewById(R.id.bar4),
            findViewById(R.id.bar5), findViewById(R.id.bar6),
            findViewById(R.id.bar7)
        };

        android.os.Handler h = new android.os.Handler();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                // Si la capa se ocultó o no hay reproducción, paramos de animar agresivamente
                if (capaPantallaApagada.getVisibility() == View.GONE || contenedorVideo.getVisibility() == View.GONE) return;

                if (!exoPlayer.isPlaying()) {
                    h.postDelayed(this, 500); // Reintentar lento si está pausado
                    return;
                }
                
                java.util.Random rnd = new java.util.Random();
                for (View b : bars) {
                    if (b != null) {
                        int height = rnd.nextInt(50) + 10; // altura aleatoria entre 10 y 60 dp
                        android.view.ViewGroup.LayoutParams lp = b.getLayoutParams();
                        lp.height = (int) android.util.TypedValue.applyDimension(
                            android.util.TypedValue.COMPLEX_UNIT_DIP, height, getResources().getDisplayMetrics());
                        b.setLayoutParams(lp);
                    }
                }
                h.postDelayed(this, 100); // Más rápido para que parezca fluido
            }
        };
        h.post(r);
    }

    private void navegarACarpeta(File nuevaCarpeta) {
        if (nuevaCarpeta == null || !nuevaCarpeta.exists()) return;

        carpetaReproduciendoActualmente = nuevaCarpeta;
        txtRutaActual.setText("Ruta: " + nuevaCarpeta.getAbsolutePath());
        
        // Aplicar colores de tema según modo actual
        int textColor = configNightMode ? android.graphics.Color.WHITE : android.graphics.Color.BLACK;
        txtRutaActual.setTextColor(textColor);
        
        if (btnConfiguracion != null) btnConfiguracion.setColorFilter(textColor);
        if (btnInicio != null) btnInicio.setColorFilter(textColor);
        if (btnScreenOff != null) {
            btnScreenOff.setColorFilter(isScreenOffMode ? android.graphics.Color.parseColor("#BB86FC") : textColor);
        }

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
            if (f.isDirectory()) {
                nombresParaMostrar.add("📁 " + f.getName());
            } else if (isAudioFile(f.getName())) {
                nombresParaMostrar.add("🎵 " + getFileNameToDisplay(f.getName()));
            } else {
                nombresParaMostrar.add("🎬 " + getFileNameToDisplay(f.getName()));
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.item_carpeta, R.id.txtNombreCarpeta, nombresParaMostrar) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);

                TextView text = view.findViewById(R.id.txtNombreCarpeta);
                int textColor = configNightMode ? android.graphics.Color.WHITE : android.graphics.Color.BLACK;
                text.setTextColor(textColor);

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
        return n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".avi") || n.endsWith(".webm") || n.endsWith(".mp3");
    }

    private boolean isAudioFile(String path) {
        if (path == null) return false;
        return path.toLowerCase().endsWith(".mp3");
    }

    private String getFileNameToDisplay(String nombre) {
        if (configHideExtension) {
            int lastDot = nombre.lastIndexOf('.');
            if (lastDot > 0) {
                return nombre.substring(0, lastDot);
            }
        }
        return nombre;
    }

    private void reproducirVideosDeCarpeta(File videoInicial, File carpeta) {
        boolean isAudio = isAudioFile(videoInicial.getAbsolutePath());
        
        // Auto-activar capa de música si es MP3
        if (isAudio) {
            capaPantallaApagada.setVisibility(View.VISIBLE);
            iniciarAnimacionBarras();
            cargarMetadatosAudio(videoInicial);
        } else {
            // Resetear carátula para videos
            if (imgAlbumArt != null) {
                imgAlbumArt.setImageResource(R.drawable.ic_minimal_player);
                imgAlbumArt.setPadding(40, 40, 40, 40);
                imgAlbumArt.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }

            // Si es video, solo mostrar capa si el botón manual está activo
            capaPantallaApagada.setVisibility(isScreenOffMode ? View.VISIBLE : View.GONE);
            if (isScreenOffMode) iniciarAnimacionBarras();
        }

        mostrarNombreVideo(videoInicial.getName());

        // Cambio visual de pantallas
        contenedorExplorador.setVisibility(View.GONE);
        contenedorVideo.setVisibility(View.VISIBLE);
        capaZonasToque.setVisibility(View.VISIBLE);
        ocultarBarrasSistema();
        mostrarFeedback(0, android.view.Gravity.CENTER);

        exoPlayer.stop();
        exoPlayer.clearMediaItems();

        videosEnReproduccionActual.clear();
        File[] archivos = carpeta.listFiles();
        if (archivos != null) {
            for (File f : archivos) {
                if (f.isFile() && esVideo(f.getName())) {
                    videosEnReproduccionActual.add(f);
                }
            }
        }

        // --- LOGICA DE ORDEN / ALEATORIO ---
        if (isShuffleMode) {
            // Mezclar la lista
            Collections.shuffle(videosEnReproduccionActual);
            // Pero asegurar que el video que seleccionamos sea el PRIMERO en reproducirse
            videosEnReproduccionActual.remove(videoInicial);
            videosEnReproduccionActual.add(0, videoInicial);
        } else {
            // Orden alfabético normal
            Collections.sort(videosEnReproduccionActual, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        }

        if (videosEnReproduccionActual.isEmpty()) return;

        int indiceReal = 0;
        if (!isShuffleMode) {
            for (int i = 0; i < videosEnReproduccionActual.size(); i++) {
                if (videosEnReproduccionActual.get(i).getAbsolutePath().equals(videoInicial.getAbsolutePath())) {
                    indiceReal = i;
                    break;
                }
            }
        }

        // Inyectar Dummies y cargar ExoPlayer
        if (tieneAnteriorCarpetaGlobal()) {
            exoPlayer.addMediaItem(MediaItem.fromUri(Uri.parse("asset:///dummy_prev.mp4")));
            indiceReal++;
        }

        for (File v : videosEnReproduccionActual) {
            exoPlayer.addMediaItem(MediaItem.fromUri(Uri.fromFile(v)));
        }

        if (tieneSiguienteCarpetaGlobal()) {
            exoPlayer.addMediaItem(MediaItem.fromUri(Uri.parse("asset:///dummy_next.mp4")));
        }

        exoPlayer.seekTo(indiceReal, 0);
        
        // Resume playback logic...
        SharedPreferences resPrefs = getSharedPreferences("resume_prefs", MODE_PRIVATE);
        if (resPrefs.getString("last_video_path", "").equals(videoInicial.getAbsolutePath())) {
            long pos = resPrefs.getLong("last_video_pos", 0);
            if (pos > 0) exoPlayer.seekTo(indiceReal, pos);
        }

        exoPlayer.prepare();
        exoPlayer.play();
    }


    private void cargarMetadatosAudio(File file) {
        if (txtMusicTitle == null || imgAlbumArt == null) return;
        
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            
            if (title != null) {
                txtMusicTitle.setText(title + (artist != null ? " - " + artist : ""));
            } else {
                txtMusicTitle.setText(getFileNameToDisplay(file.getName()));
            }

            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(art, 0, art.length);
                imgAlbumArt.setImageBitmap(bitmap);
                imgAlbumArt.setPadding(0, 0, 0, 0);
                imgAlbumArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                imgAlbumArt.setImageResource(R.drawable.ic_home_custom);
                imgAlbumArt.setPadding(40, 40, 40, 40);
                imgAlbumArt.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
        } catch (Exception e) {
            txtMusicTitle.setText(getFileNameToDisplay(file.getName()));
            imgAlbumArt.setImageResource(R.drawable.ic_home_custom);
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
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
            File siguienteCarpeta = null;

            if (indiceActual != -1 && indiceActual + 1 < listaCarpetas.size()) {
                // Hay una carpeta siguiente normal
                siguienteCarpeta = listaCarpetas.get(indiceActual + 1);
            } else {
                // Hemos llegado al final de todas las carpetas
                Toast.makeText(this, "Has llegado al final. Volviendo a la primera carpeta.", Toast.LENGTH_LONG).show();
                if (!listaCarpetas.isEmpty()) {
                    siguienteCarpeta = listaCarpetas.get(0); // Volver a la primera
                }
            }

            if (siguienteCarpeta != null) {
                // Forzar el cambio visual del explorador a esa carpeta tras bambalinas
                navegarACarpeta(siguienteCarpeta);

                contenedorExplorador.setVisibility(View.GONE);
                playerView.setVisibility(View.VISIBLE);
                capaZonasToque.setVisibility(View.VISIBLE);

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
            capaZonasToque.setVisibility(View.GONE);
            contenedorExplorador.setVisibility(View.VISIBLE);
            listViewArchivos.setVisibility(View.VISIBLE);
            txtRutaActual.setVisibility(View.VISIBLE);
            mostrarBarrasSistema();
        }
        // CASO B: Navegación jerárquica (Subir de nivel)
        else {
            File actual = carpetaReproduciendoActualmente;
            File interna = Environment.getExternalStorageDirectory();
            
            if (actual != null && !actual.getAbsolutePath().equals(interna.getAbsolutePath())) {
                File padre = actual.getParentFile();
                
                // Si el padre es inaccesible o es la carpeta /storage (lista de discos), saltamos a la interna
                if (padre == null || padre.getAbsolutePath().equals("/storage") || padre.getAbsolutePath().equals("/storage/emulated")) {
                    navegarACarpeta(interna);
                } else {
                    navegarACarpeta(padre);
                }
            } else {
                // CASO C: Prevenir salida accidental en la raíz
                if (tiempoPrimerClickAtras + 2000 > System.currentTimeMillis()) {
                    super.onBackPressed();
                } else {
                    Toast.makeText(this, "Presiona atrás de nuevo para salir", Toast.LENGTH_SHORT).show();
                    tiempoPrimerClickAtras = System.currentTimeMillis();
                }
            }
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
        if (txtNombreVideo == null) return;
        
        String nombreLimpio = getFileNameToDisplay(nombre);
        txtNombreVideo.setText(nombreLimpio);
        if (txtMusicTitle != null) txtMusicTitle.setText(nombreLimpio);
        
        // Ajustar posición según configuración
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) txtNombreVideo.getLayoutParams();
        if (configTitleTop) {
            params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
            params.topMargin = (int) (20 * getResources().getDisplayMetrics().density);
            params.bottomMargin = 0;
        } else {
            params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
            params.bottomMargin = (int) (120 * getResources().getDisplayMetrics().density); // Un poco más arriba de la barra
            params.topMargin = 0;
        }
        txtNombreVideo.setLayoutParams(params);
        
        // Asegurar visibilidad del contenedor para la previsualización
        if (contenedorVideo != null) {
            contenedorVideo.setVisibility(View.VISIBLE);
        }
        txtNombreVideo.setVisibility(View.VISIBLE);

        // Cancelar ocultamientos previos para evitar parpadeos
        txtNombreVideo.removeCallbacks(null);

        // Oculta el nombre automáticamente después de 3 segundos
        txtNombreVideo.postDelayed(() -> {
            txtNombreVideo.setVisibility(View.GONE);
            // Si el reproductor no estaba activo, volvemos a ocultar el contenedor
            if (exoPlayer != null && !exoPlayer.isPlaying() && contenedorExplorador != null && contenedorExplorador.getVisibility() == View.VISIBLE) {
                if (contenedorVideo != null) contenedorVideo.setVisibility(View.GONE);
            }
        }, 3000);
    }

    private void mostrarFeedback(int resId, int gravity) {
        if (resId != 0) {
            imgFeedback.setImageResource(resId);
            
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) imgFeedback.getLayoutParams();
            lp.gravity = gravity;
            imgFeedback.setLayoutParams(lp);
            
            imgFeedback.setVisibility(View.VISIBLE);
        }
        layoutProgreso.setVisibility(View.VISIBLE);

        // Cancelar cualquier ocultamiento programado previo
        handlerUI.removeCallbacks(runnableOcultarUI);

        // SOLO programar el ocultamiento si el usuario NO está presionando la pantalla
        if (!isUserHolding) {
            handlerUI.postDelayed(runnableOcultarUI, 3000);
        }
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
                        
                        // GUARDAR POSICION PARA RESUME (Cada 5 segundos para no saturar memoria)
                        if (currentPos % 5000 < 1000) {
                            int index = exoPlayer.getCurrentMediaItemIndex();
                            if (videosEnReproduccionActual != null && !videosEnReproduccionActual.isEmpty()) {
                                // Ajustar indice por los dummies
                                int realIndex = index - (tieneAnteriorCarpetaGlobal() ? 1 : 0);
                                if (realIndex >= 0 && realIndex < videosEnReproduccionActual.size()) {
                                    File currentFile = videosEnReproduccionActual.get(realIndex);
                                    SharedPreferences.Editor editor = getSharedPreferences("resume_prefs", MODE_PRIVATE).edit();
                                    editor.putString("last_video_path", currentFile.getAbsolutePath());
                                    editor.putLong("last_video_pos", currentPos);
                                    editor.apply();
                                }
                            }
                        }
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
        View view = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        
        // Guardar valores originales para restaurar si cancela
        final int oldHeight = configItemHeight;
        final int oldTextSize = configTextSize;
        final boolean oldNightMode = configNightMode;
        final boolean oldTitleTop = configTitleTop;
        final boolean oldPilotMode = configPilotMode;
        final boolean oldHideExtension = configHideExtension;

        TextInputEditText editFolder = view.findViewById(R.id.editDefaultFolder);
        SeekBar seekBarSkip = view.findViewById(R.id.seekBarSkipTime);
        TextView txtSkip = view.findViewById(R.id.txtValueSeek);
        SwitchMaterial switchTitle = view.findViewById(R.id.switchTitlePosition);
        SeekBar seekBarText = view.findViewById(R.id.seekBarTextSize);
        SeekBar seekBarHeight = view.findViewById(R.id.seekBarItemHeight);
        
        SwitchMaterial switchNight = view.findViewById(R.id.switchNightMode);
        SwitchMaterial switchPilot = view.findViewById(R.id.switchPilotMode);
        SwitchMaterial switchHideExt = view.findViewById(R.id.switchHideExtension);

        editFolder.setText(configDefaultFolder);
        seekBarSkip.setProgress(configSeekSeconds);
        txtSkip.setText(configSeekSeconds + " segundos");
        switchTitle.setChecked(configTitleTop);
        seekBarText.setProgress(configTextSize);
        seekBarHeight.setProgress(configItemHeight);
        switchNight.setChecked(configNightMode);
        switchPilot.setChecked(configPilotMode);
        switchHideExt.setChecked(configHideExtension);

        seekBarSkip.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) progress = 1;
                txtSkip.setText(progress + " segundos");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarText.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                configTextSize = progress;
                if (carpetaReproduciendoActualmente != null) navegarACarpeta(carpetaReproduciendoActualmente);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                configItemHeight = progress;
                if (carpetaReproduciendoActualmente != null) navegarACarpeta(carpetaReproduciendoActualmente);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        switchNight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            configNightMode = isChecked;
            aplicarTemaFondo();
            actualizarColoresDialogo(view);
            if (carpetaReproduciendoActualmente != null) navegarACarpeta(carpetaReproduciendoActualmente);
        });

        switchTitle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            configTitleTop = isChecked;
            // Forzar visualización del título para previsualización
            mostrarNombreVideo("Ejemplo de Posición");
        });

        switchPilot.setOnCheckedChangeListener((buttonView, isChecked) -> {
            configPilotMode = isChecked;
        });

        switchHideExt.setOnCheckedChangeListener((buttonView, isChecked) -> {
            configHideExtension = isChecked;
            if (carpetaReproduciendoActualmente != null) navegarACarpeta(carpetaReproduciendoActualmente);
        });

        actualizarColoresDialogo(view);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Configuración")
                .setView(view)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String f = editFolder.getText().toString();
                    configDefaultFolder = f.isEmpty() ? "Videos Musicales" : f;
                    configSeekSeconds = seekBarSkip.getProgress();
                    
                    SharedPreferences.Editor editor = getSharedPreferences("config_repro", MODE_PRIVATE).edit();
                    editor.putInt("item_height", configItemHeight);
                    editor.putInt("text_size", configTextSize);
                    editor.putString("default_folder", configDefaultFolder);
                    editor.putInt("seek_seconds", configSeekSeconds);
                    editor.putBoolean("title_top", configTitleTop);
                    editor.putBoolean("night_mode", configNightMode);
                    editor.putBoolean("pilot_mode", configPilotMode);
                    editor.putBoolean("hide_extension", configHideExtension);
                    editor.apply();

                    Toast.makeText(this, "Ajustes guardados", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    // Restaurar valores originales
                    configItemHeight = oldHeight;
                    configTextSize = oldTextSize;
                    configNightMode = oldNightMode;
                    configTitleTop = oldTitleTop;
                    configPilotMode = oldPilotMode;
                    configHideExtension = oldHideExtension;
                    
                    aplicarTemaFondo();
                    if (carpetaReproduciendoActualmente != null) navegarACarpeta(carpetaReproduciendoActualmente);
                    if (contenedorVideo.getVisibility() == View.VISIBLE) {
                        mostrarNombreVideo(txtNombreVideo.getText().toString());
                    }
                })
                .show();
    }

    private void aplicarTemaFondo() {
        View root = findViewById(android.R.id.content);
        int bgColor = configNightMode ? android.graphics.Color.parseColor("#121212") : android.graphics.Color.parseColor("#F5F5F5");
        int barColor = configNightMode ? android.graphics.Color.parseColor("#1A1A1A") : android.graphics.Color.WHITE;
        int textColor = configNightMode ? android.graphics.Color.WHITE : android.graphics.Color.BLACK;

        root.setBackgroundColor(bgColor);
        
        if (contenedorExplorador != null) {
            contenedorExplorador.setBackgroundColor(bgColor);
        }

        if (layoutBarraInferior != null) {
            layoutBarraInferior.setBackgroundColor(barColor);
        }

        if (txtRutaActual != null) {
            txtRutaActual.setTextColor(textColor);
        }

        if (btnConfiguracion != null) btnConfiguracion.setColorFilter(textColor);
        if (btnInicio != null) btnInicio.setColorFilter(textColor);
    }

    private void actualizarColoresDialogo(View dialogView) {
        if (dialogView == null) return;
        
        int bgColor = configNightMode ? android.graphics.Color.parseColor("#1A1A1A") : android.graphics.Color.WHITE;
        int textColor = configNightMode ? android.graphics.Color.WHITE : android.graphics.Color.BLACK;
        int subTitleColor = configNightMode ? android.graphics.Color.parseColor("#888888") : android.graphics.Color.parseColor("#666666");

        dialogView.setBackgroundColor(bgColor);
        actualizarRecursivo(dialogView, textColor, subTitleColor);
    }

    private void actualizarRecursivo(View view, int textColor, int subColor) {
        // Soporte especial para TextInputLayout (bordes y etiquetas)
        if (view instanceof TextInputLayout) {
            TextInputLayout til = (TextInputLayout) view;
            til.setDefaultHintTextColor(android.content.res.ColorStateList.valueOf(subColor));
            til.setHintTextColor(android.content.res.ColorStateList.valueOf(textColor));
            til.setBoxStrokeColor(textColor);
        }

        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                actualizarRecursivo(group.getChildAt(i), textColor, subColor);
            }
        }
        
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            String text = tv.getText().toString();
            if (text.equals("GENERAL") || text.equals("APARIENCIA")) {
                tv.setTextColor(subColor);
            } else {
                tv.setTextColor(textColor);
            }
            
            if (view instanceof EditText) {
                ((EditText) view).setHintTextColor(subColor);
            }
        } else if (view instanceof android.widget.SeekBar) {
            android.widget.SeekBar sb = (android.widget.SeekBar) view;
            sb.getThumb().setTint(textColor);
            sb.getProgressDrawable().setTint(textColor);
        }
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
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();

        if (action == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_HEADSETHOOK:
                    if (exoPlayer != null) {
                        if (exoPlayer.isPlaying()) {
                            exoPlayer.pause();
                            mostrarFeedback(android.R.drawable.ic_media_pause, android.view.Gravity.CENTER);
                        } else {
                            exoPlayer.play();
                            mostrarFeedback(android.R.drawable.ic_media_play, android.view.Gravity.CENTER);
                        }
                    }
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    if (exoPlayer != null) {
                        exoPlayer.play();
                        mostrarFeedback(android.R.drawable.ic_media_play, android.view.Gravity.CENTER);
                    }
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    if (exoPlayer != null) {
                        exoPlayer.pause();
                        mostrarFeedback(android.R.drawable.ic_media_pause, android.view.Gravity.CENTER);
                    }
                    return true;

                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    irAlSiguienteVideo();
                    mostrarFeedback(android.R.drawable.ic_media_next, android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    irAlVideoAnterior();
                    mostrarFeedback(android.R.drawable.ic_media_previous, android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                    return true;
                    
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    if (exoPlayer != null) {
                        exoPlayer.stop();
                        mostrarFeedback(android.R.drawable.ic_media_pause, android.view.Gravity.CENTER);
                    }
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Evita que la UI se rompa al rotar, manteniendo el modo inmersivo si el video está activo
        if (contenedorVideo.getVisibility() == View.VISIBLE) {
            ocultarBarrasSistema();
        }
    }

    private void solicitarAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build();
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void escanearYRefrescar() {
        // 1. Localizar memoria interna
        File interna = Environment.getExternalStorageDirectory();

        // 2. Intentar localizar USB en /storage/
        File storage = new File("/storage/");
        if (storage.exists() && storage.listFiles() != null) {
            for (File f : storage.listFiles()) {
                if (f.isDirectory() && !f.getName().equals("emulated") && !f.getName().equals("self")) {
                    
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

        // 3. Si no hay USB, buscamos favorita en interna
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