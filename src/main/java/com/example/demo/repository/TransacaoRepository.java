package com.example.demo.repository;

import com.example.demo.entity.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    List<Transacao> findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(
            Long contaOrigemId,
            Long contaDestinoId
    );

}
