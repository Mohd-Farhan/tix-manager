import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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

  getUserById(id: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${id}`);
  }

  getAgents(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/agents`);
  }

  getAllUsersAdmin(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/admin/all`);
  }

  getAllUsersAdminPaged(page: number = 0, size: number = 20, sort: string = 'createdAt,desc'): Observable<PageResponse<User>> {
    return this.http.get<PageResponse<User>>(`${this.apiUrl}/admin/paged?page=${page}&size=${size}&sort=${sort}`);
  }

  createUser(data: { username: string; email: string; password?: string; role: string }): Observable<User> {
    return this.http.post<User>(this.apiUrl, data);
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
    return this.http.put<User>(`${this.apiUrl}/${userId}`, data);
  }

  softDeleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
