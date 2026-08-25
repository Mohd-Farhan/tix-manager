import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TicketPriority } from '../../../models/ticket.model';

@Component({
  selector: 'app-priority-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="priority-badge">
      <span class="priority-dot" [ngClass]="priorityClass"></span>
      @if (showLabel) { <span class="priority-label">{{ label }}</span> }
    </div>
  `,
  styles: [`
    .priority-badge { display: inline-flex; align-items: center; gap: 0.4rem; }
    .priority-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
    .priority-low { background: #16a34a; }
    .priority-medium { background: var(--tertiary, #4b88d4); }
    .priority-high { background: var(--error, #ba1a1a); }
    .priority-label { font-size: 0.8125rem; font-weight: 600; color: var(--on-surface); text-transform: capitalize; }
  `],
})
export class PriorityBadgeComponent {
  @Input() priority!: TicketPriority;
  @Input() showLabel = true;

  get priorityClass(): string {
    return { LOW: 'priority-low', MEDIUM: 'priority-medium', HIGH: 'priority-high' }[this.priority] ?? '';
  }

  get label(): string {
    return this.priority?.toLowerCase() ?? '';
  }
}
