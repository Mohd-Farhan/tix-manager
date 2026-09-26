package com.support.service;

import com.support.dto.SlaMetricsDTO;
import com.support.entity.Ticket;
import com.support.entity.TicketPriority;
import com.support.entity.TicketStatus;
import com.support.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ==============================================================================================
 * SERVICE: SlaService (Service Level Agreement Management & Automated Escalation Engine)
 * ==============================================================================================
 * 
 * WHY THIS SERVICE ARCHITECTURE (ITIL v4 & Enterprise Operations Standard):
 * 1. Priority-Based Due Date Computation:
 *    - Automatically calculates resolution deadlines based on ticket priority:
 *      HIGH (4h default), MEDIUM (24h default), LOW (72h default).
 * 
 * 2. Automated Scheduled Priority Escalation:
 *    - Scans active tickets past their SLA resolution deadline using chunked pagination (50 rows/page).
 *    - Dynamically escalates priority (LOW -> MEDIUM, MEDIUM -> HIGH).
 *    - Records an immutable audit log entry by SYSTEM.
 *    - Sends asynchronous email alerts to assigned agents and operational monitors.
 * 
 * 3. Real-Time Operational KPI Metrics:
 *    - Aggregates active ticket SLA statuses (within SLA, approaching breach, breached)
 *      and calculates historical SLA compliance rate percentages.
 */
@Slf4j
@Service
public class SlaService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TicketRepository ticketRepository;
    private final AuditService auditService;
    private final EmailService emailService;

    private final long highHours;
    private final long mediumHours;
    private final long lowHours;
    private final long warningHours;
    private final boolean escalationEnabled;

    public SlaService(
            TicketRepository ticketRepository,
            AuditService auditService,
            EmailService emailService,
            @Value("${app.sla.high-hours:4}") long highHours,
            @Value("${app.sla.medium-hours:24}") long mediumHours,
            @Value("${app.sla.low-hours:72}") long lowHours,
            @Value("${app.sla.warning-hours:2}") long warningHours,
            @Value("${app.sla.escalation-enabled:true}") boolean escalationEnabled) {
        this.ticketRepository = ticketRepository;
        this.auditService = auditService;
        this.emailService = emailService;
        this.highHours = highHours;
        this.mediumHours = mediumHours;
        this.lowHours = lowHours;
        this.warningHours = warningHours;
        this.escalationEnabled = escalationEnabled;
    }

    /**
     * Calculates the SLA target due date given a ticket priority and baseline start timestamp.
     */
    public LocalDateTime calculateSlaDueAt(TicketPriority priority, LocalDateTime baseline) {
        LocalDateTime start = (baseline != null) ? baseline : LocalDateTime.now();
        if (priority == null) {
            return start.plusHours(mediumHours);
        }

        return switch (priority) {
            case HIGH -> start.plusHours(highHours);
            case MEDIUM -> start.plusHours(mediumHours);
            case LOW -> start.plusHours(lowHours);
        };
    }

    /**
     * Background batch processor for SLA breaches and priority escalation.
     * Uses chunked pagination to safely process unbounded active ticket queues.
     */
    @Transactional
    public int checkAndEscalateBreachedTickets() {
        LocalDateTime now = LocalDateTime.now();
        List<TicketStatus> activeStatuses = List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);

        int totalEscalated = 0;
        int pageNumber = 0;
        int pageSize = 50;
        Page<Ticket> page;

        do {
            page = ticketRepository.findByStatusInAndSlaDueAtBeforeAndSlaBreachedFalse(
                    activeStatuses,
                    now,
                    PageRequest.of(pageNumber, pageSize, Sort.by("slaDueAt").ascending()));

            for (Ticket ticket : page.getContent()) {
                ticket.setSlaBreached(true);
                TicketPriority oldPriority = ticket.getPriority();
                boolean autoEscalated = false;

                if (escalationEnabled) {
                    if (oldPriority == TicketPriority.LOW) {
                        ticket.setPriority(TicketPriority.MEDIUM);
                        ticket.setEscalated(true);
                        autoEscalated = true;
                    } else if (oldPriority == TicketPriority.MEDIUM) {
                        ticket.setPriority(TicketPriority.HIGH);
                        ticket.setEscalated(true);
                        autoEscalated = true;
                    } else if (oldPriority == TicketPriority.HIGH) {
                        ticket.setEscalated(true);
                        autoEscalated = true;
                    }
                }

                ticketRepository.save(ticket);
                totalEscalated++;

                String formattedDue = ticket.getSlaDueAt() != null ? ticket.getSlaDueAt().format(DATE_FORMATTER) : "N/A";
                String auditMsg = String.format(
                        "SLA breached (Due: %s)! %s",
                        formattedDue,
                        autoEscalated ? "Priority escalated from " + oldPriority + " to " + ticket.getPriority() : "Marked as breached"
                );

                auditService.recordEntityChange("TICKET", ticket.getId(), "SLA_BREACH", "SYSTEM", auditMsg);
                log.warn("Ticket #{} breached SLA. Auto-escalated: {}. New priority: {}",
                        ticket.getId(), autoEscalated, ticket.getPriority());

                // Asynchronously notify assigned agent if one is present
                if (ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getEmail() != null) {
                    emailService.sendSlaBreachEmail(
                            ticket.getAssignedAgent().getEmail(),
                            ticket.getId(),
                            ticket.getTitle(),
                            ticket.getPriority().name(),
                            formattedDue,
                            ticket.isEscalated()
                    );
                }
            }

            pageNumber++;
        } while (page.hasNext());

        if (totalEscalated > 0) {
            log.info("SLA Escalation Engine processed {} breached ticket(s).", totalEscalated);
        }

        return totalEscalated;
    }

    /**
     * Aggregates live operational SLA performance indicators for dashboard KPI widgets.
     */
    @Transactional(readOnly = true)
    public SlaMetricsDTO getSlaMetrics() {
        LocalDateTime now = LocalDateTime.now();
        List<TicketStatus> activeStatuses = List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);

        long totalActive = ticketRepository.countByStatusIn(activeStatuses);
        long breached = ticketRepository.countByStatusInAndSlaBreachedTrue(activeStatuses);
        long nearBreach = ticketRepository.countByStatusInAndSlaBreachedFalseAndSlaDueAtBetween(
                activeStatuses, now, now.plusHours(warningHours));

        long withinSla = Math.max(0, totalActive - breached - nearBreach);
        long totalResolved = ticketRepository.countTotalResolved();
        long resolvedWithinSla = ticketRepository.countResolvedWithinSla();

        double complianceRate = totalResolved > 0
                ? Math.round(((double) resolvedWithinSla / totalResolved) * 1000.0) / 10.0
                : 100.0;

        return SlaMetricsDTO.builder()
                .totalActive(totalActive)
                .withinSla(withinSla)
                .nearBreach(nearBreach)
                .breached(breached)
                .totalResolved(totalResolved)
                .resolvedWithinSla(resolvedWithinSla)
                .complianceRatePercent(complianceRate)
                .build();
    }
}
