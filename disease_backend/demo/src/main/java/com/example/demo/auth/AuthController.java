package com.example.demo.auth;

import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.SignupRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UserRepository userRepo;

    public AuthController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @PostMapping("/signup")
    public AuthResponse signup(@RequestBody SignupRequest req) {
        String username = (req.getUsername() == null) ? "" : req.getUsername().trim();
        String password = (req.getPassword() == null) ? "" : req.getPassword().trim();
        String email = (req.getEmail() == null) ? "" : req.getEmail().trim();

        boolean accept = Boolean.TRUE.equals(req.getAcceptTerms());

        if (username.isEmpty() || password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password are required");
        }
        if (email.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid email format");
        }
        if (!accept) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "must accept terms");
        }
        if (userRepo.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        }

        Double heightCm = req.getHeightCm();
        Double weightKg = req.getWeightKg();
        if (heightCm == null || weightKg == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "heightCm and weightKg are required");
        }
        if (heightCm < 50 || heightCm > 250) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "heightCm out of range (50-250)");
        }
        if (weightKg < 10 || weightKg > 400) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "weightKg out of range (10-400)");
        }
        if (heightCm <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "heightCm must be > 0");
        }

        double bmi = calcBmi(heightCm, weightKg); 
        String bmiStatus = bmiStatusOf(bmi);

        String passwordHash = "{plain}" + password;

        User u = new User(username, passwordHash);
        u.setFirstName(trimOrNull(req.getFirstName()));
        u.setMiddleName(trimOrNull(req.getMiddleName()));
        u.setLastName(trimOrNull(req.getLastName()));
        u.setEmail(email);
        u.setAcceptTerms(true);

        u.setBmi(bmi);
        u.setBmiStatus(bmiStatus);

        u = userRepo.save(u);

        return toAuthResponse(u);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest req) {
        String username = (req.getUsername() == null) ? "" : req.getUsername().trim();
        String password = (req.getPassword() == null) ? "" : req.getPassword().trim();

        User u = userRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"));

        if (!u.getPasswordHash().equals("{plain}" + password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }

        return toAuthResponse(u);
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static double calcBmi(double heightCm, double weightKg) {
        double m = heightCm / 100.0;
        double bmi = weightKg / (m * m);
        return Math.round(bmi * 10.0) / 10.0; 
    }
    private static String bmiStatusOf(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25.0) return "Healthy";
        if (bmi < 30.0) return "Overweight";
        if (bmi < 40.0) return "Obese";
        return "Severely Obese";
    }
    private AuthResponse toAuthResponse(User u) {
        String token = UUID.randomUUID().toString();
        return new AuthResponse(
            u.getId(),
            u.getUsername(),
            token,
            u.getFirstName(),
            u.getMiddleName(),
            u.getLastName(),
            u.getEmail(),
            u.getBmi(),
            u.getBmiStatus()
        );
    }
}