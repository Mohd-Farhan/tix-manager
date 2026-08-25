import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../services/user.service';
import { AuthService } from '../../../services/auth.service';
import { User, UserRole } from '../../../models/user.model';
import { SearchBoxComponent } from '../../../shared/components/search-box/search-box.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { ModalShellComponent } from '../../../shared/components/modal-shell/modal-shell.component';
import { ToastService } from '../../../shared/services/toast.service';

@Component({
  selector: 'app-admin-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule, SearchBoxComponent, EmptyStateComponent, ModalShellComponent],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.css',
})
export class UserManagementComponent implements OnInit {
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private toast = inject(ToastService);
  private cdr = inject(ChangeDetectorRef);

  users: User[] = [];
  filteredUsers: User[] = [];
  searchTerm = '';
  roleFilter = 'ALL';
  showAddUserModal = false;
  isLoading = true;

  // Add user form
  newUsername = '';
  newEmail = '';
  newRole: UserRole = UserRole.CUSTOMER;
  formError = '';
  formSuccess = '';

  roles = [
    { value: UserRole.CUSTOMER, label: 'Customer' },
    { value: UserRole.SUPPORT_AGENT, label: 'Support Agent' },
    { value: UserRole.ADMIN, label: 'Admin' },
  ];

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading = true;
    this.userService.getAllUsersAdmin().subscribe({
      next: (users) => {
        this.users = users.map(u => ({
          ...u,
          status: u.deleted ? 'inactive' : 'active'
        }));
        this.applyFilters();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  onSearch(): void {
    this.applyFilters();
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  private applyFilters(): void {
    let result = [...this.users];
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(
        (u) => u.username.toLowerCase().includes(term) || u.email.toLowerCase().includes(term) || u.id.toString().includes(term)
      );
    }
    if (this.roleFilter !== 'ALL') {
      result = result.filter((u) => u.role === this.roleFilter);
    }
    this.filteredUsers = result;
  }

  openAddUserModal(): void {
    this.showAddUserModal = true;
    this.newUsername = '';
    this.newEmail = '';
    this.newRole = UserRole.CUSTOMER;
    this.formError = '';
    this.formSuccess = '';
    this.cdr.detectChanges();
  }

  closeAddUserModal(): void {
    this.showAddUserModal = false;
    this.cdr.detectChanges();
  }

  addUser(): void {
    if (!this.newUsername.trim() || !this.newEmail.trim()) {
      this.formError = 'Please fill in all fields.';
      this.formSuccess = '';
      return;
    }
    if (!this.newEmail.includes('@')) {
      this.formError = 'Please enter a valid email address.';
      this.formSuccess = '';
      return;
    }

    const payload = {
      username: this.newUsername.trim(),
      email: this.newEmail.trim(),
      password: 'password123',
      role: this.newRole,
    };

    this.authService.register(payload).subscribe({
      next: (created) => {
        this.formError = '';
        this.formSuccess = `User "${created.username}" created successfully.`;
        this.toast.success(`User "${created.username}" created with default password "password123".`);
        this.newUsername = '';
        this.newEmail = '';
        this.newRole = UserRole.CUSTOMER;
        this.loadUsers();
        this.cdr.detectChanges();
        setTimeout(() => this.closeAddUserModal(), 1200);
      },
      error: (err) => {
        this.formError = err.error?.message || err.error || 'Failed to create user.';
        this.formSuccess = '';
        this.cdr.detectChanges();
      }
    });
  }

  changeRole(userId: number, newRole: UserRole): void {
    const user = this.users.find(u => u.id === userId);
    if (user) {
      user.role = newRole;
      this.toast.info(`User role set to ${newRole}.`);
      this.applyFilters();
    }
  }

  toggleStatus(userId: number): void {
    this.userService.softDeleteUser(userId).subscribe({
      next: () => {
        this.toast.success(`User #${userId} status updated.`);
        this.loadUsers();
      },
      error: () => {
        this.toast.error(`Failed to update status for user #${userId}.`);
      }
    });
  }

  getRoleLabel(role: UserRole): string {
    return { CUSTOMER: 'Customer', SUPPORT_AGENT: 'Support Agent', ADMIN: 'Admin' }[role] ?? role;
  }

  getRoleClass(role: UserRole): string {
    return { CUSTOMER: 'role-customer', SUPPORT_AGENT: 'role-agent', ADMIN: 'role-admin' }[role] ?? '';
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }
}

