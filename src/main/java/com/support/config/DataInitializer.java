package com.support.config;

import com.support.entity.*;
import com.support.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketStatusHistoryRepository historyRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            log.debug("Database already seeded, skipping initialization.");
            return;
        }

        // 1. Create System Admin
        User sysAdmin = new User();
        sysAdmin.setUsername("sysadmin");
        sysAdmin.setPassword(passwordEncoder.encode("sysadmin123"));
        sysAdmin.setEmail("sysadmin@tixmanager.com");
        sysAdmin.setRole(UserRole.SYSTEM_ADMIN);
        userRepository.save(sysAdmin);

        // 2. Create Admin
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEmail("admin@tixmanager.com");
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);

        // 2. Create Support Agents
        User priya = new User();
        priya.setUsername("priya_agent");
        priya.setPassword(passwordEncoder.encode("agent123"));
        priya.setEmail("priya@tixmanager.com");
        priya.setRole(UserRole.SUPPORT_AGENT);
        userRepository.save(priya);

        User rahul = new User();
        rahul.setUsername("rahul_agent");
        rahul.setPassword(passwordEncoder.encode("agent123"));
        rahul.setEmail("rahul@tixmanager.com");
        rahul.setRole(UserRole.SUPPORT_AGENT);
        userRepository.save(rahul);

        // 3. Create Customers
        User farhan = new User();
        farhan.setUsername("farhan_dev");
        farhan.setPassword(passwordEncoder.encode("customer123"));
        farhan.setEmail("farhan@example.com");
        farhan.setRole(UserRole.CUSTOMER);
        userRepository.save(farhan);

        User alex = new User();
        alex.setUsername("alex_chen");
        alex.setPassword(passwordEncoder.encode("customer123"));
        alex.setEmail("alex@example.com");
        alex.setRole(UserRole.CUSTOMER);
        userRepository.save(alex);

        // 4. Create Sample Tickets
        // Ticket 1: In Progress
        Ticket t1 = new Ticket();
        t1.setTitle("Login page shows 403 error after password reset");
        t1.setDescription("After resetting my password via the forgot-password flow, I am unable to log in. The page returns a 403 Forbidden error.");
        t1.setStatus(TicketStatus.IN_PROGRESS);
        t1.setPriority(TicketPriority.HIGH);
        t1.setSlaDueAt(java.time.LocalDateTime.now().plusHours(2)); // Approaching breach (Warning)
        t1.setSlaBreached(false);
        t1.setEscalated(false);
        t1.setCustomer(farhan);
        t1.setAssignedAgent(priya);
        ticketRepository.save(t1);

        TicketStatusHistory h1 = new TicketStatusHistory();
        h1.setTicket(t1);
        h1.setPreviousStatus(TicketStatus.OPEN);
        h1.setNewStatus(TicketStatus.IN_PROGRESS);
        h1.setChangedBy(priya);
        historyRepository.save(h1);

        Message m1 = new Message();
        m1.setTicket(t1);
        m1.setSender(farhan);
        m1.setContent("Hi, I am unable to log in after resetting my password. I get a 403 Forbidden error every time.");
        messageRepository.save(m1);

        Message m2 = new Message();
        m2.setTicket(t1);
        m2.setSender(priya);
        m2.setContent("Hello Farhan, thank you for reporting this. I can see the issue in our logs. I am working on a fix now.");
        messageRepository.save(m2);

        // Ticket 2: Open / Unassigned
        Ticket t2 = new Ticket();
        t2.setTitle("Cannot export monthly invoice PDF");
        t2.setDescription("When I click the 'Export PDF' button on the billing page, nothing happens. The browser console shows a network timeout error.");
        t2.setStatus(TicketStatus.OPEN);
        t2.setPriority(TicketPriority.MEDIUM);
        t2.setSlaDueAt(java.time.LocalDateTime.now().plusHours(18)); // Within SLA (OK)
        t2.setSlaBreached(false);
        t2.setEscalated(false);
        t2.setCustomer(farhan);
        ticketRepository.save(t2);

        TicketStatusHistory h2 = new TicketStatusHistory();
        h2.setTicket(t2);
        h2.setPreviousStatus(null);
        h2.setNewStatus(TicketStatus.OPEN);
        h2.setChangedBy(farhan);
        historyRepository.save(h2);

        // Ticket 3: Resolved
        Ticket t3 = new Ticket();
        t3.setTitle("Request to upgrade subscription plan");
        t3.setDescription("I would like to upgrade my current Basic plan to the Professional plan. Please let me know the steps required.");
        t3.setStatus(TicketStatus.RESOLVED);
        t3.setPriority(TicketPriority.LOW);
        t3.setSlaDueAt(java.time.LocalDateTime.now().minusDays(1));
        t3.setResolvedAt(java.time.LocalDateTime.now().minusHours(28));
        t3.setSlaBreached(false);
        t3.setEscalated(false);
        t3.setCustomer(farhan);
        t3.setAssignedAgent(rahul);
        ticketRepository.save(t3);

        TicketStatusHistory h3a = new TicketStatusHistory();
        h3a.setTicket(t3);
        h3a.setPreviousStatus(TicketStatus.OPEN);
        h3a.setNewStatus(TicketStatus.IN_PROGRESS);
        h3a.setChangedBy(rahul);
        historyRepository.save(h3a);

        TicketStatusHistory h3b = new TicketStatusHistory();
        h3b.setTicket(t3);
        h3b.setPreviousStatus(TicketStatus.IN_PROGRESS);
        h3b.setNewStatus(TicketStatus.RESOLVED);
        h3b.setChangedBy(rahul);
        historyRepository.save(h3b);

        Message m3 = new Message();
        m3.setTicket(t3);
        m3.setSender(farhan);
        m3.setContent("I would like to upgrade to the Professional plan. What are the steps?");
        messageRepository.save(m3);

        Message m4 = new Message();
        m4.setTicket(t3);
        m4.setSender(rahul);
        m4.setContent("Hi Farhan! I have processed your upgrade request. Your plan has been upgraded to Professional.");
        messageRepository.save(m4);

        // Ticket 4: Alex's Open Ticket (Breached & Auto-Escalated)
        Ticket t4 = new Ticket();
        t4.setTitle("Webhook delivery failing with 502 errors");
        t4.setDescription("Our webhook endpoint has been receiving 502 Bad Gateway responses from TixManager for the past 3 hours.");
        t4.setStatus(TicketStatus.OPEN);
        t4.setPriority(TicketPriority.HIGH);
        t4.setSlaDueAt(java.time.LocalDateTime.now().minusHours(2)); // Breached
        t4.setSlaBreached(true);
        t4.setEscalated(true);
        t4.setCustomer(alex);
        ticketRepository.save(t4);

        TicketStatusHistory h4 = new TicketStatusHistory();
        h4.setTicket(t4);
        h4.setPreviousStatus(null);
        h4.setNewStatus(TicketStatus.OPEN);
        h4.setChangedBy(alex);
        historyRepository.save(h4);

        log.info("Database initialized with seed data (users: {}, tickets: {})", userRepository.count(), ticketRepository.count());
    }
}
