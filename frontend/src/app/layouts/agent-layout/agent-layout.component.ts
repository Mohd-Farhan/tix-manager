import { Component, OnInit, OnDestroy, inject, HostListener, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { User, UserRole } from '../../models/user.model';

export type AgentModalType = 'profile' | 'preferences' | 'password' | null;

@Component({
  selector: 'app-agent-layout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './agent-layout.component.html',
  styleUrl: './agent-layout.component.css',
})
export class AgentLayoutComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private router = inject(Router);
  private elementRef = inject(ElementRef);
  private cdr = inject(ChangeDetectorRef);
  private subscription = new Subscription();

  user: User = this.authService.getCurrentUser() || {
    id: 2,
    username: 'Agent',
    email: '',
    role: UserRole.SUPPORT_AGENT
  };
  isDark = false;
  sidebarOpen = false;
  profileDropdownOpen = false;

  // Modal
  activeModal: AgentModalType = null;

  // Preferences
  emailOnAssignment = true;
  emailOnCustomerReply = true;
  soundNotifications = true;
  autoRefreshQueue = true;

  // Password form
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  passwordMessage = '';
  passwordError = false;

  navItems: Array<{ label: string, route: string, icon: string, queryParams?: any }> = [
    {
      label: 'Dashboard',
      route: '/agent/dashboard',
      icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>`,
    },
    {
      label: 'Ticket Queue',
      route: '/agent/tickets',
      icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><line x1="10" y1="9" x2="8" y2="9"/></svg>`,
    }
  ];

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentUser();
    if (currentUser) {
      this.user = currentUser;
    }
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

  openModal(type: AgentModalType): void {
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
    this.authService.logout();
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
