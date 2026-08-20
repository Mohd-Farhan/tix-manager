import { Component, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-system-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './system-settings.component.html',
  styleUrl: './system-settings.component.css',
})
export class SystemSettingsComponent {
  private cdr = inject(ChangeDetectorRef);

  // General
  systemName = 'TixManager';
  supportEmail = 'support@tixmanager.com';
  defaultPriority = 'MEDIUM';

  // Notifications
  globalEmailNotifications = true;
  autoAssignment = false;
  escalationAfterHours = 48;
  notifyAdminOnEscalation = true;

  // Security
  minPasswordLength = 8;
  requireSpecialChars = true;
  sessionTimeoutMinutes = 60;
  enforce2FA = false;

  // Toast
  toastMessage = '';
  toastVisible = false;

  saveGeneral(): void {
    this.showToast('General settings saved successfully.');
  }

  saveNotifications(): void {
    this.showToast('Notification settings saved successfully.');
  }

  saveSecurity(): void {
    this.showToast('Security settings saved successfully.');
  }

  private showToast(message: string): void {
    this.toastMessage = message;
    this.toastVisible = true;
    this.cdr.detectChanges();
    setTimeout(() => {
      this.toastVisible = false;
      this.cdr.detectChanges();
    }, 3000);
  }
}
