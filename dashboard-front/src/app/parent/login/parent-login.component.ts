import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ParentApiService } from '../services/parent-api.service';
import { ParentAuthService } from '../services/parent-auth.service';

@Component({
  selector: 'app-parent-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="min-h-screen flex items-center justify-center p-4"
         style="background: radial-gradient(ellipse at 30% 50%, rgba(16,185,129,0.12) 0%, transparent 60%), #0f172a;">
      <div class="w-full max-w-md">

        <!-- Logo -->
        <div class="text-center mb-8">
          <img src="assets/mentorx-logo.png" alt="Mentor-X" class="mx-auto mb-3"
               style="height:64px;object-fit:contain;" />
          <p class="text-slate-400 text-sm mt-2">بوابة أولياء الأمور</p>
        </div>

        <!-- Card -->
        <div class="edu-card !p-8 animate-slide-up">

          <!-- Step 1: Phone -->
          <ng-container *ngIf="step() === 1">
            <h2 class="text-xl font-bold text-white mb-1">تسجيل الدخول</h2>
            <p class="text-slate-400 text-sm mb-6">أدخل رقم هاتفك المسجّل لدينا</p>

            <form [formGroup]="phoneForm" (ngSubmit)="sendOtp()" class="space-y-5">
              <div>
                <label class="edu-label">رقم الهاتف</label>
                <div class="relative">
                  <input type="tel" formControlName="phone" placeholder="01xxxxxxxxx"
                         class="edu-input pr-10" dir="ltr" />
                  <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-base">phone</span>
                </div>
                <p *ngIf="phoneForm.get('phone')?.invalid && phoneForm.get('phone')?.touched" class="edu-error">
                  <span class="material-icons-round text-xs">error</span>
                  يجب إدخال رقم هاتف مصري صحيح (01xxxxxxxxx)
                </p>
              </div>

              <p *ngIf="errorMsg()" class="text-red-400 text-sm text-center">{{ errorMsg() }}</p>

              <button type="submit" [disabled]="loading() || phoneForm.invalid" class="btn-primary w-full justify-center text-base py-3">
                <span *ngIf="loading()" class="material-icons-round animate-spin text-base">refresh</span>
                <span *ngIf="!loading()" class="material-icons-round text-base">send</span>
                {{ loading() ? 'جارٍ الإرسال...' : 'إرسال رمز التحقق' }}
              </button>
            </form>
          </ng-container>

          <!-- Step 2: OTP -->
          <ng-container *ngIf="step() === 2">
            <h2 class="text-xl font-bold text-white mb-1">رمز التحقق</h2>
            <p class="text-slate-400 text-sm mb-6">
              أُرسل رمز التحقق إلى <span class="text-emerald-400 font-mono">{{ phoneForm.value.phone }}</span>
            </p>

            <form [formGroup]="otpForm" (ngSubmit)="verifyOtp()" class="space-y-5">
              <div>
                <label class="edu-label">رمز التحقق (OTP)</label>
                <div class="relative">
                  <input type="text" formControlName="otp" placeholder="123456"
                         class="edu-input pr-10 text-center tracking-widest text-lg font-mono" dir="ltr"
                         maxlength="6" />
                  <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-base">pin</span>
                </div>
                <p *ngIf="otpForm.get('otp')?.invalid && otpForm.get('otp')?.touched" class="edu-error">
                  <span class="material-icons-round text-xs">error</span>
                  الرمز يجب أن يكون 4-8 أرقام
                </p>
              </div>

              <p *ngIf="errorMsg()" class="text-red-400 text-sm text-center">{{ errorMsg() }}</p>

              <button type="submit" [disabled]="loading() || otpForm.invalid" class="btn-primary w-full justify-center text-base py-3"
                      style="background:linear-gradient(135deg,#059669,#10b981)">
                <span *ngIf="loading()" class="material-icons-round animate-spin text-base">refresh</span>
                <span *ngIf="!loading()" class="material-icons-round text-base">login</span>
                {{ loading() ? 'جارٍ التحقق...' : 'تسجيل الدخول' }}
              </button>

              <button type="button" (click)="goBack()" class="w-full text-center text-slate-400 hover:text-white text-sm transition-colors">
                ← تغيير رقم الهاتف
              </button>
            </form>
          </ng-container>

        </div>
      </div>
    </div>
  `
})
export class ParentLoginComponent {
  step = signal(1);
  loading = signal(false);
  errorMsg = signal('');

  phoneForm = this.fb.group({
    phone: ['', [Validators.required, Validators.pattern(/^01[0-9]{9}$/)]]
  });

  otpForm = this.fb.group({
    otp: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(8)]]
  });

  constructor(
    private fb: FormBuilder,
    private parentApi: ParentApiService,
    private parentAuth: ParentAuthService,
    private router: Router
  ) {}

  sendOtp(): void {
    if (this.phoneForm.invalid) { this.phoneForm.markAllAsTouched(); return; }
    this.loading.set(true);
    this.errorMsg.set('');

    this.parentApi.startLogin(this.phoneForm.value.phone!).subscribe({
      next: () => {
        this.loading.set(false);
        this.step.set(2);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err?.error?.message || 'رقم الهاتف غير مسجّل';
        this.errorMsg.set(msg);
      }
    });
  }

  verifyOtp(): void {
    if (this.otpForm.invalid) { this.otpForm.markAllAsTouched(); return; }
    this.loading.set(true);
    this.errorMsg.set('');

    this.parentApi.completeLogin(this.phoneForm.value.phone!, this.otpForm.value.otp!).subscribe({
      next: (res: any) => {
        const data = res?.data ?? res;
        const token = data?.token ?? data?.accessToken;
        if (!token) {
          this.loading.set(false);
          this.errorMsg.set('لم يتم استلام رمز المصادقة');
          return;
        }
        this.parentAuth.saveLogin(token, data?.parent ?? null);
        this.loading.set(false);
        this.router.navigate(['/parent/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err?.error?.message || 'رمز التحقق غير صحيح أو منتهي الصلاحية';
        this.errorMsg.set(msg);
      }
    });
  }

  goBack(): void {
    this.step.set(1);
    this.errorMsg.set('');
    this.otpForm.reset();
  }
}
