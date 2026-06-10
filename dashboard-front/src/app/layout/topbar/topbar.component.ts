import { Component, OnInit, OnDestroy, Output, EventEmitter, signal } from '@angular/core';
import { Router, NavigationEnd, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter, interval, Subscription } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { CategoryStateService } from '../../services/category-state.service';
import { Category } from '../../models/models';
import { extractList } from '../../core/api-response.model';

const PAGE_TITLES: Record<string, string> = {
  '/dashboard': 'مركز القيادة والتحكم',
  '/levels-categories': 'إدارة المراحل التعليمية',
  '/courses': 'تخطيط المستويات الدراسية',
  '/sessions': 'خارطة المنهج والوحدات',
  '/materials': 'مكتبة المحتوى الرقمي',
  '/quizzes': 'نظام التقييم والاختبارات',
  '/assignments': 'إدارة التكليفات والمهام',
  '/question-bank': 'مستودع بنك الأسئلة',
  '/students': 'شؤون الطلاب والقبول',
  '/staff': 'إدارة فريق العمل',
  '/wallet': 'التقارير المالية والتحصيل',
  '/coupons': 'منظومة الكوبونات',
  '/access-codes': 'أكواد الدخول والوصول',
  '/enrollments': 'سجل اشتراكات الطلاب',
  '/attendance': 'سجل الحضور الذكي',
  '/student-cards': 'كروت الهوية الذكية',
  '/banners': 'البانرات الترويجية',
  '/centers': 'المراكز التعليمية',
  '/profile': 'إعدادات الحساب الشخصي',
  '/notifications': 'التنبيهات والإشعارات',
};

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <header class="topbar">

      <!-- Right: Menu button + Title -->
      <div class="topbar-right">
        <button (click)="menuToggle.emit()" class="btn-icon">
          <span class="material-icons-round">menu</span>
        </button>
        <h1 class="topbar-title animate-fade-in tracking-tight">{{ currentTitle() }}</h1>
      </div>

      <!-- Center: Logo -->
      <div class="category-selector-wrap flex items-center justify-center">
        <a routerLink="/dashboard" class="flex items-center">
          <img src="assets/mentorx-logo-topbar.png" alt="Mentor-X" style="height:36px;object-fit:contain;cursor:pointer;" />
        </a>
      </div>

      <!-- Left: Actions -->
      <div class="topbar-actions">

        <!-- User Profile -->
        <a routerLink="/profile" class="user-chip" id="user-chip">
          <div class="hidden sm:flex flex-col items-end min-w-0">
            <span class="user-chip-name text-white">{{ auth.currentUser()?.fullName }}</span>
            <span class="user-chip-role text-slate-500">{{ auth.currentUser()?.subject || 'معلم متخصص' }}</span>
          </div>
          <div class="user-chip-avatar">
            {{ getInitials(auth.currentUser()?.fullName) }}
          </div>
        </a>

      </div>
    </header>
  `
})
export class TopbarComponent implements OnInit, OnDestroy {
  @Output() menuToggle = new EventEmitter<void>();
  currentTitle = signal('الرئيسية');
  unreadCount = signal(0);
  categories = signal<Category[]>([]);

  private routerSub?: Subscription;
  private pollingSub?: Subscription;

  constructor(
    private router: Router,
    private api: ApiService,
    public auth: AuthService,
    public categoryState: CategoryStateService
  ) {}

  ngOnInit(): void {
    // Page title tracking
    this.routerSub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => {
        this.currentTitle.set(this.resolveTitle(this.router.url.split('?')[0]));
      });
    this.currentTitle.set(this.resolveTitle(this.router.url.split('?')[0]));

    // Load categories for selector
    this.api.getCategories().subscribe({
      next: (res: any) => {
        const list = extractList<Category>(res);
        const active = list.filter((c: Category) => c.status === 'ACTIVE');
        this.categories.set(active);
        this.categoryState.setCategories(active);
      },
      error: () => {}
    });

    // Notification badge
    this.loadUnreadCount();
    this.pollingSub = interval(30000).subscribe(() => this.loadUnreadCount());
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
    this.pollingSub?.unsubscribe();
  }

  resolveTitle(url: string): string {
    if (PAGE_TITLES[url]) return PAGE_TITLES[url];
    if (/\/level\/\d+\/students/.test(url)) return 'طلاب الصف الدراسي';
    if (/\/level\/\d+\/data/.test(url)) return 'نظرة عامة على الصف';
    if (/\/level\/\d+\/categories\/\d+\/courses/.test(url)) return 'كورسات التصنيف';
    if (/\/level\/\d+\/categories/.test(url)) return 'تصنيفات الصف الدراسي';
    if (/\/level\/\d+\/add-course/.test(url)) return 'إضافة كورس جديد';
    if (/\/level\/\d+\/add-category/.test(url)) return 'إضافة تصنيف جديد';
    if (/\/courses\/\d+\/sessions/.test(url)) return 'محاضرات الكورس';
    if (/\/sessions\/\d+\/content/.test(url)) return 'محتوى المحاضرة';
    if (/\/video\/\d+/.test(url)) return 'مشاهدة المحتوى';
    return 'الرئيسية';
  }

  selectCategory(cat: Category | null): void {
    this.categoryState.selectCategory(cat);
  }

  getLevelBadge(cat: Category): string {
    return cat.levelName ? cat.levelName.substring(0, 3) : '—';
  }

  loadUnreadCount(): void {
    this.api.getUnreadCount().subscribe({
      next: (r: any) => this.unreadCount.set(r.count || 0),
      error: () => {}
    });
  }

  getInitials(name?: string): string {
    if (!name) return '??';
    return name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
  }
}
