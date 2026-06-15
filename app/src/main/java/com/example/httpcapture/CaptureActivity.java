package com.example.httpcapture;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CaptureActivity extends AppCompatActivity {
    
    private Button btnStart;
    private TextView tvServerStatus, tvLogs;
    private RecyclerView recyclerView;
    private RequestAdapter adapter;
    private List<RequestModel> requestList;
    private FlaskServer flaskServer;
    private FileCreatorService fileCreatorService;
    private boolean isServerRunning = false;
    private ExecutorService executor;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        
        btnStart = findViewById(R.id.btnStart);
        tvServerStatus = findViewById(R.id.tvServerStatus);
        tvLogs = findViewById(R.id.tvLogs);
        recyclerView = findViewById(R.id.recyclerView);
        
        tvLogs.setMovementMethod(new ScrollingMovementMethod());
        
        requestList = new ArrayList<>();
        adapter = new RequestAdapter(requestList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        fileCreatorService = new FileCreatorService(this);
        
        btnStart.setOnClickListener(v -> {
            if (!isServerRunning) {
                startCapture();
            } else {
                stopCapture();
            }
        });
        
        // Restore state
        if (savedInstanceState != null) {
            isServerRunning = savedInstanceState.getBoolean("server_running", false);
            if (isServerRunning) {
                btnStart.setText("⏹ STOP");
                tvServerStatus.setText("🟢 Server Running on 127.0.0.1:8080");
            }
        }
    }
    
    private void startCapture() {
        executor.execute(() -> {
            try {
                // Step 1: Create localconfig.json files
                publishLog("📁 Creating localconfig.json files...");
                
                boolean fileCreated = fileCreatorService.createLocalConfigFiles();
                if (!fileCreated) {
                    publishLog("❌ Failed to create config files! Check Shizuku permission.");
                    mainHandler.post(() -> {
                        Toast.makeText(CaptureActivity.this, 
                            "Failed to create files! Shizuku may not be running.", 
                            Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                
                publishLog("✅ localconfig.json files created successfully!");
                publishLog("📁 Path 1: /storage/emulated/0/Android/data/com.dts.freefireth/files/");
                publishLog("📁 Path 2: /storage/emulated/0/Android/data/com.dts.freefireth.max/files/");
                
                // Step 2: Start Flask server
                publishLog("🚀 Starting HTTP Capture Server...");
                
                flaskServer = new FlaskServer(8080, new FlaskServer.RequestCallback() {
                    @Override
                    public void onRequest(String method, String path, String headers, String body) {
                        RequestModel model = new RequestModel(method, path, headers, body);
                        mainHandler.post(() -> {
                            requestList.add(0, model);
                            adapter.notifyItemInserted(0);
                            recyclerView.scrollToPosition(0);
                        });
                        publishLog("📥 " + method + " " + path);
                    }
                    
                    @Override
                    public void onResponse(int statusCode, String headers, String body) {
                        if (requestList.size() > 0) {
                            RequestModel lastRequest = requestList.get(0);
                            lastRequest.setResponse("Status: " + statusCode + "\nHeaders: " + 
                                headers + "\nBody: " + (body.length() > 200 ? 
                                body.substring(0, 200) + "..." : body));
                            mainHandler.post(() -> adapter.notifyItemChanged(0));
                        }
                    }
                });
                
                flaskServer.start();
                isServerRunning = true;
                
                mainHandler.post(() -> {
                    tvServerStatus.setText("🟢 Server Running on 127.0.0.1:8080");
                    btnStart.setText("⏹ STOP");
                });
                
                publishLog("✅ Server started successfully!");
                publishLog("📡 Listening on: http://127.0.0.1:8080");
                publishLog("📋 All requests will be captured and displayed below");
                
            } catch (IOException e) {
                publishLog("❌ Error: " + e.getMessage());
                mainHandler.post(() -> {
                    Toast.makeText(CaptureActivity.this, 
                        "Error starting server: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void stopCapture() {
        executor.execute(() -> {
            if (flaskServer != null) {
                flaskServer.stop();
                flaskServer = null;
            }
            isServerRunning = false;
            
            mainHandler.post(() -> {
                tvServerStatus.setText("🔴 Server Stopped");
                btnStart.setText("▶ START CAPTURE");
            });
            
            publishLog("⏹ Server stopped");
        });
    }
    
    private void publishLog(String message) {
        mainHandler.post(() -> {
            tvLogs.append(message + "\n");
            // Auto scroll to bottom
            final ScrollView scrollView = (ScrollView) tvLogs.getParent();
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("server_running", isServerRunning);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServerRunning) {
            stopCapture();
        }
        executor.shutdown();
    }
}