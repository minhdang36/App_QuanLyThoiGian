package com.example.myapplication.ui.pomodoro;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.ui.ai.AiAutoScheduleActivity;
import com.example.myapplication.ui.main.MainActivity;
import com.example.myapplication.ui.project.ProjectsActivity;
import com.example.myapplication.ui.task.AddTaskActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Locale;

public class PomodoroActivity extends AppCompatActivity {

    private TextView tabFocus, tabShortBreak, tabLongBreak;
    private TextView tvTimerDisplay, tvSessionCount, btnStartTimer;
    private ImageButton btnResetTimer, btnSoundToggle;
    private MaterialCardView cardSoundSelector;
    private TextView tvSoundTitle, tvSoundSubtitle;
    private ImageView btnPlayPauseSound;

    private TextView chipSound1, chipSound2, chipSound3, chipSound4, chipSound5, chipSound6;

    private LinearLayout navAgenda, navProjects, navTimeline;
    private FloatingActionButton fabAdd;

    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private long timeRemainingInMillis = 25 * 60 * 1000L;
    private long timerDurationInMillis = 25 * 60 * 1000L;

    private int currentSession = 1;
    private final int totalSessions = 4;

    private boolean isSoundPlaying = false;
    private AudioTrack audioTrack;
    private Thread audioThread;
    private int selectedSoundIndex = 0;

    private final String[] soundTitles = {
            "Rừng thông & Gió nhẹ",
            "Tiếng mưa rơi thư giãn",
            "Sóng biển rì rào",
            "Quán cà phê yên tĩnh",
            "Binaural Beats - 432Hz",
            "Tiếng ồn trắng (White Noise)"
    };

    private final String[] soundSubtitles = {
            "Tần số thiên nhiên - 432Hz",
            "Âm thanh hạt mưa xoa dịu",
            "Sóng biển nhịp nhàng 220Hz",
            "Không gian làm việc ấm cúng",
            "Sóng não tập trung chuyên sâu",
            "Loại bỏ tạp âm xung quanh"
    };

    private final double[] soundFrequencies = {432.0, 300.0, 220.0, 528.0, 432.0, 180.0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro);

        tabFocus = findViewById(R.id.tabFocus);
        tabShortBreak = findViewById(R.id.tabShortBreak);
        tabLongBreak = findViewById(R.id.tabLongBreak);

        tvTimerDisplay = findViewById(R.id.tvTimerDisplay);
        tvSessionCount = findViewById(R.id.tvSessionCount);
        btnStartTimer = findViewById(R.id.btnStartTimer);
        btnResetTimer = findViewById(R.id.btnResetTimer);
        btnSoundToggle = findViewById(R.id.btnSoundToggle);

        chipSound1 = findViewById(R.id.chipSound1);
        chipSound2 = findViewById(R.id.chipSound2);
        chipSound3 = findViewById(R.id.chipSound3);
        chipSound4 = findViewById(R.id.chipSound4);
        chipSound5 = findViewById(R.id.chipSound5);
        chipSound6 = findViewById(R.id.chipSound6);

        cardSoundSelector = findViewById(R.id.cardSoundSelector);
        tvSoundTitle = findViewById(R.id.tvSoundTitle);
        tvSoundSubtitle = findViewById(R.id.tvSoundSubtitle);
        btnPlayPauseSound = findViewById(R.id.btnPlayPauseSound);

        navAgenda = findViewById(R.id.navAgenda);
        navProjects = findViewById(R.id.navProjects);
        navTimeline = findViewById(R.id.navTimeline);
        fabAdd = findViewById(R.id.fabAdd);

