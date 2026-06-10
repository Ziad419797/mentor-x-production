import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { ParentAuthService } from '../services/parent-auth.service';
import { ParentApiService } from '../services/parent-api.service';

@Component({
  selector: 'app-parent-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-screen" style="background:#0f172a; direction:rtl;">

      <!-- Top Header -->
      <header style="background:#1e293b; border-bottom:1px solid rgba(255,255,255,0.06);"
              class="sticky top-0 z-50 px-4 py-3 flex items-center justify-between">

        <!-- Logo -->
        <a routerLink="/parent/dashboard" class="flex items-center gap-3">
          <img src="assets/mentorx-logo-topbar.png" alt="Mentor-X"
               style="height:32px;object-fit:contain;" />
          <span class="text-white font-semibold text-sm hidden sm:block">بوابة أولياء الأمور</span>
        </a>

        <!-- Right: Parent name + Logout -->
        <div class="flex items-center gap-3">
          <div class="flex flex-col items-end">
            <span class="text-white text-sm font-medium">{{ parentAuth.currentParent()?.name || 'ولي الأمر' }}</span>
            <span class="text-slate-400 text-xs">{{ summary()?.childrenCount || 0 }} أبناء مسجّلون</span>
          </div>
          <div class="w-9 h-9 rounded-full flex items-center justify-center text-white font-bold text-sm"
               style="background:linear-gradient(135deg,#059669,#10b981)">
            {{ initials() }}
          </div>
          <button (click)="logout()"
                  class="flex items-center gap-1 text-slate-400 hover:text-red-400 transition-colors text-sm px-2 py-1 rounded">
            <span class="material-icons-round text-base">logout</span>
            <span class="hidden sm:block">خروج</span>
          </button>
        </div>
      </header>

      <!-- Main Content -->
      <main class="p-4 sm:p-6 max-w-5xl mx-auto">
        <router-outlet />
      </main>
    </div>
  `
})
export class ParentShellComponent implements OnInit {
  summary = signal<any>(null);

  constructor(
    public parentAuth: ParentAuthService,
    private parentApi: ParentApiService
  ) {}

  ngOnInit(): void {
    this.parentApi.getDashboardSummary().subscribe({
      next: (res: any) => {
        const data = res?.data ?? res;
        this.summary.set(data);
        if (data?.parentName) {
          const current = this.parentAuth.currentParent() || {};
          this.parentAuth.saveLogin(this.parentAuth.getToken()!, { ...current, name: data.parentName });
        }
      },
      error: () => {}
    });
  }

  initials(): string {
    const name = this.parentAuth.currentParent()?.name || '';
    return name.split(' ').map((n: string) => n[0]).join('').substring(0, 2).toUpperCase() || 'ولأ';
  }

  logout(): void {
    this.parentApi.parentLogout().subscribe({ error: () => {} });
    this.parentAuth.logout();
  }
}
