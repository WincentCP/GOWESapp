package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class ConfirmRideActivity extends AppCompatActivity {

    private CardView payAsYouGoCard, useWalletCard;
    private ImageView radioBtnPayg, radioBtnWallet;
    private LinearLayout linkCardPrompt, linkedCardDetails;
    private RelativeLayout authLayout, discountLayout;
    private Button btnConfirm;

    // States
    private boolean isWalletSelected = false;
    private boolean isCardLinked = true; // Set this based on user data
    private double walletBalance = 0; // Set this based on user data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_your_ride);

        // Find views
        payAsYouGoCard = findViewById(R.id.pay_as_you_go_card);
        useWalletCard = findViewById(R.id.use_wallet_card);
        radioBtnPayg = findViewById(R.id.radio_btn_payg);
        radioBtnWallet = findViewById(R.id.radio_btn_wallet);
        linkCardPrompt = findViewById(R.id.link_card_prompt);
        linkedCardDetails = findViewById(R.id.linked_card_details);
        authLayout = findViewById(R.id.auth_layout);
        discountLayout = findViewById(R.id.discount_layout);
        btnConfirm = findViewById(R.id.btn_confirm);
        ImageView backButton = findViewById(R.id.iv_back);

        // Set click listeners for payment options
        payAsYouGoCard.setOnClickListener(v -> selectPayAsYouGo());
        useWalletCard.setOnClickListener(v -> selectWallet());

        // Set default state
        selectWallet();

        // Confirm button click
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // --- Add logic to check balance ---
                double requiredBalance = 25000; // Example hold amount
                if (isWalletSelected && walletBalance < requiredBalance) {
                    showInsufficientBalanceDialog();
                } else {
                    // Start the ride
                    startActivity(new Intent(ConfirmRideActivity.this, ActiveRideActivity.class));
                }
            }
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void selectPayAsYouGo() {
        isWalletSelected = false;
        // Update UI
        payAsYouGoCard.setBackgroundResource(R.drawable.card_payment_selected);
        useWalletCard.setBackgroundResource(R.drawable.card_payment_unselected);
        radioBtnPayg.setImageResource(R.drawable.ic_radio_button_checked);
        radioBtnWallet.setImageResource(R.drawable.ic_radio_button_unchecked);

        // Show correct pricing
        authLayout.setVisibility(View.VISIBLE);
        discountLayout.setVisibility(View.GONE);

        // Show correct card details
        if (isCardLinked) {
            linkCardPrompt.setVisibility(View.GONE);
            linkedCardDetails.setVisibility(View.VISIBLE);
            btnConfirm.setEnabled(true);
        } else {
            linkCardPrompt.setVisibility(View.VISIBLE);
            linkedCardDetails.setVisibility(View.GONE);
            btnConfirm.setEnabled(false); // Can't ride without a card
        }
    }

    private void selectWallet() {
        isWalletSelected = true;
        // Update UI
        payAsYouGoCard.setBackgroundResource(R.drawable.card_payment_unselected);
        useWalletCard.setBackgroundResource(R.drawable.card_payment_selected);
        radioBtnPayg.setImageResource(R.drawable.ic_radio_button_unchecked);
        radioBtnWallet.setImageResource(R.drawable.ic_radio_button_checked);

        // Show correct pricing
        authLayout.setVisibility(View.GONE);
        discountLayout.setVisibility(View.VISIBLE);

        // Wallet is always "linked"
        linkCardPrompt.setVisibility(View.GONE);
        linkedCardDetails.setVisibility(View.GONE);

        // You can ride even with 0 balance, but will be prompted
        btnConfirm.setEnabled(true);
    }

    private void showInsufficientBalanceDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_insufficient_balance);

        Button topUpButton = dialog.findViewById(R.id.btn_top_up_now);
        Button maybeLaterButton = dialog.findViewById(R.id.btn_maybe_later);

        if (topUpButton != null) {
            topUpButton.setOnClickListener(v -> {
                showTopUpDialog();
                dialog.dismiss();
            });
        }

        if (maybeLaterButton != null) {
            maybeLaterButton.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showTopUpDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.dialog_top_up_balance);
        // --- Add click listeners for top-up amounts ---
        dialog.show();
    }
}