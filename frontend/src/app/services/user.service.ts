import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { User } from '../models/user.model';

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
