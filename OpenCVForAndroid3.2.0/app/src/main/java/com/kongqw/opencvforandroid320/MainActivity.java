package com.kongqw.opencvforandroid320;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

    }

    /**
     * 拍照
     *
     * @param view view
     */
    public void onPhotograph(View view) {
        startActivity(new Intent(this, RobotPhotographActivity.class));
    }

    /**
     * 追踪
     *
     * @param view view
     */
    public void onTracking(View view) {
        startActivity(new Intent(this, RobotTrackingActivity.class));
    }
}
