import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { StudentAuthService } from '../services/student-auth.service';

function isTokenValid(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    if (!payload.exp) return true;
    return payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
}

export const studentAuthGuard: CanActivateFn = () => {
  const auth   = inject(StudentAuthService);
  const router = inject(Router);
  const token  = auth.token;
  if (token && isTokenValid(token)) return true;
  auth.logout();
  return router.createUrlTree(['/login']);
};

export const studentGuestGuard: CanActivateFn = () => {
  const auth   = inject(StudentAuthService);
  const router = inject(Router);
  const token  = auth.token;
  if (!token || !isTokenValid(token)) {
    if (token) auth.logout(); // clear expired token
    return true;
  }
  return router.createUrlTree(['/home']);
};
