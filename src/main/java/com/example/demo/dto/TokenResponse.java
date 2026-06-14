package com.example.demo.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
