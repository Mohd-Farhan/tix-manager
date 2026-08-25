import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { TicketService } from '../../../services/ticket.service';
import { AuthService } from '../../../services/auth.service';
import { Ticket, TicketStatus, TicketPriority } from '../../../models/ticket.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit, OnDestroy {
  private ticketService = inject(TicketService);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);
  private subscription = new Subscription();

  stats = { open: 0, inProgress: 0, resolved: 0, total: 0 };
  recentTickets: Ticket[] = [];
  userName = '';
  isLoading = true;

  ngOnInit(): void {
    this.userName = this.authService.getCurrentUser()?.username || 'Customer';
    this.refreshDashboard();

    this.subscription.add(
      this.ticketService.ticketCreated$.subscribe(() => {
        this.refreshDashboard();
      })
    );

    this.subscription.add(
      this.ticketService.ticketUpdated$.subscribe(() => {
        this.refreshDashboard();
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  openCreateTicketModal(): void {
    this.ticketService.triggerCreateTicketModal();
  }

  private refreshDashboard(): void {
    const user = this.authService.getCurrentUser();
    if (!user) return;

    this.ticketService.getTicketsForUser(user.id).subscribe({
      next: (tickets) => {
        this.isLoading = false;
        const activeTickets = tickets.filter(t => !t.deleted);
        this.stats = {
          open: activeTickets.filter(t => t.status === TicketStatus.OPEN).length,
          inProgress: activeTickets.filter(t => t.status === TicketStatus.IN_PROGRESS).length,
          resolved: activeTickets.filter(t => t.status === TicketStatus.RESOLVED).length,
          total: activeTickets.length,
        };
        this.recentTickets = activeTickets
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          .slice(0, 5);
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  getStatusClass(status: TicketStatus): string {
    const map: Record<string, string> = {
      OPEN: 'status-open',
      IN_PROGRESS: 'status-progress',
      RESOLVED: 'status-resolved',
    };
    return map[status] ?? '';
  }

  getStatusLabel(status: TicketStatus): string {
    const map: Record<string, string> = {
      OPEN: 'Open',
      IN_PROGRESS: 'In Progress',
      RESOLVED: 'Resolved',
    };
    return map[status] ?? status;
  }

  getPriorityClass(priority: TicketPriority): string {
    const map: Record<string, string> = {
      LOW: 'priority-low',
      MEDIUM: 'priority-medium',
      HIGH: 'priority-high',
    };
    return map[priority] ?? '';
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }
}
