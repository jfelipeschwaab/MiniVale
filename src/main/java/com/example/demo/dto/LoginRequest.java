package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email é inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        String senha

) {

}
