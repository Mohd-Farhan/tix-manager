import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AttachmentResponse } from '../models/attachment.model';

@Injectable({
  providedIn: 'root'
})
export class AttachmentService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/tickets`;

  /**
   * Upload an attachment (screenshot, image, document, log) to a ticket or ticket message.
   */
  uploadAttachment(ticketId: number, file: File, messageId?: number): Observable<AttachmentResponse> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    if (messageId) {
      formData.append('messageId', messageId.toString());
    }

    return this.http.post<AttachmentResponse>(`${this.baseUrl}/${ticketId}/attachments`, formData);
  }

  /**
   * Fetch all attachments associated with this ticket and its message threads.
   */
  getTicketAttachments(ticketId: number): Observable<AttachmentResponse[]> {
    return this.http.get<AttachmentResponse[]>(`${this.baseUrl}/${ticketId}/attachments`);
  }

  /**
   * Permanently delete an attachment (requires ownership or ADMIN/SUPPORT_AGENT role).
   */
  deleteAttachment(ticketId: number, attachmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${ticketId}/attachments/${attachmentId}`);
  }

  /**
   * Returns full URL for inline preview (image rendering or PDF view).
   */
  getPreviewUrl(ticketId: number, attachmentId: number): string {
    return `${this.baseUrl}/${ticketId}/attachments/${attachmentId}/preview`;
  }

  /**
   * Returns full URL for downloading the attachment.
   */
  getDownloadUrl(ticketId: number, attachmentId: number): string {
    return `${this.baseUrl}/${ticketId}/attachments/${attachmentId}/download`;
  }

  /**
   * Helper to check if attachment is an image for lightbox / thumbnail display.
   */
  isImage(attachment: AttachmentResponse): boolean {
    return !!attachment.contentType && attachment.contentType.startsWith('image/');
  }

  /**
   * Helper to check if attachment is a PDF.
   */
  isPdf(attachment: AttachmentResponse): boolean {
    return attachment.contentType === 'application/pdf';
  }
}
