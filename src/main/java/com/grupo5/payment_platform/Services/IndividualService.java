package com.grupo5.payment_platform.Services;

import com.grupo5.payment_platform.DTOs.IndividualRequestDTO;
import com.grupo5.payment_platform.DTOs.IndividualResponseDTO;
import com.grupo5.payment_platform.Exceptions.IndividualNotFoundException;
import com.grupo5.payment_platform.Models.IndividualModel;
import com.grupo5.payment_platform.Repositories.IndividualRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class IndividualService {

    private final IndividualRepository individualRepository;

    public IndividualService(IndividualRepository individualRepository) {
        this.individualRepository = individualRepository;
    }

    //Metodo para listar todas as entidades
    public List<IndividualResponseDTO> findAll(){
        return individualRepository.findAll()
                .stream()
                .map(IndividualResponseDTO::fromIndividual)
                .toList();
    }

    //Metodo para listar entidade por id
    public IndividualResponseDTO findById(UUID id){
       IndividualModel individual = individualRepository.findById(id).orElseThrow(() ->
               new IndividualNotFoundException("User Not Found"));
        return  IndividualResponseDTO.fromIndividual(individual);
    }

    //Metodo para criar uma entidade
    public IndividualResponseDTO create(IndividualRequestDTO dto){
        IndividualModel ind = new IndividualModel();
        ind.setFullName(dto.fullname());
        ind.setCpf(dto.cpf());
        ind.setEmail(dto.email());
        ind.setBalance(dto.balance());
        individualRepository.save(ind);
        return IndividualResponseDTO.fromIndividual(ind);
    }


    //Metodo para alterar a entidade por id
    public IndividualResponseDTO alter(UUID id, IndividualRequestDTO dto){
        IndividualModel updateIndividual = individualRepository.findById(id).orElseThrow(()->
                new IndividualNotFoundException("User Not Found"));
        updateIndividual.setFullName(dto.fullname());
        updateIndividual.setCpf(dto.cpf());
        updateIndividual.setEmail(dto.email());
        individualRepository.save(updateIndividual);
        return IndividualResponseDTO.fromIndividual(updateIndividual);
    }

}
