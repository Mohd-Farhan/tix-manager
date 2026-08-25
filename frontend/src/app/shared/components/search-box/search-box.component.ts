import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, debounceTime, distinctUntilChanged } from 'rxjs';

@Component({
  selector: 'app-search-box',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="search-box">
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
      <input type="text" [placeholder]="placeholder" [ngModel]="value" (ngModelChange)="onInput($event)" />
    </div>
  `,
  styles: [`
    .search-box { position: relative; width: 100%; }
    .search-box svg {
      position: absolute; left: 1rem; top: 50%; transform: translateY(-50%);
      color: var(--on-surface-variant); pointer-events: none;
    }
    input {
      width: 100%; padding: 0.75rem 1rem 0.75rem 2.75rem; font-size: 0.9375rem;
      color: var(--on-surface); background: var(--surface-container-lowest);
      border: 1.5px solid var(--outline-variant); border-radius: var(--radius-lg, 12px);
      transition: all 0.15s ease; font-family: inherit;
    }
    input:focus {
      outline: none; border-color: var(--blue-500);
      box-shadow: 0 0 0 3px color-mix(in srgb, var(--blue-500) 12%, transparent);
    }
    input::placeholder { color: var(--on-surface-variant); opacity: 0.7; }
  `],
})
export class SearchBoxComponent implements OnInit, OnDestroy {
  @Input() placeholder = 'Search...';
  @Input() value = '';
  @Input() debounce = 250;
  @Output() searchChange = new EventEmitter<string>();

  private inputSubject = new Subject<string>();
  private sub!: Subscription;

  ngOnInit(): void {
    this.sub = this.inputSubject.pipe(
      debounceTime(this.debounce),
      distinctUntilChanged(),
    ).subscribe((val) => this.searchChange.emit(val));
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  onInput(val: string): void {
    this.value = val;
    this.inputSubject.next(val);
  }
}
