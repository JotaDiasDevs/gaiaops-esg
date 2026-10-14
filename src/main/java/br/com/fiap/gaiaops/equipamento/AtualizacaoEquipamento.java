package br.com.fiap.gaiaops.equipamento;

import java.util.Map;

import br.com.fiap.gaiaops.comum.Dominio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Dados para atualizar um equipamento. O codigo vem da URL e nao pode mudar:
 * ele e a chave referenciada por medicoes, residuos e conformidade.
 *
 * A atualizacao e feita com $set campo a campo, entao atributos que nao vierem
 * na requisicao continuam no documento (nada e apagado por omissao).
 */
public record AtualizacaoEquipamento(

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

        @Schema(example = "700")
        @PositiveOrZero
        Integer populacaoAtendida,

        @Schema(description = "Campos livres a incluir ou substituir no documento",
                example = "{\"metas_esg\": {\"energia_kwh_mes\": 8500}}")
        Map<String, Object> atributos) {
}
