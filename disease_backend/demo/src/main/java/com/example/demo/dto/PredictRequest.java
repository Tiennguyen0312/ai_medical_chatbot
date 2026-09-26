package com.example.demo.dto;
import java.util.List;

public class PredictRequest {
    private String text;
    private List<String> symptoms;
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public List<String> getSymptoms() { return symptoms; }
    public void setSymptoms(List<String> symptoms) { this.symptoms = symptoms; }}
