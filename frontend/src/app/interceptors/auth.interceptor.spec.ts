import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { provideRouter, Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService, LoginResponse } from '../services/auth.service';
import { of, throwError } from 'rxjs';
import { User, UserRole } from '../models/user.model';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authServiceMock: {
    getToken: ReturnType<typeof vi.fn>;
    refreshToken: ReturnType<typeof vi.fn>;
    clearSessionAndRedirect: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  const mockUser: User = {
    id: 1,
    username: 'agent_smith',
    email: 'agent@example.com',
    role: UserRole.SUPPORT_AGENT,
  };

  const mockLoginResponse: LoginResponse = {
    token: 'new-rotated-access-token',
    refreshToken: 'new-rotated-refresh-token',
    tokenType: 'Bearer',
    expiresIn: 900,
    user: mockUser,
  };

  beforeEach(() => {
    authServiceMock = {
      getToken: vi.fn(),
      refreshToken: vi.fn(),
      clearSessionAndRedirect: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should add Authorization Bearer header when token exists and endpoint is not /api/auth/', () => {
    authServiceMock.getToken.mockReturnValue('valid.jwt.token');

    http.get('/api/tickets').subscribe();

    const req = httpMock.expectOne('/api/tickets');
    expect(req.request.headers.get('Authorization')).toBe('Bearer valid.jwt.token');
    req.flush([]);
  });

  it('should NOT add Authorization header when request is to /api/auth/login', () => {
    authServiceMock.getToken.mockReturnValue('valid.jwt.token');

    http.post('/api/auth/login', { username: 'test' }).subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('should handle 401 error by refreshing token and retrying failed request', () => {
    authServiceMock.getToken.mockReturnValue('expired.jwt.token');
    authServiceMock.refreshToken.mockReturnValue(of(mockLoginResponse));

    http.get('/api/tickets/1').subscribe((res) => {
      expect(res).toEqual({ id: 1, title: 'Network Outage' });
    });

    // Initial request fails with 401
    const initialReq = httpMock.expectOne('/api/tickets/1');
    expect(initialReq.request.headers.get('Authorization')).toBe('Bearer expired.jwt.token');
    initialReq.flush({ status: 401, error: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    // Verify refreshToken was called
    expect(authServiceMock.refreshToken).toHaveBeenCalled();

    // Retried request with rotated token
    const retriedReq = httpMock.expectOne('/api/tickets/1');
    expect(retriedReq.request.headers.get('Authorization')).toBe('Bearer new-rotated-access-token');
    retriedReq.flush({ id: 1, title: 'Network Outage' });
  });

  it('should clear session and redirect when refreshToken fails on 401', () => {
    authServiceMock.getToken.mockReturnValue('expired.jwt.token');
    authServiceMock.refreshToken.mockReturnValue(throwError(() => new Error('Revoked token')));

    http.get('/api/tickets/2').subscribe({
      next: () => expect.unreachable(),
      error: (err) => {
        expect(err).toBeTruthy();
      },
    });

    const initialReq = httpMock.expectOne('/api/tickets/2');
    initialReq.flush({ status: 401, error: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(authServiceMock.refreshToken).toHaveBeenCalled();
    expect(authServiceMock.clearSessionAndRedirect).toHaveBeenCalled();
  });
});
