import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TicketService } from '../../../services/ticket.service';
import { AuthService } from '../../../services/auth.service';
import { Ticket, TicketPriority, TicketStatus } from '../../../models/ticket.model';
import { User, UserRole } from '../../../models/user.model';

@Component({
  selector: 'app-agent-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private ticketService = inject(TicketService);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);
  
  user: User = this.authService.getCurrentUser() || {
    id: 2,
    username: 'Agent',
    email: '',
    role: UserRole.SUPPORT_AGENT
  };
  stats = { unassigned: 0, myOpen: 0, myInProgress: 0, myResolved: 0, totalAll: 0 };
  slaMetrics: import('../../../models/ticket.model').SlaMetrics | null = null;
  unassignedTickets: Ticket[] = [];
  myRecentTickets: Ticket[] = [];
  isLoading = true;

  ngOnInit(): void {
    const u = this.authService.getCurrentUser();
    if (u) this.user = u;
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    this.isLoading = true;
    this.ticketService.getSlaMetrics().subscribe({
      next: (metrics) => {
        this.slaMetrics = metrics;
        this.cdr.detectChanges();
      }
    });

    this.ticketService.getAllTickets().subscribe({
      next: (tickets) => {
        this.isLoading = false;
        const allActive = tickets.filter(t => !t.deleted);
        const myTickets = allActive.filter(t => t.assignedAgentId === this.user.id);

        this.stats = {
          unassigned: allActive.filter(t => !t.assignedAgentId && t.status === TicketStatus.OPEN).length,
          myOpen: myTickets.filter(t => t.status === TicketStatus.OPEN).length,
          myInProgress: myTickets.filter(t => t.status === TicketStatus.IN_PROGRESS).length,
          myResolved: myTickets.filter(t => t.status === TicketStatus.RESOLVED).length,
          totalAll: allActive.length,
        };

        // Unassigned tickets
        this.unassignedTickets = allActive
          .filter(t => !t.assignedAgentId && t.status === TicketStatus.OPEN)
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          .slice(0, 5);

        // My recent non-resolved tickets
        this.myRecentTickets = myTickets
          .filter(t => t.status !== TicketStatus.RESOLVED)
          .sort((a, b) => new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime())
          .slice(0, 5);

        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  assignToMe(ticketId: number, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.ticketService.assignTicket(ticketId, this.user.id).subscribe({
      next: () => {
        this.loadDashboardData();
      }
    });
  }

  getStatusClass(status: TicketStatus): string {
    return { OPEN: 'status-open', IN_PROGRESS: 'status-progress', RESOLVED: 'status-resolved' }[status] ?? '';
  }

  getStatusLabel(status: TicketStatus): string {
    return { OPEN: 'Open', IN_PROGRESS: 'In Progress', RESOLVED: 'Resolved' }[status] ?? status;
  }

  getPriorityClass(priority: TicketPriority): string {
    return { LOW: 'priority-low', MEDIUM: 'priority-medium', HIGH: 'priority-high' }[priority] ?? '';
  }

  formatDate(dateStr: string): string {
    const d = new Date(dateStr);
    const now = new Date();
    const diffDays = Math.floor((now.getTime() - d.getTime()) / (1000 * 3600 * 24));
    
    if (diffDays === 0) {
      return d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    } else if (diffDays === 1) {
      return 'Yesterday';
    } else if (diffDays < 7) {
      return `${diffDays} days ago`;
    }
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }
}
