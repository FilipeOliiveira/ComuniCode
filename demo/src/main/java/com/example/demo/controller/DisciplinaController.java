package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.security.UsuarioAutenticado;
import com.example.demo.services.DisciplinaService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/disciplinas")
public class DisciplinaController {
    private final DisciplinaService service;
    public DisciplinaController(DisciplinaService service) { this.service = service; }

    @GetMapping
    public List<DisciplinaResponse> listar(@RequestParam(required = false) String nome) {
        return (nome == null ? service.listarTodas() : service.buscarPorNome(nome))
                .stream().map(DisciplinaResponse::de).toList();
    }

    @GetMapping("/{id}")
    public DisciplinaResponse buscar(@PathVariable UUID id) { return DisciplinaResponse.de(service.buscarPorId(id)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DisciplinaResponse criar(@RequestBody DisciplinaRequest request,
                                    @AuthenticationPrincipal UsuarioAutenticado principal) {
        return DisciplinaResponse.de(service.criarDisciplina(request.toEntity(), principal.getId()));
    }
}
