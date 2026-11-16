package com.example.demo.exception;

/** Falha ao falar com o armazenamento de arquivos (Supabase Storage); vira HTTP 503. */
public class ArmazenamentoIndisponivelException extends RuntimeException {
    public ArmazenamentoIndisponivelException(String mensagem) { super(mensagem); }
    public ArmazenamentoIndisponivelException(String mensagem, Throwable causa) { super(mensagem, causa); }
}
