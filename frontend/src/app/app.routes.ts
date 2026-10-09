import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./platform/status-page/status-page').then((m) => m.StatusPage),
  },
  { path: '**', redirectTo: '' },
];
