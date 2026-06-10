import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';

const ACTION_ICON: Record<string, string> = {
  'شحن محفظة': 'account_balance_wallet', 'قبول طالب': 'how_to_reg',
  'رفض طالب': 'person_remove', 'حظر طالب': 'block',
  'تغيير حالة طالب': 'manage_accounts', 'إضافة كورس': 'library_add',
  'اشتراك في كورس': 'card_membership', 'إنشاء أكواد وصول': 'key',
  'تسجيل دخول مدرس': 'login', 'تسجيل دخول موظف': 'badge',
  'حذف اشتراك': 'person_remove', 'تعديل طالب': 'edit',
};
const ACTION_COLOR: Record<string, string> = {
  'شحن محفظة': 'text-emerald-400 bg-emerald-500/10', 'قبول طالب': 'text-blue-400 bg-blue-500/10',
  'رفض طالب': 'text-red-400 bg-red-500/10', 'حظر طالب': 'text-red-500 bg-red-500/15',
  'تغيير حالة طالب': 'text-amber-400 bg-amber-500/10', 'إضافة كورس': 'text-indigo-400 bg-indigo-500/10',
  'اشتراك في كورس': 'text-purple-400 bg-purple-500/10', 'إنشاء أكواد وصول': 'text-teal-400 bg-teal-500/10',
  'تسجيل دخول مدرس': 'text-sky-400 bg-sky-500/10', 'تسجيل دخول موظف': 'text-cyan-400 bg-cyan-500/10',
  'حذف اشتراك': 'text-rose-400 bg-rose-500/10', 'تعديل طالب': 'text-amber-400 bg-amber-500/10',
};
const ACTIONS = [
  'شحن محفظة','قبول طالب','رفض طالب','حظر طالب','تغيير حالة طالب',
  'إضافة كورس','اشتراك في كورس','إنشاء أكواد وصول',
  'تسجيل دخول مدرس','تسجيل دخول موظف','حذف اشتراك','تعديل طالب'
];

