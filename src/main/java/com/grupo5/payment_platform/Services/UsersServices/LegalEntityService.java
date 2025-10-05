package com.grupo5.payment_platform.Services.UsersServices;

import com.grupo5.payment_platform.DTOs.AuthDTOs.LegalEntityRequestDTO;
import com.grupo5.payment_platform.DTOs.AuthDTOs.LegalEntityResponseDTO;
import com.grupo5.payment_platform.Exceptions.LegalEntityNotFoundException;
import com.grupo5.payment_platform.Models.Users.LegalEntityModel;
import com.grupo5.payment_platform.Repositories.LegalEntityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LegalEntityService {

    private final LegalEntityRepository legalEntityRepository;

    public LegalEntityService(LegalEntityRepository legalEntityRepository) {
        this.legalEntityRepository = legalEntityRepository;
    }

    //Metodo para listar todas as entidades
    public List<LegalEntityResponseDTO> findAll(){
        return legalEntityRepository.findAll()
                .stream()
                .map(LegalEntityResponseDTO::fromLegalEntity)
                .toList();
    }

    //Metodo para listar entidade por id
    public LegalEntityResponseDTO findById(UUID id){
        LegalEntityModel entity = legalEntityRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Usuário não encontrado"));
        return  LegalEntityResponseDTO.fromLegalEntity(entity);
    }

    //Metodo para criar uma entidade
    public LegalEntityResponseDTO create(LegalEntityRequestDTO dto){
        LegalEntityModel entity = new LegalEntityModel();
        entity.setLegalName(dto.legalName());
        entity.setCnpj(dto.cnpj());
        entity.setEmail(dto.email());
        entity.setBalance(dto.balance());
        legalEntityRepository.save(entity);
        return LegalEntityResponseDTO.fromLegalEntity(entity);
    }

    //Metodo para alterar a entidade por id
    public LegalEntityResponseDTO alter(UUID id, LegalEntityRequestDTO dto){
        LegalEntityModel updateEntity = legalEntityRepository.findById(id).orElseThrow(()->
                new LegalEntityNotFoundException("Usuário não encontrado"));
        updateEntity.setLegalName(dto.legalName());
        updateEntity.setCnpj(dto.cnpj());
        updateEntity.setEmail(dto.email());
        legalEntityRepository.save(updateEntity);
        return LegalEntityResponseDTO.fromLegalEntity(updateEntity);
    }

}
