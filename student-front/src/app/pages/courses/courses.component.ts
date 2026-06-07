import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { StudentApiService } from '../../services/student-api.service';
import { switchMap, timeout, catchError } from 'rxjs/operators';
import { of, forkJoin } from 'rxjs';

@Component({
  selector: 'app-courses',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './courses.component.html'
})
export class CoursesComponent implements OnInit {
  loading      = signal(true);
  categories   = signal<any[]>([]);
  enrollments  = signal<Set<number>>(new Set());

  categoryState: Record<number, { expanded: boolean; courses: any[]; loading: boolean }> = {};

  // Course details modal
  activeCourse    = signal<any | null>(null);
  sessions        = signal<any[]>([]);
  loadingSessions = signal(false);

  // Session drill-down
  activeSession      = signal<any | null>(null);
  weekContents       = signal<{weekId:number;title:string;materials:any[];quizzes:any[];assignments:any[];loading:boolean}[]>([]);
  sessionAccessError = signal(false);
  weeksLoading       = signal(false);

  // Payment modal
  paymentCourse    = signal<any | null>(null);
  paymentStep      = signal<'choose' | 'wallet' | 'code' | 'online'>('choose');
  paymentLoading   = signal(false);
  paymentError     = signal('');
  paymentSuccess   = signal('');
  accessCodeInput  = signal('');
  walletBalanceVal = signal<number | null>(null);
  walletLoading    = signal(false);

  studentOnline: boolean | null = null;

  private gradients = [
    'linear-gradient(135deg,#183764,#0f4080)',
    'linear-gradient(135deg,#006b58,#004d3e)',
    'linear-gradient(135deg,#7c3d00,#562d00)',
    'linear-gradient(135deg,#4a1d96,#2e1065)',
    'linear-gradient(135deg,#9d174d,#831843)',
    'linear-gradient(135deg,#1e3a5f,#0f2040)',
  ];

  constructor(private api: StudentApiService, public router: Router) {}

  ngOnInit() {
    const safe = <T>(obs: any, fallback: T) =>
      obs.pipe(timeout({ each: 8000, with: () => of(fallback) }), catchError(() => of(fallback)));

    safe(this.api.getMe(), null).subscribe({
      next: (me: any) => {
        if (!me) { this.loading.set(false); return; }
        this.studentOnline = me.online ?? null;
        safe(this.api.getLevels(), []).subscribe({
          next: (lvls: any[]) => {
            const grade = me.grade ?? me.level?.name ?? '';
            const matched = (lvls || []).find((l: any) => l.name === grade);
            const levelId = matched?.id ?? lvls[0]?.id;
            if (levelId) this.loadCategories(levelId);
            else this.loading.set(false);
          },
          error: () => this.loading.set(false)
        });
      },
      error: () => this.loading.set(false)
    });

    this.api.getMyEnrollments().subscribe({
      next: enr => {
        const ids = new Set<number>((Array.isArray(enr) ? enr : []).map((e: any) => e.courseId ?? e.course?.id));
        this.enrollments.set(ids);
      }
    });
  }

  private savedExpandedIds(): number[] {
    try { return JSON.parse(sessionStorage.getItem('cat_expanded') || '[]'); } catch { return []; }
  }
  private saveExpandedState() {
    const ids = Object.entries(this.categoryState)
      .filter(([, s]) => s.expanded).map(([id]) => Number(id));
    sessionStorage.setItem('cat_expanded', JSON.stringify(ids));
  }

  loadCategories(levelId: number) {
    this.loading.set(true);
    const prevExpanded = new Set(this.savedExpandedIds());
    this.api.getCategoriesByLevel(levelId).subscribe({
      next: cats => {
        const list = Array.isArray(cats) ? cats : [];
        this.categories.set(list);
        list.forEach((c: any) => {
          this.categoryState[c.id] = { expanded: prevExpanded.has(c.id), courses: [], loading: false };
        });
        this.loading.set(false);
        // reload courses for previously expanded categories
        list.filter((c: any) => prevExpanded.has(c.id)).forEach((c: any) => {
          const st = this.categoryState[c.id];
          st.loading = true;
          this.api.getCoursesByCategory(c.id).subscribe({
            next: cs => {
              st.courses = (Array.isArray(cs) ? cs : []).sort((a: any, b: any) => {
                if (a.pinned && !b.pinned) return -1;
                if (!a.pinned && b.pinned) return 1;
                return (a.orderNumber ?? 0) - (b.orderNumber ?? 0);
              });
              st.loading = false;
            },
            error: () => { st.courses = []; st.loading = false; }
          });
        });
      },
      error: () => this.loading.set(false)
    });
  }

