import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastrService } from 'ngx-toastr';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-wallet-history',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10" dir="rtl">

      <!-- Header -->
      <div class="flex items-center gap-4">
        <a routerLink="/students"
           class="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors">
          <span class="material-icons-round">arrow_forward</span>
        </a>
        <div>
          <h2 class="text-white font-bold text-2xl tracking-tight">سجل المحفظة</h2>
          <p class="text-slate-400 text-sm mt-0.5">عرض معاملات محفظة الطالب</p>
        </div>
      </div>

      <!-- Search Bar -->
      <div class="edu-card p-4 flex flex-wrap gap-3 items-end">
        <div class="flex-1 min-w-[200px]">
          <label class="edu-label text-xs">بحث برقم الهاتف أو كود الطالب</label>
          <div class="relative">
            <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-sm">search</span>
            <input type="text" [(ngModel)]="searchQuery"
                   (keydown.enter)="search()"
                   class="edu-input pr-9" placeholder="رقم الهاتف أو كود الطالب...">
          </div>
        </div>
        <button (click)="search()" [disabled]="searching()"
                class="btn-primary h-11 px-6 disabled:opacity-50">
          {{ searching() ? 'جاري البحث...' : 'بحث' }}
        </button>
      </div>

      <!-- Student Card -->
      <div *ngIf="student()" class="edu-card p-5 flex items-center gap-5">
        <div class="w-14 h-14 rounded-full overflow-hidden bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white font-black text-xl flex-shrink-0">
          <img *ngIf="student()?.profileImageUrl" [src]="student()?.profileImageUrl" class="w-full h-full object-cover">
          <span *ngIf="!student()?.profileImageUrl">{{ (student()?.fullName || '?')[0] }}</span>
        </div>
        <div class="flex-1">
          <div class="text-white font-bold text-lg">{{ student()?.fullName }}</div>
          <div class="text-slate-400 text-sm">{{ student()?.phone }} · {{ student()?.studentCode }}</div>
          <div class="text-slate-500 text-xs mt-0.5">{{ student()?.grade || '' }} {{ student()?.centerName ? '· ' + student()?.centerName : '' }}</div>
        </div>
        <div class="text-center">
          <div class="text-3xl font-black text-yellow-400">{{ walletBalance() }}</div>
          <div class="text-slate-400 text-xs mt-0.5">الرصيد الحالي (ج.م)</div>
        </div>
      </div>

      <!-- Students Search Results (multiple) -->
      <div *ngIf="searchResults().length > 1" class="edu-card p-0 overflow-hidden">
        <div class="px-4 py-3 border-b border-slate-700 text-slate-400 text-sm">
          {{ searchResults().length }} نتيجة — اختر الطالب
        </div>
        <div class="divide-y divide-slate-800">
          <button *ngFor="let s of searchResults()"
                  (click)="selectStudent(s)"
                  class="w-full flex items-center gap-3 px-4 py-3 hover:bg-slate-800/50 transition-colors text-right">
            <div class="w-9 h-9 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white font-bold text-sm flex-shrink-0">
              {{ (s.fullName || '?')[0] }}
            </div>
            <div class="flex-1">
              <div class="text-white text-sm font-semibold">{{ s.fullName }}</div>
              <div class="text-slate-400 text-xs">{{ s.phone }} · ID: {{ s.id }}</div>
            </div>
          </button>
        </div>
      </div>

      <!-- Transactions Table -->
      <div *ngIf="student()" class="edu-card p-0 overflow-hidden">
        <div class="flex items-center justify-between px-5 py-4 border-b border-slate-700">
          <div>
            <h3 class="text-white font-semibold">سجل المعاملات</h3>
            <p class="text-slate-400 text-xs mt-0.5">{{ transactions().length }} معاملة</p>
          </div>
          <div class="flex items-center gap-2">
            <select [(ngModel)]="filterType" class="edu-select text-xs h-9 py-0">
              <option value="">كل الأنواع</option>
              <option value="DEPOSIT">إيداع</option>
              <option value="PURCHASE">شراء</option>
              <option value="REFUND">استرداد</option>
              <option value="EXPIRY">انتهاء صلاحية</option>
            </select>
          </div>
        </div>

        <div *ngIf="loadingTx()" class="text-center py-16 text-slate-500">جاري التحميل...</div>

        <div *ngIf="!loadingTx()" class="overflow-x-auto">
          <table class="edu-table">
            <thead>
              <tr>
                <th>#</th>
                <th>رقم العملية</th>
                <th>النوع</th>
                <th>المبلغ</th>
                <th>الرصيد بعد</th>
                <th>الوصف</th>
                <th>المرجع</th>
                <th>التاريخ</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let tx of filteredTx(); let i = index">
                <td class="text-slate-500">{{ i + 1 }}</td>
                <td><code class="text-slate-400 text-xs font-mono">{{ tx.transactionNumber || '—' }}</code></td>
                <td>
                  <span class="px-2 py-0.5 rounded-full text-xs font-semibold"
                        [class]="txBadge(tx.type)">
                    {{ txLabel(tx.type) }}
                  </span>
                </td>
                <td>
                  <span [class]="tx.type === 'DEPOSIT' || tx.type === 'REFUND'
                    ? 'text-green-400 font-bold' : 'text-red-400 font-bold'">
                    {{ tx.type === 'DEPOSIT' || tx.type === 'REFUND' ? '+' : '-' }}{{ tx.amount | number:'1.2-2' }} ج.م
                  </span>
                </td>
                <td class="text-slate-300 font-semibold">{{ tx.balanceAfter | number:'1.2-2' }} ج.م</td>
                <td class="text-slate-400 text-xs max-w-[240px]">{{ tx.description || '—' }}</td>
                <td class="text-slate-500 text-xs font-mono">{{ tx.referenceId || '—' }}</td>
                <td class="text-slate-500 text-xs">{{ tx.completedAt || tx.createdAt | date:'short' }}</td>
              </tr>
              <tr *ngIf="filteredTx().length === 0">
                <td colspan="8" class="text-center py-16 text-slate-600 italic">لا توجد معاملات</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Empty state -->
      <div *ngIf="!student() && !searching() && searched()"
           class="edu-card p-12 text-center text-slate-500">
        <span class="material-icons-round text-4xl mb-2 block">search_off</span>
        لم يتم العثور على طالب بهذا البحث
      </div>

    </div>
  `
})
export class WalletHistoryComponent implements OnInit {
  searchQuery = '';
  searching   = signal(false);
  searched    = signal(false);

  searchResults = signal<any[]>([]);
  student       = signal<any | null>(null);
  transactions  = signal<any[]>([]);
  loadingTx     = signal(false);
  filterType    = '';

  filteredTx = computed(() => {
    const list = this.transactions();
    if (!this.filterType) return list;
    return list.filter(tx => tx.type === this.filterType);
  });

  walletBalance = signal<number>(0);

  constructor(
    private api: ApiService,
    private route: ActivatedRoute,
    private toastr: ToastrService
  ) {}

  ngOnInit() {
    // Direct link from student list passes studentId — load wallet directly
    const idStr = this.route.snapshot.paramMap.get('studentId');
    if (idStr) {
      const id = Number(idStr);
      this.searching.set(true);
      this.student.set({ id, fullName: '...', phone: '' });
      this.searched.set(true);
      this.searching.set(false);
      this.loadWalletData(id);
    }
  }

  search() {
    const q = this.searchQuery.trim();
    if (!q) return;
    this.searching.set(true);
    this.searched.set(false);
    this.student.set(null);
    this.searchResults.set([]);
    this.transactions.set([]);

    // Phone: starts with 0 and all digits; otherwise treat as studentCode
    const isPhone = /^0\d+$/.test(q);
    const req$ = isPhone
      ? this.api.searchStudentByPhone(q)
      : this.api.searchStudentByCode(q);

    req$.subscribe({
      next: (s: any) => {
        this.searching.set(false);
        this.searched.set(true);
        if (s?.id) {
          this.selectStudent(s);
        } else {
          this.toastr.warning('لم يتم العثور على طالب');
        }
      },
      error: () => {
        this.searching.set(false);
        this.searched.set(true);
        this.toastr.error('لم يتم العثور على طالب بهذا البحث');
      }
    });
  }

  selectStudent(s: any) {
    this.student.set(s);
    this.searchResults.set([]);
    this.loadWalletData(s.id);
  }

  private loadWalletData(id: number) {
    this.loadingTx.set(true);
    this.transactions.set([]);
    this.walletBalance.set(0);

    forkJoin({
      wallet: this.api.getStudentWallet(id).pipe(catchError((e) => { console.error('[wallet err]', e?.status, e?.error); return of(null); })),
      txs:    this.api.getStudentWalletTransactions(id).pipe(catchError((e) => { console.error('[txs err]', e?.status, e?.error); return of([]); }))
    }).subscribe({
      next: ({ wallet, txs }) => {
        console.log('[wallet]', wallet);
        console.log('[txs raw]', txs);
        this.walletBalance.set(Number(wallet?.balance ?? 0));
        const sorted = [...(txs as any[])].sort((a: any, b: any) =>
          new Date(a.completedAt || a.createdAt).getTime() -
          new Date(b.completedAt || b.createdAt).getTime()
        );
        this.transactions.set(sorted);
        this.loadingTx.set(false);
      },
      error: (e) => { console.error('[forkJoin err]', e); this.loadingTx.set(false); }
    });
  }

  txLabel(type: string): string {
    return { DEPOSIT: 'إيداع', PURCHASE: 'شراء', REFUND: 'استرداد', EXPIRY: 'انتهاء صلاحية' }[type] ?? type;
  }

  txBadge(type: string): string {
    return ({
      DEPOSIT: 'bg-green-500/20 text-green-300',
      PURCHASE: 'bg-red-500/20 text-red-300',
      REFUND: 'bg-blue-500/20 text-blue-300',
      EXPIRY: 'bg-slate-500/20 text-slate-400',
    } as any)[type] ?? 'bg-slate-700 text-slate-300';
  }
}
