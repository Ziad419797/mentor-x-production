import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  return next(req).pipe(
    catchError(err => {
      if (err.status === 401) {
        // امسح بيانات الجلسة وودي للـ login
        ['s_token','s_refreshToken','s_studentCode','s_phone','s_accountStatus'].forEach(k => localStorage.removeItem(k));
        router.navigate(['/login']);
      }
      return throwError(() => err);
    })
  );
};
