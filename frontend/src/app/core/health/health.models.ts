/** `UNREACHABLE` is a client-side state: the backend could not be contacted at all. */
export type PlatformStatus = 'UP' | 'DOWN' | 'UNREACHABLE';

export interface PlatformHealth {
  status: PlatformStatus;
}
