package edu.uph.m23si1.gowesapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue; // Added missing import
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WalletFragment extends Fragment {

    private static final String TAG = "WalletFragment";
    private static final int MIN_TOP_UP_AMOUNT = 30000;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration balanceListener;
    private ListenerRegistration transactionsListener;

    private TextView tvWalletBalance;
    private RecyclerView rvTransactions;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionList;

    // UI Components for Card/Linking
    private CardView linkedCardView;
    private CardView linkPaymentView;
    private TextView tvCardLast4;
    private ImageView btnRemoveCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvWalletBalance = view.findViewById(R.id.tv_wallet_balance_main);
        rvTransactions = view.findViewById(R.id.rv_transactions);

        // Initialize Card UI Components
        linkedCardView = view.findViewById(R.id.linked_card_view);
        linkPaymentView = view.findViewById(R.id.link_payment_view);
        tvCardLast4 = view.findViewById(R.id.tv_card_last4);
        btnRemoveCard = view.findViewById(R.id.btn_remove_card);

        setupRecyclerView();

        View btnTopUp = view.findViewById(R.id.btn_top_up);
        if (btnTopUp != null) {
            btnTopUp.setOnClickListener(v -> showTopUpDialog());
        }

        // Setup Link Payment Click
        if (linkPaymentView != null) {
            linkPaymentView.setOnClickListener(v -> showLinkCardDialog());
        }

        // Setup Remove Card Click
        if (btnRemoveCard != null) {
            btnRemoveCard.setOnClickListener(v -> showRemoveCardConfirmation());
        }

        return view;
    }

    private void setupRecyclerView() {
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(transactionList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTransactions.setAdapter(transactionAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            startWalletListener(user.getUid());
            startTransactionsListener(user.getUid());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (balanceListener != null) balanceListener.remove();
        if (transactionsListener != null) transactionsListener.remove();
    }

    private void startWalletListener(String userId) {
        DocumentReference userDoc = db.collection("users").document(userId);
        balanceListener = userDoc.addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) return;

            // 1. Balance Update
            Number balance = snapshot.getDouble("walletBalance");
            if (balance == null) balance = 0.0;
            if (tvWalletBalance != null) {
                tvWalletBalance.setText(formatCurrency(balance.doubleValue()));
            }

            // 2. Card Link Status Update
            Boolean isCardLinked = snapshot.getBoolean("isCardLinked");
            String last4 = snapshot.getString("cardLast4"); // Assuming you save this field

            updateCardUI(Boolean.TRUE.equals(isCardLinked), last4);
        });
    }

    private void updateCardUI(boolean isLinked, String last4) {
        if (isLinked) {
            if (linkedCardView != null) linkedCardView.setVisibility(View.VISIBLE);
            if (linkPaymentView != null) linkPaymentView.setVisibility(View.GONE);

            if (tvCardLast4 != null) {
                tvCardLast4.setText("•••• " + (last4 != null ? last4 : "4324"));
            }
        } else {
            if (linkedCardView != null) linkedCardView.setVisibility(View.GONE);
            if (linkPaymentView != null) linkPaymentView.setVisibility(View.VISIBLE);
        }
    }

    private void startTransactionsListener(String userId) {
        Query query = db.collection("users").document(userId)
                .collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20);

        transactionsListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed.", e);
                return;
            }

            if (snapshots != null) {
                transactionList.clear();
                for (QueryDocumentSnapshot doc : snapshots) {
                    try {
                        Double amount = doc.getDouble("amount");
                        String description = doc.getString("description");
                        Long timestamp = doc.getLong("timestamp");
                        String type = doc.getString("type");
                        String status = doc.getString("status");

                        double amtVal = amount != null ? amount : 0.0;
                        String descVal = description != null ? description : "Transaction";
                        long timeVal = timestamp != null ? timestamp : System.currentTimeMillis();
                        String typeVal = type != null ? type : "General";
                        String statusVal = status != null ? status : "Completed";

                        Transaction txn = new Transaction(amtVal, descVal, timeVal, typeVal, statusVal);
                        transactionList.add(txn);
                    } catch (Exception ex) {
                        Log.e(TAG, "Error parsing transaction", ex);
                    }
                }
                transactionAdapter.notifyDataSetChanged();
            }
        });
    }

    private void showLinkCardDialog() {
        if (getContext() == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_link_payment, null);
        dialog.setContentView(view);

        EditText etCardNumber = view.findViewById(R.id.et_card_number);
        EditText etExpiry = view.findViewById(R.id.et_expiry); // Assuming ID from your dialog layout
        View btnLink = view.findViewById(R.id.btn_link);
        View btnCancel = view.findViewById(R.id.btn_cancel);

        // Add TextWatcher for Expiry Date Formatting (MM/YY)
        if (etExpiry != null) {
            etExpiry.addTextChangedListener(new TextWatcher() {
                boolean isDeleting;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    isDeleting = count > after;
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (!isDeleting && s.length() == 2 && !s.toString().contains("/")) {
                        s.append("/");
                    }
                }
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnLink != null) {
            btnLink.setOnClickListener(v -> {
                String cardNumber = etCardNumber.getText().toString().replaceAll("\\s+", "");

                // Basic Validation
                if (cardNumber.length() < 12) {
                    Toast.makeText(getContext(), "Invalid Card Number", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (etExpiry != null && etExpiry.getText().length() < 5) {
                    Toast.makeText(getContext(), "Invalid Expiry Date", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Extract last 4 digits
                String last4 = cardNumber.substring(cardNumber.length() - 4);

                // Update Firestore
                Map<String, Object> updates = new HashMap<>();
                updates.put("isCardLinked", true);
                updates.put("cardLast4", last4);

                db.collection("users").document(mAuth.getCurrentUser().getUid())
                        .set(updates, SetOptions.merge())
                        .addOnSuccessListener(a -> {
                            Toast.makeText(getContext(), "Card Linked!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to link card", Toast.LENGTH_SHORT).show());
            });
        }
        dialog.show();
    }

    private void showRemoveCardConfirmation() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Remove Card")
                .setMessage("Are you sure you want to remove this payment method?")
                .setPositiveButton("Remove", (dialog, which) -> removeLinkedCard())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeLinkedCard() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isCardLinked", false);
        updates.put("cardLast4", FieldValue.delete()); // Remove field

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .update(updates)
                .addOnSuccessListener(a -> Toast.makeText(getContext(), "Card Removed", Toast.LENGTH_SHORT).show());
    }

    private void showTopUpDialog() {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_top_up_balance, null);
        dialog.setContentView(dialogView);

        dialogView.setClickable(true);
        dialogView.setFocusableInTouchMode(true);

        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnTopUp = dialogView.findViewById(R.id.btn_pay_now);
        EditText etAmount = dialogView.findViewById(R.id.et_amount);

        View.OnClickListener presetListener = v -> {
            if (etAmount != null && v instanceof Button) {
                String text = ((Button) v).getText().toString();
                String clean = text.replaceAll("[^0-9]", "");
                etAmount.setText(clean);
                etAmount.setSelection(etAmount.getText().length());
            }
        };

        int[] presetIds = {
                R.id.btn_amount_30k,
                R.id.btn_amount_50k,
                R.id.btn_amount_100k,
                R.id.btn_amount_200k
        };

        for (int id : presetIds) {
            View btn = dialogView.findViewById(id);
            if (btn != null) btn.setOnClickListener(presetListener);
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnTopUp != null) {
            btnTopUp.setOnClickListener(v -> {
                String amountStr = etAmount.getText().toString().trim();

                if (amountStr.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int amount = Integer.parseInt(amountStr);
                    if (amount < MIN_TOP_UP_AMOUNT) {
                        Toast.makeText(getContext(), "Min Top Up Rp 30.000", Toast.LENGTH_SHORT).show();
                    } else {
                        processTopUp(amount, dialog);
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                }
            });
        }

        dialog.show();
    }

    private void processTopUp(int amount, BottomSheetDialog dialog) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        DocumentReference userRef = db.collection("users").document(user.getUid());

        userRef.update("walletBalance", com.google.firebase.firestore.FieldValue.increment(amount))
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Top Up Success!", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();

                    Map<String, Object> txn = new HashMap<>();
                    txn.put("amount", amount);
                    txn.put("description", "Top Up Wallet");
                    txn.put("type", "TopUp");
                    txn.put("status", "Success");
                    txn.put("timestamp", System.currentTimeMillis());

                    userRef.collection("transactions").add(txn);
                })
                .addOnFailureListener(e -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("walletBalance", amount);
                    userRef.set(data, SetOptions.merge()).addOnSuccessListener(v -> dialog.dismiss());
                });
    }

    private String formatCurrency(double amount) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeID);
        currencyFormatter.setMaximumFractionDigits(0);
        return currencyFormatter.format(amount);
    }
}