import { Component, OnInit, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Session, Week, Material, Quiz, Assignment, QuizQuestion, AssignmentQuestion } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

type TabType = 'videos' | 'quizzes' | 'assignments' | 'materials';

interface QuestionDraft {
  description: string;
  options: string[];
  correctAnswer: string;
  mark: number;
  explanation: string;
  explanationUrl: string;
}

@Component({
  selector: 'app-session-content',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './session-content.component.html'
})
export class SessionContentComponent implements OnInit {
  sessionId    = signal(0);
  sessionTitle = signal('');
  courseId     = signal(0);

  loading  = signal(false);
  saving   = signal(false);

  activeTab = signal<TabType>('videos');

  // Data
  allMaterials   = signal<Material[]>([]);
  quizList       = signal<Quiz[]>([]);
  assignmentList = signal<Assignment[]>([]);
  defaultWeekId  = signal<number | null>(null);

  // Derived
  videos()        { return this.allMaterials().filter(m => m.materialType === 'VIDEO' || m.materialType === 'YOUTUBE'); }
  otherMaterials(){ return this.allMaterials().filter(m => m.materialType !== 'VIDEO' && m.materialType !== 'YOUTUBE'); }
  quizzes()       { return this.quizList(); }
  assignments()   { return this.assignmentList(); }

  tabCount(key: TabType): number {
    if (key === 'videos')      return this.videos().length;
    if (key === 'quizzes')     return this.quizzes().length;
    if (key === 'assignments') return this.assignments().length;
    if (key === 'materials')   return this.otherMaterials().length;
    return 0;
  }

  tabs = [
    { key: 'videos'      as TabType, label: 'فيديوهات', icon: 'smart_display' },
    { key: 'quizzes'     as TabType, label: 'امتحانات',  icon: 'quiz'          },
    { key: 'assignments' as TabType, label: 'واجبات',    icon: 'assignment'    },
    { key: 'materials'   as TabType, label: 'مواد',      icon: 'folder'        },
  ];

  // Modals
  showVideoModal      = signal(false);
  showQuizModal       = signal(false);
  showAssignmentModal = signal(false);
  showMaterialModal   = signal(false);

  // Preview Modals
  showQuizPreview        = signal(false);
  previewQuizData        = signal<Quiz | null>(null);
  previewQuizQuestions   = signal<QuizQuestion[]>([]);
  previewQuizLoading     = signal(false);
  previewQuizAnswers     = signal<Record<number, string>>({});
  previewQuizSubmitted   = signal(false);
  showAssignmentPreview  = signal(false);
  previewAssignmentData  = signal<Assignment | null>(null);
  previewAssignmentQs    = signal<AssignmentQuestion[]>([]);
  previewAssignmentLoading = signal(false);
  previewAssignmentAnswers = signal<Record<number, string>>({});
  previewAssignmentSubmitted = signal(false);
  showFilePreview       = signal(false);
  previewFileData       = signal<Material | null>(null);

  // Quiz Questions Management Modal
  showQuizQModal      = signal(false);
  activeQuiz          = signal<Quiz | null>(null);
  managedQuizQs       = signal<QuizQuestion[]>([]);
  loadingQuizQs       = signal(false);
  savingQuizQ         = signal(false);
  editingQuizQ        = signal<QuizQuestion | null>(null);
  quizQForm: { description: string; options: string[]; correctAnswer: string; mark: number; imageUrl: string } =
    { description: '', options: ['', '', '', ''], correctAnswer: '', mark: 1, imageUrl: '' };

  // Quiz Attempts Modal
  showQuizAttemptsModal = signal(false);
  quizAttempts          = signal<any[]>([]);
  loadingAttempts       = signal(false);
  activeQuizForAttempts = signal<Quiz | null>(null);

  // Assignment Questions Management Modal
  showAssignQModal    = signal(false);
  activeAssignment    = signal<Assignment | null>(null);
  managedAssignQs     = signal<AssignmentQuestion[]>([]);
  loadingAssignQs     = signal(false);
  savingAssignQ       = signal(false);
  editingAssignQ      = signal<AssignmentQuestion | null>(null);
  assignQForm: { description: string; options: string[]; correctAnswer: string; mark: number; imageUrl: string } =
    { description: '', options: ['', '', '', ''], correctAnswer: '', mark: 1, imageUrl: '' };

