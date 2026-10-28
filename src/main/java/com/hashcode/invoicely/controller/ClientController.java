package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.dto.client.ClientBulkActionRequest;
import com.hashcode.invoicely.dto.client.ClientDto;
import com.hashcode.invoicely.dto.common.ApiResponse;
import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<GenericManyResponse<ClientDto>> getClients(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "per_page", defaultValue = "20") int perPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort) {
        return ResponseEntity.ok(clientService.getClients(page, perPage, status, filter, sort));
    }

    @GetMapping("/create")
    public ResponseEntity<ApiResponse<ClientDto>> getBlankClient() {
        return ResponseEntity.ok(ApiResponse.of(clientService.getBlankClient()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientDto>> getClientById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(clientService.getClientById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClientDto>> createClient(@RequestBody ClientDto clientDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(clientService.createClient(clientDto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientDto>> updateClient(
            @PathVariable("id") UUID id,
            @RequestBody ClientDto clientDto) {
        return ResponseEntity.ok(ApiResponse.of(clientService.updateClient(id, clientDto)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<ClientDto>>> bulkAction(@RequestBody ClientBulkActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(clientService.bulkAction(request)));
    }

    @PostMapping("/{targetId}/{sourceId}/merge")
    public ResponseEntity<ApiResponse<ClientDto>> mergeClients(
            @PathVariable("targetId") UUID targetId,
            @PathVariable("sourceId") UUID sourceId) {
        return ResponseEntity.ok(ApiResponse.of(clientService.mergeClients(targetId, sourceId)));
    }

    @PostMapping("/{id}/purge")
    public ResponseEntity<ApiResponse<ClientDto>> purgeClient(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.of(clientService.purgeClient(id)));
    }
}
