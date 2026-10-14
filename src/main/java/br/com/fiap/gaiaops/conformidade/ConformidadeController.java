package br.com.fiap.gaiaops.conformidade;

import java.util.List;

import org.bson.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/conformidade")
@Tag(name = "Conformidade", description = "Licencas, audiencias, conselhos, ouvidoria e TACs (pilar G)")
public class ConformidadeController {

    private final ConformidadeService service;

    public ConformidadeController(ConformidadeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista registros de governanca, com filtro opcional por tipo")
    public List<Document> listar(@RequestParam(required = false) String tipo) {
        return service.listar(tipo);
    }

    @GetMapping("/licencas/vencimentos")
    @Operation(summary = "Licencas vencidas ou que vencem nos proximos N dias")
    public List<Document> vencimentos(@RequestParam(defaultValue = "180") int dias) {
        return service.licencasComVencimentoAte(dias);
    }
}
