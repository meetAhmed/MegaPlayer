package search.youtube.gosling.james.searchyoutube;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;

import search.youtube.gosling.james.searchyoutube.Adapters.AudioTracksAdapter;
import search.youtube.gosling.james.searchyoutube.models.AudioItem;
import search.youtube.gosling.james.searchyoutube.store.AudioService;
import search.youtube.gosling.james.searchyoutube.store.Constants;
import search.youtube.gosling.james.searchyoutube.store.StorageUtil;
import search.youtube.gosling.james.searchyoutube.store.storeRoom;

public class AudioPlayerActivity extends AppCompatActivity {

    ArrayList<AudioItem> audioItemArrayList;
    RecyclerView audio_tracks_recyclerView;
    AudioTracksAdapter tracksAdapter;
    StorageUtil storage;
    private AudioService audioService;
    boolean serviceBound = false;
    private BroadcastReceiver broadcastReceiver, updateSeekBarReceiver, playingStatus;
    TextView songName, totalTimeText, currentTimeText;
    public static final String Broadcast_PLAY_NEW_AUDIO = "search.youtube.gosling.james.searchyoutube.PlayNewAudio";
    public static final String RESUME_AUDIO = "search.youtube.gosling.james.searchyoutube.change_status_playing";
    public static final String SEEKBAR_BROADCAST = "search.youtube.gosling.james.searchyoutube.change_seekbar_status";
    SeekBar seekBar;
    ImageButton status_bar_play, status_bar_prev, status_bar_next;
    int seekbarProgress = 0;
    ImageView coverPic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        getSupportActionBar().setTitle("Audio Player");
        storage = new StorageUtil(getApplicationContext());
        audio_tracks_recyclerView = (RecyclerView) findViewById(R.id.audio_tracks);
        audio_tracks_recyclerView.setLayoutManager(new GridLayoutManager(getApplicationContext(), 1));

        Log.i("ad123", "activity - " + serviceBound + " -- " + isServiceRunning());

        songName = (TextView) findViewById(R.id.songName);
        totalTimeText = (TextView) findViewById(R.id.totalTimeText);
        currentTimeText = (TextView) findViewById(R.id.currentTimeText);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        status_bar_play = (ImageButton) findViewById(R.id.status_bar_play);
        status_bar_prev = (ImageButton) findViewById(R.id.status_bar_prev);
        status_bar_next = (ImageButton) findViewById(R.id.status_bar_next);
        coverPic = (ImageView) findViewById(R.id.coverPic);

