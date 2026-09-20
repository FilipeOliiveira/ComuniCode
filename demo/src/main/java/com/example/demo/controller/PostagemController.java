package com.example.demo.controller;

import com.example.demo.model.Postagem;
import com.example.demo.service.PostagemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/postagens")
public class PostagemController {

    @Autowired
    private PostagemService service;

    @PostMapping
    public ResponseEntity<Postagem> criar(@RequestBody Postagem postagem) {
        return ResponseEntity.ok(service.criarPostagem(postagem));
    }

    @GetMapping
    public ResponseEntity<List<Postagem>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }
}