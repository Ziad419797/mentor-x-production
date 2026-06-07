import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Course, Session, Week, Quiz, QuizQuestion, QuizAttempt, QuizStatistics } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { CategoryStateService } from '../../services/category-state.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-quizzes',
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

      <!-- Quizzes List -->
      <div class="space-y-4">
        <div class="flex items-center justify-between">
          <h2 class="text-white font-bold text-xl">بنك الاختبارات والتقييمات</h2>
          <button (click)="openQuizModal()" [disabled]="selectedWeekId() === 0" class="btn-primary">
            <span class="material-icons-round">add</span>
            اختبار جديد
          </button>
        </div>

        <div class="edu-card p-0 overflow-hidden shadow-xl">
          <table class="edu-table">
            <thead>
              <tr>
                <th>عنوان الاختبار</th>
                <th>المدة</th>
                <th>درجة النجاح</th>
                <th>المحاولات</th>
                <th>الحالة</th>
                <th class="text-center">إجراءات</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let q of quizzes()">
                <td class="font-bold text-slate-200">{{ q.title }}</td>
                <td>{{ q.durationMinutes }} د</td>
                <td>{{ q.passingScore }}%</td>
                <td>{{ q.attemptsAllowed }}</td>
                <td>
                  <span class="bg-indigo-500/10 text-indigo-400 px-2 py-0.5 rounded-full font-bold text-xs border border-indigo-500/20">
                    نشط
                  </span>
                </td>
                <td>
                  <div class="flex items-center justify-center gap-2">
                    <button (click)="openStatsModal(q)" class="btn-icon text-emerald-400" title="إحصائيات">
                      <span class="material-icons-round text-sm">analytics</span>
                    </button>
                    <button (click)="openQuestionsModal(q)" class="btn-icon text-amber-400" title="إدارة الأسئلة">
                      <span class="material-icons-round text-sm">assignment</span>
                    </button>
                    <button (click)="deleteQuiz(q.id)" class="btn-icon text-red-400" title="حذف">
                      <span class="material-icons-round text-sm">delete</span>
                    </button>
                  </div>
                </td>
              </tr>
              <tr *ngIf="selectedWeekId() > 0 && quizzes().length === 0">
                <td colspan="6" class="text-center py-16 text-slate-500 italic">لا توجد اختبارات مضافة في هذا الدرس</td>
              </tr>
              <tr *ngIf="selectedWeekId() === 0">
                <td colspan="6" class="text-center py-20 text-slate-700 italic">يرجى اختيار المستوى، الوحدة، والدرس لعرض الاختبارات</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Create/Edit Quiz Modal -->
      <div *ngIf="showQuizModal()" class="modal-overlay">
        <div class="modal-box max-w-md">
          <div class="modal-header">
            <h3 class="text-white font-bold">إضافة كويز جديد</h3>
            <button (click)="showQuizModal.set(false)" class="btn-icon"><span class="material-icons-round">close</span></button>
          </div>
          <div class="modal-body space-y-4">
             <div>
               <label class="edu-label">عنوان الكويز *</label>
               <input type="text" [(ngModel)]="quizForm.title" class="edu-input" placeholder="مثال: اختبار الأسبوع الأول">
             </div>
             <div class="grid grid-cols-2 gap-4">
               <div>
                 <label class="edu-label">مدة الاختبار (بالدقائق)</label>
                 <input type="number" [(ngModel)]="quizForm.durationMinutes" class="edu-input" placeholder="30">
               </div>
               <div>
                 <label class="edu-label">درجة النجاح (%)</label>
                 <input type="number" [(ngModel)]="quizForm.passingScore" class="edu-input" placeholder="50">
               </div>
             </div>
             <div>
               <label class="edu-label">عدد المحاولات المسموحة للطلاب</label>
               <input type="number" [(ngModel)]="quizForm.attemptsAllowed" class="edu-input" placeholder="1">
             </div>
          </div>
          <div class="modal-footer">
            <button (click)="showQuizModal.set(false)" class="btn-secondary">إلغاء</button>
            <button (click)="saveQuiz()" [disabled]="!quizForm.title" class="btn-primary">حفظ الاختبار</button>
          </div>
        </div>
      </div>

      <!-- Manage Questions Modal -->
      <div *ngIf="showQuestionsModal()" class="modal-overlay">
        <div class="modal-box max-w-4xl h-[90vh] flex flex-col">
          <div class="modal-header">
             <div class="flex items-center gap-3">
               <div class="w-10 h-10 rounded-xl bg-amber-500/10 flex items-center justify-center text-amber-400">
                 <span class="material-icons-round">assignment</span>
               </div>
               <div>
                 <h3 class="text-white font-bold">إدارة أسئلة: {{ selectedQuiz()?.title }}</h3>
                 <span class="text-[10px] text-slate-500">إجمالي الأسئلة: {{ questions().length }}</span>
               </div>
             </div>
             <button (click)="showQuestionsModal.set(false)" class="btn-icon"><span class="material-icons-round">close</span></button>
          </div>
          
          <div class="modal-body flex-1 overflow-y-auto custom-scrollbar p-6 space-y-8">
             
             <!-- Add Question Form -->
             <div class="p-6 bg-slate-900/50 rounded-2xl border border-slate-700/50 space-y-6">
                <h4 class="text-slate-200 font-bold flex items-center gap-2">
                  <span class="w-2 h-6 bg-amber-500 rounded-full"></span>
                  إضافة سؤال جديد
                </h4>
                <div>
                  <label class="edu-label">نص السؤال</label>
                  <textarea [(ngModel)]="questionForm.questionText" class="edu-input min-h-[80px]" placeholder="اكتب نص السؤال هنا..."></textarea>
                </div>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div *ngFor="let choice of ['A','B','C','D']" class="relative">
                    <label class="edu-label">خيار {{ choice }}</label>
                    <input type="text" [(ngModel)]="questionForm.choices[choice]" class="edu-input" [placeholder]="'خيار ' + choice">
                    <input type="radio" name="correct" [value]="choice" [(ngModel)]="questionForm.correctAnswer" class="absolute left-3 bottom-3 scale-125 accent-emerald-500">
                  </div>
                </div>
                <div class="flex justify-end pt-2">
                   <button (click)="addQuestion()" [disabled]="!questionForm.questionText || !questionForm.correctAnswer" class="btn-primary bg-emerald-600 hover:bg-emerald-500">
                     <span class="material-icons-round">add</span>
                     إضافة سؤال MCQ
                   </button>
                </div>
             </div>

             <!-- Questions List -->
             <div class="space-y-4">
                <div class="flex items-center justify-between">
                  <h4 class="text-slate-400 font-bold text-sm">قائمة الأسئلة الحالية</h4>
                  <button (click)="deleteAllQuestions()" class="text-red-400 text-xs hover:underline flex items-center gap-1">
                    <span class="material-icons-round text-sm">delete_sweep</span>
                    حذف جميع الأسئلة
                  </button>
                </div>
                <div *ngFor="let q of questions(); let i = index" class="p-5 bg-slate-800/40 rounded-xl border border-slate-700 flex items-start justify-between group">
                   <div class="space-y-3">
                      <div class="flex items-center gap-2">
                        <span class="w-6 h-6 rounded bg-slate-800 flex items-center justify-center text-xs font-bold text-slate-500">{{ i + 1 }}</span>
                        <p class="text-slate-200 font-medium">{{ q.description }}</p>
                      </div>
                      <div class="grid grid-cols-2 gap-x-8 gap-y-1 pr-8">
                         <div *ngFor="let choice of ['A','B','C','D']" [class.text-emerald-400]="q.correctAnswer === choice" class="text-xs flex items-center gap-2">
                           <span class="w-4 h-4 rounded-full border border-slate-600 flex items-center justify-center text-[8px]" [class.bg-emerald-500]="q.correctAnswer === choice" [class.border-emerald-500]="q.correctAnswer === choice">
                             {{ choice }}
                           </span>
                           {{ getChoiceValue(q, choice) }}
                         </div>
                      </div>
                   </div>
                   <button (click)="deleteQuestion(q.id)" class="btn-icon text-red-500/50 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-all">
                     <span class="material-icons-round text-sm">delete</span>
                   </button>
                </div>
                <div *ngIf="questions().length === 0" class="py-10 text-center text-slate-600 border border-dashed border-slate-800 rounded-xl">
                   لا توجد أسئلة مضافة بعد
                </div>
             </div>
          </div>
        </div>
      </div>

      <!-- Quiz Statistics Modal -->
      <div *ngIf="showStatsModal()" class="modal-overlay">
        <div class="modal-box max-w-4xl h-[80vh] flex flex-col">
          <div class="modal-header">
             <div class="flex items-center gap-3">
               <div class="w-10 h-10 rounded-xl bg-emerald-500/10 flex items-center justify-center text-emerald-400">
                 <span class="material-icons-round">analytics</span>
               </div>
               <h3 class="text-white font-bold">إحصائيات: {{ selectedQuiz()?.title }}</h3>
             </div>
             <button (click)="showStatsModal.set(false)" class="btn-icon"><span class="material-icons-round">close</span></button>
          </div>
          <div class="modal-body flex-1 overflow-y-auto custom-scrollbar p-6">
             
             <!-- Stats Overview -->
             <div class="grid grid-cols-4 gap-4 mb-8">
                <div class="p-4 bg-slate-900/60 rounded-xl border border-slate-700">
                  <p class="text-slate-500 text-[10px] uppercase mb-1">إجمالي المحاولات</p>
                  <h5 class="text-xl font-bold text-white">{{ quizStats()?.totalAttempts }}</h5>
                </div>
                <div class="p-4 bg-emerald-500/5 rounded-xl border border-emerald-500/20">
                  <p class="text-emerald-500/70 text-[10px] uppercase mb-1">نسبة النجاح</p>
                  <h5 class="text-xl font-bold text-emerald-400">{{ (quizStats()?.passedCount || 0) / (quizStats()?.totalAttempts || 1) * 100 | number:'1.0-1' }}%</h5>
                </div>
                <div class="p-4 bg-slate-900/60 rounded-xl border border-slate-700">
                  <p class="text-slate-500 text-[10px] uppercase mb-1">متوسط الدرجات</p>
                  <h5 class="text-xl font-bold text-white">{{ quizStats()?.averageScore | number:'1.0-1' }}</h5>
                </div>
                <div class="p-4 bg-red-500/5 rounded-xl border border-red-500/20">
                  <p class="text-red-500/70 text-[10px] uppercase mb-1">حالات الرسوب</p>
                  <h5 class="text-xl font-bold text-red-400">{{ quizStats()?.failedCount }}</h5>
                </div>
             </div>

             <!-- Attempts Table -->
             <h4 class="text-slate-200 font-bold text-sm mb-4">محاولات الطلاب</h4>
             <div class="edu-card p-0 overflow-hidden border-slate-800">
               <table class="edu-table">
                 <thead>
                   <tr>
                     <th>الطالب</th>
                     <th>الدرجة</th>
                     <th>الوقت</th>
                     <th>النتيجة</th>
                     <th class="text-center">الإجراءات</th>
                   </tr>
                 </thead>
                 <tbody>
                   <tr *ngFor="let att of quizAttempts()">
                     <td>{{ att.studentName }}</td>
                     <td class="font-bold">{{ att.score }}</td>
                     <td class="text-xs">{{ att.timeTakenMinutes }} دقيقة</td>
                     <td>
                        <span [class]="att.passed ? 'text-emerald-400' : 'text-red-400'">{{ att.passed ? 'ناجح' : 'راسب' }}</span>
                     </td>
                     <td>
                        <div class="flex justify-center">
                          <button (click)="deleteAttempt(att.id)" class="btn-icon text-red-400" title="حذف المحاولة">
                            <span class="material-icons-round text-sm">delete</span>
                          </button>
                        </div>
                     </td>
                   </tr>
                 </tbody>
               </table>
             </div>
          </div>
        </div>
      </div>

    </div>
  `
})
export class QuizzesComponent implements OnInit, OnDestroy {
  courses = signal<Course[]>([]);
  sessions = signal<Session[]>([]);
  weeks = signal<Week[]>([]);
  quizzes = signal<Quiz[]>([]);

  selectedCourseId = signal(0);
  selectedSessionId = signal(0);
  selectedWeekId = signal(0);
  selectedQuiz = signal<Quiz | null>(null);

  showQuizModal = signal(false);
  quizForm: any = { title: '', durationMinutes: 30, passingScore: 50, attemptsAllowed: 1 };

  showQuestionsModal = signal(false);
  questions = signal<QuizQuestion[]>([]);
  questionForm: any = { questionText: '', choices: { A: '', B: '', C: '', D: '' }, correctAnswer: '' };

  showStatsModal = signal(false);
  quizStats = signal<QuizStatistics | null>(null);
  quizAttempts = signal<QuizAttempt[]>([]);

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
    this.quizzes.set([]);
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
    this.quizzes.set([]);
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
    this.loadQuizzes();
  }

  loadQuizzes() {
    if (this.selectedWeekId() > 0) {
      this.api.getQuizzesByWeek(this.selectedWeekId()).subscribe({ next: q => this.quizzes.set(q || []), error: () => this.quizzes.set([]) });
    }
  }

  openQuizModal() {
    this.quizForm = { title: '', durationMinutes: 30, passingScore: 50, attemptsAllowed: 1 };
    this.showQuizModal.set(true);
  }

  saveQuiz() {
    const data = {
      weekId: this.selectedWeekId(),
      title: this.quizForm.title,
      durationMinutes: this.quizForm.durationMinutes,
      passingScore: this.quizForm.passingScore,
      attemptsAllowed: this.quizForm.attemptsAllowed
    };
    this.api.createQuiz(data).subscribe({
      next: () => {
        this.toastr.success('تم إنشاء الاختبار بنجاح');
        this.showQuizModal.set(false);
        this.loadQuizzes();
      },
      error: (err) => {
        this.toastr.error(err?.error?.message || 'حدث خطأ أثناء إنشاء الاختبار');
      }
    });
  }

  deleteQuiz(id: number) {
    if (!confirm('هل أنت متأكد من حذف هذا الاختبار وجميع البيانات المرتبطة به؟')) return;
    this.api.deleteQuiz(id).subscribe({
      next: () => {
        this.toastr.success('تم الحذف');
        this.loadQuizzes();
      }
    });
  }

  // Question Management
  openQuestionsModal(quiz: Quiz) {
    this.selectedQuiz.set(quiz);
    this.loadQuestions(quiz.id);
    this.showQuestionsModal.set(true);
    this.questionForm = { questionText: '', choices: { A: '', B: '', C: '', D: '' }, correctAnswer: '' };
  }

  loadQuestions(quizId: number) {
    this.api.getQuizQuestions(quizId).subscribe({ next: q => this.questions.set(q || []), error: () => this.questions.set([]) });
  }

  addQuestion() {
    const quizId = this.selectedQuiz()!.id;
    const data = {
      description: this.questionForm.questionText,
      options: [
        this.questionForm.choices.A,
        this.questionForm.choices.B,
        this.questionForm.choices.C,
        this.questionForm.choices.D
      ].filter((o: string) => o?.trim()),
      correctAnswer: this.questionForm.correctAnswer,
      mark: 1
    };
    this.api.addQuizQuestion(quizId, data).subscribe({
      next: () => {
        this.toastr.success('تم إضافة السؤال');
        this.loadQuestions(quizId);
        this.questionForm = { questionText: '', choices: { A: '', B: '', C: '', D: '' }, correctAnswer: '' };
      },
      error: (err) => {
        this.toastr.error(err?.error?.message || 'حدث خطأ أثناء إضافة السؤال');
      }
    });
  }

  deleteQuestion(id: number) {
    this.api.deleteQuizQuestion(id).subscribe({ next: () => this.loadQuestions(this.selectedQuiz()!.id) });
  }

  deleteAllQuestions() {
    if (!confirm('هل أنت متأكد من حذف جميع الأسئلة؟')) return;
    this.api.deleteAllQuizQuestions(this.selectedQuiz()!.id).subscribe({ next: () => this.loadQuestions(this.selectedQuiz()!.id) });
  }

  getChoiceValue(q: QuizQuestion, choice: string): string {
    const idx = choice.charCodeAt(0) - 65; // A=0, B=1, ...
    return q.options?.[idx] || '';
  }

  // Statistics
  openStatsModal(quiz: Quiz) {
    this.selectedQuiz.set(quiz);
    this.api.getQuizStatistics(quiz.id).subscribe({ next: s => this.quizStats.set(s) });
    this.api.getQuizAttempts(quiz.id).subscribe({ next: a => this.quizAttempts.set(a || []) });
    this.showStatsModal.set(true);
  }
  deleteAttempt(attemptId: number) {
    if (!confirm('هل انت متاكد من حذف هذه المحاولة؟')) return;
    this.api.deleteQuizAttempt(attemptId).subscribe({
      next: () => {
        this.toastr.success('تم الحذف');
        const quiz = this.selectedQuiz();
        if (quiz) {
          this.api.getQuizAttempts(quiz.id).subscribe({ next: a => this.quizAttempts.set(a || []) });
        }
      },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

}