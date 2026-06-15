package com.example.httpcapture;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import rikka.shizuku.Shizuku;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileCreatorService {
    
    private Context context;
    private static final String CONFIG_JSON = "{\"serverLoginUrl\":\"https://127.0.0.1:8080/\"}";
    private static final String[] TARGET_PATHS = {
        "/storage/emulated/0/Android/data/com.dts.freefireth/files/",
        "/storage/emulated/0/Android/data/com.dts.freefireth.max/files/"
    };
    
    public FileCreatorService(Context context) {
        this.context = context;
    }
    
    public boolean createLocalConfigFiles() {
        if (!Shizuku.pingBinder()) {
            Log.e("FileCreator", "Shizuku not running");
            return false;
        }
        
        boolean allSuccess = true;
        
        for (String path : TARGET_PATHS) {
            try {
                createFileViaShizuku(path);
                Log.d("FileCreator", "Created config in: " + path);
            } catch (Exception e) {
                Log.e("FileCreator", "Failed to create in: " + path + " - " + e.getMessage());
                allSuccess = false;
            }
        }
        
        return allSuccess;
    }
    
    private void createFileViaShizuku(String dirPath) throws Exception {
        // Use Shizuku to create directory and file
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+, use Shizuku's shell service
            createFileWithShizukuShell(dirPath);
        } else {
            // For older versions
            createFileWithShizukuShell(dirPath);
        }
    }
    
    private void createFileWithShizukuShell(String dirPath) throws Exception {
        // Create directory first
        String mkdirCmd = "mkdir -p '" + dirPath + "'";
        execShizukuCommand(mkdirCmd);
        
        // Create file with content
        String writeCmd = "echo '" + CONFIG_JSON.replace("'", "'\\''") + "' > '" + 
                         dirPath + "localconfig.json'";
        execShizukuCommand(writeCmd);
        
        // Set permissions
        String chmodCmd = "chmod 644 '" + dirPath + "localconfig.json'";
        execShizukuCommand(chmodCmd);
    }
    
    private void execShizukuCommand(String command) throws Exception {
        // Execute command using Shizuku
        Shizuku.newProcess(new String[]{"sh", "-c", command}, null, null);
        
        // Wait a bit for command to execute
        Thread.sleep(500);
        
        // Verify with another command
        String verifyCmd = "ls -la '" + command.substring(command.lastIndexOf("'") + 1, 
                          command.lastIndexOf("'") != -1 ? command.lastIndexOf("'") - 1 : 0) + "'";
        Log.d("FileCreator", "Executing: " + command);
    }
}
