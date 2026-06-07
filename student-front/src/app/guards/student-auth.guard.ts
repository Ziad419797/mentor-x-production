import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { StudentAuthService } from '../services/student-auth.service';

export const studentAuthGuard: CanActivateFn = () => {
  const auth   = inject(StudentAuthService);
  const router = inject(Router);
  if (auth.isLoggedIn) return true;
  return router.createUrlTree(['/login']);
};

export const studentGuestGuard: CanActivateFn = () => {
  const auth   = inject(StudentAuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn) return true;
  return router.createUrlTree(['/home']);
};
