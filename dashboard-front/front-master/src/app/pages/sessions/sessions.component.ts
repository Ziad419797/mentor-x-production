import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Course, Session, Week } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { CategoryStateService } from '../../services/category-state.service';
import { Subscription } from 'rxjs';
import { extractPage } from '../../core/api-response.model';

@Component({
  selector: 'app-sessions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sessions.component.html'
})
export class SessionsComponent implements OnInit, OnDestroy {
  courses = signal<Course[]>([]);
  sessions = signal<Session[]>([]);
  weeksMap = signal<Record<number, Week[]>>({});

  selectedCourseId = signal<number>(0);
  selectedSessionId = signal<number>(0);

  showSessionModal = signal(false);
  editingSession = signal<Session | null>(null);
  sessionForm: any = { title: '', orderNumber: 1 };

  showWeekModal = signal(false);
  editingWeek = signal<Week | null>(null);
  weekForm: any = { title: '', description: '', orderNumber: 1, lockType: 'NEVER' };

  showReorderModal = signal(false);
  reorderWeekId = signal<number>(0);
  reorderItems = signal<any[]>([]);
  dragIndex = signal<number>(-1);

  private categorySub?: Subscription;

  constructor(
    private api: ApiService,
    private toastr: ToastrService,
    public categoryState: CategoryStateService
  ) {}

  ngOnInit(): void {
    // 1. Listen to Category (Educational Stage)
    this.categorySub = this.categoryState.selectedCategory$.subscribe(cat => {
      this.selectedCourseId.set(0);
      this.categoryState.selectCourse(null);
      this.loadCourses(cat?.id ?? null);
    });

    // 2. Listen to Level (Course) from other pages
    this.categoryState.selectedCourse$.subscribe(course => {
      if (course && course.id !== this.selectedCourseId()) {
        this.selectedCourseId.set(course.id);
        this.loadSessions(course.id);
      }
    });
  }

  loadCourses(categoryId: number | null) {
    this.api.getCourses(0, 100, categoryId).subscribe({
      next: (pg) => {
        // api.getCourses already applies extractPage — pg is already PageResponse<Course>
        this.courses.set(pg.content || []);
      },
      error: () => this.courses.set([])
    });
  }

  ngOnDestroy(): void {
    this.categorySub?.unsubscribe();
  }

  onCourseChange(event: any) {
    const id = Number(event.target.value);
    const course = this.courses().find(c => c.id === id) || null;
    this.selectedCourseId.set(id);
    this.categoryState.selectCourse(course);
    this.selectedSessionId.set(0);
    this.weeksMap.set({});
    this.loadSessions(id);
  }

  loadSessions(courseId: number) {
    this.api.getSessionsByCourse(courseId).subscribe({
      next: (s) => {
        this.sessions.set(s || []);
        // Load weeks for each session
        (s || []).forEach(session => this.loadWeeks(session.id));
      },
      error: () => this.sessions.set([])
    });
  }

  loadWeeks(sessionId: number) {
    this.api.getWeeksBySession(sessionId).subscribe({
      next: (w) => {
        this.weeksMap.update(map => ({ ...map, [sessionId]: w || [] }));
      }
    });
  }

  getWeeksBySession(sessionId: number): Week[] {
    return this.weeksMap()[sessionId] || [];
  }

  selectSession(session: Session) {
    this.selectedSessionId.set(session.id);
    this.loadWeeks(session.id);
  }


  openSessionModal(s?: Session) {
    if (s) {
      this.editingSession.set(s);
      this.sessionForm = { title: s.title || '', orderNumber: s.orderNumber || 1 };
    } else {
      this.editingSession.set(null);
      this.sessionForm = { title: '', orderNumber: (this.sessions().length + 1) };
    }
    this.showSessionModal.set(true);
  }

  saveSession() {
    if (!this.sessionForm.title) return;
    const courseId = this.selectedCourseId();
    if (!courseId) return;
    const existing = this.editingSession();
    if (existing) {
      this.api.updateSession(existing.id, { ...this.sessionForm, courseId }).subscribe({
        next: updated => {
          this.sessions.update(list => list.map(s => s.id === existing.id ? { ...s, ...updated } : s));
          this.toastr.success('تم تعديل المحاضرة');
          this.showSessionModal.set(false);
        },
        error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
      });
    } else {
      this.api.createSession({ ...this.sessionForm, courseId }).subscribe({
        next: s => {
          this.sessions.update(list => [...list, s]);
          this.toastr.success('تم إضافة المحاضرة');
          this.showSessionModal.set(false);
        },
        error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
      });
    }
  }

