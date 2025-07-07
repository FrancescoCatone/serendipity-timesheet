package com.serendipity.backend.exception;

import com.serendipity.backend.model.dto.ResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    //Permessi negati (es. ruoli sbagliati)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseMessage> handleAccessDenied(AccessDeniedException ex) {
        logger.warn("Accesso negato: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ResponseMessage(403, "Non hai i permessi per eseguire questa azione."));
    }

    //Errore generico
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneric(Exception ex) {
        logger.error("Errore interno: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Si è verificato un errore interno. Contattare l'amministratore.");
    }

    //Validazione fallita
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
        String errore = ex.getBindingResult().getFieldError().getDefaultMessage();
        logger.warn("Validazione fallita: {}", errore);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Dati non validi: " + errore);
    }
}
