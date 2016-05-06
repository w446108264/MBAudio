package com.mbaudio.simple;

import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import mb.lame.MP3Recorder;

public class MainActivity extends AppCompatActivity implements MP3Recorder.OnBufferReadListener {

    @Bind(R.id.av_visualizer)
    AudioRecordVisualizerView avVisualizer;
    @Bind(R.id.iv_play)
    ImageView ivPlay;
    @Bind(R.id.iv_record)
    ImageView ivRecord;
    @Bind(R.id.iv_crop)
    ImageView ivCrop;
    @Bind(R.id.tv_time)
    TextView tvTime;

    public static final int MAX_PATTERN_MATCH_BREAK_COUNT = 10;

    private MP3Recorder mRecorder = new MP3Recorder(new File(Environment.getExternalStorageDirectory(), "test.mp3"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mRecorder.setOnBufferReadListener(this);

        avVisualizer.setSamplingRate(this.mRecorder.getSamplijgRate());
        AudioRecordVisualizerView.PaintTheme theme = new AudioRecordVisualizerView.PaintTheme();
        theme.mBgColor = 0;
        theme.mPlayedTopColor = getResources().getColor(R.color.audio_sense_wave_played_top);
        theme.mPlayedBottomColor = getResources().getColor(R.color.audio_sense_wave_played_bottom);
        theme.mUnplayTopColor = getResources().getColor(R.color.audio_sense_wave_played_top);
        theme.mUnplayBottomColor = getResources().getColor(R.color.audio_sense_wave_played_bottom);
        theme.mLineColor = getResources().getColor(R.color.audio_sense_wave_played_top);
        theme.mStaffTextColor = getResources().getColor(R.color.audio_text);
        theme.mStaffLineColor = getResources().getColor(R.color.audio_sense_staff_line);
        theme.mVerticalLineColor = getResources().getColor(R.color.audio_sense_vertical_line);
        theme.mHorzontalLine = getResources().getColor(R.color.audio_sense_wave_played_top);
        theme.mHorizontalLine2 = getResources().getColor(android.R.color.transparent);
        avVisualizer.setPaintTheme(theme);
    }

    private void pauseRecording() {
        this.mRecorder.pause();
        this.mRecorder.setOnBufferReadListener(null);
        avVisualizer.setIsRecording(false);
    }

    private void resumeRecording() {
        this.mRecorder.resume();
        this.mRecorder.setOnBufferReadListener(this);
        avVisualizer.setIsRecording(true);
    }

    private void startRecording() {
        try {
            this.mRecorder.start();
            this.mRecorder.setOnBufferReadListener(this);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        avVisualizer.setIsRecording(true);
    }

    private void stopRecording() {
        this.mRecorder.stop();
        this.mRecorder.setOnBufferReadListener(null);
        avVisualizer.setIsRecording(false);
        avVisualizer.stopAudio();
        avVisualizer.clearWaveData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecorder.stop();
    }

    @OnClick(R.id.iv_play)
    void iv_play() {
    }

    @OnClick(R.id.iv_record)
    void iv_record() {
        if (mRecorder.isRecording()) {
            ivRecord.setImageResource(R.mipmap.icon_record);
            stopRecording();
        } else {
            ivRecord.setImageResource(R.mipmap.icon_recording);
            startRecording();
        }
    }

    @OnClick(R.id.iv_crop)
    void iv_crop() {
    }

    private double calculateRealVolumeSame(short[] buffer, int readSize) {
        int sum = 0;
        for (int i = 0; i < readSize; i++) {
            sum += buffer[i] * buffer[i];
        }
        if (readSize > 0) {
            return Math.sqrt(Math.abs((double) (sum / readSize))) / ((double) this.mRecorder.getMaxVolume());
        }
        return 0.0d;
    }

    class AnonymousClass5 implements Runnable {
        final Double newData;
        final int readSize;

        AnonymousClass5(Double newData, int readSize) {
            this.newData = newData;
            this.readSize = readSize;
        }

        public void run() {
            avVisualizer.addWaveData(newData.isNaN() ? 0.0d : newData.doubleValue(), readSize);
            int duration = (int) (avVisualizer.getTotalRecordedTime() / 1000.0d);
            int ms = (int) (avVisualizer.getTotalRecordedTime() % 1000.0d);
            tvTime.setText(formatDuaration(duration) + "." + (ms / 100));
        }
    }

    public static String formatDuaration(int duration) {
        int min = duration / 60;
        int sec = duration % 60;
        return (min < MAX_PATTERN_MATCH_BREAK_COUNT ? "0" + min : min + "") + ":" + (sec < MAX_PATTERN_MATCH_BREAK_COUNT ? "0" + sec : sec + "");
    }

    @Override
    public void onBufferRead(AudioRecord audioRecord, short[] sArr, int readSize) {
        runOnUiThread(new AnonymousClass5(Double.valueOf(calculateRealVolumeSame(sArr, readSize)), readSize));
    }
}
