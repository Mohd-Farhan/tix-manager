import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors, HttpErrorResponse } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { Router } from '@angular/router';
import { errorInterceptor } from './error.interceptor';
import { ToastService } from '../services/toast.service';

describe('errorInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;
  let toastService: ToastService;
  let router: Router;

  beforeEach(() => {
    const routerMock = {
      navigate: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerMock },
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    toastService = TestBed.inject(ToastService);
    router = TestBed.inject(Router);
    toastService.clear();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should dispatch warning toast for 400 validation error', () => {
    const errorPayload = {
      status: 400,
      error: 'Validation Failed',
      message: 'Request payload validation errors',
      fieldErrors: [{ field: 'title', message: 'Title is required' }],
    };

    httpClient.post('/api/tickets', {}).subscribe({
      next: () => expect.unreachable(),
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(400);
      },
    });

    const req = httpTesting.expectOne('/api/tickets');
    req.flush(errorPayload, { status: 400, statusText: 'Bad Request' });

    const toasts = toastService.toasts();
    expect(toasts.length).toBe(1);
    expect(toasts[0].type).toBe('warning');
    expect(toasts[0].message).toContain('title: Title is required');
  });

  it('should dispatch error toast for 403 Forbidden', () => {
    httpClient.get('/api/tickets/999').subscribe({
      next: () => expect.unreachable(),
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(403);
      },
    });

    const req = httpTesting.expectOne('/api/tickets/999');
    req.flush({ message: 'Access denied' }, { status: 403, statusText: 'Forbidden' });

    const toasts = toastService.toasts();
    expect(toasts.length).toBe(1);
    expect(toasts[0].type).toBe('error');
    expect(toasts[0].title).toBe('Access Forbidden');
  });

  it('should redirect to /auth/login and show session expired warning on 401 Unauthorized', () => {
    httpClient.get('/api/tickets').subscribe({
      next: () => expect.unreachable(),
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(401);
      },
    });

    const req = httpTesting.expectOne('/api/tickets');
    req.flush({ message: 'Token expired' }, { status: 401, statusText: 'Unauthorized' });

    const toasts = toastService.toasts();
    expect(toasts.length).toBe(1);
    expect(toasts[0].title).toBe('Session Expired');
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });
});
