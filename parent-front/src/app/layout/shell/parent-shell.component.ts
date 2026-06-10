import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink } from '@angular/router';
import { ParentAuthService } from '../../services/parent-auth.service';
import { ParentApiService } from '../../services/parent-api.service';
import { ThemeService } from '../../services/theme.service';

@Component({
  selector: 'app-parent-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  template: `
    <div style="min-height:100vh;background:var(--bg-body);direction:rtl;">

      <!-- Header -->
      <header style="background:var(--bg-card);border-bottom:1px solid var(--border);
                     position:sticky;top:0;z-index:50;padding:10px 16px;
                     display:flex;align-items:center;justify-content:space-between;">

        <!-- Logo -->
        <a routerLink="/dashboard" style="display:flex;align-items:center;gap:10px;text-decoration:none;flex-shrink:0;">
          <ng-container *ngIf="logoUrl() && !logoError(); else defaultLogo">
            <img [src]="logoUrl()" alt="Logo"
                 style="height:32px;max-width:130px;object-fit:contain;"
                 (error)="logoError.set(true)" />
          </ng-container>
          <ng-template #defaultLogo>
            <div style="width:32px;height:32px;border-radius:8px;
                        background:linear-gradient(135deg,#4BBBA0,#183764);
                        display:flex;align-items:center;justify-content:center;">
              <span class="material-icons-round" style="color:#fff;font-size:18px;">school</span>
            </div>
          </ng-template>
        </a>

        <!-- Right: actions -->
        <div style="display:flex;align-items:center;gap:8px;">

          <!-- Theme toggle -->
          <button (click)="theme.toggle()"
                  style="width:34px;height:34px;border-radius:50%;border:1px solid var(--border);
                         background:var(--bg-card-alt);cursor:pointer;display:flex;align-items:center;
                         justify-content:center;color:var(--text-secondary);flex-shrink:0;"
                  [title]="theme.theme() === 'dark' ? 'وضع فاتح' : 'وضع داكن'">
            <span class="material-icons-round" style="font-size:17px;">
              {{ theme.theme() === 'dark' ? 'light_mode' : 'dark_mode' }}
            </span>
          </button>

          <!-- Profile button -->
          <div style="position:relative;">
            <button (click)="togglePopup($event)"
                    style="display:flex;align-items:center;gap:8px;background:var(--bg-card-alt);
                           border:1px solid var(--border);border-radius:40px;padding:4px 10px 4px 4px;
                           cursor:pointer;">
              <div style="width:28px;height:28px;border-radius:50%;
                          background:linear-gradient(135deg,#4BBBA0,#183764);
                          display:flex;align-items:center;justify-content:center;
                          color:#fff;font-weight:700;font-size:12px;flex-shrink:0;">
                {{ initials() }}
              </div>
              <div style="text-align:right;line-height:1.2;">
                <div style="color:var(--text-primary);font-size:12px;font-weight:600;white-space:nowrap;">
                  {{ parentAuth.currentParent()?.name || parentName() || 'ولي الأمر' }}
                </div>
                <div style="color:var(--text-secondary);font-size:10px;">{{ childrenCount() }} أبناء</div>
              </div>
              <span class="material-icons-round"
                    style="font-size:15px;color:var(--text-secondary);transition:transform 200ms;"
                    [style.transform]="popupOpen() ? 'rotate(180deg)' : 'rotate(0)'">
                expand_more
              </span>
            </button>

            <!-- Popup -->
            <div *ngIf="popupOpen()" class="profile-popup" style="left:auto;right:0;">
              <!-- Parent header -->
              <div style="padding:14px 16px;border-bottom:1px solid var(--border);background:var(--bg-card-alt);">
                <div style="color:var(--text-primary);font-weight:700;font-size:13px;">
                  {{ parentAuth.currentParent()?.name || parentName() || 'ولي الأمر' }}
                </div>
                <div style="color:var(--text-secondary);font-size:11px;margin-top:2px;">
                  {{ parentAuth.currentParent()?.phone || '' }}
                </div>
              </div>

              <!-- Children -->
              <div style="padding:8px;">
                <p style="color:var(--text-secondary);font-size:10px;font-weight:700;
                           letter-spacing:.5px;padding:4px 8px;margin:0 0 4px;">أبنائي</p>

                <a *ngFor="let child of children(); let i = index"
                   [routerLink]="['/child', child.studentId]"
                   (click)="popupOpen.set(false)"
                   style="display:flex;align-items:center;gap:10px;padding:8px;border-radius:10px;
                          text-decoration:none;transition:background 150ms;cursor:pointer;"
                   onmouseover="this.style.background='var(--bg-card-alt)'"
                   onmouseout="this.style.background='transparent'">
                  <div style="width:32px;height:32px;border-radius:10px;flex-shrink:0;
                              display:flex;align-items:center;justify-content:center;
                              color:#fff;font-weight:700;font-size:12px;"
                       [style.background]="childGradient(i)">
                    {{ getInitials(child.studentName) }}
                  </div>
                  <div style="flex:1;min-width:0;">
                    <div style="color:var(--text-primary);font-size:12px;font-weight:600;
                                white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">
                      {{ child.studentName }}
                    </div>
                    <div style="color:var(--text-secondary);font-size:10px;">{{ child.grade }}</div>
                  </div>
                  <span class="material-icons-round" style="font-size:14px;color:var(--text-muted);">chevron_left</span>
                </a>

                <div *ngIf="children().length === 0"
                     style="text-align:center;padding:12px;color:var(--text-secondary);font-size:12px;">
                  لا يوجد أبناء
                </div>
              </div>

              <!-- Logout -->
              <div style="border-top:1px solid var(--border);padding:8px;">
                <button (click)="logout()"
                        style="width:100%;display:flex;align-items:center;gap:8px;padding:9px 10px;
                               border-radius:10px;background:none;border:none;cursor:pointer;
                               color:#f87171;font-size:13px;font-family:'Cairo',sans-serif;font-weight:600;
                               transition:background 150ms;"
                        onmouseover="this.style.background='rgba(248,113,113,.08)'"
                        onmouseout="this.style.background='none'">
                  <span class="material-icons-round" style="font-size:16px;">logout</span>
                  تسجيل الخروج
                </button>
              </div>
            </div>
          </div>

        </div>
      </header>

      <!-- Overlay -->
      <div *ngIf="popupOpen()"
           style="position:fixed;inset:0;z-index:49;"
           (click)="popupOpen.set(false)"></div>

      <main style="padding:20px 16px;max-width:540px;margin:0 auto;">
        <router-outlet />
      </main>
    </div>
  `
})
export class ParentShellComponent implements OnInit {
  parentName    = signal('');
  childrenCount = signal(0);
  children      = signal<any[]>([]);
  popupOpen     = signal(false);
  logoError     = signal(false);

  private _logoUrl     = signal<string | null>(null);
  private _darkLogoUrl = signal<string | null>(null);

  private gradients = [
    'linear-gradient(135deg,#183764,#0f4080)',
    'linear-gradient(135deg,#006b58,#004d3e)',
    'linear-gradient(135deg,#4a1d96,#2e1065)',
    'linear-gradient(135deg,#9d174d,#831843)',
    'linear-gradient(135deg,#7c3d00,#562d00)',
    'linear-gradient(135deg,#1e3a5f,#0f2040)',
  ];

  constructor(
    public parentAuth: ParentAuthService,
    public theme: ThemeService,
    private parentApi: ParentApiService
  ) {}

  ngOnInit(): void {
    this.parentApi.getDashboardSummary().subscribe({
      next: (res: any) => {
        const data = res?.data ?? res;
        this.parentName.set(data?.parentName || '');
        this.childrenCount.set(data?.childrenCount || 0);
        this.children.set(data?.children || []);
        this._logoUrl.set(data?.teacherLogoUrl || null);
        this._darkLogoUrl.set(data?.teacherDarkLogoUrl || null);
        if (data?.parentName) {
          const current = this.parentAuth.currentParent() || {};
          this.parentAuth.saveLogin(this.parentAuth.getToken()!, { ...current, name: data.parentName });
        }
      },
      error: () => {}
    });
  }

  logoUrl(): string | null {
    const isDark = this.theme.theme() === 'dark';
    return isDark
      ? (this._darkLogoUrl() || this._logoUrl())
      : (this._logoUrl() || this._darkLogoUrl());
  }

  initials(): string {
    const name = this.parentAuth.currentParent()?.name || this.parentName() || '';
    if (!name) return 'أ';
    return name.split(' ').map((n: string) => n[0]).join('').substring(0, 2).toUpperCase();
  }

  getInitials(name: string): string {
    if (!name) return '؟';
    return name.split(' ').map((n: string) => n[0]).join('').substring(0, 2);
  }

  childGradient(i: number): string {
    return this.gradients[i % this.gradients.length];
  }

  togglePopup(e: Event): void {
    e.stopPropagation();
    this.popupOpen.update(v => !v);
  }

  logout(): void {
    this.popupOpen.set(false);
    this.parentApi.parentLogout().subscribe({ error: () => {} });
    this.parentAuth.logout();
  }
}
