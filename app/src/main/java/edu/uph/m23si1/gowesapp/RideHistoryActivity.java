package edu.uph.m23si1.gowesapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class RideHistoryActivity extends AppCompatActivity {

    private RecyclerView rvAllRides;
    private RideAdapter adapter;
    private List<RideModel> rideList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvAllRides = findViewById(R.id.rv_all_rides);
        progressBar = findViewById(R.id.progressBar);
        ImageButton btnBack = findViewById(R.id.btn_back);
        TextView tvTitle = findViewById(R.id.tv_title);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        loadAllRides();
    }

    private void setupRecyclerView() {
        rideList = new ArrayList<>();
        adapter = new RideAdapter(rideList);
        rvAllRides.setLayoutManager(new LinearLayoutManager(this));
        rvAllRides.setAdapter(adapter);
    }

    private void loadAllRides() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        progressBar.setVisibility(View.VISIBLE);

        // Fetching from users -> rideHistory as per RideCompleteActivity saving logic
        db.collection("users").document(userId).collection("rideHistory")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    rideList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            RideModel ride = doc.toObject(RideModel.class);
                            if (ride != null) rideList.add(ride);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "No ride history found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load history.", Toast.LENGTH_SHORT).show();
                });
    }
}