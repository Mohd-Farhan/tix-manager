import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { TicketService } from '../../../services/ticket.service';
import { AuthService } from '../../../services/auth.service';
import { Ticket, TicketStatus, TicketPriority } from '../../../models/ticket.model';
import { PaginationComponent, PageSizeOption } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-ticket-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, PaginationComponent],
  templateUrl: './ticket-list.component.html',
  styleUrl: './ticket-list.component.css',
})
export class TicketListComponent implements OnInit, OnDestroy {
  private ticketService = inject(TicketService);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);
  private subscription = new Subscription();

  allTickets: Ticket[] = [];
  filteredTickets: Ticket[] = [];
  searchQuery = '';
  activeFilter: string = 'ALL';
  sortBy: 'newest' | 'oldest' | 'priority' | 'priority-asc' = 'newest';
  dateFilter: string = 'all';
  customStartDate: string = '';
  customEndDate: string = '';
  isLoading = true;

  dateOptions = [
    { key: 'all', label: 'All Time' },
    { key: '7', label: 'Last 7 Days' },
    { key: '30', label: 'Last 30 Days' },
    { key: '90', label: 'Last 90 Days' },
    { key: 'custom', label: 'Custom Range...' }
  ];

  filters = [
    { key: 'ALL', label: 'All' },
    { key: 'OPEN', label: 'Open' },
    { key: 'IN_PROGRESS', label: 'In Progress' },
    { key: 'RESOLVED', label: 'Resolved' },
  ];

  /* Pagination */
  currentPage = 1;
  pageSize: PageSizeOption = 10;
  readonly pageSizeOptions: PageSizeOption[] = [5, 10, 20, 50, 'ALL'];

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
    this.loadTickets();

    this.subscription.add(
      this.ticketService.ticketCreated$.subscribe(() => {
        this.loadTickets();
      })
    );

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
    const user = this.authService.getCurrentUser();
    if (!user) return;

    this.isLoading = true;
    this.ticketService.getTicketsForUser(user.id).subscribe({
      next: (tickets) => {
        this.isLoading = false;
        this.allTickets = tickets.filter(t => !t.deleted);
        this.applyFilters();
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  openCreateTicketModal(): void {
    this.ticketService.triggerCreateTicketModal();
  }

  onSearch(): void {
    this.currentPage = 1;
    this.applyFilters();
  }

  setFilter(key: string): void {
    this.activeFilter = key;
    this.currentPage = 1;
    this.applyFilters();
  }

  setSort(sort: 'newest' | 'oldest' | 'priority' | 'priority-asc'): void {
    this.sortBy = sort;
    this.applyFilters();
  }

  setDateFilter(val: string): void {
    this.dateFilter = val;
    if (val !== 'custom') {
      this.customStartDate = '';
      this.customEndDate = '';
      this.currentPage = 1;
      this.applyFilters();
    }
  }

  onCustomDateChange(): void {
    // Only apply if both are set (or handle partial, but usually better when both are selected or cleared)
    if ((this.customStartDate && this.customEndDate) || (!this.customStartDate && !this.customEndDate)) {
       this.currentPage = 1;
       this.applyFilters();
    }
  }

  private applyFilters(): void {
    let result = [...this.allTickets];

    // Status filter
    if (this.activeFilter !== 'ALL') {
      result = result.filter((t) => t.status === this.activeFilter);
    }

    // Search
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      result = result.filter((t) => t.title.toLowerCase().includes(q) || t.description.toLowerCase().includes(q));
    }

    // Date filter
    if (this.dateFilter !== 'all') {
      const now = new Date();
      if (this.dateFilter === '7' || this.dateFilter === '30' || this.dateFilter === '90') {
        const days = parseInt(this.dateFilter, 10);
        const cutoff = new Date(now.getTime() - days * 24 * 60 * 60 * 1000);
        result = result.filter(t => new Date(t.createdAt) >= cutoff);
      } else if (this.dateFilter === 'custom' && this.customStartDate && this.customEndDate) {
        const start = new Date(this.customStartDate);
        const end = new Date(this.customEndDate);
        end.setHours(23, 59, 59, 999); // Include the whole end day
        result = result.filter(t => {
          const d = new Date(t.createdAt);
          return d >= start && d <= end;
        });
      }
    }

    // Sort
    if (this.sortBy === 'newest') {
      result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    } else if (this.sortBy === 'oldest') {
      result.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
    } else if (this.sortBy === 'priority') {
      const order = { HIGH: 0, MEDIUM: 1, LOW: 2 };
      result.sort((a, b) => (order[a.priority] ?? 1) - (order[b.priority] ?? 1));
    } else if (this.sortBy === 'priority-asc') {
      const order = { LOW: 0, MEDIUM: 1, HIGH: 2 };
      result.sort((a, b) => (order[a.priority] ?? 1) - (order[b.priority] ?? 1));
    }

    this.filteredTickets = result;
    this.currentPage = 1;
  }

  get pagedTickets(): Ticket[] {
    if (this.pageSize === 'ALL') return this.filteredTickets;
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredTickets.slice(start, start + this.pageSize);
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
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }

  getFilterCount(key: string): number {
    if (key === 'ALL') return this.allTickets.length;
    return this.allTickets.filter((t) => t.status === key).length;
  }
}
