import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { User, UserRole } from '../models/user.model';
import { environment } from '../../environments/environment';

/**
 * ==============================================================================================
 * AUTOMATED TESTING SUITE: Angular Service Unit Tests for AuthService
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Frontend Enterprise Standards):
 * 1. Authentication State Persistence: Verifies that login stores JWT token and user profile
 *    in localStorage, and logout cleans up credentials to prevent session leaks.
 * 2. Role Checks: Verifies `hasRole()` method accurately inspects the authenticated user's role
 *    to protect route guards and conditionally display navigation buttons.
 * 3. Signal-Based Auth State: Confirms `isAuthenticated()` computed signal updates reactively.
 */
describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/api/auth`;

  const mockUser: User = {
    id: 1,
    username: 'farhan_dev',
    email: 'farhan@example.com',
    role: UserRole.CUSTOMER,
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideRouter([{ path: 'auth/login', component: class {} }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  /**
   * TEST CASE 1: Successful login saves token and user in localStorage.
   */
  it('login — should send credentials, store token in localStorage, and update currentUser', () => {
    const credentials = { username: 'farhan_dev', password: 'Password123' };
    const mockAuthResponse = {
      token: 'jwt.test.token.xyz',
      user: mockUser,
    };

    service.login(credentials).subscribe((res) => {
      expect(res.token).toBe('jwt.test.token.xyz');
      expect(res.user.username).toBe('farhan_dev');
    });

    const req = httpMock.expectOne(`${apiUrl}/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(credentials);
    req.flush(mockAuthResponse);

    expect(localStorage.getItem('tix-token')).toBe('jwt.test.token.xyz');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.getCurrentUser()?.username).toBe('farhan_dev');
  });

  /**
   * TEST CASE 2: Logout clears tokens and resets user state.
   */
  it('logout — should remove tokens from storage and reset auth state', () => {
    localStorage.setItem('tix-token', 'existing.token');
    localStorage.setItem('tix-user', JSON.stringify(mockUser));

    service.logout();

    expect(localStorage.getItem('tix-token')).toBeNull();
    expect(localStorage.getItem('tix-user')).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.getCurrentUser()).toBeNull();
  });

  /**
   * TEST CASE 3: hasRole returns true for matching role and false for non-matching role.
   */
  it('hasRole — should correctly evaluate user role permissions', () => {
    const credentials = { username: 'farhan_dev', password: 'Password123' };
    const mockAuthResponse = {
      token: 'jwt.test.token.xyz',
      user: mockUser,
    };

    service.login(credentials).subscribe();
    const req = httpMock.expectOne(`${apiUrl}/login`);
    req.flush(mockAuthResponse);

    expect(service.hasRole(UserRole.CUSTOMER)).toBe(true);
    expect(service.hasRole(UserRole.ADMIN)).toBe(false);
    expect(service.hasRole(UserRole.SUPPORT_AGENT)).toBe(false);
  });

  /**
   * TEST CASE 4: Login with refresh token stores both tokens.
   */
  it('login — should store refreshToken in localStorage when returned', () => {
    const credentials = { username: 'farhan_dev', password: 'Password123' };
    const mockAuthResponse = {
      token: 'jwt.test.token.xyz',
      refreshToken: 'refresh-token-uuid-1234',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: mockUser,
    };

    service.login(credentials).subscribe();
    const req = httpMock.expectOne(`${apiUrl}/login`);
    req.flush(mockAuthResponse);

    expect(localStorage.getItem('tix-token')).toBe('jwt.test.token.xyz');
    expect(localStorage.getItem('tix-refresh-token')).toBe('refresh-token-uuid-1234');
    expect(service.getRefreshToken()).toBe('refresh-token-uuid-1234');
  });

  /**
   * TEST CASE 5: refreshToken exchanges old refresh token for rotated tokens.
   */
  it('refreshToken — should send refreshToken to /api/auth/refresh and update storage', () => {
    localStorage.setItem('tix-token', 'old.access.token');
    localStorage.setItem('tix-refresh-token', 'existing-refresh-token');

    const mockRefreshResponse = {
      token: 'new.access.token',
      refreshToken: 'new-rotated-refresh-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: mockUser,
    };

    service.refreshToken().subscribe((res) => {
      expect(res.token).toBe('new.access.token');
      expect(res.refreshToken).toBe('new-rotated-refresh-token');
    });

    const req = httpMock.expectOne(`${apiUrl}/refresh`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ refreshToken: 'existing-refresh-token' });
    req.flush(mockRefreshResponse);

    expect(localStorage.getItem('tix-token')).toBe('new.access.token');
    expect(localStorage.getItem('tix-refresh-token')).toBe('new-rotated-refresh-token');
  });

  /**
   * TEST CASE 6: Logout with refresh token notifies backend and cleans up storage.
   */
  it('logout — should post to /api/auth/logout with refreshToken and clear storage', () => {
    localStorage.setItem('tix-token', 'valid.token');
    localStorage.setItem('tix-refresh-token', 'valid-refresh-token');
    localStorage.setItem('tix-user', JSON.stringify(mockUser));

    service.logout();

    const req = httpMock.expectOne(`${apiUrl}/logout`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ refreshToken: 'valid-refresh-token' });
    req.flush(null);

    expect(localStorage.getItem('tix-token')).toBeNull();
    expect(localStorage.getItem('tix-refresh-token')).toBeNull();
    expect(localStorage.getItem('tix-user')).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });
});
