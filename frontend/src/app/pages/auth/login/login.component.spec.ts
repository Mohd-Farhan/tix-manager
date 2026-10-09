import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { UserService } from '../../../services/user.service';
import { ThemeService } from '../../../services/theme.service';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { UserRole } from '../../../models/user.model';
import { HttpErrorResponse } from '@angular/common/http';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  let mockAuthService: {
    login: ReturnType<typeof vi.fn>;
    getCurrentUser: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
    updateCurrentUser: ReturnType<typeof vi.fn>;
  };

  let mockToastService: {
    error: ReturnType<typeof vi.fn>;
    success: ReturnType<typeof vi.fn>;
    warning: ReturnType<typeof vi.fn>;
    info: ReturnType<typeof vi.fn>;
  };

  let mockRouter: {
    navigate: ReturnType<typeof vi.fn>;
  };

  let mockUserService: {
    updatePassword: ReturnType<typeof vi.fn>;
  };

  let mockThemeService: {
    isDark: ReturnType<typeof vi.fn>;
    toggleTheme: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    mockAuthService = {
      login: vi.fn(),
      getCurrentUser: vi.fn().mockReturnValue(null),
      logout: vi.fn(),
      updateCurrentUser: vi.fn(),
    };

    mockToastService = {
      error: vi.fn(),
      success: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    };

    mockRouter = {
      navigate: vi.fn(),
    };

    mockUserService = {
      updatePassword: vi.fn(),
    };

    mockThemeService = {
      isDark: vi.fn().mockReturnValue(false),
      toggleTheme: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: ToastService, useValue: mockToastService },
        { provide: Router, useValue: mockRouter },
        { provide: UserService, useValue: mockUserService },
        { provide: ThemeService, useValue: mockThemeService },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of({}),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize login form with empty inputs', () => {
    expect(component).toBeTruthy();
    expect(component.loginForm.valid).toBe(false);
    expect(component.loginForm.get('username')?.value).toBe('');
    expect(component.loginForm.get('password')?.value).toBe('');
  });

  it('should display error immediately when submitting incorrect credentials (401)', () => {
    component.loginForm.patchValue({
      username: 'agent_user',
      password: 'wrong_password',
    });

    const errorResponse = new HttpErrorResponse({
      status: 401,
      statusText: 'Unauthorized',
      error: { message: 'Bad credentials' },
    });

    mockAuthService.login.mockReturnValue(throwError(() => errorResponse));

    component.onSubmit();

    // Verify error banner is populated and loading state reset immediately
    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toBe('Invalid username or password. Please try again.');
    expect(mockToastService.error).toHaveBeenCalledWith(
      'Invalid username or password. Please try again.',
      'Authentication Failed'
    );

    // Verify DOM rendering
    const compiled = fixture.nativeElement as HTMLElement;
    const errorBanner = compiled.querySelector('#login-error-banner');
    expect(errorBanner).toBeTruthy();
    expect(errorBanner?.textContent).toContain('Invalid username or password');
  });

  it('should display 429 lockout error immediately when rate-limited', () => {
    component.loginForm.patchValue({
      username: 'admin',
      password: 'wrong_password',
    });

    const errorResponse = new HttpErrorResponse({
      status: 429,
      statusText: 'Too Many Requests',
      error: { message: 'Too many failed attempts. Locked for 15 minutes.' },
    });

    mockAuthService.login.mockReturnValue(throwError(() => errorResponse));

    component.onSubmit();

    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toBe('Too many failed attempts. Locked for 15 minutes.');
    expect(mockToastService.error).toHaveBeenCalledWith(
      'Too many failed attempts. Locked for 15 minutes.',
      'Authentication Failed'
    );
  });

  it('should navigate to dashboard on successful authentication', () => {
    component.loginForm.patchValue({
      username: 'support_agent',
      password: 'ValidPassword123!',
    });

    mockAuthService.login.mockReturnValue(
      of({
        token: 'mock-jwt-token',
        user: {
          id: 5,
          username: 'support_agent',
          role: UserRole.SUPPORT_AGENT,
          mustChangePassword: false,
        },
      })
    );

    component.onSubmit();

    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toBe('');
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/agent/dashboard']);
  });
});
