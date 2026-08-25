import { Component, OnInit, inject, ViewChild, ElementRef, AfterViewChecked, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TicketService } from '../../../services/ticket.service';
import { AuthService } from '../../../services/auth.service';
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
  private ticketService = inject(TicketService);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);

  @ViewChild('chatContainer') chatContainer!: ElementRef;

  ticket: Ticket | undefined;
  messages: Message[] = [];
  newMessage = '';
  currentUserId = 0;
  notFound = false;
  isLoading = true;
  isSending = false;
  
  // Status controls
  ticketStatuses = Object.values(TicketStatus);
  currentStatus: TicketStatus = TicketStatus.OPEN;
  
  private shouldScroll = false;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    const user = this.authService.getCurrentUser();
    this.currentUserId = user?.id || 0;

    this.ticketService.getTicketById(id).subscribe({
      next: (ticket) => {
        this.ticket = ticket;
        this.currentStatus = ticket.status;
        this.isLoading = false;
        this.loadMessages(id);
        this.cdr.detectChanges();
      },
      error: () => {
        this.notFound = true;
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadMessages(ticketId: number): void {
    this.ticketService.getMessageThread(ticketId).subscribe({
      next: (messages) => {
        this.messages = messages;
        this.shouldScroll = true;
        this.cdr.detectChanges();
      }
    });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || !this.ticket || this.isSending) return;

    const content = this.newMessage.trim();
    this.isSending = true;

    // Auto-assign to me if replying to an unassigned ticket
    if (!this.ticket.assignedAgentId) {
      this.ticketService.assignTicket(this.ticket.id, this.currentUserId).subscribe({
        next: (updatedTicket) => {
          this.ticket = updatedTicket;
          this.postMessage(content);
        },
        error: () => {
          this.postMessage(content);
        }
      });
    } else {
      this.postMessage(content);
    }
  }

  private postMessage(content: string): void {
    if (!this.ticket) return;

    this.ticketService.addMessage(this.ticket.id, content).subscribe({
      next: (msg) => {
        this.messages.push(msg);
        this.newMessage = '';
        this.isSending = false;
        this.shouldScroll = true;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isSending = false;
        this.cdr.detectChanges();
      }
    });
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  updateStatus(newStatus: TicketStatus): void {
    if (!this.ticket || this.ticket.status === newStatus) return;
    
    this.ticketService.updateTicketStatus(this.ticket.id, newStatus).subscribe({
      next: (updated) => {
        this.ticket = updated;
        this.currentStatus = updated.status;
        this.cdr.detectChanges();
      }
    });
  }

  assignToMe(): void {
    if (!this.ticket) return;
    this.ticketService.assignTicket(this.ticket.id, this.currentUserId).subscribe({
      next: (updated) => {
        this.ticket = updated;
        this.cdr.detectChanges();
      }
    });
  }

  private scrollToBottom(): void {
    try {
      const el = this.chatContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch (_) {}
  }

  getCustomerName(customerId: number | undefined): string {
    if (this.ticket?.customerUsername) {
      return this.ticket.customerUsername;
    }
    if (!customerId) return 'Unknown';
    return `Customer #${customerId}`;
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