        setupTabs();
        setupTimerControls();
        setupAmbientSoundControls();
        setupBottomNav();
    }

    private void setupTabs() {
        tabFocus.setOnClickListener(v -> setTimerMode(25, tabFocus));
        tabShortBreak.setOnClickListener(v -> setTimerMode(5, tabShortBreak));
        tabLongBreak.setOnClickListener(v -> setTimerMode(15, tabLongBreak));
    }

    private void setTimerMode(int minutes, TextView selectedTab) {
        if (isTimerRunning) {
            pauseTimer();
        }

        tabFocus.setBackground(null);
        tabFocus.setTextColor(0xFF777777);
        tabShortBreak.setBackground(null);
        tabShortBreak.setTextColor(0xFF777777);
        tabLongBreak.setBackground(null);
        tabLongBreak.setTextColor(0xFF777777);

        selectedTab.setBackgroundResource(R.drawable.bg_pill_brown);
        selectedTab.setTextColor(0xFFFFFFFF);

        timerDurationInMillis = minutes * 60 * 1000L;
        timeRemainingInMillis = timerDurationInMillis;
        updateTimerText();
    }

    private void setupTimerControls() {
        btnStartTimer.setOnClickListener(v -> {
            if (isTimerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnResetTimer.setOnClickListener(v -> resetTimer());
    }

    private void setupAmbientSoundControls() {
        if (chipSound1 != null) chipSound1.setOnClickListener(v -> selectSoundByIndex(0));
        if (chipSound2 != null) chipSound2.setOnClickListener(v -> selectSoundByIndex(1));
        if (chipSound3 != null) chipSound3.setOnClickListener(v -> selectSoundByIndex(2));
        if (chipSound4 != null) chipSound4.setOnClickListener(v -> selectSoundByIndex(3));
        if (chipSound5 != null) chipSound5.setOnClickListener(v -> selectSoundByIndex(4));
        if (chipSound6 != null) chipSound6.setOnClickListener(v -> selectSoundByIndex(5));

        View.OnClickListener togglePlayClick = v -> toggleSoundPlayback();

        if (btnPlayPauseSound != null) {
            btnPlayPauseSound.setOnClickListener(togglePlayClick);
        }
        if (btnSoundToggle != null) {
            btnSoundToggle.setOnClickListener(togglePlayClick);
        }

        if (cardSoundSelector != null) {
            cardSoundSelector.setOnClickListener(v -> showSoundSelectionDialog());
        }
    }

    private void selectSoundByIndex(int index) {
        selectedSoundIndex = index;

        TextView[] soundChips = {chipSound1, chipSound2, chipSound3, chipSound4, chipSound5, chipSound6};
        for (int i = 0; i < soundChips.length; i++) {
            if (soundChips[i] != null) {
                if (i == index) {
                    soundChips[i].setBackgroundResource(R.drawable.bg_pill_brown);
                    soundChips[i].setTextColor(0xFFFFFFFF);
                } else {
                    soundChips[i].setBackgroundResource(R.drawable.bg_pill_light);
                    soundChips[i].setTextColor(0xFF555555);
                }
            }
        }

        if (tvSoundTitle != null) tvSoundTitle.setText("Âm thanh nền: " + soundTitles[selectedSoundIndex]);
        if (tvSoundSubtitle != null) tvSoundSubtitle.setText(soundSubtitles[selectedSoundIndex]);

        if (!isSoundPlaying) {
            playAmbientAudio(soundFrequencies[selectedSoundIndex]);
            isSoundPlaying = true;
            if (btnPlayPauseSound != null) btnPlayPauseSound.setImageResource(android.R.drawable.ic_media_pause);
            Toast.makeText(this, "🔊 Đang phát: " + soundTitles[selectedSoundIndex], Toast.LENGTH_SHORT).show();
        } else {
            stopAmbientAudio();
            playAmbientAudio(soundFrequencies[selectedSoundIndex]);
            Toast.makeText(this, "🔊 Đã đổi sang: " + soundTitles[selectedSoundIndex], Toast.LENGTH_SHORT).show();
        }
    }

    private void showSoundSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎧 Chọn âm thanh nền thư giãn (6 loại)");
        builder.setSingleChoiceItems(soundTitles, selectedSoundIndex, (dialog, which) -> {
            selectSoundByIndex(which);
            dialog.dismiss();
        });
        builder.setNegativeButton("Đóng", null);
        builder.show();
    }

    private void toggleSoundPlayback() {
        if (isSoundPlaying) {
            stopAmbientAudio();
            isSoundPlaying = false;
            if (btnPlayPauseSound != null) btnPlayPauseSound.setImageResource(android.R.drawable.ic_media_play);
            Toast.makeText(this, "Đã tạm dừng âm thanh nền", Toast.LENGTH_SHORT).show();
        } else {
            playAmbientAudio(soundFrequencies[selectedSoundIndex]);
            isSoundPlaying = true;
            if (btnPlayPauseSound != null) btnPlayPauseSound.setImageResource(android.R.drawable.ic_media_pause);
            Toast.makeText(this, "🔊 Đang phát: " + soundTitles[selectedSoundIndex], Toast.LENGTH_SHORT).show();
        }
    }

    private void playAmbientAudio(double frequency) {
        stopAmbientAudio();

        audioThread = new Thread(() -> {
            int sampleRate = 44100;
            int numSamples = sampleRate;
            double[] sample = new double[numSamples];
            byte[] generatedSnd = new byte[2 * numSamples];

            for (int i = 0; i < numSamples; ++i) {
                sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / frequency));
            }

            int idx = 0;
            for (double dVal : sample) {
                short val = (short) ((dVal * 32767) * 0.15);
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >> 8);
            }

            int bufferSize = AudioTrack.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

            try {
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bufferSize,
                        AudioTrack.MODE_STREAM);

                audioTrack.play();

                while (isSoundPlaying && !Thread.currentThread().isInterrupted()) {
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        audioThread.start();
    }

    private void stopAmbientAudio() {
        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception ignored) {}
            audioTrack = null;
        }

        if (audioThread != null) {
            audioThread.interrupt();
            audioThread = null;
        }
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeRemainingInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemainingInMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                btnStartTimer.setText("▶ Bắt đầu");
                Toast.makeText(PomodoroActivity.this, "Hoàn thành phiên tập trung!", Toast.LENGTH_LONG).show();

                if (currentSession < totalSessions) {
                    currentSession++;
                    tvSessionCount.setText("● Phiên " + currentSession + " / " + totalSessions);
                }
            }
        }.start();

        isTimerRunning = true;
        btnStartTimer.setText("⏸ Tạm dừng");
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        btnStartTimer.setText("▶ Bắt đầu");
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        timeRemainingInMillis = timerDurationInMillis;
        updateTimerText();
        btnStartTimer.setText("▶ Bắt đầu");
    }

    private void updateTimerText() {
        int minutes = (int) (timeRemainingInMillis / 1000) / 60;
        int seconds = (int) (timeRemainingInMillis / 1000) % 60;
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvTimerDisplay.setText(timeFormatted);
    }

    private void setupBottomNav() {
        if (navAgenda != null) {
            navAgenda.setOnClickListener(v -> {
                Intent intent = new Intent(PomodoroActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(PomodoroActivity.this, AddTaskActivity.class);
                startActivity(intent);
            });
        }

        if (navProjects != null) {
            navProjects.setOnClickListener(v -> {
                Intent intent = new Intent(PomodoroActivity.this, ProjectsActivity.class);
                startActivity(intent);
            });
        }

        if (navTimeline != null) {
            navTimeline.setOnClickListener(v -> {
                Intent intent = new Intent(PomodoroActivity.this, AiAutoScheduleActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopAmbientAudio();
        super.onDestroy();
    }
}