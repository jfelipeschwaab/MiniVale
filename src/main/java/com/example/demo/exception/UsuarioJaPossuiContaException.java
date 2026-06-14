package com.example.demo.exception;

public class UsuarioJaPossuiContaException extends RuntimeException {

    public UsuarioJaPossuiContaException(Long usuarioId) {
        super("Usuário com id " + usuarioId + " já possui uma conta");
    }

}
