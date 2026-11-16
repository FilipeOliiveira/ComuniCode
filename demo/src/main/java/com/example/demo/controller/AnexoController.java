package com.example.demo.controller;

import com.example.demo.services.AnexoService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/anexos")
public class AnexoController {
    private final AnexoService service;

    public AnexoController(AnexoService service) { this.service = service; }

    /** Confere a sessao e redireciona para um link temporario (60 s) do Supabase Storage. */
    @GetMapping("/{id}/arquivo")
    public ResponseEntity<Void> abrir(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.FOUND).location(service.linkTemporario(id))
                .cacheControl(CacheControl.noStore()).build();
    }
}
