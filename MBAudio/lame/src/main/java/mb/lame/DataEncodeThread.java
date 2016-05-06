package mb.lame;

import android.media.AudioRecord;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import mb.lame.util.LameUtil;

public class DataEncodeThread extends Thread implements AudioRecord.OnRecordPositionUpdateListener {

    public static final int PROCESS_STOP = 1;
    private StopHandler mHandler;
    private byte[] mMp3Buffer;
    private FileOutputStream mFileOutputStream;
    private List<Task> mTasks;
    private CountDownLatch mHandlerInitLatch;

    /**
     * @author buihong_ha
     * @see <a>https://groups.google.com/forum/?fromgroups=#!msg/android-developers/1aPZXZG6kWk/lIYDavGYn5UJ</a>
     */
    static class StopHandler extends Handler {

        WeakReference<DataEncodeThread> encodeThread;

        public StopHandler(DataEncodeThread encodeThread) {
            this.encodeThread = new WeakReference<>(encodeThread);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == PROCESS_STOP) {
                DataEncodeThread threadRef = encodeThread.get();
                while (threadRef.processData() > 0) ;
                // Cancel any event left in the queue
                removeCallbacksAndMessages(null);
                threadRef.flushAndRelease();
                getLooper().quit();
            }
            super.handleMessage(msg);
        }
    }

    private class Task {

        private short[] rawData;
        private int readSize;

        public Task(short[] rawData, int readSize) {
            this.rawData = rawData.clone();
            this.readSize = readSize;
        }

        public short[] getData() {
            return rawData;
        }

        public int getReadSize() {
            return readSize;
        }
    }

    /**
     * Constructor
     *
     * @param file       file
     * @param bufferSize bufferSize
     * @throws FileNotFoundException file not found
     */
    public DataEncodeThread(File file, int bufferSize) throws FileNotFoundException {
        this.mHandlerInitLatch = new CountDownLatch(PROCESS_STOP);
        this.mTasks = Collections.synchronizedList(new ArrayList());
        this.mFileOutputStream = new FileOutputStream(file);
        mMp3Buffer = new byte[(int) (7200 + (bufferSize * 2 * 1.25))];
    }

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new StopHandler(this);
        mHandlerInitLatch.countDown();
        Looper.loop();
    }

    /**
     * Return the handler attach to this thread
     *
     * @return the handler attach to this thread
     */
    public Handler getHandler() {
        try {
            mHandlerInitLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return mHandler;
    }

    @Override
    public void onMarkerReached(AudioRecord recorder) {
    }

    @Override
    public void onPeriodicNotification(AudioRecord recorder) {
        processData();
    }

    private int processData() {
        if (mTasks.size() > 0) {
            Task task = mTasks.remove(0);
            short[] buffer = task.getData();
            int readSize = task.getReadSize();
            int encodedSize = LameUtil.encode(buffer, buffer, readSize, mMp3Buffer);
            if (encodedSize > 0) {
                try {
                    mFileOutputStream.write(mMp3Buffer, 0, encodedSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return readSize;
        }
        return 0;
    }

    /**
     * Flush all data left in lame buffer to file
     */
    private void flushAndRelease() {
        final int flushResult = LameUtil.flush(mMp3Buffer);
        if (flushResult > 0) {
            try {
                mFileOutputStream.write(mMp3Buffer, 0, flushResult);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (mFileOutputStream != null) {
                    try {
                        mFileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                LameUtil.close();
            }
        }
    }

    public void addTask(short[] rawData, int readSize) {
        mTasks.add(new Task(rawData, readSize));
    }
}
