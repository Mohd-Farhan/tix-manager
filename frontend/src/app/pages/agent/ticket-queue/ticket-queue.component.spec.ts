import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TicketQueueComponent } from './ticket-queue.component';
import { TicketService } from '../../../services/ticket.service';
import { AuthService } from '../../../services/auth.service';
import { provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { Ticket, TicketStatus, TicketPriority } from '../../../models/ticket.model';
import { UserRole } from '../../../models/user.model';
import { describe, it, expect, vi, beforeEach } from 'vitest';

describe('TicketQueueComponent (Skeleton Loading)', () => {
  let component: TicketQueueComponent;
  let fixture: ComponentFixture<TicketQueueComponent>;
  let ticketsSubject: Subject<Ticket[]>;
  let ticketServiceSpy: {
    getAllTickets: ReturnType<typeof vi.fn>;
    ticketCreated$: Subject<void>;
    ticketUpdated$: Subject<void>;
  };
  let authServiceSpy: { getCurrentUser: ReturnType<typeof vi.fn> };

  const mockTickets: Ticket[] = [
    {
      id: 101,
      title: 'Database connection timeouts',
      description: 'Production DB query timing out under load',
      status: TicketStatus.OPEN,
      priority: TicketPriority.HIGH,
      customerId: 1,
      assignedAgentId: 2,
      slaDueAt: new Date(Date.now() + 3600000).toISOString(),
      slaBreached: false,
      createdAt: '2026-09-01T10:00:00Z',
      deleted: false,
    },
    {
      id: 102,
      title: 'UI typo on billing page',
      description: 'Minor wording issue in subscription card',
      status: TicketStatus.RESOLVED,
      priority: TicketPriority.LOW,
      customerId: 3,
      slaDueAt: new Date(Date.now() + 7200000).toISOString(),
      slaBreached: false,
      createdAt: '2026-09-02T11:00:00Z',
      deleted: false,
    },
  ];

  beforeEach(async () => {
    ticketsSubject = new Subject<Ticket[]>();
    ticketServiceSpy = {
      getAllTickets: vi.fn().mockReturnValue(ticketsSubject),
      ticketCreated$: new Subject<void>(),
      ticketUpdated$: new Subject<void>(),
    };
    authServiceSpy = {
      getCurrentUser: vi.fn().mockReturnValue({
        id: 2,
        username: 'agent_smith',
        email: 'smith@support.com',
        role: UserRole.SUPPORT_AGENT,
      }),
    };

    await TestBed.configureTestingModule({
      imports: [TicketQueueComponent],
      providers: [
        provideRouter([]),
        { provide: TicketService, useValue: ticketServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TicketQueueComponent);
    component = fixture.componentInstance;
  });

  it('should create the TicketQueueComponent', () => {
    expect(component).toBeTruthy();
  });

  it('should render shimmer skeleton table rows when isLoading is true', () => {
    fixture.detectChanges(); // ngOnInit triggers loadTickets with pending ticketsSubject

    expect(component.isLoading).toBe(true);
    const skeletonTable = fixture.nativeElement.querySelector('.skeleton-table');
    const skeletonRows = fixture.nativeElement.querySelectorAll('.skeleton-row');
    const emptyState = fixture.nativeElement.querySelector('.empty-state');

    expect(skeletonTable).toBeTruthy();
    expect(skeletonRows.length).toBe(6);
    expect(emptyState).toBeNull();
  });

  it('should render actual ticket rows when isLoading is false', () => {
    fixture.detectChanges();
    ticketsSubject.next(mockTickets);
    fixture.detectChanges();

    expect(component.isLoading).toBe(false);
    const skeletonTable = fixture.nativeElement.querySelector('.skeleton-table');
    const ticketRows = fixture.nativeElement.querySelectorAll('.ticket-row:not(.skeleton-row)');

    expect(skeletonTable).toBeNull();
    expect(ticketRows.length).toBe(2);
  });

  it('should display empty state only when isLoading is false and no tickets match', () => {
    fixture.detectChanges();
    ticketsSubject.next([]);
    fixture.detectChanges();

    expect(component.isLoading).toBe(false);
    const skeletonTable = fixture.nativeElement.querySelector('.skeleton-table');
    const emptyState = fixture.nativeElement.querySelector('.empty-state');

    expect(skeletonTable).toBeNull();
    expect(emptyState).toBeTruthy();
  });
});
