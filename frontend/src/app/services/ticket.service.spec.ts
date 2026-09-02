import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TicketService } from './ticket.service';
import { Ticket, TicketPriority, TicketStatus } from '../models/ticket.model';
import { environment } from '../../environments/environment';

/**
 * ==============================================================================================
 * AUTOMATED TESTING SUITE: Angular Service Unit Tests for TicketService
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Frontend Enterprise Standards):
 * 1. HTTP Isolation (HttpTestingController): Intercepts outbound HTTP requests from Angular
 *    services and verifies URL, HTTP method, headers, and request body without hitting a backend.
 * 2. Reactive Event Broadcasting: Asserts that reactive subjects (ticketCreated$, ticketUpdated$)
 *    emit new values when ticket actions succeed, notifying subscribing dashboard components.
 * 3. Contract Matching: Validates that frontend TypeScript models match backend REST endpoints.
 */
describe('TicketService', () => {
  let service: TicketService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/api/tickets`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TicketService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(TicketService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify(); // Ensures that there are no outstanding unhandled HTTP requests
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  /**
   * TEST CASE 1: createTicket performs POST request and emits on ticketCreated$ stream.
   */
  it('createTicket — should post ticket payload and emit to ticketCreated$ observable', () => {
    const payload = {
      title: 'Database connection failed',
      description: 'Unable to connect to database in staging environment.',
      priority: TicketPriority.HIGH,
    };

    const mockResponse: Ticket = {
      id: 101,
      title: 'Database connection failed',
      description: 'Unable to connect to database in staging environment.',
      status: TicketStatus.OPEN,
      priority: TicketPriority.HIGH,
      customerId: 1,
      customerUsername: 'customer_dev',
      createdAt: new Date().toISOString(),
      deleted: false,
    };

    let emittedTicket: Ticket | undefined;
    service.ticketCreated$.subscribe((t) => (emittedTicket = t));

    service.createTicket(payload).subscribe((ticket) => {
      expect(ticket).toEqual(mockResponse);
      expect(ticket.status).toBe(TicketStatus.OPEN);
    });

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockResponse);

    expect(emittedTicket).toEqual(mockResponse);
  });

  /**
   * TEST CASE 2: getTicketsForUser performs GET request with userId path variable.
   */
  it('getTicketsForUser — should fetch tickets for a specific user ID', () => {
    const mockTickets: Ticket[] = [
      {
        id: 1,
        title: 'Ticket 1',
        description: 'Desc 1',
        status: TicketStatus.OPEN,
        priority: TicketPriority.LOW,
        customerId: 5,
        createdAt: new Date().toISOString(),
        deleted: false,
      },
    ];

    service.getTicketsForUser(5).subscribe((tickets) => {
      expect(tickets.length).toBe(1);
      expect(tickets[0].id).toBe(1);
    });

    const req = httpMock.expectOne(`${apiUrl}/user/5`);
    expect(req.request.method).toBe('GET');
    req.flush(mockTickets);
  });

  /**
   * TEST CASE 3: assignTicket sends PUT request with agentId query parameter.
   */
  it('assignTicket — should put assignment and notify ticketUpdated$', () => {
    const updatedTicket: Ticket = {
      id: 10,
      title: 'Ticket 10',
      description: 'Desc 10',
      status: TicketStatus.IN_PROGRESS,
      priority: TicketPriority.MEDIUM,
      customerId: 2,
      assignedAgentId: 7,
      createdAt: new Date().toISOString(),
      deleted: false,
    };

    let notifiedTicket: Ticket | undefined;
    service.ticketUpdated$.subscribe((t) => (notifiedTicket = t));

    service.assignTicket(10, 7).subscribe((res) => {
      expect(res.assignedAgentId).toBe(7);
      expect(res.status).toBe(TicketStatus.IN_PROGRESS);
    });

    const req = httpMock.expectOne(`${apiUrl}/10/assign?agentId=7`);
    expect(req.request.method).toBe('PUT');
    req.flush(updatedTicket);

    expect(notifiedTicket).toEqual(updatedTicket);
  });
});
