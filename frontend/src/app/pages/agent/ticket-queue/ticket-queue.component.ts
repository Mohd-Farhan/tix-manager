import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { TicketService } from '../../../services/ticket.service';
import { AuthService } from '../../../services/auth.service';
import { Ticket, TicketStatus, TicketPriority } from '../../../models/ticket.model';
import { PaginationComponent, PageSizeOption } from '../../../shared/components/pagination/pagination.component';
import { SlaBadgeComponent } from '../../../shared';

@Component({
  selector: 'app-agent-ticket-queue',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, PaginationComponent, SlaBadgeComponent],
  templateUrl: './ticket-queue.component.html',
  styleUrl: './ticket-queue.component.css'
})
export class TicketQueueComponent implements OnInit, OnDestroy {
  private ticketService = inject(TicketService);
  private authService = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private subscription = new Subscription();

  tickets: Ticket[] = [];
  filteredTickets: Ticket[] = [];
  currentUserId = 0;
  isLoading = true;

  // Filters
  searchTerm = '';
  statusFilter = 'ALL'; // ALL, OPEN, IN_PROGRESS, RESOLVED
  assignmentFilter = 'ALL'; // ALL, UNASSIGNED, ASSIGNED_TO_ME
  slaFilter = 'ALL'; // ALL, BREACHED, WARNING, OK
  sortBy = 'newest';

  // Pagination state
  currentPage = 1;
  pageSize: PageSizeOption = 10;
  readonly pageSizeOptions: PageSizeOption[] = [10, 20, 50, 100, 'ALL'];

  get paginatedTickets(): Ticket[] {
    if (this.pageSize === 'ALL') return this.filteredTickets;
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredTickets.slice(start, start + this.pageSize);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.cdr.detectChanges();
  }

  onPageSizeChange(size: PageSizeOption): void {
    this.pageSize = size;
    this.currentPage = 1;
    this.cdr.detectChanges();
  }

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    this.currentUserId = user?.id || 0;

    this.loadTickets();

    // Read query params for initial filters
    this.subscription.add(
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
      })
    );

    // Listen for new tickets
    this.subscription.add(
      this.ticketService.ticketCreated$.subscribe(() => {
        this.loadTickets();
      })
    );

    // Listen for ticket updates (assignments, status changes)
    this.subscription.add(
      this.ticketService.ticketUpdated$.subscribe(() => {
        this.loadTickets();
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  loadTickets(): void {
    this.isLoading = true;
    this.ticketService.getAllTickets().subscribe({
      next: (tickets) => {
        this.isLoading = false;
        this.tickets = tickets.filter(t => !t.deleted);
        this.applyFilters();
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
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
          (t.customerUsername && t.customerUsername.toLowerCase().includes(term))
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

    // 4. SLA filter
    if (this.slaFilter !== 'ALL') {
      result = result.filter((t) => {
        if (!t.slaDueAt) return false;
        if (this.slaFilter === 'BREACHED') {
          return t.slaBreached || t.slaStatus === 'BREACHED';
        } else if (this.slaFilter === 'WARNING') {
          return t.slaStatus === 'WARNING';
        } else if (this.slaFilter === 'OK') {
          return t.slaStatus === 'OK';
        }
        return true;
      });
    }

    // 5. Sorting
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
        case 'sla-due-asc':
          if (!a.slaDueAt) return 1;
          if (!b.slaDueAt) return -1;
          return new Date(a.slaDueAt).getTime() - new Date(b.slaDueAt).getTime();
        default:
          return 0;
      }
    });

    this.filteredTickets = result;
    this.currentPage = 1;
  }

  private getPriorityWeight(priority: TicketPriority): number {
    return { LOW: 1, MEDIUM: 2, HIGH: 3 }[priority] ?? 0;
  }

  assignToMe(ticketId: number, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.ticketService.assignTicket(ticketId, this.currentUserId).subscribe({
      next: () => {
        this.loadTickets();
      }
    });
  }

  getCustomerName(customerId: number): string {
    const ticket = this.tickets.find(t => t.customerId === customerId);
    return ticket?.customerUsername ?? `Customer #${customerId}`;
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
