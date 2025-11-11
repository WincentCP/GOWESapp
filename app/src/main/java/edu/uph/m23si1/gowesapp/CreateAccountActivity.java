package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class CreateAccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        // Find views
        Button continueButton = findViewById(R.id.btn_continue);
        ImageView backButton = findViewById(R.id.iv_back);

        // Continue button click
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // --- Add your form validation here ---

                // Go to Step 2: Verify Identity
                startActivity(new Intent(CreateAccountActivity.this, VerifyIdentityActivity.class));
            }
        });

        // Back button click
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}