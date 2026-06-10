import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ParentApiService } from '../../services/parent-api.service';
import { ParentAuthService } from '../../services/parent-auth.service';

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
        <div class="edu-card animate-slide-up" style="padding:32px;">

          <!-- Step 1: Phone -->
          <ng-container *ngIf="step() === 1">
            <h2 class="text-xl font-bold text-white mb-1">تسجيل الدخول</h2>
            <p class="text-slate-400 text-sm mb-6">أدخل رقم هاتفك المسجّل لدينا</p>

            <form [formGroup]="phoneForm" (ngSubmit)="sendOtp()" class="space-y-5">
              <div>
                <label class="edu-label">رقم الهاتف</label>
                <div class="relative">
                  <input type="tel" formControlName="phone" placeholder="01xxxxxxxxx"
                         class="edu-input" style="padding-right:40px;" dir="ltr" />
                  <span class="material-icons-round" style="position:absolute;right:12px;top:50%;transform:translateY(-50%);color:#64748b;font-size:16px;">phone</span>
                </div>
                <p *ngIf="phoneForm.get('phone')?.invalid && phoneForm.get('phone')?.touched" class="edu-error">
                  <span class="material-icons-round" style="font-size:12px;">error</span>
                  يجب إدخال رقم هاتف مصري صحيح (01xxxxxxxxx)
                </p>
              </div>

              <p *ngIf="errorMsg()" style="color:#f87171;font-size:13px;text-align:center;">{{ errorMsg() }}</p>

              <button type="submit" [disabled]="loading() || phoneForm.invalid" class="btn-primary w-full justify-center text-base" style="padding:12px;width:100%;">
                <span *ngIf="loading()" class="material-icons-round animate-spin" style="font-size:16px;">refresh</span>
                <span *ngIf="!loading()" class="material-icons-round" style="font-size:16px;">send</span>
                {{ loading() ? 'جارٍ الإرسال...' : 'إرسال رمز التحقق' }}
              </button>
            </form>
          </ng-container>

          <!-- Step 2: OTP -->
          <ng-container *ngIf="step() === 2">
            <h2 class="text-xl font-bold text-white mb-1">رمز التحقق</h2>
            <p class="text-slate-400 text-sm mb-6">
              أُرسل رمز التحقق إلى <span style="color:#34d399;font-family:monospace;">{{ phoneForm.value.phone }}</span>
            </p>

            <form [formGroup]="otpForm" (ngSubmit)="verifyOtp()" class="space-y-5">
              <div>
                <label class="edu-label">رمز التحقق (OTP)</label>
                <div class="relative">
                  <input type="text" formControlName="otp" placeholder="123456"
                         class="edu-input" style="padding-right:40px;text-align:center;letter-spacing:6px;font-size:18px;font-family:monospace;"
                         dir="ltr" maxlength="6" />
                  <span class="material-icons-round" style="position:absolute;right:12px;top:50%;transform:translateY(-50%);color:#64748b;font-size:16px;">pin</span>
                </div>
                <p *ngIf="otpForm.get('otp')?.invalid && otpForm.get('otp')?.touched" class="edu-error">
                  <span class="material-icons-round" style="font-size:12px;">error</span>
                  الرمز يجب أن يكون 4-8 أرقام
                </p>
              </div>

              <p *ngIf="errorMsg()" style="color:#f87171;font-size:13px;text-align:center;">{{ errorMsg() }}</p>

              <button type="submit" [disabled]="loading() || otpForm.invalid" class="btn-primary" style="background:linear-gradient(135deg,#059669,#10b981);padding:12px;width:100%;justify-content:center;">
                <span *ngIf="loading()" class="material-icons-round animate-spin" style="font-size:16px;">refresh</span>
                <span *ngIf="!loading()" class="material-icons-round" style="font-size:16px;">login</span>
                {{ loading() ? 'جارٍ التحقق...' : 'تسجيل الدخول' }}
              </button>

              <button type="button" (click)="goBack()" style="width:100%;text-align:center;color:#94a3b8;font-size:13px;background:none;border:none;cursor:pointer;padding:8px;">
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
        this.errorMsg.set(err?.error?.message || 'رقم الهاتف غير مسجّل');
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
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMsg.set(err?.error?.message || 'رمز التحقق غير صحيح أو منتهي الصلاحية');
      }
    });
  }

  goBack(): void {
    this.step.set(1);
    this.errorMsg.set('');
    this.otpForm.reset();
  }
}
