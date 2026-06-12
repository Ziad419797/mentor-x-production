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

function clearSession(): void {
  localStorage.removeItem('parentToken');
  localStorage.removeItem('parentProfile');
}

export const parentAuthGuard: CanActivateFn = () => {
  const router = inject(Router);
  const token = localStorage.getItem('parentToken');
  if (token && isTokenValid(token)) return true;
  clearSession();
  router.navigate(['/parent/login']);
  return false;
};
