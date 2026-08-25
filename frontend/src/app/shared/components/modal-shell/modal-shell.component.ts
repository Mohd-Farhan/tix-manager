import { Component, Input, Output, EventEmitter, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-modal-shell',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (visible) {
    <div class="modal-overlay" (click)="onClose()">
      <div class="modal-window" [class]="sizeClass" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <div class="modal-title-group">
            @if (icon) {
            <div class="modal-icon" [innerHTML]="icon"></div>
            }
            <span class="modal-title">{{ title }}</span>
          </div>
          <button class="modal-close-btn" (click)="onClose()" aria-label="Close">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <ng-content></ng-content>
        </div>
      </div>
    </div>
    }
  `,
  styles: [`
    .modal-overlay {
      position: fixed; top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0,0,0,0.45); backdrop-filter: blur(6px);
      display: flex; justify-content: center; align-items: flex-start;
      padding-top: 5vh; z-index: 1100;
      animation: overlayFadeIn 0.2s ease-out;
    }
    @keyframes overlayFadeIn { from { opacity: 0; } to { opacity: 1; } }
    .modal-window {
      background: var(--surface-container-low); border: 1px solid var(--outline-variant);
      border-radius: var(--radius-xl, 16px); width: 92%;
      box-shadow: 0 32px 64px rgba(0,0,0,0.2); overflow: hidden;
      animation: modalSlideDown 0.3s ease-out;
    }
    .modal-sm { max-width: 420px; }
    .modal-md { max-width: 520px; }
    .modal-lg { max-width: 720px; }
    .modal-xl { max-width: 900px; }
    @keyframes modalSlideDown { from { opacity: 0; transform: translateY(-20px); } to { opacity: 1; transform: translateY(0); } }
    .modal-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 1.25rem 1.5rem; border-bottom: 1px solid var(--outline-variant);
      background: var(--surface-container);
    }
    .modal-title-group { display: flex; align-items: center; gap: 0.75rem; }
    .modal-icon {
      width: 36px; height: 36px; border-radius: 10px;
      background: color-mix(in srgb, var(--blue-500) 12%, transparent); color: var(--blue-500);
      display: flex; align-items: center; justify-content: center;
    }
    .modal-title { font-size: 1.0625rem; font-weight: 700; color: var(--on-surface); }
    .modal-close-btn {
      width: 32px; height: 32px; border-radius: 8px; border: none;
      background: transparent; color: var(--on-surface-variant); cursor: pointer;
      display: flex; align-items: center; justify-content: center; transition: all 0.15s ease;
    }
    .modal-close-btn:hover { background: var(--surface-container-high); color: var(--on-surface); }
    .modal-body { padding: 1.5rem; }
  `],
})
export class ModalShellComponent {
  @Input() visible = false;
  @Input() title = '';
  @Input() icon = '';
  @Input() size: 'sm' | 'md' | 'lg' | 'xl' = 'md';
  @Output() closed = new EventEmitter<void>();

  get sizeClass(): string { return `modal-${this.size}`; }

  onClose(): void { this.closed.emit(); }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.visible) this.onClose();
  }
}
