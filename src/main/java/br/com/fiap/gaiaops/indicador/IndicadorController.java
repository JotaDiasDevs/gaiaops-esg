package br.com.fiap.gaiaops.indicador;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/indicadores")
@Tag(name = "Indicadores", description = "Painel ESG consolidado dos tres pilares")
public class IndicadorController {

    private final IndicadorService service;

    public IndicadorController(IndicadorService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Painel ESG do municipio (ambiental, social e governanca)")
    public PainelEsg painel() {
        return service.painel();
    }
}
