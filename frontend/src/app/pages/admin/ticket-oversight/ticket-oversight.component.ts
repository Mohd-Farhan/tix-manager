import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { TicketService } from '../../../services/ticket.service';
import { UserService } from '../../../services/user.service';
import { Ticket, TicketStatus, TicketPriority } from '../../../models/ticket.model';
import { User } from '../../../models/user.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { PriorityBadgeComponent } from '../../../shared/components/priority-badge/priority-badge.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { SearchBoxComponent } from '../../../shared/components/search-box/search-box.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { PaginationComponent, PageSizeOption } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast.service';

import { SlaBadgeComponent } from '../../../shared';

@Component({
  selector: 'app-admin-ticket-oversight',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent, PriorityBadgeComponent, EmptyStateComponent, SearchBoxComponent, ConfirmDialogComponent, PaginationComponent, SlaBadgeComponent],
  templateUrl: './ticket-oversight.component.html',
  styleUrl: './ticket-oversight.component.css',
})
export class TicketOversightComponent implements OnInit, OnDestroy {
  private ticketService = inject(TicketService);
  private userService = inject(UserService);
  private cdr = inject(ChangeDetectorRef);
  private toast = inject(ToastService);
  private subscription = new Subscription();

  tickets: Ticket[] = [];
  filteredTickets: Ticket[] = [];
  agents: User[] = [];
  isLoading = true;

  searchTerm = '';
  statusFilter = 'ALL';
  priorityFilter = 'ALL';
  assignmentFilter = 'ALL';
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

  // Confirm delete
  confirmDeleteId: number | null = null;

  ngOnInit(): void {
    this.loadData();

    this.subscription.add(
      this.ticketService.ticketUpdated$.subscribe(() => {
        this.loadData();
      })
    );

    this.subscription.add(
      this.ticketService.ticketCreated$.subscribe(() => {
        this.loadData();
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  loadData(): void {
    this.isLoading = true;
    this.ticketService.getAllTickets().subscribe({
      next: (tickets) => {
        this.tickets = tickets.filter(t => !t.deleted);
        this.applyFilters();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });

    this.userService.getAgents().subscribe({
      next: (agents) => {
        this.agents = agents;
        this.cdr.detectChanges();
      }
    });
  }

  onSearch(): void { this.applyFilters(); }
  onFilterChange(): void { this.applyFilters(); }

  private applyFilters(): void {
    let result = [...this.tickets];

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(
        (t) => t.title.toLowerCase().includes(term) || t.id.toString().includes(term) ||
          (t.customerUsername && t.customerUsername.toLowerCase().includes(term)) ||
          (t.assignedAgentName && t.assignedAgentName.toLowerCase().includes(term))
      );
    }
    if (this.statusFilter !== 'ALL') result = result.filter((t) => t.status === this.statusFilter);
    if (this.priorityFilter !== 'ALL') result = result.filter((t) => t.priority === this.priorityFilter);
    if (this.assignmentFilter === 'UNASSIGNED') result = result.filter((t) => !t.assignedAgentId);
    else if (this.assignmentFilter !== 'ALL') result = result.filter((t) => t.assignedAgentId === +this.assignmentFilter);

    result.sort((a, b) => {
      switch (this.sortBy) {
        case 'newest': return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
        case 'oldest': return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
        case 'priority-desc': return this.getPriorityWeight(b.priority) - this.getPriorityWeight(a.priority);
        case 'priority-asc': return this.getPriorityWeight(a.priority) - this.getPriorityWeight(b.priority);
        default: return 0;
      }
    });
    this.filteredTickets = result;
    this.currentPage = 1;
  }

  private getPriorityWeight(p: TicketPriority): number { return { LOW: 1, MEDIUM: 2, HIGH: 3 }[p] ?? 0; }

  reassignTicket(ticketId: number, agentId: string): void {
    const id = +agentId;
    if (!id) return;

    this.ticketService.assignTicket(ticketId, id).subscribe({
      next: () => {
        this.toast.success(`Ticket #${ticketId} assigned successfully.`);
        this.loadData();
      },
      error: () => {
        this.toast.error(`Failed to assign ticket #${ticketId}.`);
      }
    });
  }

  forceClose(ticketId: number, event: Event): void {
    event.stopPropagation();
    this.ticketService.updateTicketStatus(ticketId, TicketStatus.RESOLVED).subscribe({
      next: () => {
        this.toast.success(`Ticket #${ticketId} has been force-closed.`);
        this.loadData();
      },
      error: () => {
        this.toast.error(`Failed to close ticket #${ticketId}.`);
      }
    });
  }

  confirmDelete(ticketId: number, event: Event): void {
    event.stopPropagation();
    this.confirmDeleteId = ticketId;
    this.cdr.detectChanges();
  }

  executeDelete(): void {
    if (this.confirmDeleteId) {
      const id = this.confirmDeleteId;
      this.ticketService.softDeleteTicket(id).subscribe({
        next: () => {
          this.confirmDeleteId = null;
          this.toast.success(`Ticket #${id} deleted successfully.`);
          this.loadData();
        },
        error: () => {
          this.confirmDeleteId = null;
          this.toast.error(`Failed to delete ticket #${id}.`);
        }
      });
    }
  }

  cancelDelete(): void {
    this.confirmDeleteId = null;
    this.cdr.detectChanges();
  }

  getCustomerName(customerId: number): string {
    const ticket = this.tickets.find(t => t.customerId === customerId);
    return ticket?.customerUsername ?? `Customer #${customerId}`;
  }

  getStatusClass(s: TicketStatus): string { return { OPEN: 'status-open', IN_PROGRESS: 'status-progress', RESOLVED: 'status-resolved' }[s] ?? ''; }
  getStatusLabel(s: TicketStatus): string { return { OPEN: 'Open', IN_PROGRESS: 'In Progress', RESOLVED: 'Resolved' }[s] ?? s; }
  getPriorityClass(p: TicketPriority): string { return { LOW: 'priority-low', MEDIUM: 'priority-medium', HIGH: 'priority-high' }[p] ?? ''; }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }
}

