import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { UserService } from '../../../services/user.service';
import { ToastService } from '../../../services/toast.service';
import { UserRole } from '../../../models/user.model';
import { LogoComponent } from '../../../shared/components/logo/logo.component';

/**
 * Shared NIST SP 800-63B password complexity regex.
 * Minimum 8 characters, at least 1 uppercase letter, 1 digit, and 1 special symbol.
 */
const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*\d)(?=.*[@#$!%*?&])[A-Za-z\d@#$!%*?&]{8,}$/;

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, LogoComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private toast = inject(ToastService);

  loginForm!: FormGroup;
  changePasswordForm!: FormGroup;

  showPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;

  isLoading = false;
  errorMessage = '';
  isDark = false;

  // NIST first-login password remediation state
  forceChangeMode = false;
  tempUserId: number | null = null;
  tempRole: UserRole | null = null;

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });

    this.changePasswordForm = this.fb.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
      confirmPassword: ['', [Validators.required]],
    });

    // Check if redirected due to mandatory password change
    this.route.queryParams.subscribe((params) => {
      if (params['forcePasswordChange'] === 'true') {
        const currentUser = this.authService.getCurrentUser();
        if (currentUser && currentUser.mustChangePassword) {
          this.forceChangeMode = true;
          this.tempUserId = currentUser.id;
          this.tempRole = currentUser.role;
        }
      }
    });

    const savedTheme = localStorage.getItem('tix-theme');
    this.isDark = savedTheme === 'dark';
    document.documentElement.setAttribute('data-theme', this.isDark ? 'dark' : 'light');
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleNewPassword(): void {
    this.showNewPassword = !this.showNewPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  toggleTheme(): void {
    this.isDark = !this.isDark;
    document.documentElement.setAttribute('data-theme', this.isDark ? 'dark' : 'light');
    localStorage.setItem('tix-theme', this.isDark ? 'dark' : 'light');
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const { username, password } = this.loginForm.value;

    this.authService.login({ username, password }).subscribe({
      next: (res) => {
        this.isLoading = false;

        // NIST SP 800-63B: Force first-time password personalization
        if (res.user?.mustChangePassword) {
          this.forceChangeMode = true;
          this.tempUserId = res.user.id;
          this.tempRole = res.user.role;
          this.changePasswordForm.patchValue({ currentPassword: password });
          return;
        }

        this.redirectToRole(res.user?.role);
      },
      error: (err) => {
        this.isLoading = false;
        // OWASP ASVS v4.0 §2.2.1: Friendly messaging for HTTP 429 lockout
        if (err.status === 429) {
          this.errorMessage = err.error?.message || 'Too many failed login attempts. Access is locked for 15 minutes.';
        } else if (err.status === 401) {
          this.errorMessage = 'Invalid username or password. Please try again.';
        } else {
          this.errorMessage = err.error?.message || err.error || 'Login failed. Please verify the backend is running.';
        }
      },
    });
  }

  onChangePasswordSubmit(): void {
    if (this.changePasswordForm.invalid) {
      this.changePasswordForm.markAllAsTouched();
      return;
    }

    const { currentPassword, newPassword, confirmPassword } = this.changePasswordForm.value;

    if (newPassword !== confirmPassword) {
      this.errorMessage = 'New password and confirm password do not match.';
      return;
    }

    if (!this.tempUserId) {
      this.errorMessage = 'Session context lost. Please log in again.';
      this.forceChangeMode = false;
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.userService.updatePassword(this.tempUserId, currentPassword, newPassword).subscribe({
      next: () => {
        this.isLoading = false;
        this.authService.updateCurrentUser({ mustChangePassword: false });
        this.toast.success('Password updated successfully. Welcome to TixManager!');
        this.forceChangeMode = false;
        this.redirectToRole(this.tempRole);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || err.error || 'Failed to update password. Please check your current password.';
      },
    });
  }

  cancelPasswordChange(): void {
    this.forceChangeMode = false;
    this.authService.logout();
  }

  private redirectToRole(role?: UserRole | null): void {
    if (role === UserRole.CUSTOMER) {
      this.router.navigate(['/customer/dashboard']);
    } else if (role === UserRole.SUPPORT_AGENT) {
      this.router.navigate(['/agent/dashboard']);
    } else if (role === UserRole.ADMIN || role === UserRole.SYSTEM_ADMIN) {
      this.router.navigate(['/admin/dashboard']);
    } else {
      this.router.navigate(['/']);
    }
  }

  get f() {
    return this.loginForm.controls;
  }

  get cp() {
    return this.changePasswordForm.controls;
  }
}
