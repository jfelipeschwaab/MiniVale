package com.example.demo.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
public record TransferenciaRequest(
        @NotNull(message = "contaOrigemId é obrigatório")
        Long contaOrigemId,

        @NotNull(message = "contaDestinoId é obrigatório")
        Long contaDestinoId,

        @NotNull
        @Positive(message = "Valor deve ser maior que zero")
        BigDecimal valor
) {
}
