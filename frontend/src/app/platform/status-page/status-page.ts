import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { PlatformStatus } from '../../core/health/health.models';
import { HealthService } from '../../core/health/health.service';

/** Phase 0 development page: proves the frontend -> backend path works. Not a business feature. */
@Component({
  selector: 'app-status-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './status-page.html',
  styleUrl: './status-page.scss',
})
export class StatusPage {
  private readonly health = inject(HealthService);

  protected readonly status = signal<PlatformStatus | 'CHECKING'>('CHECKING');

  constructor() {
    this.refresh();
  }

  protected refresh(): void {
    this.status.set('CHECKING');
    this.health.getReadiness().subscribe((h) => this.status.set(h.status));
  }
}
