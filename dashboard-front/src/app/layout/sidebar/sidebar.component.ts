import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';
import { PermissionService, Permission } from '../../services/permission.service';
import { Level } from '../../models/models';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  styles: [':host { display: block; }'],
  template: `
    <aside class="sidebar" style="width:260px">

      <!-- Logo -->
      <div class="sidebar-logo">
        <div class="logo-icon"><span class="material-icons-round">school</span></div>
        <div class="logo-text animate-fade-in">
          <span class="logo-name">Teacher Dashboard</span>
        </div>
      </div>

      <nav class="sidebar-nav custom-scrollbar">

        <!-- Dashboard -->
        <a routerLink="/dashboard" routerLinkActive="nav-leaf--active" class="nav-leaf nav-leaf--root">
          <span class="accent-bar-root"></span>
          <span class="material-icons-round nav-icon-root">dashboard</span>
          <span class="nav-label-root">الرئيسية</span>
        </a>

        <!-- Academic Years section -->
        <div class="nav-section" *ngIf="perm.hasAnyPermission([P.LEVELS_VIEW, P.CATEGORIES_VIEW, P.COURSE_ADD, P.LEVEL_STUDENTS, P.LEVEL_OVERVIEW])">
          <div class="section-title">
            <span class="section-dot"></span>
            السنوات الدراسية
          </div>

          <div *ngIf="perm.hasPermission(P.LEVELS_VIEW)">
            <div *ngFor="let level of levels(); let i = index">

              <!-- Level row -->
              <button (click)="toggleLevel(level.id)"
                class="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-right transition-all mb-0.5"
                [class.bg-orange-500]="isLevelActive(level.id)"
                [class.text-white]="isLevelActive(level.id)"
                [class.text-slate-400]="!isLevelActive(level.id)"
                [class.hover:bg-slate-800]="!isLevelActive(level.id)">
                <div class="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
                     [class.bg-white]="isLevelActive(level.id)"
                     [class.text-orange-500]="isLevelActive(level.id)"
                     [class.bg-slate-800]="!isLevelActive(level.id)"
                     [class.text-slate-500]="!isLevelActive(level.id)">
                  <span class="material-icons-round text-sm">school</span>
                </div>
                <div class="flex-1 min-w-0 text-right">
                  <div class="text-xs font-bold truncate">{{ level.name }}</div>
                </div>
                <span class="material-icons-round text-sm opacity-60 shrink-0 transition-transform duration-200"
                      [style.transform]="expandedLevelId() === level.id ? 'rotate(-90deg)' : 'rotate(0)'">
                  chevron_left
                </span>
              </button>

              <!-- Sub-items -->
              <div *ngIf="expandedLevelId() === level.id"
                   class="mr-3 mb-1 border-r-2 border-orange-500/30 pr-2 space-y-0.5">

                <a *ngIf="perm.hasPermission(P.CATEGORIES_VIEW)"
                   [routerLink]="['/level', level.id, 'categories']" routerLinkActive="!text-orange-400 !bg-orange-500/10"
                   class="flex items-center gap-2 px-3 py-1.5 rounded-lg text-slate-500 text-xs font-bold hover:text-slate-200 hover:bg-slate-800/60 transition-all">
                  <span class="material-icons-round text-sm">label</span>
                  <span>التصنيفات</span>
                </a>

                <a *ngIf="perm.hasPermission(P.COURSE_ADD)"
                   [routerLink]="['/level', level.id, 'add-course']" routerLinkActive="!text-orange-400 !bg-orange-500/10"
                   class="flex items-center gap-2 px-3 py-1.5 rounded-lg text-slate-500 text-xs font-bold hover:text-slate-200 hover:bg-slate-800/60 transition-all">
                  <span class="material-icons-round text-sm">add_circle</span>
                  <span>اضافة كورس</span>
                </a>

                <a *ngIf="perm.hasPermission(P.LEVEL_STUDENTS)"
                   [routerLink]="['/level', level.id, 'students']" routerLinkActive="!text-orange-400 !bg-orange-500/10"
                   class="flex items-center gap-2 px-3 py-1.5 rounded-lg text-slate-500 text-xs font-bold hover:text-slate-200 hover:bg-slate-800/60 transition-all">
                  <span class="material-icons-round text-sm">groups</span>
                  <span>الطلاب</span>
                </a>

                <a *ngIf="perm.hasPermission(P.LEVEL_OVERVIEW)"
                   [routerLink]="['/level', level.id, 'data']" routerLinkActive="!text-orange-400 !bg-orange-500/10"
                   class="flex items-center gap-2 px-3 py-1.5 rounded-lg text-slate-500 text-xs font-bold hover:text-slate-200 hover:bg-slate-800/60 transition-all">
                  <span class="material-icons-round text-sm">bar_chart</span>
                  <span>نظرة عامة</span>
                </a>
              </div>
            </div>

            <div *ngIf="loadingLevels()" class="px-3 py-2 text-center text-slate-700 text-xs">...</div>
          </div>

          <a *ngIf="perm.isTeacher()" routerLink="/levels-categories"
             class="flex items-center gap-2 px-3 py-2 mt-1 rounded-xl text-slate-600 text-xs font-bold hover:text-slate-400 hover:bg-slate-800/40 transition-all border border-dashed border-slate-800">
            <span class="material-icons-round text-sm">add</span>
            <span>اضافة سنة دراسية</span>
          </a>
        </div>

        <!-- Assessment section -->
        <div class="nav-section" *ngIf="perm.hasAnyPermission([P.QUIZZES, P.ASSIGNMENTS, P.QUESTION_BANK, P.TOPIC_TREE])">
          <div class="section-title"><span class="section-dot"></span>التقييم والنشاط</div>
          <a *ngIf="perm.hasPermission(P.QUIZZES)"
             routerLink="/quizzes" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">quiz</span>
            <span class="nav-label">بنك الاختبارات</span>
          </a>
          <a *ngIf="perm.hasPermission(P.ASSIGNMENTS)"
             routerLink="/assignments" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">assignment_turned_in</span>
            <span class="nav-label">المهام والواجبات</span>
          </a>
          <a *ngIf="perm.hasPermission(P.QUESTION_BANK)"
             routerLink="/question-bank" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">storage</span>
            <span class="nav-label">مخزن الاسئلة</span>
          </a>
          <a *ngIf="perm.hasPermission(P.TOPIC_TREE)"
             routerLink="/topic-tree" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">account_tree</span>
            <span class="nav-label">شجرة المحتوى</span>
          </a>
        </div>

        <!-- Students section -->
        <div class="nav-section" *ngIf="perm.hasAnyPermission([P.NEW_REQUESTS, P.STUDENTS_MANAGE, P.ATTENDANCE])">
          <div class="section-title"><span class="section-dot"></span>شؤون الطلاب</div>
          <a *ngIf="perm.hasPermission(P.NEW_REQUESTS)"
             routerLink="/new-requests" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">assignment_ind</span>
            <span class="nav-label">طلبات التسجيل</span>
          </a>
          <a *ngIf="perm.hasPermission(P.STUDENTS_MANAGE)"
             routerLink="/students" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">groups</span>
            <span class="nav-label">إدارة الطلاب</span>
          </a>
          <a *ngIf="perm.hasPermission(P.ATTENDANCE)"
             routerLink="/attendance" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">verified_user</span>
            <span class="nav-label">سجل الحضور</span>
          </a>
        </div>

        <!-- Finance section -->
        <div class="nav-section" *ngIf="perm.hasAnyPermission([P.WALLET, P.COUPONS, P.CODES_LIST])">
          <div class="section-title"><span class="section-dot"></span>الادوات والمالية</div>
          <a *ngIf="perm.hasPermission(P.WALLET)"
             routerLink="/wallet" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">account_balance_wallet</span>
            <span class="nav-label">محفظة الطالب</span>
          </a>
          <a *ngIf="perm.hasPermission(P.COUPONS)"
             routerLink="/coupons" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">confirmation_number</span>
            <span class="nav-label">الكوبونات والخصم</span>
          </a>
          <a *ngIf="perm.hasPermission(P.CODES_LIST)"
             routerLink="/codes" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">list_alt</span>
            <span class="nav-label">الاكواد</span>
          </a>
        </div>

        <!-- Center & Store section -->
        <div class="nav-section" *ngIf="perm.hasAnyPermission([P.CENTER_SCHEDULE, P.BOOKS_CODES, P.HOME_LAYOUT, P.SUPPORT_CHANNELS, P.TOPIC_ANALYTICS, P.CENTERS_MANAGE])">
          <div class="section-title"><span class="section-dot"></span>السنتر والمتجر</div>
          <a *ngIf="perm.hasPermission(P.CENTER_SCHEDULE)"
             routerLink="/center-schedule" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">calendar_month</span>
            <span class="nav-label">جدول السناتر</span>
          </a>
          <a *ngIf="perm.hasPermission(P.BOOKS_CODES)"
             routerLink="/books-codes" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">storefront</span>
            <span class="nav-label">أماكن بيع الكتب والأكواد</span>
          </a>
          <a *ngIf="perm.hasPermission(P.HOME_LAYOUT)"
             routerLink="/home-layout" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">dashboard_customize</span>
            <span class="nav-label">اعدادات منصة الطالب</span>
          </a>
          <a *ngIf="perm.hasPermission(P.SUPPORT_CHANNELS)"
             routerLink="/support-channels" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">support_agent</span>
            <span class="nav-label">قنوات الدعم</span>
          </a>
          <a *ngIf="perm.hasPermission(P.TOPIC_ANALYTICS)"
             routerLink="/topic-analytics" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">analytics</span>
            <span class="nav-label">تحليل نقاط الضعف</span>
          </a>
          <a *ngIf="perm.isTeacher()"
             routerLink="/analytics" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">bar_chart</span>
            <span class="nav-label">الإحصائيات والتقارير</span>
          </a>
          <a *ngIf="perm.hasPermission(P.CENTERS_MANAGE)"
             routerLink="/centers" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">corporate_fare</span>
            <span class="nav-label">السناتر والفروع</span>
          </a>
        </div>

        <!-- Admin section -->
        <div class="nav-section" *ngIf="perm.hasAnyPermission([P.ACTIVITY_LOGS, P.STAFF_MANAGE, P.BANNERS])">
          <div class="section-title"><span class="section-dot"></span>ادارة النظام</div>
          <a *ngIf="perm.hasPermission(P.ACTIVITY_LOGS)"
             routerLink="/activity-logs" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">manage_history</span>
            <span class="nav-label">سجل النشاطات</span>
          </a>
          <a *ngIf="perm.hasPermission(P.STAFF_MANAGE)"
             routerLink="/staff" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">admin_panel_settings</span>
            <span class="nav-label">فريق العمل</span>
          </a>
          <a *ngIf="perm.hasPermission(P.BANNERS)"
             routerLink="/banners" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">campaign</span>
            <span class="nav-label">البانرات الاعلانية</span>
          </a>
        </div>

        <!-- Profile & Logout -->
        <div class="nav-section">
          <div class="section-title"><span class="section-dot"></span>الحساب</div>
          <a routerLink="/profile" routerLinkActive="nav-leaf--active" class="nav-leaf">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon">person</span>
            <span class="nav-label">الملف الشخصي</span>
          </a>
          <button (click)="auth.logout()" class="nav-leaf w-full text-right" style="color:#f87171">
            <span class="accent-bar"></span><span class="material-icons-round nav-icon" style="color:#f87171">logout</span>
            <span class="nav-label">تسجيل الخروج</span>
          </button>
        </div>

      </nav>

    </aside>
  `
})
export class SidebarComponent implements OnInit {
  levels = signal<Level[]>([]);
  loadingLevels = signal(true);
  expandedLevelId = signal<number | null>(null);

  P = Permission;

  constructor(
    public auth: AuthService,
    public perm: PermissionService,
    private api: ApiService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadLevels();

    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd)
    ).subscribe(e => {
      const match = e.url.match(/\/level\/(\d+)\//);
      if (match) this.expandedLevelId.set(Number(match[1]));
    });

    const match = this.router.url.match(/\/level\/(\d+)\//);
    if (match) this.expandedLevelId.set(Number(match[1]));
  }

  loadLevels() {
    this.loadingLevels.set(true);
    this.api.getLevels().subscribe({
      next: levels => { this.levels.set(levels || []); this.loadingLevels.set(false); },
      error: () => { this.levels.set([]); this.loadingLevels.set(false); }
    });
  }

  toggleLevel(levelId: number) {
    if (this.expandedLevelId() === levelId) {
      this.expandedLevelId.set(null);
    } else {
      this.expandedLevelId.set(levelId);
    }
  }

  isLevelActive(levelId: number): boolean {
    return this.router.url.includes('/level/' + levelId + '/') || this.expandedLevelId() === levelId;
  }
}
