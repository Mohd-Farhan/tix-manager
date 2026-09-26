package com.support.service;

import com.support.dto.SlaMetricsDTO;
import com.support.entity.Ticket;
import com.support.entity.TicketPriority;
import com.support.entity.TicketStatus;
import com.support.entity.User;
import com.support.entity.UserRole;
import com.support.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ==============================================================================================
 * UNIT TEST SUITE: SlaServiceTest
 * ==============================================================================================
 * 
 * Validates:
 * 1. Priority-based SLA resolution deadline calculation (HIGH=4h, MEDIUM=24h, LOW=72h).
 * 2. Background escalation rules (LOW -> MEDIUM, MEDIUM -> HIGH, HIGH -> flagged escalated).
 * 3. Audit trail generation and email notification dispatch upon breach.
 * 4. Aggregated operational SLA KPI metrics compilation.
 */
@ExtendWith(MockitoExtension.class)
class SlaServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private EmailService emailService;

    private SlaService slaService;

    @BeforeEach
    void setUp() {
        // High: 4h, Medium: 24h, Low: 72h, Warning: 2h, Escalation: true
        slaService = new SlaService(ticketRepository, auditService, emailService, 4L, 24L, 72L, 2L, true);
    }

    @Test
    @DisplayName("calculateSlaDueAt — Correctly applies priority SLA targets")
    void testCalculateSlaDueAt() {
        LocalDateTime baseline = LocalDateTime.of(2026, 9, 20, 10, 0);

        LocalDateTime highDue = slaService.calculateSlaDueAt(TicketPriority.HIGH, baseline);
        assertThat(highDue).isEqualTo(baseline.plusHours(4));

        LocalDateTime mediumDue = slaService.calculateSlaDueAt(TicketPriority.MEDIUM, baseline);
        assertThat(mediumDue).isEqualTo(baseline.plusHours(24));

        LocalDateTime lowDue = slaService.calculateSlaDueAt(TicketPriority.LOW, baseline);
        assertThat(lowDue).isEqualTo(baseline.plusHours(72));
    }

    @Test
    @DisplayName("checkAndEscalateBreachedTickets — Escalates LOW to MEDIUM and notifies assigned agent")
    void testCheckAndEscalate_LowToMedium() {
        User agent = new User();
        agent.setId(5L);
        agent.setUsername("priya_agent");
        agent.setEmail("priya@example.com");
        agent.setRole(UserRole.SUPPORT_AGENT);

        Ticket ticket = new Ticket();
        ticket.setId(101L);
        ticket.setTitle("Sample low ticket");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(TicketPriority.LOW);
        ticket.setSlaDueAt(LocalDateTime.now().minusMinutes(15)); // Breached 15m ago
        ticket.setSlaBreached(false);
        ticket.setAssignedAgent(agent);

        when(ticketRepository.findByStatusInAndSlaDueAtBeforeAndSlaBreachedFalse(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket)));

        int processed = slaService.checkAndEscalateBreachedTickets();

        assertThat(processed).isEqualTo(1);
        assertThat(ticket.isSlaBreached()).isTrue();
        assertThat(ticket.getPriority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(ticket.isEscalated()).isTrue();

        verify(ticketRepository).save(ticket);
        verify(auditService).recordEntityChange(eq("TICKET"), eq(101L), eq("SLA_BREACH"), eq("SYSTEM"), any());
        verify(emailService).sendSlaBreachEmail(eq("priya@example.com"), eq(101L), any(), eq("MEDIUM"), any(), eq(true));
    }

    @Test
    @DisplayName("checkAndEscalateBreachedTickets — Escalates MEDIUM to HIGH")
    void testCheckAndEscalate_MediumToHigh() {
        Ticket ticket = new Ticket();
        ticket.setId(102L);
        ticket.setTitle("Moderate issue");
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setSlaDueAt(LocalDateTime.now().minusHours(1));
        ticket.setSlaBreached(false);

        when(ticketRepository.findByStatusInAndSlaDueAtBeforeAndSlaBreachedFalse(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket)));

        int processed = slaService.checkAndEscalateBreachedTickets();

        assertThat(processed).isEqualTo(1);
        assertThat(ticket.isSlaBreached()).isTrue();
        assertThat(ticket.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(ticket.isEscalated()).isTrue();
        verify(ticketRepository).save(ticket);
    }

    @Test
    @DisplayName("checkAndEscalateBreachedTickets — HIGH priority remains HIGH but flagged as escalated")
    void testCheckAndEscalate_HighPriority() {
        Ticket ticket = new Ticket();
        ticket.setId(103L);
        ticket.setTitle("Critical outage");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(TicketPriority.HIGH);
        ticket.setSlaDueAt(LocalDateTime.now().minusHours(2));
        ticket.setSlaBreached(false);

        when(ticketRepository.findByStatusInAndSlaDueAtBeforeAndSlaBreachedFalse(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket)));

        int processed = slaService.checkAndEscalateBreachedTickets();

        assertThat(processed).isEqualTo(1);
        assertThat(ticket.isSlaBreached()).isTrue();
        assertThat(ticket.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(ticket.isEscalated()).isTrue();
        verify(ticketRepository).save(ticket);
    }

    @Test
    @DisplayName("getSlaMetrics — Compiles accurate live operational metrics")
    void testGetSlaMetrics() {
        when(ticketRepository.countByStatusIn(any())).thenReturn(20L);
        when(ticketRepository.countByStatusInAndSlaBreachedTrue(any())).thenReturn(3L);
        when(ticketRepository.countByStatusInAndSlaBreachedFalseAndSlaDueAtBetween(any(), any(), any())).thenReturn(5L);
        when(ticketRepository.countTotalResolved()).thenReturn(50L);
        when(ticketRepository.countResolvedWithinSla()).thenReturn(45L);

        SlaMetricsDTO metrics = slaService.getSlaMetrics();

        assertThat(metrics.getTotalActive()).isEqualTo(20L);
        assertThat(metrics.getBreached()).isEqualTo(3L);
        assertThat(metrics.getNearBreach()).isEqualTo(5L);
        assertThat(metrics.getWithinSla()).isEqualTo(12L); // 20 - 3 - 5 = 12
        assertThat(metrics.getTotalResolved()).isEqualTo(50L);
        assertThat(metrics.getResolvedWithinSla()).isEqualTo(45L);
        assertThat(metrics.getComplianceRatePercent()).isEqualTo(90.0);
    }
}
