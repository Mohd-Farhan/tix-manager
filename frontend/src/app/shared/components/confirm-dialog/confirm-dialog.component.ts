import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (visible) {
    <div class="confirm-overlay" (click)="onCancel()">
      <div class="confirm-modal" (click)="$event.stopPropagation()">
        <div class="confirm-content">
          <div class="confirm-icon" [ngClass]="variant">
            @if (variant === 'danger') {
            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            } @else {
            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            }
          </div>
          <h3>{{ title }}</h3>
          <p>{{ message }}</p>
          <div class="confirm-actions">
            <button class="btn-cancel" (click)="onCancel()">{{ cancelLabel }}</button>
            <button class="btn-confirm" [ngClass]="variant" (click)="onConfirm()">{{ confirmLabel }}</button>
          </div>
        </div>
      </div>
    </div>
    }
  `,
  styles: [`
    .confirm-overlay {
      position: fixed; top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0,0,0,0.45); backdrop-filter: blur(6px);
      display: flex; justify-content: center; align-items: center; z-index: 1100;
      animation: overlayIn 0.2s ease-out;
    }
    @keyframes overlayIn { from { opacity: 0; } to { opacity: 1; } }
    .confirm-modal {
      background: var(--surface-container-low); border: 1px solid var(--outline-variant);
      border-radius: var(--radius-xl, 16px); max-width: 420px; width: 92%;
      box-shadow: 0 32px 64px rgba(0,0,0,0.2); overflow: hidden;
      animation: modalIn 0.3s ease-out;
    }
    @keyframes modalIn { from { opacity: 0; transform: translateY(-20px) scale(0.95); } to { opacity: 1; transform: translateY(0) scale(1); } }
    .confirm-content { padding: 2rem; text-align: center; }
    .confirm-icon { margin-bottom: 1rem; }
    .confirm-icon.danger { color: var(--error, #ba1a1a); }
    .confirm-icon.default { color: var(--blue-500, #2170e4); }
    h3 { font-size: 1.25rem; font-weight: 700; color: var(--on-surface); margin-bottom: 0.5rem; }
    p { font-size: 0.9375rem; color: var(--on-surface-variant); margin-bottom: 1.5rem; line-height: 1.5; }
    .confirm-actions { display: flex; justify-content: center; gap: 0.75rem; }
    .btn-cancel {
      padding: 0.625rem 1.25rem; background: transparent; color: var(--on-surface);
      border: 1px solid var(--outline-variant); border-radius: var(--radius-md, 8px);
      font-size: 0.875rem; font-weight: 600; cursor: pointer; transition: all 0.15s ease;
    }
    .btn-cancel:hover { background: var(--surface-container); border-color: var(--outline); }
    .btn-confirm {
      padding: 0.625rem 1.25rem; color: #fff; border: none;
      border-radius: var(--radius-md, 8px); font-size: 0.875rem; font-weight: 600;
      cursor: pointer; transition: all 0.15s ease;
    }
    .btn-confirm.danger { background: var(--error, #ba1a1a); }
    .btn-confirm.danger:hover { opacity: 0.9; transform: translateY(-1px); box-shadow: 0 4px 12px color-mix(in srgb, var(--error) 30%, transparent); }
    .btn-confirm.default { background: linear-gradient(135deg, var(--blue-600), var(--blue-500)); }
    .btn-confirm.default:hover { transform: translateY(-1px); box-shadow: 0 4px 12px color-mix(in srgb, var(--blue-600) 30%, transparent); }
  `],
})
export class ConfirmDialogComponent {
  @Input() visible = false;
  @Input() title = 'Are you sure?';
  @Input() message = 'This action cannot be undone.';
  @Input() confirmLabel = 'Confirm';
  @Input() cancelLabel = 'Cancel';
  @Input() variant: 'danger' | 'default' = 'danger';

  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  onConfirm(): void { this.confirmed.emit(); }
  onCancel(): void { this.cancelled.emit(); }
}
