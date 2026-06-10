import { HttpInterceptorFn, HttpErrorResponse, HttpRequest, HttpHandlerFn, HttpClient, HttpBackend } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

const API_BASE = environment.apiBase;
const TOKEN_KEY = 'accessToken';
const REFRESH_KEY = 'refreshToken';

let isRefreshing = false;
const refreshedToken$ = new BehaviorSubject<string | null>(null);

function isAuthEndpoint(url: string): boolean {
  return url.includes('/api/auth/refresh') ||
         url.includes('/api/auth/teacher/login') ||
         url.includes('/api/auth/staff/login') ||
         url.includes('/api/auth/login') ||
         url.includes('/api/auth/teacher/forgot-password') ||
         url.includes('/api/auth/teacher/verify-otp') ||
         url.includes('/api/auth/teacher/reset-password') ||
         url.includes('/api/auth/staff/forgot-password') ||
         url.includes('/api/auth/staff/verify-otp') ||
         url.includes('/api/auth/staff/reset-password') ||
         url.includes('/api/auth/forgot-password') ||
         url.includes('/api/auth/verify-otp') ||
         url.includes('/api/auth/reset-password') ||
         url.includes('/api/auth/resend-otp');
}

function logoutAndRedirect(router: Router): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_KEY);
  isRefreshing = false;
  refreshedToken$.next(null);
  router.navigate(['/login']);
}

function attachToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
}

function handle401(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  http: HttpClient,
  router: Router
): Observable<any> {
  const refreshToken = localStorage.getItem(REFRESH_KEY);
  if (isAuthEndpoint(req.url) || !refreshToken) {
    logoutAndRedirect(router);
    return throwError(() => new HttpErrorResponse({ status: 401 }));
  }

  if (!isRefreshing) {
    isRefreshing = true;
    refreshedToken$.next(null);

    return http.post<any>(`${API_BASE}/api/auth/refresh`, {}, {
      headers: { Authorization: `Bearer ${refreshToken}` }
    }).pipe(
      switchMap((res: any) => {
        const newToken = res?.data?.token ?? res?.token ?? res?.data?.accessToken ?? res?.accessToken;
        if (!newToken) {
          throw new Error('No token in refresh response');
        }
        localStorage.setItem(TOKEN_KEY, newToken);
        isRefreshing = false;
        refreshedToken$.next(newToken);
        return next(attachToken(req, newToken));
      }),
      catchError((err) => {
        isRefreshing = false;
        refreshedToken$.next(null);
        logoutAndRedirect(router);
        return throwError(() => err);
      })
    );
  }

  return refreshedToken$.pipe(
    filter(token => token !== null),
    take(1),
    switchMap(token => next(attachToken(req, token)))
  );
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const backend = inject(HttpBackend);
  const http = new HttpClient(backend);
  const token = localStorage.getItem(TOKEN_KEY);

  if (!token && !isAuthEndpoint(req.url)) {
    const refreshToken = localStorage.getItem(REFRESH_KEY);
    if (refreshToken) {
      return handle401(req, next, http, router);
    }
    router.navigate(['/login']);
    return throwError(() => new HttpErrorResponse({ status: 401 }));
  }

  const authReq = attachToken(req, token);

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        return handle401(req, next, http, router);
      }
      return throwError(() => error);
    })
  );
};
