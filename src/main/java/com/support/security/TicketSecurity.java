package com.support.security;

import com.support.repository.TicketRepository;
import com.support.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Custom SpEL security evaluator for Ticket resource access.
 * Used in @PreAuthorize("@ticketSecurity.canAccessTicket(#ticketId, authentication)") annotations.
 */
@Slf4j
@Component("ticketSecurity")
public class TicketSecurity {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private SecurityUtils securityUtils;

    public boolean canAccessTicket(Long ticketId, Authentication authentication) {
        if (authentication == null || ticketId == null) {
            return false;
        }

        // Admins and Support Agents have platform-wide ticket access
        boolean isAdminOrAgent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPPORT_AGENT"));
        if (isAdminOrAgent) {
            return true;
        }

        // Customers can only access their own tickets
        Long currentUserId = securityUtils.resolveUserId(authentication);
        boolean allowed = ticketRepository.findById(ticketId)
                .map(ticket -> ticket.getCustomer() != null && ticket.getCustomer().getId().equals(currentUserId))
                .orElse(false);

        if (!allowed) {
            log.warn("Security rejection: user '{}' (id={}) unauthorized to access ticket id={}", authentication.getName(), currentUserId, ticketId);
        }
        return allowed;
    }
}
