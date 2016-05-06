package com.mbaudio.simple;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import mb.lame.MP3Recorder;


public class AudioRecordVisualizerView extends View {

    public static final int DURATION_UPDATE_TIME = 1000;
    public static final float DEFAULT_LOCATION_UPDATE_DISTANCE_IN_METERS = 1.0f;
    public static final String ACCEPT_TIME_SEPARATOR = ",";
    public static final String VIA_RESULT_SUCCESS = "0";
    public static final String NONE = "";

    private static final int DURATION_DRAW_ONCE = 16;
    private static final int MSG_UPDATE_PLAY_PROGRESS = 111;
    public static final int STATUS_PLAY = 2;
    public static final int STATUS_READY = 0;
    public static final int STATUS_RECORD = 1;
    public static final int STATUS_SCROLL = 3;
    public int PIXELS_FOR_PER_DATA;
    public int PIXELS_FOR_PER_DATA_RANGE;
    public int PIXELS_FOR_PER_STAFF_RANGE;
    public int PIXELS_FOR_PER_STAFF_VERTICAL_LARGE;
    public int PIXELS_FOR_PER_STAFF_VERTICAL_SMALL;
    public int PIXELS_FOR_STAFF_TEXT_OFFSET_X;
    public int PIXELS_FOR_STAFF_TEXT_OFFSET_Y;
    public int PIXELS_MAX_BOTTOM_Y;
    public int PIXELS_MAX_TOP_Y;
    private int mCurDrawPos;
    private int mCurPlayTime;
    public int mCurStatus;
    private int[] mFirstDownScrollPosAndOffset;
    private Handler mHandler;
    private boolean mHasInitPixelsForPerDataRange;
    private int mLastX;
    private Paint mLinePaint;
    private Paint mMainPaint;
    private MediaPlayer mMediaPlayer;
    private int mOvalRadius;
    private RectF mOvalRectF;
    private PaintTheme mPaintTheme;
    private long mPerDataDuration;
    private Rect mRect;
    private int mSamplingRate;
    private int[] mScrollPosAndOffset;
    private Paint mTextPaint;
    private List<WaveDataUnit> mWaveformData;


    public class WaveDataUnit {
        int readSize;
        double waveData;
    }

    public static class PaintTheme {
        public int mBgColor;
        public int mHorizontalLine2;
        public int mHorzontalLine;
        public int mLineColor;
        public int mPlayedBottomColor;
        public int mPlayedTopColor;
        public int mStaffLineColor;
        public int mStaffTextColor;
        public int mUnplayBottomColor;
        public int mUnplayTopColor;
        public int mVerticalLineColor;

        public PaintTheme() {
            this.mStaffTextColor = -1;
            this.mStaffLineColor = -1;
            this.mPlayedTopColor = -1;
            this.mPlayedBottomColor = -1;
            this.mUnplayTopColor = -1;
            this.mUnplayBottomColor = -1;
            this.mLineColor = -1;
            this.mVerticalLineColor = -1;
            this.mHorzontalLine = -1;
            this.mHorizontalLine2 = -1;
        }
    }

    public AudioRecordVisualizerView(Context context) {
        this(context, null);
    }

