import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { StudentAuthService } from '../../services/student-auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  phone    = '';
  password = '';
  loading  = signal(false);
  error    = signal('');
  logoUrl  = localStorage.getItem('t_logo') || '';

  constructor(private auth: StudentAuthService, private router: Router) {}

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
