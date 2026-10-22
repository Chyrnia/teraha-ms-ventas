package com.teraha.sales.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.teraha.commons.entities.Client;
import com.teraha.commons.dtos.ClientDTO;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    ClientDTO toDto(Client client);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    Client toEntity(ClientDTO clientDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(
            ClientDTO clientDTO,
            @MappingTarget Client client );
}