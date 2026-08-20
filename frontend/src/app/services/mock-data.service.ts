import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { Ticket, TicketStatus, TicketPriority, TicketStatusHistory } from '../models/ticket.model';
import { Message } from '../models/message.model';
import { User, UserRole } from '../models/user.model';

export interface SystemUser extends User {
  status: 'active' | 'inactive';
}

@Injectable({ providedIn: 'root' })
export class MockDataService {
  private customerUser: User = {
    id: 1,
    username: 'farhan_dev',
    email: 'farhan@example.com',
    role: UserRole.CUSTOMER,
    createdAt: '2025-06-01T10:00:00',
  };

  private agentUser: User = {
    id: 10,
    username: 'Priya Sharma',
    email: 'priya.sharma@tixmanager.com',
    role: UserRole.SUPPORT_AGENT,
    createdAt: '2025-01-15T08:00:00',
  };

  private adminUser: User = {
    id: 100,
    username: 'Admin',
    email: 'admin@tixmanager.com',
    role: UserRole.ADMIN,
    createdAt: '2024-01-01T00:00:00',
  };

  private currentUser: User = this.customerUser;

  /* Customer name lookup */
  private customerNames: Record<number, string> = {
    1: 'farhan_dev',
    2: 'alex_chen',
    3: 'maria_garcia',
  };

  /* All system users for admin user management */
  private systemUsers: SystemUser[] = [
    { id: 100, username: 'Admin', email: 'admin@tixmanager.com', role: UserRole.ADMIN, createdAt: '2024-01-01T00:00:00', status: 'active' },
    { id: 10, username: 'Priya Sharma', email: 'priya.sharma@tixmanager.com', role: UserRole.SUPPORT_AGENT, createdAt: '2025-01-15T08:00:00', status: 'active' },
    { id: 11, username: 'Rahul Verma', email: 'rahul.verma@tixmanager.com', role: UserRole.SUPPORT_AGENT, createdAt: '2025-02-20T09:00:00', status: 'active' },
    { id: 12, username: 'Anita Desai', email: 'anita.desai@tixmanager.com', role: UserRole.SUPPORT_AGENT, createdAt: '2025-05-10T10:00:00', status: 'inactive' },
    { id: 1, username: 'farhan_dev', email: 'farhan@example.com', role: UserRole.CUSTOMER, createdAt: '2025-06-01T10:00:00', status: 'active' },
    { id: 2, username: 'alex_chen', email: 'alex.chen@example.com', role: UserRole.CUSTOMER, createdAt: '2025-07-12T14:00:00', status: 'active' },
    { id: 3, username: 'maria_garcia', email: 'maria.garcia@example.com', role: UserRole.CUSTOMER, createdAt: '2025-08-05T11:00:00', status: 'active' },
    { id: 4, username: 'james_wilson', email: 'james.wilson@example.com', role: UserRole.CUSTOMER, createdAt: '2026-01-22T09:30:00', status: 'active' },
    { id: 5, username: 'sara_khan', email: 'sara.khan@example.com', role: UserRole.CUSTOMER, createdAt: '2026-03-14T16:45:00', status: 'inactive' },
  ];

  private nextUserId = 200;

