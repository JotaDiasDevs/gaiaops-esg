package br.com.fiap.gaiaops.equipamento;

import java.util.Map;

import br.com.fiap.gaiaops.comum.Dominio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Dados para cadastrar um equipamento publico.
 *
 * Os campos obrigatorios espelham o validador $jsonSchema da collection
 * equipamentos. Os atributos especificos de cada tipo (turnos de uma escola,
 * vazao de uma ETE...) vao em "atributos" e sao gravados livremente no
 * documento - e o esquema flexivel da Fase 5 preservado na API.
 */
public record NovoEquipamento(

        @Schema(example = "EQP-ESC-002")
        @NotBlank @Pattern(regexp = Dominio.CODIGO_EQUIPAMENTO, message = "deve seguir o padrao EQP-XXX-NNN")
        String codigo,

        @Schema(example = "EMEF Jardim das Flores")
        @NotBlank @Size(min = 3, max = 120)
        String nome,

        @Schema(example = "escola")
        @NotBlank @Pattern(regexp = Dominio.TIPOS_EQUIPAMENTO,
                message = "deve ser escola, saude, praca, saneamento, iluminacao, ecoponto ou terminal")
        String tipo,

        @Schema(example = "Morro Alto")
        @NotBlank @Size(min = 3, max = 80)
        String distrito,

        @Schema(example = "640")
        @PositiveOrZero
        Integer populacaoAtendida,

        @Schema(description = "Campos livres, especificos do tipo de equipamento",
                example = "{\"escola\": {\"turnos\": [\"manha\", \"tarde\"], \"salas\": 12}}")
        Map<String, Object> atributos) {
}
