package com.example.voxelrenderer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;



public class ModelViewerActivity extends AppCompatActivity {
    public static final int DESIRED_DEPTH_SIZE = 24;

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
        surface.setEGLConfigChooser((egl10, eglDisplay) -> getConfig());

        VoxelRenderer renderer = new VoxelRenderer();

        setContentView(surface);
        renderer.setContextAndSurface(this, surface);
        surface.setRenderer(renderer);
        isSurfaceCreated = true;



    }

    private EGLConfig getConfig() {
        //Riferimento al contesto EGL
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(display, null);

        //ottenimento di numConfigs[0] configurazioni supportate.
        int[] numConfigs = new int[1];
        egl.eglChooseConfig(display, null, null, 0, numConfigs);
        EGLConfig[] configs = new EGLConfig[numConfigs[0]];
        egl.eglChooseConfig(display, null, configs, numConfigs[0], numConfigs);

        Log.v("EGLCONFIG", "configs " + numConfigs[0]);

        //per ogni configurazione supportata
        for (EGLConfig cfg : configs) {
            int[] depthv = new int[1];
            //estraiamo il numero di bit usati per rappresentare la profondita
            egl.eglGetConfigAttrib(display, cfg, EGL10.EGL_DEPTH_SIZE, depthv);
            Log.v("EGLCONFIG", "EGL_DEPTH_SIZE: " + depthv[0]);

            /*se depthv[0] contiene un valore in bit maggiore o uguale di quello desiderato
            possiamo restituire cfg ed impostare quella configurazione nella GLSurfaceView */
            if (depthv[0] >= DESIRED_DEPTH_SIZE) {
                return cfg;
            }
        }

        // Nessuna configurazione soddisfa i requisiti
        Log.w("EGLCONFIG", "No config satisfies DESIRED_DEPTH_SIZE");
        return configs[0];
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