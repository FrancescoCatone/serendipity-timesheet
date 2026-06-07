package com.serendipity.backend.service;

import com.serendipity.backend.config.AppNotificationProperties;
import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.TimesheetStato;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmailAdminNotificationService implements AdminNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailAdminNotificationService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppNotificationProperties notificationProperties;

    public EmailAdminNotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            AppNotificationProperties notificationProperties
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.notificationProperties = notificationProperties;
    }

    @Override
    public void notifyUtenteCreated(String actorEmail, Utente createdUser) {
        send(
                "Utente creato",
                lines(
                        header("Creazione utente", actorEmail),
                        "Target: " + describeUtente(createdUser)
                )
        );
    }

    @Override
    public void notifyUtenteUpdated(String actorEmail, Utente previousUser, Utente updatedUser) {
        send(
                "Utente aggiornato",
                lines(
                        header("Aggiornamento utente", actorEmail),
                        "Prima: " + describeUtente(previousUser),
                        "Dopo: " + describeUtente(updatedUser)
                )
        );
    }

    @Override
    public void notifyUtenteDeleted(String actorEmail, Utente deletedUser) {
        send(
                "Utente eliminato",
                lines(
                        header("Eliminazione utente", actorEmail),
                        "Target: " + describeUtente(deletedUser)
                )
        );
    }

    @Override
    public void notifyPasswordChanged(String actorEmail, Utente user) {
        send(
                "Password utente aggiornata",
                lines(
                        header("Cambio password", actorEmail),
                        "Utente coinvolto: " + describeUtente(user)
                )
        );
    }

    @Override
    public void notifyClienteCreated(String actorEmail, Cliente createdCliente) {
        send(
                "Cliente creato",
                lines(
                        header("Creazione cliente", actorEmail),
                        "Target: " + describeCliente(createdCliente)
                )
        );
    }

    @Override
    public void notifyClienteUpdated(String actorEmail, Cliente previousCliente, Cliente updatedCliente) {
        send(
                "Cliente aggiornato",
                lines(
                        header("Aggiornamento cliente", actorEmail),
                        "Prima: " + describeCliente(previousCliente),
                        "Dopo: " + describeCliente(updatedCliente)
                )
        );
    }

    @Override
    public void notifyClienteDeleted(String actorEmail, Cliente deletedCliente) {
        send(
                "Cliente eliminato",
                lines(
                        header("Eliminazione cliente", actorEmail),
                        "Target: " + describeCliente(deletedCliente)
                )
        );
    }

    @Override
    public void notifyTimesheetConfermato(String actorEmail, Timesheet timesheet) {
        send(
                "Timesheet confermato",
                lines(
                        header("Conferma timesheet", actorEmail),
                        "Target: " + describeTimesheet(timesheet)
                )
        );
    }

    @Override
    public void notifyTimesheetChiuso(String actorEmail, Timesheet timesheet) {
        send(
                "Timesheet chiuso",
                lines(
                        header("Chiusura timesheet", actorEmail),
                        "Target: " + describeTimesheet(timesheet)
                )
        );
    }

    @Override
    public void notifyTimesheetRiaperto(String actorEmail, Timesheet timesheet, TimesheetStato previousState) {
        send(
                "Timesheet riaperto",
                lines(
                        header("Riapertura timesheet", actorEmail),
                        "Stato precedente: " + previousState,
                        "Target: " + describeTimesheet(timesheet)
                )
        );
    }

    @Override
    public void notifyTimesheetEliminato(String actorEmail, Timesheet timesheet) {
        send(
                "Timesheet eliminato",
                lines(
                        header("Eliminazione timesheet", actorEmail),
                        "Target: " + describeTimesheet(timesheet)
                )
        );
    }

    private void send(String subjectSuffix, String body) {
        if (!notificationProperties.isEnabled()) {
            return;
        }

        String recipient = safeTrim(notificationProperties.getRecipientEmail());
        String from = safeTrim(notificationProperties.getFromEmail());

        if (recipient == null || from == null) {
            log.warn("Notifiche email abilitate ma configurazione incompleta: recipient/from mancanti.");
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Notifiche email abilitate ma JavaMailSender non disponibile.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipient);
        message.setFrom(from);
        message.setSubject(buildSubject(subjectSuffix));
        message.setText(body);

        mailSender.send(message);
    }

    private String buildSubject(String subjectSuffix) {
        String prefix = safeTrim(notificationProperties.getSubjectPrefix());
        return (prefix == null ? "Serendipity" : prefix) + " - " + subjectSuffix;
    }

    private String header(String operationLabel, String actorEmail) {
        return lines(
                "Operazione: " + operationLabel,
                "Eseguita da: " + fallback(actorEmail),
                "Data/Ora: " + LocalDateTime.now().format(DATE_TIME_FORMATTER)
        );
    }

    private String describeUtente(Utente user) {
        if (user == null) {
            return "utente non disponibile";
        }

        return String.format(
                "%s %s | email=%s | ruolo=%s | codiceFiscale=%s",
                fallback(user.getNome()),
                fallback(user.getCognome()),
                fallback(user.getEmail()),
                user.getRuolo() == null ? "-" : user.getRuolo().name(),
                fallback(user.getCodiceFiscale())
        );
    }

    private String describeCliente(Cliente cliente) {
        if (cliente == null) {
            return "cliente non disponibile";
        }

        return String.format(
                "%s | tariffaOraria=%.2f",
                fallback(cliente.getNome()),
                cliente.getTariffaOraria()
        );
    }

    private String describeTimesheet(Timesheet timesheet) {
        if (timesheet == null) {
            return "timesheet non disponibile";
        }

        Utente owner = timesheet.getUtente();
        return String.format(
                "id=%s | periodo=%02d/%d | stato=%s | utente=%s",
                fallback(timesheet.getId()),
                timesheet.getMese(),
                timesheet.getAnno(),
                timesheet.getStato() == null ? "-" : timesheet.getStato().name(),
                owner == null ? "-" : (fallback(owner.getNome()) + " " + fallback(owner.getCognome()) + " <" + fallback(owner.getEmail()) + ">")
        );
    }

    private String lines(String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                parts.add(value);
            }
        }
        return String.join(System.lineSeparator(), parts);
    }

    private String fallback(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