  private tickets: Ticket[] = [
    // === Customer 1 (farhan_dev) tickets ===
    {
      id: 1001,
      title: 'Login page shows 403 error after password reset',
      description:
        'After resetting my password via the forgot-password flow, I am unable to log in. The page returns a 403 Forbidden error. I have tried clearing cookies and using incognito mode, but the issue persists.',
      status: TicketStatus.IN_PROGRESS,
      priority: TicketPriority.HIGH,
      customerId: 1,
      assignedAgentId: 10,
      assignedAgentName: 'Priya Sharma',
      createdAt: '2026-08-15T09:30:00',
      updatedAt: '2026-08-16T14:22:00',
      deleted: false,
      history: [
        { id: 1, ticketId: 1001, previousStatus: TicketStatus.OPEN, newStatus: TicketStatus.IN_PROGRESS, changedById: 10, changedByUsername: 'Priya Sharma', changedAt: '2026-08-15T11:00:00' },
      ],
    },
    {
      id: 1002,
      title: 'Cannot export monthly invoice PDF',
      description:
        'When I click the "Export PDF" button on the billing page, nothing happens. The browser console shows a network timeout error. This started happening after the latest update.',
      status: TicketStatus.OPEN,
      priority: TicketPriority.MEDIUM,
      customerId: 1,
      createdAt: '2026-08-16T11:45:00',
      deleted: false,
      history: [],
    },
    {
      id: 1003,
      title: 'Request to upgrade subscription plan',
      description:
        'I would like to upgrade my current Basic plan to the Professional plan. Please let me know the steps required and if there are any prorated charges for mid-cycle upgrades.',
      status: TicketStatus.RESOLVED,
      priority: TicketPriority.LOW,
      customerId: 1,
      assignedAgentId: 11,
      assignedAgentName: 'Rahul Verma',
      createdAt: '2026-08-10T08:15:00',
      updatedAt: '2026-08-12T16:30:00',
      deleted: false,
      history: [
        { id: 2, ticketId: 1003, previousStatus: TicketStatus.OPEN, newStatus: TicketStatus.IN_PROGRESS, changedById: 11, changedByUsername: 'Rahul Verma', changedAt: '2026-08-10T10:00:00' },
        { id: 3, ticketId: 1003, previousStatus: TicketStatus.IN_PROGRESS, newStatus: TicketStatus.RESOLVED, changedById: 11, changedByUsername: 'Rahul Verma', changedAt: '2026-08-12T16:30:00' },
      ],
    },
    {
      id: 1004,
      title: 'Dashboard widgets not loading on Safari',
      description:
        'The analytics dashboard widgets fail to render on Safari 17.x. Chrome and Firefox work fine. I see a blank white area where the charts should be.',
      status: TicketStatus.OPEN,
      priority: TicketPriority.HIGH,
      customerId: 1,
      createdAt: '2026-08-17T15:20:00',
      deleted: false,
      history: [],
    },
    {
      id: 1005,
      title: 'Feature request: Dark mode for reports',
      description:
        'It would be great if the report generation module supported a dark mode theme to match the rest of the application. Currently the export and preview screens are always in light mode.',
      status: TicketStatus.IN_PROGRESS,
      priority: TicketPriority.LOW,
      customerId: 1,
      assignedAgentId: 10,
      assignedAgentName: 'Priya Sharma',
      createdAt: '2026-08-14T13:00:00',
      updatedAt: '2026-08-15T09:00:00',
      deleted: false,
      history: [
        { id: 4, ticketId: 1005, previousStatus: TicketStatus.OPEN, newStatus: TicketStatus.IN_PROGRESS, changedById: 10, changedByUsername: 'Priya Sharma', changedAt: '2026-08-15T09:00:00' },
      ],
    },
    {
      id: 1006,
      title: 'Email notifications arriving late',
      description: 'Ticket status update emails are arriving 2-3 hours late. Previously they were near-instant.',
      status: TicketStatus.RESOLVED,
      priority: TicketPriority.MEDIUM,
      customerId: 1,
      assignedAgentId: 11,
      assignedAgentName: 'Rahul Verma',
      createdAt: '2026-08-08T10:00:00',
      updatedAt: '2026-08-09T17:45:00',
      deleted: false,
      history: [
        { id: 5, ticketId: 1006, previousStatus: TicketStatus.OPEN, newStatus: TicketStatus.IN_PROGRESS, changedById: 11, changedByUsername: 'Rahul Verma', changedAt: '2026-08-08T12:00:00' },
        { id: 6, ticketId: 1006, previousStatus: TicketStatus.IN_PROGRESS, newStatus: TicketStatus.RESOLVED, changedById: 11, changedByUsername: 'Rahul Verma', changedAt: '2026-08-09T17:45:00' },
      ],
    },
    // === Customer 2 (alex_chen) tickets ===
    {
      id: 2001,
      title: 'API rate limit exceeded on bulk import',
      description:
        'When importing more than 500 records via the bulk CSV import endpoint, the API returns 429 Too Many Requests after the first 200 records. The documentation says the limit is 1000 per minute.',
      status: TicketStatus.OPEN,
      priority: TicketPriority.HIGH,
      customerId: 2,
      createdAt: '2026-08-18T10:15:00',
      deleted: false,
      history: [],
    },
    {
      id: 2002,
      title: 'Two-factor authentication not sending SMS',
      description:
        'After enabling 2FA via SMS, I never receive the verification code. I have confirmed my phone number is correct and have tried multiple times over the past 2 days.',
      status: TicketStatus.OPEN,
      priority: TicketPriority.HIGH,
      customerId: 2,
      createdAt: '2026-08-17T14:30:00',
      deleted: false,
      history: [],
    },
    {
      id: 2003,
      title: 'Report scheduler runs at wrong timezone',
      description:
        'I have my timezone set to EST but scheduled reports generate at UTC time. A report scheduled for 9 AM EST arrives at 9 AM UTC (4 AM EST).',
      status: TicketStatus.IN_PROGRESS,
      priority: TicketPriority.MEDIUM,
      customerId: 2,
      assignedAgentId: 10,
      assignedAgentName: 'Priya Sharma',
      createdAt: '2026-08-12T08:00:00',
      updatedAt: '2026-08-13T10:30:00',
      deleted: false,
      history: [
        { id: 10, ticketId: 2003, previousStatus: TicketStatus.OPEN, newStatus: TicketStatus.IN_PROGRESS, changedById: 10, changedByUsername: 'Priya Sharma', changedAt: '2026-08-13T10:30:00' },
      ],
    },
    // === Customer 3 (maria_garcia) tickets ===
    {
      id: 3001,
      title: 'Custom branding logo appears pixelated',
      description:
        'After uploading a 1200x400px PNG logo via the branding settings, the logo appears blurry and pixelated on the customer portal. The original file is crisp.',
      status: TicketStatus.OPEN,
      priority: TicketPriority.LOW,
      customerId: 3,
      createdAt: '2026-08-19T09:00:00',
      deleted: false,
      history: [],
    },
    {
      id: 3002,
      title: 'Webhook delivery failing with 502 errors',
      description:
        'Our webhook endpoint has been receiving 502 Bad Gateway responses from TixManager for the past 3 hours. Our endpoint is healthy and responds to all other services correctly.',
      status: TicketStatus.OPEN,
      priority: TicketPriority.HIGH,
      customerId: 3,
      createdAt: '2026-08-19T11:30:00',
      deleted: false,
      history: [],
    },
    {
      id: 3003,
      title: 'SSO integration with Okta keeps disconnecting',
      description:
        'Our Okta SSO integration disconnects every 24 hours, forcing all users to re-authenticate. The SAML certificate is valid and has not expired. Other SAML integrations work fine.',
      status: TicketStatus.RESOLVED,
      priority: TicketPriority.MEDIUM,
      customerId: 3,
      assignedAgentId: 10,
      assignedAgentName: 'Priya Sharma',
      createdAt: '2026-08-05T13:00:00',
      updatedAt: '2026-08-07T16:00:00',
      deleted: false,
      history: [
        { id: 11, ticketId: 3003, previousStatus: TicketStatus.OPEN, newStatus: TicketStatus.IN_PROGRESS, changedById: 10, changedByUsername: 'Priya Sharma', changedAt: '2026-08-06T09:00:00' },
        { id: 12, ticketId: 3003, previousStatus: TicketStatus.IN_PROGRESS, newStatus: TicketStatus.RESOLVED, changedById: 10, changedByUsername: 'Priya Sharma', changedAt: '2026-08-07T16:00:00' },
      ],
    },
  ];

