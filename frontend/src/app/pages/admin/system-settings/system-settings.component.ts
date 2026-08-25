import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToggleSwitchComponent } from '../../../shared/components/toggle-switch/toggle-switch.component';
import { ToastService } from '../../../shared/services/toast.service';

@Component({
  selector: 'app-admin-system-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, ToggleSwitchComponent],
  templateUrl: './system-settings.component.html',
  styleUrl: './system-settings.component.css',
})
export class SystemSettingsComponent {
  private toast = inject(ToastService);

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

  saveGeneral(): void {
    this.toast.success('General settings saved successfully.');
  }

  saveNotifications(): void {
    this.toast.success('Notification settings saved successfully.');
  }

  saveSecurity(): void {
    this.toast.success('Security settings saved successfully.');
  }
}
