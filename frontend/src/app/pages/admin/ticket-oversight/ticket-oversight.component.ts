import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MockDataService, SystemUser } from '../../../services/mock-data.service';
import { Ticket, TicketStatus, TicketPriority } from '../../../models/ticket.model';
import { UserRole } from '../../../models/user.model';

@Component({
  selector: 'app-admin-ticket-oversight',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ticket-oversight.component.html',
  styleUrl: './ticket-oversight.component.css',
})
export class TicketOversightComponent implements OnInit {
  private mockData = inject(MockDataService);
  private cdr = inject(ChangeDetectorRef);

  tickets: Ticket[] = [];
  filteredTickets: Ticket[] = [];
  agents: SystemUser[] = [];

  searchTerm = '';
  statusFilter = 'ALL';
  priorityFilter = 'ALL';
  assignmentFilter = 'ALL';
  sortBy = 'newest';

  // Confirm delete
  confirmDeleteId: number | null = null;

  ngOnInit(): void {
    this.tickets = this.mockData.getAllTickets();
    this.agents = this.mockData.getAgentUsers();
    this.applyFilters();

    this.mockData.ticketUpdated$.subscribe(() => {
      this.tickets = this.mockData.getAllTickets();
      this.applyFilters();
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
          this.mockData.getCustomerNameById(t.customerId).toLowerCase().includes(term) ||
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
  }

  private getPriorityWeight(p: TicketPriority): number { return { LOW: 1, MEDIUM: 2, HIGH: 3 }[p] ?? 0; }

  reassignTicket(ticketId: number, agentId: string): void {
    const id = +agentId;
    if (!id) {
      // Unassign
      const ticket = this.tickets.find((t) => t.id === ticketId);
      if (ticket) {
        this.mockData.reassignTicket(ticketId, 0, '');
      }
      return;
    }
    const agent = this.agents.find((a) => a.id === id);
    if (agent) {
      this.mockData.reassignTicket(ticketId, agent.id, agent.username);
    }
  }

  forceClose(ticketId: number, event: Event): void {
    event.stopPropagation();
    this.mockData.forceCloseTicket(ticketId);
  }

  confirmDelete(ticketId: number, event: Event): void {
    event.stopPropagation();
    this.confirmDeleteId = ticketId;
    this.cdr.detectChanges();
  }

  executeDelete(): void {
    if (this.confirmDeleteId) {
      this.mockData.deleteTicketAdmin(this.confirmDeleteId);
      this.confirmDeleteId = null;
      this.tickets = this.mockData.getAllTickets();
      this.applyFilters();
      this.cdr.detectChanges();
    }
  }

  cancelDelete(): void {
    this.confirmDeleteId = null;
    this.cdr.detectChanges();
  }

  getCustomerName(customerId: number): string { return this.mockData.getCustomerNameById(customerId); }
  getStatusClass(s: TicketStatus): string { return { OPEN: 'status-open', IN_PROGRESS: 'status-progress', RESOLVED: 'status-resolved' }[s] ?? ''; }
  getStatusLabel(s: TicketStatus): string { return { OPEN: 'Open', IN_PROGRESS: 'In Progress', RESOLVED: 'Resolved' }[s] ?? s; }
  getPriorityClass(p: TicketPriority): string { return { LOW: 'priority-low', MEDIUM: 'priority-medium', HIGH: 'priority-high' }[p] ?? ''; }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }
}
