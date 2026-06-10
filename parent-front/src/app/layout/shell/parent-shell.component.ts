import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink } from '@angular/router';
import { ParentAuthService } from '../../services/parent-auth.service';
import { ParentApiService } from '../../services/parent-api.service';

@Component({
  selector: 'app-parent-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  template: `
    <div style="min-height:100vh;background:#0f172a;direction:rtl;">

      <!-- Header — matches student app top bar -->
      <header style="background:#162033;border-bottom:1px solid #1e293b;position:sticky;top:0;z-index:50;padding:10px 16px;display:flex;align-items:center;justify-content:space-between;">

        <a routerLink="/dashboard" style="display:flex;align-items:center;gap:10px;text-decoration:none;">
          <img src="assets/mentorx-logo-topbar.png" alt="Mentor-X" style="height:30px;object-fit:contain;" />
          <span style="color:#8892a0;font-size:12px;font-weight:600;border-right:1px solid #1e293b;padding-right:10px;">بوابة أولياء الأمور</span>
        </a>

        <div style="display:flex;align-items:center;gap:10px;">
          <div style="text-align:left;">
            <div style="color:#fff;font-size:13px;font-weight:600;">{{ parentAuth.currentParent()?.name || parentName() || 'ولي الأمر' }}</div>
            <div style="color:#8892a0;font-size:11px;">{{ childrenCount() }} أبناء</div>
          </div>
          <!-- Avatar with teal gradient matching student app -->
          <div style="width:34px;height:34px;border-radius:50%;background:linear-gradient(135deg,#4BBBA0,#183764);display:flex;align-items:center;justify-content:center;color:#fff;font-weight:700;font-size:13px;flex-shrink:0;">
            {{ initials() }}
          </div>
          <button (click)="logout()"
                  style="display:flex;align-items:center;gap:4px;color:#8892a0;background:none;border:none;cursor:pointer;font-size:12px;padding:6px;border-radius:6px;"
                  onmouseover="this.style.color='#ef4444'" onmouseout="this.style.color='#8892a0'">
            <span class="material-icons-round" style="font-size:17px;">logout</span>
          </button>
        </div>
      </header>

      <main style="padding:20px 16px;max-width:540px;margin:0 auto;">
        <router-outlet />
      </main>
    </div>
  `
})
export class ParentShellComponent implements OnInit {
  parentName = signal('');
  childrenCount = signal(0);

  constructor(
    public parentAuth: ParentAuthService,
    private parentApi: ParentApiService
  ) {}

  ngOnInit(): void {
    this.parentApi.getDashboardSummary().subscribe({
      next: (res: any) => {
        const data = res?.data ?? res;
        this.parentName.set(data?.parentName || '');
        this.childrenCount.set(data?.childrenCount || 0);
        if (data?.parentName) {
          const current = this.parentAuth.currentParent() || {};
          this.parentAuth.saveLogin(this.parentAuth.getToken()!, { ...current, name: data.parentName });
        }
      },
      error: () => {}
    });
  }

  initials(): string {
    const name = this.parentAuth.currentParent()?.name || this.parentName() || '';
    if (!name) return 'أ';
    return name.split(' ').map((n: string) => n[0]).join('').substring(0, 2).toUpperCase();
  }

  logout(): void {
    this.parentApi.parentLogout().subscribe({ error: () => {} });
    this.parentAuth.logout();
  }
}
