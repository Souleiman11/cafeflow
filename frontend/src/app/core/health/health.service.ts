import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { PlatformHealth } from './health.models';

/** Centralised access to the backend readiness probe (the only backend call in Phase 0). */
@Injectable({ providedIn: 'root' })
export class HealthService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  getReadiness(): Observable<PlatformHealth> {
    return this.http.get<{ status: string }>(`${this.config.healthBaseUrl}/readiness`).pipe(
      map((body): PlatformHealth => ({ status: body.status === 'UP' ? 'UP' : 'DOWN' })),
      catchError((error: HttpErrorResponse) =>
        // Spring Actuator answers 503 with a JSON body when a dependency is down.
        of<PlatformHealth>({ status: error.status === 503 ? 'DOWN' : 'UNREACHABLE' }),
      ),
    );
  }
}
