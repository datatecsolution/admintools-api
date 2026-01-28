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
@CrossOrigin(origins = {"http://201.190.38.238", "http://localhost:3000/"})
public class AuthCtl {
    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

//        System.out.println("login request name: ==========> " + loginRequest.getUsername());
//        System.out.println("login request name: ==========> " + loginRequest.getPassword());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
            return ResponseEntity.ok().body("Authenticated");
        } catch (AuthenticationException e) {
            //e.printStackTrace();
            return ResponseEntity.status(401).body("Authentication failed: " + e.getMessage());
        }
    }
}
