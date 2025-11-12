package edu.uph.m23si1.gowesapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.Locale;

public class WalletFragment extends Fragment {

    private static final String TAG = "WalletFragment";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration userListener;

    private TextView tvBalance;
    private CardView linkedCardView, linkPaymentView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvBalance = view.findViewById(R.id.tv_wallet_balance_main); // Pastikan ID ini ada di XML Anda
        linkedCardView = view.findViewById(R.id.linked_card_view);
        linkPaymentView = view.findViewById(R.id.link_payment_view);

        // (Perbaikan Bug 5) Tambahkan click listener
        linkPaymentView.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Membuka layar penautan kartu...", Toast.LENGTH_SHORT).show();
            // TODO: Buka Activity untuk menautkan kartu

            // Untuk demo, kita akan tautkan kartu di Firebase:
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                db.collection("users").document(user.getUid())
                        .update("isCardLinked", true)
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Kartu berhasil ditautkan!", Toast.LENGTH_SHORT).show());
            }
        });

        view.findViewById(R.id.btn_top_up).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Membuka dialog Top Up...", Toast.LENGTH_SHORT).show();
            // TODO: Buka dialog Top Up (bisa gunakan ulang logika dari ConfirmRideActivity)
        });

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
                // Perbarui Saldo
                Number balanceNum = (Number) snapshot.get("walletBalance");
                if (balanceNum != null) {
                    tvBalance.setText(formatCurrency(balanceNum.intValue()));
                }

                // (Perbaikan Bug 8) Perbarui Status Kartu
                Boolean isCardLinked = snapshot.getBoolean("isCardLinked");
                if (isCardLinked != null && isCardLinked) {
                    linkedCardView.setVisibility(View.VISIBLE);
                    linkPaymentView.setVisibility(View.GONE);
                } else {
                    linkedCardView.setVisibility(View.GONE);
                    linkPaymentView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (userListener != null) {
            userListener.remove();
        }
    }

    private String formatCurrency(int amount) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeID);
        currencyFormatter.setMaximumFractionDigits(0);
        return currencyFormatter.format(amount);
    }
}