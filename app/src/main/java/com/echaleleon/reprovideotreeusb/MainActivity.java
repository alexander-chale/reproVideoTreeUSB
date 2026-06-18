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

public class MainActivity extends AppCompatActivity {

    private TextView txtRutaActual;
    private ListView listViewArchivos;
    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private FrameLayout contenedorVideo;

    private TextView txtNombreVideo;
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

    private LinearLayout contenedorExplorador;
    private LinearLayout layoutBarraInferior;

    private List<File> archivosEnCarpetaActual = new ArrayList<>();
    private List<File> videosEnReproduccionActual = new ArrayList<>();
    private File carpetaReproduciendoActualmente;

    private final android.os.Handler handlerSeek = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable runnableSeek;
    private long pressStartTime = 0;

    private int configItemHeight = 100;
    private int configTextSize = 32;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

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

        contenedorVideo = findViewById(R.id.contenedorVideo);
        playerView = findViewById(R.id.playerView);
        txtNombreVideo = findViewById(R.id.txtNombreVideo);

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

        configurarZonasToque();

        seekBarVideo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    exoPlayer.seekTo(progress);
                    txtTiempoActual.setText(formatearTiempo(progress));
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
                handlerUI.postDelayed(runnableOcultarUI, 3000);
            }
        });

        btnShuffle.setOnClickListener(v -> {
            isShuffleMode = !isShuffleMode;
            btnShuffle.setColorFilter(isShuffleMode ? android.graphics.Color.parseColor("#BB86FC") : android.graphics.Color.parseColor("#888888"));
            Toast.makeText(this, isShuffleMode ? "Modo Aleatorio Activo" : "Modo Ordenado", Toast.LENGTH_SHORT).show();
            
            if (contenedorVideo.getVisibility() == View.VISIBLE && !videosEnReproduccionActual.isEmpty()) {
                long currentPos = exoPlayer.getCurrentPosition();
                int currentIndex = exoPlayer.getCurrentMediaItemIndex();
                File videoActual = videosEnReproduccionActual.get(currentIndex);
                reproducirVideosDeCarpeta(videoActual, carpetaReproduciendoActualmente);
                exoPlayer.seekTo(exoPlayer.getCurrentMediaItemIndex(), currentPos);
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addDataScheme("file");
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                escanearYRefrescar();
                Toast.makeText(context, "Dispositivo detectado, actualizando...", Toast.LENGTH_SHORT).show();
            }
        }, filter);

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::escanearYRefrescar, 1000);

        listViewArchivos.setOnItemClickListener((parent, view, position, id) -> {
            File archivoSeleccionado = archivosEnCarpetaActual.get(position);
            if (archivoSeleccionado.isDirectory()) {
                navegarACarpeta(archivoSeleccionado);
            } else if (esVideo(archivoSeleccionado.getName())) {
                reproducirVideosDeCarpeta(archivoSeleccionado, carpetaReproduciendoActualmente);
            }
        });

        contenedorExplorador.setVisibility(View.VISIBLE);
        contenedorVideo.setVisibility(View.GONE);
        capaZonasToque.setVisibility(View.GONE);
        mostrarBarrasSistema();

        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC);
        playerView.setPlayer(exoPlayer);

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                if (mediaItem == null || mediaItem.localConfiguration == null) return;
                String uriStr = mediaItem.localConfiguration.uri.toString();
                try {
                    String nombreArchivo = Uri.decode(new File(uriStr).getName());
                    boolean isAudio = isAudioFile(uriStr);
                    if (isAudio) {
                        capaPantallaApagada.setVisibility(View.VISIBLE);
                        iniciarAnimacionBarras();
                        String path = mediaItem.localConfiguration.uri.getPath();
                        if (path != null) cargarMetadatosAudio(new File(path));
                    } else {
                        if (imgAlbumArt != null) {
                            imgAlbumArt.setImageResource(R.drawable.ic_minimal_player);
                            imgAlbumArt.setPadding(40, 40, 40, 40);
                        }
                        capaPantallaApagada.setVisibility(isScreenOffMode ? View.VISIBLE : View.GONE);
                        if (isScreenOffMode) iniciarAnimacionBarras();
                        mostrarNombreVideo(nombreArchivo);
                    }
                    mostrarFeedback(0, android.view.Gravity.CENTER);
                } catch (Exception e) { e.printStackTrace(); }
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
                Toast.makeText(MainActivity.this, "Archivo no compatible, saltando...", Toast.LENGTH_SHORT).show();
                irAlSiguienteVideo();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                mostrarDialogoPermisos();
            } else {
                escanearYRefrescar();
            }
        }
    }

    private void configurarZonasToque() {
        zonaIzquierda.setOnTouchListener((v, event) -> {
            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isUserHolding = true; isSwipingVolume = false; startY = event.getY();
                initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                pressStartTime = System.currentTimeMillis();
                mostrarFeedback(0, android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                handlerSeek.postDelayed(() -> {
                    if (isUserHolding && !isSwipingVolume) {
                        int skipDir = configPilotMode ? 1 : -1;
                        iniciarSeekLoop(skipDir * configSeekSeconds * 1000);
                        mostrarFeedback(configPilotMode ? android.R.drawable.ic_media_ff : android.R.drawable.ic_media_rew, android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                    }
                }, 3000);
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float deltaY = startY - event.getY();
                if (Math.abs(deltaY) > 30) {
                    isSwipingVolume = true; handlerSeek.removeCallbacksAndMessages(null);
                    int volChange = (int) (deltaY / (v.getHeight() / (float)maxVol));
                    int newVol = Math.max(0, Math.min(maxVol, initialVolume + volChange));
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0);
                    pbVolumen.setVisibility(View.VISIBLE); pbVolumen.setMax(maxVol); pbVolumen.setProgress(newVol);
                    pbVolumen.removeCallbacks(null); pbVolumen.postDelayed(() -> pbVolumen.setVisibility(View.GONE), 2000);
                }
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                isUserHolding = false; handlerSeek.removeCallbacksAndMessages(null);
                if (!isSwipingVolume) {
                    if (System.currentTimeMillis() - pressStartTime < 3000) {
                        if (configPilotMode) irAlSiguienteVideo(); else irAlVideoAnterior();
                    } else mostrarFeedback(0, android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
                }
                isSwipingVolume = false; return true;
            }
            return true;
        });

        zonaCentro.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isUserHolding = true; pressStartTime = System.currentTimeMillis();
                mostrarFeedback(0, android.view.Gravity.CENTER);
                handlerSeek.postDelayed(() -> {
                    if (isUserHolding) { exoPlayer.stop(); escanearYRefrescar(); Toast.makeText(this, "Regresando al inicio...", Toast.LENGTH_SHORT).show(); }
                }, 3000);
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                isUserHolding = false; handlerSeek.removeCallbacksAndMessages(null);
                if (System.currentTimeMillis() - pressStartTime < 3000) {
                    if (exoPlayer.isPlaying()) { exoPlayer.pause(); mostrarFeedback(android.R.drawable.ic_media_pause, android.view.Gravity.CENTER); }
                    else { exoPlayer.play(); mostrarFeedback(android.R.drawable.ic_media_play, android.view.Gravity.CENTER); }
                } else mostrarFeedback(0, android.view.Gravity.CENTER);
                return true;
            }
            return true;
        });

        zonaDerecha.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isUserHolding = true; isSwipingBrightness = false; startY = event.getY();
                float curBri = getWindow().getAttributes().screenBrightness;
                initialBrightness = curBri < 0 ? 0.5f : curBri;
                pressStartTime = System.currentTimeMillis();
                mostrarFeedback(0, android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
                handlerSeek.postDelayed(() -> {
                    if (isUserHolding && !isSwipingBrightness) {
                        int skipDir = configPilotMode ? -1 : 1;
                        iniciarSeekLoop(skipDir * configSeekSeconds * 1000);
                        mostrarFeedback(configPilotMode ? android.R.drawable.ic_media_rew : android.R.drawable.ic_media_ff, android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
                    }
                }, 3000);
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float deltaY = startY - event.getY();
                if (Math.abs(deltaY) > 30) {
                    isSwipingBrightness = true; handlerSeek.removeCallbacksAndMessages(null);
                    float briChange = deltaY / v.getHeight();
                    float newBri = Math.max(0.01f, Math.min(1.0f, initialBrightness + briChange));
                    android.view.WindowManager.LayoutParams lp = getWindow().getAttributes(); lp.screenBrightness = newBri; getWindow().setAttributes(lp);
                    pbBrillo.setVisibility(View.VISIBLE); pbBrillo.setMax(100); pbBrillo.setProgress((int)(newBri*100));
                    pbBrillo.removeCallbacks(null); pbBrillo.postDelayed(() -> pbBrillo.setVisibility(View.GONE), 2000);
                }
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                isUserHolding = false; handlerSeek.removeCallbacksAndMessages(null);
                if (!isSwipingBrightness) {
                    if (System.currentTimeMillis() - pressStartTime < 3000) {
                        if (configPilotMode) irAlVideoAnterior(); else irAlSiguienteVideo();
                    } else mostrarFeedback(0, android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
                }
                isSwipingBrightness = false; return true;
            }
            return true;
        });
    }

    private void mostrarDialogoPermisos() {
        new AlertDialog.Builder(this).setTitle("Acceso al USB requerido").setMessage("Para reproducir tus videos, necesito permiso para acceder a los archivos del USB.")
                .setPositiveButton("Dar Permiso", (dialog, which) -> solicitarPermisoEspecial()).setCancelable(false).show();
    }

    private void navegarACarpeta(File nuevaCarpeta) {
        if (nuevaCarpeta == null || !nuevaCarpeta.exists()) return;
        actualizarEstadoCarpeta(nuevaCarpeta);

        int textColor = configNightMode ? android.graphics.Color.WHITE : android.graphics.Color.BLACK;
        txtRutaActual.setTextColor(textColor);
        if (btnConfiguracion != null) btnConfiguracion.setColorFilter(textColor);
        if (btnInicio != null) btnInicio.setColorFilter(textColor);
        if (btnScreenOff != null) btnScreenOff.setColorFilter(isScreenOffMode ? android.graphics.Color.parseColor("#BB86FC") : textColor);

        listViewArchivos.setVisibility(View.VISIBLE); txtRutaActual.setVisibility(View.VISIBLE);
        contenedorExplorador.setVisibility(View.VISIBLE); contenedorVideo.setVisibility(View.GONE);
        capaZonasToque.setVisibility(View.GONE); mostrarBarrasSistema();
    }

    private void actualizarEstadoCarpeta(File nuevaCarpeta) {
        carpetaReproduciendoActualmente = nuevaCarpeta;
        txtRutaActual.setText("Ruta: " + nuevaCarpeta.getAbsolutePath());

        archivosEnCarpetaActual.clear();
        File[] lista = nuevaCarpeta.listFiles();
        List<File> carpetas = new ArrayList<>(), videos = new ArrayList<>();
        if (lista != null) {
            for (File f : lista) {
                if (f.isDirectory()) carpetas.add(f); else if (esVideo(f.getName())) videos.add(f);
            }
        }
        Collections.sort(carpetas, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        Collections.sort(videos, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        archivosEnCarpetaActual.addAll(carpetas); archivosEnCarpetaActual.addAll(videos);

        List<String> nombres = new ArrayList<>();
        for (File f : archivosEnCarpetaActual) {
            if (f.isDirectory()) nombres.add("📁 " + f.getName());
            else if (isAudioFile(f.getName())) nombres.add("🎵 " + getFileNameToDisplay(f.getName()));
            else nombres.add("🎬 " + getFileNameToDisplay(f.getName()));
        }

        listViewArchivos.setAdapter(new ArrayAdapter<String>(this, R.layout.item_carpeta, R.id.txtNombreCarpeta, nombres) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView t = v.findViewById(R.id.txtNombreCarpeta);
                t.setTextColor(configNightMode ? android.graphics.Color.WHITE : android.graphics.Color.BLACK);
                t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, configTextSize);
                android.view.ViewGroup.LayoutParams lp = t.getLayoutParams();
                lp.height = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, configItemHeight, getResources().getDisplayMetrics());
                t.setLayoutParams(lp);
                return v;
            }
        });
    }

    private boolean tieneVideos(File carpeta) {
        File[] contenido = carpeta.listFiles();
        if (contenido == null) return false;
        for (File f : contenido) {
            if (f.isFile() && esVideo(f.getName())) return true;
        }
        return false;
    }

    private boolean esVideo(String n) {
        String l = n.toLowerCase();
        return l.endsWith(".mp4") || l.endsWith(".mkv") || l.endsWith(".avi") || l.endsWith(".webm") || l.endsWith(".mp3");
    }

    private boolean isAudioFile(String p) {
        if (p == null) return false;
        String l = p.toLowerCase();
        return l.endsWith(".mp3") || l.contains(".mp3?") || l.contains(".mp3/");
    }

    private String getFileNameToDisplay(String n) {
        if (configHideExtension) {
            int d = n.lastIndexOf('.');
            if (d > 0) return n.substring(0, d);
        }
        return n;
    }

    private void reproducirVideosDeCarpeta(File videoInicial, File carpeta) {
        if (videoInicial == null || carpeta == null) return;
        carpetaReproduciendoActualmente = carpeta;
        exoPlayer.stop(); exoPlayer.clearMediaItems();
        videosEnReproduccionActual.clear();
        File[] list = carpeta.listFiles();
        if (list != null) {
            for (File f : list) if (f.isFile() && esVideo(f.getName())) videosEnReproduccionActual.add(f);
        }
        if (videosEnReproduccionActual.isEmpty()) return;

        if (isShuffleMode) {
            Collections.shuffle(videosEnReproduccionActual);
            videosEnReproduccionActual.remove(videoInicial); videosEnReproduccionActual.add(0, videoInicial);
        } else {
            Collections.sort(videosEnReproduccionActual, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        }

        int startIdx = 0;
        for (int i = 0; i < videosEnReproduccionActual.size(); i++) {
            File v = videosEnReproduccionActual.get(i);
            exoPlayer.addMediaItem(MediaItem.fromUri(Uri.fromFile(v)));
            if (v.getAbsolutePath().equals(videoInicial.getAbsolutePath())) startIdx = i;
        }

        mostrarNombreVideo(videoInicial.getName());
        contenedorExplorador.setVisibility(View.GONE); contenedorVideo.setVisibility(View.VISIBLE);
        capaZonasToque.setVisibility(View.VISIBLE); ocultarBarrasSistema();
        exoPlayer.seekTo(startIdx, 0);
        SharedPreferences r = getSharedPreferences("resume_prefs", MODE_PRIVATE);
        if (r.getString("last_video_path", "").equals(videoInicial.getAbsolutePath())) {
            long p = r.getLong("last_video_pos", 0); if (p > 0) exoPlayer.seekTo(startIdx, p);
        }
        exoPlayer.prepare(); exoPlayer.play();
    }

    private void reproducirSiguienteCarpetaGlobal() {
        if (carpetaReproduciendoActualmente == null) return;
        File padre = carpetaReproduciendoActualmente.getParentFile();
        if (padre == null) return;
        File[] hermanos = padre.listFiles(File::isDirectory);
        if (hermanos != null) {
            List<File> carpetas = new ArrayList<>();
            for (File h : hermanos) carpetas.add(h);
            Collections.sort(carpetas, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            int idx = carpetas.indexOf(carpetaReproduciendoActualmente);
            
            File sig = null;
            // Buscar hacia adelante una carpeta con videos/musica
            for (int i = 1; i <= carpetas.size(); i++) {
                int nextIdx = (idx + i) % carpetas.size();
                File temp = carpetas.get(nextIdx);
                if (tieneVideos(temp)) {
                    sig = temp;
                    break;
                }
                if (nextIdx == idx) break;
            }

            if (sig != null) {
                actualizarEstadoCarpeta(sig);
                contenedorExplorador.setVisibility(View.GONE); 
                contenedorVideo.setVisibility(View.VISIBLE);
                capaZonasToque.setVisibility(View.VISIBLE);
                txtRutaActual.setVisibility(View.GONE); 
                listViewArchivos.setVisibility(View.GONE);
                ocultarBarrasSistema();
                
                for (File f : archivosEnCarpetaActual) {
                    if (!f.isDirectory() && esVideo(f.getName())) { 
                        reproducirVideosDeCarpeta(f, sig); 
                        break; 
                    }
                }
            } else {
                Toast.makeText(this, "Has llegado al final de todas las carpetas.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (contenedorVideo.getVisibility() == View.VISIBLE) {
            exoPlayer.stop(); contenedorVideo.setVisibility(View.GONE); capaZonasToque.setVisibility(View.GONE);
            contenedorExplorador.setVisibility(View.VISIBLE); listViewArchivos.setVisibility(View.VISIBLE);
            txtRutaActual.setVisibility(View.VISIBLE); mostrarBarrasSistema();
        } else {
            File act = carpetaReproduciendoActualmente;
            File i = Environment.getExternalStorageDirectory();
            if (act != null && !act.getAbsolutePath().equals(i.getAbsolutePath())) {
                File p = act.getParentFile();
                if (p == null || p.getAbsolutePath().equals("/storage") || p.getAbsolutePath().equals("/storage/emulated")) navegarACarpeta(i);
                else navegarACarpeta(p);
            } else {
                if (tiempoPrimerClickAtras + 2000 > System.currentTimeMillis()) super.onBackPressed();
                else { Toast.makeText(this, "Presiona atrás de nuevo para salir", Toast.LENGTH_SHORT).show(); tiempoPrimerClickAtras = System.currentTimeMillis(); }
            }
        }
    }

    private void cargarMetadatosAudio(File file) {
        if (txtMusicTitle == null || imgAlbumArt == null) return;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            txtMusicTitle.setText(title != null ? title + (artist != null ? " - " + artist : "") : getFileNameToDisplay(file.getName()));
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                imgAlbumArt.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(art, 0, art.length));
                imgAlbumArt.setPadding(0, 0, 0, 0); imgAlbumArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                imgAlbumArt.setImageResource(R.drawable.ic_minimal_player);
                imgAlbumArt.setPadding(40, 40, 40, 40); imgAlbumArt.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
        } catch (Exception e) {
            txtMusicTitle.setText(getFileNameToDisplay(file.getName()));
            imgAlbumArt.setImageResource(R.drawable.ic_minimal_player);
        } finally { try { retriever.release(); } catch (Exception ignored) {} }
    }

    private void mostrarNombreVideo(String nombre) {
        if (txtNombreVideo == null) return;
        String n = getFileNameToDisplay(nombre);
        txtNombreVideo.setText(n);
        if (txtMusicTitle != null) txtMusicTitle.setText(n);
        
        // Si estamos en modo audio (capa visible) o es un archivo de audio, no mostramos el flotante arriba/abajo
        if ((capaPantallaApagada != null && capaPantallaApagada.getVisibility() == View.VISIBLE) || isAudioFile(nombre)) {
            txtNombreVideo.setVisibility(View.GONE);
            return;
        }
        
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) txtNombreVideo.getLayoutParams();
        if (configTitleTop) { lp.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL; lp.topMargin = (int)(20*getResources().getDisplayMetrics().density); lp.bottomMargin = 0; }
        else { lp.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL; lp.bottomMargin = (int)(120*getResources().getDisplayMetrics().density); lp.topMargin = 0; }
        txtNombreVideo.setLayoutParams(lp);
        if (contenedorVideo != null) contenedorVideo.setVisibility(View.VISIBLE);
        txtNombreVideo.setVisibility(View.VISIBLE); txtNombreVideo.removeCallbacks(null);
        txtNombreVideo.postDelayed(() -> {
            txtNombreVideo.setVisibility(View.GONE);
            if (exoPlayer != null && !exoPlayer.isPlaying() && contenedorExplorador != null && contenedorExplorador.getVisibility() == View.VISIBLE) {
                if (contenedorVideo != null) contenedorVideo.setVisibility(View.GONE);
            }
        }, 3000);
    }

    private void mostrarFeedback(int resId, int gravity) {
        if (resId != 0) {
            imgFeedback.setImageResource(resId);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) imgFeedback.getLayoutParams();
            lp.gravity = gravity; imgFeedback.setLayoutParams(lp);
            imgFeedback.setVisibility(View.VISIBLE);
        }
        layoutProgreso.setVisibility(View.VISIBLE);
        handlerUI.removeCallbacks(runnableOcultarUI);
        if (!isUserHolding) handlerUI.postDelayed(runnableOcultarUI, 3000);
    }

    private void iniciarHilosProgreso() {
        if (runnableProgreso != null) handlerProgreso.removeCallbacks(runnableProgreso);
        runnableProgreso = new Runnable() {
            @Override
            public void run() {
                if (exoPlayer != null && exoPlayer.getPlaybackState() != Player.STATE_IDLE) {
                    long cur = exoPlayer.getCurrentPosition(), dur = exoPlayer.getDuration();
                    if (dur > 0) {
                        seekBarVideo.setMax((int) dur); seekBarVideo.setProgress((int) cur);
                        txtTiempoActual.setText(formatearTiempo(cur)); txtTiempoRestante.setText("-" + formatearTiempo(dur - cur));
                        if (cur % 5000 < 1000) {
                            int idx = exoPlayer.getCurrentMediaItemIndex();
                            if (videosEnReproduccionActual != null && idx >= 0 && idx < videosEnReproduccionActual.size()) {
                                SharedPreferences.Editor e = getSharedPreferences("resume_prefs", MODE_PRIVATE).edit();
                                e.putString("last_video_path", videosEnReproduccionActual.get(idx).getAbsolutePath());
                                e.putLong("last_video_pos", cur); e.apply();
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
        int s = (int) (ms / 1000);
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", s / 60, s % 60);
    }

    private void ocultarBarrasSistema() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) { c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars()); c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE); }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    private void mostrarBarrasSistema() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) c.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && contenedorVideo != null && contenedorVideo.getVisibility() == View.VISIBLE) ocultarBarrasSistema();
    }

    private void mostrarDialogoConfiguracion() {
        View view = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        final int oldH = configItemHeight, oldT = configTextSize;
        final boolean oldN = configNightMode, oldTi = configTitleTop, oldP = configPilotMode, oldE = configHideExtension;
        TextInputEditText editF = view.findViewById(R.id.editDefaultFolder);
        SeekBar sbSkip = view.findViewById(R.id.seekBarSkipTime);
        TextView tvSkip = view.findViewById(R.id.txtValueSeek);
        SwitchMaterial swTi = view.findViewById(R.id.switchTitlePosition);
        SeekBar sbText = view.findViewById(R.id.seekBarTextSize);
        SeekBar sbH = view.findViewById(R.id.seekBarItemHeight);
        SwitchMaterial swN = view.findViewById(R.id.switchNightMode);
        SwitchMaterial swP = view.findViewById(R.id.switchPilotMode);
        SwitchMaterial swE = view.findViewById(R.id.switchHideExtension);

        editF.setText(configDefaultFolder); sbSkip.setProgress(configSeekSeconds); tvSkip.setText(configSeekSeconds + " segundos");
        swTi.setChecked(configTitleTop); sbText.setProgress(configTextSize); sbH.setProgress(configItemHeight);
        swN.setChecked(configNightMode); swP.setChecked(configPilotMode); swE.setChecked(configHideExtension);

        sbSkip.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) { tvSkip.setText(Math.max(1, p) + " segundos"); }
            @Override public void onStartTrackingTouch(SeekBar sb) {} @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        sbText.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) { configTextSize = p; if (carpetaReproduciendoActualmente != null) navegarACarpeta(carpetaReproduciendoActualmente); }
            @Override public void onStartTrackingTouch(SeekBar sb) {} @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        sbH.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) { configItemHeight = p; if (carpetaReproduciendoActualmente != null) navegarACarpeta(carpetaReproduciendoActualmente); }
            @Override public void onStartTrackingTouch(SeekBar sb) {} @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        swN.setOnCheckedChangeListener((bv, isC) -> { configNightMode = isC; aplicarTemaFondo(); actualizarColoresDialogo(view); if (carpetaReproduciendoActualmente != null) navegarACarpeta(carpetaReproduciendoActualmente); });
        swTi.setOnCheckedChangeListener((bv, isC) -> { configTitleTop = isC; mostrarNombreVideo("Ejemplo de Posición"); });
        swP.setOnCheckedChangeListener((bv, isC) -> configPilotMode = isC);
        swE.setOnCheckedChangeListener((bv, isC) -> { configHideExtension = isC; if (carpetaReproduciendoActualmente != null) navegarACarpeta(carpetaReproduciendoActualmente); });

        actualizarColoresDialogo(view);
        new MaterialAlertDialogBuilder(this).setTitle("Configuración").setView(view).setPositiveButton("Guardar", (d, w) -> {
            configDefaultFolder = editF.getText().toString(); configSeekSeconds = sbSkip.getProgress();
            SharedPreferences.Editor e = getSharedPreferences("config_repro", MODE_PRIVATE).edit();
            e.putInt("item_height", configItemHeight); e.putInt("text_size", configTextSize);
            e.putString("default_folder", configDefaultFolder); e.putInt("seek_seconds", configSeekSeconds);
            e.putBoolean("title_top", configTitleTop); e.putBoolean("night_mode", configNightMode);
            e.putBoolean("pilot_mode", configPilotMode); e.putBoolean("hide_extension", configHideExtension); e.apply();
        }).setNegativeButton("Cancelar", (d, w) -> {
            configItemHeight = oldH; configTextSize = oldT; configNightMode = oldN; configTitleTop = oldTi; configPilotMode = oldP; configHideExtension = oldE;
            aplicarTemaFondo(); if (carpetaReproduciendoActualmente != null) navegarACarpeta(carpetaReproduciendoActualmente);
            if (contenedorVideo.getVisibility() == View.VISIBLE) mostrarNombreVideo(txtNombreVideo.getText().toString());
        }).show();
    }

    private void aplicarTemaFondo() {
        View root = findViewById(android.R.id.content);
        int bg = configNightMode ? android.graphics.Color.parseColor("#121212") : android.graphics.Color.parseColor("#F5F5F5");
        int bar = configNightMode ? android.graphics.Color.parseColor("#1A1A1A") : android.graphics.Color.WHITE;
        int txt = configNightMode ? android.graphics.Color.WHITE : android.graphics.Color.BLACK;
        root.setBackgroundColor(bg); if (contenedorExplorador != null) contenedorExplorador.setBackgroundColor(bg);
        if (layoutBarraInferior != null) layoutBarraInferior.setBackgroundColor(bar);
        if (txtRutaActual != null) txtRutaActual.setTextColor(txt);
        if (btnConfiguracion != null) btnConfiguracion.setColorFilter(txt);
        if (btnInicio != null) btnInicio.setColorFilter(txt);
        if (btnScreenOff != null) btnScreenOff.setColorFilter(isScreenOffMode ? android.graphics.Color.parseColor("#BB86FC") : txt);
    }

    private void actualizarColoresDialogo(View v) {
        if (v == null) return;
        int bg = configNightMode ? android.graphics.Color.parseColor("#1A1A1A") : android.graphics.Color.WHITE;
        int txt = configNightMode ? android.graphics.Color.WHITE : android.graphics.Color.BLACK;
        int sub = configNightMode ? android.graphics.Color.parseColor("#888888") : android.graphics.Color.parseColor("#666666");
        v.setBackgroundColor(bg); actualizarRecursivo(v, txt, sub);
    }

    private void actualizarRecursivo(View v, int t, int s) {
        if (v instanceof TextInputLayout) { TextInputLayout til = (TextInputLayout) v; til.setDefaultHintTextColor(android.content.res.ColorStateList.valueOf(s)); til.setHintTextColor(android.content.res.ColorStateList.valueOf(t)); til.setBoxStrokeColor(t); }
        if (v instanceof android.view.ViewGroup) { android.view.ViewGroup g = (android.view.ViewGroup) v; for (int i = 0; i < g.getChildCount(); i++) actualizarRecursivo(g.getChildAt(i), t, s); }
        if (v instanceof TextView) { TextView tv = (TextView) v; String text = tv.getText().toString(); tv.setTextColor((text.equals("GENERAL") || text.equals("APARIENCIA")) ? s : t); if (v instanceof EditText) ((EditText) v).setHintTextColor(s); }
        else if (v instanceof SeekBar) { SeekBar sb = (SeekBar) v; sb.getThumb().setTint(t); sb.getProgressDrawable().setTint(t); }
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

    private void irAlVideoAnterior() {
        if (exoPlayer.hasPreviousMediaItem()) exoPlayer.seekToPreviousMediaItem();
        else reproducirCarpetaAnteriorGlobal();
    }

    private void irAlSiguienteVideo() {
        if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNextMediaItem();
        else reproducirSiguienteCarpetaGlobal();
    }

    private void iniciarSeekLoop(long ms) {
        runnableSeek = new Runnable() {
            @Override public void run() {
                if (exoPlayer != null) { long np = Math.max(0, Math.min(exoPlayer.getDuration(), exoPlayer.getCurrentPosition() + ms)); exoPlayer.seekTo(np); handlerSeek.postDelayed(this, 1000); }
            }
        };
        handlerSeek.post(runnableSeek);
    }

    private void iniciarAnimacionBarras() {
        if (capaPantallaApagada.getVisibility() == View.GONE) return;
        View[] b = {findViewById(R.id.bar1), findViewById(R.id.bar2), findViewById(R.id.bar3), findViewById(R.id.bar4), findViewById(R.id.bar5), findViewById(R.id.bar6), findViewById(R.id.bar7)};
        android.os.Handler h = new android.os.Handler();
        Runnable r = new Runnable() {
            @Override public void run() {
                if (capaPantallaApagada.getVisibility() == View.GONE || contenedorVideo.getVisibility() == View.GONE) return;
                if (!exoPlayer.isPlaying()) { h.postDelayed(this, 500); return; }
                java.util.Random rnd = new java.util.Random();
                for (View v : b) { if (v != null) { android.view.ViewGroup.LayoutParams lp = v.getLayoutParams(); lp.height = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, rnd.nextInt(50) + 10, getResources().getDisplayMetrics()); v.setLayoutParams(lp); } }
                h.postDelayed(this, 100);
            }
        };
        h.post(r);
    }

    private void reproducirCarpetaAnteriorGlobal() {
        if (carpetaReproduciendoActualmente == null) return;
        File padre = carpetaReproduciendoActualmente.getParentFile();
        if (padre != null) {
            File[] hermanos = padre.listFiles(File::isDirectory);
            if (hermanos != null) {
                List<File> list = new ArrayList<>(); for (File h : hermanos) list.add(h);
                Collections.sort(list, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
                int idx = list.indexOf(carpetaReproduciendoActualmente);
                
                File ant = null;
                // Buscar hacia atrás una carpeta con contenido reproducible
                for (int i = 1; i <= list.size(); i++) {
                    int prevIdx = (idx - i + list.size()) % list.size();
                    File temp = list.get(prevIdx);
                    if (tieneVideos(temp)) {
                        ant = temp;
                        break;
                    }
                    if (prevIdx == idx) break;
                }

                if (ant != null) {
                    actualizarEstadoCarpeta(ant);
                    contenedorExplorador.setVisibility(View.GONE); 
                    contenedorVideo.setVisibility(View.VISIBLE); 
                    capaZonasToque.setVisibility(View.VISIBLE);
                    txtRutaActual.setVisibility(View.GONE); 
                    listViewArchivos.setVisibility(View.GONE);
                    ocultarBarrasSistema();
                    
                    File ult = null; 
                    for (int i = archivosEnCarpetaActual.size()-1; i>=0; i--) { 
                        File f = archivosEnCarpetaActual.get(i); 
                        if (!f.isDirectory() && esVideo(f.getName())) { ult = f; break; } 
                    }
                    if (ult != null) reproducirVideosDeCarpeta(ult, ant);
                }
            }
        }
    }

    private void solicitarPermisoEspecial() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try { Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION); i.setData(Uri.fromParts("package", getPackageName(), null)); startActivity(i); }
            catch (Exception e) { startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)); }
        }
    }

    private void escanearYRefrescar() {
        File interna = Environment.getExternalStorageDirectory();
        File storage = new File("/storage/");
        if (storage.exists() && storage.listFiles() != null) {
            for (File f : storage.listFiles()) {
                if (f.isDirectory() && !f.getName().equals("emulated") && !f.getName().equals("self")) {
                    if (configDefaultFolder != null && !configDefaultFolder.isEmpty()) {
                        File fav = new File(f, configDefaultFolder); if (fav.exists() && fav.isDirectory()) { navegarACarpeta(fav); return; }
                    }
                    navegarACarpeta(f); return;
                }
            }
        }
        if (configDefaultFolder != null && !configDefaultFolder.isEmpty()) {
            File fav = new File(interna, configDefaultFolder); if (fav.exists() && fav.isDirectory()) { navegarACarpeta(fav); return; }
        }
        navegarACarpeta(interna);
    }
}
