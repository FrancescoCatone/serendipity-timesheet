package com.serendipity.backend.model.enums;

public enum TimesheetStato {
    APERTO,       // modificabile da dipendente
    CONFERMATO,   // tutte le righe presenti; il dipendente può solo tornare ad APERTO
    CHIUSO        // non modificabile; solo ADMIN può riaprire (→ CONFERMATO)
}
