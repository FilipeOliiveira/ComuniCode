package com.example.demo.controller;

import com.example.demo.exception.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record Erro(int status, String mensagem) {}

    private ResponseEntity<Erro> erro(int status, String mensagem) {
        return ResponseEntity.status(status).body(new Erro(status, mensagem));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<Erro> naoEncontrado(RecursoNaoEncontradoException ex) { return erro(404, ex.getMessage()); }

    @ExceptionHandler(PermissaoNegadaException.class)
    ResponseEntity<Erro> proibido(PermissaoNegadaException ex) { return erro(403, ex.getMessage()); }

    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<Erro> conflito(ConflitoException ex) { return erro(409, ex.getMessage()); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Erro> integridade(DataIntegrityViolationException ex) {
        return erro(409, "Operacao conflita com os dados existentes.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Erro> invalido(IllegalArgumentException ex) { return erro(400, ex.getMessage()); }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<Erro> formato(Exception ex) { return erro(400, "Corpo ou parametros da requisicao invalidos."); }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<Erro> credenciais(BadCredentialsException ex) { return erro(401, "Credenciais invalidas."); }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Erro> status(ResponseStatusException ex) { return erro(ex.getStatusCode().value(), ex.getReason()); }
}
