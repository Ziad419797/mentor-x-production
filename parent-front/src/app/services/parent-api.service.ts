import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ParentApiService {
  private readonly base = environment.apiBase;

  constructor(private http: HttpClient) {}

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

  // ── Child Quiz Results ────────────────────────────────────────

  getChildQuizResults(studentId: number, page = 0, size = 10): Observable<any> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get(`${this.base}/api/parent/children/${studentId}/quizzes`,
      { headers: this.headers(), params });
  }

  // ── Child Assignment Results ──────────────────────────────────

  getChildAssignmentResults(studentId: number, page = 0, size = 10): Observable<any> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get(`${this.base}/api/parent/children/${studentId}/assignments`,
      { headers: this.headers(), params });
  }

  // ── Child Wallet ──────────────────────────────────────────────

  getChildWallet(studentId: number): Observable<any> {
    return this.http.get(`${this.base}/api/parent/children/${studentId}/wallet`,
      { headers: this.headers() });
  }

  // ── Child Activity Log ───────────────────────────────────────
  getChildActivity(studentId: number, page = 0, size = 15): Observable<any> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get(`${this.base}/api/parent/children/${studentId}/activity`,
      { headers: this.headers(), params });
  }

  // ── Child Analytics ───────────────────────────────────────────
  getChildAnalytics(studentId: number): Observable<any> {
    return this.http.get(`${this.base}/api/analytics/parent/child/${studentId}/all`,
      { headers: this.headers() });
  }
}
