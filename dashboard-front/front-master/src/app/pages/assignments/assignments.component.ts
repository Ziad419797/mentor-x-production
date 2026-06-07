import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Course, Session, Week, Assignment } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { CategoryStateService } from '../../services/category-state.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-assignments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">
      
      <!-- Filter Bar -->
      <div class="edu-card bg-slate-900/40 p-5 grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <label class="edu-label !text-[10px] uppercase tracking-wider text-slate-500">المستوى الدراسي</label>
          <select (change)="onCourseChange($event)" class="edu-select h-11" [ngModel]="selectedCourseId()">
            <option [value]="0">اختر المستوى</option>
            <option *ngFor="let c of courses()" [value]="c.id">{{ c.title }}</option>
          </select>
        </div>
        <div>
          <label class="edu-label !text-[10px] uppercase tracking-wider text-slate-500">الوحدة الدراسية</label>
          <select (change)="onSessionChange($event)" class="edu-select h-11" [disabled]="selectedCourseId() === 0" [ngModel]="selectedSessionId()">
            <option [value]="0">اختر الوحدة</option>
            <option *ngFor="let s of sessions()" [value]="s.id">{{ s.title }}</option>
          </select>
        </div>
        <div>
          <label class="edu-label !text-[10px] uppercase tracking-wider text-slate-500">الدرس التعليمي</label>
          <select (change)="onWeekChange($event)" class="edu-select h-11" [disabled]="selectedSessionId() === 0" [ngModel]="selectedWeekId()">
            <option [value]="0">اختر الدرس</option>
            <option *ngFor="let w of weeks()" [value]="w.id">{{ w.title }}</option>
          </select>
        </div>
      </div>

      <!-- Assignments List -->
      <div class="space-y-4">
        <div class="flex items-center justify-between">
          <h2 class="text-white font-bold text-xl">المهام والواجبات الدراسية</h2>
          <button (click)="openModal()" [disabled]="selectedWeekId() === 0" class="btn-primary">
            <span class="material-icons-round">add</span>
            إضافة تكليف
          </button>
        </div>

        <div class="grid grid-cols-1 gap-4">
          <div *ngFor="let a of assignments()" class="edu-card p-5 border-slate-800 flex items-center justify-between group bg-slate-900/60">
             <div class="flex items-center gap-5">
                <div class="w-12 h-12 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
                  <span class="material-icons-round">task_alt</span>
                </div>
                <div>
                   <h4 class="text-white font-bold text-base mb-1">{{ a.title }}</h4>
                   <div class="flex items-center gap-3">
                      <span class="text-[10px] text-slate-500 flex items-center gap-1">
                        <span class="material-icons-round text-xs">calendar_today</span>
                        {{ a.deadline | date:'mediumDate' }}
                      </span>
                      <span class="w-1 h-1 rounded-full bg-slate-700"></span>
                      <span class="text-[10px] text-indigo-400 font-bold">{{ a.submissionsCount || 0 }} تسليم</span>
                   </div>
                </div>
             </div>
             <div class="flex items-center gap-2">
                <button (click)="viewSubmissions(a)" class="btn-icon h-9 w-9 bg-slate-800 text-emerald-400" title="عرض التسليمات"><span class="material-icons-round text-sm">group</span></button>
                <button (click)="openModal(a)" class="btn-icon h-9 w-9 bg-slate-800 text-indigo-400"><span class="material-icons-round text-sm">edit</span></button>
                <button (click)="deleteAssignment(a.id)" class="btn-icon h-9 w-9 bg-slate-800 text-red-400"><span class="material-icons-round text-sm">delete</span></button>
             </div>
          </div>

          <div *ngIf="selectedWeekId() > 0 && assignments().length === 0" class="py-24 text-center text-slate-700 border-2 border-dashed border-slate-800 rounded-3xl">
             <span class="material-icons-round text-5xl mb-4 opacity-10">assignment</span>
             <p class="text-sm">لا توجد مهام مضافة لهذا الدرس</p>
          </div>
          <div *ngIf="selectedWeekId() === 0" class="py-24 text-center text-slate-700 border-2 border-dashed border-slate-800 rounded-3xl">
             <p class="text-sm">اختر المستوى والوحدة والدرس لعرض المهام</p>
          </div>
        </div>
      </div>

      <!-- Assignment Modal -->
      <div *ngIf="showModal()" class="modal-overlay">
         <div class="modal-box max-w-lg">
            <div class="modal-header">
               <h3 class="text-white font-bold">{{ editingAssignment() ? 'تعديل تكليف' : 'إضافة تكليف جديد' }}</h3>
               <button (click)="showModal.set(false)" class="btn-icon"><span class="material-icons-round">close</span></button>
            </div>
            <div class="modal-body space-y-4">
               <div>
                  <label class="edu-label">عنوان التكليف *</label>
                  <input type="text" [(ngModel)]="assignmentForm.title" class="edu-input" placeholder="مثال: واجب الدرس الثاني">
               </div>
               <div>
                  <label class="edu-label">الوصف والتعليمات</label>
                  <textarea [(ngModel)]="assignmentForm.description" class="edu-input min-h-[100px]" placeholder="اكتب تعليمات التكليف هنا..."></textarea>
               </div>
               <div>
                  <label class="edu-label">آخر موعد للتسليم</label>
                  <input type="date" [(ngModel)]="assignmentForm.deadline" class="edu-input">
               </div>
               <div>
                  <label class="edu-label">أسئلة التكليف *</label>
                  <div *ngFor="let q of assignmentForm.questions; let qi = index" class="p-3 bg-slate-900/60 rounded-xl border border-slate-800 space-y-2 mb-2">
                    <div class="flex items-center gap-2">
                      <input type="text" [(ngModel)]="assignmentForm.questions[qi].description" class="edu-input flex-1 text-sm" placeholder="نص السؤال">
                      <button type="button" (click)="removeQuestion(qi)" *ngIf="assignmentForm.questions.length > 1" class="btn-icon h-8 w-8 text-red-400"><span class="material-icons-round text-sm">remove_circle</span></button>
                    </div>
                    <div class="flex items-center gap-2">
                      <input type="text" [(ngModel)]="assignmentForm.questions[qi].correctAnswer" class="edu-input flex-1 text-xs" placeholder="الإجابة النموذجية">
                      <input type="number" [(ngModel)]="assignmentForm.questions[qi].mark" class="edu-input w-20 text-xs" placeholder="درجة">
                    </div>
                  </div>
                  <button type="button" (click)="addQuestion()" class="h-9 w-full rounded-xl border border-dashed border-slate-700 text-slate-500 hover:text-indigo-400 hover:border-indigo-500 text-xs transition-all">
                    <span class="material-icons-round text-sm align-middle">add</span> إضافة سؤال
                  </button>
               </div>
            </div>
            <div class="modal-footer">
               <button (click)="showModal.set(false)" class="btn-secondary">إلغاء</button>
               <button (click)="saveAssignment()" [disabled]="!assignmentForm.title" class="btn-primary px-10">حفظ</button>
            </div>
         </div>
      </div>

    </div>
  `
})
export class AssignmentsComponent implements OnInit, OnDestroy {
  courses = signal<Course[]>([]);
  sessions = signal<Session[]>([]);
  weeks = signal<Week[]>([]);
  assignments = signal<Assignment[]>([]);

  selectedCourseId = signal(0);
  selectedSessionId = signal(0);
  selectedWeekId = signal(0);

  showModal = signal(false);
  editingAssignment = signal<Assignment | null>(null);
  assignmentForm: any = { title: '', description: '', deadline: '', questions: [{ description: '', correctAnswer: '', mark: 5 }] };

  private categorySub?: Subscription;

  constructor(
    private api: ApiService,
    private toastr: ToastrService,
    public categoryState: CategoryStateService
  ) {}

  ngOnInit(): void {
    // 1. Sync with Stage
    this.categorySub = this.categoryState.selectedCategory$.subscribe(cat => {
      this.selectedCourseId.set(0);
      this.loadCourses(cat?.id ?? null);
    });

    // 2. Sync with Level (Course)
    this.categoryState.selectedCourse$.subscribe(course => {
      if (course && course.id !== this.selectedCourseId()) {
        this.selectedCourseId.set(course.id);
        this.loadSessions(course.id);
      }
    });

    // 3. Sync with Unit (Session)
    this.categoryState.selectedSession$.subscribe(session => {
      if (session && session.id !== this.selectedSessionId()) {
        this.selectedSessionId.set(session.id);
        this.loadWeeks(session.id);
      }
    });
  }

  loadCourses(categoryId: number | null) {
    this.api.getCourses(0, 100, categoryId).subscribe({
      next: (pg) => {
        this.courses.set(pg.content || []);
      }
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
    this.selectedWeekId.set(0);
    this.sessions.set([]);
    this.weeks.set([]);
    this.assignments.set([]);
    if (id > 0) this.loadSessions(id);
  }

  loadSessions(courseId: number) {
    this.api.getSessionsByCourse(courseId).subscribe({
      next: (s) => this.sessions.set(s || [])
    });
  }

  onSessionChange(event: any) {
    const id = Number(event.target.value);
    const session = this.sessions().find(s => s.id === id) || null;
    this.selectedSessionId.set(id);
    this.categoryState.selectSession(session);
    this.selectedWeekId.set(0);
    this.weeks.set([]);
    this.assignments.set([]);
    if (id > 0) this.loadWeeks(id);
  }

  loadWeeks(sessionId: number) {
    this.api.getWeeksBySession(sessionId).subscribe({
      next: (w) => this.weeks.set(w || [])
    });
  }

  onWeekChange(event: any) {
    const id = Number(event.target.value);
    this.selectedWeekId.set(id);
    this.loadAssignments();
  }

  loadAssignments() {
    if (this.selectedWeekId() > 0) {
      this.api.getAssignmentsByWeek(this.selectedWeekId()).subscribe({ next: a => this.assignments.set(a) });
    }
  }

  openModal(a?: Assignment) {
    if (a) {
      this.editingAssignment.set(a);
      this.assignmentForm = {
        title: a.title,
        description: a.description || '',
        deadline: a.deadline ? a.deadline.substring(0, 10) : '',
        questions: a.questions?.length ? a.questions.map(q => ({ description: q.description, correctAnswer: q.correctAnswer || '', mark: q.mark || 5 })) : [{ description: '', correctAnswer: '', mark: 5 }]
      };
    } else {
      this.editingAssignment.set(null);
      this.assignmentForm = { title: '', description: '', deadline: '', questions: [{ description: '', correctAnswer: '', mark: 5 }] };
    }
    this.showModal.set(true);
  }

  addQuestion() {
    this.assignmentForm.questions = [...this.assignmentForm.questions, { description: '', correctAnswer: '', mark: 5 }];
  }

  removeQuestion(index: number) {
    this.assignmentForm.questions = this.assignmentForm.questions.filter((_: any, i: number) => i !== index);
  }

  saveAssignment() {
    const payload: any = {
      title: this.assignmentForm.title,
      description: this.assignmentForm.description || undefined,
      deadline: this.assignmentForm.deadline || undefined,
      weekId: this.selectedWeekId(),
      questions: this.assignmentForm.questions.filter((q: any) => q.description)
    };

    const editing = this.editingAssignment();
    const obs = editing
      ? this.api.updateAssignment(editing.id, payload)
      : this.api.createAssignment(payload);

    obs.subscribe({
      next: () => {
        this.toastr.success('تم الحفظ بنجاح');
        this.showModal.set(false);
        this.loadAssignments();
      },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  deleteAssignment(id: number) {
    if (!confirm('هل أنت متأكد من الحذف؟')) return;
    this.api.deleteAssignment(id).subscribe({
      next: () => {
        this.toastr.success('تم الحذف');
        this.loadAssignments();
      },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  viewSubmissions(a: Assignment) {
    this.toastr.info('عرض تسليمات: ' + a.title);
  }
}
