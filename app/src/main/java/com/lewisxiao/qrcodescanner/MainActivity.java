package com.lewisxiao.qrcodescanner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        new IntentIntegrator(this).initiateScan(); // `this` is the current Activity
//        IntentIntegrator intentIntegrator = new IntentIntegrator(this);
//        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE); //设置扫描的类型
//        intentIntegrator.setOrientationLocked(false);  //方向锁定
//        intentIntegrator.setCaptureActivity(CustomCaptureActivity.class);
//        intentIntegrator.setCameraId(0); //前置相机还是后置相机
//        intentIntegrator.setBeepEnabled(false); //是否发出成功的声音
//        intentIntegrator.setBarcodeImageEnabled(true);
//        intentIntegrator.initiateScan();

        new IntentIntegrator(this)
                .setOrientationLocked(false)
                .setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
                .setPrompt("将二维码/条码放入框内，即可自动扫描")
                .setCaptureActivity(CustomCaptureActivity.class)
                .initiateScan(); // 初始化扫描
    }

    // Get the results:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
