package br.com.petfy.healthcare.exception;

import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PetfyHealthcareException.class)
    public ResponseEntity<ErrorResponse> handlePetfyHealthCareException(PetfyHealthcareException ex) {
        log.error("Handled business exception: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), ex.getCode(), ex.getHttpStatus().value(), LocalDateTime.now());
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    /**
     * Sem este handler, uma falha de @Valid cairia no handler generico abaixo e
     * voltaria como 500, escondendo do cliente que o problema esta no payload.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));

        log.warn("Requisicao invalida: {}", detalhes);

        ErrorResponse errorResponse = new ErrorResponse(
                detalhes,
                ErrorMessageEnum.INVALID_REQUEST.getCode(),
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Corpo que o Jackson nao conseguiu ler - JSON truncado, aspas soltas, acento em
     * Latin-1 onde a API espera UTF-8.
     *
     * Sem este handler a excecao cai no generico abaixo e volta <b>500</b>, dizendo que o
     * servidor falhou quando quem errou foi o cliente. O dano nao e so de etiqueta: 500
     * entra no alerta de erro do servidor, e o front traduz o codigo 500 como "nao
     * conseguimos agora, tente de novo" - conselho inutil, porque tentar de novo com o
     * mesmo corpo quebrado da o mesmo resultado.
     *
     * <b>A mensagem do Jackson nao vai no corpo</b>, de proposito: ela carrega trecho do
     * payload recebido, e payload desta API tem dado pessoal de tutor e de saude de animal.
     * O detalhe fica no log, onde ja existe controle de acesso.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleCorpoIlegivel(HttpMessageNotReadableException ex) {
        log.warn("Corpo ilegivel na requisicao: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                ErrorMessageEnum.MALFORMED_REQUEST_BODY.getMessage(),
                ErrorMessageEnum.MALFORMED_REQUEST_BODY.getCode(),
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Requisicao multipart sem a parte do arquivo.
     *
     * O contrato declara {@code required: ["file"]} nas duas rotas que recebem arquivo, e sem
     * este handler a falta dela caia no generico e voltava <b>500</b> — servidor assumindo a
     * culpa de um erro de quem chamou, e entrando no alerta de erro do servidor por isso.
     *
     * O codigo e proprio, e nao o ATTACHMENT_EMPTY: arquivo vazio e um arquivo escolhido, e a
     * tela responde "esse arquivo esta vazio". Aqui nao veio arquivo nenhum, e o que a tela tem
     * a dizer e "escolha um arquivo".
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleParteAusente(MissingServletRequestPartException ex) {
        log.warn("Requisicao multipart sem a parte esperada: {}", ex.getRequestPartName());

        ErrorResponse errorResponse = new ErrorResponse(
                ErrorMessageEnum.MISSING_FILE_PART.getMessage(),
                ErrorMessageEnum.MISSING_FILE_PART.getCode(),
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(
                ErrorMessageEnum.INTERNAL_ERROR.getMessage(),
                ErrorMessageEnum.INTERNAL_ERROR.getCode(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now());

        return ResponseEntity.internalServerError().body(error);
    }

}
