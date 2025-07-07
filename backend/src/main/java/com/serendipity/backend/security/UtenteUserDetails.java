package com.serendipity.backend.security;

import com.serendipity.backend.model.entity.Utente;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class UtenteUserDetails implements UserDetails {

    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    private UtenteUserDetails(String email, String password, String ruolo) {
        this.username = email;
        this.password = password;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + ruolo));
    }

    public static UtenteUserDetails build(Utente utente) {
        return new UtenteUserDetails(
                utente.getEmail(),
                utente.getPassword(),
                utente.getRuolo().name()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UtenteUserDetails)) return false;
        UtenteUserDetails that = (UtenteUserDetails) o;
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
