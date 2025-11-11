package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.Locale;

public class RideCompleteActivity extends AppCompatActivity {

    private TextView tvRideDuration, tvAmountPaid, tvThankYou;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_complete);

        // Find views
        tvRideDuration = findViewById(R.id.tv_ride_duration);
        tvAmountPaid = findViewById(R.id.tv_amount_paid);
        tvThankYou = findViewById(R.id.tv_subtitle);

        // Get data from the Intent
        String duration = getIntent().getStringExtra("RIDE_DURATION");
        // Get the cost as a double, with a default of 0.0
        double cost = getIntent().getDoubleExtra("FINAL_COST", 0.0);

        // Populate the views
        if (duration != null) {
            tvRideDuration.setText(duration);
        }

        // Format the double cost into "Rp 7.600" style
        Locale localeID = new Locale("in", "ID");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeID);
        currencyFormatter.setMaximumFractionDigits(0); // No decimals
        tvAmountPaid.setText(currencyFormatter.format(cost));


        // You can also get user name from shared preferences
        // String userName = "Wicent";
        // tvThankYou.setText("Thank you, " + userName + "! Stay safe 🌿");

        Button backHomeButton = findViewById(R.id.btn_back_home);
        backHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go back to MainActivity
                Intent intent = new Intent(RideCompleteActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }
}