package com.zyl.camera2inservice;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class CameraWindow extends LinearLayout {

    private Context context;
    private WindowManager mWindowManager;
    private ImageView imageView;
    public SurfaceView surfaceView;
    public TextView tvFps;


    @SuppressLint("WrongConstant")
    public CameraWindow(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.window, this);
        this.context = context;
        initView();
    }

    private void initView() {
        //imageView = findViewById(R.id.javaCameraView);
        surfaceView = findViewById(R.id.surface_view);
        tvFps = findViewById(R.id.tv_fps);
        mWindowManager = (WindowManager) context.getSystemService(Service.WINDOW_SERVICE);
    }

}