  toggleCategory(cat: any) {
    const state = this.categoryState[cat.id];
    if (!state) return;
    if (state.expanded) {
      state.expanded = false;
      this.saveExpandedState();
      return;
    }
    state.expanded = true;
    this.saveExpandedState();
    if (state.courses.length === 0 && !state.loading) {
      state.loading = true;
      this.api.getCoursesByCategory(cat.id).subscribe({
        next: cs => {
          const list = Array.isArray(cs) ? cs : [];
          state.courses = list.sort((a: any, b: any) => {
            if (a.pinned && !b.pinned) return -1;
            if (!a.pinned && b.pinned) return 1;
            return (a.orderNumber ?? 0) - (b.orderNumber ?? 0);
          });
          state.loading = false;
        },
        error: () => { state.courses = []; state.loading = false; }
      });
    }
  }

  // ── Course details modal ──────────────────────────────────────
  openDetails(c: any, ev: Event) {
    ev.stopPropagation();
    this.activeCourse.set(c);
    this.sessions.set([]);
    this.loadingSessions.set(true);
    this.api.getSessionsByCourse(c.id).subscribe({
      next: ss => {
        const all = Array.isArray(ss) ? ss : [];
        const filtered = all.filter((s: any) => {
          const t = s.teachingType;
          if (!t || t === 'BOTH') return true;
          if (this.studentOnline === true)  return t === 'ONLINE';
          if (this.studentOnline === false) return t === 'CENTER';
          return true;
        });
        this.sessions.set(filtered);
        this.loadingSessions.set(false);
      },
      error: () => { this.sessions.set([]); this.loadingSessions.set(false); }
    });
  }

  closeDetails() { this.activeCourse.set(null); this.activeSession.set(null); }
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

  // ── Payment modal ──────────────────────────────────────────────
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
      next: (r: any) => { this.walletBalanceVal.set(Number(r?.balance ?? r ?? 0)); this.walletLoading.set(false); },
      error: () => this.walletLoading.set(false)
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
        if (res?.success === false) { this.paymentError.set(res?.message || 'فشل الدفع'); return; }
        this._markEnrolled(course.id);
        this.paymentSuccess.set('تم الاشتراك بنجاح 🎉');
        setTimeout(() => this.closePayment(), 2000);
      },
      error: (err: any) => { this.paymentLoading.set(false); this.paymentError.set(err?.error?.message || 'حدث خطأ أثناء الدفع'); }
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
        if (!res?.success) { this.paymentError.set(res?.message || 'الكود غير صحيح'); return; }
        if (res?.enrollmentsCreated > 0) this._markEnrolled(course.id);
        this.paymentSuccess.set(res?.message || 'تم تفعيل الكود بنجاح 🎉');
        setTimeout(() => this.closePayment(), 2000);
      },
      error: (err: any) => { this.paymentLoading.set(false); this.paymentError.set(err?.error?.message || 'الكود غير صحيح أو منتهي الصلاحية'); }
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
        if (res?.redirectUrl) { window.location.href = res.redirectUrl; return; }
        if (res?.success) { this._markEnrolled(course.id); this.paymentSuccess.set('تم الاشتراك بنجاح 🎉'); setTimeout(() => this.closePayment(), 2000); }
        else this.paymentError.set(res?.message || 'فشل الاتصال ببوابة الدفع');
      },
      error: (err: any) => { this.paymentLoading.set(false); this.paymentError.set(err?.error?.message || 'فشل الاتصال ببوابة الدفع'); }
    });
  }

  private _markEnrolled(courseId: number) {
    const s = new Set(this.enrollments()); s.add(courseId);
    this.enrollments.set(s);
  }

  isEnrolled(id: number): boolean { return this.enrollments().has(id); }

  enrollFreeError = signal('');

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
    this.router.navigate(['/courses', course.id]);
  }

  gradient(i: number): string { return this.gradients[i % this.gradients.length]; }

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

  coursePrice(c: any): string {
    const p = c?.price;
    return p != null ? Number(p).toLocaleString('ar-EG') + ' EGP' : '';
  }

  courseOldPrice(c: any): string {
    const op = c?.discountedPrice;
    return op != null && op !== c?.price ? Number(op).toLocaleString('ar-EG') + ' EGP' : '';
  }
}
