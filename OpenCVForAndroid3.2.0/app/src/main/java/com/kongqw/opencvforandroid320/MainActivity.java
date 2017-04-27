package com.kongqw.opencvforandroid320;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.kongqw.CamShifting;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2, View.OnTouchListener {

    private static final String TAG = "OCVSample::Activity";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;

    private MenuItem mItemFace50;
    private MenuItem mItemFace40;
    private MenuItem mItemFace30;
    private MenuItem mItemFace20;
    private MenuItem mItemType;

    private Mat mRgba;
    private Mat mGray;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
//    private DetectionBasedTracker mNativeDetector;

    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeFaceSize = 0.2f;
    private int mAbsoluteFaceSize = 0;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    // System.loadLibrary("detection_based_tracker");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        // mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();

                    cs = new CamShifting();
                    mTrackWindow = new Rect[1];
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private CamShifting cs;
    private boolean isTracking = false;
    private Rect[] facesArray;
    private Rect[] mTrackWindow;

    public MainActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnTouchListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (!isTracking) {
//            if (mAbsoluteFaceSize == 0) {
//                int height = mGray.rows();
//                if (Math.round(height * mRelativeFaceSize) > 0) {
//                    mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
//                }
////            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
//            }
//
//            MatOfRect faces = new MatOfRect();
//            if (mJavaDetector != null) {
//                mJavaDetector.detectMultiScale(
//                        mGray,
//                        faces,
//                        1.1,
//                        5,
//                        2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
//                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize),
//                        new Size()
//                );
//            }
//
//            facesArray = faces.toArray();
//            for (int i = 0; i < facesArray.length; i++) {
//                Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
//                cs.create_tracked_object(mRgba, facesArray, cs);
////                isTracking = true;
//            }
        } else {
            Log.i(TAG, "onCameraFrame: cs = " + cs + "  mTrackWindow = " + mTrackWindow);
            RotatedRect rotatedRect = cs.camshift_track_face(mRgba, mTrackWindow, cs);
//            Imgproc.ellipse(mRgba, rotatedRect, FACE_RECT_COLOR, 6);
            Imgproc.ellipse(mRgba, rotatedRect, FACE_RECT_COLOR, 6);
            // Imgproc.rectangle(mRgba, facesArray[0].tl(), facesArray[0].br(), FACE_RECT_COLOR, 3);

            Rect rect = rotatedRect.boundingRect();

            double x = rotatedRect.center.x;
            double y = rotatedRect.center.y;
            Log.i(TAG, "onCameraFrame: 目标坐标[ " + x + " , " + y + " ]");

            Imgproc.rectangle(mRgba, rect.tl(), rect.br(), FACE_RECT_COLOR, 3);

//            mTrackWindow[0] = rotatedRect.boundingRect();
        }

        System.gc();

        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemFace50 = menu.add("Face size 50%");
        mItemFace40 = menu.add("Face size 40%");
        mItemFace30 = menu.add("Face size 30%");
        mItemFace20 = menu.add("Face size 20%");
        mItemType = menu.add(mDetectorName[mDetectorType]);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemFace50)
            setMinFaceSize(0.5f);
        else if (item == mItemFace40)
            setMinFaceSize(0.4f);
        else if (item == mItemFace30)
            setMinFaceSize(0.3f);
        else if (item == mItemFace20)
            setMinFaceSize(0.2f);
        else if (item == mItemType) {
            int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
            item.setTitle(mDetectorName[tmpDetectorType]);
            setDetectorType(tmpDetectorType);
        }
        return true;
    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    private void setDetectorType(int type) {
        if (mDetectorType != type) {
            mDetectorType = type;
        }
    }

    private int winWidth = 200;
    int xDown;
    int yDown;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();
        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xDown = (int) event.getX() - xOffset;
                yDown = (int) event.getY() - yOffset;
                break;
            case MotionEvent.ACTION_UP:
                int xUp = (int) event.getX() - xOffset;
                int yUp = (int) event.getY() - yOffset;

                Rect rect = new Rect(Math.min(xDown, xUp), Math.min(yDown, yUp), Math.abs(xUp - xDown), Math.abs(yUp - yDown));
                mTrackWindow[0] = rect;

                cs.create_tracked_object(mRgba, mTrackWindow, cs);

                isTracking = true;
                Toast.makeText(getApplicationContext(), "已经选中跟踪目标！", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }


//        if ((x < 0) || (y < 0) || (x > cols) || (y > rows))
//            return false;
//        Rect rect = new Rect(x - winWidth / 2, y - winWidth / 2, winWidth, winWidth);
//        mTrackWindow[0] = rect;
//
//        cs.create_tracked_object(mRgba, mTrackWindow, cs);
//
//        isTracking = true;
        return true;
    }
}
