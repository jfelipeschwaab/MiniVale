package com.example.demo.security;

import com.example.demo.repository.ContaRepository;
import com.example.demo.repository.TransferenciaPendenteRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class ContaSecurity {
    private final ContaRepository contaRepository;
    private final TransferenciaPendenteRepository transferenciaPendenteRepository;

    public ContaSecurity(ContaRepository contaRepository, TransferenciaPendenteRepository transferenciaPendenteRepository) {
        this.contaRepository = contaRepository;
        this.transferenciaPendenteRepository = transferenciaPendenteRepository;
    }

    public boolean isOwner(Long contaId, Authentication authentication) {
        return contaRepository.findById(contaId)
                .map(conta -> conta.getUsuario().getEmail().equals(authentication.getName()))
                .orElse(false);
    }

    public boolean isOwnerOfTransferenciaPendente(Long transferenciaPendenteId, Authentication authentication) {
        return transferenciaPendenteRepository.findById(transferenciaPendenteId)
                .map(pendente -> pendente.getContaOrigem().getUsuario().getEmail().equals(authentication.getName()))
                .orElse(false);
    }
}
