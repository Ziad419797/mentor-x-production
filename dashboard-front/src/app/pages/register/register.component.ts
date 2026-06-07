import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="min-h-screen bg-[#0f172a] flex items-center justify-center p-4 relative overflow-hidden">
      <!-- Decorative Blobs -->
      <div class="absolute -top-24 -right-24 w-96 h-96 bg-indigo-600/10 rounded-full blur-3xl"></div>
      <div class="absolute -bottom-24 -left-24 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl"></div>

      <div class="w-full max-w-md z-10">
        <!-- Logo -->
        <div class="flex flex-col items-center mb-8">
          <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-xl shadow-indigo-500/20 mb-4">
            <span class="material-icons-round text-white text-3xl">school</span>
          </div>
          <h1 class="text-white text-3xl font-black tracking-tight mb-1">EduCore</h1>
          <p class="text-slate-500 font-medium">تسجيل مدرس جديد</p>
        </div>

        <div class="edu-card p-8 bg-slate-900/40 backdrop-blur-xl border-slate-700/50 shadow-2xl">
          <div *ngIf="!pendingApproval()" class="animate-fade-in">
            <form [formGroup]="registerForm" (ngSubmit)="onSubmit()" class="space-y-5">
              <!-- Name -->
              <div>
                <label class="edu-label">الاسم الكامل</label>
                <div class="relative">
                  <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-lg">person</span>
                  <input type="text" formControlName="fullName" class="edu-input pr-10" placeholder="أدخل اسمك بالكامل" (input)="onArabicInput($event, 'fullName')">
                </div>
                <div *ngIf="f['fullName'].touched && f['fullName'].errors?.['required']" class="edu-error">الاسم مطلوب</div>
                <div *ngIf="f['fullName'].touched && f['fullName'].errors?.['pattern']" class="edu-error">الاسم يجب أن يكون باللغة العربية فقط</div>
              </div>

              <!-- Phone -->
              <div>
                <label class="edu-label">رقم الهاتف</label>
                <div class="relative">
                  <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-lg">phone_iphone</span>
                  <input type="tel" formControlName="phone" class="edu-input pr-10 text-left ltr" placeholder="01xxxxxxxxx">
                </div>
                <div *ngIf="f['phone'].touched && f['phone'].invalid" class="edu-error">
                   رقم هاتف مصري صحيح مطلوب
                </div>
              </div>

              <!-- Password -->
              <div>
                <label class="edu-label">كلمة المرور</label>
                <div class="relative">
                  <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-lg">lock</span>
                  <input [type]="showPassword() ? 'text' : 'password'" formControlName="password" class="edu-input pr-10" placeholder="⬢⬢⬢⬢⬢⬢⬢⬢">
                  <button type="button" (click)="showPassword.set(!showPassword())" class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-indigo-400 transition-colors">
                    <span class="material-icons-round text-lg">{{ showPassword() ? 'visibility_off' : 'visibility' }}</span>
                  </button>
                </div>
                <div *ngIf="f['password'].touched && f['password'].invalid" class="edu-error">
                  كلمة المرور يجب أن لا تقل عن 6 أحرف
                </div>
              </div>

              <button type="submit" [disabled]="registerForm.invalid || loading()" class="btn-primary w-full justify-center h-12 text-lg shadow-lg shadow-indigo-600/20 active:scale-95">
                <span *ngIf="!loading()">تسجيل الحساب</span>
                <span *ngIf="loading()" class="flex items-center gap-2">
                  <svg class="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                  جاري الإرسال...
                </span>
              </button>
            </form>

            <div class="mt-8 pt-6 border-t border-slate-800 text-center">
              <p class="text-slate-500 text-sm">
                لديك حساب بالفعل؟
                <a routerLink="/login" class="text-indigo-400 hover:text-indigo-300 font-bold mr-1">تسجيل الدخول</a>
              </p>
            </div>
          </div>

          <!-- Pending Approval State -->
          <div *ngIf="pendingApproval()" class="text-center py-6 animate-slide-up">
            <div class="w-20 h-20 bg-emerald-500/10 rounded-full flex items-center justify-center mx-auto mb-6">
              <span class="material-icons-round text-emerald-500 text-4xl">verified</span>
            </div>
            <h2 class="text-white text-2xl font-bold mb-3">تم إرسال طلبك!</h2>
            <p class="text-slate-400 mb-8 leading-relaxed">
              حسابك قيد المراجعة حالياً من قبل الإدارة.
              <br>سنقوم بإشعارك فور تفعيل الحساب.
            </p>
            <a routerLink="/login" class="btn-secondary w-full justify-center">العودة لتسجيل الدخول</a>
          </div>
        </div>
      </div>
    </div>
  `
})
export class RegisterComponent {
  registerForm: FormGroup;
  loading = signal(false);
  pendingApproval = signal(false);
  showPassword = signal(false);

  constructor(
    private fb: FormBuilder,
    private api: ApiService,
    private toastr: ToastrService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.pattern(/^[\u0600-\u06FF\s]+$/)]],
      phone: ['', [Validators.required, Validators.pattern(/^01[0125][0-9]{8}$/)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  get f() { return this.registerForm.controls; }

  // يمنع كتابة أي حرف غير عربي في الوقت الفعلي
  onArabicInput(event: Event, controlName: string) {
    const input = event.target as HTMLInputElement;
    const filtered = input.value.replace(/[^\u0600-\u06FF\s]/g, '');
    if (input.value !== filtered) {
      input.value = filtered;
      this.registerForm.get(controlName)?.setValue(filtered, { emitEvent: false });
    }
  }

  onSubmit() {
    if (this.registerForm.invalid || this.loading()) return;
    this.loading.set(true);
    const { fullName, phone, password } = this.registerForm.value;
    this.api.register({ name: fullName, phone, password }).subscribe({
      next: () => { this.loading.set(false); this.pendingApproval.set(true); },
      error: (err: any) => {
        this.loading.set(false);
        this.toastr.error(err?.error?.message || 'حدث خطأ أثناء التسجيل');
      }
    });
  }
}
