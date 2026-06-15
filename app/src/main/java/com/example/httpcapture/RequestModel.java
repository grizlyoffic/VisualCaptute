package com.example.httpcapture;

public class RequestModel {
    private String method;
    private String path;
    private String headers;
    private String body;
    private String response;
    private long timestamp;
    
    public RequestModel(String method, String path, String headers, String body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getHeaders() { return headers; }
    public String getBody() { return body; }
    public String getResponse() { return response; }
    public long getTimestamp() { return timestamp; }
    
    public void setResponse(String response) {
        this.response = response;
    }
}