import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { StudentApiService } from '../../services/student-api.service';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError, map, switchMap, timeout } from 'rxjs/operators';

// ── Prayer times calculator (Cairo, Egypt) ──────────────────────
function calcPrayerTimes(date: Date): { name: string; time: Date }[] {
  // Coordinates: Cairo 30.0444°N, 31.2357°E — Muslim World League method
  const lat = 30.0444, lng = 31.2357;
  const d = date;
  const year = d.getFullYear(), month = d.getMonth() + 1, day = d.getDate();
  const jd = Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day - 1524.5
    + (month <= 2 ? -Math.floor(year / 100) + Math.floor(year / 400) + 2 : 0);

  const T = (jd - 2451545) / 36525;
  const L0 = (280.46646 + 36000.76983 * T) % 360;
  const M = (357.52911 + 35999.05029 * T - 0.0001537 * T * T) * Math.PI / 180;
  const e = 0.016708634 - 0.000042037 * T;
  const C = (1.914602 - 0.004817 * T) * Math.sin(M) + 0.019993 * Math.sin(2 * M);
  const sun = (L0 + C) * Math.PI / 180;
  const dec = Math.asin(Math.sin(23.439 * Math.PI / 180) * Math.sin(sun));
  const eot = (-104 * Math.sin(M) + 596 * Math.sin(2 * sun) - 4 * Math.sin(3 * M)
    + 150 * Math.cos(M) - 307 * Math.cos(2 * sun)) / 3600;
  const transit = 12 - lng / 15 - eot;

  function hourAngle(angle: number) {
    const h = Math.acos((-Math.sin(angle) - Math.sin(lat * Math.PI / 180) * Math.sin(dec))
      / (Math.cos(lat * Math.PI / 180) * Math.cos(dec)));
    return h * 12 / Math.PI;
  }

  const toDate = (hr: number) => {
    const total = ((hr % 24) + 24) % 24;
    const h = Math.floor(total), m = Math.round((total - h) * 60);
    const dt = new Date(date); dt.setHours(h, m, 0, 0); return dt;
  };

  const fajrHA  = hourAngle(-18 * Math.PI / 180);
  const sunrise = hourAngle(-0.833 * Math.PI / 180);
  const asr     = transit + (1 / 15) * Math.acos(
    (Math.sin(Math.atan(1 / (1 + Math.tan(Math.abs(lat * Math.PI / 180 - dec))))) -
      Math.sin(lat * Math.PI / 180) * Math.sin(dec)) /
    (Math.cos(lat * Math.PI / 180) * Math.cos(dec))) * 12 / Math.PI;
  const maghribHA = hourAngle(-0.833 * Math.PI / 180);
  const ishaHA    = hourAngle(-17 * Math.PI / 180);

  return [
    { name: 'الفجر',    time: toDate(transit - fajrHA) },
    { name: 'الظهر',    time: toDate(transit) },
    { name: 'العصر',    time: toDate(asr) },
    { name: 'المغرب',   time: toDate(transit + maghribHA) },
    { name: 'العشاء',   time: toDate(transit + ishaHA) },
  ];
}

