package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class RidesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rides, container, false);

        // Inisialisasi tombol Scan QR di Fragment Rides
        MaterialButton btnScanQr = view.findViewById(R.id.btn_scan_qr);
        TextView tvViewAll = view.findViewById(R.id.tv_view_all);

        // Logic Tombol Scan
        if (btnScanQr != null) {
            btnScanQr.setOnClickListener(v -> {
                // Buka Activity Scan QR
                Intent intent = new Intent(getContext(), ScanQrActivity.class);
                startActivity(intent);
            });
        }

        // Logic View All Recent Rides
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                // TODO: Implementasi daftar lengkap riwayat perjalanan
                Toast.makeText(getContext(), "Viewing all rides history...", Toast.LENGTH_SHORT).show();
            });
        }

        return view;
    }
}