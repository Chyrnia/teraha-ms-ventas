package com.teraha.sales.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapping;

import com.teraha.commons.entities.SaleInvoice;
import com.teraha.commons.entities.SaleInvoiceDetail;
import com.teraha.commons.dtos.SaleInvoiceDTO;
import com.teraha.commons.dtos.SaleInvoiceDetailDTO;


@Mapper(componentModel = "spring")
public interface SaleInvoiceMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(source = "thirdParty.id", target = "clientId")
    SaleInvoiceDTO toDto(SaleInvoice saleInvoice);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(source = "product.id", target = "productId")
    SaleInvoiceDetailDTO toDto(SaleInvoiceDetail detail);

}