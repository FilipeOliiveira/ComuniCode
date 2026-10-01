package com.example.demo.dto;

import com.example.demo.model.Tag;
import java.util.UUID;

public record TagResponse(UUID id, String nome) {
    public static TagResponse de(Tag tag) { return new TagResponse(tag.getId(), tag.getNome()); }
}
