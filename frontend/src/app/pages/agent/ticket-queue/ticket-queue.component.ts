import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { MockDataService } from '../../../services/mock-data.service';
import { Ticket, TicketStatus, TicketPriority } from '../../../models/ticket.model';

@Component({
  selector: 'app-agent-ticket-queue',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './ticket-queue.component.html',
  styleUrl: './ticket-queue.component.css'
})
export class TicketQueueComponent implements OnInit {
  private mockData = inject(MockDataService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  tickets: Ticket[] = [];
  filteredTickets: Ticket[] = [];
  currentUserId = 0;

  // Filters
  searchTerm = '';
  statusFilter = 'ALL'; // ALL, OPEN, IN_PROGRESS, RESOLVED
  assignmentFilter = 'ALL'; // ALL, UNASSIGNED, ASSIGNED_TO_ME
  sortBy = 'newest';

  ngOnInit(): void {
    this.currentUserId = this.mockData.getCurrentUser().id;
    this.tickets = this.mockData.getAllTickets();
    
    // Read query params for initial filters
    this.route.queryParams.subscribe(params => {
      const filter = params['filter'];
      if (filter === 'assigned') {
        this.assignmentFilter = 'ASSIGNED_TO_ME';
      } else if (filter === 'unassigned') {
        this.assignmentFilter = 'UNASSIGNED';
        this.statusFilter = 'OPEN';
      } else if (filter === 'my-open') {
        this.assignmentFilter = 'ASSIGNED_TO_ME';
        this.statusFilter = 'OPEN';
      } else if (filter === 'my-progress') {
        this.assignmentFilter = 'ASSIGNED_TO_ME';
        this.statusFilter = 'IN_PROGRESS';
      } else if (filter === 'my-resolved') {
        this.assignmentFilter = 'ASSIGNED_TO_ME';
        this.statusFilter = 'RESOLVED';
      }
      this.applyFilters();
    });

    // Listen for new tickets
    this.mockData.ticketCreated$.subscribe(() => {
      this.tickets = this.mockData.getAllTickets();
      this.applyFilters();
    });

    // Listen for ticket updates (assignments, status changes)
    this.mockData.ticketUpdated$.subscribe(() => {
      this.tickets = this.mockData.getAllTickets();
      this.applyFilters();
    });
  }

  onSearch(): void {
    this.applyFilters();
  }

  onFilterChange(): void {
    // Clear query params when user manually changes filters
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {},
      replaceUrl: true
    });
    this.applyFilters();
  }

  private applyFilters(): void {
    let result = [...this.tickets];

    // 1. Search filter
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(
        (t) =>
          t.title.toLowerCase().includes(term) ||
          t.id.toString().includes(term) ||
          (t.assignedAgentName && t.assignedAgentName.toLowerCase().includes(term)) ||
          this.mockData.getCustomerNameById(t.customerId).toLowerCase().includes(term)
      );
    }

    // 2. Status filter
    if (this.statusFilter !== 'ALL') {
      result = result.filter((t) => t.status === this.statusFilter);
    }

    // 3. Assignment filter
    if (this.assignmentFilter === 'UNASSIGNED') {
      result = result.filter((t) => !t.assignedAgentId);
    } else if (this.assignmentFilter === 'ASSIGNED_TO_ME') {
      result = result.filter((t) => t.assignedAgentId === this.currentUserId);
    }

    // 4. Sorting
    result.sort((a, b) => {
      switch (this.sortBy) {
        case 'newest':
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
        case 'oldest':
          return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
        case 'priority-desc':
          return this.getPriorityWeight(b.priority) - this.getPriorityWeight(a.priority);
        case 'priority-asc':
          return this.getPriorityWeight(a.priority) - this.getPriorityWeight(b.priority);
        case 'recently-updated':
          return new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime();
        default:
          return 0;
      }
    });

    this.filteredTickets = result;
  }

  private getPriorityWeight(priority: TicketPriority): number {
    return { LOW: 1, MEDIUM: 2, HIGH: 3 }[priority] ?? 0;
  }

  assignToMe(ticketId: number, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.mockData.assignTicket(ticketId, this.currentUserId);
  }

  getCustomerName(customerId: number): string {
    return this.mockData.getCustomerNameById(customerId);
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
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }
}
