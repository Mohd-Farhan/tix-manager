package com.support.security;

import com.support.repository.TicketRepository;
import com.support.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Custom SpEL security evaluator for Ticket resource access.
 * Used in @PreAuthorize("@ticketSecurity.canAccessTicket(#ticketId, authentication)") annotations.
 */
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
        return ticketRepository.findById(ticketId)
                .map(ticket -> ticket.getCustomer() != null && ticket.getCustomer().getId().equals(currentUserId))
                .orElse(false);
    }
}
