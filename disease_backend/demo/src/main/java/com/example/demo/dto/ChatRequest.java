package com.example.demo.dto;

public class ChatRequest 
{
    private String session_id;
    private String message;
    private String username;
    public String getSession_id() { return session_id; }
    public void setSession_id(String session_id) { this.session_id = session_id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }}
