import { TestBed, ComponentFixture } from '@angular/core/testing';
import { SlaBadgeComponent } from './sla-badge.component';
import { Ticket, TicketPriority, TicketStatus } from '../../../models/ticket.model';

describe('SlaBadgeComponent', () => {
  let component: SlaBadgeComponent;
  let fixture: ComponentFixture<SlaBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SlaBadgeComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SlaBadgeComponent);
    component = fixture.componentInstance;
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should render nothing if ticket is not provided or has no slaDueAt', () => {
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.sla-badge');
    expect(badge).toBeNull();
  });

  it('should render SLA Met for a resolved ticket that met SLA', () => {
    component.ticket = {
      id: 1,
      title: 'Resolved on time',
      description: 'Test',
      status: TicketStatus.RESOLVED,
      priority: TicketPriority.LOW,
      customerId: 1,
      createdAt: new Date().toISOString(),
      slaDueAt: new Date(Date.now() + 3600000).toISOString(),
      slaBreached: false,
      deleted: false,
    };

    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.sla-badge');
    expect(badge).toBeTruthy();
    expect(badge.classList.contains('sla-resolved-met')).toBe(true);
    expect(component.displayText).toBe('SLA Met');
  });

  it('should render SLA Breached for a resolved ticket that breached SLA', () => {
    component.ticket = {
      id: 2,
      title: 'Resolved late',
      description: 'Test',
      status: TicketStatus.RESOLVED,
      priority: TicketPriority.HIGH,
      customerId: 1,
      createdAt: new Date().toISOString(),
      slaDueAt: new Date(Date.now() - 3600000).toISOString(),
      slaBreached: true,
      deleted: false,
    };

    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.sla-badge');
    expect(badge).toBeTruthy();
    expect(badge.classList.contains('sla-resolved-breached')).toBe(true);
    expect(component.displayText).toBe('SLA Breached');
  });

  it('should render warning badge when remaining time is less than 2 hours', () => {
    const fortyFiveMinutesFromNow = new Date(Date.now() + 45 * 60 * 1000).toISOString();
    component.ticket = {
      id: 3,
      title: 'Near breach',
      description: 'Test',
      status: TicketStatus.IN_PROGRESS,
      priority: TicketPriority.MEDIUM,
      customerId: 1,
      createdAt: new Date().toISOString(),
      slaDueAt: fortyFiveMinutesFromNow,
      slaBreached: false,
      deleted: false,
    };

    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.sla-badge');
    expect(badge).toBeTruthy();
    expect(badge.classList.contains('sla-warning')).toBe(true);
    expect(component.displayText).toContain('left');
  });

  it('should render breached badge and escalated pill when ticket is escalated', () => {
    const pastTime = new Date(Date.now() - 3600 * 1000).toISOString();
    component.ticket = {
      id: 4,
      title: 'Breached ticket',
      description: 'Test',
      status: TicketStatus.OPEN,
      priority: TicketPriority.HIGH,
      customerId: 1,
      createdAt: new Date().toISOString(),
      slaDueAt: pastTime,
      slaBreached: true,
      escalated: true,
      deleted: false,
    };

    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.sla-badge');
    expect(badge).toBeTruthy();
    expect(badge.classList.contains('sla-breached')).toBe(true);
    expect(component.displayText).toContain('Breached by');

    const escalatedPill = fixture.nativeElement.querySelector('.escalated-pill');
    expect(escalatedPill).toBeTruthy();
    expect(escalatedPill.textContent.trim()).toBe('Escalated');
  });

  it('should render deadline text when showDeadline is true', () => {
    const futureTime = new Date(Date.now() + 24 * 3600 * 1000).toISOString();
    component.ticket = {
      id: 5,
      title: 'Future ticket',
      description: 'Test',
      status: TicketStatus.OPEN,
      priority: TicketPriority.LOW,
      customerId: 1,
      createdAt: new Date().toISOString(),
      slaDueAt: futureTime,
      slaBreached: false,
      deleted: false,
    };
    component.showDeadline = true;

    fixture.detectChanges();
    const deadline = fixture.nativeElement.querySelector('.sla-deadline');
    expect(deadline).toBeTruthy();
    expect(deadline.textContent).toContain('Due:');
  });
});
