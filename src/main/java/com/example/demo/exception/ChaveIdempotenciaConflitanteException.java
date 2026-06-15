package com.example.demo.exception;

public class ChaveIdempotenciaConflitanteException extends RuntimeException {

    public ChaveIdempotenciaConflitanteException(String mensagem) {
        super(mensagem);
    }

}
