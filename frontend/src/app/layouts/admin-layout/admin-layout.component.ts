import { Component, OnInit, OnDestroy, inject, HostListener, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subscription } from 'rxjs';
import { MockDataService } from '../../services/mock-data.service';
import { User, UserRole } from '../../models/user.model';

export type AdminModalType = 'profile' | 'preferences' | 'password' | null;

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.css',
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  private mockData = inject(MockDataService);
  private router = inject(Router);
  private elementRef = inject(ElementRef);
  private cdr = inject(ChangeDetectorRef);
  private subscription = new Subscription();

  user!: User;
  isDark = false;
  sidebarOpen = false;
  profileDropdownOpen = false;

  activeModal: AdminModalType = null;

  // Preferences
  emailOnTicketCreate = true;
  emailOnEscalation = true;
  soundNotifications = true;
  dailyDigest = true;

  // Password form
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  passwordMessage = '';
  passwordError = false;

  navItems = [
    {
      label: 'Dashboard',
      route: '/admin/dashboard',
      icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>`,
    },
    {
      label: 'Users',
      route: '/admin/users',
      icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`,
    },
    {
      label: 'Tickets',
      route: '/admin/tickets',
      icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><line x1="10" y1="9" x2="8" y2="9"/></svg>`,
    },
    {
      label: 'Settings',
      route: '/admin/settings',
      icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>`,
    },
  ];

  ngOnInit(): void {
    this.mockData.switchToAdmin();
    this.user = this.mockData.getCurrentUser();
    const savedTheme = localStorage.getItem('tix-theme');
    this.isDark = savedTheme === 'dark';
    this.applyTheme();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  toggleTheme(): void {
    this.isDark = !this.isDark;
    this.applyTheme();
    localStorage.setItem('tix-theme', this.isDark ? 'dark' : 'light');
    this.cdr.detectChanges();
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
    this.cdr.detectChanges();
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
    this.cdr.detectChanges();
  }

  toggleProfileDropdown(event?: Event): void {
    if (event) event.stopPropagation();
    this.profileDropdownOpen = !this.profileDropdownOpen;
    this.cdr.detectChanges();
  }

  closeProfileDropdown(): void {
    this.profileDropdownOpen = false;
    this.cdr.detectChanges();
  }

  openModal(type: AdminModalType): void {
    this.closeProfileDropdown();
    this.activeModal = type;
    this.passwordMessage = '';
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.cdr.detectChanges();
  }

  closeModal(): void {
    this.activeModal = null;
    this.cdr.detectChanges();
  }

  logout(): void {
    this.closeProfileDropdown();
    this.closeModal();
    this.mockData.switchToCustomer();
    localStorage.removeItem('tix-token');
    this.router.navigate(['/auth/login']);
  }

  getUserInitial(): string {
    return this.user?.username?.charAt(0).toUpperCase() ?? 'A';
  }

  getRoleLabel(role: UserRole): string {
    const map: Record<string, string> = {
      CUSTOMER: 'Customer',
      SUPPORT_AGENT: 'Support Agent',
      ADMIN: 'Admin',
    };
    return map[role] ?? role;
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return 'Unknown';
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    });
  }

  updatePassword(): void {
    if (!this.currentPassword || !this.newPassword || !this.confirmPassword) {
      this.showPasswordMessage('Please fill all password fields.', true);
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.showPasswordMessage('New passwords do not match.', true);
      return;
    }
    if (this.newPassword.length < 8) {
      this.showPasswordMessage('Password must be at least 8 characters.', true);
      return;
    }
    this.showPasswordMessage('Password updated successfully.', false);
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.cdr.detectChanges();
    setTimeout(() => {
      if (!this.passwordError) this.closeModal();
    }, 1000);
  }

  private showPasswordMessage(msg: string, isError: boolean): void {
    this.passwordMessage = msg;
    this.passwordError = isError;
    this.cdr.detectChanges();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const profileDropdown = this.elementRef.nativeElement.querySelector('.profile-menu-container');
    if (profileDropdown && !profileDropdown.contains(target)) {
      this.profileDropdownOpen = false;
      this.cdr.detectChanges();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.activeModal) this.closeModal();
    this.profileDropdownOpen = false;
    this.sidebarOpen = false;
    this.cdr.detectChanges();
  }

  private applyTheme(): void {
    document.documentElement.setAttribute('data-theme', this.isDark ? 'dark' : 'light');
  }
}
