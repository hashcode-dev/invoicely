package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.common.GenericManyResponse;
import com.hashcode.invoicely.dto.vendor.VendorBulkActionRequest;
import com.hashcode.invoicely.dto.vendor.VendorDto;
import com.hashcode.invoicely.entity.relational.VendorEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface VendorService {

    GenericManyResponse<VendorDto> getVendors(int page, int perPage, String status, String filter, String sort);

    VendorDto getBlankVendor();

    VendorDto getVendorById(UUID id);

    VendorDto createVendor(VendorDto dto);

    VendorDto updateVendor(UUID id, VendorDto dto);

    VendorDto uploadDocument(UUID id, MultipartFile file);

    List<VendorDto> bulkAction(VendorBulkActionRequest request);

    VendorDto purgeVendor(UUID id);

    VendorDto restoreVendor(UUID id);

    VendorDto deleteVendor(UUID id);

    VendorDto toDto(VendorEntity entity);
}
