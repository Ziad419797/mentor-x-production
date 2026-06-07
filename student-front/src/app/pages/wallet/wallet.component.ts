import { Component, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { StudentApiService } from '../../services/student-api.service';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-wallet',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div dir="rtl" class="max-w-2xl mx-auto pb-10 space-y-6">

  <h2 class="font-black text-2xl text-[#183764] dark:text-white">المحفظة وسجل الشحن</h2>
  <p class="text-[#8892a0] text-sm -mt-4">اشحن محفظتك بسهولة وتابع كل عمليات الشحن السابقة.</p>

  <!-- Pending deposits banner -->
  <div *ngIf="pendingDeposits().length > 0"
       class="bg-yellow-50 dark:bg-yellow-500/10 border border-yellow-200 dark:border-yellow-500/30 rounded-2xl p-4 space-y-3">
    <div class="flex items-center gap-2 text-yellow-700 dark:text-yellow-400">
      <span class="material-symbols-outlined" style="font-size:20px">schedule</span>
      <p class="font-bold text-sm">عمليات شحن معلقة — يمكنك استكمال الدفع</p>
    </div>
    <div *ngFor="let tx of pendingDeposits()"
         class="flex items-center justify-between bg-white dark:bg-black/20 rounded-xl px-4 py-3">
      <div>
        <p class="text-sm font-bold text-[#183764] dark:text-white">{{ tx.amount | number:'1.0-0' }} ج.م</p>
        <p class="text-[10px] text-[#8892a0]">{{ tx.createdAt | date:'short' }}</p>
      </div>
      <button (click)="resumeDeposit(tx)"
              class="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-yellow-500 text-white text-xs font-bold hover:opacity-90 transition-opacity">
        <span class="material-symbols-outlined" style="font-size:14px">play_arrow</span>
        استكمال الدفع
      </button>
    </div>
  </div>

  <!-- Balance card -->
  <div class="bg-gradient-to-br from-[#183764] to-[#1a4a8a] rounded-2xl p-6 text-white shadow-xl flex items-center justify-between">
    <div>
      <p class="text-sm opacity-70 mb-1">رصيدك الحالي</p>
      <p class="text-4xl font-black">
        <span *ngIf="loadingWallet()">...</span>
        <span *ngIf="!loadingWallet()">{{ wallet()?.balance | number:'1.0-0' }}</span>
      </p>
      <p class="text-xs opacity-60 mt-1">جنيه مصري</p>
    </div>
    <div class="text-right space-y-1 text-xs opacity-70">
      <p>مشحون: <span class="font-bold text-white">{{ wallet()?.totalDeposited | number:'1.0-0' }} ج.م</span></p>
      <p>مصروف: <span class="font-bold text-white">{{ wallet()?.totalSpent | number:'1.0-0' }} ج.م</span></p>
    </div>
  </div>

  <!-- Charge methods -->
  <div class="grid grid-cols-1 md:grid-cols-2 gap-4">

    <!-- شحن باستخدام كود -->
    <div class="bg-white dark:bg-[#162033] border border-[#DDE1EA] dark:border-slate-800 rounded-2xl p-5 shadow-sm">
      <div class="flex items-center gap-3 mb-4">
        <div class="w-10 h-10 rounded-xl bg-[#e8edf7] dark:bg-white/10 flex items-center justify-center">
          <span class="material-symbols-outlined text-[#183764] dark:text-white" style="font-size:20px">key</span>
        </div>
        <div>
          <h3 class="font-bold text-[#183764] dark:text-white text-sm">شحن باستخدام كود</h3>
          <p class="text-[#8892a0] text-xs">أدخل كود الشحن الخاص بك</p>
        </div>
      </div>

      <input type="text" [(ngModel)]="codeInput" (ngModelChange)="codeInput = $event.toUpperCase()"
             placeholder="أدخل الكود هنا..."
             class="w-full px-4 py-2.5 rounded-xl border border-[#DDE1EA] dark:border-slate-700 bg-[#F5F6FA] dark:bg-white/5 text-[#183764] dark:text-white text-sm font-mono font-bold tracking-widest outline-none focus:border-[#183764] mb-3 text-center">

      <div *ngIf="codeError()" class="text-red-500 text-xs mb-2 text-center">{{ codeError() }}</div>
      <div *ngIf="codeSuccess()" class="text-green-600 dark:text-green-400 text-xs mb-2 text-center font-bold">{{ codeSuccess() }}</div>

      <button (click)="redeemCode()"
              [disabled]="!codeInput.trim() || redeemingCode()"
              class="w-full py-2.5 rounded-xl bg-[#183764] text-white text-sm font-bold hover:opacity-90 transition-opacity disabled:opacity-40">
        <span *ngIf="!redeemingCode()">تفعيل الكود</span>
        <span *ngIf="redeemingCode()" class="flex items-center justify-center gap-2">
          <span class="material-symbols-outlined animate-spin" style="font-size:16px">refresh</span>
          جاري التحقق...
        </span>
      </button>
    </div>

    <!-- شحن اونلاين -->
    <div #onlineSection class="bg-white dark:bg-[#162033] border border-[#DDE1EA] dark:border-slate-800 rounded-2xl p-5 shadow-sm">
      <div class="flex items-center gap-3 mb-4">
        <div class="w-10 h-10 rounded-xl bg-[#fff4e8] dark:bg-[#F59239]/10 flex items-center justify-center">
          <span class="material-symbols-outlined text-[#F59239]" style="font-size:20px">credit_card</span>
        </div>
        <div>
          <h3 class="font-bold text-[#183764] dark:text-white text-sm">شحن اونلاين</h3>
          <p class="text-[#8892a0] text-xs">فيزا / فوري / فودافون كاش</p>
        </div>
      </div>

      <input type="number" [(ngModel)]="onlineAmount"
             min="10" max="50000" placeholder="المبلغ بالجنيه..."
             class="w-full px-4 py-2.5 rounded-xl border border-[#DDE1EA] dark:border-slate-700 bg-[#F5F6FA] dark:bg-white/5 text-[#183764] dark:text-white text-sm font-bold outline-none focus:border-[#183764] mb-3 text-center">

      <!-- طرق الدفع -->
      <div *ngIf="loadingMethods()" class="flex justify-center py-3">
        <span class="material-symbols-outlined animate-spin text-[#8892a0]" style="font-size:20px">refresh</span>
      </div>
      <div *ngIf="!loadingMethods() && paymentMethods().length > 0" class="mb-3">
        <p class="text-xs text-[#8892a0] mb-2">اختر طريقة الدفع:</p>
        <div class="grid grid-cols-2 gap-2">
          <button *ngFor="let m of paymentMethods()"
                  (click)="selectedMethodId.set(m.paymentId)"
                  [class]="selectedMethodId() === m.paymentId
                    ? 'flex items-center gap-2 p-2 rounded-xl border-2 border-[#183764] bg-[#e8edf7] dark:bg-white/10 text-xs font-bold text-[#183764] dark:text-white'
                    : 'flex items-center gap-2 p-2 rounded-xl border border-[#DDE1EA] dark:border-slate-700 text-xs text-[#8892a0] hover:border-[#183764] transition-colors'">
            <img *ngIf="m.logo" [src]="m.logo" class="w-6 h-6 object-contain rounded" onerror="this.style.display='none'">
            <span>{{ m.name_ar }}</span>
          </button>
        </div>
      </div>

      <div *ngIf="onlineError()" class="text-red-500 text-xs mb-2 text-center">{{ onlineError() }}</div>

      <button (click)="depositOnline()"
              [disabled]="!onlineAmount || onlineAmount < 10 || !selectedMethodId() || depositingOnline()"
              class="w-full py-2.5 rounded-xl bg-[#F59239] text-white text-sm font-bold hover:opacity-90 transition-opacity disabled:opacity-40">
        <span *ngIf="!depositingOnline()">ادفع {{ onlineAmount || '' }} ج.م</span>
        <span *ngIf="depositingOnline()" class="flex items-center justify-center gap-2">
          <span class="material-symbols-outlined animate-spin" style="font-size:16px">refresh</span>
          جاري إنشاء رابط الدفع...
        </span>
      </button>

      <p class="text-[#8892a0] text-[10px] text-center mt-2">
        بعد الدفع سيُضاف الرصيد تلقائياً لمحفظتك
      </p>
    </div>
  </div>

  <!-- Transactions -->
  <div class="bg-white dark:bg-[#162033] border border-[#DDE1EA] dark:border-slate-800 rounded-2xl p-5 shadow-sm">
    <div class="flex items-center justify-between mb-4">
      <h3 class="font-bold text-[#183764] dark:text-white">سجل المعاملات</h3>
      <button *ngIf="completedTx().length > 5 && !showAll()"
              (click)="showAll.set(true)"
              class="text-xs text-[#183764] dark:text-white font-bold hover:underline">
        عرض الكل ({{ completedTx().length }})
      </button>
      <button *ngIf="showAll()"
              (click)="showAll.set(false)"
              class="text-xs text-[#8892a0] hover:underline">
        عرض أقل
      </button>
    </div>

    <div *ngIf="loadingTx()" class="flex justify-center py-8">
      <span class="material-symbols-outlined animate-spin text-[#8892a0]">refresh</span>
    </div>

    <div *ngIf="!loadingTx()" class="space-y-2">
      <div *ngFor="let tx of displayedTx()"
           class="flex items-center gap-3 p-3 rounded-xl border border-[#DDE1EA] dark:border-slate-800">
        <div class="w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0"
             [class]="isCredit(tx.type) ? 'bg-green-100 dark:bg-green-500/20' : 'bg-red-100 dark:bg-red-500/20'">
          <span class="material-symbols-outlined" style="font-size:18px"
                [class]="isCredit(tx.type) ? 'text-green-600' : 'text-red-500'">
            {{ isCredit(tx.type) ? 'arrow_downward' : 'arrow_upward' }}
          </span>
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-xs font-semibold text-[#183764] dark:text-white truncate">
            {{ tx.description || typeLabel(tx.type) }}
          </p>
          <p class="text-[10px] text-[#8892a0] mt-0.5">{{ tx.createdAt | date:'short' }}</p>
        </div>
        <span class="font-black text-sm flex-shrink-0"
              [class]="isCredit(tx.type) ? 'text-green-600' : 'text-red-500'">
          {{ isCredit(tx.type) ? '+' : '-' }}{{ tx.amount | number:'1.0-0' }} ج.م
        </span>
      </div>
      <div *ngIf="completedTx().length === 0"
           class="text-center py-10 text-[#8892a0] text-sm">لا توجد معاملات مكتملة</div>
    </div>
  </div>

</div>

<!-- ══════════════════ Payment Modal ══════════════════ -->
<div *ngIf="showPaymentModal()"
     class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
  <div class="bg-white dark:bg-[#162033] rounded-2xl shadow-2xl w-full max-w-lg mx-4 overflow-hidden flex flex-col"
       style="max-height: 90vh;">

    <div class="flex items-center justify-between px-5 py-4 border-b border-[#DDE1EA] dark:border-slate-800 flex-shrink-0">
      <div>
        <p class="font-bold text-[#183764] dark:text-white">إتمام عملية الدفع</p>
        <p class="text-xs text-[#8892a0] mt-0.5">أكمل الدفع ثم انتظر التأكيد التلقائي</p>
      </div>
      <button (click)="closePaymentModal()" class="text-[#8892a0] hover:text-red-500 transition-colors">
        <span class="material-symbols-outlined">close</span>
      </button>
    </div>

    <div class="px-5 py-2 bg-[#F5F6FA] dark:bg-black/20 border-b border-[#DDE1EA] dark:border-slate-800 flex items-center gap-2 flex-shrink-0">
      <span class="material-symbols-outlined animate-spin text-[#183764] dark:text-white" style="font-size:16px">refresh</span>
      <p class="text-xs text-[#8892a0]">في انتظار تأكيد الدفع... ستُغلق هذه النافذة تلقائياً.</p>
    </div>

    <div class="flex-1 overflow-hidden">
      <iframe *ngIf="paymentIframeUrl()"
              [src]="paymentIframeUrl()!"
              class="w-full border-0"
              style="height: 500px;"
              allow="payment *">
      </iframe>
    </div>
  </div>
</div>

<!-- Payment success toast -->
<div *ngIf="paymentSuccessToast()"
     class="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 flex items-center gap-3 px-6 py-3 rounded-2xl bg-green-500 text-white shadow-xl">
  <span class="material-symbols-outlined">check_circle</span>
  <p class="font-bold text-sm">تم إضافة الرصيد بنجاح! 🎉</p>
</div>
  `
})
export class WalletPageComponent implements OnInit, OnDestroy {
  wallet            = signal<any>(null);
  walletTx          = signal<any[]>([]);
  loadingWallet     = signal(true);
  loadingTx         = signal(true);
  showAll           = signal(false);
  paymentStatus     = signal<string>('');

  codeInput         = '';
  redeemingCode     = signal(false);
  codeError         = signal('');
  codeSuccess       = signal('');

  onlineAmount: number | null = null;
  depositingOnline  = signal(false);
  onlineError       = signal('');
  paymentMethods    = signal<any[]>([]);
  loadingMethods    = signal(false);
  selectedMethodId  = signal<number | null>(null);

  showPaymentModal    = signal(false);
  paymentIframeUrl    = signal<SafeResourceUrl | null>(null);
  paymentSuccessToast = signal(false);
  private pollingTxNumber = '';
  private pollingInterval: any = null;

  // معاملات منتهية فقط (بدون PENDING)
  completedTx = computed(() =>
    this.walletTx().filter(tx => tx.status !== 'PENDING')
  );

  // معاملات شحن معلقة فقط
  pendingDeposits = computed(() =>
    this.walletTx().filter(tx => tx.status === 'PENDING' && ['TOP_UP','DEPOSIT'].includes(tx.type))
  );

  constructor(
    private api: StudentApiService,
    private route: ActivatedRoute,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
    const status = this.route.snapshot.queryParamMap.get('status');
    if (status) this.paymentStatus.set(status);

    this.api.getMyWallet().pipe(catchError(() => of(null))).subscribe(w => {
      this.wallet.set(w);
      this.loadingWallet.set(false);
    });
    this.loadTransactions();
    this.loadPaymentMethods();
  }

  ngOnDestroy() {
    this.stopPolling();
  }

  loadTransactions() {
    this.loadingTx.set(true);
    this.api.getMyWalletTransactions().pipe(catchError(() => of([]))).subscribe((txs: any) => {
      const list = Array.isArray(txs) ? txs : (txs?.content ?? []);
      this.walletTx.set(list);
      this.loadingTx.set(false);
    });
  }

  loadPaymentMethods() {
    this.loadingMethods.set(true);
    this.api.getWalletPaymentMethods().pipe(catchError(() => of([]))).subscribe((methods: any) => {
      const list = Array.isArray(methods) ? methods : (methods?.data ?? []);
      this.paymentMethods.set(list);
      this.loadingMethods.set(false);
      if (list.length > 0) this.selectedMethodId.set(list[0].paymentId);
    });
  }

  displayedTx = () => this.showAll() ? this.completedTx() : this.completedTx().slice(0, 5);

  redeemCode() {
    const code = this.codeInput.trim();
    if (!code) return;
    this.codeError.set('');
    this.codeSuccess.set('');
    this.redeemingCode.set(true);
    this.api.redeemAccessCode(code).pipe(catchError(e => {
      this.codeError.set(e?.error?.message || 'الكود غير صحيح أو منتهي الصلاحية');
      return of(null);
    })).subscribe(res => {
      this.redeemingCode.set(false);
      if (!res) return;
      this.codeSuccess.set('تم تفعيل الكود بنجاح! ✓');
      this.codeInput = '';
      this.api.getMyWallet().pipe(catchError(() => of(null))).subscribe(w => this.wallet.set(w));
      this.loadTransactions();
    });
  }

  resumeDeposit(tx: any) {
    this.onlineAmount = tx.amount;
    if (this.selectedMethodId()) {
      this.depositOnline();
    } else {
      // scroll to online section and show hint
      document.querySelector('[data-online-section]')?.scrollIntoView({ behavior: 'smooth' });
      this.onlineError.set('اختر طريقة الدفع أولاً ثم اضغط ادفع');
    }
  }

  depositOnline() {
    if (!this.onlineAmount || this.onlineAmount < 10 || !this.selectedMethodId()) return;
    this.onlineError.set('');
    this.depositingOnline.set(true);

    this.api.walletDepositOnline(this.onlineAmount, this.selectedMethodId()!).pipe(
      catchError(e => {
        this.onlineError.set(e?.error?.message || 'حدث خطأ، حاول مرة أخرى');
        return of(null);
      })
    ).subscribe(res => {
      this.depositingOnline.set(false);
      if (!res) return;
      const url = res?.redirectUrl || res?.data?.redirectUrl;
      const txNumber = res?.transactionNumber || res?.data?.transactionNumber;
      if (url && txNumber) {
        this.loadTransactions(); // refresh to show new PENDING
        this.openPaymentModal(url, txNumber);
      } else {
        this.onlineError.set('لم يتم استلام رابط الدفع، حاول مرة أخرى');
      }
    });
  }

  openPaymentModal(url: string, txNumber: string) {
    this.pollingTxNumber = txNumber;
    this.paymentIframeUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(url));
    this.showPaymentModal.set(true);
    this.startPolling();
  }

  closePaymentModal() {
    this.stopPolling();
    this.showPaymentModal.set(false);
    this.paymentIframeUrl.set(null);
    this.pollingTxNumber = '';
    this.loadTransactions();
    this.api.getMyWallet().pipe(catchError(() => of(null))).subscribe(w => this.wallet.set(w));
  }

  startPolling() {
    this.stopPolling();
    this.pollingInterval = setInterval(() => {
      if (!this.pollingTxNumber) return;
      this.api.getDepositStatus(this.pollingTxNumber).pipe(catchError(() => of(null))).subscribe((res: any) => {
        const status = res?.status || res?.data?.status;
        if (status === 'COMPLETED') {
          this.stopPolling();
          this.showPaymentModal.set(false);
          this.paymentIframeUrl.set(null);
          this.paymentSuccessToast.set(true);
          setTimeout(() => this.paymentSuccessToast.set(false), 4000);
          this.api.getMyWallet().pipe(catchError(() => of(null))).subscribe(w => this.wallet.set(w));
          this.loadTransactions();
        } else if (status === 'FAILED') {
          this.stopPolling();
          this.showPaymentModal.set(false);
          this.paymentIframeUrl.set(null);
          this.onlineError.set('فشلت عملية الدفع. حاول مرة أخرى.');
          this.loadTransactions();
        }
      });
    }, 3000);
  }

  stopPolling() {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
      this.pollingInterval = null;
    }
  }

  isCredit(type: string): boolean {
    return ['TOP_UP', 'DEPOSIT', 'REFUND'].includes(type);
  }

  typeLabel(type: string): string {
    const map: any = {
      TOP_UP: 'شحن محفظة', DEPOSIT: 'شحن محفظة',
      DEDUCTION: 'خصم', PURCHASE: 'شراء كورس',
      REFUND: 'استرداد', TRANSFER: 'تحويل'
    };
    return map[type] || type;
  }
}
