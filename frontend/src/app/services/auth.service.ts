import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { User, UserRole } from '../models/user.model';

export interface LoginResponse {
  token: string;
  refreshToken?: string;
  tokenType?: string;
  expiresIn?: number;
  user: User;
}


@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private tokenKey = 'tix-token';
  private refreshTokenKey = 'tix-refresh-token';
  private userKey = 'tix-user';

  private currentUserSignal = signal<User | null>(this.getStoredUser());
  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.currentUserSignal() && !!this.getToken());
  readonly userRole = computed(() => this.currentUserSignal()?.role ?? null);
  readonly mustChangePassword = computed(() => !!this.currentUserSignal()?.mustChangePassword);

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

  getRefreshToken(): string | null {
    return localStorage.getItem(this.refreshTokenKey);
  }

  getCurrentUser(): User | null {
    return this.currentUserSignal();
  }

  login(credentials: { username: string; password: string }): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/api/auth/login`, credentials).pipe(
      tap((res) => {
        if (res.token && res.user) {
          localStorage.setItem(this.tokenKey, res.token);
          if (res.refreshToken) {
            localStorage.setItem(this.refreshTokenKey, res.refreshToken);
          }
          localStorage.setItem(this.userKey, JSON.stringify(res.user));
          this.currentUserSignal.set(res.user);
        }
      })
    );
  }

  /**
   * Refreshes the short-lived access token using the database-persisted refresh token.
   * Updates local storage with the newly rotated refresh token and access token.
   */
  refreshToken(): Observable<LoginResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      this.clearSessionAndRedirect();
      throw new Error('No refresh token available');
    }

    return this.http.post<LoginResponse>(`${environment.apiUrl}/api/auth/refresh`, { refreshToken }).pipe(
      tap((res) => {
        if (res.token) {
          localStorage.setItem(this.tokenKey, res.token);
          if (res.refreshToken) {
            localStorage.setItem(this.refreshTokenKey, res.refreshToken);
          }
          if (res.user) {
            localStorage.setItem(this.userKey, JSON.stringify(res.user));
            this.currentUserSignal.set(res.user);
          }
        }
      })
    );
  }

  logout(): void {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      // Notify backend to burn the refresh token and terminate active session
      this.http.post(`${environment.apiUrl}/api/auth/logout`, { refreshToken }).subscribe({
        next: () => {},
        error: () => {}
      });
    }
    this.clearSessionAndRedirect();
  }

  clearSessionAndRedirect(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    localStorage.removeItem(this.userKey);
    this.currentUserSignal.set(null);
    this.router.navigate(['/auth/login']);
  }

  hasRole(role: UserRole): boolean {
    return this.currentUserSignal()?.role === role;
  }

  updateCurrentUser(partial: Partial<User>): void {
    const current = this.currentUserSignal();
    if (current) {
      const updated = { ...current, ...partial };
      localStorage.setItem(this.userKey, JSON.stringify(updated));
      this.currentUserSignal.set(updated);
    }
  }
}
