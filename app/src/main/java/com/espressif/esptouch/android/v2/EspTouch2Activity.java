package com.espressif.esptouch.android.v2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;

import com.espressif.esptouch.android.EspTouchActivityAbs;
import com.espressif.esptouch.android.EspTouchApp;
import com.espressif.esptouch.android.R;
import com.espressif.esptouch.android.databinding.ActivityEsptouch2Binding;
import com.espressif.iot.esptouch2.provision.EspProvisioner;
import com.espressif.iot.esptouch2.provision.EspProvisioningRequest;
import com.espressif.iot.esptouch2.provision.EspSyncListener;
import com.espressif.iot.esptouch2.provision.IEspProvisioner;
import com.espressif.iot.esptouch2.provision.TouchNetUtil;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;

public class EspTouch2Activity extends EspTouchActivityAbs {
    private static final String TAG = EspTouch2Activity.class.getSimpleName();

    private static final int REQUEST_PERMISSION = 0x01;

    private EspProvisioner mProvisioner;

    private ActivityEsptouch2Binding mBinding;

    private InetAddress mAddress;
    private String mSsid;
    private byte[] mSsidBytes;
    private String mBssid;
    private CharSequence mMessage;
    private int mMessageVisible;
    private int mControlVisible;
    private SurfaceView surfaceView;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private ToneGenerator toneGen1;
    private TextView barcodeText;
    private String barcodeData;

