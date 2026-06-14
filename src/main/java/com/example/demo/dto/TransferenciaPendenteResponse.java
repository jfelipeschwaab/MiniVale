package com.example.demo.dto;

import java.time.Instant;

public record TransferenciaPendenteResponse(
        Long id,
        Instant expiraEm,
        String codigoOtpSimulado
) {
}
