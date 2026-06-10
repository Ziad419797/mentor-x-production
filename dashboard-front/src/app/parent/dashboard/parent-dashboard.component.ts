import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ParentApiService } from '../services/parent-api.service';

@Component({
  selector: 'app-parent-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div>
      <!-- Page Header -->
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-white">لوحة التحكم</h1>
        <p class="text-slate-400 text-sm mt-1">متابعة أداء أبنائك الدراسي</p>
      </div>

      <!-- Summary Stats -->
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-8" *ngIf="summary()">
        <div class="edu-card !p-4 text-center">
          <div class="text-2xl font-bold text-white">{{ summary().childrenCount }}</div>
          <div class="text-slate-400 text-xs mt-1">الأبناء المسجّلون</div>
        </div>
        <div class="edu-card !p-4 text-center">
          <div class="text-2xl font-bold text-emerald-400">
            {{ totalEnrollments() }}
          </div>
          <div class="text-slate-400 text-xs mt-1">الاشتراكات النشطة</div>
        </div>
        <div class="edu-card !p-4 text-center">
          <div class="text-2xl font-bold text-indigo-400">
            {{ totalLessons() }}
          </div>
          <div class="text-slate-400 text-xs mt-1">الدروس المكتملة</div>
        </div>
        <div class="edu-card !p-4 text-center">
          <div class="text-2xl font-bold" [class.text-red-400]="summary().totalUnreadNotifications > 0"
               [class.text-slate-400]="summary().totalUnreadNotifications === 0">
            {{ summary().totalUnreadNotifications }}
          </div>
          <div class="text-slate-400 text-xs mt-1">إشعارات غير مقروءة</div>
        </div>
      </div>

      <!-- Loading skeleton -->
      <div *ngIf="loading()" class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div *ngFor="let i of [1,2]" class="edu-card !p-6 animate-pulse">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 rounded-full bg-slate-700"></div>
            <div class="flex-1 space-y-2">
              <div class="h-4 bg-slate-700 rounded w-3/4"></div>
              <div class="h-3 bg-slate-700 rounded w-1/2"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Children Cards -->
      <div *ngIf="!loading() && summary()">
        <h2 class="text-white font-semibold mb-4">أبنائي</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <a *ngFor="let child of summary().children"
             [routerLink]="['/parent/child', child.studentId]"
             class="edu-card !p-5 block transition-all hover:border-emerald-500/40 hover:shadow-lg hover:-translate-y-0.5"
             style="cursor:pointer;">

            <!-- Child header -->
            <div class="flex items-center gap-4 mb-4">
              <!-- Avatar -->
              <div class="w-14 h-14 rounded-full flex items-center justify-center text-white font-bold text-lg flex-shrink-0 overflow-hidden"
                   style="background:linear-gradient(135deg,#059669,#10b981)">
                <img *ngIf="child.profileImageUrl" [src]="child.profileImageUrl"
                     class="w-full h-full object-cover" [alt]="child.studentName" />
                <span *ngIf="!child.profileImageUrl">{{ getInitials(child.studentName) }}</span>
              </div>

              <!-- Name & details -->
              <div class="flex-1 min-w-0">
                <h3 class="text-white font-semibold truncate">{{ child.studentName }}</h3>
                <p class="text-slate-400 text-xs">{{ child.grade }} • {{ child.studyType }}</p>
                <p class="text-slate-500 text-xs font-mono">{{ child.studentCode }}</p>
              </div>

              <!-- Unread badge -->
              <div *ngIf="child.unreadNotifications > 0"
                   class="flex-shrink-0 w-6 h-6 rounded-full bg-red-500 text-white text-xs flex items-center justify-center font-bold">
                {{ child.unreadNotifications }}
              </div>
            </div>

            <!-- Stats row -->
            <div class="grid grid-cols-3 gap-2 pt-3 border-t border-slate-700/60">
              <div class="text-center">
                <div class="text-white font-semibold text-lg">{{ child.activeEnrollments }}</div>
                <div class="text-slate-500 text-xs">اشتراكات</div>
              </div>
              <div class="text-center">
                <div class="text-emerald-400 font-semibold text-lg">{{ child.completedLessons }}</div>
                <div class="text-slate-500 text-xs">دروس مكتملة</div>
              </div>
              <div class="text-center">
                <div class="text-indigo-400 font-semibold text-lg">{{ child.totalAttendance }}</div>
                <div class="text-slate-500 text-xs">حضور</div>
              </div>
            </div>

            <!-- View arrow -->
            <div class="flex items-center justify-end mt-3 text-slate-500 text-xs gap-1">
              <span>عرض التفاصيل</span>
              <span class="material-icons-round text-sm">chevron_left</span>
            </div>
          </a>
        </div>

        <!-- Empty state -->
        <div *ngIf="summary().children?.length === 0" class="text-center py-16 text-slate-400">
          <span class="material-icons-round text-5xl mb-3 block opacity-30">people</span>
          <p>لا يوجد أبناء مسجّلون حتى الآن</p>
        </div>
      </div>

      <!-- Error state -->
      <div *ngIf="!loading() && !summary()" class="text-center py-16 text-slate-400">
        <span class="material-icons-round text-5xl mb-3 block opacity-30">cloud_off</span>
        <p>تعذّر تحميل البيانات</p>
        <button (click)="load()" class="mt-4 text-indigo-400 hover:underline text-sm">إعادة المحاولة</button>
      </div>
    </div>
  `
})
export class ParentDashboardComponent implements OnInit {
  summary = signal<any>(null);
  loading = signal(true);

  constructor(private parentApi: ParentApiService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.parentApi.getDashboardSummary().subscribe({
      next: (res: any) => {
        this.summary.set(res?.data ?? res);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  totalEnrollments(): number {
    return (this.summary()?.children || []).reduce((s: number, c: any) => s + (c.activeEnrollments || 0), 0);
  }

  totalLessons(): number {
    return (this.summary()?.children || []).reduce((s: number, c: any) => s + (c.completedLessons || 0), 0);
  }

  getInitials(name: string): string {
    if (!name) return '؟';
    return name.split(' ').map((n: string) => n[0]).join('').substring(0, 2);
  }
}