    private ActivityResultLauncher<Intent> mProvisionLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mBinding = ActivityEsptouch2Binding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mProvisionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> mBinding.confirmBtn.setEnabled(true)
        );

        mBinding.controlGroup.setVisibility(View.INVISIBLE);
        mBinding.confirmBtn.setOnClickListener(v -> {
            if (launchProvisioning()) {
                mBinding.confirmBtn.setEnabled(false);
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        EspTouchApp.getInstance().observeBroadcast(this, action -> check());

        check();

        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC,100);
        surfaceView = findViewById(R.id.surface_view);
        barcodeText = findViewById(R.id.barcode_text);
        initialiseDetectorsAndSources();
    }

    private void initialiseDetectorsAndSources() {

        //Toast.makeText(getApplicationContext(), "Barcode scanner started", Toast.LENGTH_SHORT).show();

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true) //you should add this feature
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(EspTouch2Activity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(surfaceView.getHolder());
                    } else {
                        ActivityCompat.requestPermissions(EspTouch2Activity.this, new
                                String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });


        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                // Toast.makeText(getApplicationContext(), "To prevent memory leaks barcode scanner has been stopped", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {


                    barcodeText.post(new Runnable() {

                        @Override
                        public void run() {

                            if (barcodes.valueAt(0).email != null) {
                                barcodeText.removeCallbacks(null);
                                barcodeData = barcodes.valueAt(0).email.address;
                                barcodeText.setText(barcodeData);
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                            } else {

                                barcodeData = barcodes.valueAt(0).displayValue;
                                barcodeText.setText(barcodeData);
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);

                            }
                        }
                    });

                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mProvisioner = new EspProvisioner(getApplicationContext());
        SyncListener syncListener = new SyncListener(mProvisioner);
        mProvisioner.startSync(syncListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mProvisioner != null) {
            mProvisioner.stopSync();
            mProvisioner.close();
            mProvisioner = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            check();
            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected String getEspTouchVersion() {
        return getString(R.string.esptouch2_about_version, IEspProvisioner.ESPTOUCH_VERSION);
    }

    private boolean launchProvisioning() {
        EspProvisioningRequest request = genRequest();
        if (request == null) {
            return false;
        }
        if (mProvisioner != null) {
            mProvisioner.close();
        }

        Intent intent = new Intent(EspTouch2Activity.this, EspProvisioningActivity.class);
        intent.putExtra(EspProvisioningActivity.KEY_PROVISION_REQUEST, request);
        intent.putExtra(EspProvisioningActivity.KEY_DEVICE_COUNT, getDeviceCount());
        mProvisionLauncher.launch(intent);

        return true;
    }

    private boolean checkState() {
        StateResult stateResult = checkPermission();
        if (!stateResult.permissionGranted) {
            mMessage = stateResult.message;
            mBinding.messageView.setOnClickListener(v -> {
                String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
                ActivityCompat.requestPermissions(EspTouch2Activity.this, permissions, REQUEST_PERMISSION);
            });
            return false;
        }

        stateResult = checkLocation();
        if (stateResult.locationRequirement) {
            mMessage = stateResult.message;
            mBinding.messageView.setOnClickListener(null);
            return false;
        }

        stateResult = checkWifi();
        mSsid = stateResult.ssid;
        mSsidBytes = stateResult.ssidBytes;
        mBssid = stateResult.bssid;
        mMessage = stateResult.message;
        mAddress = stateResult.address;
        return stateResult.wifiConnected && !stateResult.is5G;
    }

    private byte[] getBssidBytes() {
        return mBssid == null ? null : TouchNetUtil.convertBssid2Bytes(mBssid);
    }

    private void invalidateAll() {
        mBinding.controlGroup.setVisibility(mControlVisible);
        mBinding.apSsidText.setText(mSsid);
        mBinding.apBssidText.setText(mBssid);
        mBinding.ipText.setText(mAddress == null ? "" : mAddress.getHostAddress());
        mBinding.messageView.setText(mMessage);
        mBinding.messageView.setVisibility(mMessageVisible);
    }

    private void check() {
        if (checkState()) {
            mControlVisible = View.VISIBLE;
            mMessageVisible = View.GONE;
        } else {
            mControlVisible = View.GONE;
            mMessageVisible = View.VISIBLE;

            if (mProvisioner != null) {
                if (mProvisioner.isSyncing()) {
                    mProvisioner.stopSync();
                }
                if (mProvisioner.isProvisioning()) {
                    mProvisioner.stopProvisioning();
                }
            }
        }
        invalidateAll();
    }

    private EspProvisioningRequest genRequest() {
        mBinding.aesKeyEdit.setError(null);
        mBinding.customDataEdit.setError(null);

        CharSequence aesKeyChars = mBinding.aesKeyEdit.getText();
        byte[] aesKey = null;
        if (aesKeyChars != null && aesKeyChars.length() > 0) {
            aesKey = aesKeyChars.toString().getBytes();
        }
        if (aesKey != null && aesKey.length != 16) {
            mBinding.aesKeyEdit.setError(getString(R.string.esptouch2_aes_key_error));
            return null;
        }

        CharSequence customDataChars = mBinding.customDataEdit.getText();
        byte[] customData = null;
        if (customDataChars != null && customDataChars.length() > 0) {
            customData = customDataChars.toString().getBytes();
        }
        int customDataMaxLen = EspProvisioningRequest.RESERVED_LENGTH_MAX;
        if (customData != null && customData.length > customDataMaxLen) {
            mBinding.customDataEdit.setError(getString(R.string.esptouch2_custom_data_error, customDataMaxLen));
            return null;
        }

        CharSequence password = mBinding.apPasswordEdit.getText();
        return new EspProvisioningRequest.Builder(getApplicationContext())
                .setSSID(mSsidBytes)
                .setBSSID(getBssidBytes())
                .setPassword(password == null ? null : password.toString().getBytes())
                .setAESKey(aesKey)
                .setReservedData(customData)
                .build();
    }

    private int getDeviceCount() {
        CharSequence deviceCountStr = mBinding.deviceCountEdit.getText();
        int deviceCount = -1;
        if (deviceCountStr != null && deviceCountStr.length() > 0) {
            try {
                deviceCount = Integer.parseInt(deviceCountStr.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return deviceCount;
    }

    private static class SyncListener implements EspSyncListener {
        private final WeakReference<EspProvisioner> provisioner;

        SyncListener(EspProvisioner provisioner) {
            this.provisioner = new WeakReference<>(provisioner);
        }

        @Override
        public void onStart() {
            Log.d(TAG, "SyncListener onStart");
        }

        @Override
        public void onStop() {
            Log.d(TAG, "SyncListener onStop");
        }

        @Override
        public void onError(Exception e) {
            e.printStackTrace();
            EspProvisioner provisioner = this.provisioner.get();
            if (provisioner != null) {
                provisioner.stopSync();
            }
        }
    }
}
