import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TicketStatus } from '../../../models/ticket.model';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span class="status-badge" [ngClass]="statusClass">{{ label }}</span>`,
  styles: [`
    .status-badge {
      font-size: 0.6875rem; font-weight: 700; padding: 0.2rem 0.5rem;
      border-radius: 9999px; text-transform: uppercase; letter-spacing: 0.04em;
      display: inline-block; white-space: nowrap;
    }
    .status-open { background: color-mix(in srgb, var(--blue-500) 12%, transparent); color: var(--blue-600); }
    .status-progress { background: color-mix(in srgb, var(--tertiary) 12%, transparent); color: var(--tertiary); }
    .status-resolved { background: color-mix(in srgb, #16a34a 12%, transparent); color: #16a34a; }
  `],
})
export class StatusBadgeComponent {
  @Input() status!: TicketStatus;

  get statusClass(): string {
    return { OPEN: 'status-open', IN_PROGRESS: 'status-progress', RESOLVED: 'status-resolved' }[this.status] ?? '';
  }

  get label(): string {
    return { OPEN: 'Open', IN_PROGRESS: 'In Progress', RESOLVED: 'Resolved' }[this.status] ?? this.status;
  }
}
