package br.com.fiap.gaiaops.medicao;

import java.time.Instant;
import java.util.Map;

import br.com.fiap.gaiaops.comum.Dominio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

/**
 * Leitura enviada por um sensor urbano (ingestao IoT).
 *
 * "leitura" e "referencia" sao livres porque cada tipo de sensor mede grandezas
 * diferentes: IQAr para qualidade do ar, LAeq para ruido, nivel_cm para corregos.
 */
public record NovaMedicao(

        @Schema(example = "EQP-PRA-002")
        @NotBlank @Pattern(regexp = Dominio.CODIGO_EQUIPAMENTO, message = "deve seguir o padrao EQP-XXX-NNN")
        String equipamentoId,

        @Schema(example = "qualidade_ar")
        @NotBlank @Pattern(regexp = Dominio.TIPOS_MEDICAO,
                message = "deve ser qualidade_ar, ruido, energia, agua, trafego ou hidrologia")
        String tipo,

        @Schema(description = "Momento da leitura (UTC). Se omitido, usa o horario do servidor.")
        Instant data,

        @Schema(example = "{\"id\": \"SEN-AR-004\", \"modelo\": \"Libelium Smart Environment PRO\"}")
        Map<String, Object> sensor,

        @Schema(example = "{\"mp10_ug_m3\": 91, \"indice_iqar\": 142}")
        @NotEmpty
        Map<String, Object> leitura,

        @Schema(example = "{\"norma\": \"CONAMA 491/2018\"}")
        Map<String, Object> referencia) {
}
