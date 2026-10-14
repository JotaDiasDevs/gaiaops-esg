package br.com.fiap.gaiaops.comum;

/** Requisicao bem formada, mas que viola uma regra de negocio. Vira HTTP 422. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
