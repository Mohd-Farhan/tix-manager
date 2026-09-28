import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChangePasswordModalComponent } from './change-password-modal.component';
import { UserService } from '../../../services/user.service';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { of, throwError } from 'rxjs';
import { UserRole } from '../../../models/user.model';
import { describe, it, expect, vi, beforeEach } from 'vitest';

describe('ChangePasswordModalComponent', () => {
  let component: ChangePasswordModalComponent;
  let fixture: ComponentFixture<ChangePasswordModalComponent>;
  let userServiceSpy: { updatePassword: ReturnType<typeof vi.fn> };
  let authServiceSpy: { getCurrentUser: ReturnType<typeof vi.fn> };
  let toastServiceSpy: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    userServiceSpy = {
      updatePassword: vi.fn(),
    };
    authServiceSpy = {
      getCurrentUser: vi.fn().mockReturnValue({
        id: 42,
        username: 'test_agent',
        email: 'agent@support.com',
        role: UserRole.SUPPORT_AGENT,
      }),
    };
    toastServiceSpy = {
      success: vi.fn(),
      error: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [ChangePasswordModalComponent],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ChangePasswordModalComponent);
    component = fixture.componentInstance;
    component.isOpen = true;
    fixture.detectChanges();
  });

  it('should create the change password modal component', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with empty values and invalid status', () => {
    expect(component.passwordForm.valid).toBe(false);
    expect(component.passwordForm.get('currentPassword')?.value).toBe('');
    expect(component.passwordForm.get('newPassword')?.value).toBe('');
    expect(component.passwordForm.get('confirmPassword')?.value).toBe('');
  });

  it('should validate complexity regex for new password', () => {
    const newPwdControl = component.passwordForm.get('newPassword');

    // Too short (<8)
    newPwdControl?.setValue('Ab1@');
    expect(newPwdControl?.valid).toBe(false);

    // No uppercase
    newPwdControl?.setValue('password123@');
    expect(newPwdControl?.valid).toBe(false);

    // No number
    newPwdControl?.setValue('Password@@@');
    expect(newPwdControl?.valid).toBe(false);

    // No special character
    newPwdControl?.setValue('Password1234');
    expect(newPwdControl?.valid).toBe(false);

    // Valid
    newPwdControl?.setValue('StrongPass123!');
    expect(newPwdControl?.valid).toBe(true);
  });

  it('should validate that confirmPassword matches newPassword', () => {
    component.passwordForm.patchValue({
      currentPassword: 'OldPassword123!',
      newPassword: 'NewPassword123!',
      confirmPassword: 'DifferentPassword123!',
    });

    expect(component.passwordForm.errors?.['passwordMismatch']).toBe(true);
    expect(component.passwordForm.valid).toBe(false);

    component.passwordForm.patchValue({
      confirmPassword: 'NewPassword123!',
    });

    expect(component.passwordForm.errors).toBeNull();
    expect(component.passwordForm.valid).toBe(true);
  });

  it('should toggle password visibility flags', () => {
    expect(component.showCurrentPassword).toBe(false);
    component.toggleCurrentPassword();
    expect(component.showCurrentPassword).toBe(true);

    expect(component.showNewPassword).toBe(false);
    component.toggleNewPassword();
    expect(component.showNewPassword).toBe(true);

    expect(component.showConfirmPassword).toBe(false);
    component.toggleConfirmPassword();
    expect(component.showConfirmPassword).toBe(true);
  });

  it('should call userService.updatePassword on valid submit and emit passwordChanged', () => {
    vi.useFakeTimers();
    const emitSpy = vi.spyOn(component.passwordChanged, 'emit');
    userServiceSpy.updatePassword.mockReturnValue(of(undefined as unknown as void));

    component.passwordForm.setValue({
      currentPassword: 'CurrentPassword123!',
      newPassword: 'BrandNewPassword123!',
      confirmPassword: 'BrandNewPassword123!',
    });

    component.onSubmit();

    expect(userServiceSpy.updatePassword).toHaveBeenCalledWith(
      42,
      'CurrentPassword123!',
      'BrandNewPassword123!'
    );
    expect(component.successMessage).toContain('Password updated successfully');
    expect(emitSpy).toHaveBeenCalled();
    expect(toastServiceSpy.success).toHaveBeenCalled();

    vi.advanceTimersByTime(1300);
    expect(component.isOpen).toBe(false);
    vi.useRealTimers();
  });

  it('should display error message on userService failure', () => {
    userServiceSpy.updatePassword.mockReturnValue(
      throwError(() => ({ error: { message: 'Current password does not match.' } }))
    );

    component.passwordForm.setValue({
      currentPassword: 'WrongPassword123!',
      newPassword: 'BrandNewPassword123!',
      confirmPassword: 'BrandNewPassword123!',
    });

    component.onSubmit();

    expect(userServiceSpy.updatePassword).toHaveBeenCalled();
    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toBe('Current password does not match.');
  });

  it('should emit closed and reset state when close() is called', () => {
    const closeSpy = vi.spyOn(component.closed, 'emit');
    component.errorMessage = 'Some error';
    component.close();

    expect(component.isOpen).toBe(false);
    expect(component.errorMessage).toBe('');
    expect(closeSpy).toHaveBeenCalled();
  });
});
