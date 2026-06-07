import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Coupon, Course } from '../../models/models';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-coupons',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">
      
      <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
        <div class="flex items-center gap-2 p-1 bg-slate-900/50 rounded-xl border border-slate-700/50 w-fit">
          <button (click)="activeTab.set('ALL')" [class.active]="activeTab() === 'ALL'" class="edu-tab px-6">كل الكوبونات</button>
          <button (click)="activeTab.set('VALID')" [class.active]="activeTab() === 'VALID'" class="edu-tab px-6">صالحة</button>
          <button (click)="activeTab.set('EXPIRED')" [class.active]="activeTab() === 'EXPIRED'" class="edu-tab px-6">منتهية</button>
        </div>

        <div class="flex items-center gap-3">
          <button (click)="openPreviewModal()" class="btn-secondary h-11 px-6">
            <span class="material-icons-round text-sm">visibility</span>
            معاينة كوبون
          </button>
          <button (click)="openCouponModal()" class="btn-primary h-11 px-6 shadow-lg shadow-indigo-600/20">
            <span class="material-icons-round text-sm">add</span>
            إنشاء كوبون
          </button>
        </div>
      </div>

      <!-- Coupons Table -->
      <div class="edu-card p-0 overflow-hidden shadow-xl">
        <table class="edu-table">
          <thead>
            <tr>
              <th>الكود</th>
              <th>نوع الخصم</th>
              <th>قيمة الخصم</th>
              <th>الاستخدام</th>
              <th>تاريخ الانتهاء</th>
              <th>الحالة</th>
              <th class="text-center">إجراءات</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let c of filteredCoupons()">
              <td><code class="bg-slate-800 px-2 py-1 rounded text-indigo-400 font-mono text-sm font-bold border border-slate-700">{{ c.code }}</code></td>
              <td>
                 <span class="text-xs">{{ c.type === 'PERCENTAGE' ? 'نسبة مئوية' : 'مبلغ ثابت' }}</span>
              </td>
              <td>
                 <span class="text-white font-bold">{{ c.value }}{{ c.type === 'PERCENTAGE' ? '%' : ' ج.م' }}</span>
              </td>
              <td>
                 <div class="flex flex-col gap-1 w-24">
                    <div class="flex items-center justify-between text-[9px] text-slate-500 font-bold">
                       <span>{{ c.usedCount }}</span>
                       <span>{{ c.maxUses }}</span>
                    </div>
                    <div class="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                       <div class="h-full bg-indigo-500" [style.width.%]="((c.usedCount ?? 0) / (c.maxUses ?? 1)) * 100"></div>
                    </div>
                 </div>
              </td>
              <td class="text-xs text-slate-400">{{ c.expiresAt | date:'mediumDate' }}</td>
              <td>
                 <button (click)="toggleCoupon(c)" 
                          [class.text-emerald-400]="c.status === 'ACTIVE'"
                          [class.text-slate-600]="c.status !== 'ACTIVE'"
                          class="flex items-center gap-2 text-xs font-medium hover:opacity-80 transition-opacity">
                   <span class="w-1.5 h-1.5 rounded-full" [class.bg-emerald-500]="c.status === 'ACTIVE'" [class.bg-slate-700]="c.status !== 'ACTIVE'"></span>
                   {{ c.status === 'ACTIVE' ? 'نشط' : 'معطل' }}
                 </button>
              </td>
              <td>
                <div class="flex items-center justify-center gap-2">
                  <button (click)="openCouponModal(c)" class="btn-icon text-indigo-400"><span class="material-icons-round text-sm">edit</span></button>
                  <button (click)="deleteCoupon(c.id)" class="btn-icon text-red-400"><span class="material-icons-round text-sm">delete</span></button>
                </div>
              </td>
            </tr>
            <tr *ngIf="filteredCoupons().length === 0">
               <td colspan="7" class="text-center py-20 text-slate-600 italic">لا توجد كوبونات للعرض</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Coupon Modal -->
      <div *ngIf="showModal()" class="modal-overlay">
        <div class="modal-box max-w-md">
          <div class="modal-header">
            <h3 class="text-white font-bold">{{ editingCoupon() ? 'تعديل كوبون' : 'إنشاء كوبون جديد' }}</h3>
            <button (click)="showModal.set(false)" class="btn-icon"><span class="material-icons-round">close</span></button>
          </div>
          <div class="modal-body space-y-4">
             <div>
               <label class="edu-label">كود الخصم *</label>
               <input type="text" [(ngModel)]="couponForm.code" class="edu-input font-mono uppercase text-left ltr" placeholder="SUMMER2024">
             </div>
             <div class="grid grid-cols-2 gap-4">
               <div>
                 <label class="edu-label">نوع الخصم</label>
                 <select [(ngModel)]="couponForm.type" class="edu-select">
                    <option value="PERCENTAGE">نسبة مئوية (%)</option>
                    <option value="FIXED_AMOUNT">مبلغ ثابت (ج.م)</option>
                 </select>
               </div>
               <div>
                 <label class="edu-label">القيمة</label>
                 <input type="number" [(ngModel)]="couponForm.value" class="edu-input" placeholder="0">
               </div>
             </div>
             <div class="grid grid-cols-2 gap-4">
               <div>
                 <label class="edu-label">أقصى استخدام</label>
                 <input type="number" [(ngModel)]="couponForm.maxUses" class="edu-input" placeholder="100">
               </div>
               <div>
                 <label class="edu-label">تاريخ الانتهاء</label>
                 <input type="date" [(ngModel)]="couponForm.expiresAt" class="edu-input">
               </div>
             </div>
          </div>
          <div class="modal-footer">
            <button (click)="showModal.set(false)" class="btn-secondary">إلغاء</button>
            <button (click)="saveCoupon()" [disabled]="!couponForm.code || !couponForm.value" class="btn-primary px-10">حفظ</button>
          </div>
        </div>
      </div>

      <!-- Preview Modal -->
      <div *ngIf="showPreviewModal()" class="modal-overlay">
        <div class="modal-box max-w-sm p-8 text-center space-y-6">
           <div class="w-20 h-20 bg-indigo-500/10 rounded-full flex items-center justify-center mx-auto">
             <span class="material-icons-round text-indigo-400 text-4xl">preview</span>
           </div>
           <div>
             <h3 class="text-white font-black text-xl mb-1">معاينة الكوبون</h3>
             <p class="text-slate-500 text-xs">اختبر قيمة الخصم قبل تفعيل الكود</p>
           </div>
           
           <div class="space-y-4 text-right">
              <div>
                <label class="edu-label text-[10px]">كود الخصم</label>
                <input type="text" [(ngModel)]="previewData.code" class="edu-input font-mono uppercase text-left ltr" placeholder="CODE123">
              </div>
              <div>
                <label class="edu-label text-[10px]">الكورس المستهدف</label>
                <select [(ngModel)]="previewData.courseId" class="edu-select h-11">
                  <option *ngFor="let c of courses()" [value]="c.id">{{ c.title }} ({{ c.price }} ج.م)</option>
                </select>
              </div>
           </div>

           <div *ngIf="previewResult()" class="p-4 bg-emerald-500/10 rounded-2xl border border-emerald-500/20 animate-slide-up">
              <p class="text-emerald-400 font-bold text-lg">السعر بعد الخصم: {{ previewResult() }} ج.م</p>
           </div>

           <div class="flex gap-3">
              <button (click)="showPreviewModal.set(false)" class="btn-secondary flex-1 justify-center">إغلاق</button>
              <button (click)="performPreview()" [disabled]="!previewData.code || !previewData.courseId" class="btn-primary flex-1 justify-center">اختبار</button>
           </div>
        </div>
      </div>

    </div>
  `
})
export class CouponsComponent implements OnInit {
  activeTab = signal<'ALL' | 'VALID' | 'EXPIRED'>('ALL');
  coupons = signal<Coupon[]>([]);
  courses = signal<Course[]>([]);
  
  showModal = signal(false);
  editingCoupon = signal<Coupon | null>(null);
  couponForm: any = { code: '', type: 'PERCENTAGE', value: 0, maxUses: 100, expiresAt: '', status: 'ACTIVE' };

  showPreviewModal = signal(false);
  previewData = { code: '', courseId: 0 };
  previewResult = signal<number | null>(null);

  constructor(private api: ApiService, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.loadCoupons();
    this.api.getCourses(0, 100).subscribe({
      next: (pg) => {
        this.courses.set(pg.content || []);
      },
      error: () => this.courses.set([])
    });
  }

  loadCoupons() {
    this.api.getCoupons().subscribe({ next: c => this.coupons.set(c) });
  }

  filteredCoupons() {
    const now = new Date();
    return this.coupons().filter(c => {
      if (this.activeTab() === 'VALID') return c.expiresAt ? new Date(c.expiresAt) > now && c.status === 'ACTIVE' : c.status === 'ACTIVE';
      if (this.activeTab() === 'EXPIRED') return c.expiresAt ? new Date(c.expiresAt) < now : false;
      return true;
    });
  }

  openCouponModal(c?: Coupon) {
    if (c) {
      this.editingCoupon.set(c);
      this.couponForm = { ...c, expiresAt: c.expiresAt?.split('T')[0] || '' };
    } else {
      this.editingCoupon.set(null);
      this.couponForm = { code: '', type: 'PERCENTAGE', value: 0, maxUses: 100, expiresAt: '', status: 'ACTIVE' };
    }
    this.showModal.set(true);
  }

  saveCoupon() {
    const payload = { ...this.couponForm };
    // Backend expects LocalDateTime — convert 'YYYY-MM-DD' → 'YYYY-MM-DDTHH:mm:ss'
    if (payload.expiresAt && !payload.expiresAt.includes('T')) {
      payload.expiresAt = payload.expiresAt + 'T23:59:59';
    }
    if (!payload.expiresAt) {
      delete payload.expiresAt;
    }
    const obs = this.editingCoupon()
      ? this.api.updateCoupon(this.editingCoupon()!.id, payload)
      : this.api.createCoupon(payload);

    obs.subscribe({
      next: () => {
        this.toastr.success('تم الحفظ بنجاح');
        this.showModal.set(false);
        this.loadCoupons();
      },
      error: (err: any) => {
        this.toastr.error(err?.error?.message || 'حدث خطأ أثناء الحفظ');
      }
    });
  }

  toggleCoupon(c: Coupon) {
    this.api.toggleCoupon(c.id).subscribe({ next: () => this.loadCoupons() });
  }

  deleteCoupon(id: number) {
    if (!confirm('هل أنت متأكد من حذف هذا الكوبون؟')) return;
    this.api.deleteCoupon(id).subscribe({
      next: () => {
        this.toastr.success('تم الحذف');
        this.loadCoupons();
      }
    });
  }

  openPreviewModal() {
    this.previewData = { code: '', courseId: 0 };
    this.previewResult.set(null);
    this.showPreviewModal.set(true);
  }

  performPreview() {
    this.api.previewCoupon(this.previewData.code, this.previewData.courseId).subscribe({
      next: (res: any) => {
        this.previewResult.set(res?.finalPrice ?? res?.discountedPrice ?? null);
      },
      error: () => this.toastr.error('كود غير صالح أو منتهي الصلاحية')
    });
  }
}