  deleteSession(id: number) {
    if (!confirm('هل أنت متأكد من حذف هذه المحاضرة؟')) return;
    this.api.deleteSession(id).subscribe({
      next: () => {
        this.sessions.update(list => list.filter(s => s.id !== id));
        this.toastr.success('تم الحذف');
      },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  openWeekModal(sessionId: number, w?: Week) {
    this.selectedSessionId.set(sessionId);
    if (w) {
      this.editingWeek.set(w);
      this.weekForm = {
        title: w.title || '',
        description: w.description || '',
        orderNumber: w.orderNumber || 1,
        lockType: w.lockType || 'NEVER'
      };
    } else {
      this.editingWeek.set(null);
      const weeks = this.getWeeksBySession(sessionId);
      this.weekForm = { title: '', description: '', orderNumber: weeks.length + 1, lockType: 'NEVER' };
    }
    this.showWeekModal.set(true);
  }

  saveWeek() {
    if (!this.weekForm.title) return;
    const sessionId = this.selectedSessionId();
    const existing = this.editingWeek();
    if (existing) {
      this.api.updateWeek(existing.id, { ...this.weekForm, sessionIds: [sessionId] }).subscribe({
        next: updated => {
          this.weeksMap.update(map => ({
            ...map,
            [sessionId]: (map[sessionId] || []).map(w => w.id === existing.id ? { ...w, ...updated } : w)
          }));
          this.toastr.success('تم تعديل الدرس');
          this.showWeekModal.set(false);
        },
        error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
      });
    } else {
      this.api.createWeek({ ...this.weekForm, sessionIds: [sessionId] } as any).subscribe({
        next: w => {
          this.weeksMap.update(map => ({
            ...map,
            [sessionId]: [...(map[sessionId] || []), w]
          }));
          this.toastr.success('تم إضافة الدرس');
          this.showWeekModal.set(false);
        },
        error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
      });
    }
  }

  deleteWeek(id: number) {
    if (!confirm('هل أنت متأكد من حذف هذا الدرس؟')) return;
    this.api.deleteWeek(id).subscribe({
      next: () => {
        this.weeksMap.update(map => {
          const newMap = { ...map };
          for (const sid in newMap) {
            newMap[sid] = newMap[sid].filter(w => w.id !== id);
          }
          return newMap;
        });
        this.toastr.success('تم الحذف');
      },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  openReorderModal(weekId: number) {
    this.reorderWeekId.set(weekId);
    this.api.getMaterialsByWeek(weekId).subscribe({
      next: items => {
        this.reorderItems.set(items.map((m, i) => ({ ...m, _order: i })));
        this.showReorderModal.set(true);
      },
      error: () => this.toastr.error('حدث خطأ في تحميل المحتوى')
    });
  }

  moveReorderUp(i: number) {
    const list = [...this.reorderItems()];
    if (i === 0) return;
    [list[i - 1], list[i]] = [list[i], list[i - 1]];
    this.reorderItems.set(list);
  }

  moveReorderDown(i: number) {
    const list = [...this.reorderItems()];
    if (i >= list.length - 1) return;
    [list[i], list[i + 1]] = [list[i + 1], list[i]];
    this.reorderItems.set(list);
  }

  saveReorder() {
    const items = this.reorderItems().map((m, i) => ({ id: m.id, orderNumber: i + 1 }));
    this.api.reorderMaterials(items).subscribe({
      next: () => {
        this.toastr.success('تم حفظ الترتيب');
        this.showReorderModal.set(false);
        this.loadWeeks(this.reorderWeekId());
      },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  onDragStart(event: DragEvent, i: number) {
    this.dragIndex.set(i);
    event.dataTransfer?.setData('text/plain', String(i));
  }

  onDragOver(event: DragEvent, i: number) {
    event.preventDefault();
  }

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
    const map: Record<string, string> = {
      VIDEO: 'smart_display', YOUTUBE: 'smart_display',
      PDF: 'picture_as_pdf', IMAGE: 'image', AUDIO: 'headphones',
      DOC: 'description', PPT: 'slideshow', ARCHIVE: 'folder_zip'
    };
    return type ? (map[type] ?? 'insert_drive_file') : 'insert_drive_file';
  }
}
