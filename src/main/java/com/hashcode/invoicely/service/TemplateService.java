package com.hashcode.invoicely.service;

import com.hashcode.invoicely.dto.document.TemplateRequest;
import com.hashcode.invoicely.dto.document.TemplateResponse;

public interface TemplateService {

    TemplateResponse renderTemplate(TemplateRequest request);

    byte[] renderPreview(String hash);
}
