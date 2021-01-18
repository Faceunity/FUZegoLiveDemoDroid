package im.zego.soundlevelandspectrum.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.Random;

import im.zego.common.util.DeviceInfoManager;
import im.zego.soundlevelandspectrum.R;

public class SpectrumView extends View {

    private Context mContext;
    private Paint mPaint;
    private int mPaintColor;
    private float mStrokeWidth;
    private float mHeight, mWidth;
    private float mPadding;
    private boolean running = true;

    private Thread mThread;

    public SpectrumView(Context context) {
        this(context, null);
    }

    public SpectrumView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpectrumView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.BeatLoadView);
        mPaintColor = typedArray.getColor(R.styleable.BeatLoadView_paintColor, Color.GRAY);
        mHeight = typedArray.getDimension(R.styleable.BeatLoadView_itemHeight, dp2px(20));
        mStrokeWidth = typedArray.getDimension(R.styleable.BeatLoadView_strokeWidth, dp2px(2));
        mPadding = typedArray.getDimension(R.styleable.BeatLoadView_itemsPadding, dp2px(4));

        mWidth = DeviceInfoManager.getScreenWidth(context);

        mStrokeWidth = (mWidth / 64);

        typedArray.recycle();
        initPaint();
    }


    private void initPaint() {
        mPaint = new Paint();
        mPaint.setColor(mPaintColor);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setStyle(Paint.Style.FILL);

        mThread = new Thread() {
            @Override
            public void run() {
                while (true) {

                    if (running)
                        postInvalidate();

                    try {
                        sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        };

        mThread.start();

        for (int i = 0; i < maxLine; i++) {
            color[i] = getColor();
        }
    }

    int[] color = new int[64];
    int maxLine = 64;
    float stopTmp;
    float[] frequencySpectrums = new float[64];

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0; i < maxLine; i++) {
            double stopY = (getStopY(frequencySpectrums[i]));
            mPaint.setColor(color[i]);
            canvas.drawLine(mStrokeWidth * i, mHeight, mStrokeWidth * i, (float) (mHeight - (stopY)), mPaint);
        }
    }

    private int getColor() {
        Random random = new Random();
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return Color.rgb(r, g, b);
    }

    private double getStopY(double frequencySpectrum) {
        double value = frequencySpectrum < 0 ? 0 : frequencySpectrum;
        value = value >= 0 ? value : -value;
        double itemH;
        if (value > 10) {
            itemH = (double) (Math.log(value) / 20 * mHeight);
        } else {
            itemH = value / 10;
        }
        return itemH;
    }


    private DecelerateInterpolator[] decelerateInterpolator = new DecelerateInterpolator[64];


    private float dp2px(int dp) {
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        return displayMetrics.density * dp;
    }


    public void updateFrequencySpectrum(float[] frequencySpectrumList) {
        frequencySpectrums = frequencySpectrumList;

    }
}
