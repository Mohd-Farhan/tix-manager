import {
  Component,
  inject,
  ElementRef,
  HostListener,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NotificationService } from '../../../services/notification.service';
import { AuthService } from '../../../services/auth.service';
import { NotificationItem, NotificationType } from '../../../models/notification.model';
import { UserRole } from '../../../models/user.model';

/**
 * ==============================================================================================
 * COMPONENT: NotificationBellComponent (In-App Notification Center & Popover)
 * ==============================================================================================
 * Topbar navigation bell with live unread counter badge, glassmorphism dropdown popover,
 * real-time interaction, and deep-linking to relevant ticket records.
 */
@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-bell.component.html',
  styleUrl: './notification-bell.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationBellComponent {
  private notificationService = inject(NotificationService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private elementRef = inject(ElementRef);

  readonly isOpen = signal<boolean>(false);
  readonly unreadCount = this.notificationService.unreadCount;
  readonly notifications = this.notificationService.notifications;
  readonly isLoading = this.notificationService.isLoading;

  /**
   * Closes popover when clicking outside component bounds.
   */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.isOpen.set(false);
    }
  }

  toggleDropdown(event: MouseEvent): void {
    event.stopPropagation();
    const nextState = !this.isOpen();
    this.isOpen.set(nextState);

    if (nextState) {
      this.notificationService.fetchNotifications().subscribe();
    }
  }

  closeDropdown(): void {
    this.isOpen.set(false);
  }

  markAllAsRead(event: MouseEvent): void {
    event.stopPropagation();
    this.notificationService.markAllAsRead().subscribe();
  }

  onNotificationClick(item: NotificationItem): void {
    if (!item.read) {
      this.notificationService.markAsRead(item.id).subscribe();
    }

    this.isOpen.set(false);

    // Navigate to ticket detail based on user role
    const role = this.authService.userRole();
    if (item.ticketId) {
      if (role === UserRole.CUSTOMER) {
        this.router.navigate(['/customer/tickets', item.ticketId]);
      } else if (role === UserRole.SUPPORT_AGENT) {
        this.router.navigate(['/agent/tickets', item.ticketId]);
      } else {
        this.router.navigate(['/admin/tickets']);
      }
    }
  }

  /**
   * Helper formatting ISO date into human-readable relative time string.
   */
  timeAgo(dateStr: string): string {
    if (!dateStr) return '';
    try {
      const now = new Date().getTime();
      const past = new Date(dateStr).getTime();
      const diffSec = Math.floor((now - past) / 1000);

      if (diffSec < 45) return 'Just now';
      if (diffSec < 3600) return `${Math.floor(diffSec / 60)}m ago`;
      if (diffSec < 86400) return `${Math.floor(diffSec / 3600)}h ago`;
      if (diffSec < 604800) return `${Math.floor(diffSec / 86400)}d ago`;

      return new Date(dateStr).toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
      });
    } catch {
      return '';
    }
  }

  getBadgeLabel(type: NotificationType): string {
    switch (type) {
      case NotificationType.TICKET_ASSIGNED:
        return 'Assigned';
      case NotificationType.STATUS_CHANGED:
        return 'Status';
      case NotificationType.NEW_MESSAGE:
        return 'Reply';
      case NotificationType.SLA_BREACH:
        return 'SLA Alert';
      default:
        return 'Alert';
    }
  }
}
