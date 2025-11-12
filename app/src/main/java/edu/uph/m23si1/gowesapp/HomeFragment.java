package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration userListener;

    private CardView activeRideBanner;
    private TextView tvUserName, tvWalletBalance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_home, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        activeRideBanner = view.findViewById(R.id.active_ride_banner);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvWalletBalance = view.findViewById(R.id.tv_wallet_balance_main);

        // Banner click listener
        activeRideBanner.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ActiveRideActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            startUserListener(user.getUid());
        } else {
            // Jika pengguna tidak login, kembalikan ke alur pendaftaran
            goToSplash();
        }
    }

    private void startUserListener(String userId) {
        final DocumentReference userDocRef = db.collection("users").document(userId);

        userListener = userDocRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                Log.d(TAG, "Current data: " + snapshot.getData());

                // Perbarui UI dengan data pengguna
                String fullName = snapshot.getString("fullName");
                if (fullName != null) {
                    tvUserName.setText(fullName);
                }

                Number balanceNum = (Number) snapshot.get("walletBalance");
                if (balanceNum != null) {
                    tvWalletBalance.setText(formatCurrency(balanceNum.intValue()));
                }

                // (Perbaikan Bug 3) Perbarui status banner active ride
                Boolean isActiveRide = snapshot.getBoolean("isActiveRide");
                if (isActiveRide != null && isActiveRide) {
                    activeRideBanner.setVisibility(View.VISIBLE);
                } else {
                    activeRideBanner.setVisibility(View.GONE);
                }

                // TODO: Perbarui juga statistik (Total Rides, CO2, dll.)

            } else {
                Log.d(TAG, "Current data: null");
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        // Hentikan listener untuk menghemat resource
        if (userListener != null) {
            userListener.remove();
        }
    }

    private void goToSplash() {
        Intent intent = new Intent(getContext(), SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private String formatCurrency(int amount) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeID);
        currencyFormatter.setMaximumFractionDigits(0);
        return currencyFormatter.format(amount);
    }
}