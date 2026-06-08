import { Component, OnInit, signal } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AttendanceRecord } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

type AttendanceTypeFilter = '' | 'ONLINE' | 'CENTER';

interface EnrichedAttendanceRecord extends AttendanceRecord {
  studentCode?: string;
  studentPhone?: string;
  parentPhone?: string;
  governorate?: string;
  centerName?: string;
  groupName?: string;
  online?: boolean;
  enrolledAt?: string;
  progress?: number;
  completedLessonsCount?: number;
  totalLessonsCount?: number;
  totalWatchTimeSeconds?: number;
  quizScore?: number;
  quizPassed?: boolean;
  assignmentSubmitted?: boolean;
  assignmentScore?: number;
}

@Component({
  selector: 'app-session-attendance',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="space-y-4 animate-fade-in pb-10" dir="rtl">

      <!-- Header -->
      <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
        <div class="flex items-center gap-3">
          <button (click)="goBack()"
                  class="w-9 h-9 rounded-xl bg-slate-800 flex items-center justify-center text-slate-400 hover:text-white hover:bg-slate-700 transition-colors">
            <span class="material-icons-round text-base">arrow_forward</span>
          </button>
          <div>
            <h2 class="text-white font-black text-xl">حضور المحاضرة</h2>
            <p class="text-slate-500 text-xs mt-0.5">{{ sessionTitle() }}</p>
          </div>
          <span *ngIf="sessionType()" [ngClass]="{
                  'bg-emerald-900/30 text-emerald-400 border-emerald-800/30': sessionType() === 'ONLINE',
                  'bg-blue-900/30 text-blue-400 border-blue-800/30': sessionType() === 'CENTER',
                  'bg-amber-900/30 text-amber-400 border-amber-800/30': sessionType() === 'BOTH'
                }" class="text-[10px] border px-2 py-1 rounded-full font-bold">
            {{ sessionType() === 'ONLINE' ? 'أونلاين' : sessionType() === 'CENTER' ? 'سنتر' : 'سنتر+أونلاين' }}
          </span>
          <span class="bg-slate-700 text-slate-300 text-xs font-bold px-2 py-1 rounded-full">{{ filteredRecords().length }}</span>
        </div>
      </div>

      <!-- Search + Filters -->
      <div class="flex items-center gap-1.5 flex-wrap">
        <div class="relative">
          <span class="material-icons-round absolute right-2 top-1/2 -translate-y-1/2 text-slate-500" style="font-size:13px;pointer-events:none">search</span>
          <input type="text" [(ngModel)]="searchQuery" (ngModelChange)="onFiltersChanged()"
            class="filter-search" placeholder="اسم / كود الطالب...">
        </div>
        <select [(ngModel)]="filterType" (ngModelChange)="onFiltersChanged()" class="filter-input" style="width:90px">
          <option value="">النوع</option>
          <option value="ONLINE">أونلاين</option>
          <option value="CENTER">سنتر (QR)</option>
        </select>
        <select [(ngModel)]="filterGovernorate" (ngModelChange)="onFiltersChanged()" class="filter-input" style="width:95px">
          <option value="">المحافظة</option>
          <option *ngFor="let g of governorateNames" [value]="g">{{ g }}</option>
        </select>
        <select [(ngModel)]="filterCenter" (ngModelChange)="onCenterChanged()" class="filter-input" style="width:95px">
          <option value="">السنتر</option>
          <option *ngFor="let c of centers" [value]="c.name">{{ c.name }}</option>
        </select>
        <select *ngIf="filterCenter" [(ngModel)]="filterGroup" (ngModelChange)="onFiltersChanged()" class="filter-input" style="width:95px">
          <option value="">الجروب</option>
          <option *ngFor="let g of groupsForSelectedCenter" [value]="g.title || g.name">{{ g.title || g.name }}</option>
        </select>
        <button *ngIf="searchQuery || filterType || filterGovernorate || filterCenter || filterGroup"
          (click)="clearFilters()" title="مسح الفلاتر"
          class="flex items-center justify-center rounded-lg bg-slate-800 text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-colors border border-slate-700"
          style="height:30px;width:30px;flex-shrink:0">
          <span class="material-icons-round" style="font-size:14px">filter_alt_off</span>
        </button>
      </div>

      <!-- Table -->
      <div class="edu-card p-0 overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full text-xs">
            <thead class="bg-slate-800/70 border-b border-slate-700">
              <tr>
                <th class="px-3 py-3 text-right font-bold text-slate-400">كود الطالب</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 min-w-[160px]">الطالب</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden md:table-cell">الهاتف</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden md:table-cell">هاتف ولي الأمر</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell">المحافظة</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400">النوع</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell">السنتر</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell">الجروب</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell">المصدر</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400">التقدم</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400">وقت الحضور</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngIf="loading()">
                <td colspan="11" class="text-center py-10 text-slate-500">
                  <span class="material-icons-round animate-spin text-2xl">refresh</span>
                </td>
              </tr>
              <tr *ngIf="!loading() && filteredRecords().length === 0">
                <td colspan="11" class="text-center py-14 text-slate-500">
                  <span class="material-icons-round text-4xl opacity-20 block mb-2">groups</span>
                  لا يوجد طلبة سجّلوا حضورهم في هذه المحاضرة
                </td>
              </tr>
              <tr *ngFor="let r of filteredRecords()"
                class="border-b border-slate-800/40 hover:bg-slate-800/20 transition-colors">

                <td class="px-3 py-2.5">
                  <span class="text-slate-400 font-bold" dir="ltr">{{ r.studentCode || '—' }}</span>
                </td>

                <td class="px-3 py-2.5">
                  <div class="flex items-center gap-2 min-w-[150px]">
                    <div class="w-8 h-8 rounded-full flex-shrink-0 bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-xs font-black text-white">
                      {{ (r.studentName || '?')[0] }}
                    </div>
                    <span class="text-slate-200 font-semibold text-xs">{{ r.studentName || '—' }}</span>
                  </div>
                </td>

                <td class="px-3 py-2.5 hidden md:table-cell">
                  <span class="text-slate-400" dir="ltr">{{ r.studentPhone || '—' }}</span>
                </td>

                <td class="px-3 py-2.5 hidden md:table-cell">
                  <span class="text-slate-400" dir="ltr">{{ r.parentPhone || '—' }}</span>
                </td>

                <td class="px-3 py-2.5 hidden lg:table-cell">
                  <span class="text-slate-400">{{ r.governorate || '—' }}</span>
                </td>

                <td class="px-3 py-2.5">
                  <span class="px-2 py-0.5 rounded-full text-[10px] font-bold"
                        [ngClass]="r.type === 'ONLINE' ? 'bg-sky-500/15 text-sky-400' : 'bg-violet-500/15 text-violet-400'">
                    {{ r.type === 'ONLINE' ? 'أونلاين' : 'سنتر' }}
                  </span>
                </td>

                <td class="px-3 py-2.5 hidden lg:table-cell">
                  <span class="text-slate-400">{{ r.centerName || '—' }}</span>
                </td>

                <td class="px-3 py-2.5 hidden lg:table-cell">
                  <span class="text-slate-400">{{ r.type !== 'ONLINE' ? (r.groupName || '—') : '—' }}</span>
                </td>

                <td class="px-3 py-2.5 hidden lg:table-cell">
                  <span class="text-slate-500 text-[10px]">{{ sourceLabel(r.source) }}</span>
                </td>

                <td class="px-3 py-2.5" style="min-width:120px">
                  <button (click)="openProgressPopup(r)"
                          class="w-full text-right group cursor-pointer"
                          title="اضغط لمعرفة تفاصيل التقدم">
                    <div class="flex items-center justify-between mb-1">
                      <span class="text-[10px] text-slate-400 group-hover:text-indigo-400 transition-colors">{{ (r.progress ?? 0) | number:'1.0-0' }}%</span>
                      <span class="material-icons-round text-slate-600 group-hover:text-indigo-400 transition-colors" style="font-size:12px">info</span>
                    </div>
                    <div class="w-full h-1.5 rounded-full bg-slate-700/60 overflow-hidden">
                      <div class="h-full rounded-full transition-all"
                           [ngClass]="(r.progress ?? 0) >= 100 ? 'bg-emerald-500' : (r.progress ?? 0) > 0 ? 'bg-indigo-500' : 'bg-slate-600'"
                           [style.width.%]="r.progress ?? 0"></div>
                    </div>
                  </button>
                </td>

                <td class="px-3 py-2.5">
                  <div class="text-slate-400">{{ (r.attendedAt || r.date) | date:'d/M/yy' }}</div>
                  <div class="text-slate-500 text-[10px]">{{ (r.attendedAt || r.date) | date:'HH:mm' }}</div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

    </div>

    <!-- Progress detail popup -->
    <div *ngIf="popupRecord() as p"
         class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
         (click)="closeProgressPopup()">
      <div class="edu-card max-w-md w-full p-5 space-y-4" dir="rtl" (click)="$event.stopPropagation()">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <div class="w-9 h-9 rounded-full flex-shrink-0 bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-xs font-black text-white">
              {{ (p.studentName || '?')[0] }}
            </div>
            <div>
              <p class="text-white font-bold text-sm">{{ p.studentName || '—' }}</p>
              <p class="text-slate-500 text-[11px]" dir="ltr">{{ p.studentCode || '—' }}</p>
            </div>
          </div>
          <button (click)="closeProgressPopup()" class="w-8 h-8 rounded-lg bg-slate-800 flex items-center justify-center text-slate-400 hover:text-white hover:bg-slate-700 transition-colors">
            <span class="material-icons-round text-base">close</span>
          </button>
        </div>

        <div>
          <div class="flex items-center justify-between mb-1">
            <span class="text-slate-400 text-xs font-bold">نسبة التقدم في الكورس</span>
            <span class="text-white text-sm font-black">{{ (p.progress ?? 0) | number:'1.0-0' }}%</span>
          </div>
          <div class="w-full h-2 rounded-full bg-slate-700/60 overflow-hidden">
            <div class="h-full rounded-full transition-all"
                 [ngClass]="(p.progress ?? 0) >= 100 ? 'bg-emerald-500' : (p.progress ?? 0) > 0 ? 'bg-indigo-500' : 'bg-slate-600'"
                 [style.width.%]="p.progress ?? 0"></div>
          </div>
          <p class="text-slate-500 text-[11px] mt-1.5">
            أكمل {{ p.completedLessonsCount ?? 0 }} من {{ p.totalLessonsCount ?? 0 }} عنصر تعليمي (فيديوهات، كويزات، واجبات — بدون احتساب الملفات)
          </p>
        </div>

        <div class="grid grid-cols-2 gap-2.5">
          <div class="rounded-xl bg-slate-800/60 border border-slate-700/50 p-3">
            <div class="flex items-center gap-1.5 text-slate-400 text-[11px] font-bold mb-1">
              <span class="material-icons-round text-[14px]">visibility</span>
              مدة المشاهدة
            </div>
            <p class="text-white font-black text-sm" dir="ltr">{{ formatDuration(p.totalWatchTimeSeconds) }}</p>
          </div>
          <div class="rounded-xl bg-slate-800/60 border border-slate-700/50 p-3">
            <div class="flex items-center gap-1.5 text-slate-400 text-[11px] font-bold mb-1">
              <span class="material-icons-round text-[14px]">{{ p.type === 'ONLINE' ? 'wifi' : 'qr_code_scanner' }}</span>
              تسجيل الحضور
            </div>
            <p class="text-white font-black text-sm">{{ p.type === 'ONLINE' ? 'أونلاين' : 'سنتر' }} · {{ sourceLabel(p.source) }}</p>
          </div>
          <div class="rounded-xl bg-slate-800/60 border border-slate-700/50 p-3">
            <div class="flex items-center gap-1.5 text-slate-400 text-[11px] font-bold mb-1">
              <span class="material-icons-round text-[14px]">quiz</span>
              نتيجة الكويز
            </div>
            <p class="text-white font-black text-sm">
              <ng-container *ngIf="p.quizScore != null; else noQuiz">
                {{ p.quizScore }}% · <span [ngClass]="p.quizPassed ? 'text-emerald-400' : 'text-red-400'">{{ p.quizPassed ? 'ناجح' : 'راسب' }}</span>
              </ng-container>
              <ng-template #noQuiz><span class="text-slate-500 font-semibold">لم يتم الحل بعد</span></ng-template>
            </p>
          </div>
          <div class="rounded-xl bg-slate-800/60 border border-slate-700/50 p-3">
            <div class="flex items-center gap-1.5 text-slate-400 text-[11px] font-bold mb-1">
              <span class="material-icons-round text-[14px]">assignment</span>
              الواجب
            </div>
            <p class="text-white font-black text-sm">
              <ng-container *ngIf="p.assignmentSubmitted; else noAssignment">
                تم التسليم<ng-container *ngIf="p.assignmentScore != null"> · {{ p.assignmentScore }}%</ng-container>
              </ng-container>
              <ng-template #noAssignment><span class="text-slate-500 font-semibold">لم يتم التسليم</span></ng-template>
            </p>
          </div>
        </div>

        <p class="text-slate-500 text-[11px] flex items-center gap-1.5">
          <span class="material-icons-round text-[14px]">schedule</span>
          سجّل الحضور في {{ (p.attendedAt || p.date) | date:'d/M/yy - h:mm a' }}
        </p>
      </div>
    </div>
  `
})
export class SessionAttendanceComponent implements OnInit {
  sessionId    = signal(0);
  courseId     = signal<number | null>(null);
  sessionTitle = signal('');
  sessionType  = signal<'' | 'ONLINE' | 'CENTER' | 'BOTH'>('');
  loading      = signal(false);
  records      = signal<EnrichedAttendanceRecord[]>([]);
  popupRecord  = signal<EnrichedAttendanceRecord | null>(null);

  searchQuery = '';
  filterType: AttendanceTypeFilter = '';
  filterGovernorate = '';
  filterCenter = '';
  filterGroup = '';

  // بيانات الفلاتر — زي صفحة الطلبة بالظبط
  governorateNames: string[] = [];
  centers: any[] = [];
  groups: any[] = [];
  groupsForSelectedCenter: any[] = [];

  // نسخة مفلترة بنحدّثها يدوياً (مش computed لأن الفلاتر مش signals)
  private _filtered = signal<EnrichedAttendanceRecord[]>([]);
  filteredRecords() { return this._filtered(); }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private api: ApiService,
    private toastr: ToastrService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.sessionId.set(id);

    const qp = this.route.snapshot.queryParamMap;
    this.sessionTitle.set(qp.get('title') || '');
    this.sessionType.set((qp.get('type') as any) || '');
    if (qp.get('type') === 'CENTER') this.filterType = 'CENTER';
    else if (qp.get('type') === 'ONLINE') this.filterType = 'ONLINE';

    const cid = qp.get('courseId');
    if (cid) this.courseId.set(Number(cid));

    this.loadFilterOptions();
    this.loadAttendance();
  }

  goBack() { this.location.back(); }

  onCenterChanged() {
    this.filterGroup = '';
    this.refreshGroupsForCenter();
    this.applyFilters();
  }

  onFiltersChanged() { this.applyFilters(); }

  clearFilters() {
    this.searchQuery = '';
    this.filterType = '';
    this.filterGovernorate = '';
    this.filterCenter = '';
    this.filterGroup = '';
    this.groupsForSelectedCenter = [];
    this.applyFilters();
  }

  refreshGroupsForCenter() {
    if (!this.filterCenter) { this.groupsForSelectedCenter = []; return; }
    const center = this.centers.find(c => c.name === this.filterCenter);
    this.groupsForSelectedCenter = this.groups.filter(g =>
      (center && g.centerId === center.id) || g.centerName === this.filterCenter
    );
  }

  applyFilters() {
    const q = this.searchQuery.trim().toLowerCase();
    const type = this.filterType;
    const gov = this.filterGovernorate;
    const center = this.filterCenter;
    const group = this.filterGroup;
    const filtered = this.records().filter(r => {
      if (type && (r.type as string) !== type) return false;
      if (gov && (r.governorate || '') !== gov) return false;
      if (center && (r.centerName || '') !== center) return false;
      if (group && (r.groupName || '') !== group) return false;
      if (q) {
        const hay = `${r.studentName ?? ''} ${r.studentCode ?? ''}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
    this._filtered.set(filtered);
  }

  loadFilterOptions() {
    this.api.getCenters().subscribe({ next: (r: any) => this.centers = Array.isArray(r) ? r : (r?.data || []), error: () => {} });
    this.api.getGovernorateNames().subscribe({ next: (r: any) => this.governorateNames = Array.isArray(r) ? r : [], error: () => {} });
    this.api.getMyGroups().subscribe({ next: (r: any) => this.groups = Array.isArray(r) ? r : [], error: () => {} });
  }

  openProgressPopup(r: EnrichedAttendanceRecord) { this.popupRecord.set(r); }
  closeProgressPopup() { this.popupRecord.set(null); }

  formatDuration(seconds?: number): string {
    const s = Math.max(0, Math.round(seconds || 0));
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    if (h > 0) return `${h} س ${m} د`;
    if (m > 0) return `${m} د`;
    return `${s} ث`;
  }

  // إثراء سجلات الحضور ببيانات الطالب الكاملة (هاتف، ولي أمر، محافظة، سنتر، جروب)
  // + بيانات التقدم في الكورس (تيجي من الاشتراكات) — نفس فكرة صفحة المشتركين
  enrichWithStudentInfo(records: EnrichedAttendanceRecord[]) {
    if (!records || records.length === 0) return;

    const enrollments$ = this.courseId()
      ? this.api.getEnrollmentsByCourse(this.courseId()!).pipe(catchError(() => of([])))
      : of([] as any[]);

    forkJoin({
      students: this.api.getActiveStudents(0, 1000).pipe(catchError(() => of({ content: [] } as any))),
      enrollments: enrollments$
    }).subscribe(({ students, enrollments }) => {
      const list: any[] = (students as any)?.content || [];
      const byCode = new Map<string, any>();
      const byId   = new Map<number, any>();
      for (const s of list) {
        if (s.studentCode) byCode.set(s.studentCode, s);
        if (s.code)        byCode.set(s.code, s);
        if (s.id != null)  byId.set(s.id, s);
      }

      const enrByStudentId = new Map<number, any>();
      for (const e of (enrollments as any[])) {
        if (e?.studentId != null) enrByStudentId.set(e.studentId, e);
      }

      const updated = records.map(r => {
        const match = (r.studentId != null ? byId.get(r.studentId) : null)
                   || (r.studentCode ? byCode.get(r.studentCode) : null);
        const enr = r.studentId != null ? enrByStudentId.get(r.studentId) : null;
        return {
          ...r,
          studentCode: r.studentCode || match?.studentCode || match?.code,
          studentPhone: r.studentPhone || match?.studentPhone || match?.phone,
          parentPhone: match?.parentPhone,
          governorate: match?.governorate,
          centerName:  match?.centerName,
          groupName:   match?.groupName,
          online:      match?.online,
          progress: enr?.progress,
          completedLessonsCount: enr?.completedLessonsCount,
          totalLessonsCount: enr?.totalLessonsCount,
          totalWatchTimeSeconds: enr?.totalWatchTimeSeconds,
          quizScore: enr?.quizScore,
          quizPassed: enr?.quizPassed,
          assignmentSubmitted: enr?.assignmentSubmitted,
          assignmentScore: enr?.assignmentScore,
        } as EnrichedAttendanceRecord;
      });
      this.records.set(updated);
      this.applyFilters();
    });
  }

  sourceLabel(source?: string): string {
    if (source === 'QR_SCAN') return 'مسح QR';
    if (source === 'ONLINE_ACCESS') return 'دخول أونلاين';
    if (source === 'MANUAL') return 'يدوي';
    return '—';
  }

  // المحاضرة بترتبط بالحضور عن طريق "الدرس" (Week) مش مباشرة، فبنجمع
  // حضور كل الدروس المرتبطة بالمحاضرة دي في قائمة واحدة
  loadAttendance() {
    this.loading.set(true);
    this.records.set([]);
    this.api.getWeeksBySession(this.sessionId()).subscribe({
      next: (weeks: any[]) => {
        if (!weeks || weeks.length === 0) { this.loading.set(false); this.applyFilters(); return; }
        const calls = weeks.map(w =>
          this.api.getAttendanceByWeek(w.id).pipe(catchError(() => of([])))
        );
        forkJoin(calls).subscribe((lists: AttendanceRecord[][]) => {
          const merged: EnrichedAttendanceRecord[] = lists.flat();
          merged.sort((a, b) =>
            new Date(b.attendedAt || b.date || 0).getTime() -
            new Date(a.attendedAt || a.date || 0).getTime()
          );
          this.records.set(merged);
          this.loading.set(false);
          this.applyFilters();
          this.enrichWithStudentInfo(merged);
        });
      },
      error: () => { this.loading.set(false); this.toastr.error('تعذر تحميل بيانات الحضور'); }
    });
  }
}
