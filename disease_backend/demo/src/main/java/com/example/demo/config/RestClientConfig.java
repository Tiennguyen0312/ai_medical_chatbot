package com.example.demo.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig 
{	@Bean
    public RestClient mlRestClient(
    		@Value("${ml.base-url:http://127.0.0.1:8000}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();}
}