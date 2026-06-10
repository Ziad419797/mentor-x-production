import { Routes } from '@angular/router';
import { parentAuthGuard, parentGuestGuard } from './guards/parent-auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  {
    path: 'login',
    canActivate: [parentGuestGuard],
    loadComponent: () => import('./pages/login/parent-login.component').then(m => m.ParentLoginComponent)
  },
  {
    path: '',
    canActivate: [parentAuthGuard],
    loadComponent: () => import('./layout/shell/parent-shell.component').then(m => m.ParentShellComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/parent-dashboard.component').then(m => m.ParentDashboardComponent)
      },
      {
        path: 'child/:studentId',
        loadComponent: () => import('./pages/child-detail/child-detail.component').then(m => m.ChildDetailComponent)
      }
    ]
  },
  { path: '**', redirectTo: 'login' }
];
