package com.lewisxiao.qrcodescanner;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class CustomCaptureActivity extends Activity implements DecoratedBarcodeView.TorchListener {
    public static final int SPOT_SUCCESS = 0xeeff00;
    private CaptureManager mCapture;
    private DecoratedBarcodeView mBarcodeScannerView;

    private ImageView mSwitchLightView;
    private ImageView mOpenAlbumView;

    private boolean isLightOn;

    private QRSpotHelper mQrSpotHelper;
    private ContentLoadingProgressBar mProgressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_capture);

        initView();
        initCaptureManager(savedInstanceState);
        initListener();
    }

    private void initCaptureManager(Bundle savedInstanceState) {
        mCapture = new CaptureManager(this, mBarcodeScannerView);
        mCapture.initializeFromIntent(getIntent(), savedInstanceState);
        mCapture.decode();
    }

    private void initView() {
        mBarcodeScannerView = findViewById(R.id.zxing_barcode_scanner);
        mSwitchLightView = findViewById(R.id.btn_switch_light);
        mOpenAlbumView = findViewById(R.id.btn_open_album);
        mProgressBar = findViewById(R.id.progress);

        if (!hasFlash()) {
            mSwitchLightView.setVisibility(View.GONE);
        }

        mProgressBar.setVisibility(View.GONE);
    }

    private void initListener() {
        mBarcodeScannerView.setTorchListener(this);

        mSwitchLightView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isLightOn) {
                    mBarcodeScannerView.setTorchOff();
                } else {
                    mBarcodeScannerView.setTorchOn();
                }
            }
        });

        // open album
        mOpenAlbumView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mQrSpotHelper == null) {
                    mQrSpotHelper = new QRSpotHelper(CustomCaptureActivity.this, mOnSpotCallBack);
                }
                mQrSpotHelper.spotFromAlbum();
            }
        });
    }

    /*
      check if phone has flashlight function
     */
    private boolean hasFlash() {
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    // recognize photo
    private QRSpotHelper.OnSpotCallBack mOnSpotCallBack = new QRSpotHelper.OnSpotCallBack() {
        @Override
        public void onSpotStart() {
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onSpotSuccess(Result result) {
            mProgressBar.setVisibility(View.GONE);
            String data = result.getText();
            Intent intent = new Intent();
            intent.putExtra("data", data);
            setResult(SPOT_SUCCESS, intent);
            finish();
        }

        @Override
        public void onSpotError() {
            mProgressBar.setVisibility(View.GONE);
            Toast.makeText(CustomCaptureActivity.this, "qrcode not found", Toast.LENGTH_SHORT).show();
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        mCapture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCapture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCapture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mCapture.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        mCapture.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mBarcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public void onTorchOn() {
        isLightOn = true;
    }

    @Override
    public void onTorchOff() {
        isLightOn = false;
    }
}
