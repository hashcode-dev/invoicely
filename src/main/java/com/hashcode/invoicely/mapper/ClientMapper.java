package com.hashcode.invoicely.mapper;

import com.hashcode.invoicely.dto.client.ClientContactDto;
import com.hashcode.invoicely.dto.client.ClientDto;
import com.hashcode.invoicely.entity.relational.ClientContactEntity;
import com.hashcode.invoicely.entity.relational.ClientEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ClientMapper {

    public ClientDto toDto(ClientEntity entity) {
        if (entity == null) {
            return null;
        }

        List<ClientContactDto> contactDtos = entity.getContacts() != null
                ? entity.getContacts().stream().map(this::toContactDto).collect(Collectors.toList())
                : Collections.emptyList();

        return ClientDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .assignedUserId(entity.getAssignedUserId())
                .name(entity.getName())
                .displayName(entity.getName())
                .number(entity.getClientNumber())
                .website(entity.getWebsite())
                .phone(entity.getPhone())
                .balance(entity.getBalance())
                .paidToDate(entity.getPaidToDate())
                .creditBalance(entity.getCreditBalance())
                .currencyId(entity.getCurrencyId())
                .address1(entity.getAddress1())
                .address2(entity.getAddress2())
                .city(entity.getCity())
                .state(entity.getState())
                .postalCode(entity.getPostalCode())
                .countryId(entity.getCountryId())
                .shippingAddress1(entity.getShippingAddress1())
                .shippingAddress2(entity.getShippingAddress2())
                .shippingCity(entity.getShippingCity())
                .shippingState(entity.getShippingState())
                .shippingPostalCode(entity.getShippingPostalCode())
                .shippingCountryId(entity.getShippingCountryId())
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
                .settings(Collections.emptyMap())
                .gatewayTokens(Collections.emptyList())
                .documents(Collections.emptyList())
                .build();
    }

    public ClientContactDto toContactDto(ClientContactEntity contact) {
        if (contact == null) {
            return null;
        }
        return ClientContactDto.builder()
                .id(contact.getId())
                .firstName(contact.getFirstName())
                .lastName(contact.getLastName())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .isPrimary(contact.isPrimary())
                .sendEmail(contact.isSendEmail())
                .contactKey(contact.getContactKey())
                .customValue1(contact.getCustomValue1())
                .customValue2(contact.getCustomValue2())
                .customValue3(contact.getCustomValue3())
                .customValue4(contact.getCustomValue4())
                .isLocked(contact.isLocked())
                .createdAt(contact.getCreatedAt() != null ? contact.getCreatedAt().getEpochSecond() : null)
                .updatedAt(contact.getUpdatedAt() != null ? contact.getUpdatedAt().getEpochSecond() : null)
                .archivedAt(contact.getArchivedAt() != null ? contact.getArchivedAt().getEpochSecond() : null)
                .build();
    }
}
