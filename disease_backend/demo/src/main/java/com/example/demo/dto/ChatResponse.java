package com.example.demo.dto;
import java.util.List;

public class ChatResponse {
    private String reply;
    private List<String> options; 
    private String state;         
    private String redirectTo;    
    public ChatResponse() {}
    public ChatResponse(String reply) {
        this.reply = reply;
    }
    public ChatResponse(String reply, List<String> options) {
        this.reply = reply;
        this.options = options;
    }
    public ChatResponse(String reply, List<String> options, String state) {
        this.reply = reply;
        this.options = options;
        this.state = state;
    }
    public ChatResponse(String reply, List<String> options, String state, String redirectTo) {
        this.reply = reply;
        this.options = options;
        this.state = state;
        this.redirectTo = redirectTo;}  
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getRedirectTo() { return redirectTo; }
    public void setRedirectTo(String redirectTo) { this.redirectTo = redirectTo; }
}