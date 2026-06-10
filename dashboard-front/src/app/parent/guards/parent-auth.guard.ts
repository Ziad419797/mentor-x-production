import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const parentAuthGuard: CanActivateFn = () => {
  const router = inject(Router);
  const token = localStorage.getItem('parentToken');
  if (token) return true;
  router.navigate(['/parent/login']);
  return false;
};

export const parentGuestGuard: CanActivateFn = () => {
  const router = inject(Router);
  const token = localStorage.getItem('parentToken');
  if (!token) return true;
  router.navigate(['/parent/dashboard']);
  return false;
};
