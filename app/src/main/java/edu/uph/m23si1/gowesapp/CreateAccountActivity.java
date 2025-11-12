package edu.uph.m23si1.gowesapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateAccountActivity extends AppCompatActivity {

    private static final String TAG = "CreateAccountActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText etFullName, etPhone;
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFullName = findViewById(R.id.et_full_name);
        etPhone = findViewById(R.id.et_phone);
        btnContinue = findViewById(R.id.btn_continue);

        btnContinue.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString();
            String phone = etPhone.getText().toString();

            // TODO: Tambahkan validasi email/password (Firebase Auth memerlukan email)
            // Untuk demo ini, kita akan menggunakan login anonim

            // (Perbaikan Bug 8)
            // Kita akan login secara anonim untuk mendapatkan UID
            // Di aplikasi nyata, Anda akan menggunakan createAccountWithEmailAndPassword
            mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        createUserDocument(user.getUid(), fullName, phone);
                    }
                } else {
                    Log.w(TAG, "signInAnonymously:failure", task.getException());
                    Toast.makeText(CreateAccountActivity.this, "Authentication failed.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void createUserDocument(String userId, String fullName, String phone) {
        // (Perbaikan Bug 8) Buat dokumen pengguna baru di Firestore
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", fullName);
        userData.put("phone", phone);
        userData.put("walletBalance", 0); // Saldo 0 untuk pengguna baru
        userData.put("isCardLinked", false); // Kartu belum tertaut
        userData.put("isActiveRide", false); // Tidak ada perjalanan aktif
        // ... (data profil lainnya)

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "DocumentSnapshot successfully written!");

                    // Lanjutkan ke langkah verifikasi
                    Intent intent = new Intent(CreateAccountActivity.this, VerifyIdentityActivity.class);
                    startActivity(intent);

                    // Jika Anda ingin langsung ke main (melewatkan verifikasi untuk tes):
                    // Intent intent = new Intent(CreateAccountActivity.this, MainActivity.class);
                    // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    // startActivity(intent);
                    // finish();
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error writing document", e));
    }
}