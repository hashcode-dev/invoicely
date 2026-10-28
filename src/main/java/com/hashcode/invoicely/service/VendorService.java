package com.hashcode.invoicely.service;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.vendor.VendorBulkActionRequest;
import com.hashcode.invoicely.dto.vendor.VendorContactDto;
import com.hashcode.invoicely.dto.vendor.VendorDto;
import com.hashcode.invoicely.entity.relational.CompanyEntity;
import com.hashcode.invoicely.entity.relational.VendorContactEntity;
import com.hashcode.invoicely.entity.relational.VendorEntity;
import com.hashcode.invoicely.repository.relational.CompanyRepository;
import com.hashcode.invoicely.repository.relational.VendorRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final CompanyRepository companyRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Transactional(readOnly = true)
    public GenericManyResponse<VendorDto> getVendors(int page, int perPage, String status, String filter, String sort) {
        UUID companyId = getRequiredCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = perPage > 0 ? perPage : 20;

        Sort sorting = Sort.by(Sort.Direction.DESC, "updatedAt");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split("\\|");
            if (parts.length == 2) {
                sorting = Sort.by("desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
            }
        }

        Pageable pageable = PageRequest.of(pageIndex, pageSize, sorting);

        Specification<VendorEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("companyId"), companyId));

            if ("archived".equalsIgnoreCase(status)) {
                predicates.add(cb.isNotNull(root.get("archivedAt")));
                predicates.add(cb.isFalse(root.get("isDeleted")));
            } else if ("deleted".equalsIgnoreCase(status)) {
                predicates.add(cb.isTrue(root.get("isDeleted")));
            } else if ("all".equalsIgnoreCase(status)) {
                // Return everything under this company
            } else {
                // Default to active
                predicates.add(cb.isFalse(root.get("isDeleted")));
                predicates.add(cb.isNull(root.get("archivedAt")));
            }

            if (StringUtils.hasText(filter)) {
                String searchPattern = "%" + filter.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate numberMatch = cb.like(cb.lower(root.get("vendorNumber")), searchPattern);
                Predicate phoneMatch = cb.like(cb.lower(root.get("phone")), searchPattern);
                predicates.add(cb.or(nameMatch, numberMatch, phoneMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<VendorEntity> vendorPage = vendorRepository.findAll(spec, pageable);
        List<VendorDto> dtos = vendorPage.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return GenericManyResponse.of(dtos, vendorPage.getTotalElements(), page, pageSize);
    }

    @Transactional(readOnly = true)
    public VendorDto getBlankVendor() {
        UUID companyId = getRequiredCompanyId();
        String nextNumber = sequenceGeneratorService.generateSequence("vendor_" + companyId);

        String currencyId = "USD";
        String countryId = "US";
        Optional<CompanyEntity> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isPresent()) {
            currencyId = companyOpt.get().getCurrencyId();
            countryId = companyOpt.get().getCountryId();
        }

        VendorContactDto defaultContact = VendorContactDto.builder()
                .isPrimary(true)
                .sendEmail(true)
                .build();

        return VendorDto.builder()
                .number(nextNumber)
                .currencyId(currencyId)
                .countryId(countryId)
                .contacts(Collections.singletonList(defaultContact))
                .documents(Collections.emptyList())
                .build();
    }

    @Transactional(readOnly = true)
    public VendorDto getVendorById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        VendorEntity entity = vendorRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + id));
        return toDto(entity);
    }

    @Transactional
    public VendorDto createVendor(VendorDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        String vendorNumber = StringUtils.hasText(dto.getNumber())
                ? dto.getNumber().trim()
                : sequenceGeneratorService.generateSequence("vendor_" + companyId);

        String name = StringUtils.hasText(dto.getName()) ? dto.getName().trim() : "Unnamed Vendor";

        VendorEntity entity = VendorEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .assignedUserId(dto.getAssignedUserId())
                .vendorNumber(vendorNumber)
                .name(name)
                .website(dto.getWebsite())
                .phone(dto.getPhone())
                .currencyId(StringUtils.hasText(dto.getCurrencyId()) ? dto.getCurrencyId() : "USD")
                .address1(dto.getAddress1())
                .address2(dto.getAddress2())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .countryId(dto.getCountryId())
                .vatNumber(dto.getVatNumber())
                .idNumber(dto.getIdNumber())
                .routingId(dto.getRoutingId())
                .publicNotes(dto.getPublicNotes())
                .privateNotes(dto.getPrivateNotes())
                .customValue1(dto.getCustomValue1())
                .customValue2(dto.getCustomValue2())
                .customValue3(dto.getCustomValue3())
                .customValue4(dto.getCustomValue4())
                .isTaxExempt(dto.isTaxExempt())
                .isDeleted(false)
                .contacts(new ArrayList<>())
                .build();

        if (dto.getContacts() != null && !dto.getContacts().isEmpty()) {
            boolean hasPrimary = false;
            for (VendorContactDto contactDto : dto.getContacts()) {
                if (StringUtils.hasText(contactDto.getEmail())) {
                    boolean isPrimary = contactDto.isPrimary();
                    if (isPrimary) {
                        hasPrimary = true;
                    }
                    VendorContactEntity contactEntity = VendorContactEntity.builder()
                            .firstName(contactDto.getFirstName())
                            .lastName(contactDto.getLastName())
                            .email(contactDto.getEmail().trim())
                            .phone(contactDto.getPhone())
                            .isPrimary(isPrimary)
                            .sendEmail(contactDto.isSendEmail())
                            .customValue1(contactDto.getCustomValue1())
                            .customValue2(contactDto.getCustomValue2())
                            .customValue3(contactDto.getCustomValue3())
                            .customValue4(contactDto.getCustomValue4())
                            .build();
                    entity.addContact(contactEntity);
                }
            }
            if (!hasPrimary && !entity.getContacts().isEmpty()) {
                entity.getContacts().get(0).setPrimary(true);
            }
        }

        VendorEntity saved = vendorRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public VendorDto updateVendor(UUID id, VendorDto dto) {
        UUID companyId = getRequiredCompanyId();
        VendorEntity entity = vendorRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + id));

        if (StringUtils.hasText(dto.getName())) {
            entity.setName(dto.getName().trim());
        }
        if (StringUtils.hasText(dto.getNumber())) {
            entity.setVendorNumber(dto.getNumber().trim());
        }
        if (dto.getAssignedUserId() != null) {
            entity.setAssignedUserId(dto.getAssignedUserId());
        }
        entity.setWebsite(dto.getWebsite());
        entity.setPhone(dto.getPhone());
        if (dto.getCurrencyId() != null) {
            entity.setCurrencyId(dto.getCurrencyId());
        }
        entity.setAddress1(dto.getAddress1());
        entity.setAddress2(dto.getAddress2());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setPostalCode(dto.getPostalCode());
        entity.setCountryId(dto.getCountryId());
        entity.setVatNumber(dto.getVatNumber());
        entity.setIdNumber(dto.getIdNumber());
        entity.setRoutingId(dto.getRoutingId());
        entity.setPublicNotes(dto.getPublicNotes());
        entity.setPrivateNotes(dto.getPrivateNotes());
        entity.setCustomValue1(dto.getCustomValue1());
        entity.setCustomValue2(dto.getCustomValue2());
        entity.setCustomValue3(dto.getCustomValue3());
        entity.setCustomValue4(dto.getCustomValue4());
        entity.setTaxExempt(dto.isTaxExempt());

        if (dto.getContacts() != null) {
            entity.getContacts().clear();
            boolean hasPrimary = false;
            for (VendorContactDto contactDto : dto.getContacts()) {
                if (StringUtils.hasText(contactDto.getEmail())) {
                    boolean isPrimary = contactDto.isPrimary();
                    if (isPrimary) {
                        hasPrimary = true;
                    }
                    VendorContactEntity contactEntity = VendorContactEntity.builder()
                            .firstName(contactDto.getFirstName())
                            .lastName(contactDto.getLastName())
                            .email(contactDto.getEmail().trim())
                            .phone(contactDto.getPhone())
                            .isPrimary(isPrimary)
                            .sendEmail(contactDto.isSendEmail())
                            .customValue1(contactDto.getCustomValue1())
                            .customValue2(contactDto.getCustomValue2())
                            .customValue3(contactDto.getCustomValue3())
                            .customValue4(contactDto.getCustomValue4())
                            .build();
                    entity.addContact(contactEntity);
                }
            }
            if (!hasPrimary && !entity.getContacts().isEmpty()) {
                entity.getContacts().get(0).setPrimary(true);
            }
        }

        VendorEntity updated = vendorRepository.save(entity);
        return toDto(updated);
    }

    @Transactional
    public List<VendorDto> bulkAction(VendorBulkActionRequest request) {
        UUID companyId = getRequiredCompanyId();
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return Collections.emptyList();
        }

        List<VendorEntity> entities = vendorRepository.findByCompanyIdAndIdIn(companyId, request.getIds());
        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";

        List<VendorEntity> resultEntities;

        switch (action) {
            case "archive":
                Instant now = Instant.now();
                for (VendorEntity vendor : entities) {
                    vendor.setArchivedAt(now);
                    vendor.setDeleted(false);
                }
                resultEntities = vendorRepository.saveAll(entities);
                break;

            case "restore":
                for (VendorEntity vendor : entities) {
                    vendor.setArchivedAt(null);
                    vendor.setDeleted(false);
                }
                resultEntities = vendorRepository.saveAll(entities);
                break;

            case "delete":
                Instant delTime = Instant.now();
                for (VendorEntity vendor : entities) {
                    vendor.setDeleted(true);
                    vendor.setArchivedAt(delTime);
                }
                resultEntities = vendorRepository.saveAll(entities);
                break;

            default:
                resultEntities = entities;
                break;
        }

        return resultEntities.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public VendorDto purgeVendor(UUID id) {
        UUID companyId = getRequiredCompanyId();
        VendorEntity entity = vendorRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + id));

        vendorRepository.delete(entity);
        return toDto(entity);
    }

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

    private VendorContactDto toContactDto(VendorContactEntity contact) {
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

    private UUID getRequiredCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalArgumentException("Tenant company context is missing");
        }
        return companyId;
    }
}
