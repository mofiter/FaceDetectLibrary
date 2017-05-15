package com.kongqw.opencvforandroid320;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.kongqw.ObjectTracker2;
import com.kongqw.RobotTrackingView;
import com.kongqw.listener.OpenCVLoadListener;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class RobotTrackingActivity extends Activity {

    private static final String TAG = "RobotTrackingActivity";
    private RobotTrackingView robotTrackingView;
    private ImageView imageView;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_robot_tracking);

        robotTrackingView = (RobotTrackingView) findViewById(R.id.tracking_view);
        robotTrackingView.setOnOpenCVLoadListener(new OpenCVLoadListener() {
            @Override
            public void onOpenCVLoadSuccess() {
                robotTrackingView.setOnCalcBackProjectListener(new ObjectTracker2.OnCalcBackProjectListener() {
                    @Override
                    public void onCalcBackProject(final Mat backProject) {
                        Log.i(TAG, "onCalcBackProject: " + backProject);
                        RobotTrackingActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (null == bitmap) {
                                    bitmap = Bitmap.createBitmap(backProject.width(), backProject.height(), Bitmap.Config.ARGB_8888);
                                }
                                Utils.matToBitmap(backProject, bitmap);
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                });
            }

            @Override
            public void onOpenCVLoadFail() {

            }
        });

        imageView = (ImageView) findViewById(R.id.image_view);
    }

    public void swapCamera(View view) {
        robotTrackingView.swapCamera();
    }
}
