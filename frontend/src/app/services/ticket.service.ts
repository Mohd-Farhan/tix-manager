import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Ticket, TicketPriority, TicketStatus, TicketStatusHistory } from '../models/ticket.model';
import { Message } from '../models/message.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root',
})
export class TicketService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/api/tickets`;

  // UI / Reactive events
  private ticketCreatedSubject = new Subject<Ticket>();
  ticketCreated$ = this.ticketCreatedSubject.asObservable();

  private ticketUpdatedSubject = new Subject<Ticket>();
  ticketUpdated$ = this.ticketUpdatedSubject.asObservable();

  private openCreateTicketSubject = new Subject<void>();
  openCreateTicket$ = this.openCreateTicketSubject.asObservable();

  triggerCreateTicketModal(): void {
    this.openCreateTicketSubject.next();
  }

  notifyTicketCreated(ticket: Ticket): void {
    this.ticketCreatedSubject.next(ticket);
  }

  notifyTicketUpdated(ticket: Ticket): void {
    this.ticketUpdatedSubject.next(ticket);
  }

  createTicket(data: { title: string; description: string; priority: TicketPriority; customerId?: number }): Observable<Ticket> {
    return this.http.post<Ticket>(this.apiUrl, data).pipe(
      tap((ticket) => this.notifyTicketCreated(ticket))
    );
  }

  getTicketsForUser(userId: number): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(`${this.apiUrl}/user/${userId}`);
  }

  getTicketsForUserPaged(userId: number, page: number = 0, size: number = 20, sort: string = 'createdAt,desc'): Observable<PageResponse<Ticket>> {
    return this.http.get<PageResponse<Ticket>>(`${this.apiUrl}/user/${userId}/paged?page=${page}&size=${size}&sort=${sort}`);
  }

  getAllTickets(): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(this.apiUrl);
  }

  getAllTicketsPaged(page: number = 0, size: number = 20, sort: string = 'createdAt,desc'): Observable<PageResponse<Ticket>> {
    return this.http.get<PageResponse<Ticket>>(`${this.apiUrl}/paged?page=${page}&size=${size}&sort=${sort}`);
  }

  getTicketById(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.apiUrl}/${id}`);
  }

  assignTicket(ticketId: number, agentId: number): Observable<Ticket> {
    return this.http.put<Ticket>(`${this.apiUrl}/${ticketId}/assign?agentId=${agentId}`, {}).pipe(
      tap((ticket) => this.notifyTicketUpdated(ticket))
    );
  }

  updateTicketStatus(ticketId: number, status: TicketStatus): Observable<Ticket> {
    return this.http.put<Ticket>(`${this.apiUrl}/${ticketId}/status?status=${status}`, {}).pipe(
      tap((ticket) => this.notifyTicketUpdated(ticket))
    );
  }

  addMessage(ticketId: number, content: string): Observable<Message> {
    return this.http.post<Message>(`${this.apiUrl}/${ticketId}/messages`, { content });
  }

  getMessageThread(ticketId: number): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.apiUrl}/${ticketId}/messages`);
  }

  getTicketStatusHistory(ticketId: number): Observable<TicketStatusHistory[]> {
    return this.http.get<TicketStatusHistory[]>(`${this.apiUrl}/${ticketId}/history`);
  }

  softDeleteTicket(ticketId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${ticketId}`).pipe(
      tap(() => this.ticketUpdatedSubject.next({ id: ticketId } as Ticket))
    );
  }

  getAllTicketsAdmin(): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(`${this.apiUrl}/admin/all`);
  }
}
