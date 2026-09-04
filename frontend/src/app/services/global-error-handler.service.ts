import { ErrorHandler, Injectable, inject, NgZone } from '@angular/core';
import { ToastService } from './toast.service';

/**
 * ==============================================================================================
 * GLOBAL CLIENT ERROR HANDLER (Angular ErrorHandler)
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Application Reliability):
 * 1. Runtime Crash Prevention: Intercepts unhandled JavaScript runtime exceptions before they crash the UI.
 * 2. Observability Logging: Logs component stack traces and diagnostic context.
 * 3. Graceful Recovery: Safely runs within Angular's NgZone to notify the user and maintain responsiveness.
 */
@Injectable({
  providedIn: 'root',
})
export class GlobalErrorHandler implements ErrorHandler {
  private readonly zone = inject(NgZone);
  private readonly toastService = inject(ToastService);

  handleError(error: unknown): void {
    const message = error instanceof Error ? error.message : String(error);
    const stack = error instanceof Error ? error.stack : undefined;

    console.error('[Application Runtime Error]:', { message, stack, error });

    // Execute in NgZone so Angular change detection updates the toast UI
    this.zone.run(() => {
      // Don't show toast for benign chunk loading retry or cancelled navigation
      if (!message.includes('Loading chunk') && !message.includes('Navigation cancelled')) {
        this.toastService.error('An unexpected client application error occurred.', 'Application Error');
      }
    });
  }
}
