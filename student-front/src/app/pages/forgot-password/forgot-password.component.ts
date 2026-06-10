import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

const API = environment.apiBase;

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './forgot-password.component.html'
})
export class ForgotPasswordComponent {

  step = signal(1); // 1=phone, 2=otp, 3=new-password, 4=success
  loading = signal(false);

  phone = '';
  otpDigits = ['','','','','',''];
  otpCountdown = signal(0);
  private otpTimer: any;
  newPassword = '';
  confirmPassword = '';
  showPass = false;
  err: any = {};

  get otpString() { return this.otpDigits.join(''); }

  constructor(private http: HttpClient, private router: Router) {}

  async s1Next() {
    this.err = {};
    if (!/^01[0-9]{9}$/.test(this.phone.trim())) {
      this.err.phone = 'رقم الهاتف غير صحيح'; return;
    }
    this.loading.set(true);
    try {
      await this.http.post(`${API}/api/auth/forgot-password`, { phone: this.phone.trim() }).toPromise();
      this.step.set(2);
      this.startOtpTimer();
    } catch (e: any) {
      this.err.phone = e?.error?.message || 'الرقم غير مسجل';
    } finally {
      this.loading.set(false);
    }
  }

  startOtpTimer() {
    clearInterval(this.otpTimer);
    let secs = 300;
    this.otpCountdown.set(secs);
    this.otpTimer = setInterval(() => {
      secs--; this.otpCountdown.set(secs);
      if (secs <= 0) clearInterval(this.otpTimer);
    }, 1000);
  }

  get otpCountdownFormatted() {
    const s = this.otpCountdown();
    return Math.floor(s/60) + ':' + String(s%60).padStart(2,'0');
  }

  onOtpInput(i: number, event: any) {
    const val = event.target.value.replace(/\D/g, '');
    this.otpDigits[i] = val.slice(-1);
    event.target.value = this.otpDigits[i];
    if (val && i < 5) {
      const next = document.getElementById('f-otp-' + (i+1)) as HTMLInputElement;
      if (next) next.focus();
    }
    if (this.otpDigits.every(d => d)) this.s2Verify();
  }

  onOtpKeydown(i: number, event: KeyboardEvent) {
    if (event.key === 'Backspace' && !this.otpDigits[i] && i > 0) {
      const prev = document.getElementById('f-otp-' + (i-1)) as HTMLInputElement;
      if (prev) prev.focus();
    }
  }

  async s2Verify() {
    this.err = {};
    if (this.otpString.length < 6) { this.err.otp = 'أدخل الرمز كاملاً'; return; }
    this.loading.set(true);
    try {
      await this.http.post(`${API}/api/auth/verify-otp`, {
        phone: this.phone.trim(),
        otp: parseInt(this.otpString, 10)
      }).toPromise();
      this.step.set(3);
    } catch (e: any) {
      this.err.otp = e?.error?.message || 'الرمز غير صحيح أو منتهي الصلاحية';
    } finally {
      this.loading.set(false);
    }
  }

  async s3Submit() {
    this.err = {};
    if (this.newPassword.length < 6)              { this.err.password = 'كلمة المرور 6 أحرف على الأقل'; return; }
    if (this.newPassword !== this.confirmPassword) { this.err.confirm = 'كلمتا المرور غير متطابقتين'; return; }
    this.loading.set(true);
    try {
      await this.http.post(`${API}/api/auth/reset-password`, {
        phone: this.phone.trim(),
        newPassword: this.newPassword
      }).toPromise();
      this.step.set(4);
    } catch (e: any) {
      this.err.password = e?.error?.message || 'حدث خطأ';
    } finally {
      this.loading.set(false);
    }
  }

  goLogin() { this.router.navigate(['/login']); }
}
