package edu.psu.jjb24.lullabycreator_v2;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.widget.Toast;

import java.util.HashSet;

public class MusicPlayerService extends Service implements AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener {

    private static final String ACTION_PLAY = "edu.psu.jjb24.multimediaexample.action.PLAY";
    private static final String ACTION_STOP = "edu.psu.jjb24.multimediaexample.action.STOP";
    private static final String ACTION_STATUS = "edu.psu.jjb24.multimediaexample.action.STATUS";
    private static final String EXTRA_RESOURCE_ID = "edu.psu.jjb24.multimediaexample.extra.RESOURCE_ID";


    private HashSet<MediaPlayer> mediaPlayers = new HashSet<>();
    private BroadcastReceiver musicIntentReceiver;

    /**
     * Starts this service to play the resource id given.
     */
    public static void play(Context context, int resourceId) {
        Intent intent = new Intent(context, MusicPlayerService.class);
        intent.setAction(ACTION_PLAY);
        intent.putExtra(EXTRA_RESOURCE_ID, resourceId);
        context.startService(intent);
    }

    /**
     * Starts this service to stop all streams that are currently playing.
     */
    public static void stop(Context context) {
        Intent intent = new Intent(context, MusicPlayerService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    /**
     * Starts this service to send a broadcast with number of streams playing
     */
    public static void status(Context context) {
        Intent intent = new Intent(context, MusicPlayerService.class);
        intent.setAction(ACTION_STATUS);
        context.startService(intent);
    }

    /**
     * Process the intent that was just received
     *
     * @param intent  the intent that was sent to the service
     * @param flags   not used
     * @param startId not used
     * @return sticky value
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PLAY.equals(action)) {
                final int resourceId = intent.getIntExtra(EXTRA_RESOURCE_ID, 0);
                play(resourceId);
            } else if (ACTION_STOP.equals(action)) {
                stop();
            } else if (ACTION_STATUS.equals(action)) {
                Intent statusIntent = new Intent("edu.psu.jjb24.multimediaexample.STATUS_UPDATE");
                statusIntent.putExtra("status", mediaPlayers.size());
                sendBroadcast(statusIntent);
            }
        }

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    /**
     * Since this is not a bound service, we simply return null here.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void play(int resId) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Toast.makeText(this, "Try again when current application is done playing audio.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set up the mediaPlayerObject
        MediaPlayer mediaPlayer = MediaPlayer.create(this, resId);
        mediaPlayers.add(mediaPlayer);
        mediaPlayer.setOnCompletionListener(this);

        // If not doing so already, create a broadcast receiver to listen for audio becoming noisy
        if (musicIntentReceiver == null) {
            musicIntentReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    Toast.makeText(ctx, "Audio becoming noisy!", Toast.LENGTH_LONG).show();
                    MusicPlayerService.this.stop();
                }
            };
            IntentFilter intentFilter = new IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            registerReceiver(musicIntentReceiver, intentFilter);
        }
        mediaPlayer.start();

        // Broadcast an intent to update the status displayed in the activity
        Intent intent = new Intent("edu.psu.jjb24.multimediaexample.STATUS_UPDATE");
        intent.putExtra("status", mediaPlayers.size());
        sendBroadcast(intent);
    }


    private void stop() {
        // Update the collection of mediaPlayers, and be sure to release resources
        for (MediaPlayer mediaPlayer : mediaPlayers) {
            if (mediaPlayer.isPlaying()) mediaPlayer.pause();
            mediaPlayer.release();
        }
        mediaPlayers.clear();

        // Update status
        Intent intent = new Intent("edu.psu.jjb24.multimediaexample.STATUS_UPDATE");
        intent.putExtra("status", 0);
        sendBroadcast(intent);

        // No longer need focus
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);

        // Unregister receiver for audio becoming noisy
        if (musicIntentReceiver != null) {
            unregisterReceiver(musicIntentReceiver);
            musicIntentReceiver = null;
        }
    }

    /**
     * Method from the AudioManager.OnAudioFocusChangeListener interface
     *
     * @see AudioManager.OnAudioFocusChangeListener
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for either a long or short amount of time: either way stop playback and
                // release media players
                Toast.makeText(getApplicationContext(), "Lost Audio Focus!", Toast.LENGTH_LONG).show();
                stop();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                for (MediaPlayer mediaPlayer : mediaPlayers) {
                    if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                }
                break;
        }
    }


    /**
     * Method from the MediaPlayer.OnCompletionListener  interface
     *
     * @see MediaPlayer.OnCompletionListener
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        mediaPlayers.remove(mp);
        mp.release();

        if (mediaPlayers.size() == 0) {
            stop();
        } else {
            Intent intent = new Intent("edu.psu.jjb24.multimediaexample.STATUS_UPDATE");
            intent.putExtra("status", mediaPlayers.size());
            sendBroadcast(intent);
        }
    }

}
