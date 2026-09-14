package br.ufn.tfg.teamrec.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Traduz excecoes em respostas HTTP consistentes.
 *
 * Concentrar isso aqui mantem os controllers limpos: eles nunca precisam
 * de try/catch nem devolvem stacktrace ao cliente.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Recurso inexistente -> 404. */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiError> tratarNaoEncontrado(RecursoNaoEncontradoException ex,
                                                        HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.de(404, "Nao encontrado", ex.getMessage(), req.getRequestURI()));
    }

    /** Violacao de regra do dominio -> 422. */
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ApiError> tratarRegraDeNegocio(RegraDeNegocioException ex,
                                                         HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                ApiError.de(422, "Regra de negocio violada", ex.getMessage(), req.getRequestURI()));
    }

    /** Registro duplicado detectado pela regra de negocio -> 409. */
    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ApiError> tratarConflito(ConflitoException ex,
                                                   HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.de(409, "Conflito", ex.getMessage(), req.getRequestURI()));
    }

    /** Microsservico de apoio fora do ar -> 503. */
    @ExceptionHandler(ServicoExternoException.class)
    public ResponseEntity<ApiError> tratarServicoExterno(ServicoExternoException ex,
                                                         HttpServletRequest req) {
        log.error("Falha ao comunicar com o servico externo [{}]", ex.getServico(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                ApiError.de(503, "Servico indisponivel", ex.getMessage(), req.getRequestURI()));
    }

    /** Corpo de requisicao reprovado pelo Bean Validation (@Valid) -> 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> tratarCorpoInvalido(MethodArgumentNotValidException ex,
                                                        HttpServletRequest req) {
        List<ApiError.CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ApiError.CampoInvalido(e.getField(), e.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(ApiError.deValidacao(
                400, "Um ou mais campos estao invalidos.", req.getRequestURI(), campos));
    }

    /** Validacao em parametros de path/query (@Validated no controller) -> 400. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> tratarParametroInvalido(ConstraintViolationException ex,
                                                            HttpServletRequest req) {
        List<ApiError.CampoInvalido> campos = ex.getConstraintViolations().stream()
                .map(v -> new ApiError.CampoInvalido(v.getPropertyPath().toString(), v.getMessage()))
                .toList();

        return ResponseEntity.badRequest().body(ApiError.deValidacao(
                400, "Um ou mais parametros estao invalidos.", req.getRequestURI(), campos));
    }

    /** Parametro de query/path reprovado pela validacao nativa do Spring MVC (@Min, @Max...) -> 400. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> tratarParametroInvalidoNoMetodo(HandlerMethodValidationException ex,
                                                                    HttpServletRequest req) {
        List<ApiError.CampoInvalido> campos = ex.getParameterValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream()
                        .map(erro -> new ApiError.CampoInvalido(
                                resultado.getMethodParameter().getParameterName(), erro.getDefaultMessage())))
                .toList();
        return ResponseEntity.badRequest().body(ApiError.deValidacao(
                400, "Um ou mais parametros estao invalidos.", req.getRequestURI(), campos));
    }

    /** URL inexistente -> 404 (em vez de cair no handler generico). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> tratarRotaInexistente(NoResourceFoundException ex,
                                                          HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.de(
                404, "Nao encontrado", "Rota inexistente: " + req.getRequestURI(), req.getRequestURI()));
    }

    /** Verbo HTTP nao suportado pelo endpoint -> 405. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> tratarMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex,
                                                             HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiError.de(
                405, "Metodo nao permitido", ex.getMessage(), req.getRequestURI()));
    }

    /** Corpo ausente ou JSON malformado -> 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> tratarCorpoIlegivel(HttpMessageNotReadableException ex,
                                                        HttpServletRequest req) {
        return ResponseEntity.badRequest().body(ApiError.de(
                400, "Requisicao malformada",
                "Corpo da requisicao ausente ou em formato JSON invalido.", req.getRequestURI()));
    }

    /** Parametro com tipo incompativel, ex.: /desenvolvedores/abc -> 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> tratarTipoIncompativel(MethodArgumentTypeMismatchException ex,
                                                           HttpServletRequest req) {
        return ResponseEntity.badRequest().body(ApiError.deValidacao(
                400, "Parametro com tipo invalido.", req.getRequestURI(),
                List.of(new ApiError.CampoInvalido(ex.getName(),
                        "Valor \"" + ex.getValue() + "\" nao e compativel com o tipo esperado."))));
    }

    /** Violacao de restricao do banco, ex.: nome unico duplicado -> 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> tratarConflitoDeDados(DataIntegrityViolationException ex,
                                                          HttpServletRequest req) {
        log.warn("Violacao de integridade em {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.de(
                409, "Conflito de dados",
                "A operacao viola uma restricao de integridade (registro duplicado ou em uso).",
                req.getRequestURI()));
    }

    /** Rede de seguranca: qualquer falha nao prevista -> 500, sem vazar detalhes internos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> tratarErroInesperado(Exception ex, HttpServletRequest req) {
        log.error("Erro nao tratado em {}", req.getRequestURI(), ex);
        return ResponseEntity.internalServerError().body(ApiError.de(
                500, "Erro interno", "Ocorreu um erro inesperado ao processar a requisicao.",
                req.getRequestURI()));
    }
}
