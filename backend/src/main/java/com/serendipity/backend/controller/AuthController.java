package com.serendipity.backend.controller;

import com.serendipity.backend.model.dto.AuthRequest;
import com.serendipity.backend.model.dto.AuthResponse;
import com.serendipity.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        var authToken = new UsernamePasswordAuthenticationToken(
                request.getEmail(), request.getPassword()
        );

        var authentication = authenticationManager.authenticate(authToken);
        var userDetails = (UserDetails) authentication.getPrincipal();

        String jwt = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(jwt));
    }

}
