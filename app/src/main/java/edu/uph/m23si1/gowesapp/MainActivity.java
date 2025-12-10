package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabScan;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        fabScan = findViewById(R.id.fab_scan);

        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
        }

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    replaceFragment(new HomeFragment());
                    return true;
                } else if (itemId == R.id.nav_rides) {
                    replaceFragment(new RidesFragment());
                    return true;
                } else if (itemId == R.id.nav_wallet) {
                    replaceFragment(new WalletFragment());
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    replaceFragment(new ProfileFragment());
                    return true;
                }
                return false;
            }
        });

        fabScan.setOnClickListener(v -> checkActiveRideAndScan());

        View placeholderItem = findViewById(R.id.nav_placeholder);
        if (placeholderItem != null) {
            placeholderItem.setClickable(false);
        }
    }

    private void checkActiveRideAndScan() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Boolean isActive = snapshot.getBoolean("isActiveRide");

                        if (Boolean.TRUE.equals(isActive)) {
                            Toast.makeText(MainActivity.this, "You have an ongoing ride!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity.this, ActiveRideActivity.class);
                            intent.putExtra("IS_NEW_RIDE", false);
                            startActivity(intent);
                        } else {
                            startActivity(new Intent(MainActivity.this, ScanQrActivity.class));
                        }
                    } else {
                        startActivity(new Intent(MainActivity.this, ScanQrActivity.class));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }
}