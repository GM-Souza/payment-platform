package com.grupo5.payment_platform.Controllers;

import com.grupo5.payment_platform.Services.DatabaseCleanerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/db")
public class DatabaseCleanerController {

    private final DatabaseCleanerService cleanerService;

    public DatabaseCleanerController(DatabaseCleanerService cleanerService) {
        this.cleanerService = cleanerService;
    }

    @DeleteMapping("/limpar")
    public ResponseEntity<String> limparTudo() {
        cleanerService.limparTudo();
        return ResponseEntity.ok("Banco de dados limpo com sucesso!");
    }
}