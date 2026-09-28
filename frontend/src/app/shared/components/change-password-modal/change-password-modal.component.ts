import { Component, Input, Output, EventEmitter, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
import { UserService } from '../../../services/user.service';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';

function passwordMatchValidator(group: AbstractControl): { [key: string]: boolean } | null {
  const newPwd = group.get('newPassword')?.value;
  const confirmPwd = group.get('confirmPassword')?.value;
  if (newPwd && confirmPwd && newPwd !== confirmPwd) {
    return { passwordMismatch: true };
  }
  return null;
}

@Component({
  selector: 'app-change-password-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-password-modal.component.html',
  styleUrl: './change-password-modal.component.css',
})
export class ChangePasswordModalComponent {
  @Input() isOpen = false;
  @Input() userId?: number;
  @Output() closed = new EventEmitter<void>();
  @Output() passwordChanged = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private toast = inject(ToastService);

  showCurrentPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  passwordForm: FormGroup = this.fb.group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: [
        '',
        [
          Validators.required,
          Validators.pattern(/^(?=.*[A-Z])(?=.*\d)(?=.*[@#$!%*?&])[A-Za-z\d@#$!%*?&]{8,}$/),
        ],
      ],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordMatchValidator }
  );

  get f() {
    return this.passwordForm.controls;
  }

  get newPasswordValue(): string {
    return this.f['newPassword']?.value || '';
  }

  get confirmPasswordValue(): string {
    return this.f['confirmPassword']?.value || '';
  }

  // Real-time password criteria indicators
  get hasMinLength(): boolean {
    return this.newPasswordValue.length >= 8;
  }

  get hasUppercase(): boolean {
    return /[A-Z]/.test(this.newPasswordValue);
  }

  get hasDigit(): boolean {
    return /\d/.test(this.newPasswordValue);
  }

  get hasSpecialChar(): boolean {
    return /[@#$!%*?&]/.test(this.newPasswordValue);
  }

  get passwordsMatch(): boolean {
    return (
      this.confirmPasswordValue.length > 0 &&
      this.newPasswordValue === this.confirmPasswordValue
    );
  }

  toggleCurrentPassword(): void {
    this.showCurrentPassword = !this.showCurrentPassword;
  }

  toggleNewPassword(): void {
    this.showNewPassword = !this.showNewPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  onSubmit(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const currentUserId = this.userId || this.authService.getCurrentUser()?.id;
    if (!currentUserId) {
      this.errorMessage = 'Unable to identify active user session. Please re-login.';
      return;
    }

    const { currentPassword, newPassword } = this.passwordForm.value;

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.userService.updatePassword(currentUserId, currentPassword, newPassword).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Password updated successfully! Keeping your account secure.';
        this.toast.success('Password updated successfully.');
        this.passwordChanged.emit();
        this.passwordForm.reset();

        setTimeout(() => {
          this.close();
        }, 1200);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage =
          err.error?.message ||
          err.error?.detail ||
          err.error ||
          'Failed to update password. Please check your current password.';
      },
    });
  }

  close(): void {
    if (this.isLoading) return;
    this.isOpen = false;
    this.passwordForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
    this.showCurrentPassword = false;
    this.showNewPassword = false;
    this.showConfirmPassword = false;
    this.closed.emit();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isOpen) {
      this.close();
    }
  }
}
