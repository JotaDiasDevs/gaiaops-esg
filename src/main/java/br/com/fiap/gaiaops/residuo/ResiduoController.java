package br.com.fiap.gaiaops.residuo;

import java.util.List;

import org.bson.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/residuos")
@Tag(name = "Residuos", description = "Coleta seletiva, cooperativas e destinacao final (pilares E e S)")
public class ResiduoController {

    private final ResiduoService service;

    public ResiduoController(ResiduoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista coletas de residuos (mais recentes primeiro)")
    public List<Document> listar(@RequestParam(required = false) String categoria,
                                 @RequestParam(defaultValue = "50") int limite) {
        return service.listar(categoria, limite);
    }

    @GetMapping("/resumo")
    @Operation(summary = "Peso coletado por categoria e tipo de destinacao")
    public List<Document> resumo() {
        return service.resumoPorCategoria();
    }
}
