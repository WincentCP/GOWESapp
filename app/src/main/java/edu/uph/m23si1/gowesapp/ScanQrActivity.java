package edu.uph.m23si1.gowesapp;

import android.Manifest;
import android.annotation.SuppressLint;
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
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@androidx.camera.core.ExperimentalGetImage
public class ScanQrActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "ScanQrActivity";
    private static final String QR_PREFIX = "GOWES-"; // matches qrCodeId in Realtime DB

    private PreviewView cameraPreview;
    private ConstraintLayout scannerUiLayout;
    private ConstraintLayout warningLayout;
    private Button btnGoBack;
    private ImageView ivBack;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;

    private boolean isScanningPaused = false;
    private Handler autoRedirectHandler = new Handler(Looper.getMainLooper());
    private Runnable autoRedirectRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        // Initialize Views safely
        cameraPreview = findViewById(R.id.camera_preview);
        scannerUiLayout = findViewById(R.id.scanner_ui_layout);
        warningLayout = findViewById(R.id.warning_layout);
        btnGoBack = findViewById(R.id.btn_go_back);
        ivBack = findViewById(R.id.iv_back);

        if (cameraPreview == null) {
            Log.e(TAG, "Camera Preview is null! Check layout XML IDs.");
            Toast.makeText(this, "Error initializing camera view", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (ivBack != null) ivBack.setOnClickListener(v -> finish());
        if (btnGoBack != null) btnGoBack.setOnClickListener(v -> finish());

        if (isCameraPermissionGranted()) {
            startCameraSafely();
        } else {
            requestCameraPermission();
        }

        checkActiveRideStatus();

        // Auto redirect timer (fallback if user doesn't scan anything)
        autoRedirectRunnable = () -> {
            if (!isScanningPaused && !isFinishing()) {
                int randomNum = new Random().nextInt(4) + 1; // 1..4
                String bikeCoreId = String.format("BK-%03d", randomNum); // BK-001..BK-004
                String qrValue = QR_PREFIX + bikeCoreId; // GOWES-BK-001..GOWES-BK-004
                Log.d(TAG, "Auto redirect using mock QR: " + qrValue);
                navigateToBikeDetails(qrValue);
            }
        };
        autoRedirectHandler.postDelayed(autoRedirectRunnable, 5000);
    }

    private void checkActiveRideStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        DocumentReference userDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid());
        userDoc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Boolean isActiveRide = documentSnapshot.getBoolean("isActiveRide");
                if (Boolean.TRUE.equals(isActiveRide)) {
                    showWarningLayout();
                } else {
                    showScannerLayout();
                }
            }
        });
    }

    private boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraSafely();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCameraSafely() {
        try {
            startCamera();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start camera", e);
            Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor();

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(1280, 720))
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            if (isScanningPaused || imageProxy.getImage() == null) {
                imageProxy.close();
                return;
            }

            InputImage image = InputImage.fromMediaImage(
                    imageProxy.getImage(),
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty()) {
                            String qrCodeValue = barcodes.get(0).getRawValue();
                            Log.d(TAG, "Scanned QR value: " + qrCodeValue);

                            if (qrCodeValue != null) {
                                isScanningPaused = true;
                                navigateToBikeDetails(qrCodeValue);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Barcode scan failed", e))
                    .addOnCompleteListener(task -> imageProxy.close());
        });

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
            );
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void navigateToBikeDetails(String bikeIdOrQrValue) {
        autoRedirectHandler.removeCallbacks(autoRedirectRunnable);

        try {
            Log.d(TAG, "navigateToBikeDetails called with: " + bikeIdOrQrValue);
            Intent intent = new Intent(ScanQrActivity.this, BikeDetailsActivity.class);
            intent.putExtra("BIKE_ID", bikeIdOrQrValue);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Failed to open BikeDetailsActivity", e);
            Toast.makeText(
                    this,
                    "Failed to open bike details: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void showWarningLayout() {
        if (scannerUiLayout != null) scannerUiLayout.setVisibility(View.GONE);
        if (warningLayout != null) warningLayout.setVisibility(View.VISIBLE);
    }

    private void showScannerLayout() {
        if (scannerUiLayout != null) scannerUiLayout.setVisibility(View.VISIBLE);
        if (warningLayout != null) warningLayout.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoRedirectHandler.removeCallbacks(autoRedirectRunnable);
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
    }
}