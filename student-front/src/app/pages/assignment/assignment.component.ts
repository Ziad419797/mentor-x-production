import { Component, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { StudentApiService } from '../../services/student-api.service';

type Phase = 'intro' | 'solving' | 'done';

@Component({
  selector: 'app-assignment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './assignment.component.html'
})
export class AssignmentComponent implements OnInit, OnDestroy {
  loading      = signal(true);
  phase        = signal<Phase>('intro');
  assignmentId = signal(0);
  assignment   = signal<any | null>(null);
  questions    = signal<any[]>([]);
  answers      = signal<Record<number, string>>({});
  currentIndex = signal(0);
  submitting   = signal(false);
  result       = signal<any | null>(null);
  error        = signal('');
  timeLeft     = signal(0);

  private timerInterval: any;

  answeredCount = computed(() =>
    this.questions().filter(q => !!this.answers()[q.id]).length
  );
  currentQ = computed(() => this.questions()[this.currentIndex()] ?? null);

  constructor(
    private api: StudentApiService,
    private route: ActivatedRoute,
    public  router: Router
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.assignmentId.set(id);

    this.api.getAssignmentById(id).subscribe({
      next: a => this.assignment.set(a),
      error: () => {}
    });

    this.api.getAssignmentQuestions(id).subscribe({
      next: qs => { this.questions.set(Array.isArray(qs) ? qs : []); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  ngOnDestroy() { clearInterval(this.timerInterval); }

  start() {
    this.phase.set('solving');
    const mins = this.assignment()?.durationMinutes;
    if (mins && mins > 0) {
      this.timeLeft.set(mins * 60);
      this.timerInterval = setInterval(() => {
        const t = this.timeLeft() - 1;
        if (t <= 0) { clearInterval(this.timerInterval); this.submit(); }
        else this.timeLeft.set(t);
      }, 1000);
    }
  }

  formatTime(s: number): string {
    const m = Math.floor(s / 60);
    const sec = s % 60;
    return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
  }

  select(qId: number, opt: string) {
    if (this.phase() === 'done') return;
    const cur = { ...this.answers() };
    cur[qId] = opt;
    this.answers.set(cur);
  }

  goTo(i: number) {
    if (i >= 0 && i < this.questions().length) this.currentIndex.set(i);
  }

  isAnswered(i: number): boolean {
    const q = this.questions()[i];
    return q ? !!this.answers()[q.id] : false;
  }

  optClass(q: any, opt: string): string {
    if (this.phase() !== 'done') {
      return this.answers()[q.id] === opt
        ? 'border-[#F59239] bg-orange-50 dark:bg-[#F59239]/10 text-[#F59239]'
        : 'border-[#DDE1EA] dark:border-slate-700 text-[#183764] dark:text-slate-300 hover:border-[#F59239]/50';
    }
    if (opt === q.correctAnswer) return 'border-green-500 bg-green-50 dark:bg-green-500/10 text-green-700 dark:text-green-400';
    if (this.answers()[q.id] === opt) return 'border-red-400 bg-red-50 dark:bg-red-500/10 text-red-600';
    return 'border-[#DDE1EA] dark:border-slate-700 text-slate-400';
  }

  allAnswered(): boolean {
    return this.questions().every(q => !!this.answers()[q.id]);
  }

  submit() {
    if (this.submitting()) return;
    clearInterval(this.timerInterval);
    this.error.set('');
    this.submitting.set(true);
    const payload = Object.entries(this.answers()).map(([qId, answer]) => ({
      questionId: Number(qId), answer
    }));
    this.api.submitAssignment(this.assignmentId(), payload).subscribe({
      next: res => { this.result.set(res); this.phase.set('done'); this.submitting.set(false); },
      error: (e: any) => { this.error.set(e?.error?.message || 'خطأ في التسليم'); this.submitting.set(false); }
    });
  }
}
