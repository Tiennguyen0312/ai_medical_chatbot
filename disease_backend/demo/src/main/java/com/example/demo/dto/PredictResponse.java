package com.example.demo.dto;
import java.util.List;
import java.util.Map;

public class PredictResponse {
    private List<Map<String, Object>> top3;
    private Map<String, Object> advice;
    public List<Map<String, Object>> getTop3() 
    {
        return top3;
    }
    public void setTop3(List<Map<String, Object>> top3) 
    {
        this.top3 = top3;
    }
    public Map<String, Object> getAdvice() 
    {
        return advice;
    }
    public void setAdvice(Map<String, Object> advice) 
    {
        this.advice = advice;
    }
}