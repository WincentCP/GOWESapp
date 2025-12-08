package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;

    private TextView tvUserName, tvUserPhone, tvVerifiedChip;
    private TextView tvStatRides, tvStatCo2;
    private MaterialButton btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserPhone = view.findViewById(R.id.tv_user_phone);
        tvVerifiedChip = view.findViewById(R.id.tv_verified_chip);

        tvStatRides = view.findViewById(R.id.tv_stat_rides);
        tvStatCo2 = view.findViewById(R.id.tv_stat_co2);

        btnLogout = view.findViewById(R.id.btn_logout);

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        setupMenuClickListeners(view);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            startUserListener(user.getUid());
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
                // FIX: Ensure Name and Phone are populated exactly as entered
                String name = snapshot.getString("fullName");
                String phone = snapshot.getString("phone");
                Boolean isVerified = snapshot.getBoolean("identityVerified");

                if (name != null && !name.isEmpty()) {
                    tvUserName.setText(name);
                }

                if (phone != null && !phone.isEmpty()) {
                    tvUserPhone.setText(phone);
                }

                if (Boolean.TRUE.equals(isVerified)) {
                    tvVerifiedChip.setVisibility(View.VISIBLE);
                } else {
                    tvVerifiedChip.setVisibility(View.GONE);
                }

                tvStatRides.setText("24");
                tvStatCo2.setText("12kg");
            }
        });
    }

    private void setupMenuClickListeners(View view) {
        View personalInfo = view.findViewById(R.id.btn_personal_info);
        if (personalInfo != null) {
            personalInfo.setOnClickListener(v -> Toast.makeText(getContext(), "Personal Info clicked", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (userListener != null) {
            userListener.remove();
        }
    }
}