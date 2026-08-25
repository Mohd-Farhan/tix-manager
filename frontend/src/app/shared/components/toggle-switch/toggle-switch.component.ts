import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-toggle-switch',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toggle-row">
      <div class="toggle-info">
        <span class="toggle-label">{{ label }}</span>
        @if (description) { <span class="toggle-desc">{{ description }}</span> }
      </div>
      <button class="toggle-btn" [class.on]="value" (click)="toggle()" [attr.aria-label]="label" type="button">
        <div class="toggle-knob"></div>
      </button>
    </div>
  `,
  styles: [`
    .toggle-row {
      display: flex; align-items: center; justify-content: space-between;
      padding: 1rem 0; border-bottom: 1px solid var(--outline-variant, #c6c6cd);
    }
    :host(:last-child) .toggle-row,
    .toggle-row:last-of-type { border-bottom: none; }
    .toggle-info { display: flex; flex-direction: column; gap: 0.125rem; flex: 1; margin-right: 1rem; }
    .toggle-label { font-size: 0.9375rem; font-weight: 600; color: var(--on-surface); }
    .toggle-desc { font-size: 0.8125rem; color: var(--on-surface-variant); }
    .toggle-btn {
      position: relative; width: 48px; height: 26px; border-radius: 13px;
      background: var(--surface-container-high, #e6e8ea); border: 2px solid var(--outline-variant, #c6c6cd);
      cursor: pointer; transition: all 0.25s ease; padding: 0; flex-shrink: 0;
    }
    .toggle-btn.on { background: var(--blue-500, #2170e4); border-color: var(--blue-500, #2170e4); }
    .toggle-knob {
      position: absolute; top: 2px; left: 2px; width: 18px; height: 18px;
      border-radius: 50%; background: #fff;
      box-shadow: 0 1px 3px rgba(0,0,0,0.2); transition: transform 0.25s ease;
    }
    .toggle-btn.on .toggle-knob { transform: translateX(22px); }
  `],
})
export class ToggleSwitchComponent {
  @Input() value = false;
  @Input() label = '';
  @Input() description = '';
  @Output() toggled = new EventEmitter<boolean>();

  toggle(): void {
    this.toggled.emit(!this.value);
  }
}
