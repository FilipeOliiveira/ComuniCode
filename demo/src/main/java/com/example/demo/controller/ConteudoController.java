package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.services.ConteudoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conteudos")
public class ConteudoController {
    private final ConteudoService service;
    public ConteudoController(ConteudoService service) { this.service = service; }

    @GetMapping
    public List<ConteudoResponse> listar(@RequestParam UUID disciplinaId) {
        return service.listarPorDisciplina(disciplinaId).stream().map(ConteudoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public ConteudoResponse buscar(@PathVariable UUID id) { return ConteudoResponse.de(service.buscarPorId(id)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConteudoResponse criar(@RequestBody ConteudoRequest request) {
        return ConteudoResponse.de(service.criarConteudo(request.toEntity(), request.disciplinaId()));
    }
}