        loadSongs();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (tracksAdapter != null) {
                    tracksAdapter.index = storage.loadAudioIndex();
                    tracksAdapter.notifyDataSetChanged();
                    loadThings();
                }
            }
        };
        playingStatus = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Boolean status = intent.getBooleanExtra("status", false);
                if (status) {
                    status_bar_play.setImageResource(android.R.drawable.ic_media_pause);
                } else {
                    status_bar_play.setImageResource(android.R.drawable.ic_media_play);
                }
            }
        };
        updateSeekBarReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String intentType = intent.getStringExtra("INTENT_TYPE");
                if (intentType.equalsIgnoreCase("SEEKBAR_RESULT")) {
                    long totalDuration = intent.getLongExtra("totalDuration", -1);
                    long currentDuration = intent.getLongExtra("currentDuration", -1);
                    int percentage = 0;
                    if (currentDuration > 0) {
                        currentTimeText.setText(storeRoom.milliSecondsToTimer(currentDuration));
                        percentage = storeRoom.getProgressPercentage(currentDuration, totalDuration);
                        seekBar.setProgress(percentage);
                        storage.storeAudioPosition(currentDuration);
                        status_bar_play.setImageResource(android.R.drawable.ic_media_pause);
                    }
                    Log.i("12d123", storeRoom.milliSecondsToTimer(totalDuration) + " ::: " + storeRoom.milliSecondsToTimer(currentDuration) + " ::: " + percentage + "%");
                }
            }
        };
    }


    private void loadSongs() {
        audioItemArrayList = new ArrayList<>();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    String durationInLong = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    String durationInString = storeRoom.milliSecondsToTimer(Long.valueOf(durationInLong));

                    AudioItem audioItem = new AudioItem(name, artist, url, title, durationInLong, durationInString);
                    audioItemArrayList.add(audioItem);
                    // Log.i("233321211", storeRoom.coverPicture(url) + "");
                } while (cursor.moveToNext());
            }

            cursor.close();
            tracksAdapter = new AudioTracksAdapter(audioItemArrayList, new AudioTracksAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position) {
                    playAudio(position);
                    tracksAdapter.index = position;
                    tracksAdapter.notifyDataSetChanged();
                }
            });
            audio_tracks_recyclerView.setAdapter(tracksAdapter);
            //
            loadThings();

            status_bar_play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("ad123", "button clicked - " + serviceBound);
                    if (!isServiceRunning()) {
                        storage.clearCachedAudioPlaylist();
                        storage.storeAudio(audioItemArrayList);

                        Intent playerIntent = new Intent(getApplicationContext(), AudioService.class);
                        playerIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                        startService(playerIntent);
                        bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
                        loadThings();
                    } else {
                        //Service is active
                        //Send a broadcast to the service -> PLAY_NEW_AUDIO
                        Intent broadcastIntent = new Intent(RESUME_AUDIO);
                        sendBroadcast(broadcastIntent);
                        loadThings();
                    }
                }
            });
            status_bar_next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int audioIndex = storage.loadAudioIndex();
                    if (audioIndex == audioItemArrayList.size() - 1) {
                        //if last in playlist
                        audioIndex = 0;
                    } else {
                        //get next in playlist
                        audioIndex += 1;
                    }
                    playAudio(audioIndex);
                }
            });

            status_bar_prev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int audioIndex = storage.loadAudioIndex();
                    if (audioIndex == 0) {
                        //if first in playlist
                        //set index to the last of audioList
                        audioIndex = audioItemArrayList.size() - 1;
                    } else {
                        //get previous in playlist
                        audioIndex -= 1;
                    }
                    playAudio(audioIndex);
                }
            });

        }// if ends here
    }// method ends here

    public void loadThings() {
        final AudioItem currentItem = audioItemArrayList.get(storage.loadAudioIndex());
        songName.setText(currentItem.getTitle());
        totalTimeText.setText(currentItem.getDurationInString());
        seekBar.setMax(100);
        long storePosition = storage.loadAudioPosition();
        Bitmap bitmap = storeRoom.coverPicture(currentItem.getUrl());
        if (bitmap != null) {
            coverPic.setImageBitmap(bitmap);
        } else {
            coverPic.setImageResource(R.drawable.cd);
        }
        if (storePosition > 0) {
            currentTimeText.setText(storeRoom.milliSecondsToTimer(storePosition));
            seekBar.setProgress(storeRoom.getProgressPercentage(storePosition, Long.parseLong(currentItem.getDurationInLong())));
        } else {
            currentTimeText.setText("0:00");
            seekBar.setProgress(0);
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekbarProgress = storeRoom.progressToTimer(progress, Integer.parseInt(currentItem.getDurationInLong()));
                Log.i("123sadsd", seekbarProgress + "");
                currentTimeText.setText(storeRoom.milliSecondsToTimer(seekbarProgress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.i("123sadsd", "stopped" + isServiceRunning());
                if (isServiceRunning()) {
                    Intent broadcastIntent = new Intent(SEEKBAR_BROADCAST);
                    broadcastIntent.putExtra("seekbarProgress", seekbarProgress);
                    sendBroadcast(broadcastIntent);
                } else {
                    storage.setSeekPosition(seekbarProgress);
                }
            }
        });
    }

    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioService.LocalBinder binder = (AudioService.LocalBinder) service;
            audioService = binder.getService();
            serviceBound = true;
            Log.i("ad123", "service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            Log.i("ad123", "service dis connected");
        }
    };

    private void playAudio(int audioIndex) {

        //Check is service is active
        if (!isServiceRunning()) {
            //Store Serializable audioList to SharedPreferences
            storage.clearCachedAudioPlaylist();
            storage.storeAudio(audioItemArrayList);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, AudioService.class);
            playerIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            loadThings();
        } else {
            //Store the new audioIndex to SharedPreferences
            storage.storeAudioIndex(audioIndex);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
            loadThings();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.i("ad123", "putting " + serviceBound);
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
        Log.i("ad123", "getting " + serviceBound);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver((broadcastReceiver), new IntentFilter(AudioService.SERVICE_RESULT));
        LocalBroadcastManager.getInstance(this).registerReceiver((updateSeekBarReceiver), new IntentFilter(AudioService.UPDATE_SEEKBAR_RESULT));
        LocalBroadcastManager.getInstance(this).registerReceiver((playingStatus), new IntentFilter(AudioService.MEDIA_PLAYING_STATUS));
        storage.removeSeekPosition();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playingStatus);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateSeekBarReceiver);
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("search.youtube.gosling.james.searchyoutube.store.AudioService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
