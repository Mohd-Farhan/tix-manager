import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { User, BulkUploadResult, BulkUploadHistoryItem } from '../models/user.model';

import { PageResponse } from '../models/page.model';
export type { PageResponse };

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/api/users`;

  // ── Reactive State (Signal-based) ──────────────────────────────
  // Cached user list for admin views. Mutations auto-sync the cache
  // so components reflect changes without manual reload.
  private readonly usersSignal = signal<User[]>([]);
  readonly users = this.usersSignal.asReadonly();
  readonly userCount = computed(() => this.usersSignal().length);
  readonly agentCount = computed(() =>
    this.usersSignal().filter(u => u.role === 'SUPPORT_AGENT' && u.active !== false && !u.deleted).length
  );

  // ── API Methods (with cache sync) ─────────────────────────────

  getUserById(id: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${id}`);
  }

  getAgents(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/agents`);
  }

  getAllUsersAdmin(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/admin/all`).pipe(
      tap(users => this.usersSignal.set(users))
    );
  }

  getAllUsersAdminPaged(page: number = 0, size: number = 20, sort: string = 'createdAt,desc'): Observable<PageResponse<User>> {
    return this.http.get<PageResponse<User>>(`${this.apiUrl}/admin/paged?page=${page}&size=${size}&sort=${sort}`);
  }

  createUser(data: { username: string; email: string; password?: string; role: string }): Observable<User> {
    return this.http.post<User>(this.apiUrl, data).pipe(
      tap(user => this.usersSignal.update(list => [...list, user]))
    );
  }

  bulkUploadUsers(file: File): Observable<BulkUploadResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<BulkUploadResult>(`${this.apiUrl}/bulk-upload`, formData);
  }

  getBulkUploadHistory(): Observable<BulkUploadHistoryItem[]> {
    return this.http.get<BulkUploadHistoryItem[]>(`${this.apiUrl}/bulk-upload/history`);
  }

  getBulkUploadHistoryPaged(page: number = 0, size: number = 20, sort: string = 'createdAt,desc'): Observable<PageResponse<BulkUploadHistoryItem>> {
    return this.http.get<PageResponse<BulkUploadHistoryItem>>(`${this.apiUrl}/bulk-upload/history/paged?page=${page}&size=${size}&sort=${sort}`);
  }

  updatePassword(userId: number, currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${userId}/password`, {
      currentPassword,
      newPassword,
    });
  }

  updateProfile(userId: number, data: Partial<User>): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${userId}`, data).pipe(
      tap(updated => this.usersSignal.update(list =>
        list.map(u => u.id === updated.id ? updated : u)
      ))
    );
  }

  softDeleteUser(id: number): Observable<void> {
    return this.deleteUser(id);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => this.usersSignal.update(list =>
        list.map(u => u.id === id ? { ...u, deleted: true } : u)
      ))
    );
  }

  deactivateUser(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/deactivate`, {}).pipe(
      tap(() => this.usersSignal.update(list =>
        list.map(u => u.id === id ? { ...u, active: false } : u)
      ))
    );
  }

  activateUser(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/activate`, {}).pipe(
      tap(() => this.usersSignal.update(list =>
        list.map(u => u.id === id ? { ...u, active: true } : u)
      ))
    );
  }

  reactivateUser(id: number): Observable<void> {
    return this.activateUser(id);
  }
}
