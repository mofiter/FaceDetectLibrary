package com.kongqw;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.kongqw.listener.OnCalcBackProjectListener;

import org.opencv.R;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

/**
 * Created by kqw on 2016/7/13.
 * RobotCameraView
 */
public class RobotTrackingView extends BaseRobotCameraView implements View.OnTouchListener, OnCalcBackProjectListener {

    private static final String TAG = "RobotPhotographView2";
    private CascadeClassifier mFaceDetector;
    private static final Scalar FACE_RECT_COLOR = new Scalar(255, 0, 255, 255);

    // CamShift 目标追踪器
    private ObjectTracker objectTracker;
    // 追踪目标区域
    private Rect mTrackWindow;
    // 追踪状态
    private boolean isTracking;

    @Override
    public void onOpenCVLoadSuccess() {
        Log.i(TAG, "onOpenCVLoadSuccess: ");
        // 人脸检测器
        mFaceDetector = mObjectDetector.getJavaDetector(R.raw.lbpcascade_frontalface);
        // 目标追踪器
        objectTracker = new ObjectTracker();
        objectTracker.setOnCalcBackProjectListener(this);
    }

    @Override
    public void onOpenCVLoadFail() {
        Log.i(TAG, "onOpenCVLoadFail: ");
    }

    public RobotTrackingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnTouchListener(this);
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        // 子线程（非UI线程）
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        // 目标追踪
        if (isTracking && null != mTrackWindow) {

            RotatedRect rotatedRect = objectTracker.objectTracking(mRgba);
            Imgproc.ellipse(mRgba, rotatedRect, FACE_RECT_COLOR, 6);

            Rect rect = rotatedRect.boundingRect();
            Imgproc.rectangle(mRgba, rect.tl(), rect.br(), FACE_RECT_COLOR, 3);
        }

        // 检测人脸  最小大小占屏比 0.2
        Rect[] detectObject = mObjectDetector.detectObject(mFaceDetector, mRgba, 0.2F);
        for (Rect rect : detectObject) {
            // 画出人脸位置
            Imgproc.rectangle(mRgba, rect.tl(), rect.br(), FACE_RECT_COLOR, 3);
        }
        return mRgba;
    }


    int xDown;
    int yDown;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();
        int xOffset = (getWidth() - cols) / 2;
        int yOffset = (getHeight() - rows) / 2;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTracking = false;
                xDown = (int) event.getX() - xOffset;
                yDown = (int) event.getY() - yOffset;
                break;
            case MotionEvent.ACTION_UP:
                int xUp = (int) event.getX() - xOffset;
                int yUp = (int) event.getY() - yOffset;

                int width = Math.abs(xUp - xDown);
                int height = Math.abs(yUp - yDown);

                if (0 == width || 0 == height) {
                    Toast.makeText(getContext(), "目标太小", Toast.LENGTH_SHORT).show();
                    break;
                }

                // 获取跟踪目标
                mTrackWindow = new Rect(Math.min(xDown, xUp), Math.min(yDown, yUp), width, height);

                // 创建跟踪目标
                objectTracker.createTrackedObject(mRgba, mTrackWindow);

                isTracking = true;

                Toast.makeText(getContext().getApplicationContext(), "已经选中跟踪目标！", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        return true;
    }

    private OnCalcBackProjectListener mOnCalcBackProjectListener;

    public void setOnCalcBackProjectListener(OnCalcBackProjectListener listener) {
        mOnCalcBackProjectListener = listener;
    }

    @Override
    public void onCalcBackProject(Mat backProject) {
        if (null != mOnCalcBackProjectListener) {
            mOnCalcBackProjectListener.onCalcBackProject(backProject);
        }
    }
}
