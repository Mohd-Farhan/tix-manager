import { Injectable, signal } from '@angular/core';
import { Subject } from 'rxjs';
import { Toast, ToastType } from '../models/toast.model';

export interface ToastEvent {
  message: string;
  type: ToastType;
  duration: number;
}

/**
 * ==============================================================================================
 * SERVICE: ToastService (Reactive UI Notification State)
 * ==============================================================================================
 * 
 * Central notification manager supporting both modern Angular Signals and RxJS observables.
 */
@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private readonly toastsSignal = signal<Toast[]>([]);
  public readonly toasts = this.toastsSignal.asReadonly();

  // Backward compatibility for components using toast$ observable
  private readonly toastSubject = new Subject<ToastEvent>();
  public readonly toast$ = this.toastSubject.asObservable();

  show(
    messageOrType: string,
    typeOrMessage: ToastType | string = 'info',
    titleOrDuration: string | number = 'Notification',
    durationMs = 4500
  ): string {
    let type: ToastType = 'info';
    let message = '';
    let title: string | undefined = undefined;
    let duration = durationMs;

    const validTypes: ToastType[] = ['success', 'error', 'info', 'warning'];

    // Overload check: show('success', 'File uploaded', 'Title') vs show('File uploaded', 'success', 3000)
    if (validTypes.includes(messageOrType as ToastType)) {
      type = messageOrType as ToastType;
      message = String(typeOrMessage);
      title = typeof titleOrDuration === 'string' ? titleOrDuration : undefined;
      duration = typeof titleOrDuration === 'number' ? titleOrDuration : durationMs;
    } else {
      message = messageOrType;
      type = validTypes.includes(typeOrMessage as ToastType) ? (typeOrMessage as ToastType) : 'info';
      if (typeof titleOrDuration === 'number') {
        duration = titleOrDuration;
      } else {
        title = titleOrDuration;
      }
    }

    const id = `toast-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
    const newToast: Toast = {
      id,
      type,
      title,
      message,
      durationMs: duration,
      timestamp: new Date(),
    };

    this.toastsSignal.update((current) => [...current, newToast]);
    this.toastSubject.next({ message, type, duration });

    if (duration > 0) {
      setTimeout(() => {
        this.remove(id);
      }, duration);
    }

    return id;
  }

  success(message: string, titleOrDuration: string | number = 'Success'): string {
    const title = typeof titleOrDuration === 'string' ? titleOrDuration : 'Success';
    const duration = typeof titleOrDuration === 'number' ? titleOrDuration : 4000;
    return this.show('success', message, title, duration);
  }

  error(message: string, titleOrDuration: string | number = 'Error'): string {
    const title = typeof titleOrDuration === 'string' ? titleOrDuration : 'Error';
    const duration = typeof titleOrDuration === 'number' ? titleOrDuration : 6000;
    return this.show('error', message, title, duration);
  }

  warning(message: string, titleOrDuration: string | number = 'Warning'): string {
    const title = typeof titleOrDuration === 'string' ? titleOrDuration : 'Warning';
    const duration = typeof titleOrDuration === 'number' ? titleOrDuration : 5000;
    return this.show('warning', message, title, duration);
  }

  info(message: string, titleOrDuration: string | number = 'Information'): string {
    const title = typeof titleOrDuration === 'string' ? titleOrDuration : 'Information';
    const duration = typeof titleOrDuration === 'number' ? titleOrDuration : 4000;
    return this.show('info', message, title, duration);
  }

  remove(id: string): void {
    this.toastsSignal.update((current) => current.filter((t) => t.id !== id));
  }

  clear(): void {
    this.toastsSignal.set([]);
  }
}
