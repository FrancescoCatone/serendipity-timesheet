package com.serendipity.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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
}

