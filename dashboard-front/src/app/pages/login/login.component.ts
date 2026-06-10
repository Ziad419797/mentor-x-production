import { Component, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="min-h-screen bg-slate-950 flex items-center justify-center p-4"
         style="background: radial-gradient(ellipse at 70% 50%, rgba(99,102,241,0.15) 0%, transparent 60%), #0f172a;">
      <div class="w-full max-w-md">
        <!-- Logo -->
        <div class="text-center mb-8">
          <img src="assets/mentorx-logo.png" alt="Mentor-X" class="mx-auto mb-2" style="height:72px;object-fit:contain;" />
        </div>

        <!-- Card -->
        <div class="edu-card !p-8 animate-slide-up">
          <h2 class="text-xl font-bold text-white mb-6">تسجيل الدخول</h2>

          <form [formGroup]="form" (ngSubmit)="onSubmit()" class="space-y-5">
            <!-- Phone -->
            <div>
              <label class="edu-label">رقم الهاتف</label>
              <div class="relative">
                <input type="tel" formControlName="phone" placeholder="01xxxxxxxxx"
                       class="edu-input pr-10" dir="ltr" />
                <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-base">phone</span>
              </div>
              <p *ngIf="form.get('phone')?.invalid && form.get('phone')?.touched" class="edu-error">
                <span class="material-icons-round text-xs">error</span>
                يجب إدخال رقم هاتف مصري صحيح (01xxxxxxxxx)
              </p>
            </div>

            <!-- Password -->
            <div>
              <label class="edu-label">كلمة المرور</label>
              <div class="relative">
                <input [type]="showPass() ? 'text' : 'password'" formControlName="password"
                       placeholder="كلمة المرور..." class="edu-input pr-10 pl-10" />
                <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-base">lock</span>
                <button type="button" (click)="showPass.set(!showPass())"
                        class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300">
                  <span class="material-icons-round text-base">{{ showPass() ? 'visibility_off' : 'visibility' }}</span>
                </button>
              </div>
              <p *ngIf="form.get('password')?.invalid && form.get('password')?.touched" class="edu-error">
                <span class="material-icons-round text-xs">error</span>
                يجب إدخال كلمة المرور (6 أحرف على الأقل)
              </p>
            </div>

            <!-- Submit -->
            <button type="submit" [disabled]="loading() || form.invalid" class="btn-primary w-full justify-center text-base py-3">
              <span *ngIf="loading()" class="material-icons-round animate-spin text-base">refresh</span>
              <span *ngIf="!loading()" class="material-icons-round text-base">login</span>
              {{ loading() ? 'تسجيل الدخول' : 'تسجيل الدخول' }}
            </button>

            <!-- Forgot -->
            <p class="text-center text-sm text-slate-400">
              نسيت كلمة المرور؟
              <a routerLink="/forgot-password" class="text-indigo-400 hover:text-indigo-300 font-medium">استعادة الحساب</a>
            </p>
          </form>
        </div>
      </div>
    </div>
  `
})
export class LoginComponent {
  form = this.fb.group({
    phone: ['', [Validators.required, Validators.pattern(/^01[0-9]{9}$/)]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });
  loading = signal(false);
  showPass = signal(false);

  constructor(
    private fb: FormBuilder,
    private api: ApiService,
    private auth: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {}

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    const creds = { phone: this.form.value.phone!, password: this.form.value.password! };

    // جرّب teacher أولاً، لو فشل جرّب staff
    this.api.login(creds).subscribe({
      next: (res: any) => this.handleLoginSuccess(res),
      error: () => {
        this.api.staffLogin(creds).subscribe({
          next: (res: any) => this.handleLoginSuccess(res),
          error: () => {
            this.toastr.error('رقم الهاتف أو كلمة المرور غير صحيحة', 'خطأ');
            this.loading.set(false);
          }
        });
      }
    });
  }

  private handleLoginSuccess(res: any): void {
    const data = res?.data ?? res;
    const token = data?.accessToken ?? data?.token;
    const refreshToken = data?.refreshToken ?? null;
    if (!token) {
      this.toastr.error('لم يتم استلام رمز المصادقة', 'خطأ');
      this.loading.set(false);
      return;
    }
    this.auth.saveTokens(token, refreshToken);
    const profile = data?.teacher ?? data?.staff ?? data?.user ?? data;
    this.auth.saveProfile(profile);
    this.loading.set(false);
    this.router.navigate(['/dashboard']);
  }
}
