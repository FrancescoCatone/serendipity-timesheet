package com.serendipity.backend.model.dto.update;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AggiornaPasswordDto {

    @NotBlank(message = "La password attuale è obbligatoria")
    private String oldPassword;

    @NotBlank(message = "La nuova password è obbligatoria")
    @Size(min = 6, max = 50, message = "La password deve contenere almeno 6 caratteri")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
