import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { StudentAuthService } from '../../services/student-auth.service';
import { StudentApiService } from '../../services/student-api.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit {
  phone    = '';
  password = '';
  loading  = signal(false);
  error    = signal('');
  logoUrl  = signal(localStorage.getItem('t_logo') || '');

  constructor(
    private auth: StudentAuthService,
    private api: StudentApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Always fetch latest light-mode logo — no auth required
    this.api.getPublicBranding().subscribe({
      next: (b: any) => {
        if (b?.logoUrl) {
          localStorage.setItem('t_logo', b.logoUrl);
          this.logoUrl.set(b.logoUrl);
        }
        if (b?.darkLogoUrl) localStorage.setItem('t_dark_logo', b.darkLogoUrl);
      },
      error: () => {}
    });
  }

  login() {
    if (!this.phone.trim() || !this.password.trim()) {
      this.error.set('يرجى إدخال رقم الهاتف وكلمة المرور');
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.phone, this.password).subscribe({
      next: () => this.router.navigate(['/home']),
      error: (err: any) => {
        this.error.set(err?.error?.message || 'بيانات الدخول غير صحيحة');
        this.loading.set(false);
      }
    });
  }
}
