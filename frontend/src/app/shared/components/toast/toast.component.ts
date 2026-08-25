import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { ToastService, ToastEvent } from '../../services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (visible) {
    <div class="toast" [ngClass]="'toast-' + currentToast.type" [class.visible]="visible">
      @if (currentToast.type === 'success') {
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
      } @else if (currentToast.type === 'error') {
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
      } @else {
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
      }
      <span>{{ currentToast.message }}</span>
    </div>
    }
  `,
  styles: [`
    :host { position: fixed; bottom: 2rem; left: 50%; transform: translateX(-50%); z-index: 99999; pointer-events: none; }
    .toast {
      display: flex; align-items: center; gap: 0.625rem;
      padding: 0.875rem 1.5rem; border-radius: var(--radius-lg, 12px);
      font-size: 0.9375rem; font-weight: 600; white-space: nowrap;
      box-shadow: 0 16px 48px rgba(0,0,0,0.18);
      background: var(--surface-container-high, #e6e8ea);
      border: 1px solid var(--outline-variant, #c6c6cd);
      animation: toastIn 0.3s ease-out;
      pointer-events: auto;
    }
    .toast-success { color: #16a34a; }
    .toast-error { color: var(--error, #ba1a1a); }
    .toast-info { color: var(--blue-500, #2170e4); }
    @keyframes toastIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
  `],
})
export class ToastComponent implements OnInit, OnDestroy {
  private toastService = inject(ToastService);
  private sub!: Subscription;
  private timer: any;

  visible = false;
  currentToast: ToastEvent = { message: '', type: 'success', duration: 3000 };

  ngOnInit(): void {
    this.sub = this.toastService.toast$.subscribe((event) => {
      clearTimeout(this.timer);
      this.currentToast = event;
      this.visible = true;
      this.timer = setTimeout(() => { this.visible = false; }, event.duration);
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    clearTimeout(this.timer);
  }
}
