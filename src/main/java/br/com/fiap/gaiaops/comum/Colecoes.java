package br.com.fiap.gaiaops.comum;

import java.util.List;

/**
 * Nomes das collections do MongoDB - os mesmos definidos na Fase 5
 * (scripts/00-setup.mongodb.js), para que API e scripts enxerguem a mesma base.
 */
public final class Colecoes {

    public static final String EQUIPAMENTOS = "equipamentos";
    public static final String MEDICOES = "medicoes";
    public static final String RESIDUOS = "residuos";
    public static final String PROGRAMAS_SOCIAIS = "programas_sociais";
    public static final String CONFORMIDADE = "conformidade";

    /** Ordem da carga inicial: equipamentos primeiro, pois as demais o referenciam. */
    public static final List<String> TODAS =
            List.of(EQUIPAMENTOS, MEDICOES, RESIDUOS, PROGRAMAS_SOCIAIS, CONFORMIDADE);

    private Colecoes() {
    }
}
