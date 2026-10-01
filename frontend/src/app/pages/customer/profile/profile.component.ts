import { Component, OnInit, inject, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ThemeService } from '../../../services/theme.service';
import { UserService } from '../../../services/user.service';
import { User, UserRole } from '../../../models/user.model';
import { ChangePasswordModalComponent } from '../../../shared/components/change-password-modal/change-password-modal.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, ChangePasswordModalComponent],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
})
export class ProfileComponent implements OnInit, AfterViewInit {
  private authService = inject(AuthService);
  private themeService = inject(ThemeService);
  private userService = inject(UserService);
  private route = inject(ActivatedRoute);

  user: User = this.authService.getCurrentUser() || {
    id: 1,
    username: 'Customer',
    email: '',
    role: UserRole.CUSTOMER
  };
  get isDark(): boolean {
    return this.themeService.isDark();
  }
  isChangePasswordModalOpen = false;

  // Password form (mock only)
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  passwordMessage = '';
  passwordError = false;

  // Preferences
  emailOnStatusChange = true;
  emailOnReply = true;

  ngOnInit(): void {
    const u = this.authService.getCurrentUser();
    if (u) this.user = u;
  }

  ngAfterViewInit(): void {
    this.route.fragment.subscribe((fragment) => {
      if (fragment) {
        setTimeout(() => {
          const el = document.getElementById(fragment + '-section');
          if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
            el.classList.add('highlight-section');
            setTimeout(() => el.classList.remove('highlight-section'), 1500);
          }
        }, 100);
      }
    });
  }

  getRoleLabel(role: UserRole): string {
    const map: Record<string, string> = {
      CUSTOMER: 'Customer',
      SUPPORT_AGENT: 'Support Agent',
      ADMIN: 'Admin',
    };
    return map[role] ?? role;
  }

  getUserInitial(): string {
    return this.user?.username?.charAt(0).toUpperCase() ?? 'U';
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return 'Unknown';
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    });
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
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
      },
      error: (err) => {
        this.showPasswordMessage(err.error?.message || 'Failed to update password.', true);
      }
    });
  }

  private showPasswordMessage(msg: string, isError: boolean): void {
    this.passwordMessage = msg;
    this.passwordError = isError;
    setTimeout(() => {
      this.passwordMessage = '';
    }, 4000);
  }
}
