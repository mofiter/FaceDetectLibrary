package com.kongqw.opencvforandroid320;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.kongqw.RobotPhotographView;

public class RobotPhotographActivity extends Activity {

    private RobotPhotographView robotCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_robot_photograph);

        robotCameraView = (RobotPhotographView) findViewById(R.id.photograph_view);
    }


    /**
     * 切换摄像头
     *
     * @param view view
     */
    public void swapCamera(View view) {
        robotCameraView.swapCamera();
    }
}
