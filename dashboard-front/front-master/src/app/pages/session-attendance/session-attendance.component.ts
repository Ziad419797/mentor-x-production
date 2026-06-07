import { Component, OnInit, signal, computed } from '@angular/core';
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
  governorate?: string;
  centerName?: string;
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
          <input type="text" [(ngModel)]="searchQuery"
            class="filter-search" placeholder="اسم / كود الطالب...">
        </div>
        <select [(ngModel)]="filterType" class="filter-input" style="width:90px">
          <option value="">النوع</option>
          <option value="ONLINE">أونلاين</option>
          <option value="CENTER">سنتر (QR)</option>
        </select>
        <select [(ngModel)]="filterGovernorate" class="filter-input" style="width:95px">
          <option value="">المحافظة</option>
          <option *ngFor="let g of governorateNames" [value]="g">{{ g }}</option>
        </select>
        <select [(ngModel)]="filterCenter" class="filter-input" style="width:95px">
          <option value="">السنتر</option>
          <option *ngFor="let c of centers" [value]="c.name">{{ c.name }}</option>
        </select>
        <button *ngIf="searchQuery || filterType || filterGovernorate || filterCenter"
          (click)="clearFilters()" title="مسح الفلاتر"
          class="flex items-center justify-center rounded-lg bg-slate-800 text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-colors border border-slate-700"
          style="height:30px;width:30px;flex-shrink:0">
          <span class="material-icons-round" style="font-size:14px">filter_alt_off</span>
        </button>
        <span class="text-slate-500" style="font-size:11px">{{ filteredRecords().length }}</span>
      </div>

      <!-- Table -->
      <div class="edu-card p-0 overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full text-xs">
            <thead class="bg-slate-800/70 border-b border-slate-700">
              <tr>
                <th class="px-3 py-3 text-right font-bold text-slate-400">الكود</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 min-w-[160px]">الطالب</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden md:table-cell">الدرس</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400">النوع</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell">المصدر</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400">وقت الحضور</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngIf="loading()">
                <td colspan="6" class="text-center py-10 text-slate-500">
                  <span class="material-icons-round animate-spin text-2xl">refresh</span>
                </td>
              </tr>
              <tr *ngIf="!loading() && filteredRecords().length === 0">
                <td colspan="6" class="text-center py-14 text-slate-500">
                  <span class="material-icons-round text-4xl opacity-20 block mb-2">groups</span>
                  لا يوجد طلبة سجّلوا حضورهم في هذه المحاضرة
                </td>
              </tr>
              <tr *ngFor="let r of filteredRecords()"
                class="border-b border-slate-800/40 hover:bg-slate-800/20 transition-colors">

                <td class="px-3 py-2.5">
                  <code class="text-[10px] text-amber-400 font-mono bg-amber-500/10 px-1.5 py-0.5 rounded">{{ r.studentCode || '—' }}</code>
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
                  <span class="text-slate-400">{{ r.weekTitle || '—' }}</span>
                </td>

                <td class="px-3 py-2.5">
                  <span [ngClass]="r.type === 'ONLINE'
                          ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                          : 'bg-blue-500/20 text-blue-400 border border-blue-500/30'"
                        class="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full">
                    <span class="material-icons-round text-[11px]">{{ r.type === 'ONLINE' ? 'wifi' : 'qr_code_scanner' }}</span>
                    {{ r.type === 'ONLINE' ? 'أونلاين' : 'سنتر' }}
                  </span>
                </td>

                <td class="px-3 py-2.5 hidden lg:table-cell">
                  <span class="text-slate-500 text-[10px]">{{ sourceLabel(r.source) }}</span>
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
  `
})
export class SessionAttendanceComponent implements OnInit {
  sessionId    = signal(0);
  sessionTitle = signal('');
  sessionType  = signal<'' | 'ONLINE' | 'CENTER' | 'BOTH'>('');
  loading      = signal(false);
  records      = signal<EnrichedAttendanceRecord[]>([]);

  searchQuery = '';
  filterType: AttendanceTypeFilter = '';
  filterGovernorate = '';
  filterCenter = '';

  // بيانات الفلاتر — زي صفحة الطلبة بالظبط
  governorateNames: string[] = [];
  centers: any[] = [];

  filteredRecords = computed(() => {
    const q = this.searchQuery.trim().toLowerCase();
    const type = this.filterType;
    const gov = this.filterGovernorate;
    const center = this.filterCenter;
    return this.records().filter(r => {
      if (type && (r.type as string) !== type) return false;
      if (gov && (r.governorate || '') !== gov) return false;
      if (center && (r.centerName || '') !== center) return false;
      if (q) {
        const hay = `${r.studentName ?? ''} ${r.studentCode ?? ''}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  });

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

    this.loadFilterOptions();
    this.loadAttendance();
  }

  goBack() { this.location.back(); }

  clearFilters() {
    this.searchQuery = '';
    this.filterType = '';
    this.filterGovernorate = '';
    this.filterCenter = '';
  }

  loadFilterOptions() {
    this.api.getCenters().subscribe({ next: (r: any) => this.centers = Array.isArray(r) ? r : (r?.data || []), error: () => {} });
    this.api.getGovernorateNames().subscribe({ next: (r: any) => this.governorateNames = Array.isArray(r) ? r : [], error: () => {} });
  }

  // إثراء سجلات الحضور ببيانات المحافظة و السنتر الخاصة بكل طالب
  // (مش موجودة في رد الباك إند للحضور، فبنجيبها من قائمة الطلبة و نربطها بالكود/الاسم)
  enrichWithStudentInfo(records: EnrichedAttendanceRecord[]) {
    if (!records || records.length === 0) return;
    this.api.getActiveStudents(0, 1000).subscribe({
      next: (page: any) => {
        const list: any[] = page?.content || [];
        if (!list.length) return;
        const byCode = new Map<string, any>();
        const byId   = new Map<number, any>();
        for (const s of list) {
          if (s.studentCode) byCode.set(s.studentCode, s);
          if (s.code)        byCode.set(s.code, s);
          if (s.id != null)  byId.set(s.id, s);
        }
        const updated = records.map(r => {
          const match = (r.studentId != null ? byId.get(r.studentId) : null)
                     || (r.studentCode ? byCode.get(r.studentCode) : null);
          if (!match) return r;
          return { ...r, governorate: match.governorate, centerName: match.centerName };
        });
        this.records.set(updated);
      },
      error: () => {}
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
        if (!weeks || weeks.length === 0) { this.loading.set(false); return; }
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
          this.enrichWithStudentInfo(merged);
        });
      },
      error: () => { this.loading.set(false); this.toastr.error('تعذر تحميل بيانات الحضور'); }
    });
  }
}
