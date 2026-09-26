package com.example.demo.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
public class SecurityConfig 
{	@Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    	http
    	  .csrf(csrf -> csrf.disable())
    	  .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    	  .authorizeHttpRequests(auth -> auth
    	      .requestMatchers("/api/**", "/h2-console/**", "/error").permitAll()
    	      .anyRequest().permitAll()
    	  )
    	  .headers(headers -> headers.frameOptions(frame -> frame.disable()));         
        return http.build();}
}