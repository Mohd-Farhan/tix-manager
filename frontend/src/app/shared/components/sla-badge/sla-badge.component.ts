import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Ticket, TicketStatus } from '../../../models/ticket.model';

@Component({
  selector: 'app-sla-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (ticket && ticket.slaDueAt) {
      <div class="sla-container" [class.compact]="compact" [title]="dueTooltip">
        <span class="sla-badge" [ngClass]="statusClass">
          @if (showIcon) {
            <span class="sla-icon">
              @switch (statusClass) {
                @case ('sla-resolved-met') {
                  <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>
                }
                @case ('sla-breached') {
                  <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/></svg>
                }
                @case ('sla-resolved-breached') {
                  <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/></svg>
                }
                @default {
                  <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd"/></svg>
                }
              }
            </span>
          }
          <span class="sla-text">{{ displayText }}</span>
          @if (isEscalated) {
            <span class="escalated-pill">Escalated</span>
          }
        </span>
        @if (showDeadline) {
          <span class="sla-deadline">Due: {{ ticket.slaDueAt | date:'MMM d, y, h:mm a' }}</span>
        }
      </div>
    }
  `,
  styles: [`
    .sla-container {
      display: inline-flex;
      flex-direction: column;
      gap: 0.2rem;
    }

    .sla-container.compact {
      flex-direction: row;
      align-items: center;
      gap: 0.35rem;
    }

    .sla-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      padding: 0.2rem 0.55rem;
      border-radius: var(--radius-full, 9999px);
      font-size: 0.75rem;
      font-weight: 600;
      line-height: 1.2;
      letter-spacing: 0.02em;
      white-space: nowrap;
      transition: all 0.2s ease;
    }

    .sla-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 13px;
      height: 13px;
      flex-shrink: 0;
    }

    .sla-icon :deep(svg) {
      width: 100%;
      height: 100%;
    }

    .sla-text {
      display: inline-block;
    }

    .escalated-pill {
      font-size: 0.625rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      background: var(--error, #ba1a1a);
      color: var(--on-error, #ffffff);
      padding: 0.1rem 0.35rem;
      border-radius: 4px;
      margin-left: 0.15rem;
    }

    .sla-deadline {
      font-size: 0.6875rem;
      color: var(--on-surface-variant, #45464d);
    }

    /* Status Variants mapped to Azure Professional Tokens */
    .sla-ok {
      background: var(--blue-50, #f0f5fd);
      color: var(--blue-700, #004da8);
      border: 1px solid var(--blue-200, #adc6ff);
    }

    .sla-warning {
      background: color-mix(in srgb, #eab308 14%, var(--surface-bright, #ffffff));
      color: #92400e;
      border: 1px solid color-mix(in srgb, #eab308 35%, transparent);
    }

    .sla-breached {
      background: var(--error-container, #ffdad6);
      color: var(--on-error-container, #93000a);
      border: 1px solid var(--error, #ba1a1a);
    }

    .sla-resolved-met {
      background: color-mix(in srgb, #16a34a 12%, var(--surface-bright, #ffffff));
      color: #15803d;
      border: 1px solid color-mix(in srgb, #16a34a 28%, transparent);
    }

    .sla-resolved-breached {
      background: var(--surface-container-high, #e6e8ea);
      color: var(--on-surface-variant, #45464d);
      border: 1px solid var(--outline-variant, #c6c6cd);
    }
  `],
})
export class SlaBadgeComponent implements OnInit, OnDestroy {
  @Input() ticket!: Ticket;
  @Input() showIcon = true;
  @Input() showDeadline = false;
  @Input() compact = false;

  private timerInterval?: any;
  displayText = '';
  statusClass = '';
  isEscalated = false;

  ngOnInit(): void {
    this.updateState();
    this.timerInterval = setInterval(() => this.updateState(), 30000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  get dueTooltip(): string {
    if (!this.ticket?.slaDueAt) return '';
    const dateStr = new Date(this.ticket.slaDueAt).toLocaleString();
    return `SLA Target: ${dateStr}`;
  }

  private updateState(): void {
    if (!this.ticket || !this.ticket.slaDueAt) {
      this.displayText = '';
      this.statusClass = '';
      this.isEscalated = false;
      return;
    }

    this.isEscalated = !!this.ticket.escalated;

    // Check if resolved
    if (this.ticket.status === TicketStatus.RESOLVED || this.ticket.resolvedAt) {
      if (this.ticket.slaBreached) {
        this.statusClass = 'sla-resolved-breached';
        this.displayText = 'SLA Breached';
      } else {
        this.statusClass = 'sla-resolved-met';
        this.displayText = 'SLA Met';
      }
      return;
    }

    // Active ticket countdown
    const dueTime = new Date(this.ticket.slaDueAt).getTime();
    const nowTime = Date.now();
    const diffSec = Math.floor((dueTime - nowTime) / 1000);

    if (diffSec < 0 || this.ticket.slaBreached) {
      this.statusClass = 'sla-breached';
      const pastSec = Math.abs(diffSec);
      this.displayText = `Breached by ${this.formatDuration(pastSec)}`;
    } else if (diffSec <= 7200) { // <= 2 hours
      this.statusClass = 'sla-warning';
      this.displayText = `${this.formatDuration(diffSec)} left`;
    } else {
      this.statusClass = 'sla-ok';
      this.displayText = `${this.formatDuration(diffSec)} left`;
    }
  }

  private formatDuration(seconds: number): string {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);

    if (hours >= 24) {
      const days = Math.floor(hours / 24);
      const remHours = hours % 24;
      return remHours > 0 ? `${days}d ${remHours}h` : `${days}d`;
    }
    if (hours > 0) {
      return minutes > 0 ? `${hours}h ${minutes}m` : `${hours}h`;
    }
    return `${Math.max(1, minutes)}m`;
  }
}
