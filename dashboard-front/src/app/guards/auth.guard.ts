import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

function isTokenValid(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    if (!payload.exp) return true;
    return payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
}

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const token = localStorage.getItem('accessToken');
  if (token && isTokenValid(token)) return true;
    localStorage.removeItem('accessToken');
  router.navigate(['/login']);
  return false;
};

export const guestGuard: CanActivateFn = () => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      if (!payload.exp || payload.exp * 1000 > Date.now()) return inject(Router).createUrlTree(['/dashboard']);
    } catch {}
  }
  return true;
};
