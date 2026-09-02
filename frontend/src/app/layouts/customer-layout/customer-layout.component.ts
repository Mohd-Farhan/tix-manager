import { Component, OnInit, OnDestroy, inject, HostListener, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { TicketService } from '../../services/ticket.service';
import { UserService } from '../../services/user.service';
import { User, UserRole } from '../../models/user.model';
import { TicketPriority } from '../../models/ticket.model';
import { LogoComponent } from '../../shared/components/logo/logo.component';

export type ActiveModalType = 'profile' | 'preferences' | 'password' | 'create-ticket' | null;

@Component({
  selector: 'app-customer-layout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterOutlet, RouterLink, RouterLinkActive, LogoComponent],
  templateUrl: './customer-layout.component.html',
  styleUrl: './customer-layout.component.css',
})
export class CustomerLayoutComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private ticketService = inject(TicketService);
  private userService = inject(UserService);
  private router = inject(Router);
  private elementRef = inject(ElementRef);
  private cdr = inject(ChangeDetectorRef);
  private subscription = new Subscription();

  user: User = this.authService.getCurrentUser() || {
    id: 1,
    username: 'Customer',
    email: '',
    role: UserRole.CUSTOMER
  };
  isDark = false;
  sidebarOpen = false;
  profileDropdownOpen = false;

  // Active modal window (top center of content area)
  activeModal: ActiveModalType = null;

  // Preferences state
  emailOnStatusChange = true;
  emailOnReply = true;
  soundNotifications = true;

  // Password form state
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  passwordMessage = '';
  passwordError = false;

  // Create Ticket form state
  ticketTitle = '';
  ticketDescription = '';
  ticketPriority: TicketPriority = TicketPriority.MEDIUM;
  isCreatingTicket = false;
  ticketCreatedSuccess = false;
  ticketErrorMessage = '';

  priorities = [
    { value: TicketPriority.LOW, label: 'Low', desc: 'Minor issue, no urgency' },
    { value: TicketPriority.MEDIUM, label: 'Medium', desc: 'Standard support request' },
    { value: TicketPriority.HIGH, label: 'High', desc: 'Critical issue, urgent attention' },
  ];

  navItems = [
    {
      label: 'Dashboard',
      route: '/customer/dashboard',
      icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>`,
    },
    {
      label: 'My Tickets',
      route: '/customer/tickets',
      icon: `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><line x1="10" y1="9" x2="8" y2="9"/></svg>`,
    },
  ];

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentUser();
    if (currentUser) {
      this.user = currentUser;
    }
    const savedTheme = localStorage.getItem('tix-theme');
    this.isDark = savedTheme === 'dark';
    this.applyTheme();

    this.subscription.add(
      this.ticketService.openCreateTicket$.subscribe(() => {
        this.openModal('create-ticket');
        this.cdr.detectChanges();
      })
    );
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
    if (event) {
      event.stopPropagation();
    }
    this.profileDropdownOpen = !this.profileDropdownOpen;
    this.cdr.detectChanges();
  }

  closeProfileDropdown(): void {
    this.profileDropdownOpen = false;
    this.cdr.detectChanges();
  }

  openModal(type: ActiveModalType): void {
    this.closeProfileDropdown();
    this.activeModal = type;
    this.passwordMessage = '';
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.ticketTitle = '';
    this.ticketDescription = '';
    this.ticketPriority = TicketPriority.MEDIUM;
    this.ticketErrorMessage = '';
    this.ticketCreatedSuccess = false;
    this.isCreatingTicket = false;
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
    return this.user?.username?.charAt(0).toUpperCase() ?? 'U';
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

    this.userService.updatePassword(this.user.id, this.currentPassword, this.newPassword).subscribe({
      next: () => {
        this.showPasswordMessage('Password updated successfully.', false);
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.cdr.detectChanges();

        setTimeout(() => {
          this.closeModal();
        }, 1000);
      },
      error: (err) => {
        this.showPasswordMessage(err.error?.message || 'Failed to update password.', true);
        this.cdr.detectChanges();
      }
    });
  }

  private showPasswordMessage(msg: string, isError: boolean): void {
    this.passwordMessage = msg;
    this.passwordError = isError;
    this.cdr.detectChanges();
  }

  submitCreateTicket(): void {
    if (!this.ticketTitle.trim()) {
      this.ticketErrorMessage = 'Please enter a ticket title.';
      this.cdr.detectChanges();
      return;
    }

    if (!this.ticketDescription.trim()) {
      this.ticketErrorMessage = 'Please provide a detailed description.';
      this.cdr.detectChanges();
      return;
    }

    this.ticketErrorMessage = '';
    this.isCreatingTicket = true;
    this.cdr.detectChanges();

    this.ticketService.createTicket({
      title: this.ticketTitle.trim(),
      description: this.ticketDescription.trim(),
      priority: this.ticketPriority,
      customerId: this.user.id,
    }).subscribe({
      next: () => {
        this.isCreatingTicket = false;
        this.ticketCreatedSuccess = true;
        this.cdr.detectChanges();

        setTimeout(() => {
          this.closeModal();
        }, 700);
      },
      error: (err) => {
        this.isCreatingTicket = false;
        this.ticketErrorMessage = err.error?.message || err.error || 'Failed to create ticket. Please try again.';
        this.cdr.detectChanges();
      }
    });
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
    if (this.activeModal) {
      this.closeModal();
    }
    this.profileDropdownOpen = false;
    this.sidebarOpen = false;
    this.cdr.detectChanges();
  }

  private applyTheme(): void {
    document.documentElement.setAttribute('data-theme', this.isDark ? 'dark' : 'light');
  }
}
