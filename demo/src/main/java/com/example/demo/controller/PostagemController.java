package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.services.PostagemApiService;
import com.example.demo.security.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/postagens")
public class PostagemController {

    public record EdicaoRequest(String titulo, String descricao) {}
    private final PostagemApiService service;

    public PostagemController(PostagemApiService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostagemResponse criar(@RequestBody PostagemRequest request,
                                  @AuthenticationPrincipal UsuarioAutenticado principal) {
        return service.criar(request, principal.getId());
    }

    @GetMapping
    public List<PostagemResponse> listar(@RequestParam(required = false) UUID conteudoId,
                                        @RequestParam(required = false) UUID autorId,
                                        @RequestParam(required = false) UUID tagId) {
        return service.listar(conteudoId, autorId, tagId);
    }

    @GetMapping("/{id}")
    public PostagemResponse buscar(@PathVariable UUID id) { return service.buscar(id); }

    @PatchMapping("/{id}")
    public PostagemResponse editar(@PathVariable UUID id, @RequestBody EdicaoRequest request,
                                   @AuthenticationPrincipal UsuarioAutenticado principal) {
        return service.editar(id, principal.getId(), request.titulo(), request.descricao());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id, @AuthenticationPrincipal UsuarioAutenticado principal) {
        service.excluir(id, principal.getId());
    }
}
