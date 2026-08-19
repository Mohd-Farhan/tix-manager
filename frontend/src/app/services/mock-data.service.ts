import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { Ticket, TicketStatus, TicketPriority, TicketStatusHistory } from '../models/ticket.model';
import { Message } from '../models/message.model';
import { User, UserRole } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class MockDataService {
  private currentUser: User = {
    id: 1,
    username: 'farhan_dev',
    email: 'farhan@example.com',
    role: UserRole.CUSTOMER,
    createdAt: '2025-06-01T10:00:00',
  };

  private tickets: Ticket[] = [
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
  ];

  private messages: Message[] = [
    { id: 1, ticketId: 1001, senderId: 1, senderUsername: 'farhan_dev', content: 'Hi, I am unable to log in after resetting my password. I get a 403 Forbidden error every time.', createdAt: '2026-08-15T09:30:00' },
    { id: 2, ticketId: 1001, senderId: 10, senderUsername: 'Priya Sharma', content: 'Hello Farhan, thank you for reporting this. I can see the issue in our logs. It appears the password reset token was not properly invalidated. I am working on a fix now.', createdAt: '2026-08-15T11:15:00' },
    { id: 3, ticketId: 1001, senderId: 1, senderUsername: 'farhan_dev', content: 'Thank you for the quick response! Let me know if you need any additional information from my end.', createdAt: '2026-08-15T11:30:00' },
    { id: 4, ticketId: 1001, senderId: 10, senderUsername: 'Priya Sharma', content: 'I have pushed a fix to staging. Could you please try logging in again and let me know if the issue persists?', createdAt: '2026-08-16T14:22:00' },
    { id: 5, ticketId: 1003, senderId: 1, senderUsername: 'farhan_dev', content: 'I would like to upgrade to the Professional plan. What are the steps?', createdAt: '2026-08-10T08:15:00' },
    { id: 6, ticketId: 1003, senderId: 11, senderUsername: 'Rahul Verma', content: 'Hi Farhan! I have processed your upgrade request. Your plan has been upgraded to Professional with prorated billing. You should see the changes reflected in your dashboard within the next hour.', createdAt: '2026-08-12T16:30:00' },
  ];

  private nextTicketId = 1007;
  private nextMessageId = 7;

  // Reactive events
  private openCreateTicketSubject = new Subject<void>();
  openCreateTicket$ = this.openCreateTicketSubject.asObservable();

  private ticketCreatedSubject = new Subject<Ticket>();
  ticketCreated$ = this.ticketCreatedSubject.asObservable();

  triggerCreateTicket(): void {
    this.openCreateTicketSubject.next();
  }

  getCurrentUser(): User {
    return { ...this.currentUser };
  }

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
}
