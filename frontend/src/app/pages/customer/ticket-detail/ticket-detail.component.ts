import { Component, OnInit, inject, ViewChild, ElementRef, AfterViewChecked, ChangeDetectorRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TicketService } from '../../../services/ticket.service';
import { AuthService } from '../../../services/auth.service';
import { AttachmentService } from '../../../services/attachment.service';
import { ToastService } from '../../../services/toast.service';
import { Ticket, TicketStatus, TicketPriority } from '../../../models/ticket.model';
import { Message } from '../../../models/message.model';
import { AttachmentResponse } from '../../../models/attachment.model';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './ticket-detail.component.html',
  styleUrl: './ticket-detail.component.css',
})
export class TicketDetailComponent implements OnInit, AfterViewChecked {
  private route = inject(ActivatedRoute);
  private ticketService = inject(TicketService);
  private authService = inject(AuthService);
  private attachmentService = inject(AttachmentService);
  private toastService = inject(ToastService);
  private cdr = inject(ChangeDetectorRef);

  @ViewChild('chatContainer') chatContainer!: ElementRef;
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  @ViewChild('directFileInput') directFileInput!: ElementRef<HTMLInputElement>;

  ticket: Ticket | undefined;
  messages: Message[] = [];
  attachments: AttachmentResponse[] = [];
  newMessage = '';
  currentUserId = 0;
  currentUserRole: string | null = null;
  notFound = false;
  isLoading = true;
  isSending = false;
  isUploadingDirect = false;

  selectedFile: File | null = null;
  selectedPreviewAttachment: AttachmentResponse | null = null;

  private shouldScroll = false;
  private readonly MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
  private readonly ALLOWED_EXTENSIONS = [
    'png', 'jpg', 'jpeg', 'gif', 'webp', 'pdf', 'txt', 'log', 'csv', 'json', 'xml', 'zip'
  ];

  @HostListener('window:keydown.escape')
  onEscapePress(): void {
    if (this.selectedPreviewAttachment) {
      this.closePreview();
    }
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    const user = this.authService.getCurrentUser();
    this.currentUserId = user?.id || 0;
    this.currentUserRole = user?.role || null;

    this.ticketService.getTicketById(id).subscribe({
      next: (ticket) => {
        this.ticket = ticket;
        this.isLoading = false;
        this.loadMessages(id);
        this.loadAttachments(id);
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

  loadAttachments(ticketId: number): void {
    this.attachmentService.getTicketAttachments(ticketId).subscribe({
      next: (atts) => {
        this.attachments = atts;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load attachments', err);
      }
    });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    if (!this.validateFile(file)) {
      input.value = '';
      return;
    }

    this.selectedFile = file;
    this.cdr.detectChanges();
  }

  removeSelectedFile(): void {
    this.selectedFile = null;
    if (this.fileInput?.nativeElement) {
      this.fileInput.nativeElement.value = '';
    }
    this.cdr.detectChanges();
  }

  onDirectFileUpload(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0 || !this.ticket) return;

    const file = input.files[0];
    if (!this.validateFile(file)) {
      input.value = '';
      return;
    }

    this.isUploadingDirect = true;
    this.attachmentService.uploadAttachment(this.ticket.id, file).subscribe({
      next: (att) => {
        this.attachments.unshift(att);
        this.isUploadingDirect = false;
        input.value = '';
        this.toastService.show(`Attachment '${file.name}' uploaded successfully.`, 'success');
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isUploadingDirect = false;
        input.value = '';
        const msg = err?.error?.message || 'Failed to upload attachment.';
        this.toastService.show(msg, 'error');
        this.cdr.detectChanges();
      }
    });
  }

  private validateFile(file: File): boolean {
    if (file.size > this.MAX_FILE_SIZE) {
      this.toastService.show('File size exceeds maximum allowed limit of 10MB.', 'error');
      return false;
    }

    const ext = file.name.split('.').pop()?.toLowerCase() || '';
    if (!this.ALLOWED_EXTENSIONS.includes(ext)) {
      this.toastService.show(`Unsupported file type (.${ext}). Allowed: images, PDF, logs, CSV, JSON, XML, ZIP.`, 'error');
      return false;
    }

    return true;
  }

  sendMessage(): void {
    if ((!this.newMessage.trim() && !this.selectedFile) || !this.ticket || this.isSending) return;

    const content = this.newMessage.trim() || (this.selectedFile ? `Attached: ${this.selectedFile.name}` : '');
    const fileToUpload = this.selectedFile;

    this.isSending = true;

    this.ticketService.addMessage(this.ticket.id, content).subscribe({
      next: (msg) => {
        this.messages.push(msg);
        this.newMessage = '';
        this.removeSelectedFile();
        this.shouldScroll = true;

        if (fileToUpload) {
          this.attachmentService.uploadAttachment(this.ticket!.id, fileToUpload, msg.id).subscribe({
            next: (att) => {
              this.attachments.push(att);
              this.isSending = false;
              this.cdr.detectChanges();
            },
            error: (err) => {
              this.isSending = false;
              const errorMsg = err?.error?.message || 'Message sent, but attachment upload failed.';
              this.toastService.show(errorMsg, 'error');
              this.cdr.detectChanges();
            }
          });
        } else {
          this.isSending = false;
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        this.isSending = false;
        const msg = err?.error?.message || 'Failed to send message.';
        this.toastService.show(msg, 'error');
        this.cdr.detectChanges();
      }
    });
  }

  deleteAttachment(attachment: AttachmentResponse): void {
    if (!this.ticket) return;
    if (!confirm(`Are you sure you want to permanently delete '${attachment.fileName}'?`)) return;

    this.attachmentService.deleteAttachment(this.ticket.id, attachment.id).subscribe({
      next: () => {
        this.attachments = this.attachments.filter(a => a.id !== attachment.id);
        if (this.selectedPreviewAttachment?.id === attachment.id) {
          this.closePreview();
        }
        this.toastService.show(`Deleted '${attachment.fileName}'.`, 'info');
        this.cdr.detectChanges();
      },
      error: (err) => {
        const msg = err?.error?.message || 'Could not delete attachment.';
        this.toastService.show(msg, 'error');
      }
    });
  }

  canDelete(attachment: AttachmentResponse): boolean {
    if (this.currentUserRole === 'ADMIN' || this.currentUserRole === 'SYSTEM_ADMIN' || this.currentUserRole === 'SUPPORT_AGENT') {
      return true;
    }
    return attachment.uploaderId === this.currentUserId;
  }

  getAttachmentsForMessage(messageId: number): AttachmentResponse[] {
    return this.attachments.filter(a => a.messageId === messageId);
  }

  getTicketLevelAttachments(): AttachmentResponse[] {
    return this.attachments.filter(a => !a.messageId);
  }

  openPreview(attachment: AttachmentResponse): void {
    this.selectedPreviewAttachment = attachment;
  }

  closePreview(): void {
    this.selectedPreviewAttachment = null;
  }

  isImage(attachment: AttachmentResponse): boolean {
    return this.attachmentService.isImage(attachment);
  }

  isPdf(attachment: AttachmentResponse): boolean {
    return this.attachmentService.isPdf(attachment);
  }

  getPreviewUrl(attachment: AttachmentResponse): string {
    return this.attachmentService.getPreviewUrl(this.ticket?.id || attachment.ticketId, attachment.id);
  }

  getDownloadUrl(attachment: AttachmentResponse): string {
    return this.attachmentService.getDownloadUrl(this.ticket?.id || attachment.ticketId, attachment.id);
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private scrollToBottom(): void {
    try {
      const el = this.chatContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch (_) {}
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
