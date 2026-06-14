package com.example.demo.exception;

public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException(String mensagem) {
        super(mensagem);
    }

}
