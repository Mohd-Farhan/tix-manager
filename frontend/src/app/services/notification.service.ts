import { Injectable, inject, signal, computed, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subscription, interval, tap, catchError, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { NotificationItem, UnreadCountResponse } from '../models/notification.model';
import { AuthService } from './auth.service';

/**
 * ==============================================================================================
 * SERVICE: NotificationService (In-App Notifications State & API Client)
 * ==============================================================================================
 * Connects frontend UI components (such as the topbar notification bell) to the backend
 * Observer Pattern notification endpoints. Manages unread badge counters and alerts list.
 */
@Injectable({
  providedIn: 'root',
})
export class NotificationService implements OnDestroy {
  private http = inject(HttpClient);
  private authService = inject(AuthService);

  private readonly apiUrl = `${environment.apiUrl}/api/notifications`;
  private pollSub?: Subscription;

  // Reactive State Signals
  readonly notifications = signal<NotificationItem[]>([]);
  readonly unreadCount = signal<number>(0);
  readonly isLoading = signal<boolean>(false);

  readonly hasUnread = computed(() => this.unreadCount() > 0);

  constructor() {
    this.startPolling();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  /**
   * Starts periodic polling for unread count when user is authenticated.
   */
  startPolling(intervalMs: number = 20000): void {
    this.stopPolling();

    // Immediate check
    if (this.authService.isAuthenticated()) {
      this.fetchUnreadCount().subscribe();
    }

    this.pollSub = interval(intervalMs).subscribe(() => {
      if (this.authService.isAuthenticated()) {
        this.fetchUnreadCount().subscribe();
      }
    });
  }

  stopPolling(): void {
    if (this.pollSub) {
      this.pollSub.unsubscribe();
      this.pollSub = undefined;
    }
  }

  /**
   * Loads full notification list for current user.
   */
  fetchNotifications(): Observable<NotificationItem[]> {
    this.isLoading.set(true);
    return this.http.get<NotificationItem[]>(this.apiUrl).pipe(
      tap((items) => {
        this.notifications.set(items);
        const unread = items.filter((n) => !n.read).length;
        this.unreadCount.set(unread);
        this.isLoading.set(false);
      }),
      catchError((err) => {
        this.isLoading.set(false);
        return of([]);
      })
    );
  }

  /**
   * Checks unread notification count for badge display.
   */
  fetchUnreadCount(): Observable<UnreadCountResponse> {
    return this.http.get<UnreadCountResponse>(`${this.apiUrl}/unread-count`).pipe(
      tap((res) => {
        if (res && typeof res.unreadCount === 'number') {
          this.unreadCount.set(res.unreadCount);
        }
      }),
      catchError(() => of({ unreadCount: 0 }))
    );
  }

  /**
   * Marks a specific notification as read.
   */
  markAsRead(notificationId: number): Observable<NotificationItem | null> {
    // Optimistic UI update
    this.notifications.update((list) =>
      list.map((item) => (item.id === notificationId ? { ...item, read: true } : item))
    );
    this.unreadCount.update((c) => Math.max(0, c - 1));

    return this.http.put<NotificationItem>(`${this.apiUrl}/${notificationId}/read`, {}).pipe(
      catchError(() => of(null))
    );
  }

  /**
   * Marks all notifications as read for current user.
   */
  markAllAsRead(): Observable<void> {
    // Optimistic UI update
    this.notifications.update((list) =>
      list.map((item) => ({ ...item, read: true }))
    );
    this.unreadCount.set(0);

    return this.http.put<void>(`${this.apiUrl}/read-all`, {}).pipe(
      catchError(() => of(undefined))
    );
  }
}
