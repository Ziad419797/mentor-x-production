import { Injectable } from '@angular/core';
import { HttpClient, HttpBackend, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Parent-specific API service.
 * Uses HttpBackend directly to BYPASS the teacher auth interceptor,
 * then attaches the parentToken manually.
 */
@Injectable({ providedIn: 'root' })
export class ParentApiService {
  private readonly base = environment.apiBase;
  private http: HttpClient;

  constructor(backend: HttpBackend) {
    // Bypass the global auth interceptor
    this.http = new HttpClient(backend);
  }

  private headers(): HttpHeaders {
    const token = localStorage.getItem('parentToken');
    return new HttpHeaders(token ? { Authorization: `Bearer ${token}` } : {});
  }

  // ── Auth ──────────────────────────────────────────────────────

  startLogin(parentPhone: string): Observable<any> {
    return this.http.post(`${this.base}/api/parent/start-login`, { parentPhone });
  }

  completeLogin(parentPhone: string, otp: string): Observable<any> {
    return this.http.post(`${this.base}/api/parent/complete-login`, { parentPhone, otp });
  }

  parentLogout(): Observable<any> {
    return this.http.post(`${this.base}/api/parent/logout`, {}, { headers: this.headers() });
  }

  // ── Children ─────────────────────────────────────────────────

  getChildren(): Observable<any> {
    return this.http.get(`${this.base}/api/parent/children`, { headers: this.headers() });
  }

  // ── Dashboard ─────────────────────────────────────────────────

  getDashboardSummary(): Observable<any> {
    return this.http.get(`${this.base}/api/parent/dashboard/summary`, { headers: this.headers() });
  }

  getChildOverview(studentId: number): Observable<any> {
    return this.http.get(`${this.base}/api/parent/dashboard/child/${studentId}/overview`, { headers: this.headers() });
  }

  getChildAttendance(studentId: number, page = 0, size = 15): Observable<any> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get(`${this.base}/api/parent/dashboard/child/${studentId}/attendance`,
      { headers: this.headers(), params });
  }

  getChildEnrollments(studentId: number): Observable<any> {
    return this.http.get(`${this.base}/api/parent/dashboard/child/${studentId}/enrollments`, { headers: this.headers() });
  }

  getChildProgress(studentId: number, sessionId: number): Observable<any> {
    return this.http.get(`${this.base}/api/parent/dashboard/child/${studentId}/progress/${sessionId}`, { headers: this.headers() });
  }
}
