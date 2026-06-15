package com.example.httpcapture;

import android.util.Log;
import fi.iki.elonen.NanoHTTPD;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FlaskServer extends NanoHTTPD {
    
    private RequestCallback callback;
    private static final String LOG_FOLDER = "payload";
    
    public interface RequestCallback {
        void onRequest(String method, String path, String headers, String body);
        void onResponse(int statusCode, String headers, String body);
    }
    
    public FlaskServer(int port, RequestCallback callback) throws IOException {
        super(port);
        this.callback = callback;
        createLogFolder();
    }
    
    private void createLogFolder() {
        File folder = new File(android.os.Environment.getExternalStorageDirectory(), LOG_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }
    
    @Override
    public Response serve(IHTTPSession session) {
        String method = session.getMethod().name();
        String uri = session.getUri();
        Map<String, String> headers = session.getHeaders();
        Map<String, String> parms = session.getParms();
        Map<String, String> files = new HashMap<>();
        
        // Get request body
        String requestBody = "";
        try {
            Integer contentLength = Integer.parseInt(headers.getOrDefault("content-length", "0"));
            if (contentLength > 0) {
                byte[] buffer = new byte[contentLength];
                session.getInputStream().read(buffer, 0, contentLength);
                requestBody = new String(buffer);
            }
        } catch (Exception e) {
            Log.e("FlaskServer", "Error reading body: " + e.getMessage());
        }
        
        // Notify callback
        String headersString = headers.toString();
        if (callback != null) {
            callback.onRequest(method, uri, headersString, requestBody);
        }
        
        // Create response
        String responseBody = "{\"status\":\"ok\",\"message\":\"Request captured\"}";
        Response response = newFixedLengthResponse(Response.Status.OK, 
            "application/json", responseBody);
        
        // Notify callback about response
        if (callback != null) {
            callback.onResponse(200, "Content-Type: application/json", responseBody);
        }
        
        // Save to log file
        saveToLogFile(method, uri, headersString, requestBody, 200, responseBody);
        
        return response;
    }
    
    private void saveToLogFile(String method, String path, String reqHeaders, 
                              String reqBody, int statusCode, String resBody) {
        try {
            String safeName = path.replace("/", "_");
            if (safeName.isEmpty()) safeName = "root";
            
            File logFile = new File(android.os.Environment.getExternalStorageDirectory(), 
                                   LOG_FOLDER + "/" + safeName + ".txt");
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("\n========================================\n");
            logBuilder.append("TIMESTAMP: ").append(timestamp).append("\n");
            logBuilder.append("ENDPOINT: ").append(path).append("\n");
            logBuilder.append("========================================\n\n");
            logBuilder.append("=== REQUEST ===\n");
            logBuilder.append("Method: ").append(method).append("\n");
            logBuilder.append("URL: ").append(path).append("\n");
            logBuilder.append("Headers: ").append(reqHeaders).append("\n");
            logBuilder.append("Body: ").append(reqBody).append("\n\n");
            logBuilder.append("=== RESPONSE ===\n");
            logBuilder.append("Status Code: ").append(statusCode).append("\n");
            logBuilder.append("Body: ").append(resBody).append("\n");
            logBuilder.append("========================================\n");
            
            FileOutputStream fos = new FileOutputStream(logFile, true);
            fos.write(logBuilder.toString().getBytes());
            fos.close();
            
        } catch (Exception e) {
            Log.e("FlaskServer", "Error saving log: " + e.getMessage());
        }
    }
}