package com.grupo5.payment_platform.Controllers;

import com.grupo5.payment_platform.DTOs.AuthDTOs.IndividualRequestDTO;
import com.grupo5.payment_platform.DTOs.AuthDTOs.IndividualResponseDTO;
import com.grupo5.payment_platform.Services.UsersServices.IndividualService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/individual")
public class IndividualController {

    private final IndividualService individualService;

    public IndividualController(IndividualService individualService) {
        this.individualService = individualService;
    }

    @PostMapping
    // Endpoint para criar um novo indivíduo
    public ResponseEntity<IndividualResponseDTO> createIndividual(@RequestBody IndividualRequestDTO individual) {
        IndividualResponseDTO newIndividual = individualService.create(individual);
        return ResponseEntity.status(HttpStatus.CREATED).body(newIndividual);
    }

    @GetMapping("/{id}")
    // Endpoint para obter um indivíduo por ID
    public ResponseEntity<IndividualResponseDTO> getIndividualById(@PathVariable UUID id) {
        IndividualResponseDTO findIndividual = individualService.findById(id);
        return ResponseEntity.ok(findIndividual);
    }

    @GetMapping
    // Endpoint para obter todos os indivíduos
    public ResponseEntity<List<IndividualResponseDTO>> getAllIndividuals() {
        List<IndividualResponseDTO> allIndividuals = individualService.findAll();
        return ResponseEntity.ok(allIndividuals);
    }

    @PutMapping("{id}")
    // Endpoint para atualizar um indivíduo existente
    public ResponseEntity<IndividualResponseDTO> updateIndividual(@PathVariable UUID id, @RequestBody IndividualRequestDTO individual) {
        IndividualResponseDTO alterIndividual = individualService.alter(id, individual);
        return ResponseEntity.ok(alterIndividual);
    }

}
