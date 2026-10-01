package com.example.demo.controller;

import com.example.demo.dto.TagResponse;
import com.example.demo.services.TagService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {
    public record TagRequest(String nome) {}
    private final TagService service;
    public TagController(TagService service) { this.service = service; }

    @GetMapping
    public List<TagResponse> listar() { return service.listarTodas().stream().map(TagResponse::de).toList(); }

    @GetMapping("/busca")
    public TagResponse buscar(@RequestParam String nome) { return TagResponse.de(service.buscarPorNome(nome)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse criar(@RequestBody TagRequest request) { return TagResponse.de(service.criarTag(request.nome())); }
}