function calcStreak(): number {
  const today = new Date().toDateString();
  const last  = localStorage.getItem('s_lastLogin');
  const saved = parseInt(localStorage.getItem('s_streak') || '0', 10);
  if (!last) {
    localStorage.setItem('s_lastLogin', today);
    localStorage.setItem('s_streak', '1');
    return 1;
  }
  if (last === today) return saved || 1;
  const yesterday = new Date(); yesterday.setDate(yesterday.getDate() - 1);
  const streak = last === yesterday.toDateString() ? saved + 1 : 1;
  localStorage.setItem('s_lastLogin', today);
  localStorage.setItem('s_streak', String(streak));
  return streak;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit, OnDestroy {
  loading         = signal(true);
  studentName     = signal('الطالب');
  studentFullName = signal('الطالب');
  studentCode     = signal('');
  studentGrade    = signal('');
  studentCenter   = signal('');
  studentOnline   = signal<boolean | null>(null);
  stats           = signal<any>(null);
  wallet          = signal<any>(null);
  announcements   = signal<any[]>([]);
  featuredCourses  = signal<any[]>([]);
  supportChannels  = signal<any[]>([]);
  supportOpen      = signal(false);
  enrollments      = signal<any[]>([]);
  enrolledIds      = signal<Set<number>>(new Set());
  teacher          = signal<any>(null);
  streak           = signal(1);
  showPrayerModal  = signal(false);
  prayerName       = signal('');
  enrollingId      = signal<number | null>(null);
  enrollError      = signal('');
  enrollFreeError  = signal('');

  // Course details modal
  activeCourseSessions = signal<any[]>([]);
  activeCourse         = signal<any | null>(null);
  loadingSessions      = signal(false);

  // Session drill-down
  activeSession      = signal<any | null>(null);
  weekContents       = signal<{weekId:number;title:string;materials:any[];quizzes:any[];assignments:any[];loading:boolean}[]>([]);
  sessionAccessError = signal(false);
  weeksLoading       = signal(false);

  // Payment modal
  paymentCourse        = signal<any | null>(null);
  paymentStep          = signal<'choose' | 'wallet' | 'code' | 'online'>('choose');
  paymentLoading       = signal(false);
  paymentError         = signal('');
  paymentSuccess       = signal('');
  accessCodeInput      = signal('');
  walletBalanceVal     = signal<number | null>(null);
  walletLoading        = signal(false);

  featuredIndex  = signal(0);
  enrollIndex    = signal(0);
  layoutConfig   = signal<any[]>([]);
  isDark         = signal(document.documentElement.classList.contains('dark'));
  private carouselTimer?:       ReturnType<typeof setInterval>;
  private enrollCarouselTimer?: ReturnType<typeof setInterval>;
  private readonly saveScroll = () => sessionStorage.setItem('home_scroll', String(window.scrollY));
  private prayerTimer?: ReturnType<typeof setTimeout>;

  private courseGradients = [
    'linear-gradient(135deg,#183764,#0f4080)',
    'linear-gradient(135deg,#006b58,#004d3e)',
    'linear-gradient(135deg,#7c3d00,#562d00)',
    'linear-gradient(135deg,#4a1d96,#2e1065)',
    'linear-gradient(135deg,#9d174d,#831843)',
    'linear-gradient(135deg,#1e3a5f,#0f2040)',
  ];

  constructor(private api: StudentApiService, public router: Router) {}

  ngOnInit() {
    window.addEventListener('scroll', this.saveScroll);
    new MutationObserver(() =>
      this.isDark.set(document.documentElement.classList.contains('dark'))
    ).observe(document.documentElement, { attributes: true, attributeFilter: ['class'] });
    this.streak.set(calcStreak());

    this.api.getSupportChannels().subscribe({
      next: (data: any) => this.supportChannels.set(Array.isArray(data) ? data : []),
      error: () => {}
    });
    this.schedulePrayerAlerts();

    const safe = <T>(obs: any, fallback: T) =>
      obs.pipe(timeout({ each: 8000, with: () => of(fallback) }), catchError(() => of(fallback)));

    forkJoin({
      me:          safe(this.api.getMe(), null),
      stats:       safe(this.api.getMyStats(), null),
      wallet:      safe(this.api.getMyWallet(), null),
      ann:         safe(this.api.getAnnouncements(), []),
      enrollments: safe(this.api.getMyEnrollments(), []),
      teacher:     safe(this.api.getTeacherProfile(), null),
      featured:    safe(this.api.getFeaturedCourses(6), []),
      layout:      safe(this.api.getTeacherHomeLayout(), null),
    }).subscribe(({ me: _me, stats, wallet, ann, enrollments, teacher, featured, layout }) => {
      const me = _me as any;
      if (me) {
        const full = me.fullName || me.name || 'الطالب';
        this.studentFullName.set(full);
        this.studentName.set(full.split(' ')[0] || full);
        this.studentCode.set(me.studentCode || '');
        this.studentGrade.set(me.grade || me.level?.name || '');
        this.studentCenter.set(me.centerName || me.center?.name || '');
        this.studentOnline.set(me.online ?? null);
      }
      this.stats.set(stats);
      this.wallet.set(wallet);
      this.announcements.set(Array.isArray(ann) ? ann.slice(0, 3) : []);
      const enrArr = Array.isArray(enrollments) ? enrollments : [];
      this.enrollments.set(enrArr);
      if (enrArr.length > 3) {
        this.enrollCarouselTimer = setInterval(() => {
          const pages = Math.ceil(this.enrollments().length / 3);
          this.enrollIndex.set((this.enrollIndex() + 1) % pages);
        }, 4000);
      }
      this.enrolledIds.set(new Set(enrArr.map((e: any) => e.courseId ?? e.course?.id)));
      this.teacher.set(teacher);
      this.featuredCourses.set(Array.isArray(featured) ? featured : []);
      // Parse layout config
      if (layout && typeof layout === 'string') {
        try { this.layoutConfig.set(JSON.parse(layout) as any[]); } catch { this.layoutConfig.set([]); }
      } else if (Array.isArray(layout)) {
        this.layoutConfig.set(layout);
      }
      this.loading.set(false);
      // Start carousel auto-advance
      this.carouselTimer = setInterval(() => {
        const len = this.featuredCourses().length;
        if (len > 1) this.featuredIndex.set((this.featuredIndex() + 1) % len);
      }, 3500);

      // Restore scroll position after navigating back
      const saved = sessionStorage.getItem('home_scroll');
      if (saved) {
        sessionStorage.removeItem('home_scroll');
        const target = +saved;
        if (target > 0) {
          // Hide content to prevent flash, scroll, then show
          document.body.style.opacity = '0';
          let attempts = 0;
          const tryScroll = () => {
            if (document.body.scrollHeight > target + window.innerHeight || attempts > 30) {
              window.scrollTo({ top: target, behavior: 'instant' as ScrollBehavior });
              document.body.style.opacity = '';
            } else {
              attempts++;
              requestAnimationFrame(tryScroll);
            }
          };
          requestAnimationFrame(tryScroll);
        }
      }
    });
  }

  ngOnDestroy() {
    window.removeEventListener('scroll', this.saveScroll);
    if (this.prayerTimer) clearTimeout(this.prayerTimer);
    if (this.carouselTimer)       clearInterval(this.carouselTimer);
    if (this.enrollCarouselTimer) clearInterval(this.enrollCarouselTimer);
  }

  nextEnroll() {
    const pages = Math.ceil(this.enrollments().length / 3);
    if (pages > 1) this.enrollIndex.set((this.enrollIndex() + 1) % pages);
  }
  prevEnroll() {
    const pages = Math.ceil(this.enrollments().length / 3);
    if (pages > 1) this.enrollIndex.set((this.enrollIndex() - 1 + pages) % pages);
  }
  goToEnroll(i: number) { this.enrollIndex.set(i); }

  nextCourse() {
    const len = this.featuredCourses().length;
    if (len > 1) this.featuredIndex.set((this.featuredIndex() + 1) % len);
  }
  prevCourse() {
    const len = this.featuredCourses().length;
    if (len > 1) this.featuredIndex.set((this.featuredIndex() - 1 + len) % len);
  }

  private shownPrayers = new Set<string>();

  private schedulePrayerAlerts() {
    // امسح localStorage keys القديمة من يوم تاني
    const today = new Date().toDateString();
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i);
      if (k && k.startsWith('s_prayer_') && !k.endsWith(today)) {
        localStorage.removeItem(k);
      }
    }

    const prayers = calcPrayerTimes(new Date());
    const now = Date.now();
    for (const p of prayers) {
      const diff = p.time.getTime() - now;
      // الصلاة جاية في أقل من 24 ساعة ولسه ما جاتش
      if (diff > 0 && diff < 24 * 60 * 60 * 1000) {
        const name = p.name;
        setTimeout(() => {
          if (!this.shownPrayers.has(name)) {
            this.shownPrayers.add(name);
            this.prayerName.set(name);
            this.showPrayerModal.set(true);
          }
        }, diff);
      }
    }
  }

  // ── Enrollment ───────────────────────────────────────────────
  isEnrolled(courseId: number): boolean { return this.enrolledIds().has(courseId); }

  // Open payment modal
  openPayment(course: any, ev: Event) {
    ev.stopPropagation();
    if (this.isEnrolled(course.id)) return;
    this.paymentCourse.set(course);
    this.paymentStep.set('choose');
    this.paymentError.set('');
    this.paymentSuccess.set('');
    this.accessCodeInput.set('');
    this.walletBalanceVal.set(null);
  }

  closePayment() { this.paymentCourse.set(null); }

  selectWallet() {
    this.paymentStep.set('wallet');
    this.paymentError.set('');
    this.walletLoading.set(true);
    this.api.getWalletBalance().subscribe({
      next: (r: any) => {
        this.walletBalanceVal.set(Number(r?.balance ?? r ?? 0));
        this.walletLoading.set(false);
      },
      error: () => { this.walletLoading.set(false); }
    });
  }

  confirmWalletPay() {
    const course = this.paymentCourse();
    if (!course) return;
    this.paymentLoading.set(true);
    this.paymentError.set('');
    this.api.createOrder(course.id).pipe(
      switchMap((order: any) => this.api.payWithWallet(order.id ?? order.orderId))
    ).subscribe({
      next: (res: any) => {
        this.paymentLoading.set(false);
        if (res?.success === false) {
          this.paymentError.set(res?.message || 'فشل الدفع');
          return;
        }
        this._markEnrolled(course.id);
        this.paymentSuccess.set('تم الاشتراك بنجاح 🎉');
        setTimeout(() => this.closePayment(), 2000);
      },
      error: (err: any) => {
        this.paymentLoading.set(false);
        this.paymentError.set(err?.error?.message || 'حدث خطأ أثناء الدفع');
      }
    });
  }

  redeemCode() {
    const code = this.accessCodeInput().trim();
    const course = this.paymentCourse();
    if (!code || !course) return;
    this.paymentLoading.set(true);
    this.paymentError.set('');
    this.api.redeemAccessCode(code, course.id).subscribe({
      next: (res: any) => {
        this.paymentLoading.set(false);
        if (!res?.success) {
          this.paymentError.set(res?.message || 'الكود غير صحيح');
          return;
        }
        if (res?.enrollmentsCreated > 0) this._markEnrolled(course.id);
        this.paymentSuccess.set(res?.message || 'تم تفعيل الكود بنجاح 🎉');
        setTimeout(() => this.closePayment(), 2000);
      },
      error: (err: any) => {
        this.paymentLoading.set(false);
        this.paymentError.set(err?.error?.message || 'الكود غير صحيح أو منتهي الصلاحية');
      }
    });
  }

  payOnline() {
    const course = this.paymentCourse();
    if (!course) return;
    this.paymentLoading.set(true);
    this.paymentError.set('');
    this.api.createOrder(course.id).pipe(
      switchMap((order: any) => this.api.payOnline(order.id ?? order.orderId))
    ).subscribe({
      next: (res: any) => {
        this.paymentLoading.set(false);
        if (res?.redirectUrl) {
          window.location.href = res.redirectUrl;
        } else if (res?.success) {
          this._markEnrolled(course.id);
          this.paymentSuccess.set('تم الاشتراك بنجاح 🎉');
          setTimeout(() => this.closePayment(), 2000);
        } else {
          this.paymentError.set(res?.message || 'فشل الاتصال ببوابة الدفع');
        }
      },
      error: (err: any) => {
        this.paymentLoading.set(false);
        this.paymentError.set(err?.error?.message || 'فشل الاتصال ببوابة الدفع');
      }
    });
  }

  private _markEnrolled(courseId: number) {
    const s = new Set(this.enrolledIds()); s.add(courseId);
    this.enrolledIds.set(s);
    this.featuredCourses.set(
      this.featuredCourses().map(c => c.id === courseId ? { ...c, isEnrolled: true } : c)
    );
  }

  // legacy — kept for any direct call
  enroll(course: any, ev: Event) { this.openPayment(course, ev); }

  enrollFree(course: any, ev: Event) {
    ev.stopPropagation();
    if (!course || this.isEnrolled(course.id)) return;
    this.enrollFreeError.set('');
    this.api.purchaseCourse(course.id).subscribe({
      next: () => { this._markEnrolled(course.id); this.closeDetails(); },
      error: (err: any) => {
        const msg = err?.error?.message || err?.message || 'حدث خطأ أثناء الاشتراك';
        this.enrollFreeError.set(msg);
      }
    });
  }

  goToCourse(course: any, ev: Event) {
    ev.stopPropagation();
    const courseId = course?.course?.id ?? course?.courseId ?? course?.id;
    this.router.navigate(['/courses', courseId]);
  }

  // ── Course details modal ──────────────────────────────────────
  openDetails(course: any, ev: Event) {
    ev.stopPropagation();
    this.activeCourse.set(course);
    this.activeCourseSessions.set([]);
    this.loadingSessions.set(true);
    this.api.getSessionsByCourse(course.id).subscribe({
      next: (sessions: any[]) => {
        const all = Array.isArray(sessions) ? sessions : [];
        // فلترة حسب نوع الطالب
        const online = this.studentOnline();
        const filtered = all.filter((s: any) => {
          const t = s.teachingType;
          if (!t || t === 'BOTH') return true;
          if (online === true)  return t === 'ONLINE';
          if (online === false) return t === 'CENTER';
          return true;
        });
        this.activeCourseSessions.set(filtered);
        this.loadingSessions.set(false);
      },
      error: () => { this.activeCourseSessions.set([]); this.loadingSessions.set(false); }
    });
  }

  closeDetails() { this.activeCourse.set(null); this.activeSession.set(null); this.enrollFreeError.set(''); }
  backToSessions() { this.activeSession.set(null); }

  openSession(s: any) {
    this.activeSession.set(s);
    this.weekContents.set([]);
    this.sessionAccessError.set(false);
    this.weeksLoading.set(true);
    this.api.getWeeksBySession(s.id).subscribe({
      next: (weeks: any[]) => {
        this.weeksLoading.set(false);
        const list = (Array.isArray(weeks) ? weeks : []).map((w: any) => ({
          weekId: w.id, title: w.title, materials: [], quizzes: [], assignments: [], loading: true
        }));
        this.weekContents.set(list);
        if (list.length === 0) return;
        list.forEach((w, idx) => {
          Promise.all([
            this.api.getMaterialsByWeek(w.weekId).toPromise().catch(() => []),
            this.api.getQuizzesByWeek(w.weekId).toPromise().catch(() => []),
            this.api.getAssignmentsByWeek(w.weekId).toPromise().catch(() => []),
          ]).then(([mats, quizzes, assignments]) => {
            const arr = [...this.weekContents()];
            arr[idx] = { ...arr[idx], materials: mats || [], quizzes: quizzes || [], assignments: assignments || [], loading: false };
            this.weekContents.set(arr);
          });
        });
      },
      error: () => {
        this.weeksLoading.set(false);
        this.sessionAccessError.set(true);
      }
    });
  }

  formatDuration(seconds: number): string {
    if (!seconds) return '';
    const m = Math.round(seconds / 60);
    return m < 60 ? `${m} دقيقة` : `${Math.floor(m/60)}س ${m%60}د`;
  }

  matIcon(type: string): string {
    const map: any = { VIDEO:'play_circle', YOUTUBE:'smart_display', PDF:'picture_as_pdf',
      IMAGE:'image', DOC:'description', PPT:'slideshow', AUDIO:'headphones',
      ARCHIVE:'folder_zip', OTHER:'attachment' };
    return map[type] || 'attachment';
  }

  matColor(type: string): string {
    const map: any = { VIDEO:'text-blue-500', YOUTUBE:'text-red-500', PDF:'text-red-600',
      IMAGE:'text-purple-500', DOC:'text-blue-600', PPT:'text-orange-500',
      AUDIO:'text-green-500', ARCHIVE:'text-yellow-600', OTHER:'text-[#8892a0]' };
    return map[type] || 'text-[#8892a0]';
  }

  // ── Computed getters ──────────────────────────────────────────
  get initial(): string { return this.studentName()[0] || 'ط'; }

  get studyTypeLabel(): string {
    const o = this.studentOnline();
    if (o === true)  return 'أونلاين';
    if (o === false) return 'سنتر';
    return '';
  }

  get enrollCount(): string {
    const s = this.stats();
    return s ? String(s.totalEnrollments ?? s.activeEnrollments ?? '—') : '—';
  }

  get quizzesSolved(): string {
    const s = this.stats();
    return s ? String(s.totalQuizzesTaken ?? '—') : '—';
  }

  get avgProgress(): string {
    const s = this.stats();
    return s?.averageProgress != null ? Math.round(s.averageProgress) + '%' : '—';
  }

  get avgProgressNum(): number {
    return Math.min(100, Math.round(this.stats()?.averageProgress ?? 0));
  }

  get completedCount(): string {
    const s = this.stats();
    return s ? String(s.completedEnrollments ?? '—') : '—';
  }

  get videoCount(): string {
    const s = this.stats() as any;
    return s ? String(s.totalVideos ?? s.videoCount ?? s.totalMaterials ?? '—') : '—';
  }

  get walletBalance(): string {
    const w = this.wallet();
    return w ? (w.balance ?? w.amount ?? 0).toLocaleString('ar-EG') : '—';
  }

  get teacherName():          string { return this.teacher()?.name    || ''; }
  get teacherSubject():       string { return this.teacher()?.subject || ''; }
  get teacherQuote():         string { return this.teacher()?.quote   || this.teacher()?.bio || ''; }
  get teacherAvatar():        string { return this.teacher()?.profileImageUrl || ''; }
  get teacherCourseCount():   number { return this.teacher()?.courseCount  ?? 0; }
  get teacherStudentCount():  number { return this.teacher()?.studentCount ?? 0; }
  get teacherHomeCardImage(): string { return (this.teacher() as any)?.homeCardImageUrl || ''; }
  get teacherLogoUrl():       string { return (this.teacher() as any)?.logoUrl || ''; }
  get teacherCardUrl():       string { return (this.teacher() as any)?.teacherCardUrl || ''; }
  get teacherCardDarkUrl():   string { return (this.teacher() as any)?.teacherCardDarkUrl || ''; }
  get activeTeacherCard():    string {
    if (this.isDark() && this.teacherCardDarkUrl) return this.teacherCardDarkUrl;
    if (this.teacherCardUrl) return this.teacherCardUrl;
    return '';
  }
  get teacherFacebook():      string { return (this.teacher() as any)?.facebookUrl || ''; }
  get teacherYoutube():       string { return (this.teacher() as any)?.youtubeUrl || ''; }
  get teacherInstagram():     string { return (this.teacher() as any)?.instagramUrl || ''; }
  get teacherTiktok():        string { return (this.teacher() as any)?.tiktokUrl || ''; }
  get teacherWhatsapp():      string { return (this.teacher() as any)?.whatsappNumber || ''; }
  get teacherTelegram():      string { return (this.teacher() as any)?.telegramUrl || ''; }
  get teacherHasSocials(): boolean {
    const t = this.teacher() as any;
    return !!(t?.facebookUrl || t?.youtubeUrl || t?.instagramUrl || t?.tiktokUrl || t?.whatsappNumber || t?.telegramUrl);
  }

  // ── Layout config helpers ─────────────────────────────────────
  private getWidget(id: string): any {
    const cfg = this.layoutConfig();
    if (!cfg || cfg.length === 0) return { enabled: true, widthPct: 50, heightPx: 0, rowSpan: 1, order: 99 };
    return cfg.find((w: any) => w.id === id) ?? { enabled: true, widthPct: 50, heightPx: 0, rowSpan: 1, order: 99 };
  }
  isEnabled(id: string): boolean { return this.getWidget(id)?.enabled !== false; }
  getOrder(id: string): number { return this.getWidget(id)?.order ?? 99; }
  widgetStyle(id: string): string {
    const w = this.getWidget(id);
    const pct = w?.widthPct ?? 50;
    const colMap: Record<number,number> = {25:3, 33:4, 50:6, 66:8, 75:9, 100:12};
    const closest = (Object.keys(colMap) as unknown as number[])
      .map(Number).reduce((a,b) => Math.abs(b-pct) < Math.abs(a-pct) ? b : a);
    const col    = `grid-column:span ${colMap[closest] ?? 6}`;
    const row    = (w?.rowSpan ?? 1) > 1 ? `grid-row:span ${w.rowSpan}` : '';
    const height = w?.heightPx ? `height:${w.heightPx}px;overflow:hidden` : '';
    const order  = `order:${w?.order ?? 99}`;
    return [col, row, order, height].filter(Boolean).join(';');
  }
  orderedWidgets(): string[] {
    const cfg = this.layoutConfig();
    if (!cfg || cfg.length === 0) return ['welcome','teacher_card','featured_courses','my_courses_link','my_progress','stats'];
    return [...cfg].sort((a: any, b: any) => a.order - b.order).map((w: any) => w.id);
  }

  get progressEnrollments(): any[] { return this.enrollments().slice(0, 3); }
  get myCoursesDisplay():    any[] { return this.enrollments().slice(0, 4); }

  courseTitle(e: any): string  { return e?.course?.title ?? e?.courseTitle ?? e?.title ?? 'كورس'; }
  courseTeacher(e: any): string { return e?.course?.teacherName ?? e?.teacherName ?? e?.course?.teacher?.fullName ?? ''; }
  enrollmentProgress(e: any): number { return Math.min(100, Math.round(e?.progress ?? e?.completionPercentage ?? 0)); }
  progressColor(i: number): string { return ['#183764','#4BBBA0','#F59239'][i % 3]; }
  courseGradient(i: number): string { return this.courseGradients[i % this.courseGradients.length]; }
  coursePrice(c: any): string {
    const p = c?.price;
    return p != null ? Number(p).toLocaleString('ar-EG') + ' EGP' : '';
  }

  courseOldPrice(c: any): string {
    const op = c?.discountedPrice;
    return op != null && op !== c?.price ? Number(op).toLocaleString('ar-EG') + ' EGP' : '';
  }

  openSupport()  { this.supportOpen.set(true); }
  closeSupport() { this.supportOpen.set(false); }

  get supportGroups(): { name: string; channels: any[] }[] {
    const map = new Map<string, any[]>();
    for (const ch of this.supportChannels()) {
      const g = ch.groupName || 'دعم';
      if (!map.has(g)) map.set(g, []);
      map.get(g)!.push(ch);
    }
    return [...map.entries()].map(([name, channels]) => ({ name, channels }));
  }

  channelUrl(ch: any): string {
    if (ch.type === 'WHATSAPP') return `https://wa.me/${ch.value.replace(/[^0-9]/g, '')}`;
    if (ch.type === 'TELEGRAM') return `https://t.me/${ch.value.replace('@', '')}`;
    return ch.value;
  }
}
