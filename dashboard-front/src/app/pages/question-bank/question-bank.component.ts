import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { QuestionBankItem } from '../../models/models';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-question-bank',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-white font-bold text-2xl">بنك الأسئلة</h2>
          <p class="text-slate-500 text-sm mt-1">تخزين وإدارة الأسئلة لاستخدامها في الاختبارات المختلفة</p>
        </div>
        <button (click)="openModal()" class="btn-primary">
          <span class="material-icons-round">add</span>
          إضافة سؤال للبنك
        </button>
      </div>

      <!-- Filter bar -->
      <div class="edu-card bg-slate-900/40 p-4 grid grid-cols-1 md:grid-cols-3 gap-4">
        <select (change)="onTopicFilterChange($event)" class="edu-select">
          <option value="0">اختر جزئية لعرض الأسئلة</option>
          <option *ngFor="let t of topics()" [value]="t.id">{{ t.name }}</option>
        </select>
        <div class="relative">
          <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500">search</span>
          <input type="text" [(ngModel)]="searchQuery" (input)="filterQuestions()" class="edu-input pr-10" placeholder="ابحث بنص السؤال...">
        </div>
        <div class="flex items-center gap-2">
          <span class="text-slate-500 text-sm">{{ filteredQuestions().length }} سؤال</span>
        </div>
      </div>

      <!-- Questions list -->
      <div class="space-y-4">
        <div *ngFor="let q of filteredQuestions(); trackBy: trackById" class="edu-card p-6 border-slate-800 hover:border-indigo-500/30 transition-all group">
          <div class="flex items-start justify-between gap-4">
            <div class="space-y-4 flex-1">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="bg-indigo-500/10 text-indigo-400 text-[9px] font-bold px-2 py-0.5 rounded border border-indigo-500/20 uppercase tracking-wider">{{ q.topicName || 'عام' }}</span>
                <span *ngIf="q.mark" class="bg-emerald-500/10 text-emerald-400 text-[9px] font-bold px-2 py-0.5 rounded border border-emerald-500/20">{{ q.mark }} درجة</span>
                <span *ngIf="q.difficulty" class="bg-slate-800 text-slate-400 text-[9px] font-bold px-2 py-0.5 rounded border border-slate-700">{{ getDifficultyLabel(q.difficulty) }}</span>
              </div>
              <h4 class="text-slate-200 font-bold leading-relaxed">{{ q.description }}</h4>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-x-12 gap-y-2">
                <div *ngFor="let choice of getChoices(q); let i = index" class="flex items-center gap-3 text-xs"
                     [class.text-emerald-400]="q.correctAnswer === choice.letter"
                     [class.text-slate-500]="q.correctAnswer !== choice.letter">
                  <span class="w-5 h-5 rounded-full border border-current flex items-center justify-center font-bold text-[10px]"
                        [class.bg-emerald-500]="q.correctAnswer === choice.letter"
                        [class.text-white]="q.correctAnswer === choice.letter">{{ choice.letter }}</span>
                  {{ choice.text }}
                </div>
              </div>
            </div>
            <div class="flex flex-col gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
              <button (click)="openModal(q)" class="btn-icon h-9 w-9 bg-slate-800"><span class="material-icons-round text-sm">edit</span></button>
              <button (click)="deleteQuestion(q.id)" class="btn-icon h-9 w-9 bg-slate-800 text-red-400"><span class="material-icons-round text-sm">delete</span></button>
            </div>
          </div>
        </div>

        <div *ngIf="selectedTopicId() === 0 && !loading()" class="py-32 text-center text-slate-700 border-2 border-dashed border-slate-800 rounded-3xl">
          <span class="material-icons-round text-5xl mb-4 block opacity-10">quiz</span>
          <p class="text-sm">اختر جزئية من القائمة لعرض أسئلتها</p>
        </div>

        <div *ngIf="selectedTopicId() > 0 && !loading() && filteredQuestions().length === 0" class="py-32 text-center text-slate-700 border-2 border-dashed border-slate-800 rounded-3xl">
          <span class="material-icons-round text-5xl mb-4 block opacity-10">quiz</span>
          <p class="text-sm">لا توجد أسئلة في هذه الجزئية بعد</p>
        </div>

        <div *ngIf="loading()" class="py-32 text-center text-slate-500">
          <span class="material-icons-round animate-spin text-4xl block mb-2">refresh</span>
          جاري التحميل...
        </div>
      </div>

      <!-- Question Modal -->
      <div *ngIf="showModal()" class="modal-overlay">
        <div class="modal-box max-w-2xl">
          <div class="modal-header">
            <h3 class="text-white font-bold">{{ editingQuestion() ? 'تعديل سؤال' : 'إضافة سؤال للبنك' }}</h3>
            <button (click)="showModal.set(false)" class="btn-icon"><span class="material-icons-round">close</span></button>
          </div>
          <div class="modal-body space-y-6">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="edu-label">الجزئية <span class="text-red-400">*</span></label>
                <select [(ngModel)]="questionForm.topicId" class="edu-select">
                  <option [value]="0" disabled>اختر الجزئية</option>
                  <option *ngFor="let t of topics()" [value]="t.id">{{ t.name }}</option>
                </select>
              </div>
              <div>
                <label class="edu-label">الدرجة</label>
                <input type="number" [(ngModel)]="questionForm.mark" min="1" class="edu-input" placeholder="1">
              </div>
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="edu-label">مستوى الصعوبة</label>
                <select [(ngModel)]="questionForm.difficulty" class="edu-select">
                  <option value="EASY">سهل</option>
                  <option value="MEDIUM">متوسط</option>
                  <option value="HARD">صعب</option>
                </select>
              </div>
              <div>
                <label class="edu-label">وسم مفاهيمي</label>
                <input type="text" [(ngModel)]="questionForm.conceptTag" class="edu-input" placeholder="اختياري">
              </div>
            </div>
            <div>
              <label class="edu-label">نص السؤال <span class="text-red-400">*</span></label>
              <textarea [(ngModel)]="questionForm.description" class="edu-input min-h-[100px]" placeholder="اكتب نص السؤال هنا..."></textarea>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div *ngFor="let letter of ['A','B','C','D']" class="relative">
                <label class="edu-label">خيار {{ letter }}</label>
                <input type="text" [(ngModel)]="questionForm.options[letter]" class="edu-input" [placeholder]="'خيار ' + letter">
                <input type="radio" name="correct" [value]="letter" [(ngModel)]="questionForm.correctAnswer" class="absolute left-3 bottom-3 scale-125 accent-emerald-500">
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button (click)="showModal.set(false)" class="btn-secondary">إلغاء</button>
            <button (click)="saveQuestion()"
                    [disabled]="!questionForm.description || !questionForm.correctAnswer || questionForm.topicId === 0"
                    class="btn-primary px-10">
              حفظ في البنك
            </button>
          </div>
        </div>
      </div>
    </div>
  `
})
export class QuestionBankComponent implements OnInit {
  questions = signal<QuestionBankItem[]>([]);
  filteredQuestions = signal<QuestionBankItem[]>([]);
  topics = signal<{id: number, name: string}[]>([]);
  selectedTopicId = signal(0);
  loading = signal(false);
  searchQuery = '';
  showModal = signal(false);
  editingQuestion = signal<QuestionBankItem | null>(null);
  questionForm: any = {
    topicId: 0,
    description: '',
    options: { A: '', B: '', C: '', D: '' },
    correctAnswer: '',
    mark: 1,
    difficulty: 'MEDIUM',
    conceptTag: ''
  };

  constructor(private api: ApiService, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.api.getBankTopics().subscribe({
      next: t => this.topics.set(t || []),
      error: () => this.topics.set([])
    });
  }

  loadQuestionsByTopic(topicId: number) {
    this.api.getBankQuestionsByTopic(topicId).subscribe({
      next: (items: any[]) => {
        this.questions.set(items || []);
        this.filteredQuestions.set(items || []);
      },
      error: () => this.questions.set([])
    });
  }

  onTopicFilterChange(event: any) {
    const topicId = Number(event.target.value);
    this.selectedTopicId.set(topicId);
    this.searchQuery = '';
    if (topicId > 0) {
      this.loadQuestionsByTopic(topicId);
    } else {
      this.editingQuestion.set(null);
      this.questionForm = {
        topicId: 0,
        description: '',
        options: { A: '', B: '', C: '', D: '' },
        correctAnswer: 'A',
        mark: 1,
        difficulty: 'MEDIUM',
        conceptTag: ''
      };
    }
    this.showModal.set(true);
  }
  trackById(index: number, q: any): number { return q.id; }

  getDifficultyLabel(d?: string): string {
    if (d === 'EASY') return 'سهل';
    if (d === 'HARD') return 'صعب';
    return 'متوسط';
  }

  getChoices(q: any): {letter: string, text: string}[] {
    const letters = ['A', 'B', 'C', 'D', 'E'];
    const opts: string[] = Array.isArray(q.options) ? q.options : Object.values(q.options || {});
    return opts.map((text, i) => ({ letter: letters[i] || String(i + 1), text: String(text) }));
  }

  openModal(q?: any) {
    if (q) {
      this.editingQuestion.set(q);
      const opts = Array.isArray(q.options) ? q.options : Object.values(q.options || {});
      this.questionForm = {
        topicId: q.topicId || 0,
        description: q.description,
        options: { A: opts[0] || '', B: opts[1] || '', C: opts[2] || '', D: opts[3] || '' },
        correctAnswer: q.correctAnswer,
        mark: q.mark || 1,
        difficulty: q.difficulty || 'MEDIUM',
        conceptTag: q.conceptTag || ''
      };
    } else {
      this.editingQuestion.set(null);
      this.questionForm = {
        topicId: this.selectedTopicId(),
        description: '',
        options: { A: '', B: '', C: '', D: '' },
        correctAnswer: 'A',
        mark: 1,
        difficulty: 'MEDIUM',
        conceptTag: ''
      };
    }
    this.showModal.set(true);
  }

  saveQuestion() {
    const opts = this.questionForm.options;
    const payload: any = {
      topicId: this.questionForm.topicId || this.selectedTopicId(),
      description: this.questionForm.description,
      options: [opts.A, opts.B, opts.C, opts.D].filter(Boolean),
      correctAnswer: this.questionForm.correctAnswer,
      mark: this.questionForm.mark,
      difficulty: this.questionForm.difficulty,
      conceptTag: this.questionForm.conceptTag || undefined
    };
    const editing = this.editingQuestion();
    const obs = editing
      ? this.api.updateQuestionInBank(editing.id, payload)
      : this.api.addQuestionToBank(payload);
    obs.subscribe({
      next: () => {
        this.toastr.success('تم الحفظ');
        this.showModal.set(false);
        if (this.selectedTopicId() > 0) this.loadQuestionsByTopic(this.selectedTopicId());
      },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  deleteQuestion(id: number) {
    if (!confirm('هل انت متاكد من الحذف؟')) return;
    this.api.deleteQuestionFromBank(id).subscribe({
      next: () => {
        this.toastr.success('تم الحذف');
        this.questions.set(this.questions().filter(q => q.id !== id));
        this.filterQuestions();
      },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  filterQuestions() {
    const q = this.searchQuery.trim().toLowerCase();
    const all = this.questions();
    if (!q) {
      this.filteredQuestions.set(all);
    } else {
      this.filteredQuestions.set(all.filter(item => item.description?.toLowerCase().includes(q)));
    }
  }

}
