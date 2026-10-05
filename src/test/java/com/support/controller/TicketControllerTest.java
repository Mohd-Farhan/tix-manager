package com.support.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.config.GlobalExceptionHandler;
import com.support.dto.CreateMessageRequest;
import com.support.dto.CreateTicketRequest;
import com.support.dto.MessageResponse;
import com.support.dto.TicketResponse;
import com.support.entity.TicketPriority;
import com.support.entity.TicketStatus;
import com.support.service.TicketService;
import com.support.strategy.routing.RoutingStrategyType;
import com.support.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ==============================================================================================
 * AUTOMATED TESTING SUITE: Controller Slice Test for TicketController
 * ==============================================================================================
 */
@WebMvcTest(controllers = TicketController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private com.support.security.JwtService jwtService;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockBean
    private com.support.service.SlaService slaService;

    @MockBean
    private com.support.security.TicketSecurity ticketSecurity;

    /**
     * TEST CASE 1: Create ticket with valid payload returns 201 Created.
     */
    @Test
    @DisplayName("POST /api/tickets — Create ticket and return 201 CREATED")
    void testCreateTicket_Success() throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Billing invoice error")
                .description("Incorrect charge on March invoice.")
                .priority(TicketPriority.MEDIUM)
                .build();

        TicketResponse createdResponse = TicketResponse.builder()
                .id(1L)
                .title("Billing invoice error")
                .description("Incorrect charge on March invoice.")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.MEDIUM)
                .customerId(5L)
                .build();

        when(securityUtils.resolveUserId(any())).thenReturn(5L);
        when(ticketService.createTicket(any(CreateTicketRequest.class), eq(5L))).thenReturn(createdResponse);

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Billing invoice error"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    /**
     * TEST CASE 2: Get all tickets returns 200 OK and list of tickets.
     */
    @Test
    @DisplayName("GET /api/tickets — Return active tickets list")
    void testGetAllTickets_Success() throws Exception {
        TicketResponse t1 = TicketResponse.builder().id(1L).title("Issue 1").status(TicketStatus.OPEN).build();
        TicketResponse t2 = TicketResponse.builder().id(2L).title("Issue 2").status(TicketStatus.IN_PROGRESS).build();

        when(ticketService.getAllActiveTickets()).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    /**
     * TEST CASE 3: Assign ticket to agent returns 200 OK.
     */
    @Test
    @DisplayName("PUT /api/tickets/{id}/assign — Assign ticket to agent")
    void testAssignTicket_Success() throws Exception {
        TicketResponse assignedResponse = TicketResponse.builder()
                .id(10L)
                .status(TicketStatus.IN_PROGRESS)
                .assignedAgentId(3L)
                .build();

        when(ticketService.assignTicket(10L, 3L)).thenReturn(assignedResponse);

        mockMvc.perform(put("/api/tickets/10/assign?agentId=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.assignedAgentId").value(3))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    /**
     * TEST CASE: Auto-assign ticket using Strategy Pattern returns 200 OK.
     */
    @Test
    @DisplayName("PUT /api/tickets/{id}/auto-assign — Auto-assign ticket to optimal agent via strategy")
    void testAutoAssignTicket_Success() throws Exception {
        TicketResponse autoAssignedResponse = TicketResponse.builder()
                .id(10L)
                .status(TicketStatus.IN_PROGRESS)
                .assignedAgentId(5L)
                .build();

        when(ticketService.autoAssignTicket(10L, RoutingStrategyType.WORKLOAD_BALANCED))
                .thenReturn(autoAssignedResponse);

        mockMvc.perform(put("/api/tickets/10/auto-assign?strategy=WORKLOAD_BALANCED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.assignedAgentId").value(5))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    /**
     * TEST CASE 4: Post message to ticket thread returns 201 Created.
     */
    @Test
    @DisplayName("POST /api/tickets/{id}/messages — Post message to conversation thread")
    void testAddMessage_Success() throws Exception {
        CreateMessageRequest request = CreateMessageRequest.builder().content("Checking on the status").build();
        MessageResponse savedResponse = MessageResponse.builder().id(99L).ticketId(10L).content("Checking on the status").build();
        TicketResponse existingTicket = TicketResponse.builder().id(10L).customerId(5L).build();

        when(securityUtils.resolveUserId(any())).thenReturn(5L);
        when(ticketService.getTicketById(10L)).thenReturn(existingTicket);
        when(ticketService.addMessage(eq(10L), eq(5L), eq("Checking on the status"))).thenReturn(savedResponse);

        mockMvc.perform(post("/api/tickets/10/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.content").value("Checking on the status"));
    }

    /**
     * TEST CASE 5: Concurrent update throws ObjectOptimisticLockingFailureException -> 409 Conflict.
     */
    @Test
    @DisplayName("PUT /api/tickets/{id}/assign — Concurrent modification returns 409 CONFLICT")
    void testAssignTicket_ConcurrentModification_Returns409() throws Exception {
        when(ticketService.assignTicket(10L, 3L))
                .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(com.support.entity.Ticket.class, 10L));

        mockMvc.perform(put("/api/tickets/10/assign?agentId=3"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Concurrent Modification Conflict"))
                .andExpect(jsonPath("$.message").value("This ticket was updated by another user or agent in the background. Please refresh and try again."));
    }

    /**
     * TEST CASE 6: GET /api/tickets/sla-metrics returns 200 OK and SLA statistics.
     */
    @Test
    @DisplayName("GET /api/tickets/sla-metrics — Returns 200 OK with SLA metrics")
    void testGetSlaMetrics_Success() throws Exception {
        com.support.dto.SlaMetricsDTO metrics = com.support.dto.SlaMetricsDTO.builder()
                .totalActive(15)
                .withinSla(10)
                .nearBreach(3)
                .breached(2)
                .totalResolved(100)
                .resolvedWithinSla(94)
                .complianceRatePercent(94.0)
                .build();

        when(slaService.getSlaMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/api/tickets/sla-metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActive").value(15))
                .andExpect(jsonPath("$.withinSla").value(10))
                .andExpect(jsonPath("$.nearBreach").value(3))
                .andExpect(jsonPath("$.breached").value(2))
                .andExpect(jsonPath("$.complianceRatePercent").value(94.0));
    }

    /**
     * TEST CASE 7: PATCH /api/tickets/{id}/priority adjusts priority and returns 200 OK.
     */
    @Test
    @DisplayName("PATCH /api/tickets/{id}/priority — Adjusts priority level")
    void testUpdatePriority_Success() throws Exception {
        com.support.dto.PriorityUpdateRequest request = com.support.dto.PriorityUpdateRequest.builder()
                .priority(TicketPriority.HIGH)
                .build();

        TicketResponse updated = TicketResponse.builder()
                .id(10L)
                .title("Sample ticket")
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.OPEN)
                .slaStatus("WARNING")
                .remainingSeconds(3600L)
                .build();

        when(securityUtils.resolveUserId(any())).thenReturn(2L);
        when(ticketService.updatePriority(eq(10L), eq(TicketPriority.HIGH), eq(2L))).thenReturn(updated);

        mockMvc.perform(patch("/api/tickets/10/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.slaStatus").value("WARNING"));
    }
}
