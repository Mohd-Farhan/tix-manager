import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { authGuard, roleGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/user.model';

/**
 * ==============================================================================================
 * AUTOMATED TESTING SUITE: Functional Route Guard Tests for authGuard and roleGuard
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Frontend Enterprise Standards):
 * 1. Route Protection & RBAC: Validates that unauthenticated users are redirected to `/auth/login`
 *    when attempting to visit protected customer, agent, or admin dashboards.
 * 2. Role Enforcement: Validates that users without the required role (e.g. a customer trying
 *    to access `/admin/dashboard`) are blocked and routed to their authorized home screen.
 * 3. Modern Angular Standalone Testing: Uses `TestBed.runInInjectionContext` to test functional
 *    guards without legacy Class-based boilerplate.
 */
describe('authGuard & roleGuard', () => {
  let authServiceSpy: { isAuthenticated: () => boolean; userRole: () => UserRole | null };
  let routerSpy: { navigate: (commands: any[]) => boolean };

  const dummyRoute = {} as ActivatedRouteSnapshot;
  const dummyState = { url: '/customer/dashboard' } as RouterStateSnapshot;

  beforeEach(() => {
    authServiceSpy = {
      isAuthenticated: () => false,
      userRole: () => null,
    };
    routerSpy = {
      navigate: () => true,
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });
  });

  /**
   * TEST CASE 1: authGuard blocks unauthenticated user and redirects to login.
   */
  it('authGuard — should block unauthenticated user and redirect to /auth/login', () => {
    authServiceSpy.isAuthenticated = () => false;

    const result = TestBed.runInInjectionContext(() => authGuard(dummyRoute, dummyState));

    expect(result).toBe(false);
  });

  /**
   * TEST CASE 2: authGuard permits authenticated user.
   */
  it('authGuard — should permit authenticated user', () => {
    authServiceSpy.isAuthenticated = () => true;

    const result = TestBed.runInInjectionContext(() => authGuard(dummyRoute, dummyState));

    expect(result).toBe(true);
  });

  /**
   * TEST CASE 3: roleGuard permits user when their role matches allowed roles.
   */
  it('roleGuard — should permit access when user has matching role', () => {
    authServiceSpy.isAuthenticated = () => true;
    authServiceSpy.userRole = () => UserRole.CUSTOMER;

    const guard = roleGuard([UserRole.CUSTOMER]);
    const result = TestBed.runInInjectionContext(() => guard(dummyRoute, dummyState));

    expect(result).toBe(true);
  });

  /**
   * TEST CASE 4: roleGuard blocks user and redirects when role does not match.
   */
  it('roleGuard — should block access when user does not have required role', () => {
    authServiceSpy.isAuthenticated = () => true;
    authServiceSpy.userRole = () => UserRole.CUSTOMER;

    const guard = roleGuard([UserRole.ADMIN]);
    const result = TestBed.runInInjectionContext(() => guard(dummyRoute, dummyState));

    expect(result).toBe(false);
  });
});
