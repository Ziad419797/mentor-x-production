import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

/** Decode JWT and return true if it's still valid (not expired). */
function isTokenValid(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    if (!payload.exp) return true; // no expiry claim — assume valid
    return payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
}

function clearSession(): void {
  localStorage.removeItem('parentToken');
  localStorage.removeItem('parentProfile');
}

export const parentAuthGuard: CanActivateFn = () => {
  const router = inject(Router);
  const token = localStorage.getItem('parentToken');
  if (token && isTokenValid(token)) return true;
  clearSession();
  router.navigate(['/login']);
  return false;
};

export const parentGuestGuard: CanActivateFn = () => {
  const router = inject(Router);
  const token = localStorage.getItem('parentToken');
  if (!token || !isTokenValid(token)) {
    clearSession();
    return true;
  }
  router.navigate(['/dashboard']);
  return false;
};
