import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../services/user.service';
import { AuthService } from '../../../services/auth.service';
import { User, UserRole, BulkUploadResult } from '../../../models/user.model';
import { SearchBoxComponent } from '../../../shared/components/search-box/search-box.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { ModalShellComponent } from '../../../shared/components/modal-shell/modal-shell.component';
import { PaginationComponent, PageSizeOption } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast.service';

import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-admin-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, SearchBoxComponent, EmptyStateComponent, ModalShellComponent, PaginationComponent],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.css',
})
export class UserManagementComponent implements OnInit {
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private toast = inject(ToastService);
  private cdr = inject(ChangeDetectorRef);

  currentUser: User | null = null;
  users: User[] = [];
  filteredUsers: User[] = [];
  searchTerm = '';
  roleFilter = 'ALL';
  showAddUserModal = false;
  showBulkModal = false;
  isLoading = true;

  // Pagination state
  currentPage = 1;
  pageSize: PageSizeOption = 10;
  readonly pageSizeOptions: PageSizeOption[] = [10, 20, 50, 100, 'ALL'];

  get paginatedUsers(): User[] {
    if (this.pageSize === 'ALL') return this.filteredUsers;
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredUsers.slice(start, start + this.pageSize);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.cdr.detectChanges();
  }

  onPageSizeChange(size: PageSizeOption): void {
    this.pageSize = size;
    this.currentPage = 1;
    this.cdr.detectChanges();
  }

  // Add user form
  newUsername = '';
  newEmail = '';
  newPassword = '';
  newRole: UserRole = UserRole.CUSTOMER;
  formError = '';
  formSuccess = '';

  // Bulk upload
  selectedFile: File | null = null;
  isBulkUploading = false;
  bulkResult: BulkUploadResult | null = null;
  bulkError = '';

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadUsers();
  }

  get isSystemAdmin(): boolean {
    return this.currentUser?.role === UserRole.SYSTEM_ADMIN;
  }

  get availableRolesForCreation(): { value: UserRole; label: string }[] {
    if (this.isSystemAdmin) {
      return [
        { value: UserRole.CUSTOMER, label: 'Customer' },
        { value: UserRole.SUPPORT_AGENT, label: 'Support Agent' },
        { value: UserRole.ADMIN, label: 'Admin' },
        { value: UserRole.SYSTEM_ADMIN, label: 'System Admin' },
      ];
    }
    return [
      { value: UserRole.CUSTOMER, label: 'Customer' },
      { value: UserRole.SUPPORT_AGENT, label: 'Support Agent' },
    ];
  }

  canModifyUser(user: User): boolean {
    if (this.currentUser?.id === user.id) {
      return false;
    }
    if (!this.isSystemAdmin && (user.role === UserRole.ADMIN || user.role === UserRole.SYSTEM_ADMIN)) {
      return false;
    }
    return true;
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
    this.currentPage = 1;
  }

  openAddUserModal(): void {
    this.showAddUserModal = true;
    this.newUsername = '';
    this.newEmail = '';
    this.newPassword = '';
    this.newRole = UserRole.CUSTOMER;
    this.formError = '';
    this.formSuccess = '';
    this.cdr.detectChanges();
  }

  closeAddUserModal(): void {
    this.showAddUserModal = false;
    this.cdr.detectChanges();
  }

  openBulkModal(): void {
    this.showBulkModal = true;
    this.selectedFile = null;
    this.bulkResult = null;
    this.bulkError = '';
    this.cdr.detectChanges();
  }

  closeBulkModal(): void {
    this.showBulkModal = false;
    this.selectedFile = null;
    this.cdr.detectChanges();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.bulkError = '';
    }
  }

  uploadCsv(): void {
    if (!this.selectedFile) {
      this.bulkError = 'Please choose a CSV file to upload.';
      return;
    }

    this.isBulkUploading = true;
    this.bulkError = '';
    this.bulkResult = null;

    this.userService.bulkUploadUsers(this.selectedFile).subscribe({
      next: (res) => {
        this.isBulkUploading = false;
        this.bulkResult = res;
        this.toast.success(`Bulk upload complete: ${res.successCount} created, ${res.failureCount} failed.`);
        this.loadUsers();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isBulkUploading = false;
        this.bulkError = err.error?.message || err.error || 'Bulk upload failed.';
        this.cdr.detectChanges();
      }
    });
  }

  downloadSampleCsv(): void {
    const csvContent = 'username,email,password,role\n' +
      'priya_agent,priya@tixmanager.com,,SUPPORT_AGENT\n' +
      'alex_dev,alex@example.com,,CUSTOMER\n';
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'sample_users.csv';
    link.click();
  }

  addUser(): void {
    if (!this.newUsername.trim() || !this.newEmail.trim()) {
      this.formError = 'Please fill in all required fields.';
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
      password: this.newPassword.trim() || undefined,
      role: this.newRole,
    };

    this.userService.createUser(payload).subscribe({
      next: (created) => {
        this.formError = '';
        this.formSuccess = `User "${created.username}" created successfully.`;
        this.toast.success(`User "${created.username}" created.`);
        this.newUsername = '';
        this.newEmail = '';
        this.newPassword = '';
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
    if (!user) return;

    if (!this.canModifyUser(user)) {
      this.toast.error('Insufficient permissions to modify this user.');
      return;
    }

    this.userService.updateProfile(userId, { role: newRole }).subscribe({
      next: () => {
        user.role = newRole;
        this.toast.success(`User role updated to ${this.getRoleLabel(newRole)}.`);
        this.applyFilters();
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Failed to update user role.');
        this.loadUsers();
      }
    });
  }

  toggleStatus(userId: number): void {
    const user = this.users.find(u => u.id === userId);
    if (user && !this.canModifyUser(user)) {
      this.toast.error('Cannot modify this user account.');
      return;
    }

    const action = user?.status === 'active' ? 'deactivate' : 'reactivate';
    if (!confirm(`Are you sure you want to ${action} user "${user?.username}"? This action can be reversed by an administrator.`)) {
      return;
    }

    this.userService.softDeleteUser(userId).subscribe({
      next: () => {
        this.toast.success(`User #${userId} ${action}d successfully.`);
        this.loadUsers();
      },
      error: (err) => {
        this.toast.error(err.error?.message || `Failed to update status for user #${userId}.`);
      }
    });
  }

  getRoleLabel(role: UserRole): string {
    return {
      CUSTOMER: 'Customer',
      SUPPORT_AGENT: 'Support Agent',
      ADMIN: 'Admin',
      SYSTEM_ADMIN: 'System Admin'
    }[role] ?? role;
  }

  getRoleClass(role: UserRole): string {
    return {
      CUSTOMER: 'role-customer',
      SUPPORT_AGENT: 'role-agent',
      ADMIN: 'role-admin',
      SYSTEM_ADMIN: 'role-sysadmin'
    }[role] ?? '';
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }
}
