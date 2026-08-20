import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MockDataService } from '../../../services/mock-data.service';
import { Ticket, TicketPriority, TicketStatus } from '../../../models/ticket.model';
import { User } from '../../../models/user.model';

@Component({
  selector: 'app-agent-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private mockData = inject(MockDataService);
  
  user!: User;
  stats = { unassigned: 0, myOpen: 0, myInProgress: 0, myResolved: 0, totalAll: 0 };
  unassignedTickets: Ticket[] = [];
  myRecentTickets: Ticket[] = [];

  ngOnInit(): void {
    this.user = this.mockData.getCurrentUser();
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    this.stats = this.mockData.getAgentStats(this.user.id);
    
    // Get up to 5 unassigned tickets for quick action
    this.unassignedTickets = this.mockData.getUnassignedTickets()
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 5);

    // Get up to 5 of my recent open/in-progress tickets
    this.myRecentTickets = this.mockData.getAgentAssignedTickets(this.user.id)
      .filter(t => t.status !== TicketStatus.RESOLVED)
      .sort((a, b) => new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime())
      .slice(0, 5);
  }

  assignToMe(ticketId: number, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.mockData.assignTicket(ticketId, this.user.id);
    this.loadDashboardData(); // Refresh lists
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
