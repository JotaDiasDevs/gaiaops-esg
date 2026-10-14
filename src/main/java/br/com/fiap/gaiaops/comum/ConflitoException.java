package br.com.fiap.gaiaops.comum;

/** Operacao conflita com o estado atual (ex.: codigo ja cadastrado). Vira HTTP 409. */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