  private messages: Message[] = [
    { id: 1, ticketId: 1001, senderId: 1, senderUsername: 'farhan_dev', content: 'Hi, I am unable to log in after resetting my password. I get a 403 Forbidden error every time.', createdAt: '2026-08-15T09:30:00' },
    { id: 2, ticketId: 1001, senderId: 10, senderUsername: 'Priya Sharma', content: 'Hello Farhan, thank you for reporting this. I can see the issue in our logs. It appears the password reset token was not properly invalidated. I am working on a fix now.', createdAt: '2026-08-15T11:15:00' },
    { id: 3, ticketId: 1001, senderId: 1, senderUsername: 'farhan_dev', content: 'Thank you for the quick response! Let me know if you need any additional information from my end.', createdAt: '2026-08-15T11:30:00' },
    { id: 4, ticketId: 1001, senderId: 10, senderUsername: 'Priya Sharma', content: 'I have pushed a fix to staging. Could you please try logging in again and let me know if the issue persists?', createdAt: '2026-08-16T14:22:00' },
    { id: 5, ticketId: 1003, senderId: 1, senderUsername: 'farhan_dev', content: 'I would like to upgrade to the Professional plan. What are the steps?', createdAt: '2026-08-10T08:15:00' },
    { id: 6, ticketId: 1003, senderId: 11, senderUsername: 'Rahul Verma', content: 'Hi Farhan! I have processed your upgrade request. Your plan has been upgraded to Professional with prorated billing. You should see the changes reflected in your dashboard within the next hour.', createdAt: '2026-08-12T16:30:00' },
    { id: 7, ticketId: 2003, senderId: 2, senderUsername: 'alex_chen', content: 'The scheduled reports are running at the wrong time. My timezone is set to EST but they execute at UTC.', createdAt: '2026-08-12T08:00:00' },
    { id: 8, ticketId: 2003, senderId: 10, senderUsername: 'Priya Sharma', content: 'Hi Alex, I can confirm this is a known issue with our report scheduler. We are working on a timezone-aware fix. Will update you once deployed.', createdAt: '2026-08-13T10:30:00' },
    { id: 9, ticketId: 3003, senderId: 3, senderUsername: 'maria_garcia', content: 'Our Okta SSO disconnects every day. All 50 team members have to log in again each morning.', createdAt: '2026-08-05T13:00:00' },
    { id: 10, ticketId: 3003, senderId: 10, senderUsername: 'Priya Sharma', content: 'Hi Maria, I have identified the issue — your SAML session lifetime was set to 24h instead of rolling. I have updated the configuration. Please let me know if it persists.', createdAt: '2026-08-06T09:15:00' },
    { id: 11, ticketId: 3003, senderId: 3, senderUsername: 'maria_garcia', content: 'Confirmed fixed! Thank you so much for the quick turnaround.', createdAt: '2026-08-07T15:30:00' },
  ];

