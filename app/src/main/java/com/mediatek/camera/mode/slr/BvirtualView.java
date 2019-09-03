package com.mediatek.camera.mode.slr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import com.android.camera.CameraActivity;
import com.bvirtual.BlurInfo;
import com.bvirtual.SmoothBlurJni;
import com.mediatek.camera.R;
import com.mediatek.camera.ui.Rotatable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BvirtualView extends View implements
        PreviewAreaChangeListener, Rotatable {
    private static final String TAG = "BvirtualView";
    private CameraActivity mCamera;
    private int mOnSingleX;
    private int mOnSingleY;
    private final static int IN_SHARPNESS_RADIUS = 200;
    private final static int OUT_SHARPNESS_RADIUS = 320;
    private final static int REFERENCE_ASPECT_SIZE = 640;
    private final static int SUPPORT_MAX_ASPECT_SIZE = 480;
    private final static boolean SHOW_PREVIEW_DEBUG_LOG = false;
    private RectF mPreviewArea = new RectF();
    private Bitmap mPartBitmap;
    private Matrix mRotationMatrix;
    private Matrix mTranslateMatrix;
    private float mPartRadius;
    private Paint mPaint;
    private PaintFlagsDrawFilter mPFDF;
    private Path mPath;
    private float mSphereSize;
    private float mCenterX;
    private float mCenterY;
    private GestureDetector mGestureDetector;
    private boolean mIsInSeekbarArea;
    private float mTranslateYProgess;
    private Bitmap mSeekbarPoint;
    private Bitmap mCircleOutWhite;
    private Bitmap mCircleOutGreen;
    private float mCircleInRadius;
    private float mCircleOutRadius;
    private int mSlop;
    private boolean mIsFousing;
    private boolean mIsCancelDiaphragm = true;
    private final static int CANCEL_DELAY = 5000;
    private boolean mIsFouseMovingSucceed;
    private int mBlurDegress;
    private int mOrientation;
    private int PREFERENCE_FACTOR = 1000;
    private float mSphereRadiusScale;
    // The value for android.hardware.Camera.setDisplayOrientation.
    private int mCameraDisplayOrientation;
    private RectF mRectF = new RectF();

    public RectF getmPreviewArea() {
        return mPreviewArea;
    }

    private Runnable mCancelDiaphragmRunnable = new Runnable() {
        @Override
        public void run() {
            mIsCancelDiaphragm = true;
            mIsFouseMovingSucceed = false;
        }
    };

    public BvirtualView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mPartRadius = context.getResources().getDimension(R.dimen.freeme_diaphragm_radius);
        BitmapDrawable drawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.freeme_vb_diaphragm_part);
        BitmapDrawable pointDrawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.freeme_seekbar_scroll_point);
        BitmapDrawable outWhiteDrawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.freeme_circle_out_white);
        BitmapDrawable outGreenDrawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.freeme_circle_out_green);
        int color = Color.WHITE;
        mCircleOutGreen = outGreenDrawable.getBitmap();
        mCircleOutWhite = outWhiteDrawable.getBitmap();
        mSeekbarPoint = pointDrawable.getBitmap();
        mPartBitmap = drawable.getBitmap();
        mSphereSize = mSeekbarPoint.getWidth();
        mRotationMatrix = new Matrix();
        mTranslateMatrix = new Matrix();
        mPaint = new Paint();
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);
        mPFDF = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        mPath = new Path();
        mCircleOutRadius = mPartBitmap.getWidth() / 2.0f;
        mCircleInRadius = mCircleOutRadius * 0.75f;
        mPath.addCircle(mCircleOutRadius, mCircleOutRadius, mCircleInRadius, Path.Direction.CCW);
        mGestureDetector = new GestureDetector(context, new GestureListener());
        mBlurDegress = 4;
        mTranslateYProgess = 0.5f;
        mSphereRadiusScale = mSphereSize / 2 / mPartRadius;
    }

    public void setTranslateYProgess(float progess) {
        mTranslateYProgess = progess;
    }

    private void computeCenterCoordinate(float x, float y) {
        int intX = (int) x;
        int intY = (int) y;
        int halfSize = (int) mPartRadius;

        if (intX >= halfSize && intX <= getMeasuredWidth() - halfSize) {
            mCenterX = x;
        } else if (intX < halfSize) {
            mCenterX = halfSize;
        } else {
            mCenterX = getMeasuredWidth() - halfSize;
        }
        if (intY >= halfSize && intY <= getMeasuredHeight() - halfSize) {
            mCenterY = y;
        } else if (intY < halfSize) {
            mCenterY = halfSize;
        } else {
            mCenterY = getMeasuredHeight() - halfSize;
        }
    }

    private boolean isInSeekbarSlideArea(float x, float y) {
        boolean isInArea = false;
        PointF point = compulateSeekbarCoordinate(isScreenPortrait());
        int intX = (int) x;
        int intY = (int) y;
        float size = 3.0f * mSphereSize;
        int leftX = (int) (point.x - size);
        int topY = (int) (point.y - size);
        int rightX = (int) (point.x + size);
        int bottomY = (int) (point.y + size);
        if (intX > leftX && intX < rightX && intY > topY && intY < bottomY) {
            isInArea = true;
        }
        return isInArea;
    }

    private PointF compulateSeekbarCoordinate(boolean isPortait) {
        PointF point = new PointF();
        float progress = mTranslateYProgess;
//        int factorProgress = (int) (mTranslateYProgess * PREFERENCE_FACTOR);
//        int sphere = (int) (mSphereRadiusScale * PREFERENCE_FACTOR);
//        if (factorProgress <= sphere) {
//            progress = mSphereRadiusScale;
//        } else if (factorProgress >= (PREFERENCE_FACTOR - sphere)) {
//            progress = 1 - mSphereRadiusScale;
//       }
        if (isPortait) {
            int span = (int) (getMeasuredWidth() - mCenterX - mPartRadius - mSphereSize);
            if (span < 0) {
                point.x = mCenterX - mPartRadius - mSphereSize / 2;
            } else {
                point.x = mCenterX + mPartRadius + mSphereSize / 2;
            }
            float delta = progress * 2 * mPartRadius;
            point.y = mCenterY - mPartRadius + delta;
        } else {
            int span = (int) (getMeasuredHeight() - (mCenterY + mPartRadius + mSphereSize));
            if (span < 0) {
                point.y = mCenterY - mPartRadius - mSphereSize / 2;
            } else {
                point.y = mCenterY + mPartRadius + mSphereSize / 2;
            }
            float delta = progress * 2 * mPartRadius;
            point.x = mCenterX + mPartRadius - delta;
        }

        return point;
    }

    @Override
    public void setOrientation(int orientation, boolean animation) {
        orientation = orientation % 360;
        if (mOrientation == orientation) {
            return;
        }
        mOrientation = orientation;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private MotionEvent mDown;
        @Override
        public boolean onDown(MotionEvent event) {
            mDown = MotionEvent.obtain(event);
            if (!mIsInSeekbarArea) {
                mIsInSeekbarArea = isInSeekbarSlideArea(event.getX(), event.getY());
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent ev, float distanceX, float distanceY) {
            if (mIsInSeekbarArea) {
                getHandler().removeCallbacks(mCancelDiaphragmRunnable);
                float threshold = Math.min(getMeasuredHeight(), getMeasuredWidth());
                float step = distanceY / threshold;
                boolean isPortait = isScreenPortrait();
                if (isPortait) {
                    if (Math.abs(distanceY) > Math.abs(distanceX)) {
                        mTranslateYProgess -= step * 1.5f;
                        if (mTranslateYProgess < 0f) {
                            mTranslateYProgess = 0f;
                        } else if (mTranslateYProgess > 1f) {
                            mTranslateYProgess = 1f;
                        }
                        invalidate();
                    }
                } else {
                    step = distanceX / threshold;
                    if (Math.abs(distanceX) > Math.abs(distanceY)) {
                        mTranslateYProgess += step * 1.5f;
                        if (mTranslateYProgess < 0f) {
                            mTranslateYProgess = 0f;
                        } else if (mTranslateYProgess > 1f) {
                            mTranslateYProgess = 1f;
                        }
                        invalidate();
                        return true;
                    }
                }
            }
            int deltaX = (int) (ev.getX() - mDown.getX());
            int deltaY = (int) (ev.getY() - mDown.getY());
            if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
                if (Math.abs(deltaX) > mSlop || Math.abs(deltaY) > mSlop) {
                    // Calculate the direction of the swipe.
                   /* if (deltaX >= Math.abs(deltaY)) {
                        // Swipe right.
                        MainActivityLayout appRootView = mCamera.getCameraAppUI().getAppRootView();
                        if (appRootView != null) {
                            appRootView.redirectTouchEventsTo(mCamera.getSceneModeRoot());
                        }
                    } else if (deltaX <= -Math.abs(deltaY)) {
                        // Swipe left.
                    }*/
                }
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            computeCenterCoordinate(e.getX(), e.getY());
            mCamera.onSingleTapUp(null, (int) mCenterX, (int) mCenterY);
           /* mIsFousing = true;
            mIsCancelDiaphragm = false;*/
            mIsInSeekbarArea = false;
            return true;
        }
    }

    public void onAutoFocus(boolean focused) {
        mIsFousing = false;
        getHandler().removeCallbacks(mCancelDiaphragmRunnable);
        getHandler().postDelayed(mCancelDiaphragmRunnable, CANCEL_DELAY);
        invalidate();
    }

    public void onAutoFocusMoving(boolean moving) {
        if (mIsFouseMovingSucceed) {
            return;
        }
        mIsFousing = moving;
        mIsCancelDiaphragm = false;
        getHandler().removeCallbacks(mCancelDiaphragmRunnable);
        if (!mIsFousing) {
            mIsFouseMovingSucceed = true;
            getHandler().postDelayed(mCancelDiaphragmRunnable, CANCEL_DELAY);
        }
        invalidate();
    }

    public void setCameraDisplayOrientation(int orientation) {
        mCameraDisplayOrientation = orientation;
    }

    public void setFaces(Camera.Face[] faces) {
        if (faces != null && faces.length > 0) {
            int rw = (int) mPreviewArea.width();
            int rh = (int) mPreviewArea.height();
            if (((rh > rw) && ((mCameraDisplayOrientation == 0) || (mCameraDisplayOrientation == 180)))
                    || ((rw > rh) && ((mCameraDisplayOrientation == 90) || (mCameraDisplayOrientation == 270)))) {
                int temp = rw;
                rw = rh;
                rh = temp;
            }
            boolean isFontCamera = mCamera.getCurrentCameraId() == 1 ? true : false;
            CameraUtil.prepareMatrix(mRotationMatrix, isFontCamera, mCameraDisplayOrientation, rw, rh);
            mRectF.set(faces[0].rect);
            mRotationMatrix.mapRect(mRectF);
            mRectF.offset(mPreviewArea.left, mPreviewArea.top);
            float x = mRectF.centerX();
            float y = mRectF.centerY() - mRectF.height() / 5.8f;
            computeCenterCoordinate(x, y);
            onSingleTapUp((int) mCenterX, (int) mCenterY);
        }
    }

    public void onSingleTapUp(int x, int y) {
        mOnSingleX = x;
        mOnSingleY = y;
        invalidate();
    }

    public void init(CameraActivity cameraActivity) {
        mCamera = cameraActivity;
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        mPreviewArea.set(previewArea);
        ViewParent viewParent = getParent();
        viewParent.requestLayout();
    }

    public static class SharpnessRect {
        public int top;
        public int bottom;
        public int left;
        public int right;

        public SharpnessRect(int x, int y, int r, int bitmapW, int bitmapH) {
            top = y - r;
            left = x - r;
            bottom = y + r;
            right = x + r;
            if (top < 0 || top > bitmapH) {
                top = 0;
            }
            if (left < 0 || left > bitmapW) {
                left = 0;
            }
            if (bottom > bitmapH) {
                bottom = bitmapH;
            }
            if (right > bitmapW) {
                right = bitmapW;
            }
        }

        public int getWidth() {
            return right - left;
        }

        public int getHeight() {
            return bottom - top;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append("left:").append(left).append(",top:").append(top).append(",right:").append(right)
                    .append(",bottom:").append(bottom).append(",width:").append(getWidth())
                    .append(",height:").append(getHeight());
            return str.toString();
        }
    }

    private void drawTrueBgVirtualWithCanvas(Canvas canvas) {
        if (mPreviewArea == null) {
            return;
        }
        long time0 = System.currentTimeMillis();
        int oriSize = Math.min((int) mPreviewArea.width(), (int) mPreviewArea.height());
        boolean useTransform = false;
        float scale = oriSize / (float) (REFERENCE_ASPECT_SIZE);
        if (oriSize > SUPPORT_MAX_ASPECT_SIZE) {
            useTransform = true;
        }
        int sampleFactor = useTransform ? 2 : 1;
        float apectScale = 1.0f;
        Bitmap preview = mCamera.getCameraDeviceCtrl().getPreviewBitmap();

        long time1 = System.currentTimeMillis();
        if (SHOW_PREVIEW_DEBUG_LOG) {
            Log.e(TAG, "getPreviewFrame :" + (time1 - time0) + " ms;" + " w:" + preview.getWidth() + ",h:" + preview.getHeight());
            time0 = System.currentTimeMillis();
        }
        GaussianBlur blur = mCamera.getGaussianBlur();
        if (preview != null && blur != null) {
            float progess = 1 - mTranslateYProgess;
            if (progess < 0.1f) {
                progess = 0.1f;
            } else if (progess > 0.8f) {
                progess = 0.8f;
            }
            mBlurDegress = (int) (progess * 10);
            Bitmap bgBlurBitmap = blur.blurBitmap(preview, mBlurDegress);
            if (SHOW_PREVIEW_DEBUG_LOG) {
                time1 = System.currentTimeMillis();
                Log.e(TAG, "blur bitmap :" + (time1 - time0) + " ms");
                time0 = System.currentTimeMillis();
            }
            BlurInfo info = new BlurInfo();
            info.x = (int) (mOnSingleX / apectScale);
            info.y = (int) (mOnSingleY / apectScale);
            info.inRadius = (int) (IN_SHARPNESS_RADIUS * scale / apectScale);
            info.outRadius = (int) (OUT_SHARPNESS_RADIUS * scale / apectScale);
            SmoothBlurJni.smoothRender(bgBlurBitmap, preview, info);
            if (SHOW_PREVIEW_DEBUG_LOG) {
                time1 = System.currentTimeMillis();
                Log.e(TAG, "smooth render :" + (time1 - time0) + " ms");
            }
            Matrix matrix = new Matrix();
            matrix.setScale(apectScale, apectScale);
            canvas.drawBitmap(bgBlurBitmap, matrix, null);

            //saveBitmap(bgBlurBitmap);
            preview.recycle();
            bgBlurBitmap.recycle();
        }
    }

    public static String sdCardDir = Environment.getExternalStorageDirectory() + "/fingerprintimages/";

    public static void saveBitmap(Bitmap bitmap) {
        try {
            File dirFile = new File(sdCardDir);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }
            File file = new File(sdCardDir, "luoran" + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != View.VISIBLE) {
            mIsCancelDiaphragm = true;
            if (mCamera != null) {
                mOrientation = mCamera.getCurrentOrientation();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = mGestureDetector.onTouchEvent(event);
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {
            getHandler().removeCallbacks(mCancelDiaphragmRunnable);
            getHandler().postDelayed(mCancelDiaphragmRunnable, CANCEL_DELAY);
        }
        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mPreviewArea != null) {
            Log.i(TAG, "onMeasure,width:" + mPreviewArea.width() + ",height:" + mPreviewArea.height());
            this.setMeasuredDimension((int) mPreviewArea.width(), (int) mPreviewArea.height());
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mOnSingleX = (right - left) / 2;
        mOnSingleY = (bottom - top) / 2;
        mCenterX = mOnSingleX;
        mCenterY = mOnSingleY;
        Log.i(TAG, "onLayout mOnSingleX:" + mOnSingleX + ",mOnSingleY:" + mOnSingleY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTrueBgVirtualWithCanvas(canvas);
        drawDiaphragm(canvas);
    }

    private boolean isScreenPortrait() {
        int orientation = mCamera.getCurrentOrientation() % 360;
        mOrientation = orientation;
        boolean isPortrait = true;
        if (mOrientation == 90 || mOrientation == 270) {
            isPortrait = false;
        }
        return isPortrait;
    }

    private void drawDiaphragm(Canvas canvas) {
        mRotationMatrix.setScale(0.9f, 0.9f, mCircleOutRadius, mCircleOutRadius);
        mRotationMatrix.postTranslate(mCenterX - mCircleOutRadius, mCenterY - mCircleOutRadius);
        if (mIsFousing) {
            canvas.drawBitmap(mCircleOutWhite, mRotationMatrix, null);
        } else {
            canvas.drawBitmap(mCircleOutGreen, mRotationMatrix, null);
            boolean isPortrait = isScreenPortrait();
            PointF pointF = compulateSeekbarCoordinate(isPortrait);

            canvas.drawBitmap(mSeekbarPoint, pointF.x - mSphereSize / 2, pointF.y -
                    mSphereSize / 2, null);
            if (isPortrait) {
                int topStopY = (int) (pointF.y - mSphereSize / 2 - (mCenterY - mPartRadius));
                int bottomStopY = (int) (mCenterY + mPartRadius - (pointF.y + mSphereSize / 2));
                if (topStopY <= 0) {
                    canvas.drawLine(pointF.x, pointF.y + mSphereSize / 2, pointF.x, mCenterY
                            + mPartRadius, mPaint);
                } else if (bottomStopY <= 0) {
                    canvas.drawLine(pointF.x, mCenterY - mPartRadius, pointF.x, pointF.y -
                            mSphereSize / 2, mPaint);
                } else {
                    canvas.drawLine(pointF.x, mCenterY - mPartRadius, pointF.x, pointF.y -
                            mSphereSize / 2, mPaint);
                    canvas.drawLine(pointF.x, pointF.y + mSphereSize / 2, pointF.x, mCenterY
                            + mPartRadius, mPaint);
                }
            } else {
                int leftStopX = (int) (pointF.x - mSphereSize / 2 - (mCenterX - mPartRadius));
                int rightStopX = (int) (mCenterX + mPartRadius - (pointF.x + mSphereSize / 2));
                if (leftStopX <= 0) {
                    canvas.drawLine(pointF.x + mSphereSize / 2, pointF.y, mCenterX +
                            mPartRadius, pointF.y, mPaint);
                } else if (rightStopX <= 0) {
                    canvas.drawLine(mCenterX - mPartRadius, pointF.y, pointF.x - mSphereSize
                            / 2, pointF.y, mPaint);
                } else {
                    canvas.drawLine(mCenterX - mPartRadius, pointF.y, pointF.x - mSphereSize
                            / 2, pointF.y, mPaint);
                    canvas.drawLine(pointF.x + mSphereSize / 2, pointF.y, mCenterX +
                            mPartRadius, pointF.y, mPaint);
                }
            }

            if (mIsInSeekbarArea) {
                canvas.save();
                canvas.translate(mCenterX - mCircleOutRadius, mCenterY - mCircleOutRadius);
                canvas.setDrawFilter(mPFDF);
                canvas.clipPath(mPath, Region.Op.REPLACE);
                float[] point = new float[2];
                for (int i = 0; i < 360; i += 60) {
                    float progess = mTranslateYProgess;
                    if (progess < 0.1f) {
                        progess = 0.1f;
                    } else if (progess > 0.9f) {
                        progess = 0.9f;
                    }
                    point[0] = -mCircleInRadius * (1 - progess);
                    point[1] = 0;
                    mRotationMatrix.setRotate(i, mCircleOutRadius, mCircleOutRadius);
                    mTranslateMatrix.setRotate(i + 120);
                    mTranslateMatrix.mapPoints(point);
                    mRotationMatrix.postTranslate(point[0], point[1]);
                    canvas.drawBitmap(mPartBitmap, mRotationMatrix, mPaint);
                }
                canvas.restore();
            }
        }
    }

    public byte[] blendJpegData(byte[] rawData) {
        synchronized (BvirtualView.this) {
            Bitmap src_bitmap = null;
            if (mPreviewArea == null) {
                return null;
            }
            long time0 = System.currentTimeMillis();
            long time1;
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                src_bitmap = BitmapFactory.decodeByteArray(rawData, 0, rawData.length, options);
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "blendOutput() fail!", e);
                return null;
            }

            if (src_bitmap == null) {
                Log.e(TAG, "src_bitmap = null, blendOutput() fail!");
                return null;
            }
            time1 = System.currentTimeMillis();
            Log.i(TAG, "decode rawData :" + (time1 - time0) + " ms");
            time0 = System.currentTimeMillis();
            GaussianBlur blur = mCamera.getGaussianBlur();
            if (blur == null) {
                return null;
            }

            int previewMin = Math.min((int) mPreviewArea.width(), (int) mPreviewArea.height());
            float oriScale = previewMin / (float) (REFERENCE_ASPECT_SIZE);
            int pictureMin = Math.min(src_bitmap.getWidth(), src_bitmap.getHeight());
            float scale = pictureMin / (float) previewMin;
            boolean mirror = mCamera.getCurrentCameraId() == 1 ? true : false;
            mTranslateMatrix.setScale(mirror ? -1 : 1, 1, src_bitmap.getWidth() / 2, src_bitmap.getHeight() / 2);
            float[] point = new float[2];
            point[0] = (mOnSingleX * scale);
            point[1] = (mOnSingleY * scale);
            mTranslateMatrix.mapPoints(point);
            int x = (int) point[0];
            int y = (int) point[1];
            Bitmap bgBlurBitmap = blur.blurBitmap(src_bitmap, mBlurDegress * 3);
            time1 = System.currentTimeMillis();
            Log.i(TAG, "blur bitmap :" + (time1 - time0) + " ms");
            time0 = System.currentTimeMillis();
            BlurInfo info = new BlurInfo();
            info.x = x;
            info.y = y;
            info.inRadius = (int) (IN_SHARPNESS_RADIUS * oriScale * scale);
            info.outRadius = (int) (OUT_SHARPNESS_RADIUS * oriScale * scale);
            SmoothBlurJni.smoothRender(bgBlurBitmap, src_bitmap, info);
            Canvas canvas = new Canvas(bgBlurBitmap);
            canvas.save(Canvas.ALL_SAVE_FLAG);
            canvas.restore();
            time1 = System.currentTimeMillis();
            Log.e(TAG, "blend bitmap :" + (time1 - time0) + " ms");
            time0 = System.currentTimeMillis();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bgBlurBitmap.compress(Bitmap.CompressFormat.JPEG, 95, baos);
            byte[] retData = baos.toByteArray();
            time1 = System.currentTimeMillis();
            Log.e(TAG, "compress rawData :" + (time1 - time0) + " ms");
            src_bitmap.recycle();
            bgBlurBitmap.recycle();
            return retData;
        }
    }
}
