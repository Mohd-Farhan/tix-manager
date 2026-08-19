import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { MockDataService } from '../../../services/mock-data.service';
import { Ticket, TicketStatus, TicketPriority } from '../../../models/ticket.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit, OnDestroy {
  private mockData = inject(MockDataService);
  private subscription = new Subscription();

  stats = { open: 0, inProgress: 0, resolved: 0, total: 0 };
  recentTickets: Ticket[] = [];
  userName = '';

  ngOnInit(): void {
    this.refreshDashboard();

    this.subscription.add(
      this.mockData.ticketCreated$.subscribe(() => {
        this.refreshDashboard();
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  openCreateTicketModal(): void {
    this.mockData.triggerCreateTicket();
  }

  private refreshDashboard(): void {
    this.stats = this.mockData.getTicketStats();
    this.recentTickets = this.mockData.getTickets().slice(0, 5);
    this.userName = this.mockData.getCurrentUser().username;
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
