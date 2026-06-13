package com.example.demo.repository;

import com.example.demo.entity.Conta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface ContaRepository extends JpaRepository<Conta, Long> {
    Optional<Conta> findByUsuarioId(long usuarioId);
}
