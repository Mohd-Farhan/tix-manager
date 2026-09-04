import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { tap } from 'rxjs';

/**
 * ==============================================================================================
 * HTTP INTERCEPTOR: LoggingInterceptor (Frontend Observability & Telemetry)
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Production-Grade Observability):
 * 1. Network Latency Tracking: Measures roundtrip HTTP request duration with high-precision timer.
 * 2. Slow Request Warning: Emits warnings for network calls exceeding 1000ms latency.
 * 3. Client Telemetry: Provides structured console traces of outbound API interactions.
 */
export const loggingInterceptor: HttpInterceptorFn = (req, next) => {
  const startedAt = performance.now();
  const method = req.method.toUpperCase();
  const url = req.url;

  return next(req).pipe(
    tap({
      next: (event) => {
        if (event instanceof HttpResponse) {
          const elapsed = Math.round(performance.now() - startedAt);
          const status = event.status;

          // Highlight slow responses
          if (elapsed > 1000) {
            console.warn(`[HTTP SLOW] ${method} ${url} -> ${status} (${elapsed}ms)`);
          } else {
            console.debug(`[HTTP] ${method} ${url} -> ${status} (${elapsed}ms)`);
          }
        }
      },
      error: (error) => {
        const elapsed = Math.round(performance.now() - startedAt);
        console.error(`[HTTP FAIL] ${method} ${url} -> ${error.status || 'ERR'} (${elapsed}ms)`, error);
      },
    })
  );
};
