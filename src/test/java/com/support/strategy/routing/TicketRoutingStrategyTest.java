package com.support.strategy.routing;

import com.support.entity.Ticket;
import com.support.entity.TicketPriority;
import com.support.entity.TicketStatus;
import com.support.entity.User;
import com.support.entity.UserRole;
import com.support.exception.InvalidOperationException;
import com.support.repository.TicketRepository;
import com.support.repository.UserRepository;
import com.support.service.TicketRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * ==============================================================================================
 * AUTOMATED TESTING SUITE: Strategy Pattern Tests (Ticket Routing)
 * ==============================================================================================
 *
 * Demonstrates how the GoF Strategy Pattern isolates each routing algorithm,
 * allowing each strategy to be tested independently without coupling to persistence or REST layers.
 */
@ExtendWith(MockitoExtension.class)
class TicketRoutingStrategyTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    private User agentA;
    private User agentB;
    private User agentC;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        agentA = new User();
        agentA.setId(10L);
        agentA.setUsername("agent_alice");
        agentA.setRole(UserRole.SUPPORT_AGENT);

        agentB = new User();
        agentB.setId(20L);
        agentB.setUsername("agent_bob");
        agentB.setRole(UserRole.SUPPORT_AGENT);

        agentC = new User();
        agentC.setId(30L);
        agentC.setUsername("agent_charlie");
        agentC.setRole(UserRole.SUPPORT_AGENT);

        ticket = new Ticket();
        ticket.setId(100L);
        ticket.setTitle("Database connection timeout");
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);
    }

    @Nested
    @DisplayName("WorkloadBalancedRoutingStrategy Tests")
    class WorkloadBalancedTests {

        private WorkloadBalancedRoutingStrategy strategy;

        @BeforeEach
        void initStrategy() {
            strategy = new WorkloadBalancedRoutingStrategy(ticketRepository);
        }

        @Test
        @DisplayName("selectAgent — Picks agent with the fewest active tickets")
        void testSelectAgent_PicksLeastBusy() {
            // Arrange: Alice has 5 tickets, Bob has 1 ticket, Charlie has 3 tickets
            when(ticketRepository.countActiveTicketsByAgentId(10L)).thenReturn(5L);
            when(ticketRepository.countActiveTicketsByAgentId(20L)).thenReturn(1L);
            when(ticketRepository.countActiveTicketsByAgentId(30L)).thenReturn(3L);

            // Act
            Optional<User> selected = strategy.selectAgent(ticket, List.of(agentA, agentB, agentC));

            // Assert: Bob has the lightest workload
            assertThat(selected).isPresent();
            assertThat(selected.get()).isEqualTo(agentB);
        }

        @Test
        @DisplayName("selectAgent — Breaks ties deterministically by lowest user ID")
        void testSelectAgent_TieBreaker() {
            // Arrange: Alice (id=10) and Bob (id=20) both have 2 tickets
            when(ticketRepository.countActiveTicketsByAgentId(10L)).thenReturn(2L);
            when(ticketRepository.countActiveTicketsByAgentId(20L)).thenReturn(2L);

            // Act
            Optional<User> selected = strategy.selectAgent(ticket, List.of(agentB, agentA));

            // Assert: Alice picked due to lower ID
            assertThat(selected).isPresent();
            assertThat(selected.get().getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("selectAgent — Returns empty Optional if agent list is empty")
        void testSelectAgent_EmptyList() {
            assertThat(strategy.selectAgent(ticket, Collections.emptyList())).isEmpty();
            assertThat(strategy.selectAgent(ticket, null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("RoundRobinRoutingStrategy Tests")
    class RoundRobinTests {

        private RoundRobinRoutingStrategy strategy;

        @BeforeEach
        void initStrategy() {
            strategy = new RoundRobinRoutingStrategy();
        }

        @Test
        @DisplayName("selectAgent — Cycles circularly through candidate agents")
        void testSelectAgent_CyclesCircularly() {
            List<User> agents = List.of(agentA, agentB, agentC);

            // First 3 calls should visit each agent in order
            Optional<User> first = strategy.selectAgent(ticket, agents);
            Optional<User> second = strategy.selectAgent(ticket, agents);
            Optional<User> third = strategy.selectAgent(ticket, agents);

            // 4th call should wrap around to the first agent
            Optional<User> fourth = strategy.selectAgent(ticket, agents);

            assertThat(first).contains(agentA);
            assertThat(second).contains(agentB);
            assertThat(third).contains(agentC);
            assertThat(fourth).contains(agentA);
        }

        @Test
        @DisplayName("selectAgent — Returns empty Optional on empty agent list")
        void testSelectAgent_EmptyList() {
            assertThat(strategy.selectAgent(ticket, Collections.emptyList())).isEmpty();
        }
    }

    @Nested
    @DisplayName("PriorityBasedRoutingStrategy Tests")
    class PriorityBasedTests {

        private PriorityBasedRoutingStrategy strategy;

        @BeforeEach
        void initStrategy() {
            strategy = new PriorityBasedRoutingStrategy(ticketRepository);
        }

        @Test
        @DisplayName("selectAgent — HIGH priority ticket routed strictly to least busy agent")
        void testSelectAgent_HighPriorityRoutesToLeastBusy() {
            ticket.setPriority(TicketPriority.HIGH);

            // Alice has 4, Bob has 0, Charlie has 2
            when(ticketRepository.countActiveTicketsByAgentId(10L)).thenReturn(4L);
            when(ticketRepository.countActiveTicketsByAgentId(20L)).thenReturn(0L);
            when(ticketRepository.countActiveTicketsByAgentId(30L)).thenReturn(2L);

            Optional<User> selected = strategy.selectAgent(ticket, List.of(agentA, agentB, agentC));

            assertThat(selected).isPresent();
            assertThat(selected.get()).isEqualTo(agentB);
        }

        @Test
        @DisplayName("selectAgent — MEDIUM / LOW priority ticket distributed circularly")
        void testSelectAgent_MediumPriorityCycles() {
            ticket.setPriority(TicketPriority.MEDIUM);
            List<User> agents = List.of(agentA, agentB);

            Optional<User> first = strategy.selectAgent(ticket, agents);
            Optional<User> second = strategy.selectAgent(ticket, agents);

            assertThat(first).contains(agentA);
            assertThat(second).contains(agentB);
        }
    }

    @Nested
    @DisplayName("TicketRoutingContext Registry Tests")
    class ContextRegistryTests {

        private TicketRoutingContext context;
        private WorkloadBalancedRoutingStrategy workloadStrategy;
        private RoundRobinRoutingStrategy roundRobinStrategy;
        private PriorityBasedRoutingStrategy priorityStrategy;

        @BeforeEach
        void initContext() {
            workloadStrategy = new WorkloadBalancedRoutingStrategy(ticketRepository);
            roundRobinStrategy = new RoundRobinRoutingStrategy();
            priorityStrategy = new PriorityBasedRoutingStrategy(ticketRepository);

            context = new TicketRoutingContext(workloadStrategy, roundRobinStrategy, priorityStrategy);
        }

        @Test
        @DisplayName("getStrategy — Returns appropriate strategy bean for each enum type")
        void testGetStrategy() {
            assertThat(context.getStrategy(RoutingStrategyType.WORKLOAD_BALANCED)).isInstanceOf(WorkloadBalancedRoutingStrategy.class);
            assertThat(context.getStrategy(RoutingStrategyType.ROUND_ROBIN)).isInstanceOf(RoundRobinRoutingStrategy.class);
            assertThat(context.getStrategy(RoutingStrategyType.PRIORITY_BASED)).isInstanceOf(PriorityBasedRoutingStrategy.class);
            // Null defaults to WORKLOAD_BALANCED
            assertThat(context.getStrategy(null)).isInstanceOf(WorkloadBalancedRoutingStrategy.class);
        }

        @Test
        @DisplayName("routeTicket — Successfully delegates routing to chosen strategy")
        void testRouteTicket() {
            when(ticketRepository.countActiveTicketsByAgentId(10L)).thenReturn(10L);
            when(ticketRepository.countActiveTicketsByAgentId(20L)).thenReturn(1L);

            Optional<User> selected = context.routeTicket(ticket, List.of(agentA, agentB), RoutingStrategyType.WORKLOAD_BALANCED);

            assertThat(selected).isPresent();
            assertThat(selected.get()).isEqualTo(agentB);
        }
    }

    @Nested
    @DisplayName("TicketRoutingService Orchestration Tests")
    class ServiceOrchestrationTests {

        private TicketRoutingService routingService;
        private TicketRoutingContext context;

        @BeforeEach
        void initService() {
            context = new TicketRoutingContext(
                    new WorkloadBalancedRoutingStrategy(ticketRepository),
                    new RoundRobinRoutingStrategy(),
                    new PriorityBasedRoutingStrategy(ticketRepository)
            );
            routingService = new TicketRoutingService(userRepository, context);
        }

        @Test
        @DisplayName("resolveAgent — Successfully fetches agents and delegates to strategy")
        void testResolveAgent_Success() {
            when(userRepository.findByRole(UserRole.SUPPORT_AGENT)).thenReturn(List.of(agentA, agentB));
            when(ticketRepository.countActiveTicketsByAgentId(10L)).thenReturn(3L);
            when(ticketRepository.countActiveTicketsByAgentId(20L)).thenReturn(0L);

            User chosen = routingService.resolveAgent(ticket, RoutingStrategyType.WORKLOAD_BALANCED);

            assertThat(chosen).isEqualTo(agentB);
        }

        @Test
        @DisplayName("resolveAgent — Throws InvalidOperationException when 0 agents exist")
        void testResolveAgent_NoAgents_ThrowsException() {
            when(userRepository.findByRole(UserRole.SUPPORT_AGENT)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> routingService.resolveAgent(ticket, RoutingStrategyType.WORKLOAD_BALANCED))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("No active support agents available");
        }
    }
}
