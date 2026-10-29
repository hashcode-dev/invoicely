package com.hashcode.invoicely.controller;

import com.hashcode.invoicely.service.StaticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StaticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaticsService staticsService;

    @Test
    @DisplayName("GET /api/v1/statics should return full reference metadata dictionaries")
    void shouldReturnStatics() throws Exception {
        mockMvc.perform(get("/api/v1/statics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencies").isArray())
                .andExpect(jsonPath("$.currencies[0].code", is("USD")))
                .andExpect(jsonPath("$.countries").isArray())
                .andExpect(jsonPath("$.date_formats").isArray())
                .andExpect(jsonPath("$.datetime_formats").isArray())
                .andExpect(jsonPath("$.industries").isArray())
                .andExpect(jsonPath("$.sizes").isArray())
                .andExpect(jsonPath("$.payment_types").isArray())
                .andExpect(jsonPath("$.timezones").isArray())
                .andExpect(jsonPath("$.license_key", is("INVOICELY-ENTERPRISE-PRO")));
    }
}
