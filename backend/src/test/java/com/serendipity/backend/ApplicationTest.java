package com.serendipity.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
class ApplicationTest {

    /**
     * Verifica che il contesto Spring si carichi correttamente.
     * Copre la presenza dell'annotazione @SpringBootApplication e la corretta
     * configurazione di tutti i bean.
     */
    @Test
    void contextLoads() {
        // se arriva qui il contesto è partito senza eccezioni
    }

    /**
     * Verifica che il metodo main() sia invocabile senza eccezioni.
     * Usa un array di argomenti vuoto, come in produzione.
     * L'applicazione viene avviata tramite SpringApplication che internamente
     * riusa il contesto già caricato da @SpringBootTest.
     */
    @Test
    void main_doesNotThrow() {
        assertThatCode(() -> Application.main(new String[]{}))
                .doesNotThrowAnyException();
    }
}

