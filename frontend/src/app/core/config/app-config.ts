import { InjectionToken } from '@angular/core';
import { environment, Environment } from '../../../environments/environment';

/** Injectable application configuration. Components/services must never hardcode backend URLs. */
export const APP_CONFIG = new InjectionToken<Environment>('APP_CONFIG', {
  providedIn: 'root',
  factory: () => environment,
});