@Component({
  selector: 'app-activity-logs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="space-y-5 animate-fade-in pb-10" dir="rtl">

      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-white text-2xl font-black">سجل النشاطات</h1>
          <p class="text-slate-500 text-sm mt-0.5">{{ totalElements | number }} إجراء مسجّل على النظام</p>
        </div>
        <button (click)="load(0)" class="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-sm border border-slate-700 transition-colors">
          <span class="material-icons-round text-base">refresh</span> تحديث
        </button>
      </div>

      <!-- Filters -->
      <div class="edu-card p-4 space-y-3">
        <p class="text-slate-400 text-xs font-bold uppercase tracking-wider">فلترة النتائج</p>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
          <!-- Actor search -->
          <div class="space-y-1">
            <label class="text-[11px] text-slate-500">المنفذ (الاسم أو رقم الهاتف)</label>
            <div class="relative">
              <span class="material-icons-round absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-500" style="font-size:15px">person_search</span>
              <input type="text" [(ngModel)]="filterActor" (ngModelChange)="onFilterChange()"
                     class="edu-input pr-9 w-full text-sm" placeholder="ابحث بالاسم أو الرقم...">
            </div>
          </div>
          <!-- Action dropdown -->
          <div class="space-y-1">
            <label class="text-[11px] text-slate-500">نوع الإجراء</label>
            <select [(ngModel)]="filterAction" (ngModelChange)="onFilterChange()" class="edu-select w-full text-sm">
              <option value="">كل الإجراءات</option>
              <option *ngFor="let a of actions" [value]="a">{{ a }}</option>
            </select>
          </div>
          <!-- Date from -->
          <div class="space-y-1">
            <label class="text-[11px] text-slate-500">من تاريخ</label>
            <input type="date" [(ngModel)]="filterFrom" (ngModelChange)="onFilterChange()"
                   class="edu-input w-full text-sm">
          </div>
          <!-- Date to -->
          <div class="space-y-1">
            <label class="text-[11px] text-slate-500">إلى تاريخ</label>
            <input type="date" [(ngModel)]="filterTo" (ngModelChange)="onFilterChange()"
                   class="edu-input w-full text-sm">
          </div>
        </div>
        <div class="flex items-center gap-3">
          <button *ngIf="filterActor || filterAction || filterFrom || filterTo" (click)="clearFilter()"
            class="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-red-500/10 text-red-400 text-xs border border-red-500/20 hover:bg-red-500/20 transition-colors">
            <span class="material-icons-round text-sm">filter_alt_off</span> مسح الفلاتر
          </button>
          <span *ngIf="filterFrom || filterTo" class="text-xs text-slate-500">
            {{ filterFrom || '...' }} ← {{ filterTo || '...' }}
          </span>
        </div>
      </div>

      <!-- Table -->
      <div class="edu-card p-0 overflow-hidden">
        <table class="edu-table">
          <thead>
            <tr>
              <th class="w-10">#</th>
              <th>المنفذ</th>
              <th>الإجراء</th>
              <th>التفاصيل</th>
              <th>التوقيت</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngIf="loading()" class="hover:bg-transparent">
              <td colspan="5" class="text-center py-16">
                <span class="animate-spin inline-block h-6 w-6 border-2 border-slate-700 border-t-orange-500 rounded-full"></span>
              </td>
            </tr>

            <tr *ngFor="let log of logs(); let i = index">
              <td class="text-slate-600 text-xs">{{ (currentPage * pageSize) + i + 1 }}</td>

              <!-- Actor -->
              <td>
                <div class="flex items-center gap-2">
                  <div class="w-9 h-9 rounded-full flex-shrink-0 flex items-center justify-center text-sm font-black text-white"
                    [ngClass]="log.actorRole === 'TEACHER' ? 'bg-gradient-to-br from-indigo-500 to-purple-600' : log.actorRole === 'STAFF' ? 'bg-gradient-to-br from-teal-500 to-cyan-600' : 'bg-slate-700'">
                    {{ initial(log.actorName || log.actorUsername) }}
                  </div>
                  <div class="flex flex-col min-w-0">
                    <span class="text-slate-200 text-xs font-semibold truncate">{{ log.actorName || log.actorUsername || '—' }}</span>
                    <span class="text-[10px] text-slate-500 font-mono" dir="ltr">{{ log.actorUsername }}</span>
                    <span class="text-[9px] px-1 py-0.5 rounded mt-0.5 w-fit"
                      [ngClass]="log.actorRole === 'TEACHER' ? 'bg-indigo-500/15 text-indigo-400' : log.actorRole === 'STAFF' ? 'bg-teal-500/15 text-teal-400' : 'bg-slate-700 text-slate-400'">
                      {{ log.actorRole === 'TEACHER' ? 'مدرس' : log.actorRole === 'STAFF' ? 'موظف' : log.actorRole || '—' }}
                    </span>
                  </div>
                </div>
              </td>

              <!-- Action -->
              <td>
                <div class="flex items-center gap-2">
                  <div class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
                       [ngClass]="actionColor(log.action)">
                    <span class="material-icons-round text-sm">{{ actionIcon(log.action) }}</span>
                  </div>
                  <span class="text-white font-bold text-sm whitespace-nowrap">{{ log.action }}</span>
                </div>
              </td>

              <!-- Details -->
              <td class="text-xs text-slate-400 max-w-[280px]">
                <span *ngIf="log.details" class="leading-relaxed">{{ log.details }}</span>
                <span *ngIf="!log.details" class="text-slate-600 italic text-[11px]">—</span>
              </td>

              <!-- Time -->
              <td class="text-[11px] text-slate-500 whitespace-nowrap">
                <div>{{ log.createdAt | date:'dd/MM/yyyy' }}</div>
                <div class="text-slate-600">{{ log.createdAt | date:'HH:mm:ss' }}</div>
              </td>
            </tr>

            <tr *ngIf="!loading() && logs().length === 0" class="hover:bg-transparent">
              <td colspan="5" class="text-center py-20 text-slate-600 italic">لا توجد نشاطات مطابقة للبحث</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="flex items-center justify-between">
        <span class="text-[11px] text-slate-600">{{ totalElements | number }} إجراء — صفحة {{ currentPage + 1 }} من {{ totalPages }}</span>
        <div class="flex gap-2">
          <button class="btn-icon h-8 w-8" [disabled]="currentPage === 0" (click)="load(currentPage - 1)">
            <span class="material-icons-round">chevron_right</span>
          </button>
          <button class="btn-icon h-8 w-8" [disabled]="currentPage >= totalPages - 1" (click)="load(currentPage + 1)">
            <span class="material-icons-round">chevron_left</span>
          </button>
        </div>
      </div>
    </div>
  `
})
export class ActivityLogsComponent implements OnInit {
  logs    = signal<any[]>([]);
  loading = signal(true);
  currentPage = 0; totalPages = 1; totalElements = 0; pageSize = 30;
  filterActor = ''; filterAction = ''; filterFrom = ''; filterTo = '';
  actions = ACTIONS;

  constructor(private api: ApiService) {}
  ngOnInit() { this.load(0); }

  load(page: number) {
    this.loading.set(true);
    this.api.getActivityLogs(
      page, this.pageSize,
      this.filterActor  || undefined,
      this.filterAction || undefined,
      this.filterFrom   || undefined,
      this.filterTo     || undefined
    ).subscribe({
      next: (pg: any) => {
        this.logs.set(pg?.content || []);
        this.currentPage   = pg?.number       ?? page;
        this.totalPages    = pg?.totalPages    ?? 1;
        this.totalElements = pg?.totalElements ?? 0;
        this.loading.set(false);
      },
      error: () => { this.logs.set([]); this.loading.set(false); }
    });
  }

  onFilterChange() {
    clearTimeout((this as any)._t);
    (this as any)._t = setTimeout(() => this.load(0), 400);
  }

  clearFilter() {
    this.filterActor = ''; this.filterAction = '';
    this.filterFrom  = ''; this.filterTo     = '';
    this.load(0);
  }

  initial(name: string): string { return name ? name.trim()[0].toUpperCase() : '?'; }
  actionIcon(a: string)  { return ACTION_ICON[a]  ?? 'event_note'; }
  actionColor(a: string) { return ACTION_COLOR[a] ?? 'text-slate-400 bg-slate-800'; }
}
