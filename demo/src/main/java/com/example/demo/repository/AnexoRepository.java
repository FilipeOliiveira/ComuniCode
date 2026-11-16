package com.example.demo.repository;

import com.example.demo.model.Anexo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AnexoRepository extends JpaRepository<Anexo, UUID> {
}
