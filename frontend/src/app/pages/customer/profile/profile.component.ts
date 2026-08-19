import { Component, OnInit, inject, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MockDataService } from '../../../services/mock-data.service';
import { User, UserRole } from '../../../models/user.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
})
export class ProfileComponent implements OnInit, AfterViewInit {
  private mockData = inject(MockDataService);
  private route = inject(ActivatedRoute);

  user!: User;
  isDark = false;

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
    this.user = this.mockData.getCurrentUser();
    this.isDark = localStorage.getItem('tix-theme') === 'dark';
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
    this.isDark = !this.isDark;
    document.documentElement.setAttribute('data-theme', this.isDark ? 'dark' : 'light');
    localStorage.setItem('tix-theme', this.isDark ? 'dark' : 'light');
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

    // Mock success
    this.showPasswordMessage('Password updated successfully.', false);
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
  }

  private showPasswordMessage(msg: string, isError: boolean): void {
    this.passwordMessage = msg;
    this.passwordError = isError;
    setTimeout(() => {
      this.passwordMessage = '';
    }, 4000);
  }
}
