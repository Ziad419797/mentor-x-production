import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastrService } from 'ngx-toastr';
import { interval, take } from 'rxjs';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="min-h-screen bg-[#0f172a] flex items-center justify-center p-4 relative overflow-hidden">
      <div class="absolute -top-24 -right-24 w-96 h-96 bg-indigo-600/10 rounded-full blur-3xl"></div>
      <div class="absolute -bottom-24 -left-24 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl"></div>

      <div class="w-full max-w-md z-10">
        <div class="flex flex-col items-center mb-8">
          <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-xl shadow-indigo-500/20 mb-4">
            <span class="material-icons-round text-white text-3xl">lock_reset</span>
          </div>
          <h1 class="text-white text-3xl font-black mb-1">نسيت كلمة المرور</h1>
          <p class="text-slate-500 font-medium">استعد الوصول لحسابك</p>
        </div>

        <div class="edu-card p-8 bg-slate-900/40 backdrop-blur-xl border-slate-700/50 shadow-2xl">
          
          <!-- Step 1: Enter Phone -->
          <div *ngIf="step() === 1" class="animate-fade-in">
            <p class="text-slate-400 text-sm mb-6 leading-relaxed">أدخل رقم الهاتف المسجل وسنرسل لك رمز التحقق المكون من 6 أرقام.</p>
            <form [formGroup]="phoneForm" (ngSubmit)="onPhoneSubmit()" class="space-y-6">
              <div>
                <label class="edu-label">رقم الهاتف</label>
                <div class="relative">
                  <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-lg">phone_iphone</span>
                  <input type="tel" formControlName="phone" class="edu-input pr-10 text-left ltr" placeholder="01xxxxxxxxx">
                </div>
              </div>
              <button type="submit" [disabled]="phoneForm.invalid || loading()" class="btn-primary w-full justify-center h-12 shadow-lg shadow-indigo-600/20">
                <span *ngIf="!loading()">إرسال الرمز</span>
                <span *ngIf="loading()" class="animate-pulse">جاري الإرسال...</span>
              </button>
            </form>
          </div>

          <!-- Step 2: OTP Verification -->
          <div *ngIf="step() === 2" class="animate-fade-in text-center">
            <p class="text-slate-400 text-sm mb-6 leading-relaxed">
              تم إرسال الرمز إلى <span class="text-indigo-400 font-bold ltr">{{ phoneForm.get('phone')?.value }}</span>
            </p>
            
            <div class="flex gap-2 justify-center mb-8 ltr" (paste)="onPaste($event)">
              <input *ngFor="let i of [0,1,2,3,4,5]" 
                     type="text" 
                     maxlength="1" 
                     class="w-12 h-14 bg-slate-800 border-2 border-slate-700 rounded-xl text-center text-2xl font-bold text-white focus:border-indigo-500 focus:outline-none transition-all shadow-lg"
                     [id]="'otp-' + i"
                     (keyup)="onKeyUp($event, i)"
                     (keydown)="onKeyDown($event, i)">
            </div>

            <div class="mb-8">
              <p *ngIf="timer() > 0" class="text-slate-500 text-sm">
                يمكنك إعادة الإرسال خلال <span class="text-indigo-400 font-bold font-mono">{{ formatTime(timer()) }}</span>
              </p>
              <button *ngIf="timer() === 0" (click)="onPhoneSubmit()" [disabled]="loading()" class="text-indigo-400 hover:text-indigo-300 text-sm font-bold underline">إعادة إرسال الرمز</button>
            </div>

            <button (click)="onOtpSubmit()" [disabled]="!isOtpComplete() || loading()" class="btn-primary w-full justify-center h-12 shadow-lg shadow-indigo-600/20">
              <span *ngIf="!loading()">تحقق من الرمز</span>
              <span *ngIf="loading()" class="animate-pulse">جاري التحقق...</span>
            </button>
            
            <button (click)="step.set(1)" class="text-slate-500 text-sm mt-6 hover:text-slate-300">تغيير رقم الهاتف</button>
          </div>

          <!-- Step 3: New Password -->
          <div *ngIf="step() === 3" class="animate-fade-in">
            <p class="text-slate-400 text-sm mb-6">قم بتعيين كلمة مرور جديدة قوية لحسابك.</p>
            <form [formGroup]="passwordForm" (ngSubmit)="onPasswordSubmit()" class="space-y-5">
              <div>
                <label class="edu-label">كلمة المرور الجديدة</label>
                <div class="relative">
                  <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-lg">lock</span>
                  <input [type]="showPassword() ? 'text' : 'password'" formControlName="newPassword" class="edu-input pr-10" placeholder="⬢⬢⬢⬢⬢⬢⬢⬢">
                </div>
              </div>
              <div>
                <label class="edu-label">تأكيد كلمة المرور</label>
                <div class="relative">
                  <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-lg">lock_outline</span>
                  <input [type]="showPassword() ? 'text' : 'password'" formControlName="confirmPassword" class="edu-input pr-10" placeholder="⬢⬢⬢⬢⬢⬢⬢⬢">
                </div>
                <div *ngIf="passwordForm.errors?.['mismatch'] && passwordForm.get('confirmPassword')?.touched" class="edu-error">كلمتا المرور غير متطابقتين</div>
              </div>
              
              <div class="flex items-center gap-2">
                <input type="checkbox" id="show-pass" (change)="showPassword.set(!showPassword())" class="rounded border-slate-600 bg-slate-800 text-indigo-600 focus:ring-indigo-500">
                <label for="show-pass" class="text-xs text-slate-400 cursor-pointer">إظهار كلمة المرور</label>
              </div>

              <button type="submit" [disabled]="passwordForm.invalid || loading()" class="btn-primary w-full justify-center h-12 shadow-lg shadow-indigo-600/20">
                <span *ngIf="!loading()">تغيير كلمة المرور</span>
                <span *ngIf="loading()" class="animate-pulse">جاري الحفظ...</span>
              </button>
            </form>
          </div>

          <div class="mt-8 pt-6 border-t border-slate-800 text-center">
             <a routerLink="/login" class="text-slate-500 text-sm hover:text-slate-300 flex items-center justify-center gap-2">
               <span class="material-icons-round text-base">arrow_forward</span>
               العودة لتسجيل الدخول
             </a>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ForgotPasswordComponent {
  step = signal(1);
  loading = signal(false);
  timer = signal(120);
  otpValues = Array(6).fill('');
  showPassword = signal(false);

  phoneForm: FormGroup;
  passwordForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private api: ApiService,
    private toastr: ToastrService,
    private router: Router
  ) {
    this.phoneForm = this.fb.group({
      phone: ['', [Validators.required, Validators.pattern(/^01[0125][0-9]{8}$/)]]
    });

    this.passwordForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(g: FormGroup) {
    return g.get('newPassword')?.value === g.get('confirmPassword')?.value
      ? null : { mismatch: true };
  }

  onPhoneSubmit() {
    if (this.phoneForm.invalid) return;
    this.loading.set(true);
    this.api.forgotPassword(this.phoneForm.value.phone).subscribe({
      next: () => {
        this.loading.set(false);
        this.step.set(2);
        this.startTimer();
        this.toastr.info('تم إرسال رمز التحقق');
      },
      error: (err) => {
        this.loading.set(false);
        this.toastr.error(err.error?.message || 'فشل إرسال الرمز');
      }
    });
  }

  onOtpSubmit() {
    const otp = this.otpValues.join('');
    this.loading.set(true);
    this.api.verifyOtp(this.phoneForm.value.phone, otp).subscribe({
      next: () => {
        this.loading.set(false);
        this.step.set(3);
        this.toastr.success('تم التحقق بنجاح');
      },
      error: (err) => {
        this.loading.set(false);
        this.toastr.error(err.error?.message || 'رمز التحقق غير صحيح');
      }
    });
  }

  onPasswordSubmit() {
    if (this.passwordForm.invalid) return;
    this.loading.set(true);
    this.api.resetPassword(this.phoneForm.value.phone, this.passwordForm.value.newPassword).subscribe({
      next: () => {
        this.loading.set(false);
        this.toastr.success('تم تغيير كلمة المرور بنجاح');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading.set(false);
        this.toastr.error(err.error?.message || 'فشل تغيير كلمة المرور');
      }
    });
  }

  onKeyUp(event: any, index: number) {
    const val = event.target.value;
    if (val) {
      this.otpValues[index] = val;
      if (index < 5) {
        const nextInput = document.getElementById(`otp-${index + 1}`);
        nextInput?.focus();
      }
    }
  }

  onKeyDown(event: KeyboardEvent, index: number) {
    if (event.key === 'Backspace') {
      if (!this.otpValues[index] && index > 0) {
        const prevInput = document.getElementById(`otp-${index - 1}`);
        prevInput?.focus();
      }
      this.otpValues[index] = '';
    }
  }

  onPaste(event: ClipboardEvent) {
    const paste = event.clipboardData?.getData('text');
    if (paste && paste.length === 6 && /^\d+$/.test(paste)) {
      this.otpValues = paste.split('');
      for (let i = 0; i < 6; i++) {
        const input = document.getElementById(`otp-${i}`) as HTMLInputElement;
        if (input) input.value = this.otpValues[i];
      }
      this.onOtpSubmit();
    }
  }

  isOtpComplete() {
    return this.otpValues.every(v => v !== '');
  }

  startTimer() {
    this.timer.set(120);
    interval(1000).pipe(take(120)).subscribe({
      next: () => this.timer.set(this.timer() - 1)
    });
  }

  formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  }
}
