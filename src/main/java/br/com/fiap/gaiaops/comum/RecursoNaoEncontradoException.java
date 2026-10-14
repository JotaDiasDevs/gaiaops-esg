package br.com.fiap.gaiaops.comum;

/** Recurso solicitado nao existe na base. Vira HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
