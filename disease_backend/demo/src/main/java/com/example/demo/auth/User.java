package com.example.demo.auth;

import jakarta.persistence.*;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, length=50)
    private String username;
    @Column(nullable=false, length=100)
    private String passwordHash;
    @Column(length=60)
    private String firstName;
    @Column(length=60)
    private String middleName;
    @Column(length=60)
    private String lastName;
    @Column(length=120)
    private String email;
    @Column
    private Boolean acceptTerms;
    @Column
    private Double bmi;

    @Column(length=30)
    private String bmiStatus;
    public User() {}
    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }

    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public Boolean getAcceptTerms() { return acceptTerms; }

    public Double getBmi() { return bmi; }
    public String getBmiStatus() { return bmiStatus; }

    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setAcceptTerms(Boolean acceptTerms) { this.acceptTerms = acceptTerms; }

    public void setBmi(Double bmi) { this.bmi = bmi; }
    public void setBmiStatus(String bmiStatus) { this.bmiStatus = bmiStatus; }
}