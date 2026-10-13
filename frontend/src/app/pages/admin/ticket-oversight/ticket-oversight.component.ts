import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { TicketService } from '../../../services/ticket.service';
import { UserService } from '../../../services/user.service';
import { Ticket, TicketStatus, TicketPriority, RoutingStrategyType } from '../../../models/ticket.model';
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
  readonly skeletonRows = [1, 2, 3, 4, 5, 6];

  searchTerm = '';
  statusFilter = 'ALL';
  priorityFilter = 'ALL';
  assignmentFilter = 'ALL';
  sortColumn: string = 'createdAt';
  sortDirection: 'asc' | 'desc' = 'desc';

  // Strategy Pattern state (Admin & System Admin routing)
  selectedStrategy: RoutingStrategyType = RoutingStrategyType.WORKLOAD_BALANCED;
  autoAssigningId: number | null = null;
  isBatchExecuting = false;

  // Batch action state
  selectedBatchAction: 'AUTO_ASSIGN_ALL' | 'AUTO_ASSIGN_SELECTED' | 'UNASSIGN_SELECTED' | 'ASSIGN_TO_AGENT' = 'AUTO_ASSIGN_ALL';
  selectedBatchAgentId: number | null = null;
  selectedTicketIds = new Set<number>();

  readonly strategies = [
    { value: RoutingStrategyType.WORKLOAD_BALANCED, label: 'Workload Balanced', desc: 'Least busy agent' },
    { value: RoutingStrategyType.ROUND_ROBIN, label: 'Round Robin', desc: 'Circular fair rotation' },
    { value: RoutingStrategyType.PRIORITY_BASED, label: 'Priority Based', desc: 'SLA fast-lane for HIGH' },
  ];

  get unassignedCount(): number {
    return this.tickets.filter(t => !t.assignedAgentId && t.status !== TicketStatus.RESOLVED).length;
  }

  get activeTicketsCount(): number {
    return this.tickets.filter(t => t.status !== TicketStatus.RESOLVED).length;
  }

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
        const validIds = new Set(this.tickets.map(t => t.id));
        this.selectedTicketIds = new Set([...this.selectedTicketIds].filter(id => validIds.has(id)));
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
      const dir = this.sortDirection === 'asc' ? 1 : -1;
      switch (this.sortColumn) {
        case 'id':
          return (a.id - b.id) * dir;
        case 'title':
          return (a.title || '').localeCompare(b.title || '') * dir;
        case 'customer': {
          const cA = this.getCustomerName(a.customerId) || '';
          const cB = this.getCustomerName(b.customerId) || '';
          return cA.localeCompare(cB) * dir;
        }
        case 'status':
          return (a.status || '').localeCompare(b.status || '') * dir;
        case 'priority':
          return (this.getPriorityWeight(a.priority) - this.getPriorityWeight(b.priority)) * dir;
        case 'sla': {
          const timeA = a.slaDueAt ? new Date(a.slaDueAt).getTime() : (dir === 1 ? Infinity : -Infinity);
          const timeB = b.slaDueAt ? new Date(b.slaDueAt).getTime() : (dir === 1 ? Infinity : -Infinity);
          return (timeA - timeB) * dir;
        }
        case 'agent': {
          const agA = a.assignedAgentName || '';
          const agB = b.assignedAgentName || '';
          return agA.localeCompare(agB) * dir;
        }
        case 'createdAt':
        default: {
          const tA = new Date(a.createdAt).getTime();
          const tB = new Date(b.createdAt).getTime();
          return (tA - tB) * dir;
        }
      }
    });
    this.filteredTickets = result;
    this.currentPage = 1;
  }

  toggleSort(column: string): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = (column === 'createdAt' || column === 'priority' || column === 'id' || column === 'sla') ? 'desc' : 'asc';
    }
    this.applyFilters();
  }

  private getPriorityWeight(p: TicketPriority): number { return { LOW: 1, MEDIUM: 2, HIGH: 3 }[p] ?? 0; }

  reassignTicket(ticketId: number, agentId: string): void {
    const id = +agentId;
    if (!id) {
      this.ticketService.unassignTicket(ticketId).subscribe({
        next: () => {
          this.toast.success(`Ticket #${ticketId} unassigned and reverted to OPEN.`);
          this.loadData();
        },
        error: (err) => {
          this.toast.error(err?.error?.message || `Failed to unassign ticket #${ticketId}.`);
          this.loadData();
        }
      });
      return;
    }

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

  autoAssignSingleTicket(ticketId: number, event?: Event): void {
    if (event) event.stopPropagation();
    this.autoAssigningId = ticketId;
    this.cdr.detectChanges();

    this.ticketService.autoAssignTicket(ticketId, this.selectedStrategy).subscribe({
      next: (updatedTicket) => {
        this.autoAssigningId = null;
        const agentName = updatedTicket.assignedAgentName || 'Agent';
        this.toast.success(`Ticket #${ticketId} auto-assigned to ${agentName} (${this.getStrategyLabel(this.selectedStrategy)}).`);
        this.loadData();
      },
      error: (err) => {
        this.autoAssigningId = null;
        this.toast.error(err?.error?.message || `Failed to auto-assign ticket #${ticketId}.`);
        this.cdr.detectChanges();
      }
    });
  }

  isSelected(ticketId: number): boolean {
    return this.selectedTicketIds.has(ticketId);
  }

  toggleSelectTicket(ticketId: number, event?: Event): void {
    if (event) event.stopPropagation();
    if (this.selectedTicketIds.has(ticketId)) {
      this.selectedTicketIds.delete(ticketId);
    } else {
      this.selectedTicketIds.add(ticketId);
    }
    this.cdr.detectChanges();
  }

  isAllSelected(): boolean {
    const visible = this.paginatedTickets;
    return visible.length > 0 && visible.every(t => this.selectedTicketIds.has(t.id));
  }

  isPartiallySelected(): boolean {
    const visible = this.paginatedTickets;
    const count = visible.filter(t => this.selectedTicketIds.has(t.id)).length;
    return count > 0 && count < visible.length;
  }

  toggleSelectAll(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      this.paginatedTickets.forEach(t => this.selectedTicketIds.add(t.id));
    } else {
      this.paginatedTickets.forEach(t => this.selectedTicketIds.delete(t.id));
    }
    this.cdr.detectChanges();
  }

  isExecuteDisabled(): boolean {
    if (this.isBatchExecuting) return true;
    if (this.selectedBatchAction === 'AUTO_ASSIGN_ALL') {
      return this.unassignedCount === 0;
    }
    if (this.selectedBatchAction === 'AUTO_ASSIGN_SELECTED') {
      return this.selectedTicketIds.size === 0;
    }
    if (this.selectedBatchAction === 'UNASSIGN_SELECTED') {
      return this.selectedTicketIds.size === 0;
    }
    if (this.selectedBatchAction === 'ASSIGN_TO_AGENT') {
      return this.selectedTicketIds.size === 0 || !this.selectedBatchAgentId;
    }
    return false;
  }

  getBatchButtonLabel(): string {
    const count = this.selectedTicketIds.size;
    switch (this.selectedBatchAction) {
      case 'AUTO_ASSIGN_ALL':
        return `Auto Assign All (${this.unassignedCount})`;
      case 'AUTO_ASSIGN_SELECTED':
        return count > 0 ? `Auto Assign (${count})` : 'Auto Assign Selected';
      case 'UNASSIGN_SELECTED':
        return count > 0 ? `Unassign (${count})` : 'Unassigned Selected';
      case 'ASSIGN_TO_AGENT':
        return count > 0 ? `Assign (${count})` : 'Assign to Agent';
      default:
        return 'Execute';
    }
  }

  executeBatchAction(): void {
    if (this.isBatchExecuting) return;

    if (this.selectedBatchAction === 'AUTO_ASSIGN_ALL') {
      if (this.unassignedCount === 0) {
        this.toast.info('No unassigned OPEN tickets to assign.');
        return;
      }
      this.isBatchExecuting = true;
      this.cdr.detectChanges();

      this.ticketService.autoAssignAllUnassigned(this.selectedStrategy).subscribe({
        next: (assignedList) => {
          this.isBatchExecuting = false;
          this.toast.success(`Successfully auto-assigned ${assignedList.length} ticket(s) via ${this.getStrategyLabel(this.selectedStrategy)}.`);
          this.loadData();
        },
        error: (err) => {
          this.isBatchExecuting = false;
          this.toast.error(err?.error?.message || 'Failed to auto-assign tickets.');
          this.cdr.detectChanges();
        }
      });
      return;
    }

    const selectedIds = Array.from(this.selectedTicketIds);
    if (selectedIds.length === 0) {
      this.toast.info('Please select at least one ticket.');
      return;
    }

    if (this.selectedBatchAction === 'AUTO_ASSIGN_SELECTED') {
      this.isBatchExecuting = true;
      this.cdr.detectChanges();
      this.ticketService.batchAutoAssign(selectedIds, this.selectedStrategy).subscribe({
        next: (assignedList) => {
          this.isBatchExecuting = false;
          this.selectedTicketIds.clear();
          this.toast.success(`Successfully auto-assigned ${assignedList.length} ticket(s) via ${this.getStrategyLabel(this.selectedStrategy)}.`);
          this.loadData();
        },
        error: (err) => {
          this.isBatchExecuting = false;
          this.toast.error(err?.error?.message || 'Failed to auto-assign selected tickets.');
          this.cdr.detectChanges();
        }
      });
    } else if (this.selectedBatchAction === 'UNASSIGN_SELECTED') {
      this.isBatchExecuting = true;
      this.cdr.detectChanges();
      this.ticketService.batchUnassign(selectedIds).subscribe({
        next: (unassignedList) => {
          this.isBatchExecuting = false;
          this.selectedTicketIds.clear();
          this.toast.success(`Successfully unassigned ${unassignedList.length} ticket(s).`);
          this.loadData();
        },
        error: (err) => {
          this.isBatchExecuting = false;
          this.toast.error(err?.error?.message || 'Failed to unassign selected tickets.');
          this.cdr.detectChanges();
        }
      });
    } else if (this.selectedBatchAction === 'ASSIGN_TO_AGENT') {
      if (!this.selectedBatchAgentId) {
        this.toast.error('Please select an agent to assign tickets to.');
        return;
      }
      this.isBatchExecuting = true;
      this.cdr.detectChanges();
      const agentId = this.selectedBatchAgentId;
      const agent = this.agents.find(a => a.id === agentId);
      const agentName = agent ? agent.username : `Agent #${agentId}`;
      this.ticketService.batchAssign(selectedIds, agentId).subscribe({
        next: (assignedList) => {
          this.isBatchExecuting = false;
          this.selectedTicketIds.clear();
          this.toast.success(`Successfully assigned ${assignedList.length} ticket(s) to ${agentName}.`);
          this.loadData();
        },
        error: (err) => {
          this.isBatchExecuting = false;
          this.toast.error(err?.error?.message || 'Failed to assign selected tickets.');
          this.cdr.detectChanges();
        }
      });
    }
  }

  getStrategyLabel(val: RoutingStrategyType): string {
    const match = this.strategies.find(s => s.value === val);
    return match ? match.label : val;
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

