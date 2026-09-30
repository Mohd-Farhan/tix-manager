import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UserManagementComponent } from './user-management.component';
import { UserService } from '../../../services/user.service';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../shared/services/toast.service';
import { provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { User, UserRole } from '../../../models/user.model';
import { describe, it, expect, vi, beforeEach } from 'vitest';

describe('UserManagementComponent (Skeleton Loading)', () => {
  let component: UserManagementComponent;
  let fixture: ComponentFixture<UserManagementComponent>;
  let usersSubject: Subject<User[]>;
  let userServiceSpy: { getAllUsersAdmin: ReturnType<typeof vi.fn>; softDeleteUser: ReturnType<typeof vi.fn> };
  let authServiceSpy: { getCurrentUser: ReturnType<typeof vi.fn> };
  let toastServiceSpy: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };

  const mockUsers: User[] = [
    {
      id: 1,
      username: 'farhan_dev',
      email: 'farhan@example.com',
      role: UserRole.ADMIN,
      deleted: false,
      createdAt: '2026-09-01T10:00:00Z',
    },
    {
      id: 2,
      username: 'agent_smith',
      email: 'smith@support.com',
      role: UserRole.SUPPORT_AGENT,
      deleted: false,
      createdAt: '2026-09-02T11:00:00Z',
    },
  ];

  beforeEach(async () => {
    usersSubject = new Subject<User[]>();
    userServiceSpy = {
      getAllUsersAdmin: vi.fn().mockReturnValue(usersSubject),
      softDeleteUser: vi.fn().mockReturnValue(new Subject<void>()),
    };
    authServiceSpy = {
      getCurrentUser: vi.fn().mockReturnValue({
        id: 99,
        username: 'sysadmin',
        email: 'sysadmin@support.com',
        role: UserRole.SYSTEM_ADMIN,
      }),
    };
    toastServiceSpy = {
      success: vi.fn(),
      error: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [UserManagementComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: userServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserManagementComponent);
    component = fixture.componentInstance;
  });

  it('should create the UserManagementComponent', () => {
    expect(component).toBeTruthy();
  });

  it('should render shimmer skeleton table rows when isLoading is true', () => {
    fixture.detectChanges(); // ngOnInit triggers loadUsers with pending usersSubject

    expect(component.isLoading).toBe(true);
    const skeletonTable = fixture.nativeElement.querySelector('.skeleton-table');
    const skeletonRows = fixture.nativeElement.querySelectorAll('.skeleton-row');
    const emptyState = fixture.nativeElement.querySelector('app-empty-state');

    expect(skeletonTable).toBeTruthy();
    expect(skeletonRows.length).toBe(6);
    expect(emptyState).toBeNull();
  });

  it('should render actual user rows when isLoading is false', () => {
    fixture.detectChanges();
    usersSubject.next(mockUsers);
    fixture.detectChanges();

    expect(component.isLoading).toBe(false);
    const skeletonTable = fixture.nativeElement.querySelector('.skeleton-table');
    const userRows = fixture.nativeElement.querySelectorAll('.user-row:not(.skeleton-row)');

    expect(skeletonTable).toBeNull();
    expect(userRows.length).toBe(2);
  });

  it('should display empty state only when isLoading is false and no users match', () => {
    fixture.detectChanges();
    usersSubject.next([]);
    fixture.detectChanges();

    expect(component.isLoading).toBe(false);
    const skeletonTable = fixture.nativeElement.querySelector('.skeleton-table');
    const emptyState = fixture.nativeElement.querySelector('app-empty-state');

    expect(skeletonTable).toBeNull();
    expect(emptyState).toBeTruthy();
  });

  describe('Confirm Dialog for Deactivate/Reactivate', () => {
    it('should open confirm dialog instead of native confirm when toggleStatus is invoked', () => {
      fixture.detectChanges();
      usersSubject.next(mockUsers);
      fixture.detectChanges();

      expect(component.confirmDialog.visible).toBe(false);

      // Trigger toggleStatus for user #2 (agent_smith, active)
      component.toggleStatus(2);

      expect(component.confirmDialog.visible).toBe(true);
      expect(component.confirmDialog.targetUserId).toBe(2);
      expect(component.confirmDialog.title).toBe('Deactivate User');
      expect(component.confirmDialog.variant).toBe('danger');
      expect(userServiceSpy.softDeleteUser).not.toHaveBeenCalled();
    });

    it('should invoke softDeleteUser on confirm and reset dialog state', () => {
      fixture.detectChanges();
      usersSubject.next(mockUsers);
      fixture.detectChanges();

      component.toggleStatus(2);
      expect(component.confirmDialog.visible).toBe(true);

      component.onConfirmToggleStatus();

      expect(userServiceSpy.softDeleteUser).toHaveBeenCalledWith(2);
      expect(component.confirmDialog.visible).toBe(false);
      expect(component.confirmDialog.targetUserId).toBeNull();
    });

    it('should dismiss dialog on cancel without invoking softDeleteUser', () => {
      fixture.detectChanges();
      usersSubject.next(mockUsers);
      fixture.detectChanges();

      component.toggleStatus(2);
      expect(component.confirmDialog.visible).toBe(true);

      component.onCancelConfirmToggle();

      expect(component.confirmDialog.visible).toBe(false);
      expect(component.confirmDialog.targetUserId).toBeNull();
      expect(userServiceSpy.softDeleteUser).not.toHaveBeenCalled();
    });
  });
});
