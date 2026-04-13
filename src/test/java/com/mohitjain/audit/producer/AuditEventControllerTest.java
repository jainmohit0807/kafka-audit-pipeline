package com.mohitjain.audit.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohitjain.audit.consumer.repository.AuditRecordRepository;
import com.mohitjain.audit.producer.controller.AuditEventController;
import com.mohitjain.audit.producer.dto.AuditEventRequest;
import com.mohitjain.audit.producer.dto.AuditEventResponse;
import com.mohitjain.audit.producer.service.AuditEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditEventController.class)
class AuditEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuditEventPublisher publisher;

    @MockBean
    private AuditRecordRepository repository;

    @Test
    void publishEvent_withValidRequest_returns202() throws Exception {
        AuditEventRequest request = AuditEventRequest.builder()
                .source("user-service")
                .action("LOGIN")
                .actorId("user-123")
                .resourceType("session")
                .build();

        AuditEventResponse response = AuditEventResponse.builder()
                .eventId("evt-001")
                .topic("audit-events")
                .partition(0)
                .offset(42L)
                .publishedAt(Instant.now())
                .status("ACCEPTED")
                .build();

        when(publisher.publish(any(AuditEventRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/audit-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value("evt-001"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void publishEvent_withMissingSource_returns400() throws Exception {
        AuditEventRequest request = AuditEventRequest.builder()
                .action("LOGIN")
                .actorId("user-123")
                .resourceType("session")
                .build();

        mockMvc.perform(post("/v1/audit-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listEvents_returnsPage() throws Exception {
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/v1/audit-events"))
                .andExpect(status().isOk());
    }

    @Test
    void countEvents_returnsCount() throws Exception {
        when(repository.count()).thenReturn(42L);

        mockMvc.perform(get("/v1/audit-events/count"))
                .andExpect(status().isOk());
    }
}
