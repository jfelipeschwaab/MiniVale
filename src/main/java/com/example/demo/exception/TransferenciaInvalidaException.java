package com.example.demo.exception;

public class TransferenciaInvalidaException extends RuntimeException {

    public TransferenciaInvalidaException(String mensagem) {
        super(mensagem);
    }

}