import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-access-codes',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10" dir="rtl">

      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-white font-bold text-2xl tracking-tight">توليد أكواد الوصول</h2>
          <p class="text-slate-400 text-sm mt-1">أنشئ أكواد لفتح كورسات أو شحن المحافظ</p>
        </div>
        <a routerLink="/codes"
           class="flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-700 hover:bg-slate-600 text-white text-sm font-medium transition-colors">
          <span class="material-icons-round text-base">list</span>
          كل الأكواد
        </a>
      </div>

      <!-- Create Form Card -->
      <div class="edu-card p-6 space-y-6">
        <h3 class="text-white font-semibold text-lg border-b border-slate-700 pb-3">إنشاء أكواد جديدة</h3>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-5">

          <!-- Count -->
          <div>
            <label class="edu-label">عدد الأكواد <span class="text-red-400">*</span></label>
            <input type="number" [(ngModel)]="form.count" min="1"
                   class="edu-input" placeholder="أدخل العدد">
          </div>

          <!-- Price -->
          <div>
            <label class="edu-label">قيمة الكود (ج.م) <span class="text-red-400">*</span></label>
            <input type="number" [(ngModel)]="form.price" min="0.01" step="0.01"
                   class="edu-input" placeholder="أدخل القيمة">
          </div>

          <!-- Expiry -->
          <div>
            <label class="edu-label">تاريخ انتهاء الصلاحية</label>
            <input type="datetime-local" [(ngModel)]="form.expiresAt"
                   class="edu-input">
          </div>

        </div>

        <!-- Target Type -->
        <div>
          <label class="edu-label mb-3 block">وظيفة الكود <span class="text-red-400">*</span></label>
          <div class="grid grid-cols-3 gap-3">
            <button *ngFor="let opt of targetOptions"
                    (click)="selectTarget(opt.value)"
                    [class]="form.targetType === opt.value
                      ? 'border-2 border-indigo-500 bg-indigo-500/10 text-indigo-300 rounded-xl p-4 text-center transition-all'
                      : 'border-2 border-slate-700 bg-slate-800/60 text-slate-400 hover:border-slate-500 rounded-xl p-4 text-center transition-all'">
              <div class="text-2xl mb-1">{{ opt.icon }}</div>
              <div class="font-semibold text-sm">{{ opt.label }}</div>
              <div class="text-xs mt-1 opacity-70">{{ opt.desc }}</div>
            </button>
          </div>
        </div>

        <!-- Course Picker (only for COURSE type) -->
        <div *ngIf="form.targetType === 'COURSE'" class="animate-fade-in">
          <label class="edu-label">اختر الكورس <span class="text-red-400">*</span></label>
          <select [(ngModel)]="form.courseId" class="edu-select">
            <option [ngValue]="null">-- اختر الكورس --</option>
            <option *ngFor="let c of courses()" [ngValue]="c.id">{{ c.title }}</option>
          </select>
        </div>

        <!-- Submit -->
        <div class="flex justify-end pt-2">
          <button (click)="generate()"
                  [disabled]="generating() || !isFormValid()"
                  class="btn-primary px-8 py-3 text-base disabled:opacity-50 disabled:cursor-not-allowed">
            <span *ngIf="!generating()">
              <span class="material-icons-round text-base align-middle me-1">bolt</span>
              توليد الأكواد
            </span>
            <span *ngIf="generating()" class="flex items-center gap-2">
              <svg class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              جاري التوليد...
            </span>
          </button>
        </div>
      </div>

      <!-- Generated Codes Table -->
      <div *ngIf="generatedCodes().length > 0" class="edu-card p-0 overflow-hidden animate-fade-in">
        <div class="flex items-center justify-between px-5 py-4 border-b border-slate-700">
          <div>
            <h3 class="text-white font-semibold">الأكواد المُنشأة حديثاً</h3>
            <p class="text-slate-400 text-xs mt-0.5">
              {{ generatedCodes().length }} كود — {{ lastResult()?.targetName }}
              <span *ngIf="lastResult()?.batchLabel"> — {{ lastResult()?.batchLabel }}</span>
            </p>
          </div>
          <button (click)="exportExcel()"
                  class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-green-700 hover:bg-green-600 text-white text-xs transition-colors">
            <span class="material-icons-round text-sm">download</span>
            تصدير Excel
          </button>
        </div>

        <div class="overflow-x-auto">
          <table class="edu-table">
            <thead>
              <tr>
                <th>#</th>
                <th>الكود</th>
                <th>القيمة</th>
                <th>النوع</th>
                <th>المرتبط</th>
                <th>تاريخ الإنشاء</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let c of generatedCodes(); let i = index">
                <td class="text-slate-500">{{ i + 1 }}</td>
                <td>
                  <code class="text-indigo-400 font-mono font-bold tracking-widest text-sm">{{ c }}</code>
                </td>
                <td class="text-slate-300">
                  {{ lastResult()?.price ? (lastResult()?.price + ' ج.م') : 'مجاني' }}
                </td>
                <td>
                  <span class="px-2 py-0.5 rounded-full text-xs font-semibold"
                        [class]="targetBadgeClass(lastResult()?.targetType)">
                    {{ targetLabel(lastResult()?.targetType) }}
                  </span>
                </td>
                <td class="text-slate-300 text-xs">{{ lastResult()?.targetName || '—' }}</td>
                <td class="text-slate-500 text-xs">{{ now | date:'short' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

    </div>
  `
})
export class AccessCodesComponent implements OnInit {
  courses = signal<any[]>([]);
  categories = signal<any[]>([]);
  generating = signal(false);
  generatedCodes = signal<string[]>([]);
  lastResult = signal<any>(null);

  now = new Date();

  form: any = {
    targetType: 'WALLET',
    categoryId: null,
    courseId: null,
    count: null,
    price: null,
    expiresAt: null,
  };

  targetOptions = [
    { value: 'WALLET', icon: '🔧', label: 'عام', desc: 'يشحن المحفظة — يشتري أي كورس' },
    { value: 'COURSE', icon: '📚', label: 'كورس محدد', desc: 'مخصص لكورس بعينه' },
  ];

  constructor(private api: ApiService, private toastr: ToastrService) {}

  ngOnInit() {
    this.api.getCourses(0, 100).subscribe({
      next: (pg: any) => this.courses.set(pg.content || pg?.data?.content || []),
      error: () => this.courses.set([])
    });
    this.api.getCategories().subscribe({
      next: (list: any) => this.categories.set(Array.isArray(list) ? list : list?.content ?? []),
      error: () => this.categories.set([])
    });
  }

  selectTarget(type: string) {
    this.form.targetType = type;
    this.form.courseId   = null;
    this.form.categoryId = null;
  }

  isFormValid(): boolean {
    if (!this.form.count || this.form.count < 1) return false;
    if (!this.form.price || this.form.price <= 0) return false;
    if (this.form.targetType === 'COURSE' && !this.form.courseId) return false;
    return true;
  }

  generate() {
    if (!this.isFormValid()) return;
    this.generating.set(true);

    const payload: any = {
      targetType: this.form.targetType,
      count: this.form.count,
    };
    payload.price = this.form.price;
    if (this.form.expiresAt) payload.expiresAt = this.form.expiresAt;
    if (this.form.targetType === 'CATEGORY' && this.form.categoryId) payload.categoryId = this.form.categoryId;
    if (this.form.targetType === 'COURSE'   && this.form.courseId)   payload.courseId   = this.form.courseId;

    this.api.generateAccessCodes(payload).subscribe({
      next: (res: any) => {
        const data = res?.data ?? res;
        this.generatedCodes.set(data?.codes ?? []);
        this.lastResult.set(data);
        this.now = new Date();
        this.toastr.success(`تم توليد ${data?.codes?.length ?? 0} كود بنجاح`);
        this.generating.set(false);
        // reset form
        this.form = { targetType: 'WALLET', categoryId: null, courseId: null, count: null, price: null, expiresAt: null };
      },
      error: (err: any) => {
        this.toastr.error(err?.error?.message || 'حدث خطأ أثناء التوليد');
        this.generating.set(false);
      }
    });
  }

  exportExcel() {
    const res = this.lastResult();
    const price = res?.price ? res.price + ' ج.م' : 'مجاني';
    const type  = this.targetLabel(res?.targetType);
    const target = res?.targetName ?? '—';
    const date  = new Date().toLocaleDateString('ar-EG');

    const headers = ['#', 'الكود', 'القيمة', 'النوع', 'المرتبط', 'تاريخ الإنشاء'];
    const rows = this.generatedCodes().map((code, i) =>
      [i + 1, code, price, type, target, date]
    );

    const csvContent = [headers, ...rows]
      .map(r => r.map(cell => `"${cell}"`).join(','))
      .join('\n');

    const bom = '﻿'; // UTF-8 BOM for Excel Arabic support
    const blob = new Blob([bom + csvContent], { type: 'text/csv;charset=utf-8;' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = `access-codes-${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    this.toastr.success('تم تصدير الأكواد');
  }

  targetLabel(type: string): string {
    return { CATEGORY: 'عام', WALLET: 'محفظة', COURSE: 'كورس', SESSION: 'حصة' }[type] ?? type;
  }

  targetBadgeClass(type: string): string {
    return {
      CATEGORY: 'bg-blue-500/20 text-blue-300',
      WALLET:   'bg-yellow-500/20 text-yellow-300',
      COURSE:   'bg-green-500/20 text-green-300',
      SESSION:  'bg-purple-500/20 text-purple-300',
    }[type] ?? 'bg-slate-700 text-slate-300';
  }
}