    public AudioRecordVisualizerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AudioRecordVisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mCurStatus = STATUS_READY;
        this.mCurDrawPos = -1;
        this.mSamplingRate = MP3Recorder.DEFAULT_SAMPLING_RATE;
        this.mHasInitPixelsForPerDataRange = false;
        this.mPerDataDuration = (long) (DURATION_UPDATE_TIME / this.mSamplingRate);
        this.PIXELS_FOR_STAFF_TEXT_OFFSET_X = 15;
        this.PIXELS_FOR_STAFF_TEXT_OFFSET_Y = 50;
        this.PIXELS_FOR_PER_STAFF_RANGE = 40;
        this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE = 50;
        this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL = 20;
        this.PIXELS_FOR_PER_DATA_RANGE = 20;
        this.PIXELS_FOR_PER_DATA = 15;
        this.PIXELS_MAX_TOP_Y = 380;
        this.PIXELS_MAX_BOTTOM_Y = 280;
        this.mScrollPosAndOffset = new int[STATUS_PLAY];
        this.mFirstDownScrollPosAndOffset = new int[STATUS_PLAY];
        this.mLastX = -1;
        this.mCurPlayTime = STATUS_READY;
        this.mHandler = new Handler();
        init();
    }

    private void init() {
        this.PIXELS_FOR_PER_STAFF_RANGE = this.PIXELS_FOR_PER_DATA_RANGE * 6;
        this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE = (int) ((20.0f * getResources().getDisplayMetrics().density) + 0.5f);
        this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL = (int) ((8.0f * getResources().getDisplayMetrics().density) + 0.5f);
        this.PIXELS_FOR_STAFF_TEXT_OFFSET_X = (int) ((getResources().getDisplayMetrics().density * 3.0f) + 0.5f);
        this.PIXELS_FOR_STAFF_TEXT_OFFSET_Y = (int) ((15.0f * getResources().getDisplayMetrics().density) + 0.5f);
        this.PIXELS_MAX_TOP_Y = (int) ((130.0f * getResources().getDisplayMetrics().density) + 0.5f);
        this.PIXELS_MAX_BOTTOM_Y = (int) ((100.0f * getResources().getDisplayMetrics().density) + 0.5f);
        this.mPaintTheme = new PaintTheme();
        this.mMainPaint = new Paint();
        this.mMainPaint.setAntiAlias(true);
        this.mMainPaint.setColor(-1);
        this.mLinePaint = new Paint();
        this.mLinePaint.setAntiAlias(true);
        this.mLinePaint.setColor(-1);
        this.mLinePaint.setStrokeWidth((float) ((int) ((getResources().getDisplayMetrics().density * DEFAULT_LOCATION_UPDATE_DISTANCE_IN_METERS) + 0.5f)));
        this.mTextPaint = new Paint();
        this.mTextPaint.setAntiAlias(true);
        this.mTextPaint.setColor(-1);
        this.mTextPaint.setStrokeWidth((float) ((int) ((getResources().getDisplayMetrics().density * DEFAULT_LOCATION_UPDATE_DISTANCE_IN_METERS) + 0.5f)));
        this.mTextPaint.setTextSize((float) ((int) ((9.0f * getResources().getDisplayMetrics().density) + 0.5f)));
        this.mRect = new Rect();
        this.mOvalRectF = new RectF();
        this.mOvalRadius = (int) ((getResources().getDisplayMetrics().density * 3.0f) + 0.5f);
        this.mWaveformData = new ArrayList();
    }

    public void setPaintTheme(PaintTheme theme) {
        this.mPaintTheme = theme;
    }

    public void setSamplingRate(int rate) {
        this.mSamplingRate = rate;
    }

    public void addWaveData(double newData, int readSize) {
        WaveDataUnit data = new WaveDataUnit();
        data.waveData = newData;
        data.readSize = readSize;
        synchronized (this.mWaveformData) {
            this.mWaveformData.add(data);
        }
        if (!this.mHasInitPixelsForPerDataRange) {
            int caculatePerDataRange = caculatePixelsOfPerDataRange(readSize);
            if (caculatePerDataRange <= 0) {
                caculatePerDataRange = this.PIXELS_FOR_PER_DATA_RANGE;
            }
            this.PIXELS_FOR_PER_DATA_RANGE = caculatePerDataRange;
            this.PIXELS_FOR_PER_DATA = (int) (((double) this.PIXELS_FOR_PER_DATA_RANGE) * 0.9d);
            this.mHasInitPixelsForPerDataRange = true;
            this.mPerDataDuration = caculateTimeFromReadSize(readSize);
        }
        if (this.mCurDrawPos < this.mWaveformData.size() - 1) {
            this.mCurDrawPos += STATUS_RECORD;
            invalidate();
        }
    }

    private int caculatePixelsOfPerDataRange(int readSize) {
        if (readSize <= 0) {
            return this.PIXELS_FOR_PER_DATA_RANGE;
        }
        return (int) ((((double) readSize) * (1000.0d / ((double) this.mSamplingRate))) * (((double) this.PIXELS_FOR_PER_STAFF_RANGE) / 1000.0d));
    }

    public double getTotalRecordedTime() {
        if (this.mWaveformData == null || this.mWaveformData.size() == 0) {
            return 0.0d;
        }
        int totalReadSize = STATUS_READY;
        synchronized (this.mWaveformData) {
            for (WaveDataUnit item : this.mWaveformData) {
                totalReadSize += item.readSize;
            }
        }
        return (((double) totalReadSize) * 1000.0d) / ((double) this.mSamplingRate);
    }

    public double getCurrentScrollPosition() {
        if (this.mWaveformData == null || this.mWaveformData.size() == 0) {
            return 0.0d;
        }
        int middlePos = this.mScrollPosAndOffset[STATUS_READY];
        int startX = STATUS_READY;
        while (this.mScrollPosAndOffset[STATUS_RECORD] + startX < getWidth() / STATUS_PLAY) {
            middlePos += STATUS_RECORD;
            startX += this.PIXELS_FOR_PER_DATA_RANGE;
        }
        int readSize = STATUS_READY;
        int x = STATUS_READY;
        while (x <= middlePos && x < this.mWaveformData.size()) {
            readSize += ((WaveDataUnit) this.mWaveformData.get(x)).readSize;
            x += STATUS_RECORD;
        }
        return (double) caculateTimeFromReadSize(readSize);
    }

    public void clearWaveData() {
//        postDelayed(new 1(this), 500); 
    }

    public String getWaveformSamples(double waveFormSampleRate) {
        double currentSampleRate = getSampleRate() / 1000.0d;
        WaveDataUnit[] waves = new WaveDataUnit[Math.max(STATUS_RECORD, (int) ((currentSampleRate / waveFormSampleRate) * ((double) this.mWaveformData.size())))];
        for (int i = STATUS_READY; i < waves.length; i += STATUS_RECORD) {
            waves[i] = (WaveDataUnit) this.mWaveformData.get(Math.min((int) Math.round(((double) i) / (currentSampleRate / waveFormSampleRate)), this.mWaveformData.size() - 1));
        }
        StringBuilder sb = new StringBuilder();
        WaveDataUnit[] arr$ = waves;
        int len$ = arr$.length;
        for (int i$ = STATUS_READY; i$ < len$; i$ += STATUS_RECORD) {
            sb.append(arr$[i$].waveData + ACCEPT_TIME_SEPARATOR);
        }
        if (sb.length() <= 0) {
            return NONE;
        }
        return sb.substring(STATUS_READY, sb.length() - 1);
    }

    public double getSampleRate() {
        return getTotalRecordedTime() / ((double) this.mWaveformData.size());
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        switch (this.mCurStatus) {
            case STATUS_READY /*0*/:
            case STATUS_RECORD /*1*/:
                myRecordDraw(canvas);
                break;
            case STATUS_PLAY /*2*/:
                myPlayDraw(canvas);
                break;
            case STATUS_SCROLL /*3*/:
                myScrollDraw(canvas);
                break;
            default:
        }
    }

    public void setIsRecording(boolean isRecording) {
        if (isRecording) {
            this.mCurStatus = STATUS_RECORD;
        } else {
            this.mCurStatus = STATUS_SCROLL;
        }
        if (this.mCurStatus == STATUS_SCROLL) {
            int[] indexAndOffset = new int[STATUS_PLAY];
            indexAndOffset[STATUS_READY] = (getWidth() / STATUS_PLAY) / this.PIXELS_FOR_PER_DATA_RANGE;
            indexAndOffset[STATUS_RECORD] = (getWidth() / STATUS_PLAY) % this.PIXELS_FOR_PER_DATA_RANGE;
            if (this.mCurDrawPos > indexAndOffset[STATUS_READY]) {
                this.mScrollPosAndOffset[STATUS_READY] = this.mCurDrawPos - indexAndOffset[STATUS_READY];
                this.mScrollPosAndOffset[STATUS_RECORD] = indexAndOffset[STATUS_RECORD];
                return;
            }
            this.mScrollPosAndOffset[STATUS_READY] = STATUS_READY;
            this.mScrollPosAndOffset[STATUS_RECORD] = STATUS_READY;
            return;
        }
        this.mScrollPosAndOffset[STATUS_READY] = STATUS_READY;
        this.mScrollPosAndOffset[STATUS_RECORD] = STATUS_READY;
    }

    public boolean isRecording() {
        return this.mCurStatus == STATUS_RECORD;
    }

    public int getCurrentStatus() {
        return this.mCurStatus;
    }

    private void drawHorzontalLine(Canvas canvas) {
        this.mLinePaint.setColor(this.mPaintTheme.mHorzontalLine);
        this.mLinePaint.setStrokeWidth((float) ((int) ((((double) getResources().getDisplayMetrics().density) * 0.5d) + 0.5d)));
        canvas.drawLine(0.0f, (float) (((getHeight() + this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE) + this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL) / STATUS_PLAY), (float) getWidth(), (float) (((getHeight() + this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE) + this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL) / STATUS_PLAY), this.mLinePaint);
        this.mLinePaint.setColor(this.mPaintTheme.mHorizontalLine2);
        this.mLinePaint.setStrokeWidth((float) ((int) ((DEFAULT_LOCATION_UPDATE_DISTANCE_IN_METERS * getResources().getDisplayMetrics().density) + 0.5f)));
        canvas.drawLine(0.0f, ((float) (((getHeight() + this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE) + this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL) / STATUS_PLAY)) - this.mLinePaint.getStrokeWidth(), (float) getWidth(), ((float) (((getHeight() + this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE) + this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL) / STATUS_PLAY)) - this.mLinePaint.getStrokeWidth(), this.mLinePaint);
        Canvas canvas2 = canvas;
        canvas2.drawLine(0.0f, this.mLinePaint.getStrokeWidth() + ((float) (((getHeight() + this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE) + this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL) / STATUS_PLAY)), (float) getWidth(), this.mLinePaint.getStrokeWidth() + ((float) (((getHeight() + this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE) + this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL) / STATUS_PLAY)), this.mLinePaint);
    }

    private void drawVerticalLine(Canvas canvas, int xPos) {
        this.mLinePaint.setColor(this.mPaintTheme.mVerticalLineColor);
        this.mLinePaint.setStrokeWidth(getResources().getDisplayMetrics().density + 0.5f);
        canvas.drawLine((float) xPos, (float) ((this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE + this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL) - this.mOvalRadius), (float) xPos, (float) getHeight(), this.mLinePaint);
        this.mOvalRectF.setEmpty();
        this.mOvalRectF.set((float) (xPos - this.mOvalRadius), (float) ((this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE + this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL) - (this.mOvalRadius * STATUS_PLAY)), (float) (this.mOvalRadius + xPos), (float) (this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE + this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL));
        canvas.drawOval(this.mOvalRectF, this.mLinePaint);
        this.mOvalRectF.bottom = (float) getHeight();
        this.mOvalRectF.top = (float) (getHeight() - (this.mOvalRadius * STATUS_PLAY));
        canvas.drawOval(this.mOvalRectF, this.mLinePaint);
    }

    private void drawStaff(Canvas canvas, int startX, int offsetX, long startTime) {
        int staffStartY = this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE + this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL;
        this.mLinePaint.setColor(this.mPaintTheme.mStaffLineColor);
        canvas.drawLine(0.0f, (float) staffStartY, (float) getWidth(), (float) staffStartY, this.mLinePaint);
        int staffStartX = startX;
        int startStaffTime = (int) startTime;
        while (staffStartX < getWidth() + this.PIXELS_FOR_PER_STAFF_RANGE) {
            canvas.drawLine((float) (staffStartX - offsetX), (float) staffStartY, (float) (staffStartX - offsetX), (float) (staffStartY - this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE), this.mLinePaint);
            this.mTextPaint.setColor(this.mPaintTheme.mStaffTextColor);
            if (startStaffTime >= 0) {
                canvas.drawText(formatStaffTime(startStaffTime), (float) ((staffStartX - offsetX) + this.PIXELS_FOR_STAFF_TEXT_OFFSET_X), (float) this.PIXELS_FOR_STAFF_TEXT_OFFSET_Y, this.mTextPaint);
            }
            int perDistance = this.PIXELS_FOR_PER_STAFF_RANGE / 4;
            canvas.drawLine((float) ((staffStartX - offsetX) + perDistance), (float) staffStartY, (float) ((staffStartX - offsetX) + perDistance), (float) (staffStartY - this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL), this.mLinePaint);
            canvas.drawLine((float) ((staffStartX - offsetX) + (perDistance * STATUS_PLAY)), (float) staffStartY, (float) ((staffStartX - offsetX) + (perDistance * STATUS_PLAY)), (float) (staffStartY - this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL), this.mLinePaint);
            canvas.drawLine((float) ((staffStartX - offsetX) + (perDistance * STATUS_SCROLL)), (float) staffStartY, (float) ((staffStartX - offsetX) + (perDistance * STATUS_SCROLL)), (float) (staffStartY - this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL), this.mLinePaint);
            staffStartX += this.PIXELS_FOR_PER_STAFF_RANGE;
            startStaffTime += STATUS_RECORD;
        }
        if (this.mCurStatus == STATUS_PLAY) {
            staffStartX = startX;
            startStaffTime = (int) startTime;
            while (staffStartX > 0) {
                canvas.drawLine((float) (staffStartX - offsetX), (float) staffStartY, (float) (staffStartX - offsetX), (float) (staffStartY - this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE), this.mLinePaint);
                if (startStaffTime >= 0) {
                    canvas.drawText(formatStaffTime(startStaffTime), (float) ((staffStartX - offsetX) + this.PIXELS_FOR_STAFF_TEXT_OFFSET_X), (float) this.PIXELS_FOR_STAFF_TEXT_OFFSET_Y, this.mTextPaint);
                }
                int perDistance = this.PIXELS_FOR_PER_STAFF_RANGE / 4;
                canvas.drawLine((float) ((staffStartX - offsetX) + perDistance), (float) staffStartY, (float) ((staffStartX - offsetX) + perDistance), (float) (staffStartY - this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL), this.mLinePaint);
                canvas.drawLine((float) ((staffStartX - offsetX) + (perDistance * STATUS_PLAY)), (float) staffStartY, (float) ((staffStartX - offsetX) + (perDistance * STATUS_PLAY)), (float) (staffStartY - this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL), this.mLinePaint);
                canvas.drawLine((float) ((staffStartX - offsetX) + (perDistance * STATUS_SCROLL)), (float) staffStartY, (float) ((staffStartX - offsetX) + (perDistance * STATUS_SCROLL)), (float) (staffStartY - this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL), this.mLinePaint);
                staffStartX -= this.PIXELS_FOR_PER_STAFF_RANGE;
                startStaffTime--;
            }
        }
    }

    private void drawDataRect(Canvas canvas, int left, int top, int right, int bottom, int color) {
        this.mMainPaint.setColor(color);
        this.mRect.setEmpty();
        this.mRect.left = left;
        this.mRect.right = right;
        this.mRect.top = top;
        this.mRect.bottom = bottom;
        canvas.drawRect(this.mRect, this.mMainPaint);
    }

    private String formatStaffTime(int totalSec) {
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return (min < 10 ? VIA_RESULT_SUCCESS + min : min + NONE) + ":" + (sec < 10 ? VIA_RESULT_SUCCESS + sec : sec + NONE);
    }

    private void myRecordDraw(Canvas canvas) {
        int staffOfHalfWidth = (getWidth() / STATUS_PLAY) / this.PIXELS_FOR_PER_STAFF_RANGE;
        long startTime = 0;
        int offsetX = STATUS_READY;
        long usedTime = (long) getTotalRecordedTime();
        if (usedTime >= ((long) (staffOfHalfWidth * DURATION_UPDATE_TIME))) {
            startTime = (usedTime - ((long) (staffOfHalfWidth * DURATION_UPDATE_TIME))) / 1000;
            offsetX = (int) (((((double) usedTime) % 1000.0d) / 1000.0d) * ((double) this.PIXELS_FOR_PER_STAFF_RANGE));
        }
        drawStaff(canvas, STATUS_READY, offsetX, startTime);
        int startY = ((getHeight() + this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE) + this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL) / STATUS_PLAY;
        int verticalLinePos = STATUS_READY;
        if (this.mWaveformData.size() > 0) {
            int startDataIndex = STATUS_READY;
            if (this.mCurDrawPos > 0) {
                int offset = (getWidth() / STATUS_PLAY) / this.PIXELS_FOR_PER_DATA_RANGE;
                if (this.mCurDrawPos > offset) {
                    startDataIndex = this.mCurDrawPos - offset;
                }
            }
            int startX;
            int left;
            int right;
            int bottom;
            Canvas canvas2;
            int top;
            if (startDataIndex > 0) {
                startX = STATUS_READY;
                while (startX < (getWidth() / STATUS_PLAY) - this.PIXELS_FOR_PER_DATA_RANGE && startDataIndex < this.mWaveformData.size()) {
                    if (startDataIndex >= this.mWaveformData.size()) {
                        startDataIndex = this.mWaveformData.size() - 1;
                    }
                    left = startX;
                    if (left < 0) {
                        left = STATUS_READY;
                        right = STATUS_READY + this.PIXELS_FOR_PER_DATA;
                    } else {
                        right = left + this.PIXELS_FOR_PER_DATA;
                    }
                    bottom = startY;
                    canvas2 = canvas;
                    drawDataRect(canvas2, left, bottom - ((int) (((WaveDataUnit) this.mWaveformData.get(startDataIndex)).waveData * ((double) this.PIXELS_MAX_TOP_Y))), right, bottom, this.mPaintTheme.mPlayedTopColor);
                    top = bottom;
                    canvas2 = canvas;
                    drawDataRect(canvas2, left, top, right, top + ((int) (((WaveDataUnit) this.mWaveformData.get(startDataIndex)).waveData * ((double) this.PIXELS_MAX_BOTTOM_Y))), this.mPaintTheme.mPlayedBottomColor);
                    startDataIndex += STATUS_RECORD;
                    startX += this.PIXELS_FOR_PER_DATA_RANGE;
                }
                verticalLinePos = startX;
            } else {
                startX = STATUS_READY;
                while (startX < (getWidth() / STATUS_PLAY) - this.PIXELS_FOR_PER_DATA_RANGE && startDataIndex < this.mWaveformData.size()) {
                    left = startX;
                    right = left + this.PIXELS_FOR_PER_DATA;
                    bottom = startY;
                    canvas2 = canvas;
                    drawDataRect(canvas2, left, bottom - ((int) (((WaveDataUnit) this.mWaveformData.get(startDataIndex)).waveData * ((double) this.PIXELS_MAX_TOP_Y))), right, bottom, this.mPaintTheme.mPlayedTopColor);
                    top = bottom;
                    canvas2 = canvas;
                    drawDataRect(canvas2, left, top, right, top + ((int) (((WaveDataUnit) this.mWaveformData.get(startDataIndex)).waveData * ((double) this.PIXELS_MAX_BOTTOM_Y))), this.mPaintTheme.mPlayedBottomColor);
                    startDataIndex += STATUS_RECORD;
                    startX += this.PIXELS_FOR_PER_DATA_RANGE;
                }
                verticalLinePos = startX;
            }
        }
        drawHorzontalLine(canvas);
        drawVerticalLine(canvas, verticalLinePos);
    }

    private void myScrollDraw(Canvas canvas) {
        long usedTime;
        long startTime = 0;
        int offsetX = STATUS_READY;
        if (this.mScrollPosAndOffset[STATUS_READY] >= 0) {
            int curTotalSize = STATUS_READY;
            for (int i = STATUS_READY; i < this.mScrollPosAndOffset[STATUS_READY]; i += STATUS_RECORD) {
                curTotalSize += ((WaveDataUnit) this.mWaveformData.get(this.mScrollPosAndOffset[STATUS_READY])).readSize;
            }
            usedTime = caculateTimeFromReadSize(curTotalSize);
        } else {
            long j = (long) this.PIXELS_FOR_PER_STAFF_RANGE;
            usedTime = (((long) this.mScrollPosAndOffset[STATUS_READY]) * this.mPerDataDuration) + ((((long) this.mScrollPosAndOffset[STATUS_RECORD]) * this.mPerDataDuration) / j);
        }
        if (usedTime >= 0) {
            startTime = usedTime / 1000;
            offsetX = (int) (((((double) usedTime) % 1000.0d) / 1000.0d) * ((double) this.PIXELS_FOR_PER_STAFF_RANGE));
        } else if (usedTime < 0) {
            startTime = usedTime / 1000;
            offsetX = (int) (((((double) usedTime) % 1000.0d) / 1000.0d) * ((double) this.PIXELS_FOR_PER_STAFF_RANGE));
            if (offsetX < 0) {
                startTime--;
                offsetX += this.PIXELS_FOR_PER_STAFF_RANGE;
            }
        }
        drawStaff(canvas, STATUS_READY, offsetX, startTime);
        int startY = ((getHeight() + this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE) + this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL) / STATUS_PLAY;
        if (this.mWaveformData.size() > 0) {
            int startDataIndex = this.mScrollPosAndOffset[STATUS_READY];
            int scrollStartX = STATUS_READY;
            int scrollStartDataIndex = startDataIndex;
            while (scrollStartX < getWidth() - this.PIXELS_FOR_PER_DATA_RANGE && scrollStartDataIndex < this.mWaveformData.size()) {
                if (scrollStartDataIndex >= 0) {
                    int right;
                    if (scrollStartDataIndex >= this.mWaveformData.size()) {
                        scrollStartDataIndex = this.mWaveformData.size() - 1;
                    }
                    int left = scrollStartX - this.mScrollPosAndOffset[STATUS_RECORD];
                    if (left < 0) {
                        left = STATUS_READY;
                        right = STATUS_READY + this.PIXELS_FOR_PER_DATA;
                    } else {
                        right = left + this.PIXELS_FOR_PER_DATA;
                    }
                    int bottom = startY;
                    drawDataRect(canvas, left, bottom - ((int) (((WaveDataUnit) this.mWaveformData.get(scrollStartDataIndex)).waveData * ((double) this.PIXELS_MAX_TOP_Y))), right, bottom, this.mPaintTheme.mPlayedTopColor);
                    int top = bottom;
                    drawDataRect(canvas, left, top, right, top + ((int) (((WaveDataUnit) this.mWaveformData.get(scrollStartDataIndex)).waveData * ((double) this.PIXELS_MAX_BOTTOM_Y))), this.mPaintTheme.mPlayedBottomColor);
                }
                scrollStartDataIndex += STATUS_RECORD;
                scrollStartX += this.PIXELS_FOR_PER_DATA_RANGE;
            }
        }
        drawHorzontalLine(canvas);
        drawVerticalLine(canvas, getWidth() / STATUS_PLAY);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mCurStatus != STATUS_SCROLL) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            case STATUS_READY /*0*/:
                this.mLastX = (int) event.getX();
                this.mFirstDownScrollPosAndOffset[STATUS_READY] = this.mScrollPosAndOffset[STATUS_READY];
                this.mFirstDownScrollPosAndOffset[STATUS_RECORD] = this.mScrollPosAndOffset[STATUS_RECORD];
                return true;
            case STATUS_RECORD /*1*/:
                this.mLastX = -1;
                return true;
            case STATUS_PLAY /*2*/:
                moveTo((int) event.getX());
                return true;
            default:
                return true;
        }
    }

    private void moveTo(int eventX) {
        int[] minScrollPosAndOffset = new int[STATUS_PLAY];
        minScrollPosAndOffset[STATUS_READY] = (getWidth() / STATUS_PLAY) / this.PIXELS_FOR_PER_DATA_RANGE;
        minScrollPosAndOffset[STATUS_RECORD] = (getWidth() / STATUS_PLAY) % this.PIXELS_FOR_PER_DATA_RANGE;
        minScrollPosAndOffset[STATUS_READY] = (-minScrollPosAndOffset[STATUS_READY]) - 1;
        minScrollPosAndOffset[STATUS_RECORD] = this.PIXELS_FOR_PER_DATA_RANGE - minScrollPosAndOffset[STATUS_RECORD];
        int[] maxScrollPosAndOffset = new int[STATUS_PLAY];
        maxScrollPosAndOffset[STATUS_READY] = (getWidth() / STATUS_PLAY) / this.PIXELS_FOR_PER_DATA_RANGE;
        maxScrollPosAndOffset[STATUS_RECORD] = (getWidth() / STATUS_PLAY) % this.PIXELS_FOR_PER_DATA_RANGE;
        if (maxScrollPosAndOffset[STATUS_RECORD] > 0) {
            maxScrollPosAndOffset[STATUS_READY] = maxScrollPosAndOffset[STATUS_READY] + STATUS_RECORD;
        }
        int xOffset = (eventX - this.mLastX) % this.PIXELS_FOR_PER_DATA_RANGE;
        this.mScrollPosAndOffset[STATUS_READY] = this.mFirstDownScrollPosAndOffset[STATUS_READY] - ((eventX - this.mLastX) / this.PIXELS_FOR_PER_DATA_RANGE);
        this.mScrollPosAndOffset[STATUS_RECORD] = this.mFirstDownScrollPosAndOffset[STATUS_RECORD] - xOffset;
        if (this.mScrollPosAndOffset[STATUS_RECORD] < 0) {
            int[] iArr = this.mScrollPosAndOffset;
            iArr[STATUS_READY] = iArr[STATUS_READY] - 1;
            iArr = this.mScrollPosAndOffset;
            iArr[STATUS_RECORD] = iArr[STATUS_RECORD] + this.PIXELS_FOR_PER_DATA_RANGE;
        }
        if (this.mScrollPosAndOffset[STATUS_RECORD] > this.PIXELS_FOR_PER_DATA_RANGE) {
            int[] iArr = this.mScrollPosAndOffset;
            iArr[STATUS_READY] = iArr[STATUS_READY] + STATUS_RECORD;
            iArr = this.mScrollPosAndOffset;
            iArr[STATUS_RECORD] = iArr[STATUS_RECORD] - this.PIXELS_FOR_PER_DATA_RANGE;
        }
        if (this.mScrollPosAndOffset[STATUS_READY] < minScrollPosAndOffset[STATUS_READY]) {
            this.mScrollPosAndOffset[STATUS_READY] = minScrollPosAndOffset[STATUS_READY];
            this.mScrollPosAndOffset[STATUS_RECORD] = minScrollPosAndOffset[STATUS_RECORD];
        } else if (this.mScrollPosAndOffset[STATUS_READY] >= this.mWaveformData.size() - maxScrollPosAndOffset[STATUS_READY]) {
            this.mScrollPosAndOffset[STATUS_READY] = (this.mWaveformData.size() - maxScrollPosAndOffset[STATUS_READY]) - 1;
            this.mScrollPosAndOffset[STATUS_RECORD] = STATUS_READY;
        }
        invalidate();
    }

    public boolean isAudioPlayStatus() {
        return this.mCurStatus == STATUS_PLAY;
    }

    public boolean isPlaying() {
        return this.mMediaPlayer == null ? false : this.mMediaPlayer.isPlaying();
    }

    public boolean toggleAudioPlay() {
        if (this.mMediaPlayer.isPlaying()) {
            this.mMediaPlayer.pause();
            this.mHandler.removeMessages(MSG_UPDATE_PLAY_PROGRESS);
        } else {
            this.mMediaPlayer.start();
            this.mHandler.removeMessages(MSG_UPDATE_PLAY_PROGRESS);
            this.mHandler.sendEmptyMessage(MSG_UPDATE_PLAY_PROGRESS);
        }
        return this.mMediaPlayer.isPlaying();
    }

    public void playAudio(MediaPlayer player) {
        if (this.mCurStatus != STATUS_RECORD) {
            this.mMediaPlayer = player;
            this.mCurStatus = STATUS_PLAY;
            this.mHandler.removeMessages(MSG_UPDATE_PLAY_PROGRESS);
            this.mHandler.sendEmptyMessage(MSG_UPDATE_PLAY_PROGRESS);
        }
    }

    public void stopAudio() {
        this.mCurStatus = STATUS_SCROLL;
        this.mHandler.removeMessages(MSG_UPDATE_PLAY_PROGRESS);
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.stop();
        }
        this.mMediaPlayer = null;
    }

    private void myPlayDraw(Canvas canvas) {
        int staffOffsetX = (int) (((((double) this.mCurPlayTime) % 1000.0d) / 1000.0d) * ((double) this.PIXELS_FOR_PER_STAFF_RANGE));
        Canvas canvas2 = canvas;
        drawStaff(canvas2, getWidth() / STATUS_PLAY, staffOffsetX, (long) (this.mCurPlayTime / DURATION_UPDATE_TIME));
        int startY = ((getHeight() + this.PIXELS_FOR_PER_STAFF_VERTICAL_LARGE) + this.PIXELS_FOR_PER_STAFF_VERTICAL_SMALL) / STATUS_PLAY;
        if (this.mWaveformData.size() > 0) {
            int[] startDataIndexAndOffset = new int[STATUS_PLAY];
            caculateIndexAndOffsetFromTime(this.mCurPlayTime, startDataIndexAndOffset);
            int startDataIndex = startDataIndexAndOffset[STATUS_READY];
            int unitOffsetX = startDataIndexAndOffset[STATUS_RECORD];
            if (startDataIndex >= 0) {
                int left;
                int right;
                int bottom;
                int top;
                int startX = getWidth() / STATUS_PLAY;
                int startIndex = startDataIndex;
                while (startX < getWidth() - this.PIXELS_FOR_PER_DATA_RANGE && startIndex <= this.mCurDrawPos) {
                    if (startIndex >= this.mWaveformData.size()) {
                        startIndex = this.mWaveformData.size() - 1;
                    }
                    left = startX - unitOffsetX;
                    if (left < 0) {
                        left = STATUS_READY;
                        right = STATUS_READY + this.PIXELS_FOR_PER_DATA;
                    } else {
                        right = left + this.PIXELS_FOR_PER_DATA;
                    }
                    bottom = startY;
                    drawDataRect(canvas, left, bottom - ((int) (((WaveDataUnit) this.mWaveformData.get(startIndex)).waveData * ((double) this.PIXELS_MAX_TOP_Y))), right, bottom, this.mPaintTheme.mPlayedTopColor);
                    top = bottom;
                    drawDataRect(canvas, left, top, right, top + ((int) (((WaveDataUnit) this.mWaveformData.get(startIndex)).waveData * ((double) this.PIXELS_MAX_BOTTOM_Y))), this.mPaintTheme.mPlayedBottomColor);
                    startIndex += STATUS_RECORD;
                    startX += this.PIXELS_FOR_PER_DATA_RANGE;
                }
                startX = getWidth() / STATUS_PLAY;
                startIndex = startDataIndex;
                while (startX > 0 && startIndex >= 0) {
                    if (startIndex < 0) {
                        startIndex = STATUS_READY;
                    }
                    if (startIndex >= this.mWaveformData.size()) {
                        startIndex = this.mWaveformData.size() - 1;
                    }
                    left = (startX - unitOffsetX) - this.PIXELS_FOR_PER_DATA_RANGE;
                    right = left + this.PIXELS_FOR_PER_DATA;
                    bottom = startY;
                    drawDataRect(canvas, left, bottom - ((int) (((WaveDataUnit) this.mWaveformData.get(startIndex)).waveData * ((double) this.PIXELS_MAX_TOP_Y))), right, bottom, this.mPaintTheme.mPlayedTopColor);
                    top = bottom;
                    drawDataRect(canvas, left, top, right, top + ((int) (((WaveDataUnit) this.mWaveformData.get(startIndex)).waveData * ((double) this.PIXELS_MAX_BOTTOM_Y))), this.mPaintTheme.mPlayedBottomColor);
                    startIndex--;
                    startX -= this.PIXELS_FOR_PER_DATA_RANGE;
                }
            }
        }
        drawHorzontalLine(canvas);
        drawVerticalLine(canvas, getWidth() / STATUS_PLAY);
    }

    private long caculateTimeFromReadSize(int readSize) {
        return (long) ((((double) readSize) * 1000.0d) / ((double) this.mSamplingRate));
    }

    private void caculateIndexAndOffsetFromTime(int curPlayTime, int[] dataIndexAndOffset) {
        if (this.mWaveformData != null && this.mWaveformData.size() > 0) {
            int totalReadSize = STATUS_READY;
            for (WaveDataUnit item : this.mWaveformData) {
                totalReadSize += item.readSize;
            }
            long duration = (long) ((((double) totalReadSize) * 1000.0d) / ((double) this.mSamplingRate));
            dataIndexAndOffset[STATUS_READY] = (int) (((long) curPlayTime) / (duration / ((long) this.mWaveformData.size())));
            dataIndexAndOffset[STATUS_RECORD] = ((int) ((((long) curPlayTime) % (duration / ((long) this.mWaveformData.size()))) / (duration / ((long) this.mWaveformData.size())))) * this.PIXELS_FOR_PER_DATA_RANGE;
        }
    }
}