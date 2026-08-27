import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type LogoVariant = 'full' | 'icon';
export type LogoSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';
export type LogoColorMode = 'auto' | 'light' | 'dark' | 'white' | 'azure';

@Component({
  selector: 'app-logo',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="logo-wrapper" [ngClass]="[sizeClass, variantClass, colorModeClass]">
      @if (variant === 'full') {
        @if (colorMode === 'white') {
          <img src="/logo-white.png" alt="TixManager Logo" class="logo-image" [style.height.px]="customHeight" />
        } @else if (colorMode === 'azure') {
          <img src="/logo-azure.png" alt="TixManager Logo" class="logo-image" [style.height.px]="customHeight" />
        } @else if (colorMode === 'light') {
          <img src="/logo-light.png" alt="TixManager Logo" class="logo-image" [style.height.px]="customHeight" />
        } @else if (colorMode === 'dark') {
          <img src="/logo-dark.png" alt="TixManager Logo" class="logo-image" [style.height.px]="customHeight" />
        } @else {
          <img src="/logo-light.png" alt="TixManager Logo" class="logo-image logo-theme-light" [style.height.px]="customHeight" />
          <img src="/logo-dark.png" alt="TixManager Logo" class="logo-image logo-theme-dark" [style.height.px]="customHeight" />
        }
      } @else {
        <div class="logo-icon-box" [style.width.px]="customHeight" [style.height.px]="customHeight">
          <img src="/logo-icon.png" alt="TixManager Icon" class="logo-icon-img" />
        </div>
      }
    </div>
  `,
  styles: [`
    :host {
      display: inline-flex;
      align-items: center;
      vertical-align: middle;
    }
    .logo-wrapper {
      display: inline-flex;
      align-items: center;
      line-height: 1;
      user-select: none;
    }
    .logo-image {
      display: block;
      width: auto;
      object-fit: contain;
      transition: opacity var(--transition-fast, 150ms);
    }
    .logo-theme-light {
      display: block;
    }
    .logo-theme-dark {
      display: none;
    }
    :host-context([data-theme="dark"]) .logo-theme-light {
      display: none;
    }
    :host-context([data-theme="dark"]) .logo-theme-dark {
      display: block;
    }

    .logo-icon-box {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: var(--radius-md, 10px);
      background: linear-gradient(135deg, var(--blue-600, #0058be), var(--blue-400, #4b88d4));
      box-shadow: 0 2px 8px rgba(0, 88, 190, 0.25);
      overflow: hidden;
      padding: 4px;
    }
    .logo-icon-img {
      width: 85%;
      height: 85%;
      object-fit: contain;
      filter: brightness(0) invert(1);
    }

    /* Sizes */
    .size-xs .logo-image { height: 20px; }
    .size-sm .logo-image { height: 26px; }
    .size-md .logo-image { height: 32px; }
    .size-lg .logo-image { height: 42px; }
    .size-xl .logo-image { height: 54px; }

    .size-xs .logo-icon-box { width: 24px; height: 24px; border-radius: var(--radius-sm, 6px); }
    .size-sm .logo-icon-box { width: 30px; height: 30px; border-radius: var(--radius-sm, 6px); }
    .size-md .logo-icon-box { width: 36px; height: 36px; border-radius: var(--radius-md, 10px); }
    .size-lg .logo-icon-box { width: 44px; height: 44px; border-radius: var(--radius-md, 10px); }
    .size-xl .logo-icon-box { width: 54px; height: 54px; border-radius: var(--radius-lg, 16px); }
  `]
})
export class LogoComponent {
  @Input() variant: LogoVariant = 'full';
  @Input() size: LogoSize = 'md';
  @Input() colorMode: LogoColorMode = 'auto';
  @Input() height?: number;

  get customHeight(): number | undefined {
    return this.height;
  }

  get sizeClass(): string {
    return `size-${this.size}`;
  }

  get variantClass(): string {
    return `variant-${this.variant}`;
  }

  get colorModeClass(): string {
    return `mode-${this.colorMode}`;
  }
}
