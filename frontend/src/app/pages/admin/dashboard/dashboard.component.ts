import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { TicketService } from '../../../services/ticket.service';
import { UserService } from '../../../services/user.service';
import { Ticket, TicketStatus } from '../../../models/ticket.model';
import { User, UserRole } from '../../../models/user.model';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class AdminDashboardComponent implements OnInit {
  private ticketService = inject(TicketService);
  private userService = inject(UserService);
  private cdr = inject(ChangeDetectorRef);

  stats = { totalTickets: 0, totalUsers: 0, openTickets: 0, inProgressTickets: 0, resolvedTickets: 0, avgResolutionHours: 0, customerCount: 0, agentCount: 0 };
  agentPerformance: Array<{ agentId: number; agentName: string; open: number; inProgress: number; resolved: number; total: number }> = [];
  recentActivity: Array<{ ticketId: number; ticketTitle: string; previousStatus: TicketStatus; newStatus: TicketStatus; changedBy: string; changedAt: string }> = [];
  isLoading = true;

  ngOnInit(): void {
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    this.isLoading = true;
    forkJoin({
      tickets: this.ticketService.getAllTicketsAdmin(),
      users: this.userService.getAllUsersAdmin(),
    }).subscribe({
      next: ({ tickets, users }) => {
        this.isLoading = false;
        const activeTickets = tickets.filter((t) => !t.deleted);
        const resolved = activeTickets.filter((t) => t.status === TicketStatus.RESOLVED);

        let totalHours = 0;
        let count = 0;
        for (const t of resolved) {
          if (t.updatedAt) {
            const created = new Date(t.createdAt).getTime();
            const updated = new Date(t.updatedAt).getTime();
            totalHours += (updated - created) / (1000 * 3600);
            count++;
          }
        }

        this.stats = {
          totalTickets: activeTickets.length,
          totalUsers: users.filter((u) => !u.deleted).length,
          openTickets: activeTickets.filter((t) => t.status === TicketStatus.OPEN).length,
          inProgressTickets: activeTickets.filter((t) => t.status === TicketStatus.IN_PROGRESS).length,
          resolvedTickets: resolved.length,
          avgResolutionHours: count > 0 ? Math.round(totalHours / count) : 0,
          customerCount: users.filter((u) => u.role === UserRole.CUSTOMER && !u.deleted).length,
          agentCount: users.filter((u) => u.role === UserRole.SUPPORT_AGENT && !u.deleted).length,
        };

        const agents = users.filter((u) => u.role === UserRole.SUPPORT_AGENT);
        this.agentPerformance = agents.map((agent) => {
          const agentTickets = activeTickets.filter((t) => t.assignedAgentId === agent.id);
          return {
            agentId: agent.id,
            agentName: agent.username,
            open: agentTickets.filter((t) => t.status === TicketStatus.OPEN).length,
            inProgress: agentTickets.filter((t) => t.status === TicketStatus.IN_PROGRESS).length,
            resolved: agentTickets.filter((t) => t.status === TicketStatus.RESOLVED).length,
            total: agentTickets.length,
          };
        });

        // Derive recent activity from tickets
        this.recentActivity = activeTickets
          .sort((a, b) => new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime())
          .slice(0, 10)
          .map((t) => ({
            ticketId: t.id,
            ticketTitle: t.title,
            previousStatus: t.status === TicketStatus.RESOLVED ? TicketStatus.IN_PROGRESS : TicketStatus.OPEN,
            newStatus: t.status,
            changedBy: t.assignedAgentName || t.customerUsername || 'User',
            changedAt: t.updatedAt || t.createdAt,
          }));

        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  getStatusLabel(status: TicketStatus): string {
    return { OPEN: 'Open', IN_PROGRESS: 'In Progress', RESOLVED: 'Resolved' }[status] ?? status;
  }

  getStatusClass(status: TicketStatus): string {
    return { OPEN: 'status-open', IN_PROGRESS: 'status-progress', RESOLVED: 'status-resolved' }[status] ?? '';
  }

  getBarWidth(value: number): string {
    const max = this.stats.totalTickets || 1;
    return `${(value / max) * 100}%`;
  }

  formatDateTime(dateStr: string): string {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) + ' at ' + d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  }
}
