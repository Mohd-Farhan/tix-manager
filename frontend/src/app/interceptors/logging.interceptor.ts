import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { tap } from 'rxjs';

/**
 * ==============================================================================================
 * HTTP INTERCEPTOR: LoggingInterceptor (Frontend Observability & Telemetry)
 * ==============================================================================================
 * 
 * Production-grade request correlation & latency tracking.
 * 1. Propagates or injects 'X-Correlation-ID' header for outbound requests if absent.
 * 2. Reads 'X-Correlation-ID' from backend response headers.
 * 3. Logs telemetry with traceId correlation: [HTTP] [traceId] GET /api/... -> 200 (45ms).
 */
export const loggingInterceptor: HttpInterceptorFn = (req, next) => {
  const startedAt = performance.now();
  const method = req.method.toUpperCase();
  const url = req.url;

  // Propagate or generate correlation trace ID for end-to-end tracing
  const correlationId =
    req.headers.get('X-Correlation-ID') ||
    (typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : Math.random().toString(36).substring(2, 10));

  const tracedReq = req.clone({
    headers: req.headers.set('X-Correlation-ID', correlationId),
  });

  return next(tracedReq).pipe(
    tap({
      next: (event) => {
        if (event instanceof HttpResponse) {
          const elapsed = Math.round(performance.now() - startedAt);
          const status = event.status;
          const traceId = event.headers.get('X-Correlation-ID') || correlationId;

          // Highlight slow responses
          if (elapsed > 1000) {
            console.warn(`[HTTP SLOW] [${traceId}] ${method} ${url} -> ${status} (${elapsed}ms)`);
          } else {
            console.debug(`[HTTP] [${traceId}] ${method} ${url} -> ${status} (${elapsed}ms)`);
          }
        }
      },
      error: (error) => {
        const elapsed = Math.round(performance.now() - startedAt);
        const traceId = error.headers?.get('X-Correlation-ID') || correlationId;
        console.error(`[HTTP FAIL] [${traceId}] ${method} ${url} -> ${error.status || 'ERR'} (${elapsed}ms)`, error);
      },
    })
  );
};

