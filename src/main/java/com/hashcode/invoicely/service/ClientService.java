package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.client.ClientBulkActionRequest;
import com.hashcode.invoicely.dto.client.ClientDto;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.entity.relational.ClientEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ClientService {

    GenericManyResponse<ClientDto> getClients(int page, int perPage, String status, String filter, String sort);

    ClientDto getBlankClient();

    ClientDto getClientById(UUID id);

    ClientDto createClient(ClientDto dto);

    ClientDto updateClient(UUID id, ClientDto dto);

    ClientDto uploadDocument(UUID id, MultipartFile file);

    List<ClientDto> bulkAction(ClientBulkActionRequest request);

    ClientDto purgeClient(UUID id);

    ClientDto restoreClient(UUID id);

    ClientDto deleteClient(UUID id);

    ClientDto mergeClients(UUID targetId, UUID sourceId);

    ClientDto toDto(ClientEntity entity);
}
