package br.com.fiap.gaiaops.config;

import java.time.Clock;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** Beans de infraestrutura compartilhados pela aplicacao. */
@Configuration
public class AplicacaoConfig {

    /** Relogio injetavel: permite testar regras dependentes de data sem depender da hora real. */
    @Bean
    public Clock relogio() {
        return Clock.systemUTC();
    }

    /**
     * A API devolve documentos do MongoDB como estao no banco (esquema flexivel).
     * Sem este ajuste, o _id (ObjectId) seria serializado como um objeto interno
     * do driver; com ele, sai como o texto hexadecimal que todos conhecem.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer objectIdComoTexto() {
        return builder -> builder.serializerByType(ObjectId.class, ToStringSerializer.instance);
    }

    /** Metadados exibidos no Swagger UI. */
    @Bean
    public OpenAPI documentacaoApi(@Value("${info.app.versao}") String versao,
                                   @Value("${gaiaops.ambiente}") String ambiente) {
        return new OpenAPI().info(new Info()
                .title("GaiaOps ESG API")
                .version(versao)
                .description("""
                        Gestao ESG para Cidades Inteligentes - municipio de Vila Nova do Horizonte.
                        Ambiente: %s""".formatted(ambiente)));
    }
}
