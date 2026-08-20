import { Component, OnInit, inject, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MockDataService } from '../../../services/mock-data.service';
import { Ticket, TicketStatus, TicketPriority } from '../../../models/ticket.model';
import { Message } from '../../../models/message.model';

@Component({
  selector: 'app-agent-ticket-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './ticket-detail.component.html',
  styleUrl: './ticket-detail.component.css',
})
export class TicketDetailComponent implements OnInit, AfterViewChecked {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private mockData = inject(MockDataService);

  @ViewChild('chatContainer') chatContainer!: ElementRef;

  ticket: Ticket | undefined;
  messages: Message[] = [];
  newMessage = '';
  currentUserId = 0;
  notFound = false;
  
  // Status controls
  ticketStatuses = Object.values(TicketStatus);
  currentStatus: TicketStatus = TicketStatus.OPEN;
  
  private shouldScroll = false;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.ticket = this.mockData.getTicketById(id);
    this.currentUserId = this.mockData.getCurrentUser().id;

    if (this.ticket) {
      this.currentStatus = this.ticket.status;
      this.messages = this.mockData.getMessagesForTicket(id);
      this.shouldScroll = true;
    } else {
      this.notFound = true;
    }
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || !this.ticket) return;

    // Auto-assign to me if replying to an unassigned ticket
    if (!this.ticket.assignedAgentId) {
      this.assignToMe();
    }

    const msg = this.mockData.addMessage(this.ticket.id, this.newMessage.trim());
    this.messages.push(msg);
    this.newMessage = '';
    this.shouldScroll = true;
    
    // Refresh ticket to show assignment if it just happened
    this.ticket = this.mockData.getTicketById(this.ticket.id);
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  updateStatus(newStatus: TicketStatus): void {
    if (!this.ticket || this.ticket.status === newStatus) return;
    
    const updated = this.mockData.updateTicketStatus(this.ticket.id, newStatus);
    if (updated) {
      this.ticket = updated;
      this.currentStatus = updated.status;
    }
  }

  assignToMe(): void {
    if (!this.ticket) return;
    const updated = this.mockData.assignTicket(this.ticket.id, this.currentUserId);
    if (updated) {
      this.ticket = updated;
    }
  }

  private scrollToBottom(): void {
    try {
      const el = this.chatContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch (_) {}
  }

  getCustomerName(customerId: number | undefined): string {
    if (!customerId) return 'Unknown';
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
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }

  formatTime(dateStr: string): string {
    return new Date(dateStr).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  }

  formatDateTime(dateStr: string): string {
    return this.formatDate(dateStr) + ' at ' + this.formatTime(dateStr);
  }
}
