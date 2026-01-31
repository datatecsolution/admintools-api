package net.datatecsolution.admintools.web.controller;

import net.datatecsolution.admintools.domain.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = { "http://201.190.38.238", "http://localhost:3000/" })
public class AuthCtl {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private net.datatecsolution.admintools.config.JwtUtil jwtUtil;

    @Autowired
    private net.datatecsolution.admintools.domain.service.CustomUserDetailsService userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            final org.springframework.security.core.userdetails.UserDetails userDetails = userDetailsService
                    .loadUserByUsername(loginRequest.getUsername());

            final String jwt = jwtUtil.generateToken(userDetails);

            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("token", jwt);
            response.put("username", userDetails.getUsername());

            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Authentication failed: " + e.getMessage());
        }
    }
}
