package com.kongqw;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.R;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by kqw on 2016/7/13.
 * RobotCameraView
 */
public class RobotPhotographView extends JavaCameraView implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "RobotCameraView";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    private CascadeClassifier mJavaDetector;

    private Mat mRgba;
    private Mat mGray;

    private int mAbsoluteFaceSize = 0;
    // 脸部占屏幕多大面积的时候开始识别
    private static final float RELATIVE_FACE_SIZE = 0.2f;
    private Size mMinSize;
    private Size mMaxSize;
    private MatOfRect mFaces;

    public RobotPhotographView(Context context, AttributeSet attrs) {
        super(context, attrs);

        loadOpenCV(context);

        setCvCameraViewListener(this);
    }

    /**
     * 加载OpenCV
     *
     * @param context 上下文
     * @return 是否加载成功
     */
    private boolean loadOpenCV(Context context) {
        // 初始化OpenCV
        return OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, context, mLoaderCallback);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getContext().getApplicationContext()) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    try {
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getContext().getApplicationContext().getDir("cascade", Context.MODE_PRIVATE);
                        File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(cascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            mJavaDetector = null;
                        }

                        // mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

                        // cascadeDir.delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    enableView();
                }
                break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();

        mFaces = new MatOfRect();

        mMinSize = new Size();
        mMaxSize = new Size();
    }

    @Override
    public void onCameraViewStopped() {

        mFaces = null;
        mMinSize = null;
        mMaxSize = null;

        mGray.release();
        mRgba.release();
    }

    private Size getMinSize() {
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * RELATIVE_FACE_SIZE) > 0) {
                // 高度乘以百分比  四舍五入
                mAbsoluteFaceSize = Math.round(height * RELATIVE_FACE_SIZE);
            }
        }
        mMinSize.width = mMinSize.height = mAbsoluteFaceSize;
        return mMinSize;
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        // 子线程（非UI线程）
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();


        // 使用Java人脸检测
        if (mJavaDetector != null)
            mJavaDetector.detectMultiScale(
                    mGray, // 要检查的灰度图像
                    mFaces, // 检测到的人脸
                    1.1, // 表示在前后两次相继的扫描中，搜索窗口的比例系数。默认为1.1即每次搜索窗口依次扩大10%;
                    10, // 默认是3 控制误检测，表示默认几次重叠检测到人脸，才认为人脸存在
                    Objdetect.CASCADE_SCALE_IMAGE,
                    getMinSize(), // 目标最小可能的大小
                    mMaxSize); // 目标最大可能的大小


        Rect[] facesArray = mFaces.toArray();
        Log.i(TAG, "onCameraFrame: mAbsoluteFaceSize = " + mAbsoluteFaceSize);
        Log.i(TAG, "onCameraFrame: facesArray " + facesArray.length);

        for (Rect rect : facesArray) {
            // 在屏幕上画出人脸位置
            Imgproc.rectangle(mRgba, rect.tl(), rect.br(), FACE_RECT_COLOR, 3);
        }
        return mRgba;
    }

    private int mCameraIndexCount = 0;

    public void swapCamera() {
        disableView();
        setCameraIndex(++mCameraIndexCount % Camera.getNumberOfCameras());
        enableView();
    }
}
