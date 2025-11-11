package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        fabScan = findViewById(R.id.fab_scan);

        // Set the default fragment to Home
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
        }

        // Handle bottom navigation item selection
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

        // Handle Scan FAB click
        fabScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch the ScanQrActivity
                startActivity(new Intent(MainActivity.this, ScanQrActivity.class));
            }
        });

        // This is a small trick to make the "Scan" placeholder in the menu un-selectable
        // We find its ID and disable it, so it doesn't show a ripple effect.
        View placeholderItem = findViewById(R.id.nav_placeholder);
        if (placeholderItem != null) {
            placeholderItem.setClickable(false);
        }
    }

    // Helper method to replace the fragment in the container
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }
}