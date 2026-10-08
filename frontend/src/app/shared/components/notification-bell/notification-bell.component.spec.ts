import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NotificationBellComponent } from './notification-bell.component';
import { NotificationService } from '../../../services/notification.service';
import { AuthService } from '../../../services/auth.service';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { signal } from '@angular/core';
import { NotificationItem, NotificationType } from '../../../models/notification.model';
import { UserRole } from '../../../models/user.model';

describe('NotificationBellComponent', () => {
  let component: NotificationBellComponent;
  let fixture: ComponentFixture<NotificationBellComponent>;

  const notificationsSignal = signal<NotificationItem[]>([
    {
      id: 1,
      title: 'Ticket Assigned: #100',
      message: 'You have been assigned to ticket',
      type: NotificationType.TICKET_ASSIGNED,
      ticketId: 100,
      read: false,
      createdAt: new Date().toISOString(),
    },
  ]);
  const unreadCountSignal = signal<number>(1);
  const isLoadingSignal = signal<boolean>(false);

  const mockNotificationService = {
    notifications: notificationsSignal,
    unreadCount: unreadCountSignal,
    isLoading: isLoadingSignal,
    fetchNotifications: vi.fn().mockReturnValue(of([])),
    markAsRead: vi.fn().mockReturnValue(of(null)),
    markAllAsRead: vi.fn().mockReturnValue(of(undefined)),
  };

  const mockAuthService = {
    userRole: signal<UserRole | null>(UserRole.SUPPORT_AGENT),
  };

  const mockRouter = {
    navigate: vi.fn(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationBellComponent],
      providers: [
        { provide: NotificationService, useValue: mockNotificationService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationBellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create notification bell component', () => {
    expect(component).toBeTruthy();
    expect(component.unreadCount()).toBe(1);
  });

  it('should toggle dropdown open and fetch notifications', () => {
    expect(component.isOpen()).toBe(false);

    const event = new MouseEvent('click');
    component.toggleDropdown(event);

    expect(component.isOpen()).toBe(true);
    expect(mockNotificationService.fetchNotifications).toHaveBeenCalled();
  });

  it('should close dropdown when closeDropdown is called', () => {
    component.isOpen.set(true);
    component.closeDropdown();
    expect(component.isOpen()).toBe(false);
  });

  it('should navigate to ticket and mark as read on notification click', () => {
    const item = notificationsSignal()[0];
    component.isOpen.set(true);

    component.onNotificationClick(item);

    expect(mockNotificationService.markAsRead).toHaveBeenCalledWith(1);
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/agent/tickets', 100]);
    expect(component.isOpen()).toBe(false);
  });

  it('should mark all notifications as read when markAllAsRead is triggered', () => {
    const event = new MouseEvent('click');
    component.markAllAsRead(event);

    expect(mockNotificationService.markAllAsRead).toHaveBeenCalled();
  });
});
