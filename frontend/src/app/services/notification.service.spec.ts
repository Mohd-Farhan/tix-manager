import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { NotificationService } from './notification.service';
import { AuthService } from './auth.service';
import { NotificationItem, NotificationType } from '../models/notification.model';
import { environment } from '../../environments/environment';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;

  const mockNotifications: NotificationItem[] = [
    {
      id: 1,
      title: 'Ticket Assigned: #100',
      message: 'You were assigned to ticket',
      type: NotificationType.TICKET_ASSIGNED,
      ticketId: 100,
      read: false,
      createdAt: new Date().toISOString(),
    },
    {
      id: 2,
      title: 'Status Updated: #100',
      message: 'Ticket status is now RESOLVED',
      type: NotificationType.STATUS_CHANGED,
      ticketId: 100,
      read: true,
      createdAt: new Date().toISOString(),
    },
  ];

  beforeEach(() => {
    const authServiceStub = {
      isAuthenticated: () => false,
      userRole: () => null,
    };

    TestBed.configureTestingModule({
      providers: [
        NotificationService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceStub },
      ],
    });

    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    service.stopPolling();
  });

  it('should be created with initial state', () => {
    expect(service).toBeTruthy();
    expect(service.notifications()).toEqual([]);
    expect(service.unreadCount()).toBe(0);
    expect(service.hasUnread()).toBe(false);
  });

  it('should fetch notifications and update signals', () => {
    service.fetchNotifications().subscribe((items) => {
      expect(items.length).toBe(2);
      expect(service.notifications().length).toBe(2);
      expect(service.unreadCount()).toBe(1);
      expect(service.hasUnread()).toBe(true);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/notifications`);
    expect(req.request.method).toBe('GET');
    req.flush(mockNotifications);
  });

  it('should fetch unread count and update unreadCount signal', () => {
    service.fetchUnreadCount().subscribe((res) => {
      expect(res.unreadCount).toBe(5);
      expect(service.unreadCount()).toBe(5);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/notifications/unread-count`);
    expect(req.request.method).toBe('GET');
    req.flush({ unreadCount: 5 });
  });

  it('should mark single notification as read optimistically', () => {
    service.notifications.set([...mockNotifications]);
    service.unreadCount.set(1);

    service.markAsRead(1).subscribe();

    expect(service.notifications().find((n) => n.id === 1)?.read).toBe(true);
    expect(service.unreadCount()).toBe(0);

    const req = httpMock.expectOne(`${environment.apiUrl}/api/notifications/1/read`);
    expect(req.request.method).toBe('PUT');
    req.flush({ ...mockNotifications[0], read: true });
  });

  it('should mark all notifications as read optimistically', () => {
    service.notifications.set([...mockNotifications]);
    service.unreadCount.set(1);

    service.markAllAsRead().subscribe();

    expect(service.notifications().every((n) => n.read)).toBe(true);
    expect(service.unreadCount()).toBe(0);

    const req = httpMock.expectOne(`${environment.apiUrl}/api/notifications/read-all`);
    expect(req.request.method).toBe('PUT');
    req.flush(null);
  });
});
