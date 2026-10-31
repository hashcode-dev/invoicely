package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.vendor.VendorContactDto;
import com.hashcode.invoicely.dto.vendor.VendorDto;
import com.hashcode.invoicely.entity.relational.VendorContactEntity;
import com.hashcode.invoicely.entity.relational.VendorEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class VendorMapper {

    public VendorDto toDto(VendorEntity entity) {
        if (entity == null) {
            return null;
        }

        List<VendorContactDto> contactDtos = entity.getContacts() != null
                ? entity.getContacts().stream().map(this::toContactDto).collect(Collectors.toList())
                : Collections.emptyList();

        return VendorDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .assignedUserId(entity.getAssignedUserId())
                .name(entity.getName())
                .number(entity.getVendorNumber())
                .website(entity.getWebsite())
                .phone(entity.getPhone())
                .currencyId(entity.getCurrencyId())
                .address1(entity.getAddress1())
                .address2(entity.getAddress2())
                .city(entity.getCity())
                .state(entity.getState())
                .postalCode(entity.getPostalCode())
                .countryId(entity.getCountryId())
                .vatNumber(entity.getVatNumber())
                .idNumber(entity.getIdNumber())
                .routingId(entity.getRoutingId())
                .publicNotes(entity.getPublicNotes())
                .privateNotes(entity.getPrivateNotes())
                .customValue1(entity.getCustomValue1())
                .customValue2(entity.getCustomValue2())
                .customValue3(entity.getCustomValue3())
                .customValue4(entity.getCustomValue4())
                .isTaxExempt(entity.isTaxExempt())
                .isDeleted(entity.isDeleted())
                .archivedAt(entity.getArchivedAt() != null ? entity.getArchivedAt().getEpochSecond() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().getEpochSecond() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().getEpochSecond() : null)
                .contacts(contactDtos)
                .documents(Collections.emptyList())
                .build();
    }

    public VendorContactDto toContactDto(VendorContactEntity contact) {
        if (contact == null) {
            return null;
        }
        return VendorContactDto.builder()
                .id(contact.getId())
                .firstName(contact.getFirstName())
                .lastName(contact.getLastName())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .isPrimary(contact.isPrimary())
                .sendEmail(contact.isSendEmail())
                .customValue1(contact.getCustomValue1())
                .customValue2(contact.getCustomValue2())
                .customValue3(contact.getCustomValue3())
                .customValue4(contact.getCustomValue4())
                .createdAt(contact.getCreatedAt() != null ? contact.getCreatedAt().getEpochSecond() : null)
                .updatedAt(contact.getUpdatedAt() != null ? contact.getUpdatedAt().getEpochSecond() : null)
                .archivedAt(contact.getArchivedAt() != null ? contact.getArchivedAt().getEpochSecond() : null)
                .build();
    }
}
