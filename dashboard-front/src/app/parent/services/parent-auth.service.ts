import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class ParentAuthService {
  private readonly TOKEN_KEY = 'parentToken';
  private readonly PROFILE_KEY = 'parentProfile';

  isLoggedIn = signal(this.hasToken());
  currentParent = signal<any>(this.loadProfile());

  constructor(private router: Router) {}

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  hasToken(): boolean {
    return !!localStorage.getItem(this.TOKEN_KEY);
  }

  private loadProfile(): any {
    const raw = localStorage.getItem(this.PROFILE_KEY);
    try { return raw ? JSON.parse(raw) : null; } catch { return null; }
  }

  saveLogin(token: string, profile?: any): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    if (profile) {
      localStorage.setItem(this.PROFILE_KEY, JSON.stringify(profile));
      this.currentParent.set(profile);
    }
    this.isLoggedIn.set(true);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.PROFILE_KEY);
    this.isLoggedIn.set(false);
    this.currentParent.set(null);
    this.router.navigate(['/parent/login']);
  }
}
