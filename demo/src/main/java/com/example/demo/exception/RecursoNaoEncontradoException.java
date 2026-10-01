package com.example.demo.exception;

public class RecursoNaoEncontradoException extends IllegalArgumentException {
    public RecursoNaoEncontradoException(String mensagem) { super(mensagem); }
}
