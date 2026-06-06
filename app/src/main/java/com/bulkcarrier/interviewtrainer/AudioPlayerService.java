package com.bulkcarrier.interviewtrainer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.speech.tts.TextToSpeech;
import java.util.*;

public class AudioPlayerService extends Service implements TextToSpeech.OnInitListener {
    public static final String ACTION_PLAY_ALL = "PLAY_ALL";
    public static final String ACTION_PLAY_FROM = "PLAY_FROM";
    public static final String ACTION_PAUSE = "PAUSE";
    public static final String ACTION_RESUME = "RESUME";
    public static final String ACTION_STOP = "STOP";
    public static final String EXTRA_START_ID = "START_ID";
    public static final String EXTRA_SECTION = "SECTION";
    public static final String EXTRA_READ_RU = "READ_RU";

    private static final String CHANNEL_ID = "bulk_interview_audio";
    private TextToSpeech tts;
    private ArrayList<Question> allQuestions = new ArrayList<>();
    private ArrayList<Question> playList = new ArrayList<>();
    private int playIndex = 0;
    private int playPart = 0; // 0 question, 1 answer, 2 ru
    private boolean isPlaying = false;
    private boolean readRu = true;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        allQuestions = QuestionRepository.load(this);
        tts = new TextToSpeech(this, this);

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BulkInterview:AudioWakeLock");
        wakeLock.setReferenceCounted(false);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) {}
                @Override public void onDone(String utteranceId) {
                    if (isPlaying) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            advancePart();
                            speakCurrent();
                        }, 600);
                    }
                }
                @Override public void onError(String utteranceId) {
                    if (isPlaying) {
                        advancePart();
                        speakCurrent();
                    }
                }
            });
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        String action = intent.getAction();
        if (ACTION_PLAY_ALL.equals(action) || ACTION_PLAY_FROM.equals(action)) {
            readRu = intent.getBooleanExtra(EXTRA_READ_RU, true);
            String section = intent.getStringExtra(EXTRA_SECTION);
            buildPlayList(section);
            int startQuestionId = intent.getIntExtra(EXTRA_START_ID, -1);
            playIndex = 0;
            if (startQuestionId > 0) {
                for (int i = 0; i < playList.size(); i++) {
                    if (playList.get(i).id == startQuestionId) {
                        playIndex = i;
                        break;
                    }
                }
            }
            playPart = 0;
            isPlaying = true;
            acquireWakeLock();
            startForeground(1, buildNotification("Starting audio..."));
            speakCurrent();
        } else if (ACTION_PAUSE.equals(action)) {
            // Android TextToSpeech has no exact pause. Stop current utterance and resume from same part later.
            if (tts != null) tts.stop();
            isPlaying = false;
            updateNotification("Paused. Resume repeats current block.");
        } else if (ACTION_RESUME.equals(action)) {
            isPlaying = true;
            acquireWakeLock();
            startForeground(1, buildNotification("Resuming..."));
            speakCurrent();
        } else if (ACTION_STOP.equals(action)) {
            stopPlayback();
        }
        return START_STICKY;
    }

    private void buildPlayList(String section) {
        playList.clear();
        for (Question q : allQuestions) {
            if (section == null || section.equals("") || section.equals("All sections") || q.section.equals(section)) {
                playList.add(q);
            }
        }
    }

    private void speakCurrent() {
        if (!isPlaying || playIndex >= playList.size()) {
            stopPlayback();
            return;
        }
        Question q = playList.get(playIndex);
        String text;
        Locale locale;
        String title;
        if (playPart == 0) {
            text = "Question " + q.id + ". " + q.q;
            locale = Locale.US;
            title = "Question " + q.id + " / " + playList.size();
        } else if (playPart == 1) {
            text = "Answer. " + q.a;
            locale = Locale.US;
            title = "Answer " + q.id + " / " + playList.size();
        } else {
            if (!readRu) {
                advancePart();
                speakCurrent();
                return;
            }
            text = "Объяснение. " + q.ru;
            locale = new Locale("ru", "RU");
            title = "RU explanation " + q.id + " / " + playList.size();
        }

        updateNotification(title + " — " + q.section);

        if (tts == null) return;
        tts.setLanguage(locale);
        tts.setSpeechRate(locale.getLanguage().equals("ru") ? 0.95f : 0.90f);

        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utt_" + q.id + "_" + playPart);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "utt_" + q.id + "_" + playPart);
    }

    private void advancePart() {
        playPart++;
        if (playPart > 2) {
            playPart = 0;
            playIndex++;
        }
    }

    private void stopPlayback() {
        isPlaying = false;
        if (tts != null) tts.stop();
        releaseWakeLock();
        stopForeground(true);
        stopSelf();
    }

    private void acquireWakeLock() {
        try {
            if (wakeLock != null && !wakeLock.isHeld()) wakeLock.acquire(6 * 60 * 60 * 1000L);
        } catch (Exception ignored) {}
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        } catch (Exception ignored) {}
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(1, buildNotification(text));
    }

    private Notification buildNotification(String text) {
        PendingIntent openApp = PendingIntent.getActivity(this, 10,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent pause = PendingIntent.getService(this, 11,
                new Intent(this, AudioPlayerService.class).setAction(ACTION_PAUSE),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent resume = PendingIntent.getService(this, 12,
                new Intent(this, AudioPlayerService.class).setAction(ACTION_RESUME),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent stop = PendingIntent.getService(this, 13,
                new Intent(this, AudioPlayerService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Bulk Carrier Interview Trainer")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(openApp)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_pause, "Pause", pause)
                .addAction(android.R.drawable.ic_media_play, "Resume", resume)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Interview Audio Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Continuous interview audio playback");
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        stopPlayback();
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
