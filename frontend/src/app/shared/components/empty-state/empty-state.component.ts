import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="empty-state">
      <div class="empty-icon" [innerHTML]="icon"></div>
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
      <ng-content></ng-content>
    </div>
  `,
  styles: [`
    .empty-state {
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      padding: 3rem 1rem; text-align: center; color: var(--on-surface-variant);
    }
    .empty-icon {
      width: 80px; height: 80px; border-radius: 50%;
      background: var(--surface-container, #eceef0);
      display: flex; align-items: center; justify-content: center;
      margin-bottom: 1.5rem; color: var(--outline, #76777d);
    }
    h3 { font-size: 1.25rem; font-weight: 700; color: var(--on-surface); margin-bottom: 0.5rem; }
    p { font-size: 0.9375rem; color: var(--on-surface-variant); max-width: 320px; line-height: 1.5; }
  `],
})
export class EmptyStateComponent {
  @Input() icon = `<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>`;
  @Input() title = 'No results found';
  @Input() description = 'Try adjusting your filters or search terms.';
}
