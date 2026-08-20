import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MockDataService } from '../../../services/mock-data.service';
import { TicketStatus } from '../../../models/ticket.model';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class AdminDashboardComponent implements OnInit {
  private mockData = inject(MockDataService);

  stats = { totalTickets: 0, totalUsers: 0, openTickets: 0, inProgressTickets: 0, resolvedTickets: 0, avgResolutionHours: 0, customerCount: 0, agentCount: 0 };
  agentPerformance: Array<{ agentId: number; agentName: string; open: number; inProgress: number; resolved: number; total: number }> = [];
  recentActivity: Array<{ ticketId: number; ticketTitle: string; previousStatus: TicketStatus; newStatus: TicketStatus; changedBy: string; changedAt: string }> = [];

  ngOnInit(): void {
    this.stats = this.mockData.getSystemStats();
    this.agentPerformance = this.mockData.getAgentPerformance();
    this.recentActivity = this.mockData.getRecentActivity(10);
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
