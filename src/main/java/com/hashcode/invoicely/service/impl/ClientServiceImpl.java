package com.hashcode.invoicely.service.impl;

import com.hashcode.invoicely.context.TenantContext;
import com.hashcode.invoicely.dto.client.ClientBulkActionRequest;
import com.hashcode.invoicely.dto.client.ClientContactDto;
import com.hashcode.invoicely.dto.client.ClientDto;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.entity.relational.ClientContactEntity;
import com.hashcode.invoicely.entity.relational.ClientEntity;
import com.hashcode.invoicely.entity.relational.CompanyEntity;
import com.hashcode.invoicely.mapper.ClientMapper;
import com.hashcode.invoicely.repository.relational.ClientRepository;
import com.hashcode.invoicely.repository.relational.CompanyRepository;
import com.hashcode.invoicely.service.ClientService;
import com.hashcode.invoicely.service.SequenceGeneratorService;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final ClientMapper clientMapper;

    @Override
    @Transactional(readOnly = true)
    public GenericManyResponse<ClientDto> getClients(int page, int perPage, String status, String filter, String sort) {
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

        Specification<ClientEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("companyId"), companyId));

            if ("archived".equalsIgnoreCase(status)) {
                predicates.add(cb.isNotNull(root.get("archivedAt")));
                predicates.add(cb.isFalse(root.get("isDeleted")));
            } else if ("deleted".equalsIgnoreCase(status)) {
                predicates.add(cb.isTrue(root.get("isDeleted")));
            } else if (!"all".equalsIgnoreCase(status)) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
                predicates.add(cb.isNull(root.get("archivedAt")));
            }

            if (StringUtils.hasText(filter)) {
                String searchPattern = "%" + filter.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate numberMatch = cb.like(cb.lower(root.get("clientNumber")), searchPattern);
                Predicate phoneMatch = cb.like(cb.lower(root.get("phone")), searchPattern);
                predicates.add(cb.or(nameMatch, numberMatch, phoneMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ClientEntity> clientPage = clientRepository.findAll(spec, pageable);
        List<ClientDto> dtos = clientPage.getContent().stream()
                .map(clientMapper::toDto)
                .collect(Collectors.toList());

        return GenericManyResponse.of(dtos, clientPage.getTotalElements(), page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDto getBlankClient() {
        UUID companyId = getRequiredCompanyId();
        String nextNumber = sequenceGeneratorService.generateSequence("client_" + companyId);

        String currencyId = "USD";
        String countryId = "US";
        Optional<CompanyEntity> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isPresent()) {
            currencyId = companyOpt.get().getCurrencyId();
            countryId = companyOpt.get().getCountryId();
        }

        ClientContactDto defaultContact = ClientContactDto.builder()
                .isPrimary(true)
                .sendEmail(true)
                .build();

        return ClientDto.builder()
                .number(nextNumber)
                .currencyId(currencyId)
                .countryId(countryId)
                .balance(BigDecimal.ZERO)
                .paidToDate(BigDecimal.ZERO)
                .creditBalance(BigDecimal.ZERO)
                .contacts(Collections.singletonList(defaultContact))
                .settings(Collections.emptyMap())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDto getClientById(UUID id) {
        UUID companyId = getRequiredCompanyId();
        ClientEntity entity = clientRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found with id: " + id));
        return clientMapper.toDto(entity);
    }

    @Override
    @Transactional
    public ClientDto createClient(ClientDto dto) {
        UUID companyId = getRequiredCompanyId();
        UUID userId = TenantContext.getUserId();

        String clientNumber = StringUtils.hasText(dto.getNumber())
                ? dto.getNumber().trim()
                : sequenceGeneratorService.generateSequence("client_" + companyId);

        String name = StringUtils.hasText(dto.getName()) ? dto.getName().trim() : "Unnamed Client";

        ClientEntity entity = ClientEntity.builder()
                .companyId(companyId)
                .userId(userId)
                .assignedUserId(dto.getAssignedUserId())
                .name(name)
                .clientNumber(clientNumber)
                .website(dto.getWebsite())
                .phone(dto.getPhone())
                .balance(BigDecimal.ZERO)
                .paidToDate(BigDecimal.ZERO)
                .creditBalance(BigDecimal.ZERO)
                .currencyId(StringUtils.hasText(dto.getCurrencyId()) ? dto.getCurrencyId() : "USD")
                .address1(dto.getAddress1())
                .address2(dto.getAddress2())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .countryId(dto.getCountryId())
                .shippingAddress1(dto.getShippingAddress1())
                .shippingAddress2(dto.getShippingAddress2())
                .shippingCity(dto.getShippingCity())
                .shippingState(dto.getShippingState())
                .shippingPostalCode(dto.getShippingPostalCode())
                .shippingCountryId(dto.getShippingCountryId())
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
            for (ClientContactDto contactDto : dto.getContacts()) {
                if (StringUtils.hasText(contactDto.getEmail())) {
                    boolean isPrimary = contactDto.isPrimary();
                    if (isPrimary) {
                        hasPrimary = true;
                    }
                    ClientContactEntity contactEntity = ClientContactEntity.builder()
                            .firstName(contactDto.getFirstName())
                            .lastName(contactDto.getLastName())
                            .email(contactDto.getEmail().trim())
                            .phone(contactDto.getPhone())
                            .isPrimary(isPrimary)
                            .sendEmail(contactDto.isSendEmail())
                            .contactKey(contactDto.getContactKey())
                            .customValue1(contactDto.getCustomValue1())
                            .customValue2(contactDto.getCustomValue2())
                            .customValue3(contactDto.getCustomValue3())
                            .customValue4(contactDto.getCustomValue4())
                            .isLocked(contactDto.isLocked())
                            .build();
                    entity.addContact(contactEntity);
                }
            }
            if (!hasPrimary && !entity.getContacts().isEmpty()) {
                entity.getContacts().get(0).setPrimary(true);
            }
        }

        ClientEntity saved = clientRepository.save(entity);
        return clientMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ClientDto updateClient(UUID id, ClientDto dto) {
        UUID companyId = getRequiredCompanyId();
        ClientEntity entity = clientRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found with id: " + id));

        if (StringUtils.hasText(dto.getName())) {
            entity.setName(dto.getName().trim());
        }
        if (StringUtils.hasText(dto.getNumber())) {
            entity.setClientNumber(dto.getNumber().trim());
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
        entity.setShippingAddress1(dto.getShippingAddress1());
        entity.setShippingAddress2(dto.getShippingAddress2());
        entity.setShippingCity(dto.getShippingCity());
        entity.setShippingState(dto.getShippingState());
        entity.setShippingPostalCode(dto.getShippingPostalCode());
        entity.setShippingCountryId(dto.getShippingCountryId());
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
            for (ClientContactDto contactDto : dto.getContacts()) {
                if (StringUtils.hasText(contactDto.getEmail())) {
                    boolean isPrimary = contactDto.isPrimary();
                    if (isPrimary) {
                        hasPrimary = true;
                    }
                    ClientContactEntity contactEntity = ClientContactEntity.builder()
                            .firstName(contactDto.getFirstName())
                            .lastName(contactDto.getLastName())
                            .email(contactDto.getEmail().trim())
                            .phone(contactDto.getPhone())
                            .isPrimary(isPrimary)
                            .sendEmail(contactDto.isSendEmail())
                            .contactKey(contactDto.getContactKey())
                            .customValue1(contactDto.getCustomValue1())
                            .customValue2(contactDto.getCustomValue2())
                            .customValue3(contactDto.getCustomValue3())
                            .customValue4(contactDto.getCustomValue4())
                            .isLocked(contactDto.isLocked())
                            .build();
                    entity.addContact(contactEntity);
                }
            }
            if (!hasPrimary && !entity.getContacts().isEmpty()) {
                entity.getContacts().get(0).setPrimary(true);
            }
        }

        ClientEntity updated = clientRepository.save(entity);
        return clientMapper.toDto(updated);
    }

    @Override
    @Transactional
    public ClientDto uploadDocument(UUID id, MultipartFile file) {
        UUID companyId = getRequiredCompanyId();
        ClientEntity entity = clientRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found with id: " + id));

        log.info("Uploaded document '{}' (size: {} bytes) for client {}",
                file.getOriginalFilename(), file.getSize(), id);

        return clientMapper.toDto(entity);
    }

    @Override
    @Transactional
    public List<ClientDto> bulkAction(ClientBulkActionRequest request) {
        UUID companyId = getRequiredCompanyId();
        List<UUID> ids = request.getIds();
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<ClientEntity> entities = clientRepository.findByCompanyIdAndIdIn(companyId, ids);
        String action = request.getAction() != null ? request.getAction().toLowerCase() : "";

        List<ClientEntity> resultEntities = new ArrayList<>();

        switch (action) {
            case "archive":
                Instant now = Instant.now();
                for (ClientEntity client : entities) {
                    client.setArchivedAt(now);
                    client.setDeleted(false);
                }
                resultEntities = clientRepository.saveAll(entities);
                break;

            case "restore":
                for (ClientEntity client : entities) {
                    client.setArchivedAt(null);
                    client.setDeleted(false);
                }
                resultEntities = clientRepository.saveAll(entities);
                break;

            case "delete":
                Instant delTime = Instant.now();
                for (ClientEntity client : entities) {
                    client.setDeleted(true);
                    client.setArchivedAt(delTime);
                }
                resultEntities = clientRepository.saveAll(entities);
                break;

            case "clone":
                for (ClientEntity original : entities) {
                    String nextNumber = sequenceGeneratorService.generateSequence("client_" + companyId);
                    ClientEntity clone = ClientEntity.builder()
                            .companyId(companyId)
                            .userId(original.getUserId())
                            .assignedUserId(original.getAssignedUserId())
                            .clientNumber(nextNumber)
                            .name(original.getName() + " (Copy)")
                            .website(original.getWebsite())
                            .phone(original.getPhone())
                            .balance(BigDecimal.ZERO)
                            .paidToDate(BigDecimal.ZERO)
                            .creditBalance(BigDecimal.ZERO)
                            .currencyId(original.getCurrencyId())
                            .address1(original.getAddress1())
                            .address2(original.getAddress2())
                            .city(original.getCity())
                            .state(original.getState())
                            .postalCode(original.getPostalCode())
                            .countryId(original.getCountryId())
                            .shippingAddress1(original.getShippingAddress1())
                            .shippingAddress2(original.getShippingAddress2())
                            .shippingCity(original.getShippingCity())
                            .shippingState(original.getShippingState())
                            .shippingPostalCode(original.getShippingPostalCode())
                            .shippingCountryId(original.getShippingCountryId())
                            .vatNumber(original.getVatNumber())
                            .idNumber(original.getIdNumber())
                            .routingId(original.getRoutingId())
                            .publicNotes(original.getPublicNotes())
                            .privateNotes(original.getPrivateNotes())
                            .customValue1(original.getCustomValue1())
                            .customValue2(original.getCustomValue2())
                            .customValue3(original.getCustomValue3())
                            .customValue4(original.getCustomValue4())
                            .isTaxExempt(original.isTaxExempt())
                            .isDeleted(false)
                            .contacts(new ArrayList<>())
                            .build();

                    for (ClientContactEntity c : original.getContacts()) {
                        ClientContactEntity contactClone = ClientContactEntity.builder()
                                .firstName(c.getFirstName())
                                .lastName(c.getLastName())
                                .email(c.getEmail())
                                .phone(c.getPhone())
                                .isPrimary(c.isPrimary())
                                .sendEmail(c.isSendEmail())
                                .build();
                        clone.addContact(contactClone);
                    }
                    resultEntities.add(clientRepository.save(clone));
                }
                break;

            default:
                resultEntities = entities;
                break;
        }

        return resultEntities.stream().map(clientMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClientDto mergeClients(UUID targetId, UUID sourceId) {
        UUID companyId = getRequiredCompanyId();
        ClientEntity target = clientRepository.findByCompanyIdAndId(companyId, targetId)
                .orElseThrow(() -> new IllegalArgumentException("Target client not found: " + targetId));
        ClientEntity source = clientRepository.findByCompanyIdAndId(companyId, sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source client not found: " + sourceId));

        target.setBalance(target.getBalance().add(source.getBalance()));
        target.setPaidToDate(target.getPaidToDate().add(source.getPaidToDate()));
        target.setCreditBalance(target.getCreditBalance().add(source.getCreditBalance()));

        source.setDeleted(true);
        source.setArchivedAt(Instant.now());

        clientRepository.save(source);
        ClientEntity savedTarget = clientRepository.save(target);

        return clientMapper.toDto(savedTarget);
    }

    @Override
    @Transactional
    public ClientDto purgeClient(UUID id) {
        UUID companyId = getRequiredCompanyId();
        ClientEntity entity = clientRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found with id: " + id));

        clientRepository.delete(entity);
        return clientMapper.toDto(entity);
    }

    @Override
    @Transactional
    public ClientDto restoreClient(UUID id) {
        UUID companyId = getRequiredCompanyId();
        ClientEntity entity = clientRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found with id: " + id));

        entity.setArchivedAt(null);
        entity.setDeleted(false);
        ClientEntity saved = clientRepository.save(entity);
        return clientMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ClientDto deleteClient(UUID id) {
        UUID companyId = getRequiredCompanyId();
        ClientEntity entity = clientRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found with id: " + id));

        clientRepository.delete(entity);
        return clientMapper.toDto(entity);
    }

    @Override
    public ClientDto toDto(ClientEntity entity) {
        return clientMapper.toDto(entity);
    }

    private UUID getRequiredCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalArgumentException("Tenant company context is missing");
        }
        return companyId;
    }
}
