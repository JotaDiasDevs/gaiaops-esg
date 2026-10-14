package br.com.fiap.gaiaops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GaiaOps ESG - API de gestao ESG para Cidades Inteligentes.
 *
 * Expoe, via REST, a base MongoDB criada na Fase 5 (municipio ficticio de
 * Vila Nova do Horizonte): equipamentos publicos, telemetria dos sensores
 * urbanos, coleta de residuos, programas sociais e conformidade.
 */
@SpringBootApplication
public class GaiaOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(GaiaOpsApplication.class, args);
    }
}
