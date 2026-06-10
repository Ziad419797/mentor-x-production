import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

const API = environment.apiBase;

@Injectable({ providedIn: 'root' })
export class StudentAuthService {
  constructor(private http: HttpClient) {}

  get token()     { return localStorage.getItem('s_token') || ''; }
  get deviceId()  { return localStorage.getItem('s_deviceId') || 'web'; }
  get isLoggedIn(){ return !!this.token; }

  headers() {
    return new HttpHeaders({
      'Authorization': `Bearer ${this.token}`,
      'X-Device-Id': this.deviceId
    });
  }

  login(phone: string, password: string): Observable<any> {
    const deviceId = localStorage.getItem('s_deviceId') || crypto.randomUUID();
    localStorage.setItem('s_deviceId', deviceId);
    return this.http.post(`${API}/api/auth/student/login`, { phone, password, deviceId }).pipe(
      map((r: any) => r.data ?? r),
      tap((d: any) => {
        localStorage.setItem('s_token', d.token || d.accessToken || '');
        localStorage.setItem('s_refreshToken', d.refreshToken || '');
        localStorage.setItem('s_studentCode', d.studentCode || '');
        localStorage.setItem('s_phone', phone);
        localStorage.setItem('s_accountStatus', d.accountStatus || 'ACTIVE');
      })
    );
  }

  logout() {
    ['s_token','s_refreshToken','s_studentCode','s_phone','s_accountStatus'].forEach(k => localStorage.removeItem(k));
  }

  getMe(): Observable<any> {
    return this.http.get(`${API}/api/auth/me`, { headers: this.headers() }).pipe(map((r: any) => r.data ?? r));
  }
}
