package mb.lame;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Message;

import java.io.File;
import java.io.IOException;

import mb.lame.util.LameUtil;

public class MP3Recorder {

    private static final PCMFormat DEFAULT_AUDIO_FORMAT = PCMFormat.PCM_16BIT;

    private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;

    // 模拟器仅支持从麦克风输入8kHz采样率
    public static final int DEFAULT_SAMPLING_RATE = 44100;

    // CHANNEL_IN_MONO单声道 , CHANNEL_CONFIGURATION_MONO 双声道
    private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    // 与DEFAULT_CHANNEL_CONFIG相关，因为是mono单声，所以是1
    private static final int DEFAULT_LAME_IN_CHANNEL = 1;

    private static final int DEFAULT_LAME_MP3_QUALITY = 7;

    // MP3 比特率
    private static final int DEFAULT_LAME_MP3_BIT_RATE = 128;

    // 每160帧作为一个周期，通知一下需要进行编码
    private static final int FRAME_COUNT = 160;

    // 最大音量
    private static final int MAX_VOLUME = 2000;

    private AudioRecord mAudioRecord = null;
    private int mBufferSize;
    private short[] mPCMBuffer;
    private DataEncodeThread mEncodeThread;
    private boolean mIsRecording = false;
    private boolean mIsPause;
    private File mRecordFile;
    private int mVolume;
    private OnBufferReadListener mReadListener;

    public interface OnBufferReadListener {
        void onBufferRead(AudioRecord audioRecord, short[] sArr, int i);
    }

    /**
     * Default constructor. Setup recorder with default sampling rate 1 channel,
     * 16 bits pcm
     *
     * @param recordFile target file
     */
    public MP3Recorder(File recordFile) {
        this.mAudioRecord = null;
        this.mIsRecording = false;
        this.mIsPause = false;
        this.mRecordFile = recordFile;
    }

    /**
     * Start recording. Create an encoding thread. Start record from this
     * thread.
     *
     * @throws IOException initAudioRecorder throws
     */
    public void start() throws IOException {
        if (mIsRecording) {
            return;
        }
        initAudioRecorder();
        mAudioRecord.startRecording();
        new Thread() {
            @Override
            public void run() {
                //设置线程权限
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                mIsRecording = true;
                while (mIsRecording) {
                    if (!MP3Recorder.this.mIsPause) {
                        int readSize = mAudioRecord.read(mPCMBuffer, 0, mBufferSize);
                        if (readSize > 0) {
                            mEncodeThread.addTask(mPCMBuffer, readSize);
                            calculateRealVolume(mPCMBuffer, readSize);
                            if (MP3Recorder.this.mReadListener != null) {
                                MP3Recorder.this.mReadListener.onBufferRead(MP3Recorder.this.mAudioRecord, MP3Recorder.this.mPCMBuffer, readSize);
                            }
                        }
                    }
                }
                // release and finalize audioRecord
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
                // stop the encoding thread and try to wait
                // until the thread finishes its job
                Message msg = Message.obtain(mEncodeThread.getHandler(), DataEncodeThread.PROCESS_STOP);
                msg.sendToTarget();
            }

            /**
             * 此计算方法来自samsung开发范例
             *
             * @param buffer buffer
             * @param readSize readSize
             */
            private void calculateRealVolume(short[] buffer, int readSize) {
                double sum = 0;
                for (int i = 0; i < readSize; i++) {
                    sum += buffer[i] * buffer[i];
                }
                if (readSize > 0) {
                    double amplitude = sum / readSize;
                    mVolume = (int) Math.sqrt(amplitude);
                }
            }
        }.start();
    }

    public String getLameVersion() {
        return LameUtil.getLameVersion();
    }

    public int getVolume() {
        return mVolume;
    }

    public int getMaxVolume() {
        return MAX_VOLUME;
    }

    public int getSamplijgRate() {
        return DEFAULT_SAMPLING_RATE;
    }

    public void stop() {
        this.mIsRecording = false;
        this.mIsPause = false;
    }

    public void pause() {
        this.mIsPause = true;
    }

    public void resume() {
        this.mIsPause = false;
    }

    public boolean isPausing() {
        return this.mIsPause;
    }

    public boolean isRecording() {
        return this.mIsRecording;
    }

    public AudioRecord getAudioRecord() {
        return this.mAudioRecord;
    }

    /**
     * Initialize audio recorder
     */
    private void initAudioRecorder() throws IOException {
        mBufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLING_RATE,
                DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat());

        int bytesPerFrame = DEFAULT_AUDIO_FORMAT.getBytesPerFrame();
        /* Get number of samples. Calculate the buffer size
		 * (round up to the factor of given frame size) 
		 * 使能被整除，方便下面的周期性通知
		 * */
        int frameSize = mBufferSize / bytesPerFrame;
        if (frameSize % FRAME_COUNT != 0) {
            frameSize += (FRAME_COUNT - frameSize % FRAME_COUNT);
            mBufferSize = frameSize * bytesPerFrame;
        }
		
		/* Setup audio recorder */
        mAudioRecord = new AudioRecord(DEFAULT_AUDIO_SOURCE,
                DEFAULT_SAMPLING_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat(),
                mBufferSize);

        mPCMBuffer = new short[mBufferSize];
		/*
		 * Initialize lame buffer
		 * mp3 sampling rate is the same as the recorded pcm sampling rate 
		 * The bit rate is 32kbps
		 * 
		 */
        LameUtil.init(DEFAULT_SAMPLING_RATE, DEFAULT_LAME_IN_CHANNEL, DEFAULT_SAMPLING_RATE, DEFAULT_LAME_MP3_BIT_RATE, DEFAULT_LAME_MP3_QUALITY);
        // Create and run thread used to encode data
        // The thread will
        mEncodeThread = new DataEncodeThread(mRecordFile, mBufferSize);
        mEncodeThread.start();
        mAudioRecord.setRecordPositionUpdateListener(mEncodeThread, mEncodeThread.getHandler());
        mAudioRecord.setPositionNotificationPeriod(FRAME_COUNT);
    }

    public void setOnBufferReadListener(OnBufferReadListener listener) {
        this.mReadListener = listener;
    }
}