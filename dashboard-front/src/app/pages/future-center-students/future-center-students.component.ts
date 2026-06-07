import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { ToastrService } from 'ngx-toastr';
import { extractPage } from '../../core/api-response.model';

@Component({
  selector: 'app-future-center-students',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-4 animate-fade-in pb-10" dir="rtl">

      <!-- Header -->
      <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-blue-500/20 border border-blue-500/30 flex items-center justify-center">
            <span class="material-icons-round text-blue-400">schedule_send</span>
          </div>
          <div>
            <h2 class="text-white font-black text-xl">طلاب السنتر في المستقبل</h2>
            <p class="text-slate-500 text-xs">طلاب مسجلين أونلاين واختاروا سنتر مستقبلي</p>
          </div>
          <span class="bg-blue-500/20 text-blue-400 text-xs font-bold px-2 py-1 rounded-full border border-blue-500/30">{{ total() }}</span>
        </div>
        <div class="flex items-center gap-2">
          <button *ngIf="students().length > 0" (click)="exportToExcel()"
            class="flex items-center gap-2 px-4 py-2 bg-teal-700 hover:bg-teal-600 text-white text-xs font-bold rounded-xl transition-colors border border-teal-600/50">
            <span class="material-icons-round text-sm">download</span>
            تصدير Excel ({{ filtered().length }})
          </button>
        </div>
      </div>

      <!-- Filters -->
      <div class="flex items-center gap-1.5 flex-wrap">
        <div class="relative">
          <span class="material-icons-round absolute right-2 top-1/2 -translate-y-1/2 text-slate-500" style="font-size:13px;pointer-events:none">search</span>
          <input type="text" [(ngModel)]="searchQuery" (ngModelChange)="applyFilter()"
            class="filter-search" placeholder="اسم / كود / هاتف / سنتر...">
        </div>
        <select [(ngModel)]="filterCenter" (ngModelChange)="applyFilter()" class="filter-input" style="width:140px">
          <option value="">كل السناتر</option>
          <option *ngFor="let c of centerList()" [value]="c">{{ c }}</option>
        </select>
        <button *ngIf="searchQuery || filterCenter" (click)="clearFilters()"
          class="flex items-center justify-center rounded-lg bg-slate-800 text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-colors border border-slate-700"
          style="height:30px;width:30px;flex-shrink:0">
          <span class="material-icons-round" style="font-size:14px">filter_alt_off</span>
        </button>
        <span class="text-slate-500" style="font-size:11px">{{ filtered().length }} من {{ total() }}</span>
      </div>

      <!-- Table -->
      <div class="edu-card p-0 overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full text-xs">
            <thead class="bg-slate-800/70 border-b border-slate-700">
              <tr>
                <th class="px-3 py-3 text-right font-bold text-slate-400">الكود</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 min-w-[160px]">الطالب</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400">هاتف الطالب</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden md:table-cell">هاتف الوالد</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell">الصف</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell">المحافظة</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400">السنتر المستقبلي</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400">الحالة</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden xl:table-cell">تاريخ التسجيل</th>
                <th class="px-3 py-3 text-center font-bold text-slate-400">إجراءات</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngIf="loading()">
                <td colspan="9" class="text-center py-10 text-slate-500">
                  <span class="material-icons-round animate-spin text-2xl">refresh</span>
                </td>
              </tr>
              <tr *ngIf="!loading() && filtered().length === 0">
                <td colspan="9" class="text-center py-10 text-slate-500">لا يوجد طلاب</td>
              </tr>
              <tr *ngFor="let s of filtered()"
                class="border-b border-slate-800/40 hover:bg-slate-800/20 transition-colors">

                <td class="px-3 py-2.5">
                  <code class="text-[10px] text-amber-400 font-mono bg-amber-500/10 px-1.5 py-0.5 rounded">{{ s.studentCode || '—' }}</code>
                </td>

                <td class="px-3 py-2.5">
                  <div class="flex items-center gap-2 min-w-[150px]">
                    <div class="w-8 h-8 rounded-full flex-shrink-0 overflow-hidden border border-slate-700">
                      <img *ngIf="s.profileImageUrl" [src]="s.profileImageUrl" class="w-full h-full object-cover">
                      <div *ngIf="!s.profileImageUrl"
                        class="w-full h-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-xs font-black text-white">
                        {{ (s.fullName || '?')[0] }}
                      </div>
                    </div>
                    <div>
                      <p class="text-slate-200 font-semibold text-xs">{{ s.fullName }}</p>
                      <p class="text-[10px] text-blue-400">أونلاين</p>
                    </div>
                  </div>
                </td>

                <td class="px-3 py-2.5">
                  <span class="text-slate-300 font-mono ltr">{{ s.phone }}</span>
                </td>

                <td class="px-3 py-2.5 hidden md:table-cell">
                  <span class="text-slate-400 font-mono ltr">{{ s.parentPhone || '—' }}</span>
                </td>

                <td class="px-3 py-2.5 hidden lg:table-cell">
                  <span class="text-slate-300">{{ s.grade || '—' }}</span>
                </td>

                <td class="px-3 py-2.5 hidden lg:table-cell">
                  <span class="text-slate-300">{{ s.governorate || '—' }}</span>
                </td>

                <td class="px-3 py-2.5">
                  <span class="text-xs font-bold px-2 py-1 rounded-full bg-blue-500/20 text-blue-300 border border-blue-500/30">
                    {{ s.centerName }}
                  </span>
                </td>

                <td class="px-3 py-2.5">
                  <span class="text-[10px] font-bold px-2 py-1 rounded-full" [class]="statusClass(s.status)">
                    {{ statusLabel(s.status) }}
                  </span>
                </td>

                <td class="px-3 py-2.5 hidden xl:table-cell">
                  <div class="text-slate-400">{{ s.createdAt | date:'d/M/yy' }}</div>
                  <div class="text-slate-500 text-[10px]">{{ s.createdAt | date:'HH:mm' }}</div>
                </td>
                <td class="px-3 py-2.5">
                  <div class="flex items-center justify-center">
                    <button (click)="transferToCenter(s)"
                      class="flex items-center gap-1 px-3 py-1.5 bg-orange-500/20 text-orange-400 border border-orange-500/30 rounded-lg text-[11px] font-bold hover:bg-orange-500/30 transition-colors"
                      title="نقل الطالب من أونلاين إلى سنتر">
                      <span class="material-icons-round text-sm">swap_horiz</span>
                      نقل للسنتر
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Summary cards -->
      <div *ngIf="!loading() && centerList().length > 0" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
        <div *ngFor="let c of centerList()" class="edu-card p-3 flex items-center gap-3">
          <div class="w-9 h-9 rounded-xl bg-blue-500/20 flex items-center justify-center flex-shrink-0">
            <span class="material-icons-round text-blue-400 text-sm">corporate_fare</span>
          </div>
          <div>
            <p class="text-[10px] text-slate-500 mb-0.5">{{ c }}</p>
            <p class="text-white font-black text-lg">{{ countByCenter(c) }}</p>
            <p class="text-[10px] text-slate-500">طالب مستقبلي</p>
          </div>
        </div>
      </div>

      <!-- Transfer Modal -->
      <div *ngIf="transferStudent()" class="fixed inset-0 z-[90] flex items-center justify-center p-4">
        <div (click)="closeTransferModal()" class="absolute inset-0 bg-black/70 backdrop-blur-sm"></div>
        <div class="relative w-full max-w-sm bg-slate-900 rounded-2xl shadow-2xl border border-slate-700/50 z-10" dir="rtl">
          <div class="p-4 border-b border-slate-800 flex items-center justify-between">
            <div>
              <h3 class="text-white font-bold text-sm">نقل إلى السنتر</h3>
              <p class="text-slate-400 text-[11px] mt-0.5">{{ transferStudent()?.fullName }}</p>
            </div>
            <button (click)="closeTransferModal()" class="btn-icon bg-slate-800">
              <span class="material-icons-round">close</span>
            </button>
          </div>
          <div class="p-4 space-y-4">
            <!-- اختيار السنتر -->
            <div>
              <label class="text-[11px] text-slate-400 font-bold block mb-2">
                السنتر <span class="text-red-400">*</span>
              </label>
              <select [(ngModel)]="selectedCenterName" (ngModelChange)="onModalCenterChange()" class="filter-input w-full" style="height:36px;font-size:12px;padding:0 10px">
                <option value="">اختر السنتر</option>
                <option *ngFor="let c of modalCenters" [value]="c.name">{{ c.name }}</option>
              </select>
            </div>
            <!-- اختيار الجروب -->
            <div>
              <label class="text-[11px] text-slate-400 font-bold block mb-2">
                الجروب <span class="text-red-400">*</span>
              </label>
              <select [(ngModel)]="selectedGroupId" class="filter-input w-full" style="height:36px;font-size:12px;padding:0 10px"
                [disabled]="groupsLoading || !selectedCenterName">
                <option [ngValue]="null">
                  {{ groupsLoading ? 'جاري التحميل...' : (!selectedCenterName ? 'اختر السنتر أولاً' : (availableGroups.length === 0 ? 'لا توجد جروبات لهذا الصف' : 'اختر الجروب')) }}
                </option>
                <option *ngFor="let g of availableGroups" [ngValue]="g.id">
                  {{ g.title || g.name }} — {{ g.dayOfWeek }} {{ g.meetingTime }}
                </option>
              </select>
            </div>
          </div>
          <div class="p-4 pt-0 flex justify-end gap-3">
            <button (click)="closeTransferModal()" class="btn-secondary text-xs h-9 px-4">إلغاء</button>
            <button (click)="confirmTransfer()" [disabled]="transferring || !selectedGroupId || !selectedCenterName"
              class="flex items-center gap-1 px-4 h-9 bg-orange-600 hover:bg-orange-500 text-white text-xs font-bold rounded-xl transition-colors disabled:opacity-50">
              <span class="material-icons-round text-sm">swap_horiz</span>
              {{ transferring ? 'جاري النقل...' : 'نقل للسنتر' }}
            </button>
          </div>
        </div>
      </div>

    </div>
  `
})
export class FutureCenterStudentsComponent implements OnInit {
  students = signal<any[]>([]);
  loading = signal(false);
  total = signal(0);
  searchQuery = '';
  filterCenter = '';
  _tick = signal(0);

  // Transfer modal
  transferStudent = signal<any | null>(null);
  availableGroups: any[] = [];
  allLevelGroups: any[] = []; // كل جروبات الصف (لفلترة السناتر)
  modalCenters: any[] = [];
  groupsLoading = false;
  selectedGroupId: number | null = null;
  selectedCenterName = '';
  transferring = false;
  levelsData: any[] = [];

  filtered = computed(() => {
    this._tick();
    let list = [...this.students()];
    const q = this.searchQuery.toLowerCase();
    if (q) list = list.filter(s =>
      (s.fullName || '').toLowerCase().includes(q) ||
      (s.studentCode || '').toLowerCase().includes(q) ||
      (s.phone || '').includes(q) ||
      (s.centerName || '').toLowerCase().includes(q)
    );
    if (this.filterCenter) list = list.filter(s => s.centerName === this.filterCenter);
    return list;
  });

  centerList = computed(() => {
    const set = new Set(this.students().map((s: any) => s.centerName).filter(Boolean));
    return Array.from(set).sort() as string[];
  });

  constructor(private api: ApiService, private toastr: ToastrService) {}

  ngOnInit() {
    this.load();
    this.api.getLevels().subscribe({ next: (levels: any[]) => this.levelsData = levels || [], error: () => {} });
  }

  load() {
    this.loading.set(true);
    this.api.getFutureCenterStudents().subscribe({
      next: (res) => {
        const pg = extractPage<any>(res);
        this.students.set(pg.content || []);
        this.total.set(pg.totalElements || 0);
        this.loading.set(false);
      },
      error: () => { this.loading.set(false); this.toastr.error('فشل تحميل البيانات'); }
    });
  }

  applyFilter() { this._tick.update(v => v + 1); }
  clearFilters() { this.searchQuery = ''; this.filterCenter = ''; this._tick.update(v => v + 1); }

  transferToCenter(student: any) {
    this.selectedGroupId = null;
    this.selectedCenterName = student.centerName || '';
    this.availableGroups = [];
    this.allLevelGroups = [];
    this.modalCenters = [];
    this.transferStudent.set(student);
    const level = this.levelsData.find((l: any) => l.name === student.grade);
    const levelId = level?.id;
    if (levelId) {
      this.groupsLoading = true;
      this.api.getMyGroups(levelId).subscribe({
        next: (groups: any[]) => {
          this.allLevelGroups = groups || [];
          // استخرج السناتر الفريدة من الجروبات
          const centerMap = new Map<string, any>();
          (groups || []).forEach((g: any) => {
            const name = g.centerName || g.center?.name;
            if (name && !centerMap.has(name)) centerMap.set(name, { name });
          });
          this.modalCenters = Array.from(centerMap.values());
          // فلتر الجروبات للسنتر الحالي (مع deduplication بالعنوان)
          this.filterGroupsByCenter(this.selectedCenterName);
          this.groupsLoading = false;
        },
        error: () => { this.allLevelGroups = []; this.modalCenters = []; this.availableGroups = []; this.groupsLoading = false; }
      });
    }
  }

  onModalCenterChange() {
    this.selectedGroupId = null;
    this.filterGroupsByCenter(this.selectedCenterName);
  }

  filterGroupsByCenter(centerName: string) {
    const filtered = this.allLevelGroups.filter((g: any) =>
      g.centerName === centerName || g.center?.name === centerName
    );
    // deduplication بالعنوان (title أو name)
    const seen = new Set<string>();
    this.availableGroups = filtered.filter((g: any) => {
      const key = g.title || g.name || String(g.id);
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
  }

  closeTransferModal() {
    this.transferStudent.set(null);
    this.availableGroups = [];
    this.allLevelGroups = [];
    this.modalCenters = [];
    this.selectedGroupId = null;
    this.selectedCenterName = '';
  }

  confirmTransfer() {
    const student = this.transferStudent();
    if (!student || !this.selectedGroupId || !this.selectedCenterName) return;
    this.transferring = true;
    this.api.transferStudentToCenter(student.id, this.selectedGroupId, this.selectedCenterName).subscribe({
      next: () => {
        this.toastr.success(`تم نقل ${student.fullName} إلى ${student.centerName}`);
        this.transferring = false;
        this.closeTransferModal();
        this.load();
      },
      error: (err: any) => { this.toastr.error(err?.error?.message || 'حدث خطأ'); this.transferring = false; }
    });
  }

  countByCenter(center: string): number {
    return this.students().filter((s: any) => s.centerName === center).length;
  }

  statusClass(status?: string): string {
    switch (status) {
      case 'ACTIVE':   return 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30';
      case 'PENDING':  return 'bg-amber-500/20 text-amber-400 border border-amber-500/30';
      case 'BLOCKED':  return 'bg-red-500/20 text-red-400 border border-red-500/30';
      case 'REJECTED': return 'bg-slate-500/20 text-slate-400 border border-slate-500/30';
      default:         return 'bg-slate-700 text-slate-400';
    }
  }

  statusLabel(status?: string): string {
    switch (status) {
      case 'ACTIVE':   return 'نشط';
      case 'PENDING':  return 'قيد المراجعة';
      case 'BLOCKED':  return 'محظور';
      case 'REJECTED': return 'مرفوض';
      default:         return status || '—';
    }
  }

  exportToExcel() {
    const list = this.filtered();
    if (!list.length) return;
    const now = new Date();
    const date = `${now.getFullYear()}${String(now.getMonth()+1).padStart(2,'0')}${String(now.getDate()).padStart(2,'0')}`;
    const time = `${String(now.getHours()).padStart(2,'0')}${String(now.getMinutes()).padStart(2,'0')}${String(now.getSeconds()).padStart(2,'0')}`;
    const headers = ['الكود','الاسم','هاتف الطالب','هاتف الوالد','الصف','المحافظة','السنتر المستقبلي','الحالة','تاريخ التسجيل'];
    const rows = list.map((s: any) => [
      s.studentCode||'', s.fullName||'', s.phone||'', s.parentPhone||'',
      s.grade||'', s.governorate||'', s.centerName||'', s.status||'',
      s.createdAt ? new Date(s.createdAt).toLocaleString('ar-EG') : ''
    ]);
    const csv = '﻿' + [headers, ...rows].map(r => r.map(c => `"${String(c).replace(/"/g,'""')}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = `future_center_${date}_${time}.csv`; a.click();
    URL.revokeObjectURL(url);
  }
}
