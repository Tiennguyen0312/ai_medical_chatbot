package com.example.demo.dto;

public class AuthResponse {
    private Long id;
    private String username;
    private String token;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private Double bmi;
    private String bmiStatus;
    public AuthResponse(Long id, String username, String token,
                        String firstName, String middleName, String lastName, String email,
                        Double bmi, String bmiStatus) {
        this.id = id;
        this.username = username;
        this.token = token;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.bmi = bmi;
        this.bmiStatus = bmiStatus;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getToken() { return token; }
    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public Double getBmi() { return bmi; }
    public String getBmiStatus() { return bmiStatus; }
}