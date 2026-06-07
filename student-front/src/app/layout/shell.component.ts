import { Component, OnInit, signal, HostListener } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { StudentAuthService } from '../services/student-auth.service';
import { StudentApiService } from '../services/student-api.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './shell.component.html'
})
export class ShellComponent implements OnInit {
  studentName     = signal('');
  studentFullName = signal('');
  studentCode     = signal('');
  notifOpen       = signal(false);
  darkMode        = signal(false);
  announcements   = signal<any[]>([]);
  teacherLogoUrl  = signal(localStorage.getItem('t_logo') || '');
  darkLogoUrl     = signal(localStorage.getItem('t_dark_logo') || '');
  studentAvatar   = signal('');
  profileOpen     = signal(false);

  constructor(
    private auth: StudentAuthService,
    private api: StudentApiService,
    private router: Router
  ) {}

  ngOnInit() {
    // Dark mode — persist in localStorage
    const saved = localStorage.getItem('s_theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const dark = saved ? saved === 'dark' : prefersDark;
    this.darkMode.set(dark);
    this.applyDarkMode(dark);

    this.api.getTeacherProfile().subscribe({
      next: (t: any) => {
        if (t?.logoUrl) {
          this.teacherLogoUrl.set(t.logoUrl);
          localStorage.setItem('t_logo', t.logoUrl);
        }
        if (t?.darkLogoUrl) {
          this.darkLogoUrl.set(t.darkLogoUrl);
          localStorage.setItem('t_dark_logo', t.darkLogoUrl);
        }
      },
      error: () => {}
    });

    this.api.getMe().subscribe({
      next: me => {
        const full = me.fullName || me.name || 'الطالب';
        this.studentFullName.set(full);
        this.studentName.set(full.split(' ')[0]);
        this.studentCode.set(me.studentCode || '');
        if (me.profileImageUrl) this.studentAvatar.set(me.profileImageUrl);
        if (me.status === 'PENDING') this.router.navigate(['/login']);
      },
      error: () => {}
    });

    this.api.getAnnouncements().subscribe({
      next: ann => this.announcements.set(Array.isArray(ann) ? ann.slice(0, 5) : []),
      error: () => {}
    });
  }

  toggleDark() {
    const next = !this.darkMode();
    this.darkMode.set(next);
    this.applyDarkMode(next);
    localStorage.setItem('s_theme', next ? 'dark' : 'light');
  }

  private applyDarkMode(dark: boolean) {
    document.documentElement.classList.toggle('dark', dark);
  }

  toggleNotif(e: Event) {
    e.stopPropagation();
    this.profileOpen.set(false);
    this.notifOpen.set(!this.notifOpen());
  }

  toggleProfile(e: Event) {
    e.stopPropagation();
    this.notifOpen.set(false);
    this.profileOpen.set(!this.profileOpen());
  }

  @HostListener('document:click')
  closeDropdowns() {
    this.notifOpen.set(false);
    this.profileOpen.set(false);
  }

  get initial(): string { return this.studentName()[0] || 'ط'; }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  get hasUnread(): boolean { return this.announcements().length > 0; }
}
