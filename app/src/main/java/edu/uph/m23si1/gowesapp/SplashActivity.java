package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // (Perbaikan Bug 1) - Periksa status login Firebase
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            Intent nextIntent;
            if (currentUser != null) {
                // Pengguna sudah login, langsung ke MainActivity
                nextIntent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                // Pengguna baru atau belum login, mulai alur pendaftaran
                nextIntent = new Intent(SplashActivity.this, CreateAccountActivity.class);
            }

            startActivity(nextIntent);
            finish();
        }, 3000); // 3 detik delay
    }
}