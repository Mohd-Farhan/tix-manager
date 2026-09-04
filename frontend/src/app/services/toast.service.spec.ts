import { TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach } from 'vitest';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ToastService);
    service.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should append a success toast to the reactive signal', () => {
    service.success('Operation completed successfully', 'Success');
    const toasts = service.toasts();

    expect(toasts.length).toBe(1);
    expect(toasts[0].type).toBe('success');
    expect(toasts[0].message).toBe('Operation completed successfully');
    expect(toasts[0].title).toBe('Success');
  });

  it('should append error, warning, and info toasts', () => {
    service.error('Database connection failed', 'Error');
    service.warning('Session expiring in 5 minutes', 'Warning');
    service.info('New message received', 'Info');

    const toasts = service.toasts();
    expect(toasts.length).toBe(3);
    expect(toasts[0].type).toBe('error');
    expect(toasts[1].type).toBe('warning');
    expect(toasts[2].type).toBe('info');
  });

  it('should remove a toast by ID', () => {
    const id1 = service.success('First toast');
    const id2 = service.error('Second toast');

    expect(service.toasts().length).toBe(2);

    service.remove(id1);
    const remaining = service.toasts();
    expect(remaining.length).toBe(1);
    expect(remaining[0].id).toBe(id2);
  });

  it('should clear all toasts', () => {
    service.success('T1');
    service.warning('T2');
    expect(service.toasts().length).toBe(2);

    service.clear();
    expect(service.toasts().length).toBe(0);
  });
});
