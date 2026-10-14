package br.com.fiap.gaiaops.comum;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converte as excecoes em respostas padronizadas (RFC 9457 - Problem Details),
 * com mensagens em portugues.
 *
 * Estende ResponseEntityExceptionHandler para que os erros padrao do Spring MVC
 * (JSON malformado, metodo nao suportado...) tambem saiam como Problem Details.
 */
@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Recurso nao encontrado", e.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    public ProblemDetail conflito(ConflitoException e) {
        return problema(HttpStatus.CONFLICT, "Conflito", e.getMessage());
    }

    /** Violacao de indice unico detectada pelo proprio banco (ex.: corrida entre duas criacoes). */
    @ExceptionHandler(DuplicateKeyException.class)
    public ProblemDetail chaveDuplicada(DuplicateKeyException e) {
        return problema(HttpStatus.CONFLICT, "Conflito", "Registro duplicado: violacao de indice unico");
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail regraNegocio(RegraNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negocio violada", e.getMessage());
    }

    /**
     * Documento recusado pelo validador $jsonSchema da collection. E a segunda
     * barreira: a primeira e a Bean Validation dos DTOs.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail rejeitadoPeloBanco(DataIntegrityViolationException e) {
        log.warn("Documento rejeitado pelo MongoDB: {}", e.getMostSpecificCause().getMessage());
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Documento rejeitado pelo banco",
                "O documento nao atende ao validador da collection");
    }

    /** Erros de Bean Validation: devolve a lista campo -> mensagem. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));

        ProblemDetail problema = e.getBody();
        problema.setTitle("Dados invalidos");
        problema.setDetail("Um ou mais campos nao passaram na validacao");
        problema.setProperty("erros", erros);
        return handleExceptionInternal(e, problema, headers, status, request);
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
