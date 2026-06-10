import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ParentAuthService } from '../services/parent-auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(ParentAuthService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401) {
        // Token expired or invalid — clear and redirect to login
        authService.logout();
      }
      return throwError(() => err);
    })
  );
};
