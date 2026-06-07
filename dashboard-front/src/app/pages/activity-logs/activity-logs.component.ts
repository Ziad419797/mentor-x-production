import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';

const ACTION_ICON: Record<string, string> = {
  'شحن محفظة':       'account_balance_wallet',
  'قبول طالب':       'how_to_reg',
  'رفض طالب':        'person_remove',
  'حظر طالب':        'block',
  'تغيير حالة طالب': 'manage_accounts',
  'إضافة كورس':      'library_add',
  'اشتراك في كورس':  'card_membership',
  'إنشاء أكواد وصول':'key',
};

const ACTION_COLOR: Record<string, string> = {
  'شحن محفظة':       'text-emerald-400 bg-emerald-500/10',
  'قبول طالب':       'text-blue-400 bg-blue-500/10',
  'رفض طالب':        'text-red-400 bg-red-500/10',
  'حظر طالب':        'text-red-400 bg-red-500/10',
  'تغيير حالة طالب': 'text-amber-400 bg-amber-500/10',
  'إضافة كورس':      'text-indigo-400 bg-indigo-500/10',
  'اشتراك في كورس':  'text-purple-400 bg-purple-500/10',
  'إنشاء أكواد وصول':'text-teal-400 bg-teal-500/10',
};

@Component({
  selector: 'app-activity-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">

      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-white text-2xl font-black">سجل النشاطات</h1>
          <p class="text-slate-500 text-sm mt-1">كل الإجراءات التي نفّذها فريق العمل على النظام</p>
        </div>
        <button (click)="load(0)" class="btn-icon h-10 w-10">
          <span class="material-icons-round">refresh</span>
        </button>
      </div>

      <!-- Filters -->
      <div class="flex gap-3 flex-wrap">
        <div class="relative">
          <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-sm">search</span>
          <input type="text" [(ngModel)]="filterActor" (ngModelChange)="onFilterChange()"
                 class="edu-input pr-9 w-56 text-sm" placeholder="فلتر بالمنفذ...">
        </div>
        <button *ngIf="filterActor" (click)="clearFilter()" class="btn-secondary text-xs h-auto py-2 px-3">
          <span class="material-icons-round text-sm">close</span> مسح الفلتر
        </button>
      </div>

      <!-- Table -->
      <div class="edu-card p-0 overflow-hidden">
        <table class="edu-table">
          <thead>
            <tr>
              <th class="w-12">#</th>
              <th>الإجراء</th>
              <th>المنفذ</th>
              <th>التفاصيل</th>
              <th>الوقت</th>
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
              <td>
                <div class="flex items-center gap-2">
                  <div class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
                       [ngClass]="actionColor(log.action)">
                    <span class="material-icons-round text-sm">{{ actionIcon(log.action) }}</span>
                  </div>
                  <span class="text-white font-bold text-sm">{{ log.action }}</span>
                </div>
              </td>
              <td>
                <div class="flex flex-col">
                  <span class="text-slate-200 text-xs font-semibold">{{ log.actorName || log.actorUsername }}</span>
                  <span *ngIf="log.actorName !== log.actorUsername"
                        class="text-[10px] text-slate-500">{{ log.actorUsername }}</span>
                </div>
              </td>
              <td class="text-xs text-slate-400 max-w-[220px]">
                <span *ngIf="log.entityType" class="inline-block bg-slate-800 text-slate-400 px-1.5 py-0.5 rounded text-[10px] font-mono ml-1">
                  {{ log.entityType }}{{ log.entityId ? ' #' + log.entityId : '' }}
                </span>
                {{ log.details }}
              </td>
              <td class="text-[11px] text-slate-500 whitespace-nowrap">
                {{ log.createdAt | date:'dd/MM/yyyy HH:mm' }}
              </td>
            </tr>

            <tr *ngIf="!loading() && logs().length === 0" class="hover:bg-transparent">
              <td colspan="5" class="text-center py-20 text-slate-600 italic">لا توجد نشاطات مسجّلة بعد</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="flex items-center justify-between">
        <span class="text-[11px] text-slate-600">
          {{ totalElements | number }} إجراء إجمالي — صفحة {{ currentPage + 1 }} من {{ totalPages }}
        </span>
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

  currentPage   = 0;
  totalPages    = 1;
  totalElements = 0;
  pageSize      = 30;
  filterActor   = '';

  constructor(private api: ApiService) {}

  ngOnInit() { this.load(0); }

  load(page: number) {
    this.loading.set(true);
    this.api.getActivityLogs(page, this.pageSize, this.filterActor || undefined).subscribe({
      next: pg => {
        this.logs.set(pg.content || []);
        this.currentPage   = pg.number ?? page;
        this.totalPages    = pg.totalPages ?? 1;
        this.totalElements = pg.totalElements ?? 0;
        this.loading.set(false);
      },
      error: () => { this.logs.set([]); this.loading.set(false); }
    });
  }

  onFilterChange() {
    clearTimeout((this as any)._debounce);
    (this as any)._debounce = setTimeout(() => this.load(0), 400);
  }

  clearFilter() { this.filterActor = ''; this.load(0); }

  actionIcon(action: string): string {
    return ACTION_ICON[action] ?? 'event_note';
  }

  actionColor(action: string): string {
    return ACTION_COLOR[action] ?? 'text-slate-400 bg-slate-800';
  }
}
