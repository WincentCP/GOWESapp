package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class VerifyIdentityActivity extends AppCompatActivity {

    private LinearLayout uploadState;
    private LinearLayout successState;
    private Button btnChoosePhoto, btnChangePhoto, btnComplete, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_identity);

        // Find layout states
        uploadState = findViewById(R.id.upload_state);
        successState = findViewById(R.id.success_state);

        // Find buttons
        btnChoosePhoto = findViewById(R.id.btn_choose_photo);
        btnChangePhoto = findViewById(R.id.btn_change_photo);
        btnComplete = findViewById(R.id.btn_complete);
        btnBack = findViewById(R.id.btn_back);

        // Set initial state (show upload prompt)
        showUploadState();

        // Choose Photo button click
        btnChoosePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // --- Add logic to open camera/gallery ---

                // After photo is selected, show the success state
                showSuccessState();
            }
        });

        // Change Photo button click
        btnChangePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // --- Add logic to open camera/gallery ---

                // For now, just toggle back to upload state
                showUploadState();
            }
        });

        // Back button click
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Complete button click
        btnComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // --- Add logic to save user data ---

                // Go to the main app
                Intent intent = new Intent(VerifyIdentityActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private void showUploadState() {
        uploadState.setVisibility(View.VISIBLE);
        successState.setVisibility(View.GONE);
    }

    private void showSuccessState() {
        uploadState.setVisibility(View.GONE);
        successState.setVisibility(View.VISIBLE);
    }
}