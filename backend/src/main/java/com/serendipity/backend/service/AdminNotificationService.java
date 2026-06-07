package com.serendipity.backend.service;

import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.TimesheetStato;

public interface AdminNotificationService {

    void notifyUtenteCreated(String actorEmail, Utente createdUser);

    void notifyUtenteUpdated(String actorEmail, Utente previousUser, Utente updatedUser);

    void notifyUtenteDeleted(String actorEmail, Utente deletedUser);

    void notifyPasswordChanged(String actorEmail, Utente user);

    void notifyClienteCreated(String actorEmail, Cliente createdCliente);

    void notifyClienteUpdated(String actorEmail, Cliente previousCliente, Cliente updatedCliente);

    void notifyClienteDeleted(String actorEmail, Cliente deletedCliente);

    void notifyTimesheetConfermato(String actorEmail, Timesheet timesheet);

    void notifyTimesheetChiuso(String actorEmail, Timesheet timesheet);

    void notifyTimesheetRiaperto(String actorEmail, Timesheet timesheet, TimesheetStato previousState);

    void notifyTimesheetEliminato(String actorEmail, Timesheet timesheet);
}