  // Assignment Attempts Modal
  showAssignAttemptsModal = signal(false);
  assignAttempts          = signal<any[]>([]);
  loadingAssignAttempts   = signal(false);
  activeAssignForAttempts = signal<Assignment | null>(null);

  videoForm = {
    title: '',
    iframeUrl: '',
    points: 0,
    maxViewCount: 3,
    cooldownMinutes: 60,
    durationMinutes: 0
  };

  quizForm: {
    title: string;
    durationMinutes: number;
    quizType: string;
    questionOrder: string;
    startDate: string;
    endDate: string;
    attemptsAllowed: number;
    points: number;
    prizeName: string;
    prizeScore: number;
    passingScore: number;
    improvable: boolean;
    questions: QuestionDraft[];
  } = {
    title: '',
    durationMinutes: 30,
    quizType: 'SESSION_QUIZ',
    questionOrder: 'FIXED',
    startDate: '',
    endDate: '',
    attemptsAllowed: 1,
    points: 0,
    prizeName: '',
    prizeScore: 0,
    passingScore: 50,
    improvable: false,
    questions: []
  };

  assignmentForm: { title: string; description: string; deadline: string; questions: { description: string; imageUrl: string; options: string[]; correctAnswer: string; mark: number }[] } = { title: '', description: '', deadline: '', questions: [] };
  materialForm   = { type: 'PDF', name: '', fileUrl: '' };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: ApiService,
    private toastr: ToastrService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
    const sid = Number(this.route.snapshot.paramMap.get('id') ?? this.route.snapshot.paramMap.get('sessionId'));
    this.sessionId.set(sid);
    this.loadContent();
  }

  loadContent() {
    this.loading.set(true);
    this.api.getWeeksBySession(this.sessionId()).subscribe({
      next: weeks => {
        if (weeks.length === 0) {
          this.api.createWeek({
            title: 'محتوى المحاضرة',
            sessionIds: [this.sessionId()],
            orderNumber: 1
          } as any).subscribe({
            next: w => {
              this.defaultWeekId.set(w.id);
              this.sessionTitle.set(this.route.snapshot.queryParamMap.get('title') ?? `محاضرة #${this.sessionId()}`);
              this.loading.set(false);
            },
            error: (err: any) => {
              this.toastr.error(err?.error?.message || 'فشل إنشاء الحصة');
              this.loading.set(false);
            }
          });
          return;
        }

        const week = weeks[0];
        this.defaultWeekId.set(week.id);
        this.sessionTitle.set(this.route.snapshot.queryParamMap.get('title') ?? week.title ?? `محاضرة #${this.sessionId()}`);

        forkJoin({
          materials:   this.api.getMaterialsByWeek(week.id).pipe(catchError(() => of([]))),
          quizzes:     this.api.getQuizzesByWeek(week.id).pipe(catchError(() => of([]))),
          assignments: this.api.getAssignmentsByWeek(week.id).pipe(catchError(() => of([])))
        }).subscribe({
          next: ({ materials, quizzes, assignments }) => {
            this.allMaterials.set(materials);
            this.quizList.set(quizzes);
            this.assignmentList.set(assignments);
            this.loading.set(false);
          },
          error: () => this.loading.set(false)
        });
      },
      error: () => this.loading.set(false)
    });
  }

  goBack() {
    const courseId = this.route.snapshot.queryParamMap.get('courseId');
    if (courseId) this.router.navigate(['/courses', courseId, 'sessions']);
    else this.router.navigate(['/courses']);
  }

  // ── Video Preview ──────────────────────────────────────────────────────────
  showPreviewModal = signal(false);
  previewMaterial  = signal<Material | null>(null);

  previewEmbedUrl(): SafeResourceUrl | null {
    const m = this.previewMaterial();
    if (!m) return null;

    let videoId = '';
    const url = m.fileUrl || m.youtubeVideoId || '';

    const embedMatch = url.match(/\/embed\/([^?&"]+)/);
    const watchMatch = url.match(/[?&]v=([^&]+)/);
    const shortMatch = url.match(/youtu\.be\/([^?]+)/);
    if (embedMatch) videoId = embedMatch[1];
    else if (watchMatch) videoId = watchMatch[1];
    else if (shortMatch) videoId = shortMatch[1];
    else videoId = url.trim();

    if (!videoId) return null;
    return this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://www.youtube.com/embed/${videoId}?autoplay=1&rel=0`
    );
  }

  previewVideo(m: Material) {
    window.open(`/video/${m.id}`, '_blank');
  }

  closePreview(event: MouseEvent) {
    // close when clicking the overlay backdrop
    this.showPreviewModal.set(false);
  }

  // ── Video ──────────────────────────────────────────────────────────────────
  openVideoModal() {
    this.videoForm = { title: '', iframeUrl: '', points: 0, maxViewCount: 3, cooldownMinutes: 60, durationMinutes: 0 };
    this.showVideoModal.set(true);
  }

  saveVideo() {
    const wid = this.defaultWeekId();
    if (!wid) { this.toastr.error('لم يتم تحميل الحصة بعد، أعد تحميل الصفحة'); return; }
    if (!this.videoForm.title || !this.videoForm.iframeUrl) return;
    this.saving.set(true);

    // Extract YouTube video ID from embed URL or plain URL
    let youtubeId = '';
    const embedMatch = this.videoForm.iframeUrl.match(/\/embed\/([^?&"]+)/);
    const watchMatch = this.videoForm.iframeUrl.match(/[?&]v=([^&]+)/);
    const shortMatch = this.videoForm.iframeUrl.match(/youtu\.be\/([^?]+)/);
    if (embedMatch) youtubeId = embedMatch[1];
    else if (watchMatch) youtubeId = watchMatch[1];
    else if (shortMatch) youtubeId = shortMatch[1];
    else youtubeId = this.videoForm.iframeUrl.trim();

    this.api.createMaterial({
      materialType: 'YOUTUBE',
      fileName: this.videoForm.title,
      fileUrl: this.videoForm.iframeUrl,
      durationSeconds: this.videoForm.durationMinutes ? this.videoForm.durationMinutes * 60 : undefined,
      studentPoints: this.videoForm.points || undefined,
      maxViewCount: this.videoForm.maxViewCount || undefined,
      cooldownMinutes: this.videoForm.cooldownMinutes || undefined,
      weekIds: [wid]
    } as any).subscribe({
      next: m => {
        this.allMaterials.set([...this.allMaterials(), m]);
        this.toastr.success('تم إضافة الفيديو');
        this.showVideoModal.set(false);
        this.saving.set(false);
      },
      error: (err: any) => { this.toastr.error(err?.error?.message || 'حدث خطأ'); this.saving.set(false); }
    });
  }

  deleteMaterial(m: Material) {
    if (!confirm(`حذف "${m.fileName}"؟`)) return;
    this.api.deleteMaterial(m.id).subscribe({
      next: () => {
        this.allMaterials.set(this.allMaterials().filter(x => x.id !== m.id));
        this.toastr.success('تم الحذف');
      },
      error: () => this.toastr.error('حدث خطأ')
    });
  }

  // ── Quiz ───────────────────────────────────────────────────────────────────
  openQuizModal() {
    this.quizForm = {
      title: '', durationMinutes: 30, quizType: 'SESSION_QUIZ',
      questionOrder: 'FIXED', startDate: '', endDate: '',
      attemptsAllowed: 1, points: 0, prizeName: '', prizeScore: 0,
      passingScore: 50, improvable: false, questions: []
    };
    this.showQuizModal.set(true);
  }

  addQuestion() {
    this.quizForm.questions.push({
      description: '',
      options: ['', '', '', ''],
      correctAnswer: '',
      mark: 1,
      explanation: '',
      explanationUrl: ''
    });
  }

  removeQuestion(index: number) {
    this.quizForm.questions.splice(index, 1);
  }

  saveQuiz() {
    const wid = this.defaultWeekId();
    if (!wid || !this.quizForm.title) return;
    this.saving.set(true);

    const payload: any = {
      title: this.quizForm.title,
      weekId: wid,
      durationMinutes: this.quizForm.durationMinutes || undefined,
      passingScore: this.quizForm.passingScore,
      attemptsAllowed: this.quizForm.attemptsAllowed,
      timeRestricted: !!this.quizForm.durationMinutes,
      quizType: this.quizForm.quizType,
      questionOrder: this.quizForm.questionOrder,
      points: this.quizForm.points,
      prizeName: this.quizForm.prizeName || undefined,
      prizeScore: this.quizForm.prizeScore || undefined,
      improvable: this.quizForm.improvable,
      startDate: this.quizForm.startDate || undefined,
      endDate: this.quizForm.endDate || undefined,
      questions: this.quizForm.questions
        .filter(q => q.description.trim() && q.correctAnswer)
        .map(q => ({
          description: q.description,
          options: q.options.filter(o => o.trim()),
          correctAnswer: q.correctAnswer,
          mark: q.mark,
          explanation: q.explanation || undefined,
          explanationUrl: q.explanationUrl || undefined
        }))
    };

    this.api.createQuiz(payload).subscribe({
      next: q => {
        this.quizList.set([...this.quizList(), q]);
        this.toastr.success('تم إضافة الامتحان');
        this.showQuizModal.set(false);
        this.saving.set(false);
      },
      error: (err: any) => { this.toastr.error(err?.error?.message || 'حدث خطأ'); this.saving.set(false); }
    });
  }

  deleteQuiz(q: Quiz) {
    if (!confirm(`حذف امتحان "${q.title}"؟`)) return;
    this.api.deleteQuiz(q.id).subscribe({
      next: () => {
        this.quizList.set(this.quizList().filter(x => x.id !== q.id));
        this.toastr.success('تم الحذف');
      },
      error: () => this.toastr.error('حدث خطأ')
    });
  }

  quizTypeLabel(t?: string): string {
    const map: Record<string, string> = {
      SESSION_QUIZ: 'محاضرة', COMPREHENSIVE: 'شامل', CUMULATIVE: 'تراكمي'
    };
    return t ? (map[t] ?? t) : '—';
  }

  // ── Assignment ─────────────────────────────────────────────────────────────
  openAssignmentModal() {
    this.assignmentForm = { title: '', description: '', deadline: '', questions: [] };
    this.showAssignmentModal.set(true);
  }

  addAssignmentQuestion() {
    this.assignmentForm.questions.push({ description: '', imageUrl: '', options: ['', ''], correctAnswer: '', mark: 1 });
  }

  removeAssignmentQuestion(index: number) {
    this.assignmentForm.questions.splice(index, 1);
  }

  saveAssignment() {
    const wid = this.defaultWeekId();
    if (!wid || !this.assignmentForm.title || !this.assignmentForm.deadline || this.assignmentForm.questions.length === 0) return;
    this.saving.set(true);

    this.api.createAssignment({
      title: this.assignmentForm.title,
      description: this.assignmentForm.description || undefined,
      deadline: this.assignmentForm.deadline,
      weekId: wid,
      questions: this.assignmentForm.questions as any
    }).subscribe({
      next: a => {
        this.assignmentList.set([...this.assignmentList(), a]);
        this.toastr.success('تم إضافة الواجب');
        this.showAssignmentModal.set(false);
        this.saving.set(false);
      },
      error: (err: any) => { this.toastr.error(err?.error?.message || 'حدث خطأ'); this.saving.set(false); }
    });
  }

  deleteAssignment(a: Assignment) {
    if (!confirm(`حذف واجب "${a.title}"؟`)) return;
    this.api.deleteAssignment(a.id).subscribe({
      next: () => {
        this.assignmentList.set(this.assignmentList().filter(x => x.id !== a.id));
        this.toastr.success('تم الحذف');
      },
      error: () => this.toastr.error('حدث خطأ')
    });
  }

  // ── Other Material ──────────────────────────────────────────────────────────
  openMaterialModal() {
    this.materialForm = { type: 'PDF', name: '', fileUrl: '' };
    this.showMaterialModal.set(true);
  }

  saveMaterial() {
    const wid = this.defaultWeekId();
    if (!wid || !this.materialForm.name || !this.materialForm.fileUrl) return;
    this.saving.set(true);

    this.api.createMaterial({
      materialType: this.materialForm.type as any,
      fileName: this.materialForm.name,
      fileUrl: this.materialForm.fileUrl,
      weekIds: [wid]
    } as any).subscribe({
      next: m => {
        this.allMaterials.set([...this.allMaterials(), m]);
        this.toastr.success('تم إضافة المادة');
        this.showMaterialModal.set(false);
        this.saving.set(false);
      },
      error: (err: any) => { this.toastr.error(err?.error?.message || 'حدث خطأ'); this.saving.set(false); }
    });
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────
  formatDuration(seconds?: number): string {
    if (!seconds) return '—';
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return m > 0 ? `${m} دقيقة${s ? ' ' + s + ' ث' : ''}` : `${s} ثانية`;
  }

  formatSize(bytes: number): string {
    if (bytes >= 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    if (bytes >= 1024) return (bytes / 1024).toFixed(0) + ' KB';
    return bytes + ' B';
  }

  formatDate(d?: string): string {
    if (!d) return '—';
    try {
      return new Date(d).toLocaleDateString('ar-EG', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch { return '—'; }
  }

  materialIcon(type?: string): string {
    const map: Record<string, string> = {
      PDF: 'picture_as_pdf', IMAGE: 'image', AUDIO: 'headphones',
      DOC: 'description', PPT: 'slideshow', ARCHIVE: 'folder_zip', OTHER: 'insert_drive_file'
    };
    return type ? (map[type] ?? 'insert_drive_file') : 'insert_drive_file';
  }

  // ── Preview Methods ─────────────────────────────────────────────────────────
  previewQuiz(q: Quiz) {
    this.previewQuizData.set(q);
    this.previewQuizQuestions.set([]);
    this.previewQuizAnswers.set({});
    this.previewQuizSubmitted.set(false);
    this.showQuizPreview.set(true);
    this.previewQuizLoading.set(true);
    this.api.getQuizQuestions(q.id).subscribe({
      next: qs => { this.previewQuizQuestions.set(qs); this.previewQuizLoading.set(false); },
      error: () => this.previewQuizLoading.set(false)
    });
  }

  selectPreviewQuizAnswer(questionId: number, option: string) {
    if (this.previewQuizSubmitted()) return;
    this.previewQuizAnswers.update(a => ({ ...a, [questionId]: option }));
  }


  getPreviewQuizScore(): { correct: number; total: number; percent: number } {
    const qs = this.previewQuizQuestions();
    const ans = this.previewQuizAnswers();
    const correct = qs.filter(q => ans[q.id] === q.correctAnswer).length;
    return { correct, total: qs.length, percent: qs.length ? Math.round(correct / qs.length * 100) : 0 };
  }

  previewAssignment(a: Assignment) {
    this.previewAssignmentData.set(a);
    this.previewAssignmentQs.set([]);
    this.previewAssignmentAnswers.set({});
    this.previewAssignmentSubmitted.set(false);
    this.showAssignmentPreview.set(true);
    this.previewAssignmentLoading.set(true);
    this.api.getAssignmentQuestions(a.id).subscribe({
      next: qs => { this.previewAssignmentQs.set(qs); this.previewAssignmentLoading.set(false); },
      error: () => this.previewAssignmentLoading.set(false)
    });
  }

  selectPreviewAssignmentAnswer(questionId: number, option: string) {
    if (this.previewAssignmentSubmitted()) return;
    this.previewAssignmentAnswers.update(a => ({ ...a, [questionId]: option }));
  }

  submitPreviewAssignment() {
    const qs = this.previewAssignmentQs();
    const ans = this.previewAssignmentAnswers();
    const unanswered = qs.filter(q => !ans[q.id]);
    if (unanswered.length > 0) {
      this.toastr.warning('يجب الإجابة على جميع الأسئلة أولاً');
      return;
    }
    this.previewAssignmentSubmitted.set(true);
  }

  submitPreviewQuiz() {
    const qs = this.previewQuizQuestions();
    const ans = this.previewQuizAnswers();
    const unanswered = qs.filter(q => !ans[q.id]);
    if (unanswered.length > 0) {
      this.toastr.warning('يجب الإجابة على جميع الأسئلة أولاً');
      return;
    }
    this.previewQuizSubmitted.set(true);
  }

  getPreviewAssignmentScore(): { correct: number; total: number; percent: number } {
    const qs = this.previewAssignmentQs();
    const ans = this.previewAssignmentAnswers();
    const correct = qs.filter(q => ans[q.id] === q.correctAnswer).length;
    return { correct, total: qs.length, percent: qs.length ? Math.round(correct / qs.length * 100) : 0 };
  }

  previewFile(m: Material) {
    this.previewFileData.set(m);
    this.showFilePreview.set(true);
  }

  // ── Quiz Questions Management ────────────────────────────────────────────────
  openQuizQModal(quiz: Quiz) {
    this.activeQuiz.set(quiz);
    this.managedQuizQs.set([]);
    this.editingQuizQ.set(null);
    this.resetQuizQForm();
    this.showQuizQModal.set(true);
    this.loadingQuizQs.set(true);
    this.api.getQuizQuestions(quiz.id).subscribe({
      next: qs => { this.managedQuizQs.set(qs); this.loadingQuizQs.set(false); },
      error: () => this.loadingQuizQs.set(false)
    });
  }

  resetQuizQForm() {
    this.quizQForm = { description: '', options: ['', '', '', ''], correctAnswer: '', mark: 1, imageUrl: '' };
  }

  startEditQuizQ(q: QuizQuestion) {
    this.editingQuizQ.set(q);
    this.quizQForm = { description: q.description, options: [...(q.options || ['','','',''])], correctAnswer: q.correctAnswer, mark: q.mark ?? 1, imageUrl: q.imageUrl ?? '' };
  }

  cancelEditQuizQ() { this.editingQuizQ.set(null); this.resetQuizQForm(); }

  saveQuizQ() {
    const quiz = this.activeQuiz();
    if (!quiz || !this.quizQForm.description.trim() || !this.quizQForm.correctAnswer) return;
    this.savingQuizQ.set(true);
    const opts = this.quizQForm.options.filter(o => o.trim());
    const data: any = { description: this.quizQForm.description, options: opts, correctAnswer: this.quizQForm.correctAnswer, mark: this.quizQForm.mark, imageUrl: this.quizQForm.imageUrl || undefined };
    const editing = this.editingQuizQ();
    if (editing) {
      this.api.updateQuizQuestion(editing.id, data).subscribe({
        next: updated => { this.managedQuizQs.set(this.managedQuizQs().map(q => q.id === updated.id ? updated : q)); this.cancelEditQuizQ(); this.savingQuizQ.set(false); this.toastr.success('تم تعديل السؤال'); },
        error: () => { this.toastr.error('خطأ في التعديل'); this.savingQuizQ.set(false); }
      });
    } else {
      this.api.addQuizQuestion(quiz.id, data).subscribe({
        next: created => {
          this.managedQuizQs.set([...this.managedQuizQs(), created]);
          this.quizList.set(this.quizList().map(q => q.id === quiz.id ? { ...q, questionsCount: (q.questionsCount ?? 0) + 1 } : q));
          this.resetQuizQForm(); this.savingQuizQ.set(false); this.toastr.success('تم إضافة السؤال');
        },
        error: () => { this.toastr.error('خطأ في الإضافة'); this.savingQuizQ.set(false); }
      });
    }
  }

  deleteQuizQ(q: QuizQuestion) {
    if (!confirm('حذف هذا السؤال؟')) return;
    this.api.deleteQuizQuestion(q.id).subscribe({
      next: () => {
        this.managedQuizQs.set(this.managedQuizQs().filter(x => x.id !== q.id));
        const quiz = this.activeQuiz();
        if (quiz) this.quizList.set(this.quizList().map(x => x.id === quiz.id ? { ...x, questionsCount: Math.max(0, (x.questionsCount ?? 1) - 1) } : x));
        this.toastr.success('تم حذف السؤال');
      },
      error: () => this.toastr.error('خطأ في الحذف')
    });
  }

  // ── Quiz Attempts ────────────────────────────────────────────────────────────
  openQuizAttempts(quiz: Quiz) {
    this.activeQuizForAttempts.set(quiz);
    this.quizAttempts.set([]);
    this.loadingAttempts.set(true);
    this.showQuizAttemptsModal.set(true);
    this.api.getQuizAttempts(quiz.id).subscribe({
      next: (list: any[]) => { this.quizAttempts.set(list); this.loadingAttempts.set(false); },
      error: () => this.loadingAttempts.set(false)
    });
  }

  openEditQuiz(quiz: Quiz) {
    this.quizForm = {
      title: quiz.title,
      durationMinutes: quiz.durationMinutes ?? 30,
      quizType: quiz.quizType ?? 'SESSION_QUIZ',
      questionOrder: (quiz as any).questionOrder ?? 'FIXED',
      startDate: quiz.startDate ?? '',
      endDate: quiz.endDate ?? '',
      attemptsAllowed: (quiz as any).attemptsAllowed ?? 1,
      points: quiz.points ?? 0,
      prizeName: (quiz as any).prizeName ?? '',
      prizeScore: (quiz as any).prizeScore ?? 0,
      passingScore: quiz.passingScore ?? 50,
      improvable: (quiz as any).improvable ?? false,
      questions: []
    };
    this.showQuizModal.set(true);
  }

  // ── Assignment Questions Management ──────────────────────────────────────────
  openAssignQModal(assignment: Assignment) {
    this.activeAssignment.set(assignment);
    this.managedAssignQs.set([]);
    this.editingAssignQ.set(null);
    this.resetAssignQForm();
    this.showAssignQModal.set(true);
    this.loadingAssignQs.set(true);
    this.api.getAssignmentQuestions(assignment.id).subscribe({
      next: qs => { this.managedAssignQs.set(qs); this.loadingAssignQs.set(false); },
      error: () => this.loadingAssignQs.set(false)
    });
  }

  resetAssignQForm() {
    this.assignQForm = { description: '', options: ['', '', '', ''], correctAnswer: '', mark: 1, imageUrl: '' };
  }

  startEditAssignQ(q: AssignmentQuestion) {
    this.editingAssignQ.set(q);
    this.assignQForm = { description: q.description, options: [...((q as any).options || ['','','',''])], correctAnswer: q.correctAnswer ?? '', mark: q.mark ?? 1, imageUrl: (q as any).imageUrl ?? '' };
  }

  cancelEditAssignQ() { this.editingAssignQ.set(null); this.resetAssignQForm(); }

  saveAssignQ() {
    const a = this.activeAssignment();
    if (!a || !this.assignQForm.description.trim()) return;
    this.savingAssignQ.set(true);
    const opts = this.assignQForm.options.filter(o => o.trim());
    const data: any = { assignmentId: a.id, description: this.assignQForm.description, options: opts, correctAnswer: this.assignQForm.correctAnswer, mark: this.assignQForm.mark, imageUrl: this.assignQForm.imageUrl || undefined };
    const editing = this.editingAssignQ();
    if (editing) {
      this.api.updateAssignmentQuestion(editing.id, data).subscribe({
        next: updated => { this.managedAssignQs.set(this.managedAssignQs().map(q => q.id === updated.id ? updated : q)); this.cancelEditAssignQ(); this.savingAssignQ.set(false); this.toastr.success('تم تعديل السؤال'); },
        error: () => { this.toastr.error('خطأ في التعديل'); this.savingAssignQ.set(false); }
      });
    } else {
      this.api.addAssignmentQuestion(data).subscribe({
        next: created => { this.managedAssignQs.set([...this.managedAssignQs(), created]); this.resetAssignQForm(); this.savingAssignQ.set(false); this.toastr.success('تم إضافة السؤال'); },
        error: () => { this.toastr.error('خطأ في الإضافة'); this.savingAssignQ.set(false); }
      });
    }
  }

  deleteAssignQ(q: AssignmentQuestion) {
    if (!confirm('حذف هذا السؤال؟')) return;
    this.api.deleteAssignmentQuestion(q.id).subscribe({
      next: () => { this.managedAssignQs.set(this.managedAssignQs().filter(x => x.id !== q.id)); this.toastr.success('تم حذف السؤال'); },
      error: () => this.toastr.error('خطأ في الحذف')
    });
  }

  openAssignAttempts(a: Assignment) {
    this.activeAssignForAttempts.set(a);
    this.assignAttempts.set([]);
    this.loadingAssignAttempts.set(true);
    this.showAssignAttemptsModal.set(true);
    this.api.getAssignmentAttempts(a.id).subscribe({
      next: (list: any[]) => { this.assignAttempts.set(list); this.loadingAssignAttempts.set(false); },
      error: () => this.loadingAssignAttempts.set(false)
    });
  }

  openEditAssignment(a: Assignment) {
    this.assignmentForm = { title: a.title, description: a.description ?? '', deadline: a.deadline ?? '', questions: [] };
    this.showAssignmentModal.set(true);
  }

  trackById(_: number, item: { id: number }) { return item.id; }
  trackByIndex(i: number) { return i; }

  getNonEmptyOptions(opts: string[]): string[] {
    return (opts || []).filter(o => o && o.trim());
  }
}
