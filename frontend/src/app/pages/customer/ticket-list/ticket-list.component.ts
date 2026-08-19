import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { MockDataService } from '../../../services/mock-data.service';
import { Ticket, TicketStatus, TicketPriority } from '../../../models/ticket.model';

@Component({
  selector: 'app-ticket-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './ticket-list.component.html',
  styleUrl: './ticket-list.component.css',
})
export class TicketListComponent implements OnInit, OnDestroy {
  private mockData = inject(MockDataService);
  private subscription = new Subscription();

  allTickets: Ticket[] = [];
  filteredTickets: Ticket[] = [];
  searchQuery = '';
  activeFilter: string = 'ALL';
  sortBy: 'newest' | 'oldest' | 'priority' = 'newest';

  filters = [
    { key: 'ALL', label: 'All' },
    { key: 'OPEN', label: 'Open' },
    { key: 'IN_PROGRESS', label: 'In Progress' },
    { key: 'RESOLVED', label: 'Resolved' },
  ];

  /* Pagination */
  page = 1;
  pageSize = 5;

  ngOnInit(): void {
    this.allTickets = this.mockData.getTickets();
    this.applyFilters();

    this.subscription.add(
      this.mockData.ticketCreated$.subscribe(() => {
        this.allTickets = this.mockData.getTickets();
        this.applyFilters();
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  openCreateTicketModal(): void {
    this.mockData.triggerCreateTicket();
  }

  onSearch(): void {
    this.page = 1;
    this.applyFilters();
  }

  setFilter(key: string): void {
    this.activeFilter = key;
    this.page = 1;
    this.applyFilters();
  }

  setSort(sort: 'newest' | 'oldest' | 'priority'): void {
    this.sortBy = sort;
    this.applyFilters();
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

    // Sort
    if (this.sortBy === 'newest') {
      result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    } else if (this.sortBy === 'oldest') {
      result.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
    } else if (this.sortBy === 'priority') {
      const order = { HIGH: 0, MEDIUM: 1, LOW: 2 };
      result.sort((a, b) => (order[a.priority] ?? 1) - (order[b.priority] ?? 1));
    }

    this.filteredTickets = result;
  }

  get pagedTickets(): Ticket[] {
    const start = (this.page - 1) * this.pageSize;
    return this.filteredTickets.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredTickets.length / this.pageSize));
  }

  prevPage(): void {
    if (this.page > 1) this.page--;
  }

  nextPage(): void {
    if (this.page < this.totalPages) this.page++;
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
