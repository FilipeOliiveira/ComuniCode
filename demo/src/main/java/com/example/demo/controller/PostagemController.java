package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.services.PostagemApiService;
import com.example.demo.services.PublicacaoService;
import com.example.demo.security.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/postagens")
public class PostagemController {

    public record EdicaoRequest(String titulo, String descricao) {}
    private final PostagemApiService service;
    private final PublicacaoService publicacao;

    public PostagemController(PostagemApiService service, PublicacaoService publicacao) {
        this.service = service;
        this.publicacao = publicacao;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostagemResponse criar(@RequestBody PostagemRequest request,
                                  @AuthenticationPrincipal UsuarioAutenticado principal) {
        return service.criar(request, principal.getId());
    }

    /** Publicacao pela interface: parte "dados" (JSON) e ate 5 arquivos na parte "arquivos". */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PostagemResponse publicar(@RequestPart("dados") PublicacaoRequest dados,
                                     @RequestPart(value = "arquivos", required = false) List<MultipartFile> arquivos,
                                     @AuthenticationPrincipal UsuarioAutenticado principal) {
        return publicacao.publicar(dados, arquivos, principal.getId());
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
