package com.example.demo.controller;

import com.example.demo.dto.ContaResponse;
import com.example.demo.dto.CriarContaRequest;
import com.example.demo.dto.TransacaoResponse;
import com.example.demo.dto.TransferenciaRequest;
import com.example.demo.entity.Conta;
import com.example.demo.service.ContaService;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/contas")
public class ContaController {
    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
    }


    @PostMapping
    public ResponseEntity<ContaResponse> criarConta
            (
            @Valid
            @RequestBody
            CriarContaRequest request
            )
    {
            ContaResponse contaCriada = contaService.criarConta(request);
            URI location = URI.create("/contas/" + contaCriada.id());
            return ResponseEntity.created(location).body(contaCriada);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContaResponse> consultarSaldo(@PathVariable Long id) {
        ContaResponse conta = contaService.consultarSaldo(id);
        return ResponseEntity.ok(conta);
    }

    @GetMapping("/{id}/extrato")
    public ResponseEntity<List<TransacaoResponse>> listarExtrato(@PathVariable Long id) {
        List<TransacaoResponse> extrato = contaService.listarExtrato(id);
        return ResponseEntity.ok(extrato);
    }

    @PostMapping("/transferencias")
    public ResponseEntity<Void> transferir(
            @Valid
            @RequestBody
            TransferenciaRequest request
    ) {
        contaService.transferir(request);
        return ResponseEntity.noContent().build();
    }
}


