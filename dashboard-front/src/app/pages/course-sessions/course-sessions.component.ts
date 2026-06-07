import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Course, Session } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-course-sessions',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './course-sessions.component.html'
})
export class CourseSessionsComponent implements OnInit {
  courseId    = signal(0);
  levelId     = signal<number | null>(null);
  courseTitle = signal('');
  sessions    = signal<Session[]>([]);
  loading     = signal(false);
  saving      = signal(false);
  linking     = signal(false);
  showModal   = signal(false);
  showLinkModal = signal(false);
  editingSession = signal<Session | null>(null);

  // Reorder
  showReorderModal   = signal(false);
  reorderItems       = signal<any[]>([]);
  reorderSessionTitle = signal('');
  loadingReorder     = signal(false);
  savingReorder      = signal(false);
  dragIndex          = signal(-1);

  // For link-existing modal
  allSessions            = signal<Session[]>([]);
  filteredAvailableSessions = signal<Session[]>([]);
  selectedLinkSession    = signal<Session | null>(null);
  loadingAllSessions     = signal(false);
  linkSearchQuery        = '';

  modalForm = { title: '', description: '', orderNumber: 1, teachingType: '' as '' | 'ONLINE' | 'CENTER' | 'BOTH' };
  modalErrors = { title: false, teachingType: false };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: ApiService,
    private toastr: ToastrService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('courseId'));
    this.courseId.set(id);
    const lvl = this.route.snapshot.queryParamMap.get('levelId');
    if (lvl) {
      this.levelId.set(Number(lvl));
    } else {
      // Auto-detect level from course categories
      this.api.getCourseLevelId(id).subscribe({
        next: (r: any) => { if (r?.levelId) this.levelId.set(r.levelId); },
        error: () => {}
      });
    }

    this.api.getCourses(0, 200).subscribe({
      next: page => {
        const c = (page.content || []).find(x => x.id === id);
        if (c) this.courseTitle.set(c.title);
      }
    });

    this.loadSessions();
  }

  loadSessions() {
    this.loading.set(true);
    this.api.getSessionsByCourse(this.courseId()).subscribe({
      next: list => { this.sessions.set(list); this.loading.set(false); },
      error: () => { this.sessions.set([]); this.loading.set(false); }
    });
  }

  goBack() { this.router.navigate(['/courses']); }

  openAddModal() {
    this.editingSession.set(null);
    this.modalForm = { title: '', description: '', orderNumber: this.sessions().length + 1, teachingType: '' };
    this.modalErrors = { title: false, teachingType: false };
    this.showModal.set(true);
  }

  openEditModal(session: Session) {
    this.editingSession.set(session);
    this.modalForm = {
      title: session.title,
      description: session.description ?? '',
      orderNumber: session.orderNumber ?? 1,
      teachingType: (session as any).teachingType ?? ''
    };
    this.modalErrors = { title: false, teachingType: false };
    this.showModal.set(true);
  }

  openLinkModal() {
    this.selectedLinkSession.set(null);
    this.linkSearchQuery = '';
    this.showLinkModal.set(true);
    this.loadingAllSessions.set(true);

    const lvl = this.levelId();
    const fetch$ = lvl
      ? this.api.getSessionsByLevel(lvl)
      : this.api.getAllSessions();

    fetch$.subscribe({
      next: all => {
        const linkedIds = new Set(this.sessions().map(s => s.id));
        const available = (all as any[]).filter((s: any) => !linkedIds.has(s.id));
        this.allSessions.set(available);
        this.filteredAvailableSessions.set(available);
        this.loadingAllSessions.set(false);
      },
      error: () => {
        this.allSessions.set([]);
        this.filteredAvailableSessions.set([]);
        this.loadingAllSessions.set(false);
        this.toastr.error('تعذر تحميل المحاضرات');
      }
    });
  }

  filterAvailableSessions() {
    const q = this.linkSearchQuery.trim().toLowerCase();
    if (!q) {
      this.filteredAvailableSessions.set(this.allSessions());
    } else {
      this.filteredAvailableSessions.set(
        this.allSessions().filter(s => s.title?.toLowerCase().includes(q))
      );
    }
  }

  saveSession() {
    this.modalErrors.title        = !this.modalForm.title.trim();
    this.modalErrors.teachingType = !this.modalForm.teachingType;
    if (this.modalErrors.title || this.modalErrors.teachingType) return;
    this.saving.set(true);

    const editing = this.editingSession();
    if (editing) {
      this.api.updateSession(editing.id, {
        title: this.modalForm.title,
        description: this.modalForm.description,
        orderNumber: this.modalForm.orderNumber,
        teachingType: this.modalForm.teachingType || undefined
      }).subscribe({
        next: updated => {
          this.sessions.set(this.sessions().map(s => s.id === updated.id ? updated : s));
          this.toastr.success('تم تعديل المحاضرة');
          this.showModal.set(false);
          this.saving.set(false);
        },
        error: (err: any) => { this.toastr.error(err?.error?.message || 'حدث خطأ'); this.saving.set(false); }
      });
    } else {
      this.api.createSession({
        title: this.modalForm.title,
        description: this.modalForm.description,
        orderNumber: this.modalForm.orderNumber,
        teachingType: this.modalForm.teachingType || undefined,
        courseIds: [this.courseId()]
      }).subscribe({
        next: created => {
          this.sessions.set([...this.sessions(), created]);
          this.toastr.success('تم إضافة المحاضرة');
          this.showModal.set(false);
          this.saving.set(false);
        },
        error: (err: any) => { this.toastr.error(err?.error?.message || 'حدث خطأ'); this.saving.set(false); }
      });
    }
  }

  linkSession() {
    const s = this.selectedLinkSession();
    if (!s) return;
    this.linking.set(true);
    this.api.linkSessionToCourse(s.id, this.courseId()).subscribe({
      next: linked => {
        this.sessions.set([...this.sessions(), linked]);
        this.toastr.success('تم إضافة المحاضرة للكورس');
        this.showLinkModal.set(false);
        this.linking.set(false);
      },
      error: (err: any) => {
        this.toastr.error(err?.error?.message || 'حدث خطأ');
        this.linking.set(false);
      }
    });
  }

  toggleStatus(session: Session) {
    this.api.toggleSessionStatus(session.id).subscribe({
      next: () => {
        this.sessions.set(this.sessions().map(s =>
          s.id === session.id ? { ...s, active: !s.active } : s
        ));
        this.toastr.success(session.active !== false ? 'تم إيقاف المحاضرة' : 'تم نشر المحاضرة');
      },
      error: () => this.toastr.error('حدث خطأ')
    });
  }

  deleteSession(session: Session) {
    if (!confirm('هل أنت متأكد من حذف المحاضرة "' + session.title + '"؟')) return;
    this.api.deleteSession(session.id).subscribe({
      next: () => {
        this.sessions.set(this.sessions().filter(s => s.id !== session.id));
        this.toastr.success('تم حذف المحاضرة');
      },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  openReorderModal(session: any) {
    this.reorderSessionTitle.set(session.title);
    this.showReorderModal.set(true);
    this.loadingReorder.set(true);
    this.reorderItems.set([]);
    this.api.getWeeksBySession(session.id).subscribe({
      next: (weeks: any[]) => {
        if (!weeks || weeks.length === 0) { this.loadingReorder.set(false); return; }
        const weekId = weeks[0].id;
        forkJoin({
          materials:   this.api.getMaterialsByWeek(weekId).pipe(catchError(() => of([]))),
          quizzes:     this.api.getQuizzesByWeek(weekId).pipe(catchError(() => of([]))),
          assignments: this.api.getAssignmentsByWeek(weekId).pipe(catchError(() => of([])))
        }).subscribe(({ materials, quizzes, assignments }) => {
          const mats = (materials as any[]).map(m => ({ ...m, _type: 'MATERIAL', _label: m.fileName || m.title || 'ملف' }));
          const qzs  = (quizzes  as any[]).map(q => ({ ...q, _type: 'QUIZ',     _label: q.title || 'كويز', materialType: 'QUIZ' }));
          const asgs = (assignments as any[]).map(a => ({ ...a, _type: 'ASSIGNMENT', _label: a.title || 'واجب', materialType: 'ASSIGNMENT' }));
          const all  = [...mats, ...qzs, ...asgs].sort((a, b) => (a.orderNumber ?? 0) - (b.orderNumber ?? 0));
          this.reorderItems.set(all);
          this.loadingReorder.set(false);
        });
      },
      error: () => { this.loadingReorder.set(false); this.toastr.error('خطأ في تحميل المحتوى'); }
    });
  }

  onDragStart(event: DragEvent, i: number) {
    this.dragIndex.set(i);
    event.dataTransfer?.setData('text/plain', String(i));
  }

  onDragOver(event: DragEvent, _i: number) { event.preventDefault(); }

  onDrop(event: DragEvent, i: number) {
    event.preventDefault();
    const from = this.dragIndex();
    if (from === i || from < 0) return;
    const list = [...this.reorderItems()];
    const [moved] = list.splice(from, 1);
    list.splice(i, 0, moved);
    this.reorderItems.set(list);
    this.dragIndex.set(-1);
  }

  getMaterialIcon(type?: string): string {
    if (!type) return 'description';
    const t = type.toUpperCase();
    if (t === 'QUIZ') return 'quiz';
    if (t === 'ASSIGNMENT') return 'assignment';
    if (t.includes('PDF')) return 'picture_as_pdf';
    if (t.includes('VIDEO')) return 'videocam';
    if (t.includes('IMAGE')) return 'image';
    return 'description';
  }

  saveReorder() {
    this.savingReorder.set(true);
    const items = this.reorderItems().map((m, i) => ({ id: m.id, orderNumber: i + 1 }));
    this.api.reorderMaterials(items).subscribe({
      next: () => {
        this.toastr.success('تم حفظ الترتيب');
        this.showReorderModal.set(false);
        this.savingReorder.set(false);
      },
      error: () => { this.toastr.error('خطأ في حفظ الترتيب'); this.savingReorder.set(false); }
    });
  }

  // ── قائمة الطلبة / الحضور: التنقل لصفحة الحضور الكاملة ────────
  goToAttendance(session: any) {
    this.router.navigate(['/sessions', session.id, 'attendance'], {
      queryParams: { title: session.title, type: session.teachingType ?? '' }
    });
  }

  trackById(_: number, item: { id: number }) { return item.id; }
}
