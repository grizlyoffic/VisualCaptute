package com.example.httpcapture;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {
    
    private List<RequestModel> requestList;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    
    public RequestAdapter(List<RequestModel> requestList) {
        this.requestList = requestList;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_request, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RequestModel model = requestList.get(position);
        
        holder.tvMethod.setText(model.getMethod());
        holder.tvPath.setText(model.getPath());
        holder.tvTime.setText(sdf.format(new Date(model.getTimestamp())));
        
        // Color code method
        int color;
        switch (model.getMethod()) {
            case "GET":
                color = 0xFF4CAF50;
                break;
            case "POST":
                color = 0xFFFF9800;
                break;
            case "PUT":
                color = 0xFF2196F3;
                break;
            case "DELETE":
                color = 0xFFF44336;
                break;
            default:
                color = 0xFF9E9E9E;
        }
        holder.tvMethod.setTextColor(color);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            showRequestDetails(v.getContext(), model);
        });
        
        // Long click to copy
        holder.itemView.setOnLongClickListener(v -> {
            copyToClipboard(v.getContext(), model);
            return true;
        });
    }
    
    private void showRequestDetails(Context context, RequestModel model) {
        StringBuilder details = new StringBuilder();
        details.append("Method: ").append(model.getMethod()).append("\n\n");
        details.append("Path: ").append(model.getPath()).append("\n\n");
        details.append("Headers:\n").append(model.getHeaders()).append("\n\n");
        details.append("Body:\n").append(model.getBody() != null ? model.getBody() : "Empty");
        
        if (model.getResponse() != null) {
            details.append("\n\nResponse:\n").append(model.getResponse());
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Request Details")
            .setMessage(details.toString())
            .setPositiveButton("Copy", (dialog, which) -> copyToClipboard(context, model))
            .setNegativeButton("Close", null)
            .show();
    }
    
    private void copyToClipboard(Context context, RequestModel model) {
        ClipboardManager clipboard = (ClipboardManager) 
            context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("request", 
            "Method: " + model.getMethod() + "\nPath: " + model.getPath() + 
            "\n\nHeaders:\n" + model.getHeaders() + "\n\nBody:\n" + model.getBody());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public int getItemCount() {
        return requestList.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMethod, tvPath, tvTime;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvMethod = itemView.findViewById(R.id.tvMethod);
            tvPath = itemView.findViewById(R.id.tvPath);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}