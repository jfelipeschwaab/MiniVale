package com.example.demo.dto;
import jakarta.validation.constraints.NotBlank;


public record ConfirmarOtpRequest(
        @NotBlank(message = "Código OTP é obrigatório")
        String codigo
) {
}
