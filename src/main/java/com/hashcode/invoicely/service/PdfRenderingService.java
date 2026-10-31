package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.document.DesignDto;

import java.util.Map;

public interface PdfRenderingService {

    byte[] generatePdf(String title, String body, Map<String, Object> meta);

    byte[] renderDesign(DesignDto design, Map<String, Object> sampleData);
}
