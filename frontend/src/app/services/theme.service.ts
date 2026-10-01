import { Injectable, computed, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

/**
 * Enterprise Theme Management Service (Azure Professional Narrative)
 *
 * Provides reactive signal-based theme state, localStorage persistence,
 * multi-tab synchronization, and DOM attribute management for Light/Dark modes.
 */
@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private readonly THEME_KEY = 'tix-theme';

  readonly currentTheme = signal<Theme>(this.getInitialTheme());
  readonly isDark = computed(() => this.currentTheme() === 'dark');

  constructor() {
    this.applyThemeToDom(this.currentTheme());
    this.initStorageListener();
  }

  /**
   * Toggle between light and dark modes
   */
  toggleTheme(): void {
    const nextTheme: Theme = this.currentTheme() === 'dark' ? 'light' : 'dark';
    this.setTheme(nextTheme);
  }

  /**
   * Explicitly set theme to light or dark
   */
  setTheme(theme: Theme): void {
    this.currentTheme.set(theme);
    this.applyThemeToDom(theme);
    if (typeof localStorage !== 'undefined') {
      try {
        localStorage.setItem(this.THEME_KEY, theme);
      } catch {
        // Storage access error fallback
      }
    }
  }

  getTheme(): Theme {
    return this.currentTheme();
  }

  private getInitialTheme(): Theme {
    if (typeof localStorage !== 'undefined') {
      try {
        const saved = localStorage.getItem(this.THEME_KEY);
        if (saved === 'dark' || saved === 'light') {
          return saved;
        }
      } catch {
        // Storage access error fallback
      }
    }

    if (
      typeof window !== 'undefined' &&
      window.matchMedia &&
      window.matchMedia('(prefers-color-scheme: dark)').matches
    ) {
      return 'dark';
    }

    return 'light';
  }

  private applyThemeToDom(theme: Theme): void {
    if (typeof document === 'undefined') return;
    const root = document.documentElement;
    root.setAttribute('data-theme', theme);
    if (theme === 'dark') {
      document.body.classList.add('dark-theme');
    } else {
      document.body.classList.remove('dark-theme');
    }
  }

  private initStorageListener(): void {
    if (typeof window !== 'undefined') {
      window.addEventListener('storage', (event: StorageEvent) => {
        if (event.key === this.THEME_KEY && (event.newValue === 'light' || event.newValue === 'dark')) {
          this.currentTheme.set(event.newValue);
          this.applyThemeToDom(event.newValue);
        }
      });
    }
  }
}
