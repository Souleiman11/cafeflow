import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { StatusPage } from './status-page';

function setup() {
  TestBed.configureTestingModule({
    imports: [StatusPage],
    providers: [provideHttpClient(), provideHttpClientTesting()],
  });
  const fixture = TestBed.createComponent(StatusPage);
  const http = TestBed.inject(HttpTestingController);
  fixture.detectChanges();
  return { fixture, http, text: () => (fixture.nativeElement as HTMLElement).textContent ?? '' };
}

describe('StatusPage', () => {
  it('shows a loading state while the probe is in flight', () => {
    const { http, text } = setup();
    expect(text()).toContain('Checking');
    http.expectOne('/actuator/health/readiness');
  });

  it('shows Ready when the backend is UP', async () => {
    const { fixture, http, text } = setup();
    http.expectOne('/actuator/health/readiness').flush({ status: 'UP' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(text()).toContain('Ready');
  });

  it('shows an error state when the backend cannot be reached', async () => {
    const { fixture, http, text } = setup();
    http.expectOne('/actuator/health/readiness').error(new ProgressEvent('error'));
    await fixture.whenStable();
    fixture.detectChanges();
    expect(text()).toContain('Cannot reach the backend');
  });
});
