package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.UsuarioAutenticado;
import com.example.demo.services.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioService service;
    private final UsuarioRepository repository;

    public AuthController(UsuarioService service, UsuarioRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal UsuarioAutenticado principal) {
        return repository.findById(principal.getId()).filter(u -> Boolean.TRUE.equals(u.getAtivo()))
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticacao necessaria."));
    }

    @PostMapping("/cadastro/aluno")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse aluno(@RequestBody CadastroAlunoRequest request) {
        return UsuarioResponse.de(service.cadastrarAluno(request.toEntity(), request.senha()));
    }

    @PostMapping("/cadastro/professor")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse professor(@RequestBody CadastroProfessorRequest request) {
        return UsuarioResponse.de(service.cadastrarProfessor(request.toEntity(), request.senha()));
    }
}
