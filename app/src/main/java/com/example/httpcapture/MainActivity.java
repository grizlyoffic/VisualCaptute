package com.example.httpcapture;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {
    
    private TextView statusText;
    private Button btnCheckShizuku, btnContinue;
    private static final int SHIZUKU_CODE = 1001;
    
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> manageStorageLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        statusText = findViewById(R.id.statusText);
        btnCheckShizuku = findViewById(R.id.btnCheckShizuku);
        btnContinue = findViewById(R.id.btnContinue);
        
        setupPermissionLaunchers();
        
        btnCheckShizuku.setOnClickListener(v -> checkShizukuStatus());
        
        btnContinue.setOnClickListener(v -> {
            if (checkShizukuReady()) {
                checkPermissions();
            }
        });
        
        // Add Shizuku listener
        Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
            if (requestCode == SHIZUKU_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread(() -> {
                        statusText.setText("✅ Shizuku Connected & Authorized");
                        btnContinue.setEnabled(true);
                    });
                }
            }
        });
        
        checkShizukuStatus();
    }
    
    private void setupPermissionLaunchers() {
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    checkShizukuAndProceed();
                } else {
                    Toast.makeText(this, "Permission required!", Toast.LENGTH_LONG).show();
                }
            });
            
        manageStorageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        checkShizukuAndProceed();
                    } else {
                        Toast.makeText(this, "Storage permission required!", Toast.LENGTH_LONG).show();
                    }
                }
            });
    }
    
    private void checkShizukuStatus() {
        if (!Shizuku.pingBinder()) {
            statusText.setText("❌ Shizuku Not Running!\nPlease start Shizuku first.");
            btnContinue.setEnabled(false);
            showShizukuNotRunningDialog();
        } else {
            if (checkShizukuPermission()) {
                statusText.setText("✅ Shizuku Connected & Authorized");
                btnContinue.setEnabled(true);
            } else {
                statusText.setText("⚠️ Shizuku Running but not authorized");
                requestShizukuPermission();
            }
        }
    }
    
    private boolean checkShizukuPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, 
                "moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }
    
    private void requestShizukuPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Shizuku.requestPermission(SHIZUKU_CODE);
        }
    }
    
    private boolean checkShizukuReady() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Please start Shizuku first!", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!checkShizukuPermission()) {
            requestShizukuPermission();
            Toast.makeText(this, "Please authorize Shizuku!", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
    
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showManageStorageDialog();
            } else {
                checkShizukuAndProceed();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                checkShizukuAndProceed();
            }
        }
    }
    
    private void showManageStorageDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Storage Permission Required")
            .setMessage("This app needs access to manage files in Android/data folder.\n\nPlease grant 'All files access' permission.")
            .setPositiveButton("Grant", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                manageStorageLauncher.launch(intent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showShizukuNotRunningDialog() {
        new AlertDialog.Builder(this)
            .setTitle("❌ Shizuku Not Running!")
            .setMessage("Shizuku is required for this app to work.\n\nPlease:\n1. Open Shizuku app\n2. Start Shizuku service\n3. Come back here")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void checkShizukuAndProceed() {
        if (checkShizukuReady()) {
            Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
            startActivity(intent);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(null);
    }
}