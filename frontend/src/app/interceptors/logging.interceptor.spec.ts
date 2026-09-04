import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { loggingInterceptor } from './logging.interceptor';

describe('loggingInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([loggingInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should pass request through and log response', () => {
    const consoleSpy = vi.spyOn(console, 'debug').mockImplementation(() => {});

    httpClient.get('/api/tickets').subscribe((res) => {
      expect(res).toEqual([{ id: 1 }]);
    });

    const req = httpTesting.expectOne('/api/tickets');
    req.flush([{ id: 1 }]);

    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});
