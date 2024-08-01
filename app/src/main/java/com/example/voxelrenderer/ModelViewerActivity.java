package com.example.voxelrenderer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.opengl.GLSurfaceView;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;
import java.io.InputStream;

public class ModelViewerActivity extends AppCompatActivity {

    private GLSurfaceView surface;
    private boolean isSurfaceCreated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags
                (WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);


        final ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);


        final ConfigurationInfo configurationInfo =
                activityManager.getDeviceConfigurationInfo();


        int supported = 1;

        if (configurationInfo.reqGlEsVersion >= 0x30000)
            supported = 3;
        else if (configurationInfo.reqGlEsVersion >= 0x20000)
            supported = 2;


        surface = new GLSurfaceView(this);
        surface.setEGLContextClientVersion(supported);
        surface.setPreserveEGLContextOnPause(true);

        NaiveVoxelRenderer renderer = new NaiveVoxelRenderer();

        setContentView(surface);
        renderer.setContextAndSurface(this, surface);
        surface.setRenderer(renderer);
        isSurfaceCreated = true;



    }

    @Override
    public void onResume() {
        super.onResume();
        if (isSurfaceCreated)
            surface.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isSurfaceCreated)
            surface.onPause();
    }

}