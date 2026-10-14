package br.com.fiap.gaiaops.medicao;

import java.util.List;

import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Telemetria urbana: qualidade do ar, ruido, energia, agua, trafego e hidrologia. */
@RestController
@RequestMapping("/api/medicoes")
@Tag(name = "Medicoes", description = "Telemetria dos sensores urbanos e alertas automaticos (pilar E)")
public class MedicaoController {

    private final MedicaoService service;

    public MedicaoController(MedicaoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista medicoes (mais recentes primeiro)")
    public List<Document> listar(@RequestParam(name = "equipamento_id", required = false) String equipamentoId,
                                 @RequestParam(required = false) String tipo,
                                 @RequestParam(defaultValue = "50") int limite) {
        return service.listar(equipamentoId, tipo, limite);
    }

    @GetMapping("/alertas")
    @Operation(summary = "Lista as medicoes que dispararam alerta")
    public List<Document> alertas(@RequestParam(required = false) String nivel,
                                  @RequestParam(name = "somente_abertos", defaultValue = "false") boolean somenteAbertos) {
        return service.alertas(nivel, somenteAbertos);
    }

    @PostMapping
    @Operation(summary = "Registra uma leitura de sensor e avalia as regras de alerta")
    public ResponseEntity<Document> registrar(@RequestBody @Valid NovaMedicao nova) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(nova));
    }
}
