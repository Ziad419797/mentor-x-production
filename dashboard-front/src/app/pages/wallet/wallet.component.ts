import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { WalletTransaction, Student } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { extractItem } from '../../core/api-response.model';

@Component({
  selector: 'app-wallet',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-8 animate-fade-in pb-10">

      <!-- Summary Stats -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div class="edu-card bg-indigo-600 border-none relative overflow-hidden">
          <div class="absolute -right-4 -top-4 w-24 h-24 bg-white/10 rounded-full blur-2xl"></div>
          <p class="text-indigo-100 text-[10px] font-bold uppercase mb-1">إجمالي المشحون هذا الشهر</p>
          <h3 class="text-3xl font-black text-white">
            {{ stats()?.totalChargedThisMonth | number:'1.0-0' }}
            <span class="text-sm font-medium opacity-70">ج.م</span>
          </h3>
          <div class="mt-4 flex items-center gap-2 text-[10px] text-white/70">
            <span class="material-icons-round text-sm">trending_up</span>
            <span>خلال الشهر الحالي</span>
          </div>
        </div>
        <div class="edu-card bg-slate-900 border-slate-800">
          <p class="text-slate-500 text-[10px] font-bold uppercase mb-1">عدد عمليات الشحن</p>
          <h3 class="text-3xl font-black text-white">{{ stats()?.topUpCountThisMonth | number }}</h3>
          <div class="mt-4 flex items-center gap-2 text-[10px] text-slate-500">
            <span class="material-icons-round text-sm">history</span>
            <span>هذا الشهر</span>
          </div>
        </div>
        <div class="edu-card bg-slate-900 border-slate-800">
          <p class="text-slate-500 text-[10px] font-bold uppercase mb-1">طلاب بأرصدة نشطة</p>
          <h3 class="text-3xl font-black text-white">{{ stats()?.studentsWithBalance | number }}</h3>
          <div class="mt-4 flex items-center gap-2 text-[10px] text-slate-500">
            <span class="material-icons-round text-sm">people_alt</span>
            <span>لديهم رصيد حالياً</span>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">

        <!-- Left Panel: Top-Up Form -->
        <div class="lg:col-span-5 space-y-6">
          <div class="edu-card p-8 border-indigo-500/30 bg-indigo-500/5">
            <h3 class="text-white font-bold text-xl mb-6 flex items-center gap-2">
              <span class="material-icons-round text-indigo-400">add_card</span>
              شحن رصيد لطالب
            </h3>

            <div class="space-y-5">
              <!-- Search Student -->
              <div>
                <label class="edu-label">رقم هاتف الطالب</label>
                <div class="flex gap-2">
                  <div class="relative flex-1">
                    <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500">phone</span>
                    <input type="tel" [(ngModel)]="searchPhone" class="edu-input pr-10 text-left ltr"
                           placeholder="01xxxxxxxxx" (keyup.enter)="searchStudent()">
                  </div>
                  <button (click)="searchStudent()" [disabled]="loadingSearch()" class="btn-primary py-2 h-auto text-xs px-4">
                    <span class="material-icons-round text-sm" *ngIf="!loadingSearch()">search</span>
                    <span class="animate-spin h-4 w-4 border-2 border-white/30 border-t-white rounded-full block" *ngIf="loadingSearch()"></span>
                  </button>
                </div>
              </div>

              <!-- Student Info Preview -->
              <div *ngIf="foundStudent()"
                   class="p-4 bg-slate-900/60 rounded-2xl border border-slate-700 animate-slide-up flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-full bg-indigo-500/10 flex items-center justify-center text-indigo-400 font-bold text-sm">
                    {{ foundStudent()?.fullName?.[0] }}
                  </div>
                  <div>
                    <h4 class="text-white font-bold text-sm">{{ foundStudent()?.fullName }}</h4>
                    <span class="text-[10px] text-slate-500 ltr block text-right">{{ foundStudent()?.phone }}</span>
                  </div>
                </div>
                <div class="text-right">
                  <p class="text-[9px] text-slate-500 uppercase">الرصيد الحالي</p>
                  <p class="text-emerald-400 font-black text-sm">{{ foundStudent()?.walletBalance | number:'1.0-2' }} ج.م</p>
                </div>
              </div>

              <!-- Amount -->
              <div>
                <label class="edu-label">المبلغ (ج.م)</label>
                <input type="number" [(ngModel)]="topUpForm.amount" class="edu-input" placeholder="0.00" min="1">
              </div>

              <!-- Validity — optional, choose days OR date -->
              <div>
                <label class="edu-label flex items-center gap-2">
                  صلاحية الرصيد
                  <span class="text-slate-600 text-[10px] font-normal">(اختياري — اتركه فارغاً للرصيد الدائم)</span>
                </label>

                <!-- Toggle -->
                <div class="flex gap-2 mb-3">
                  <button type="button"
                          (click)="validityMode = 'days'"
                          class="flex-1 py-1.5 rounded-lg text-xs font-bold transition-all"
                          [class.bg-indigo-600]="validityMode === 'days'"
                          [class.text-white]="validityMode === 'days'"
                          [class.bg-slate-800]="validityMode !== 'days'"
                          [class.text-slate-400]="validityMode !== 'days'">
                    عدد الأيام
                  </button>
                  <button type="button"
                          (click)="validityMode = 'datetime'"
                          class="flex-1 py-1.5 rounded-lg text-xs font-bold transition-all"
                          [class.bg-indigo-600]="validityMode === 'datetime'"
                          [class.text-white]="validityMode === 'datetime'"
                          [class.bg-slate-800]="validityMode !== 'datetime'"
                          [class.text-slate-400]="validityMode !== 'datetime'">
                    تاريخ وساعة
                  </button>
                  <button type="button"
                          (click)="validityMode = 'none'"
                          class="flex-1 py-1.5 rounded-lg text-xs font-bold transition-all"
                          [class.bg-slate-700]="validityMode === 'none'"
                          [class.text-white]="validityMode === 'none'"
                          [class.bg-slate-800]="validityMode !== 'none'"
                          [class.text-slate-400]="validityMode !== 'none'">
                    دائم
                  </button>
                </div>

                <input *ngIf="validityMode === 'days'"
                       type="number" [(ngModel)]="topUpForm.validDays"
                       class="edu-input" placeholder="مثلاً: 30 يوم" min="1">

                <input *ngIf="validityMode === 'datetime'"
                       type="datetime-local" [(ngModel)]="topUpForm.expiresAt"
                       class="edu-input ltr text-left">

                <p *ngIf="validityMode === 'none'" class="text-xs text-slate-500 mt-1">
                  الرصيد لن ينتهي أبداً ✓
                </p>
              </div>

              <button (click)="performTopUp()"
                      [disabled]="!foundStudent() || !topUpForm.amount || loadingAction()"
                      class="btn-primary w-full justify-center h-12 text-lg shadow-xl shadow-indigo-600/20 active:scale-95">
                <span *ngIf="!loadingAction()">شحن الآن</span>
                <span *ngIf="loadingAction()" class="flex items-center gap-2 animate-pulse">جاري المعالجة...</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Right Panel: Recent Transactions -->
        <div class="lg:col-span-7 space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="text-white font-bold flex items-center gap-2">
              <span class="material-icons-round text-slate-400">history</span>
              أحدث عمليات الشحن
            </h3>
            <button (click)="loadAll()" class="btn-icon h-9 w-9">
              <span class="material-icons-round text-sm">refresh</span>
            </button>
          </div>

          <div class="edu-card p-0 overflow-hidden shadow-xl">
            <table class="edu-table">
              <thead>
                <tr>
                  <th>الطالب</th>
                  <th>المبلغ</th>
                  <th>الصلاحية</th>
                  <th>التاريخ</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let tx of transactions()">
                  <td>
                    <div class="flex flex-col">
                      <span class="text-slate-200 font-bold text-xs">{{ tx.studentName || '—' }}</span>
                      <span class="text-[9px] text-slate-500">{{ tx.studentCode || '' }}</span>
                    </div>
                  </td>
                  <td class="text-emerald-400 font-black text-xs">+{{ tx.amount | number:'1.0-2' }} ج.م</td>
                  <td class="text-[10px]">
                    <span *ngIf="tx.expiresAt" class="text-amber-400">
                      {{ tx.expiresAt | date:'dd/MM/yy HH:mm' }}
                    </span>
                    <span *ngIf="!tx.expiresAt" class="text-slate-600">دائم</span>
                  </td>
                  <td class="text-[10px] text-slate-500">{{ tx.createdAt | date:'dd/MM/yy HH:mm' }}</td>
                </tr>
                <tr *ngIf="transactions().length === 0 && !loadingTx()">
                  <td colspan="4" class="text-center py-20 text-slate-500 italic">لا توجد عمليات شحن مؤخراً</td>
                </tr>
                <tr *ngIf="loadingTx()">
                  <td colspan="4" class="text-center py-10">
                    <span class="animate-spin inline-block h-5 w-5 border-2 border-slate-700 border-t-indigo-500 rounded-full"></span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Pagination -->
          <div class="flex items-center justify-between pt-2">
            <span class="text-[11px] text-slate-600">صفحة {{ currentPage + 1 }} من {{ totalPages }}</span>
            <div class="flex gap-2">
              <button class="btn-icon h-8 w-8" [disabled]="currentPage === 0" (click)="changePage(currentPage - 1)">
                <span class="material-icons-round">chevron_right</span>
              </button>
              <button class="btn-icon h-8 w-8" [disabled]="currentPage >= totalPages - 1" (click)="changePage(currentPage + 1)">
                <span class="material-icons-round">chevron_left</span>
              </button>
            </div>
          </div>
        </div>

      </div>
    </div>
  `
})
export class WalletComponent implements OnInit {
  transactions = signal<WalletTransaction[]>([]);
  stats        = signal<any>(null);
  searchPhone  = '';
  foundStudent = signal<Student | null>(null);

  loadingSearch = signal(false);
  loadingAction = signal(false);
  loadingTx     = signal(false);

  currentPage = 0;
  totalPages  = 1;

  validityMode: 'days' | 'datetime' | 'none' = 'none';

  topUpForm: { amount: number | null; validDays: number | null; expiresAt: string | null } = {
    amount:    null,
    validDays: null,
    expiresAt: null,
  };

  constructor(private api: ApiService, private toastr: ToastrService) {}

  ngOnInit(): void { this.loadAll(); }

  loadAll() {
    this.loadTransactions(0);
    this.api.getWalletStats().subscribe({
      next: s => this.stats.set(s),
      error: () => {}
    });
  }

  loadTransactions(page = 0) {
    this.loadingTx.set(true);
    this.api.getWalletTransactions(page, 20).subscribe({
      next: pg => {
        this.transactions.set(pg.content || []);
        this.currentPage = pg.number ?? page;
        this.totalPages  = pg.totalPages ?? 1;
        this.loadingTx.set(false);
      },
      error: () => { this.transactions.set([]); this.loadingTx.set(false); }
    });
  }

  changePage(p: number) { this.loadTransactions(p); }

  searchStudent() {
    if (!this.searchPhone) return;
    this.loadingSearch.set(true);
    this.api.searchStudentByPhone(this.searchPhone).subscribe({
      next: s => {
        this.foundStudent.set(extractItem<Student>(s));
        this.loadingSearch.set(false);
      },
      error: () => {
        this.toastr.error('الطالب غير موجود');
        this.foundStudent.set(null);
        this.loadingSearch.set(false);
      }
    });
  }

  performTopUp() {
    if (!this.foundStudent() || !this.topUpForm.amount) return;

    this.loadingAction.set(true);
    const payload: any = {
      studentId: this.foundStudent()!.id,
      amount: this.topUpForm.amount,
    };

    if (this.validityMode === 'days' && this.topUpForm.validDays) {
      payload.validDays = this.topUpForm.validDays;
    } else if (this.validityMode === 'datetime' && this.topUpForm.expiresAt) {
      payload.expiresAt = this.topUpForm.expiresAt; // ISO string
    }
    // 'none' → no expiry fields sent

    this.api.topUpWallet(payload).subscribe({
      next: () => {
        this.toastr.success('تم شحن الرصيد بنجاح');
        this.loadingAction.set(false);
        this.loadAll();
        this.foundStudent.set(null);
        this.searchPhone = '';
        this.topUpForm   = { amount: null, validDays: null, expiresAt: null };
        this.validityMode = 'none';
      },
      error: (err: any) => {
        this.toastr.error(err?.error?.message || 'فشل في عملية الشحن');
        this.loadingAction.set(false);
      }
    });
  }
}
