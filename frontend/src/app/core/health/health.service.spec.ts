import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HealthService } from './health.service';

describe('HealthService', () => {
  let service: HealthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(HealthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('maps an UP readiness response to UP', () => {
    let result: string | undefined;
    service.getReadiness().subscribe((h) => (result = h.status));
    http.expectOne('/actuator/health/readiness').flush({ status: 'UP' });
    expect(result).toBe('UP');
  });

  it('maps a 503 response to DOWN', () => {
    let result: string | undefined;
    service.getReadiness().subscribe((h) => (result = h.status));
    http
      .expectOne('/actuator/health/readiness')
      .flush({ status: 'DOWN' }, { status: 503, statusText: 'Service Unavailable' });
    expect(result).toBe('DOWN');
  });

  it('maps a network failure to UNREACHABLE', () => {
    let result: string | undefined;
    service.getReadiness().subscribe((h) => (result = h.status));
    http.expectOne('/actuator/health/readiness').error(new ProgressEvent('error'));
    expect(result).toBe('UNREACHABLE');
  });
});
