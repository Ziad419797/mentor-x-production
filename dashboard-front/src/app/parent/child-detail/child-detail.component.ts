import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { ParentApiService } from '../services/parent-api.service';

type Tab = 'overview' | 'attendance' | 'enrollments';

@Component({
  selector: 'app-child-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div>

      <!-- Back -->
      <a routerLink="/parent/dashboard" class="inline-flex items-center gap-1 text-slate-400 hover:text-white text-sm mb-5 transition-colors">
        <span class="material-icons-round text-base">arrow_forward</span>
        العودة للرئيسية
      </a>

      <!-- Child header card -->
      <div class="edu-card !p-5 mb-6" *ngIf="overview()">
        <div class="flex items-center gap-4">
          <div class="w-16 h-16 rounded-full flex items-center justify-center text-white font-bold text-xl flex-shrink-0 overflow-hidden"
               style="background:linear-gradient(135deg,#059669,#10b981)">
            <img *ngIf="overview().profileImageUrl" [src]="overview().profileImageUrl"
                 class="w-full h-full object-cover" />
            <span *ngIf="!overview().profileImageUrl">{{ getInitials(overview().studentName) }}</span>
          </div>
          <div>
            <h1 class="text-xl font-bold text-white">{{ overview().studentName }}</h1>
            <p class="text-slate-400 text-sm">{{ overview().grade }} • {{ overview().studyType }}</p>
            <p class="text-slate-500 text-xs font-mono">{{ overview().studentCode }}</p>
          </div>
        </div>
      </div>

      <!-- Loading -->
      <div *ngIf="overviewLoading()" class="edu-card !p-5 mb-6 animate-pulse">
        <div class="flex items-center gap-4">
          <div class="w-16 h-16 rounded-full bg-slate-700"></div>
          <div class="space-y-2 flex-1">
            <div class="h-5 bg-slate-700 rounded w-1/2"></div>
            <div class="h-3 bg-slate-700 rounded w-1/3"></div>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <div class="flex gap-1 mb-6 border-b border-slate-700/60 pb-0">
        <button *ngFor="let t of tabs" (click)="activeTab.set(t.key)"
                class="px-4 py-2.5 text-sm font-medium transition-all border-b-2 -mb-px"
                [class.border-emerald-500]="activeTab() === t.key"
                [class.text-emerald-400]="activeTab() === t.key"
                [class.border-transparent]="activeTab() !== t.key"
                [class.text-slate-400]="activeTab() !== t.key">
          <span class="material-icons-round text-sm align-middle ml-1">{{ t.icon }}</span>
          {{ t.label }}
        </button>
      </div>

      <!-- ── Tab: Overview ────────────────────────────────────── -->
      <ng-container *ngIf="activeTab() === 'overview' && overview()">
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
          <div class="edu-card !p-4 text-center">
            <div class="text-2xl font-bold text-indigo-400">{{ overview().totalAttendance }}</div>
            <div class="text-slate-400 text-xs mt-1">إجمالي الحضور</div>
          </div>
          <div class="edu-card !p-4 text-center">
            <div class="text-2xl font-bold text-blue-400">{{ overview().centerAttendance }}</div>
            <div class="text-slate-400 text-xs mt-1">حضور المركز</div>
          </div>
          <div class="edu-card !p-4 text-center">
            <div class="text-2xl font-bold text-cyan-400">{{ overview().onlineAttendance }}</div>
            <div class="text-slate-400 text-xs mt-1">حضور أونلاين</div>
          </div>
          <div class="edu-card !p-4 text-center">
            <div class="text-2xl font-bold text-emerald-400">{{ overview().activeEnrollments }}</div>
            <div class="text-slate-400 text-xs mt-1">اشتراكات نشطة</div>
          </div>
        </div>

        <!-- Lesson progress summary -->
        <div class="edu-card !p-5">
          <h3 class="text-white font-semibold mb-4">تقدّم الدراسة</h3>
          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="w-3 h-3 rounded-full bg-emerald-500 inline-block"></span>
                <span class="text-slate-300 text-sm">دروس مكتملة</span>
              </div>
              <span class="text-white font-semibold">{{ overview().completedLessons }}</span>
            </div>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="w-3 h-3 rounded-full bg-amber-500 inline-block"></span>
                <span class="text-slate-300 text-sm">دروس قيد التقدّم</span>
              </div>
              <span class="text-white font-semibold">{{ overview().inProgressLessons }}</span>
            </div>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="w-3 h-3 rounded-full bg-slate-600 inline-block"></span>
                <span class="text-slate-300 text-sm">دروس مقفلة</span>
              </div>
              <span class="text-white font-semibold">{{ overview().lockedLessons }}</span>
            </div>
          </div>

          <!-- Progress bar -->
          <div class="mt-5" *ngIf="totalLessons(overview()) > 0">
            <div class="flex justify-between text-xs text-slate-400 mb-1">
              <span>نسبة الإنجاز</span>
              <span>{{ completionPct(overview()) }}%</span>
            </div>
            <div class="h-2 rounded-full bg-slate-700 overflow-hidden">
              <div class="h-full rounded-full bg-emerald-500 transition-all"
                   [style.width.%]="completionPct(overview())"></div>
            </div>
          </div>
        </div>

        <!-- School & Center info -->
        <div class="edu-card !p-5 mt-4" *ngIf="overview().schoolName || overview().centerName">
          <h3 class="text-white font-semibold mb-3">معلومات إضافية</h3>
          <div class="space-y-2 text-sm">
            <div *ngIf="overview().schoolName" class="flex justify-between">
              <span class="text-slate-400">المدرسة</span>
              <span class="text-white">{{ overview().schoolName }}</span>
            </div>
            <div *ngIf="overview().centerName" class="flex justify-between">
              <span class="text-slate-400">المركز</span>
              <span class="text-white">{{ overview().centerName }}</span>
            </div>
            <div *ngIf="overview().governorate" class="flex justify-between">
              <span class="text-slate-400">المحافظة</span>
              <span class="text-white">{{ overview().governorate }}</span>
            </div>
          </div>
        </div>
      </ng-container>

      <!-- ── Tab: Attendance ──────────────────────────────────── -->
      <ng-container *ngIf="activeTab() === 'attendance'">
        <div *ngIf="attendanceLoading()" class="space-y-2">
          <div *ngFor="let i of [1,2,3,4,5]" class="edu-card !p-3 animate-pulse h-12"></div>
        </div>

        <div *ngIf="!attendanceLoading()">
          <div *ngIf="attendancePage().content.length === 0" class="text-center py-12 text-slate-400">
            <span class="material-icons-round text-4xl mb-2 block opacity-30">event_busy</span>
            <p>لا توجد سجلات حضور</p>
          </div>

          <div *ngIf="attendancePage().content.length > 0" class="space-y-2">
            <div *ngFor="let rec of attendancePage().content"
                 class="edu-card !p-3 flex items-center justify-between">
              <div class="flex items-center gap-3">
                <span class="material-icons-round text-base"
                      [class.text-emerald-400]="rec.status === 'PRESENT'"
                      [class.text-red-400]="rec.status === 'ABSENT'"
                      [class.text-amber-400]="rec.status === 'LATE'">
                  {{ rec.status === 'PRESENT' ? 'check_circle' : rec.status === 'ABSENT' ? 'cancel' : 'schedule' }}
                </span>
                <div>
                  <p class="text-white text-sm font-medium">{{ rec.sessionTitle || 'محاضرة' }}</p>
                  <p class="text-slate-400 text-xs">{{ rec.courseName || '' }}</p>
                </div>
              </div>
              <div class="text-left text-xs text-slate-400 flex flex-col items-end gap-1">
                <span class="px-2 py-0.5 rounded text-xs font-medium"
                      [class.bg-emerald-500/20]="rec.status === 'PRESENT'"
                      [class.text-emerald-400]="rec.status === 'PRESENT'"
                      [class.bg-red-500/20]="rec.status === 'ABSENT'"
                      [class.text-red-400]="rec.status === 'ABSENT'"
                      [class.bg-amber-500/20]="rec.status === 'LATE'"
                      [class.text-amber-400]="rec.status === 'LATE'">
                  {{ statusLabel(rec.status) }}
                </span>
                <span>{{ rec.attendanceType === 'CENTER' ? 'حضوري' : 'أونلاين' }}</span>
                <span>{{ formatDate(rec.attendedAt || rec.date) }}</span>
              </div>
            </div>
          </div>

          <!-- Pagination -->
          <div *ngIf="attendancePage().totalPages > 1" class="flex justify-center gap-2 mt-6">
            <button (click)="loadAttendance(attendancePage().number - 1)"
                    [disabled]="attendancePage().number === 0"
                    class="px-3 py-1.5 rounded text-sm text-slate-400 hover:text-white disabled:opacity-30 transition-colors border border-slate-700">
              السابق
            </button>
            <span class="px-3 py-1.5 text-slate-400 text-sm">
              {{ attendancePage().number + 1 }} / {{ attendancePage().totalPages }}
            </span>
            <button (click)="loadAttendance(attendancePage().number + 1)"
                    [disabled]="attendancePage().number + 1 >= attendancePage().totalPages"
                    class="px-3 py-1.5 rounded text-sm text-slate-400 hover:text-white disabled:opacity-30 transition-colors border border-slate-700">
              التالي
            </button>
          </div>
        </div>
      </ng-container>

      <!-- ── Tab: Enrollments ─────────────────────────────────── -->
      <ng-container *ngIf="activeTab() === 'enrollments'">
        <div *ngIf="enrollmentsLoading()" class="space-y-2">
          <div *ngFor="let i of [1,2,3]" class="edu-card !p-4 animate-pulse h-20"></div>
        </div>

        <div *ngIf="!enrollmentsLoading()">
          <div *ngIf="enrollments().length === 0" class="text-center py-12 text-slate-400">
            <span class="material-icons-round text-4xl mb-2 block opacity-30">school</span>
            <p>لا توجد اشتراكات</p>
          </div>

          <div *ngIf="enrollments().length > 0" class="space-y-3">
            <div *ngFor="let enr of enrollments()" class="edu-card !p-4">
              <div class="flex items-start justify-between gap-3">
                <div class="flex-1 min-w-0">
                  <h4 class="text-white font-medium text-sm truncate">{{ enr.courseTitle }}</h4>
                  <p class="text-slate-400 text-xs mt-0.5">{{ enrollmentTypeLabel(enr.enrollmentType) }}</p>
                </div>
                <span class="flex-shrink-0 px-2 py-0.5 rounded text-xs font-medium"
                      [class.bg-emerald-500/20]="enr.status === 'ACTIVE'"
                      [class.text-emerald-400]="enr.status === 'ACTIVE'"
                      [class.bg-slate-500/20]="enr.status !== 'ACTIVE'"
                      [class.text-slate-400]="enr.status !== 'ACTIVE'">
                  {{ enrollmentStatusLabel(enr.status) }}
                </span>
              </div>

              <!-- Progress bar -->
              <div class="mt-3">
                <div class="flex justify-between text-xs text-slate-500 mb-1">
                  <span>التقدّم</span>
                  <span>{{ enr.progress | number:'1.0-0' }}%</span>
                </div>
                <div class="h-1.5 rounded-full bg-slate-700 overflow-hidden">
                  <div class="h-full rounded-full transition-all"
                       [style.width.%]="enr.progress"
                       [class.bg-emerald-500]="enr.status === 'ACTIVE'"
                       [class.bg-slate-500]="enr.status !== 'ACTIVE'"></div>
                </div>
              </div>

              <div class="flex gap-4 mt-2 text-xs text-slate-500">
                <span *ngIf="enr.enrolledAt">تسجيل: {{ formatDate(enr.enrolledAt) }}</span>
                <span *ngIf="enr.expiresAt">انتهاء: {{ formatDate(enr.expiresAt) }}</span>
              </div>
            </div>
          </div>
        </div>
      </ng-container>

    </div>
  `
})
export class ChildDetailComponent implements OnInit {
  studentId = 0;
  activeTab = signal<Tab>('overview');

  overviewLoading = signal(true);
  overview = signal<any>(null);

  attendanceLoading = signal(false);
  attendancePage = signal<any>({ content: [], number: 0, totalPages: 0 });

  enrollmentsLoading = signal(false);
  enrollments = signal<any[]>([]);

  tabs = [
    { key: 'overview' as Tab, label: 'نظرة عامة', icon: 'dashboard' },
    { key: 'attendance' as Tab, label: 'الحضور', icon: 'fact_check' },
    { key: 'enrollments' as Tab, label: 'الاشتراكات', icon: 'school' },
  ];

  constructor(
    private route: ActivatedRoute,
    private parentApi: ParentApiService
  ) {}

  ngOnInit(): void {
    this.studentId = +this.route.snapshot.paramMap.get('studentId')!;
    this.loadOverview();
    this.loadAttendance(0);
    this.loadEnrollments();

    this.route.queryParams.subscribe(p => {
      if (p['tab']) this.activeTab.set(p['tab'] as Tab);
    });
  }

  loadOverview(): void {
    this.overviewLoading.set(true);
    this.parentApi.getChildOverview(this.studentId).subscribe({
      next: (res: any) => { this.overview.set(res?.data ?? res); this.overviewLoading.set(false); },
      error: () => this.overviewLoading.set(false)
    });
  }

  loadAttendance(page: number): void {
    this.attendanceLoading.set(true);
    this.parentApi.getChildAttendance(this.studentId, page, 15).subscribe({
      next: (res: any) => {
        const r = res?.data ?? res;
        this.attendancePage.set({
          content: Array.isArray(r) ? r : (r?.content || []),
          number: r?.number ?? page,
          totalPages: r?.totalPages ?? (Array.isArray(r) ? 1 : 1),
          totalElements: r?.totalElements ?? 0
        });
        this.attendanceLoading.set(false);
      },
      error: () => this.attendanceLoading.set(false)
    });
  }

  loadEnrollments(): void {
    this.enrollmentsLoading.set(true);
    this.parentApi.getChildEnrollments(this.studentId).subscribe({
      next: (res: any) => {
        const data = res?.data ?? res;
        this.enrollments.set(Array.isArray(data) ? data : (data?.content || []));
        this.enrollmentsLoading.set(false);
      },
      error: () => this.enrollmentsLoading.set(false)
    });
  }

  getInitials(name: string): string {
    if (!name) return '؟';
    return name.split(' ').map((n: string) => n[0]).join('').substring(0, 2);
  }

  totalLessons(ov: any): number {
    return (ov?.completedLessons || 0) + (ov?.inProgressLessons || 0) + (ov?.lockedLessons || 0);
  }

  completionPct(ov: any): number {
    const total = this.totalLessons(ov);
    if (!total) return 0;
    return Math.round((ov.completedLessons / total) * 100);
  }

  statusLabel(s: string): string {
    return { PRESENT: 'حضر', ABSENT: 'غائب', LATE: 'متأخر' }[s] || s;
  }

  enrollmentTypeLabel(t: string): string {
    return { ONLINE: 'أونلاين', CENTER: 'حضوري', HYBRID: 'مختلط' }[t] || t || '';
  }

  enrollmentStatusLabel(s: string): string {
    return { ACTIVE: 'نشط', EXPIRED: 'منتهي', CANCELLED: 'ملغي', COMPLETED: 'مكتمل' }[s] || s;
  }

  formatDate(d: string): string {
    if (!d) return '';
    try {
      return new Date(d).toLocaleDateString('ar-EG', { year: 'numeric', month: 'short', day: 'numeric' });
    } catch { return d; }
  }
}
