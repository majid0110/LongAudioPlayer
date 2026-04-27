package com.majid.audioplayer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQ_PERM = 1001;
    private static final long MIN_DURATION_MS = 30L * 60L * 1000L;

    private ListView lvTracks;
    private LinearLayout layoutStatus;
    private TextView tvStatus, tvCount;
    private Button btnGrant;
    private LinearLayout playerBar;
    private SeekBar seekBar;
    private TextView tvCurrent, tvDuration, tvNowPlaying, tvNowArtist;
    private Button btnPlayPause, btnPrev, btnNext, btnRewind, btnForward;

    private List<AudioTrack> tracks = new ArrayList<>();
    private TrackAdapter adapter;

    private PlayerService playerService;
    private boolean serviceBound = false;
    private final Handler handler = new Handler();
    private Runnable seekUpdater;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlayerService.LocalBinder binder = (PlayerService.LocalBinder) service;
            playerService = binder.getService();
            serviceBound = true;
            playerService.setCallback(playerCallback);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private final PlayerService.PlayerCallback playerCallback = new PlayerService.PlayerCallback() {
        @Override
        public void onPlaybackStarted(final int position) {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    updatePlayerBarForPosition(position);
                    btnPlayPause.setText("⏸");
                    startSeekUpdater();
                    adapter.setPlayingPosition(position);
                }
            });
        }
        @Override
        public void onPlaybackPaused() {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    btnPlayPause.setText("▶");
                    stopSeekUpdater();
                }
            });
        }
        @Override
        public void onPlaybackCompleted() {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    int next = playerService.getPlayingIndex() + 1;
                    if (next < tracks.size()) {
                        playTrack(next);
                    } else {
                        btnPlayPause.setText("▶");
                        stopSeekUpdater();
                    }
                }
            });
        }
        @Override
        public void onError(final String message) {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    int next = playerService.getPlayingIndex() + 1;
                    if (next < tracks.size()) playTrack(next);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        setupControls();
        startService(new Intent(this, PlayerService.class));
        bindService(new Intent(this, PlayerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        checkPermissionAndLoad();
    }

    private void bindViews() {
        lvTracks     = (ListView)     findViewById(R.id.lv_tracks);
        layoutStatus = (LinearLayout) findViewById(R.id.layout_status);
        tvStatus     = (TextView)     findViewById(R.id.tv_status);
        tvCount      = (TextView)     findViewById(R.id.tv_count);
        btnGrant     = (Button)       findViewById(R.id.btn_grant);
        playerBar    = (LinearLayout) findViewById(R.id.player_bar);
        seekBar      = (SeekBar)      findViewById(R.id.seek_bar);
        tvCurrent    = (TextView)     findViewById(R.id.tv_current);
        tvDuration   = (TextView)     findViewById(R.id.tv_duration);
        tvNowPlaying = (TextView)     findViewById(R.id.tv_now_playing);
        tvNowArtist  = (TextView)     findViewById(R.id.tv_now_artist);
        btnPlayPause = (Button)       findViewById(R.id.btn_play_pause);
        btnPrev      = (Button)       findViewById(R.id.btn_prev);
        btnNext      = (Button)       findViewById(R.id.btn_next);
        btnRewind    = (Button)       findViewById(R.id.btn_rewind);
        btnForward   = (Button)       findViewById(R.id.btn_forward);
    }

    private void setupControls() {
        btnGrant.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                requestStoragePermission();
            }
        });

        lvTracks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playTrack(position);
            }
        });

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (serviceBound) playerService.togglePause();
            }
        });

        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (serviceBound) {
                    int prev = playerService.getPlayingIndex() - 1;
                    if (prev >= 0) playTrack(prev);
                }
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (serviceBound) {
                    int next = playerService.getPlayingIndex() + 1;
                    if (next < tracks.size()) playTrack(next);
                }
            }
        });

        btnRewind.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (serviceBound) playerService.skip(-30000);
            }
        });

        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (serviceBound) playerService.skip(30000);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && serviceBound) {
                    int target = (int)((long) progress * playerService.getDuration() / 1000L);
                    playerService.seekTo(target);
                    tvCurrent.setText(AudioTrack.formatTime(target / 1000));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void checkPermissionAndLoad() {
        if (hasStoragePermission()) {
            loadTracks();
        } else {
            showStatus("Storage permission is needed\nto find audio files on your device.", true);
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            return checkSelfPermission("android.permission.READ_MEDIA_AUDIO")
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE")
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.READ_MEDIA_AUDIO"}, REQ_PERM);
        } else {
            requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, REQ_PERM);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadTracks();
            } else {
                boolean canAskAgain;
                if (Build.VERSION.SDK_INT >= 33) {
                    canAskAgain = shouldShowRequestPermissionRationale("android.permission.READ_MEDIA_AUDIO");
                } else {
                    canAskAgain = shouldShowRequestPermissionRationale("android.permission.READ_EXTERNAL_STORAGE");
                }
                if (!canAskAgain) {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Permission Required")
                        .setMessage("Please go to Settings → Apps → LongAudio Player → Permissions → Storage and allow access.")
                        .setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int w) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                } else {
                    showStatus("Permission denied.\nTap Grant Permission to try again.", true);
                }
            }
        }
    }

    private void showStatus(String msg, boolean showBtn) {
        layoutStatus.setVisibility(View.VISIBLE);
        lvTracks.setVisibility(View.GONE);
        tvStatus.setText(msg);
        btnGrant.setVisibility(showBtn ? View.VISIBLE : View.GONE);
    }

    private void hideStatus() {
        layoutStatus.setVisibility(View.GONE);
        lvTracks.setVisibility(View.VISIBLE);
    }

    private void loadTracks() {
        showStatus("Scanning audio files…", false);
        new Thread(new Runnable() {
            @Override public void run() {
                final List<AudioTrack> found = scanAudio();
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        tracks.clear();
                        tracks.addAll(found);
                        if (tracks.isEmpty()) {
                            showStatus("No audio files found.\nOnly files longer than 30 minutes are shown.", false);
                        } else {
                            hideStatus();
                            adapter = new TrackAdapter(MainActivity.this, tracks);
                            lvTracks.setAdapter(adapter);
                            tvCount.setText(tracks.size() + " files");
                        }
                    }
                });
            }
        }).start();
    }

    private List<AudioTrack> scanAudio() {
        List<AudioTrack> result = new ArrayList<>();
        ContentResolver cr = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE
        };
        Cursor cursor = null;
        try {
            cursor = cr.query(
                uri, projection,
                MediaStore.Audio.Media.DURATION + " >= ?",
                new String[]{String.valueOf(MIN_DURATION_MS)},
                MediaStore.Audio.Media.TITLE + " ASC"
            );
            if (cursor != null && cursor.moveToFirst()) {
                int idCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int pathCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int sizeCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
                do {
                    long   id       = cursor.getLong(idCol);
                    String title    = cursor.getString(titleCol);
                    String artist   = cursor.getString(artistCol);
                    String album    = cursor.getString(albumCol);
                    long   duration = cursor.getLong(durCol);
                    String path     = cursor.getString(pathCol);
                    long   size     = cursor.getLong(sizeCol);

                    if (title == null || title.isEmpty())
                        title = path != null ? path.substring(path.lastIndexOf('/') + 1) : "Unknown";
                    if (artist == null || artist.isEmpty() || artist.equals("<unknown>"))
                        artist = getString(R.string.unknown_artist);
                    if (album == null || album.isEmpty())
                        album = getString(R.string.unknown_album);

                    result.add(new AudioTrack(id, title, artist, album, duration, path, size));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return result;
    }

    private void playTrack(int index) {
        if (!serviceBound || index < 0 || index >= tracks.size()) return;
        AudioTrack track = tracks.get(index);
        playerService.play(track.path, index);
        playerBar.setVisibility(View.VISIBLE);
    }

    private void updatePlayerBarForPosition(int position) {
        if (position < 0 || position >= tracks.size()) return;
        AudioTrack track = tracks.get(position);
        tvNowPlaying.setText(track.title);
        tvNowArtist.setText(track.artist + " • " + track.getDurationFormatted());
        tvDuration.setText(track.getDurationFormatted());
        seekBar.setProgress(0);
        tvCurrent.setText("0:00:00");
    }

    private void startSeekUpdater() {
        stopSeekUpdater();
        seekUpdater = new Runnable() {
            @Override public void run() {
                if (serviceBound && playerService.isPlaying()) {
                    int cur = playerService.getCurrentPosition();
                    int dur = playerService.getDuration();
                    tvCurrent.setText(AudioTrack.formatTime(cur / 1000));
                    if (dur > 0) {
                        seekBar.setMax(1000);
                        seekBar.setProgress((int)((long) cur * 1000 / dur));
                    }
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(seekUpdater);
    }

    private void stopSeekUpdater() {
        if (seekUpdater != null) {
            handler.removeCallbacks(seekUpdater);
            seekUpdater = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSeekUpdater();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}
