import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MockDataService, SystemUser } from '../../../services/mock-data.service';
import { UserRole } from '../../../models/user.model';

@Component({
  selector: 'app-admin-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.css',
})
export class UserManagementComponent implements OnInit {
  private mockData = inject(MockDataService);
  private cdr = inject(ChangeDetectorRef);

  users: SystemUser[] = [];
  filteredUsers: SystemUser[] = [];
  searchTerm = '';
  roleFilter = 'ALL';
  showAddUserModal = false;

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

  private loadUsers(): void {
    this.users = this.mockData.getAllUsers();
    this.applyFilters();
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
    this.mockData.addUser({ username: this.newUsername.trim(), email: this.newEmail.trim(), role: this.newRole });
    this.formError = '';
    this.formSuccess = `User "${this.newUsername.trim()}" created successfully.`;
    this.newUsername = '';
    this.newEmail = '';
    this.newRole = UserRole.CUSTOMER;
    this.loadUsers();
    this.cdr.detectChanges();
    setTimeout(() => this.closeAddUserModal(), 1200);
  }

  changeRole(userId: number, newRole: UserRole): void {
    this.mockData.updateUserRole(userId, newRole);
    this.loadUsers();
  }

  toggleStatus(userId: number): void {
    this.mockData.toggleUserStatus(userId);
    this.loadUsers();
  }

  getRoleLabel(role: UserRole): string {
    return { CUSTOMER: 'Customer', SUPPORT_AGENT: 'Support Agent', ADMIN: 'Admin' }[role] ?? role;
  }

  getRoleClass(role: UserRole): string {
    return { CUSTOMER: 'role-customer', SUPPORT_AGENT: 'role-agent', ADMIN: 'role-admin' }[role] ?? '';
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }
}
