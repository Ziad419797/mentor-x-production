import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-codes',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10" dir="rtl">

      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-white font-bold text-2xl tracking-tight">كل الأكواد</h2>
          <p class="text-slate-400 text-sm mt-1">عرض وإدارة جميع أكواد الوصول</p>
        </div>
        <a routerLink="/create-codes"
           class="btn-primary flex items-center gap-2">
          <span class="material-icons-round text-base">add</span>
          توليد أكواد جديدة
        </a>
      </div>

      <!-- Stats -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="edu-card p-4 text-center">
          <div class="text-3xl font-black text-indigo-400">{{ stats().total }}</div>
          <div class="text-slate-400 text-sm mt-1">إجمالي الأكواد</div>
        </div>
        <div class="edu-card p-4 text-center">
          <div class="text-3xl font-black text-green-400">{{ stats().used }}</div>
          <div class="text-slate-400 text-sm mt-1">الأكواد المستخدمة</div>
        </div>
        <div class="edu-card p-4 text-center">
          <div class="text-3xl font-black text-yellow-400">{{ stats().totalAmount | number:'1.0-0' }}</div>
          <div class="text-slate-400 text-sm mt-1">إجمالي المبلغ (ج.م)</div>
        </div>
        <div class="edu-card p-4 text-center">
          <div class="text-3xl font-black text-blue-400">{{ stats().courseLinked }}</div>
          <div class="text-slate-400 text-sm mt-1">مرتبطة بكورسات</div>
        </div>
      </div>

      <!-- Filters -->
      <div class="edu-card p-4 flex flex-wrap items-end gap-4">
        <div class="flex-1 min-w-[180px]">
          <label class="edu-label text-xs">بحث بالكود أو الكورس</label>
          <div class="relative">
            <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-sm">search</span>
            <input type="text" [(ngModel)]="search" (ngModelChange)="onFilterChange()"
                   class="edu-input pr-9" placeholder="ابحث...">
          </div>
        </div>
        <div class="min-w-[140px]">
          <label class="edu-label text-xs">النوع</label>
          <select [(ngModel)]="filterType" (ngModelChange)="onFilterChange()" class="edu-select">
            <option value="">الكل</option>
            <option value="CATEGORY">عام</option>
            <option value="WALLET">محفظة</option>
            <option value="COURSE">كورس</option>
            <option value="SESSION">حصة</option>
          </select>
        </div>
        <div class="min-w-[140px]">
          <label class="edu-label text-xs">الحالة</label>
          <select [(ngModel)]="filterStatus" (ngModelChange)="onFilterChange()" class="edu-select">
            <option value="">الكل</option>
            <option value="active">نشط</option>
            <option value="inactive">معطل</option>
            <option value="used">مستخدم بالكامل</option>
          </select>
        </div>
        <div class="min-w-[150px]">
          <label class="edu-label text-xs">من تاريخ</label>
          <input type="date" [(ngModel)]="filterFrom" (ngModelChange)="onFilterChange()" class="edu-input">
        </div>
        <div class="min-w-[150px]">
          <label class="edu-label text-xs">إلى تاريخ</label>
          <input type="date" [(ngModel)]="filterTo" (ngModelChange)="onFilterChange()" class="edu-input">
        </div>
        <div class="min-w-[160px]">
          <label class="edu-label text-xs">المنشئ</label>
          <select [(ngModel)]="filterCreator" (ngModelChange)="onFilterChange()" class="edu-select">
            <option value="">الكل</option>
            <option *ngFor="let name of creatorNames()" [value]="name">{{ name }}</option>
          </select>
        </div>
        <button (click)="resetFilters()" class="btn-secondary h-11 px-4 self-end">
          <span class="material-icons-round text-sm">refresh</span>
        </button>
      </div>

      <!-- Table -->
      <div class="edu-card p-0 overflow-hidden shadow-xl">
        <div class="overflow-x-auto">
          <table class="edu-table">
            <thead>
              <tr>
                <th>#</th>
                <th>الكود</th>
                <th>القيمة</th>
                <th>النوع</th>
                <th>المرتبط</th>
                <th>أُنشئ بواسطة</th>
                <th>الاستخدامات</th>
                <th>الحالة</th>
                <th>تاريخ الإنشاء</th>
                <th>الإجراءات</th>
              </tr>
            </thead>
            <tbody>
              <ng-container *ngIf="!loading(); else loadingTpl">
                <tr *ngFor="let c of filteredCodes(); let i = index">
                  <td class="text-slate-500">{{ i + 1 }}</td>
                  <td>
                    <code class="text-indigo-400 font-mono font-bold tracking-widest">{{ c.code }}</code>
                  </td>
                  <td class="text-slate-300 font-semibold">
                    {{ c.price ? (c.price + ' ج.م') : 'مجاني' }}
                  </td>
                  <td>
                    <span class="px-2 py-0.5 rounded-full text-xs font-semibold"
                          [class]="targetBadgeClass(c.targetType)">
                      {{ targetLabel(c.targetType) }}
                    </span>
                  </td>
                  <td class="text-slate-300 text-xs max-w-[140px] truncate">
                    {{ c.courseName || c.categoryName || c.sessionTitle || '—' }}
                  </td>
                  <td>
                    <span class="text-slate-300 text-xs font-semibold">{{ c.createdByName || '—' }}</span>
                  </td>
                  <td class="text-center">
                    <span class="text-white font-bold">{{ c.usedCount }}</span>
                    <span class="text-slate-500">/{{ c.maxUses ?? '∞' }}</span>
                    <span *ngIf="c.remainingUses !== null && c.remainingUses !== undefined"
                          class="text-slate-500 text-xs ms-1">({{ c.remainingUses }} متبقي)</span>
                  </td>
                  <td>
                    <span *ngIf="!c.active" class="badge-gray">معطل</span>
                    <span *ngIf="c.active && isExhausted(c)" class="badge-gray">مستخدم</span>
                    <span *ngIf="c.active && !isExhausted(c)" class="badge-success">نشط</span>
                  </td>
                  <td class="text-slate-500 text-xs">{{ c.createdAt | date:'shortDate' }}</td>
                  <td>
                    <div class="flex items-center gap-1">
                      <!-- Usages -->
                      <button (click)="openUsages(c)"
                              title="عرض الاستخدامات"
                              class="p-1.5 rounded-lg bg-slate-700 hover:bg-indigo-600 text-slate-300 hover:text-white transition-colors">
                        <span class="material-icons-round text-sm">visibility</span>
                      </button>
                      <!-- Deactivate -->
                      <button *ngIf="c.active"
                              (click)="deactivate(c)"
                              title="تعطيل"
                              class="p-1.5 rounded-lg bg-slate-700 hover:bg-red-600 text-slate-300 hover:text-white transition-colors">
                        <span class="material-icons-round text-sm">block</span>
                      </button>
                      <!-- Re-enable -->
                      <button *ngIf="!c.active"
                              (click)="reactivate(c)"
                              title="إعادة تفعيل"
                              class="p-1.5 rounded-lg bg-slate-700 hover:bg-green-600 text-slate-300 hover:text-white transition-colors">
                        <span class="material-icons-round text-sm">check_circle</span>
                      </button>
                    </div>
                  </td>
                </tr>
                <tr *ngIf="filteredCodes().length === 0">
                  <td colspan="10" class="text-center py-16 text-slate-600 italic">لا توجد أكواد تطابق الفلتر</td>
                </tr>
              </ng-container>
              <ng-template #loadingTpl>
                <tr>
                  <td colspan="10" class="text-center py-16 text-slate-500">جاري التحميل...</td>
                </tr>
              </ng-template>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div *ngIf="totalPages() > 1" class="flex justify-center gap-2 p-4 border-t border-slate-700/50">
          <button *ngFor="let p of pageArray(); let i = index"
                  (click)="loadPage(i)"
                  [class.bg-indigo-600]="currentPage() === i"
                  class="w-9 h-9 rounded-lg bg-slate-800 border border-slate-700 text-white text-sm font-bold hover:bg-indigo-600 transition-colors">
            {{ i + 1 }}
          </button>
        </div>
      </div>

      <!-- Usages Modal -->
      <div *ngIf="usagesModal()" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
        <div class="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-lg max-h-[80vh] flex flex-col shadow-2xl">
          <div class="flex items-center justify-between p-5 border-b border-slate-700">
            <div>
              <h3 class="text-white font-bold">استخدامات الكود</h3>
              <code class="text-indigo-400 font-mono text-sm">{{ selectedCode()?.code }}</code>
            </div>
            <button (click)="usagesModal.set(false)" class="p-2 rounded-lg hover:bg-slate-700 text-slate-400 hover:text-white transition-colors">
              <span class="material-icons-round">close</span>
            </button>
          </div>
          <div class="overflow-y-auto flex-1 p-2">
            <div *ngIf="loadingUsages()" class="text-center py-10 text-slate-500">جاري التحميل...</div>
            <div *ngIf="!loadingUsages() && usages().length === 0" class="text-center py-10 text-slate-600 italic">
              لم يُستخدم هذا الكود بعد
            </div>
            <table *ngIf="!loadingUsages() && usages().length > 0" class="w-full text-sm">
              <thead>
                <tr class="text-slate-400 border-b border-slate-700">
                  <th class="text-right py-2 px-3 font-medium">الطالب</th>
                  <th class="text-right py-2 px-3 font-medium">النوع</th>
                  <th class="text-right py-2 px-3 font-medium">التاريخ</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let u of usages()" class="border-b border-slate-800 hover:bg-slate-800/50">
                  <td class="py-2.5 px-3">
                    <span class="text-white block">{{ u.studentName }}</span>
                    <span class="text-slate-500 text-xs font-mono">{{ u.studentCode }}</span>
                  </td>
                  <td class="py-2.5 px-3">
                    <span *ngIf="u.enrollmentsCreated === 0" class="px-2 py-0.5 rounded-full text-xs font-semibold bg-yellow-500/20 text-yellow-300">شحن محفظة</span>
                    <span *ngIf="u.enrollmentsCreated > 0" class="px-2 py-0.5 rounded-full text-xs font-semibold bg-green-500/20 text-green-300">فتح {{ u.enrollmentsCreated }} كورس</span>
                  </td>
                  <td class="py-2.5 px-3 text-slate-500 text-xs">{{ u.usedAt | date:'short' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

    </div>
  `
})
export class CodesComponent implements OnInit {
  allCodes = signal<any[]>([]);
  loading = signal(true);
  totalPages = signal(0);
  currentPage = signal(0);

  // Filters
  search = '';
  filterType = '';
  filterStatus = '';
  filterFrom = '';
  filterTo = '';
  filterCreator = '';

  // Usages modal
  usagesModal = signal(false);
  selectedCode = signal<any>(null);
  usages = signal<any[]>([]);
  loadingUsages = signal(false);

  filteredCodes = computed(() => {
    let list = this.allCodes();
    const q = this.search.toLowerCase();
    if (q) list = list.filter(c =>
      c.code?.toLowerCase().includes(q) ||
      c.courseName?.toLowerCase().includes(q) ||
      c.categoryName?.toLowerCase().includes(q) ||
      c.sessionTitle?.toLowerCase().includes(q) ||
      c.createdByName?.toLowerCase().includes(q)
    );
    if (this.filterType)    list = list.filter(c => c.targetType === this.filterType);
    if (this.filterCreator) list = list.filter(c => c.createdByName === this.filterCreator);
    if (this.filterStatus === 'active')   list = list.filter(c => c.active && !this.isExhausted(c));
    if (this.filterStatus === 'inactive') list = list.filter(c => !c.active);
    if (this.filterStatus === 'used')     list = list.filter(c => this.isExhausted(c));
    if (this.filterFrom) list = list.filter(c => c.createdAt && new Date(c.createdAt) >= new Date(this.filterFrom));
    if (this.filterTo)   list = list.filter(c => c.createdAt && new Date(c.createdAt) <= new Date(this.filterTo + 'T23:59:59'));
    return list;
  });

  creatorNames = computed(() => {
    const names = new Set<string>();
    this.allCodes().forEach(c => { if (c.createdByName) names.add(c.createdByName); });
    return Array.from(names).sort();
  });

  stats = computed(() => {
    const all = this.allCodes();
    return {
      total: all.length,
      used: all.filter(c => this.isExhausted(c)).length,
      totalAmount: all.reduce((sum, c) => sum + (c.price ? Number(c.price) : 0), 0),
      courseLinked: all.filter(c => c.targetType === 'COURSE' || c.targetType === 'SESSION').length,
    };
  });

  pageArray = computed(() => Array.from({ length: this.totalPages() }));

  constructor(private api: ApiService, private toastr: ToastrService) {}

  ngOnInit() { this.loadPage(0); }

  loadPage(page = 0) {
    this.loading.set(true);
    this.api.getMyCodes(page, 200).subscribe({
      next: (res: any) => {
        // Backend returns Page<AccessCodeDto> directly (not wrapped)
        const page = res?.data ?? res;
        this.allCodes.set(page?.content ?? []);
        this.totalPages.set(page?.totalPages ?? 1);
        this.currentPage.set(page?.number ?? 0);
        this.loading.set(false);
      },
      error: () => { this.allCodes.set([]); this.loading.set(false); }
    });
  }

  onFilterChange() { /* computed re-runs automatically */ }

  resetFilters() {
    this.search = '';
    this.filterType = '';
    this.filterStatus = '';
    this.filterFrom = '';
    this.filterTo = '';
    this.filterCreator = '';
  }

  isExhausted(c: any): boolean {
    return c.maxUses !== null && c.maxUses !== undefined && c.usedCount >= c.maxUses;
  }

  deactivate(c: any) {
    if (!confirm(`تعطيل الكود ${c.code}?`)) return;
    this.api.deactivateCode(c.id).subscribe({
      next: () => {
        c.active = false;
        this.allCodes.update(list => [...list]);
        this.toastr.success('تم تعطيل الكود');
      },
      error: (e: any) => this.toastr.error(e?.error?.message || 'فشل التعطيل')
    });
  }

  reactivate(c: any) {
    // optimistic — backend may not have reactivate endpoint; show warning
    this.toastr.info('لا يوجد endpoint لإعادة التفعيل حالياً — يمكن إضافته لاحقاً');
  }

  openUsages(c: any) {
    this.selectedCode.set(c);
    this.usagesModal.set(true);
    this.usages.set([]);
    this.loadingUsages.set(true);
    this.api.getCodeUsages(c.id).subscribe({
      next: (list: any[]) => { this.usages.set(list); this.loadingUsages.set(false); },
      error: () => { this.usages.set([]); this.loadingUsages.set(false); }
    });
  }

  targetLabel(type: string): string {
    return { CATEGORY: 'عام', WALLET: 'محفظة', COURSE: 'كورس', SESSION: 'حصة' }[type] ?? type;
  }

  targetBadgeClass(type: string): string {
    return ({
      CATEGORY: 'bg-blue-500/20 text-blue-300',
      WALLET:   'bg-yellow-500/20 text-yellow-300',
      COURSE:   'bg-green-500/20 text-green-300',
      SESSION:  'bg-purple-500/20 text-purple-300',
    } as any)[type] ?? 'bg-slate-700 text-slate-300';
  }
}
