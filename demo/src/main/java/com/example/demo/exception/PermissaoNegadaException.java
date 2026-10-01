package com.example.demo.exception;

public class PermissaoNegadaException extends IllegalArgumentException {
    public PermissaoNegadaException(String mensagem) { super(mensagem); }
}