  private nextTicketId = 4001;
  private nextMessageId = 20;
  private nextHistoryId = 20;

  // Reactive events
  private openCreateTicketSubject = new Subject<void>();
  openCreateTicket$ = this.openCreateTicketSubject.asObservable();

  private ticketCreatedSubject = new Subject<Ticket>();
  ticketCreated$ = this.ticketCreatedSubject.asObservable();

  private ticketUpdatedSubject = new Subject<Ticket>();
  ticketUpdated$ = this.ticketUpdatedSubject.asObservable();

  triggerCreateTicket(): void {
    this.openCreateTicketSubject.next();
  }

  // === User Management ===

  getCurrentUser(): User {
    return { ...this.currentUser };
  }

  switchToAgent(): void {
    this.currentUser = this.agentUser;
  }

  switchToCustomer(): void {
    this.currentUser = this.customerUser;
  }

  switchToAdmin(): void {
    this.currentUser = this.adminUser;
  }

  getCustomerNameById(customerId: number): string {
    return this.customerNames[customerId] ?? `Customer #${customerId}`;
  }

  // === Customer-scoped Methods ===

  getTickets(): Ticket[] {
    return this.tickets.filter((t) => !t.deleted && t.customerId === this.currentUser.id).map((t) => ({ ...t }));
  }

  getTicketById(id: number): Ticket | undefined {
    const ticket = this.tickets.find((t) => t.id === id && !t.deleted);
    return ticket ? { ...ticket } : undefined;
  }

  createTicket(data: { title: string; description: string; priority: TicketPriority }): Ticket {
    const ticket: Ticket = {
      id: this.nextTicketId++,
      title: data.title,
      description: data.description,
      status: TicketStatus.OPEN,
      priority: data.priority,
      customerId: this.currentUser.id,
      createdAt: new Date().toISOString(),
      deleted: false,
      history: [],
    };
    this.tickets.unshift(ticket);
    this.ticketCreatedSubject.next({ ...ticket });
    return { ...ticket };
  }

  getMessagesForTicket(ticketId: number): Message[] {
    return this.messages.filter((m) => m.ticketId === ticketId).map((m) => ({ ...m }));
  }

  addMessage(ticketId: number, content: string): Message {
    const msg: Message = {
      id: this.nextMessageId++,
      ticketId,
      senderId: this.currentUser.id,
      senderUsername: this.currentUser.username,
      content,
      createdAt: new Date().toISOString(),
    };
    this.messages.push(msg);
    return { ...msg };
  }

