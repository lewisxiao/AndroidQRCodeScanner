package com.lewisxiao.qrcodescanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.CameraPreview;
import com.journeyapps.barcodescanner.ViewfinderView;

import java.util.ArrayList;
import java.util.List;

public class CustomViewfinderView extends ViewfinderView {
    protected static final String TAG = ViewfinderView.class.getSimpleName();

    public CustomViewfinderView(Context context, AttributeSet attrs, Paint paint, int maskColor, int resultColor, int laserColor, int resultPointColor) {
        super(context, attrs);
        this.paint = paint;
        this.maskColor = maskColor;
        this.resultColor = resultColor;
        this.laserColor = laserColor;
        this.resultPointColor = resultPointColor;
    }

    protected static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    protected static final long ANIMATION_DELAY = 50L;
    protected static final int CURRENT_POINT_OPACITY = 0xA0;
    protected static final int MAX_RESULT_POINTS = 20;
    protected static final int POINT_SIZE = 6;

    protected final Paint paint;
    protected Bitmap resultBitmap;
    protected final int maskColor;
    protected final int resultColor;
    protected final int laserColor;
    protected final int resultPointColor;
    protected int scannerAlpha;
    protected List<ResultPoint> possibleResultPoints;
    protected List<ResultPoint> lastPossibleResultPoints;
    protected CameraPreview cameraPreview;

    // Cache the framingRect and previewFramingRect, so that we can still draw it after the preview
    // stopped.
    protected Rect framingRect;
    protected Rect previewFramingRect;

    // This constructor is used when the class is built from an XML resource.
    public CustomViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Resources resources = getResources();

        // Get setted attributes on view
        TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.zxing_finder);

        this.maskColor = attributes.getColor(R.styleable.zxing_finder_zxing_viewfinder_mask,
                resources.getColor(R.color.zxing_viewfinder_mask));
        this.resultColor = attributes.getColor(R.styleable.zxing_finder_zxing_result_view,
                resources.getColor(R.color.zxing_result_view));
        this.laserColor = attributes.getColor(R.styleable.zxing_finder_zxing_viewfinder_laser,
                resources.getColor(R.color.zxing_viewfinder_laser));
        this.resultPointColor = attributes.getColor(R.styleable.zxing_finder_zxing_possible_result_points,
                resources.getColor(R.color.zxing_possible_result_points));

        attributes.recycle();

        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = null;

        // custom init
        customInit(context);
    }


    public void setCameraPreview(CameraPreview view) {
        this.cameraPreview = view;
        view.addStateListener(new CameraPreview.StateListener() {
            @Override
            public void previewSized() {
                refreshSizes();
                invalidate();
            }

            @Override
            public void previewStarted() {

            }

            @Override
            public void previewStopped() {

            }

            @Override
            public void cameraError(Exception error) {

            }

            @Override
            public void cameraClosed() {

            }
        });
    }

    protected void refreshSizes() {
        if (cameraPreview == null) {
            return;
        }
        Rect framingRect = cameraPreview.getFramingRect();
        Rect previewFramingRect = cameraPreview.getPreviewFramingRect();
        if (framingRect != null && previewFramingRect != null) {
            this.framingRect = framingRect;
            this.previewFramingRect = previewFramingRect;
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        refreshSizes();
        if (framingRect == null || previewFramingRect == null) {
            return;
        }

        Rect frame = framingRect;
        Rect previewFrame = previewFramingRect;
        customDraw(frame, canvas);
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {

        // Request another update at the animation interval, but only repaint the laser line,
        // not the entire viewfinder mask.
        postInvalidateDelayed(ANIMATION_DELAY,
                frame.left - POINT_SIZE,
                frame.top - POINT_SIZE,
                frame.right + POINT_SIZE,
                frame.bottom + POINT_SIZE);
        }
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param result An image of the result.
     */
    public void drawResultBitmap(Bitmap result) {
        resultBitmap = result;
        invalidate();
    }

    /**
     * Only call from the UI thread.
     *
     * @param point a point to draw, relative to the preview frame
     */
    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        points.add(point);
        int size = points.size();
        if (size > MAX_RESULT_POINTS) {
            // trim it
            points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
        }
    }

    // properties of edge
    private Paint mLinePaint;
    private final int mLineColor = Color.BLUE;

    // properties of scroll bar
    private Bitmap mLineBm;
    private RectF mLineReact;
    private final int mStepSize = 6;
    private final int mLineHeight = 30;
    private boolean isBottom = false;

    // properties of prompt text
    private Paint mTextPaint;
    private String mPromptText;
    private int mTextMargin;


    private void customInit(Context context) {
        // initialize painter
        mLinePaint = new Paint();
        mLinePaint.setStyle(Paint.Style.FILL);
        mLinePaint.setStrokeWidth(20);
        mLinePaint.setColor(mLineColor);
        // initialize scroll bar
        mLineBm = BitmapFactory.decodeResource(getResources(), R.drawable.lan);
        // initialize prompt message
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(sp2px(14));
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextMargin = sp2px(20);

    }

    public void setPromptText(String text) {
        this.mPromptText = text;
    }

    private void customDraw(Rect frame, Canvas canvas) {
        drawSlipLine(frame, canvas);
        drawEdge(frame, canvas);
        drawPromptText(frame, canvas);
    }

    private void drawSlipLine(Rect frame, Canvas canvas) {
        if (mLineReact == null) {
            mLineReact = new RectF(frame.left + 5, frame.top, frame.right - 5, frame.top + mLineHeight);
        }

        if (isBottom) {
            mLineReact.set(frame.left + 5, frame.top, frame.right - 5, frame.top + mLineHeight);
        }
        mLineReact.offset(0, mStepSize);
        canvas.drawBitmap(mLineBm, null, mLineReact, null);

        isBottom = mLineReact.bottom + mStepSize > frame.bottom;
    }

    private void drawEdge(Rect frame, Canvas canvas) {
        canvas.drawRect(frame.left - 10, frame.top, frame.left, frame.top + 50, mLinePaint);
        canvas.drawRect(frame.left - 10, frame.top - 10, frame.left + 50, frame.top, mLinePaint);
        canvas.drawRect(frame.right - 50, frame.top - 10, frame.right + 10, frame.top, mLinePaint);
        canvas.drawRect(frame.right, frame.top, frame.right + 10, frame.top + 50, mLinePaint);

        canvas.drawRect(frame.left - 10, frame.bottom - 50, frame.left, frame.bottom, mLinePaint);
        canvas.drawRect(frame.left - 10, frame.bottom, frame.left + 50, frame.bottom + 10, mLinePaint);
        canvas.drawRect(frame.right - 50, frame.bottom, frame.right, frame.bottom + 10, mLinePaint);
        canvas.drawRect(frame.right, frame.bottom - 50, frame.right + 10, frame.bottom + 10, mLinePaint);

    }

    private void drawPromptText(Rect frame, Canvas canvas) {
        int startX = frame.left + frame.width() / 2;
        int startY = frame.bottom + mTextMargin;
        if (!TextUtils.isEmpty(mPromptText)) {
            canvas.drawText(mPromptText, startX, startY, mTextPaint);
        }
    }

    private int sp2px(float spValue) {
        final float scale = getContext().getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * scale + 0.5f);
    }
}
