import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from './api.service';
import { PermissionService } from './permission.service';
import { tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'accessToken';
  private readonly REFRESH_KEY = 'refreshToken';
  isLoggedIn = signal(this.hasToken());
  currentUser = signal<any>(null);

  constructor(
    private router: Router,
    private api: ApiService,
    private permissionService: PermissionService
  ) {
    if (this.hasToken()) {
      this.initProfile();
    }
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_KEY);
  }

  setToken(token: string, refreshToken?: string | null): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    if (refreshToken) localStorage.setItem(this.REFRESH_KEY, refreshToken);
    this.isLoggedIn.set(true);
    this.initProfile();
  }

  saveTokens(token: string, refreshToken?: string | null): void {
    this.setToken(token, refreshToken);
  }

  saveProfile(profile: any): void {
    this.currentUser.set(profile);
    this.permissionService.setProfile(profile);
  }

  updateAccessToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  initProfile() {
    this.api.getProfile().subscribe({
      next: (profile) => {
        this.currentUser.set(profile);
        this.permissionService.setProfile(profile);
      },
      error: (err) => {
        if (err.status === 403 || err.status === 401) {
          this.api.getStaffMe().subscribe({
            next: (profile) => {
              this.currentUser.set(profile);
              this.permissionService.setProfile(profile);
            },
            error: (e2) => {
              if (e2.status === 401) this.logout();
            }
          });
        }
      }
    });
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_KEY);
    this.isLoggedIn.set(false);
    this.currentUser.set(null);
    this.permissionService.setProfile(null);
    this.router.navigate(['/login']);
  }

  hasToken(): boolean {
    return !!localStorage.getItem(this.TOKEN_KEY);
  }
}