  getTicketStats(): { open: number; inProgress: number; resolved: number; total: number } {
    const userTickets = this.getTickets();
    return {
      open: userTickets.filter((t) => t.status === TicketStatus.OPEN).length,
      inProgress: userTickets.filter((t) => t.status === TicketStatus.IN_PROGRESS).length,
      resolved: userTickets.filter((t) => t.status === TicketStatus.RESOLVED).length,
      total: userTickets.length,
    };
  }

  updateUser(data: Partial<User>): User {
    this.currentUser = { ...this.currentUser, ...data };
    return { ...this.currentUser };
  }

  // === Agent-scoped Methods ===

  getAllTickets(): Ticket[] {
    return this.tickets.filter((t) => !t.deleted).map((t) => ({ ...t }));
  }

  getAgentAssignedTickets(agentId: number): Ticket[] {
    return this.tickets.filter((t) => !t.deleted && t.assignedAgentId === agentId).map((t) => ({ ...t }));
  }

  getUnassignedTickets(): Ticket[] {
    return this.tickets.filter((t) => !t.deleted && !t.assignedAgentId && t.status === TicketStatus.OPEN).map((t) => ({ ...t }));
  }

  assignTicket(ticketId: number, agentId: number): Ticket | undefined {
    const ticket = this.tickets.find((t) => t.id === ticketId && !t.deleted);
    if (!ticket) return undefined;
    ticket.assignedAgentId = agentId;
    ticket.assignedAgentName = this.currentUser.username;
    ticket.updatedAt = new Date().toISOString();
    this.ticketUpdatedSubject.next({ ...ticket });
    return { ...ticket };
  }

  updateTicketStatus(ticketId: number, newStatus: TicketStatus): Ticket | undefined {
    const ticket = this.tickets.find((t) => t.id === ticketId && !t.deleted);
    if (!ticket) return undefined;

    const prevStatus = ticket.status;
    if (prevStatus === newStatus) return { ...ticket };

    const historyEntry: TicketStatusHistory = {
      id: this.nextHistoryId++,
      ticketId,
      previousStatus: prevStatus,
      newStatus,
      changedById: this.currentUser.id,
      changedByUsername: this.currentUser.username,
      changedAt: new Date().toISOString(),
    };

    ticket.status = newStatus;
    ticket.updatedAt = new Date().toISOString();
    if (!ticket.history) ticket.history = [];
    ticket.history.push(historyEntry);

    this.ticketUpdatedSubject.next({ ...ticket });
    return { ...ticket };
  }

  getAgentStats(agentId: number): { unassigned: number; myOpen: number; myInProgress: number; myResolved: number; totalAll: number } {
    const allActive = this.tickets.filter((t) => !t.deleted);
    const myTickets = allActive.filter((t) => t.assignedAgentId === agentId);
    return {
      unassigned: allActive.filter((t) => !t.assignedAgentId && t.status === TicketStatus.OPEN).length,
      myOpen: myTickets.filter((t) => t.status === TicketStatus.OPEN).length,
      myInProgress: myTickets.filter((t) => t.status === TicketStatus.IN_PROGRESS).length,
      myResolved: myTickets.filter((t) => t.status === TicketStatus.RESOLVED).length,
      totalAll: allActive.length,
    };
  }

  // === Admin-scoped Methods ===

  getAllUsers(): SystemUser[] {
    return this.systemUsers.map((u) => ({ ...u }));
  }

  getAgentUsers(): SystemUser[] {
    return this.systemUsers.filter((u) => u.role === UserRole.SUPPORT_AGENT).map((u) => ({ ...u }));
  }

  addUser(data: { username: string; email: string; role: UserRole }): SystemUser {
    const user: SystemUser = {
      id: this.nextUserId++,
      username: data.username,
      email: data.email,
      role: data.role,
      createdAt: new Date().toISOString(),
      status: 'active',
    };
    this.systemUsers.push(user);
    // Also add to customerNames if customer
    if (data.role === UserRole.CUSTOMER) {
      this.customerNames[user.id] = user.username;
    }
    return { ...user };
  }

  updateUserRole(userId: number, newRole: UserRole): SystemUser | undefined {
    const user = this.systemUsers.find((u) => u.id === userId);
    if (!user) return undefined;
    user.role = newRole;
    return { ...user };
  }

