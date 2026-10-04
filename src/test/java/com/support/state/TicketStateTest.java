package com.support.state;

import com.support.entity.Ticket;
import com.support.entity.TicketStatus;
import com.support.entity.User;
import com.support.entity.UserRole;
import com.support.exception.InvalidOperationException;
import com.support.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ==============================================================================================
 * AUTOMATED TESTING SUITE: State Pattern Tests (Ticket Lifecycle)
 * ==============================================================================================
 *
 * This test suite demonstrates how each concrete state isolates its own transition rules
 * and lifecycle side-effects, verifying the GoF State Pattern implementation.
 */
class TicketStateTest {

    private TicketStateFactory stateFactory;
    private Ticket ticket;
    private User agent;
    private User customer;

    @BeforeEach
    void setUp() {
        stateFactory = new TicketStateFactory();

        ticket = new Ticket();
        ticket.setId(42L);
        ticket.setTitle("Payment Gateway 500");
        ticket.setStatus(TicketStatus.OPEN);

        agent = new User();
        agent.setId(2L);
        agent.setUsername("support_sarah");
        agent.setRole(UserRole.SUPPORT_AGENT);

        customer = new User();
        customer.setId(1L);
        customer.setUsername("customer_john");
        customer.setRole(UserRole.CUSTOMER);
    }

    @Nested
    @DisplayName("OpenTicketState Lifecycle Rules")
    class OpenStateTests {

        private final OpenTicketState state = new OpenTicketState();

        @Test
        @DisplayName("canTransitionTo — Permits IN_PROGRESS, denies RESOLVED")
        void testCanTransitionTo() {
            assertThat(state.canTransitionTo(TicketStatus.IN_PROGRESS)).isTrue();
            assertThat(state.canTransitionTo(TicketStatus.RESOLVED)).isFalse();
            assertThat(state.canTransitionTo(TicketStatus.OPEN)).isFalse();
        }

