import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { User, UserRole } from '../models/user.model';

export interface LoginResponse {
  token: string;
  user: User;
}


@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private tokenKey = 'tix-token';
  private userKey = 'tix-user';

  private currentUserSignal = signal<User | null>(this.getStoredUser());
  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.currentUserSignal() && !!this.getToken());
  readonly userRole = computed(() => this.currentUserSignal()?.role ?? null);

  private getStoredUser(): User | null {
    const raw = localStorage.getItem(this.userKey);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as User;
    } catch {
      return null;
    }
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getCurrentUser(): User | null {
    return this.currentUserSignal();
  }

  login(credentials: { username: string; password: string }): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/api/auth/login`, credentials).pipe(
      tap((res) => {
        if (res.token && res.user) {
          localStorage.setItem(this.tokenKey, res.token);
          localStorage.setItem(this.userKey, JSON.stringify(res.user));
          this.currentUserSignal.set(res.user);
        }
      })
    );
  }


  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.currentUserSignal.set(null);
    this.router.navigate(['/auth/login']);
  }

  hasRole(role: UserRole): boolean {
    return this.currentUserSignal()?.role === role;
  }
}
