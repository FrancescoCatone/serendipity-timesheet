package com.serendipity.backend.exception;

import com.serendipity.backend.model.dto.ResponseMessage;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleDataIntegrity_keepsApplicationMessageWhenItIsExplicit() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Email gia esistente");

        ResponseEntity<ResponseMessage> response = handler.handleDataIntegrity(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Email gia esistente");
    }

    @Test
    void handleDataIntegrity_hidesWrappedDatabaseMessage() {
        RuntimeException sqlCause = new RuntimeException(
                "duplicate key value violates unique constraint \"uk_utente_email\"");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement", sqlCause);

        ResponseEntity<ResponseMessage> response = handler.handleDataIntegrity(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Operazione non completata per un vincolo sui dati.");
    }
}
