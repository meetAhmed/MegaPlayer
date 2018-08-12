package search.youtube.gosling.james.searchyoutube.store;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;
import java.util.ArrayList;

import search.youtube.gosling.james.searchyoutube.AudioPlayerActivity;
import search.youtube.gosling.james.searchyoutube.R;
import search.youtube.gosling.james.searchyoutube.models.AudioItem;

public class AudioService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer mediaPlayer;
    private int resumePosition = 0;
    private AudioManager audioManager;
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    private ArrayList<AudioItem> audioItemArrayList;
    private int audioIndex = -1;
    private AudioItem activeAudio;
    private LocalBroadcastManager localBroadcastManager;
    Notification status;
    RemoteViews views, bigViews;
    public static final String SERVICE_RESULT = "search.youtube.gosling.james.searchyoutube.refresh.result";
    public static final String UPDATE_SEEKBAR_RESULT = "search.youtube.gosling.james.searchyoutube.update_seekbar_status";
    public static final String MEDIA_PLAYING_STATUS = "search.youtube.gosling.james.searchyoutube.playing_status";
    private Handler mHandler = new Handler();

    public AudioService() {
    }

    private IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //Invoked indicating buffering status of
        //a media resource being streamed over the network.
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopForeground(true);
        playNext();
        showNotification();
    }

    //Handle errors
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        //Invoked to communicate some info.
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //Invoked when the media source is ready for playback.
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        //Invoked indicating the completion of a seek operation.
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        //Invoked when the audio focus of the system is updated.
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) resumeMedia();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                stopForeground(true);
                stopSelf();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) pauseMedia();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this);
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(activeAudio.getUrl());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();

        Intent intent = new Intent(MEDIA_PLAYING_STATUS);
        intent.putExtra("status", true);
        localBroadcastManager.sendBroadcast(intent);
        updateViews();
    }

    private void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(new StorageUtil(getApplicationContext()).getSeekPosition());
            mediaPlayer.start();
            new StorageUtil(getApplicationContext()).removeSeekPosition();
        }
    }

    private void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    private void playNext() {

        stopMedia();
        mediaPlayer.reset();

        if (audioIndex == audioItemArrayList.size() - 1) {
            //if last in playlist
            audioIndex = 0;
            activeAudio = audioItemArrayList.get(audioIndex);
        } else {
            //get next in playlist
            audioIndex += 1;
            activeAudio = audioItemArrayList.get(audioIndex);
        }

        //Update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);
        Log.i("12312131", audioIndex + " -- " + activeAudio.getTitle());

        initMediaPlayer();
        stopForeground(true);
        updateViews();
    }

    private void skipToPrevious() {

        stopMedia();
        mediaPlayer.reset();

        if (audioIndex == 0) {
            //if first in playlist
            //set index to the last of audioList
            audioIndex = audioItemArrayList.size() - 1;
            activeAudio = audioItemArrayList.get(audioIndex);
        } else {
            //get previous in playlist
            audioIndex -= 1;
            activeAudio = audioItemArrayList.get(audioIndex);
        }

        //Update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        initMediaPlayer();
        stopForeground(true);
        updateViews();
    }

    private void pauseMedia() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
            views.setImageViewResource(R.id.status_bar_play, R.drawable.apollo_holo_dark_play);
            bigViews.setImageViewResource(R.id.status_bar_play, R.drawable.apollo_holo_dark_play);
            mNotificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
            Intent intent = new Intent(MEDIA_PLAYING_STATUS);
            intent.putExtra("status", false);
            localBroadcastManager.sendBroadcast(intent);
        }
    }

    private void controlMedia() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
            views.setImageViewResource(R.id.status_bar_play, R.drawable.apollo_holo_dark_play);
            bigViews.setImageViewResource(R.id.status_bar_play, R.drawable.apollo_holo_dark_play);
            mNotificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
            Intent intent = new Intent(MEDIA_PLAYING_STATUS);
            intent.putExtra("status", false);
            localBroadcastManager.sendBroadcast(intent);
        } else {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
            views.setImageViewResource(R.id.status_bar_play, R.drawable.apollo_holo_dark_pause);
            bigViews.setImageViewResource(R.id.status_bar_play, R.drawable.apollo_holo_dark_pause);
            mNotificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
            Intent intent = new Intent(MEDIA_PLAYING_STATUS);
            intent.putExtra("status", true);
            localBroadcastManager.sendBroadcast(intent);
        }
    }

    private void resumeMedia() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
            views.setImageViewResource(R.id.status_bar_play, R.drawable.apollo_holo_dark_pause);
            bigViews.setImageViewResource(R.id.status_bar_play, R.drawable.apollo_holo_dark_pause);
            mNotificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
            Intent intent = new Intent(MEDIA_PLAYING_STATUS);
            intent.putExtra("status", true);
            localBroadcastManager.sendBroadcast(intent);
        }
    }

    //The system calls this method when an activity, requests the service be started
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            //Load data from SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            audioItemArrayList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();

            if (audioIndex != -1 && audioIndex < audioItemArrayList.size()) {
                //index is in a valid range
                activeAudio = audioItemArrayList.get(audioIndex);
            } else {
                stopSelf();
            }
        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (requestAudioFocus() == false) {
            //Could not gain focus
            stopSelf();
        }
        updateProgressBar();
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            initMediaPlayer();
            showNotification();
        } else if (intent.getAction().equals(Constants.ACTION.PREV_ACTION)) {
            skipToPrevious();
            showNotification();
        } else if (intent.getAction().equals(Constants.ACTION.PLAY_ACTION)) {
            controlMedia();
        } else if (intent.getAction().equals(Constants.ACTION.NEXT_ACTION)) {
            playNext();
            showNotification();
        } else if (intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
            stopMedia();
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            stopForeground(true);
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        //removeNotification();
        stopForeground(true);
        //unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);
        unregisterReceiver(resumeAudioStatus);
        unregisterReceiver(seekbarUpdate);
        //clear cached playlist
        new StorageUtil(getApplicationContext()).clearCachedAudioPlaylist();
    }

    //Becoming noisy
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
            // buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                // controlMedia();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Perform one-time setup procedures

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        callStateListener();
        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver();
        //Listen for new AudioPlayerActivity to play -- BroadcastReceiver
        register_playNewAudio();
        regsiater_resume_playing();
        regsiaterSeekbarStatus();
    }

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Get the new media index form SharedPreferences
            audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();
            if (audioIndex != -1 && audioIndex < audioItemArrayList.size()) {
                //index is in a valid range
                activeAudio = audioItemArrayList.get(audioIndex);
            } else {
                stopSelf();
            }

            //A PLAY_NEW_AUDIO action received
            //reset mediaPlayer to play the new AudioPlayerActivity
            stopMedia();
            mediaPlayer.reset();
            stopForeground(true);
            initMediaPlayer();
            showNotification();
            // buildNotification(PlaybackStatus.PLAYING);
        }
    };

    private BroadcastReceiver resumeAudioStatus = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            controlMedia();
        }
    };

    private BroadcastReceiver seekbarUpdate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int val = intent.getIntExtra("seekbarProgress", 0);
            resumePosition = val;
            mediaPlayer.seekTo(val);
        }
    };

    private void register_playNewAudio() {
        IntentFilter filter = new IntentFilter(AudioPlayerActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }

    private void regsiater_resume_playing() {
        IntentFilter filter = new IntentFilter(AudioPlayerActivity.RESUME_AUDIO);
        registerReceiver(resumeAudioStatus, filter);
    }

    private void regsiaterSeekbarStatus() {
        IntentFilter filter = new IntentFilter(AudioPlayerActivity.SEEKBAR_BROADCAST);
        registerReceiver(seekbarUpdate, filter);
    }

    public class LocalBinder extends Binder {
        public AudioService getService() {
            return AudioService.this;
        }
    }

    private void showNotification() {
// Using RemoteViews to bind custom layouts into Notification
        views = new RemoteViews(getPackageName(),
                R.layout.status_bar);
        bigViews = new RemoteViews(getPackageName(),
                R.layout.status_bar_expanded);

        Bitmap bitmap = storeRoom.coverPicture(activeAudio.getUrl());
        if (bitmap == null) {
            bitmap = Constants.getDefaultAlbumArt(this);
        }
        bigViews.setImageViewBitmap(R.id.status_bar_album_art, bitmap);

        Intent notificationIntent = new Intent(this, AudioPlayerActivity.class);
        notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Intent previousIntent = new Intent(this, AudioService.class);
        previousIntent.setAction(Constants.ACTION.PREV_ACTION);
        PendingIntent ppreviousIntent = PendingIntent.getService(this, 0,
                previousIntent, 0);

        Intent playIntent = new Intent(this, AudioService.class);
        playIntent.setAction(Constants.ACTION.PLAY_ACTION);
        PendingIntent pplayIntent = PendingIntent.getService(this, 0, playIntent, 0);

        Intent nextIntent = new Intent(this, AudioService.class);
        nextIntent.setAction(Constants.ACTION.NEXT_ACTION);
        PendingIntent pnextIntent = PendingIntent.getService(this, 0,
                nextIntent, 0);

        Intent closeIntent = new Intent(this, AudioService.class);
        closeIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
        PendingIntent pcloseIntent = PendingIntent.getService(this, 0,
                closeIntent, 0);

        views.setOnClickPendingIntent(R.id.status_bar_play, pplayIntent);
        bigViews.setOnClickPendingIntent(R.id.status_bar_play, pplayIntent);

        views.setOnClickPendingIntent(R.id.status_bar_next, pnextIntent);
        bigViews.setOnClickPendingIntent(R.id.status_bar_next, pnextIntent);

        views.setOnClickPendingIntent(R.id.status_bar_prev, ppreviousIntent);
        bigViews.setOnClickPendingIntent(R.id.status_bar_prev, ppreviousIntent);

        views.setOnClickPendingIntent(R.id.status_bar_collapse, pcloseIntent);
        bigViews.setOnClickPendingIntent(R.id.status_bar_collapse, pcloseIntent);

        views.setImageViewResource(R.id.status_bar_play, R.drawable.apollo_holo_dark_pause);
        bigViews.setImageViewResource(R.id.status_bar_play, R.drawable.apollo_holo_dark_pause);

        views.setTextViewText(R.id.status_bar_track_name, activeAudio.getTitle());
        bigViews.setTextViewText(R.id.status_bar_track_name, activeAudio.getTitle());

        views.setTextViewText(R.id.status_bar_artist_name, activeAudio.getArtist());
        bigViews.setTextViewText(R.id.status_bar_artist_name, activeAudio.getArtist());

        status = new Notification.Builder(this).build();
        status.contentView = views;
        status.bigContentView = bigViews;
        status.flags = Notification.FLAG_ONGOING_EVENT;
        status.icon = R.drawable.cd;
        status.contentIntent = pendingIntent;
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
    }

    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    /**
     * Background Runnable thread
     */
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    long totalDuration = mediaPlayer.getDuration();
                    long currentDuration = mediaPlayer.getCurrentPosition();

                    Intent intent = new Intent(UPDATE_SEEKBAR_RESULT);
                    intent.putExtra("INTENT_TYPE", "SEEKBAR_RESULT");
                    intent.putExtra("totalDuration", totalDuration);
                    intent.putExtra("currentDuration", currentDuration);
                    localBroadcastManager.sendBroadcast(intent);
                }
            }
            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100);
        }
    };


    private void updateViews() {
        Intent intent = new Intent(SERVICE_RESULT);
        localBroadcastManager.sendBroadcast(intent);
    }

}