  toggleUserStatus(userId: number): SystemUser | undefined {
    const user = this.systemUsers.find((u) => u.id === userId);
    if (!user) return undefined;
    user.status = user.status === 'active' ? 'inactive' : 'active';
    return { ...user };
  }

  deleteTicketAdmin(ticketId: number): boolean {
    const ticket = this.tickets.find((t) => t.id === ticketId);
    if (!ticket) return false;
    ticket.deleted = true;
    this.ticketUpdatedSubject.next({ ...ticket });
    return true;
  }

  forceCloseTicket(ticketId: number): Ticket | undefined {
    return this.updateTicketStatus(ticketId, TicketStatus.RESOLVED);
  }

  reassignTicket(ticketId: number, agentId: number, agentName: string): Ticket | undefined {
    const ticket = this.tickets.find((t) => t.id === ticketId && !t.deleted);
    if (!ticket) return undefined;
    ticket.assignedAgentId = agentId;
    ticket.assignedAgentName = agentName;
    ticket.updatedAt = new Date().toISOString();
    this.ticketUpdatedSubject.next({ ...ticket });
    return { ...ticket };
  }

  getSystemStats(): {
    totalTickets: number;
    totalUsers: number;
    openTickets: number;
    inProgressTickets: number;
    resolvedTickets: number;
    avgResolutionHours: number;
    customerCount: number;
    agentCount: number;
  } {
    const allActive = this.tickets.filter((t) => !t.deleted);
    const resolved = allActive.filter((t) => t.status === TicketStatus.RESOLVED);

    // Calculate average resolution time from resolved tickets that have history
    let totalHours = 0;
    let count = 0;
    for (const ticket of resolved) {
      if (ticket.updatedAt) {
        const created = new Date(ticket.createdAt).getTime();
        const updated = new Date(ticket.updatedAt).getTime();
        totalHours += (updated - created) / (1000 * 3600);
        count++;
      }
    }

    return {
      totalTickets: allActive.length,
      totalUsers: this.systemUsers.filter((u) => u.status === 'active').length,
      openTickets: allActive.filter((t) => t.status === TicketStatus.OPEN).length,
      inProgressTickets: allActive.filter((t) => t.status === TicketStatus.IN_PROGRESS).length,
      resolvedTickets: resolved.length,
      avgResolutionHours: count > 0 ? Math.round(totalHours / count) : 0,
      customerCount: this.systemUsers.filter((u) => u.role === UserRole.CUSTOMER && u.status === 'active').length,
      agentCount: this.systemUsers.filter((u) => u.role === UserRole.SUPPORT_AGENT && u.status === 'active').length,
    };
  }

  getAgentPerformance(): Array<{ agentId: number; agentName: string; open: number; inProgress: number; resolved: number; total: number }> {
    const agents = this.systemUsers.filter((u) => u.role === UserRole.SUPPORT_AGENT);
    const allActive = this.tickets.filter((t) => !t.deleted);

    return agents.map((agent) => {
      const agentTickets = allActive.filter((t) => t.assignedAgentId === agent.id);
      return {
        agentId: agent.id,
        agentName: agent.username,
        open: agentTickets.filter((t) => t.status === TicketStatus.OPEN).length,
        inProgress: agentTickets.filter((t) => t.status === TicketStatus.IN_PROGRESS).length,
        resolved: agentTickets.filter((t) => t.status === TicketStatus.RESOLVED).length,
        total: agentTickets.length,
      };
    });
  }

  getRecentActivity(limit: number = 10): Array<{ ticketId: number; ticketTitle: string; previousStatus: TicketStatus; newStatus: TicketStatus; changedBy: string; changedAt: string }> {
    const allHistory: Array<{ ticketId: number; ticketTitle: string; previousStatus: TicketStatus; newStatus: TicketStatus; changedBy: string; changedAt: string }> = [];

    for (const ticket of this.tickets.filter((t) => !t.deleted)) {
      if (ticket.history) {
        for (const h of ticket.history) {
          allHistory.push({
            ticketId: ticket.id,
            ticketTitle: ticket.title,
            previousStatus: h.previousStatus,
            newStatus: h.newStatus,
            changedBy: h.changedByUsername,
            changedAt: h.changedAt,
          });
        }
      }
    }

    return allHistory
      .sort((a, b) => new Date(b.changedAt).getTime() - new Date(a.changedAt).getTime())
      .slice(0, limit);
  }
}
