package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CriarContaRequest(
        @NotNull(message = "usuarioId é obrigatório")
        Long usuarioId,

        @NotNull(message = "saldoInicial é obrigatório")
        @PositiveOrZero(message = "saldoInicial não pode ser negativo")
        BigDecimal saldoInicial
){
}
