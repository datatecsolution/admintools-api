package net.datatecsolution.admintools.web.controller;

import net.datatecsolution.admintools.domain.Costomer;
import net.datatecsolution.admintools.domain.service.CostomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/costomers")
@CrossOrigin(origins = { "http://201.190.38.238", "http://localhost:3000/" })
public class CostomerCtl {
    @Autowired
    private CostomerService costomerService;

    // @GetMapping("/all")
    // public ResponseEntity<List<Costomer>> getAll() {
    // return new ResponseEntity<>(costomerService.getdAll(), HttpStatus.OK);
    // }
    @GetMapping("name/{name}")
    public ResponseEntity<List<Costomer>> getByNome(@PathVariable String name, java.security.Principal principal) {
        String user = principal.getName();
        if (name == null || name.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return costomerService.getdByName(name, user)
                .map(costomer -> new ResponseEntity<>(costomer, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