        @Test
        @DisplayName("transitionTo — Successfully moves OPEN ticket to IN_PROGRESS")
        void testTransitionTo_InProgress_Success() {
            state.transitionTo(ticket, TicketStatus.IN_PROGRESS, agent);
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("transitionTo — Throws InvalidStateTransitionException on direct OPEN -> RESOLVED jump")
        void testTransitionTo_Resolved_ThrowsException() {
            assertThatThrownBy(() -> state.transitionTo(ticket, TicketStatus.RESOLVED, agent))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("Tickets in OPEN state cannot be directly resolved");
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        }

        @Test
        @DisplayName("assign — Automatically transitions OPEN ticket to IN_PROGRESS when agent claimed")
        void testAssign_AutoTransitionsToInProgress() {
            state.assign(ticket, agent);
            assertThat(ticket.getAssignedAgent()).isEqualTo(agent);
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("InProgressTicketState Lifecycle Rules")
    class InProgressStateTests {

        private final InProgressTicketState state = new InProgressTicketState();

        @BeforeEach
        void setupInProgressTicket() {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticket.setAssignedAgent(agent);
        }

        @Test
        @DisplayName("canTransitionTo — Permits RESOLVED and OPEN")
        void testCanTransitionTo() {
            assertThat(state.canTransitionTo(TicketStatus.RESOLVED)).isTrue();
            assertThat(state.canTransitionTo(TicketStatus.OPEN)).isTrue();
            assertThat(state.canTransitionTo(TicketStatus.IN_PROGRESS)).isFalse();
        }

        @Test
        @DisplayName("transitionTo — Resolving within SLA freezes resolvedAt and marks slaBreached=false")
        void testTransitionTo_ResolvedWithinSla() {
            ticket.setSlaDueAt(LocalDateTime.now().plusHours(4)); // 4 hours remaining

            state.transitionTo(ticket, TicketStatus.RESOLVED, agent);

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
            assertThat(ticket.getResolvedAt()).isNotNull();
            assertThat(ticket.isSlaBreached()).isFalse();
        }

        @Test
        @DisplayName("transitionTo — Resolving past SLA deadline freezes resolvedAt and marks slaBreached=true")
        void testTransitionTo_ResolvedPastSla() {
            ticket.setSlaDueAt(LocalDateTime.now().minusMinutes(30)); // 30 mins overdue

            state.transitionTo(ticket, TicketStatus.RESOLVED, agent);

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
            assertThat(ticket.getResolvedAt()).isNotNull();
            assertThat(ticket.isSlaBreached()).isTrue();
        }

        @Test
        @DisplayName("transitionTo — Return IN_PROGRESS ticket to OPEN triage pool")
        void testTransitionTo_ReturnToOpen() {
            state.transitionTo(ticket, TicketStatus.OPEN, agent);
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        }

        @Test
        @DisplayName("assign — Reassigns agent while retaining IN_PROGRESS status")
        void testAssign_ReassignAgent() {
            User newAgent = new User();
            newAgent.setId(5L);
            newAgent.setUsername("support_bob");

            state.assign(ticket, newAgent);

            assertThat(ticket.getAssignedAgent()).isEqualTo(newAgent);
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("ResolvedTicketState Lifecycle Rules")
    class ResolvedStateTests {

        private final ResolvedTicketState state = new ResolvedTicketState();

        @BeforeEach
        void setupResolvedTicket() {
            ticket.setStatus(TicketStatus.RESOLVED);
            ticket.setResolvedAt(LocalDateTime.now().minusHours(1));
            ticket.setSlaDueAt(LocalDateTime.now().plusHours(2));
            ticket.setSlaBreached(false);
        }

        @Test
        @DisplayName("canTransitionTo — Permits reopening to IN_PROGRESS, denies OPEN")
        void testCanTransitionTo() {
            assertThat(state.canTransitionTo(TicketStatus.IN_PROGRESS)).isTrue();
            assertThat(state.canTransitionTo(TicketStatus.OPEN)).isFalse();
            assertThat(state.canTransitionTo(TicketStatus.RESOLVED)).isFalse();
        }

        @Test
        @DisplayName("transitionTo — Reopening ticket clears resolvedAt and unfreezes SLA check")
        void testTransitionTo_ReopenTicket() {
            state.transitionTo(ticket, TicketStatus.IN_PROGRESS, customer);

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
            assertThat(ticket.getResolvedAt()).isNull(); // Cleared on reopen
            assertThat(ticket.isSlaBreached()).isFalse(); // Still before slaDueAt
        }

        @Test
        @DisplayName("transitionTo — Reopening ticket when SLA has expired marks slaBreached=true")
        void testTransitionTo_ReopenExpiredSlaTicket() {
            ticket.setSlaDueAt(LocalDateTime.now().minusHours(1)); // SLA deadline was in the past

            state.transitionTo(ticket, TicketStatus.IN_PROGRESS, customer);

            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
            assertThat(ticket.getResolvedAt()).isNull();
            assertThat(ticket.isSlaBreached()).isTrue();
        }

        @Test
        @DisplayName("transitionTo — Direct transition from RESOLVED to OPEN is blocked")
        void testTransitionTo_DirectOpen_ThrowsException() {
            assertThatThrownBy(() -> state.transitionTo(ticket, TicketStatus.OPEN, customer))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("Direct transition to OPEN is not allowed");
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        }

        @Test
        @DisplayName("assign — Direct assignment to RESOLVED ticket is rejected with InvalidOperationException")
        void testAssign_ResolvedTicket_ThrowsException() {
            assertThatThrownBy(() -> state.assign(ticket, agent))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("Cannot assign an agent to a RESOLVED ticket");
        }
    }

    @Nested
    @DisplayName("TicketStateFactory Coordinator Tests")
    class FactoryTests {

        @Test
        @DisplayName("getState — Returns correct state handlers for each enum status")
        void testGetState() {
            assertThat(stateFactory.getState(TicketStatus.OPEN)).isInstanceOf(OpenTicketState.class);
            assertThat(stateFactory.getState(TicketStatus.IN_PROGRESS)).isInstanceOf(InProgressTicketState.class);
            assertThat(stateFactory.getState(TicketStatus.RESOLVED)).isInstanceOf(ResolvedTicketState.class);
            assertThat(stateFactory.getState(null)).isInstanceOf(OpenTicketState.class);
        }

        @Test
        @DisplayName("applyTransition — Idempotent when target matches current status")
        void testApplyTransition_Idempotent() {
            ticket.setStatus(TicketStatus.OPEN);
            stateFactory.applyTransition(ticket, TicketStatus.OPEN, agent);
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        }

        @Test
        @DisplayName("canTransition — Correctly identifies legal and illegal transitions")
        void testCanTransition() {
            assertThat(stateFactory.canTransition(TicketStatus.OPEN, TicketStatus.OPEN)).isTrue();
            assertThat(stateFactory.canTransition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS)).isTrue();
            assertThat(stateFactory.canTransition(TicketStatus.OPEN, TicketStatus.RESOLVED)).isFalse();
            assertThat(stateFactory.canTransition(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED)).isTrue();
            assertThat(stateFactory.canTransition(TicketStatus.RESOLVED, TicketStatus.OPEN)).isFalse();
        }
    }
}
