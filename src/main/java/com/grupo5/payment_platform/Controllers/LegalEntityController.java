package com.grupo5.payment_platform.Controllers;

import com.grupo5.payment_platform.DTOs.LegalEntityDTOs.LegalEntityRequestDTO;
import com.grupo5.payment_platform.DTOs.LegalEntityDTOs.LegalEntityResponseDTO;
import com.grupo5.payment_platform.Services.UsersServices.LegalEntityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/LegalEntity")
public class LegalEntityController {

    private final LegalEntityService legalEntityService;

    public LegalEntityController(LegalEntityService legalEntityService) {
        this.legalEntityService = legalEntityService;
    }

    @PostMapping
    public ResponseEntity<LegalEntityResponseDTO> createIndividual(@RequestBody LegalEntityRequestDTO entity) {
        LegalEntityResponseDTO newEntity = legalEntityService.create(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(newEntity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LegalEntityResponseDTO> getIndividualById(@PathVariable UUID id) {
        LegalEntityResponseDTO findEntity = legalEntityService.findById(id);
        return ResponseEntity.ok(findEntity);
    }

    @GetMapping
    public ResponseEntity<List<LegalEntityResponseDTO>> getAllEntities() {
        List<LegalEntityResponseDTO> AllEntities = legalEntityService.findAll();
        return ResponseEntity.ok(AllEntities);
    }

    @PutMapping("{id}")
    public ResponseEntity<LegalEntityResponseDTO> updateIndividual(@PathVariable UUID id, @RequestBody LegalEntityRequestDTO entity) {
        LegalEntityResponseDTO alterIndividual = legalEntityService.alter(id, entity);
        return ResponseEntity.ok(alterIndividual);
    }
}
