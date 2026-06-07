import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { StudentApiService } from '../../services/student-api.service';

@Component({
  selector: 'app-quiz',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './quiz.component.html'
})
export class QuizComponent implements OnInit {
  loading    = signal(true);
  quizId     = signal(0);
  questions  = signal<any[]>([]);
  answers    = signal<Record<number, string>>({});
  submitted  = signal(false);
  submitting = signal(false);
  result     = signal<any | null>(null);
  attemptId  = signal<number | null>(null);
  error      = signal('');

  constructor(
    private api: StudentApiService,
    private route: ActivatedRoute,
    public  router: Router
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('quizId'));
    this.quizId.set(id);
    this.api.getQuizQuestions(id).subscribe({
      next: qs => { this.questions.set(Array.isArray(qs) ? qs : []); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  select(qId: number, opt: string) {
    if (this.submitted()) return;
    const cur = { ...this.answers() };
    cur[qId] = opt;
    this.answers.set(cur);
  }

  isSelected(qId: number, opt: string): boolean { return this.answers()[qId] === opt; }

  allAnswered(): boolean {
    return this.questions().every(q => !!this.answers()[q.id]);
  }

  submit() {
    if (!this.allAnswered()) { this.error.set('يرجى الإجابة على جميع الأسئلة قبل التسليم'); return; }
    this.error.set('');
    this.submitting.set(true);
    const ans = this.answers();
    const payload = Object.entries(ans).map(([qId, answer]) => ({
      questionId: Number(qId), answer
    }));

    // Try start then submit flow, or direct submit
    this.api.startQuiz(this.quizId()).subscribe({
      next: attempt => {
        const aId = attempt.id || attempt.attemptId;
        this.attemptId.set(aId);
        this.api.submitQuiz(aId, payload).subscribe({
          next: res => { this.result.set(res); this.submitted.set(true); this.submitting.set(false); },
          error: (e: any) => { this.error.set(e?.error?.message || 'خطأ في التسليم'); this.submitting.set(false); }
        });
      },
      error: () => {
        // Try direct submit without start
        this.api.submitQuiz(0, payload).subscribe({
          next: res => { this.result.set(res); this.submitted.set(true); this.submitting.set(false); },
          error: (e: any) => { this.error.set(e?.error?.message || 'خطأ في التسليم'); this.submitting.set(false); }
        });
      }
    });
  }

  getScore(): number {
    if (this.result()) return this.result().score ?? this.result().totalScore ?? 0;
    let s = 0;
    for (const q of this.questions()) {
      if (this.answers()[q.id] === q.correctAnswer) s += (q.mark ?? 1);
    }
    return s;
  }

  getTotal(): number {
    return this.questions().reduce((sum, q) => sum + (q.mark ?? 1), 0);
  }

  optClass(q: any, opt: string): string {
    if (!this.submitted()) {
      return this.isSelected(q.id, opt)
        ? 'border-primary bg-primary/10 text-primary'
        : 'border-gray-200 text-gray-600 hover:border-gray-300';
    }
    if (opt === q.correctAnswer) return 'border-green-500 bg-green-50 text-green-700';
    if (this.answers()[q.id] === opt && opt !== q.correctAnswer)
      return 'border-red-400 bg-red-50 text-red-600';
    return 'border-gray-100 text-gray-400';
  }
}
