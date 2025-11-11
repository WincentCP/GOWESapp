package edu.uph.m23si1.gowesapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class ScanQrActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "ScanQrActivity";

    private PreviewView cameraPreview;
    private ConstraintLayout scannerUiLayout;
    private ConstraintLayout warningLayout;
    private Button btnGoBack;
    private ImageView ivBack;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Handler navigationHandler = new Handler(Looper.getMainLooper()); // Handler for the 5-second delay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        cameraPreview = findViewById(R.id.camera_preview);
        scannerUiLayout = findViewById(R.id.scanner_ui_layout);
        warningLayout = findViewById(R.id.warning_layout);
        btnGoBack = findViewById(R.id.btn_go_back);
        ivBack = findViewById(R.id.iv_back);

        ivBack.setOnClickListener(v -> finish());
        btnGoBack.setOnClickListener(v -> finish());

        // Check for camera permission
        if (isCameraPermissionGranted()) {
            startCamera();
        } else {
            requestCameraPermission();
        }

        // --- Add logic here to check if a ride is active ---
        // boolean isRideActive = ...;
        // if (isRideActive) {
        //     showWarningLayout();
        // } else {
        //     showScannerLayout();
        // }
    }

    private boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {
        // 1. Set up Preview
        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(1280, 720))
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        // 2. Bind only the preview to the camera
        try {
            cameraProvider.unbindAll(); // Unbind previous cases
            cameraProvider.bindToLifecycle(this, cameraSelector, preview);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }

        // 3. Start the 5-second timer to navigate automatically
        navigationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                navigateToBikeDetails();
            }
        }, 5000); // 5000 milliseconds = 5 seconds
    }

    private void navigateToBikeDetails() {
        // Check if activity is still running to avoid crash
        if (isFinishing() || isDestroyed()) {
            return;
        }

        Log.d(TAG, "5-second timer finished. Navigating to Bike Details.");
        Intent intent = new Intent(ScanQrActivity.this, BikeDetailsActivity.class);

        // We're simulating a scan, so let's pass a fake Bike ID
        intent.putExtra("BIKE_ID", "SIMULATED_BIKE_123");
        startActivity(intent);
        finish(); // Close the scanner
    }

    private void showWarningLayout() {
        scannerUiLayout.setVisibility(View.GONE);
        warningLayout.setVisibility(View.VISIBLE);
    }

    private void showScannerLayout() {
        scannerUiLayout.setVisibility(View.VISIBLE);
        warningLayout.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the handler from running if the activity is destroyed
        navigationHandler.removeCallbacksAndMessages(null);
    }
}