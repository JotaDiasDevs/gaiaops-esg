package br.com.fiap.gaiaops.comum;

/**
 * Valores de dominio aceitos pela API, espelhando os validadores $jsonSchema
 * das collections. Ficam como expressoes regulares para uso direto em @Pattern.
 */
public final class Dominio {

    /** Padrao do codigo de equipamento, ex.: EQP-ESC-001. */
    public static final String CODIGO_EQUIPAMENTO = "^EQP-[A-Z]{3}-[0-9]{3}$";

    public static final String TIPOS_EQUIPAMENTO =
            "^(escola|saude|praca|saneamento|iluminacao|ecoponto|terminal)$";

    public static final String TIPOS_MEDICAO =
            "^(qualidade_ar|ruido|energia|agua|trafego|hidrologia)$";

    /** Limite maximo de documentos devolvidos por consulta. */
    public static final int LIMITE_MAXIMO = 500;

    private Dominio() {
    }

    /** Mantem o limite pedido pelo cliente dentro de 1..LIMITE_MAXIMO. */
    public static int limitar(int limite) {
        return Math.max(1, Math.min(limite, LIMITE_MAXIMO));
    }
}
