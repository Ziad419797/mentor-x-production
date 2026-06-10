import {
  Component, OnInit, OnDestroy, AfterViewInit,
  ViewChildren, QueryList, ElementRef, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { StudentApiService } from '../../services/student-api.service';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  selector: 'app-student-analytics',
  standalone: true,
  imports: [CommonModule],
  template: `
<div class="space-y-6">

  <!-- Header -->
  <div class="flex items-center justify-between">
    <div>
      <h1 class="text-2xl font-bold text-[#183764] dark:text-white">إحصائياتي</h1>
      <p class="text-sm text-[#8892a0] mt-1">تقرير شامل عن أداءك ومستواك</p>
    </div>
    <button (click)="load()"
            class="flex items-center gap-2 px-4 py-2 rounded-xl bg-[#4BBBA0]/10 text-[#4BBBA0] text-sm font-medium hover:bg-[#4BBBA0]/20 transition-all">
      <span class="material-symbols-outlined" style="font-size:18px">refresh</span>
      تحديث
    </button>
  </div>

  <!-- Loading / Error -->
  <div *ngIf="loading()" class="flex items-center justify-center py-20">
    <div class="w-10 h-10 border-4 border-[#4BBBA0]/30 border-t-[#4BBBA0] rounded-full animate-spin"></div>
  </div>
  <div *ngIf="error()" class="bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/30 rounded-2xl p-6 text-center text-red-600 dark:text-red-400 text-sm">
    {{ error() }}
  </div>

  <ng-container *ngIf="!loading() && !error() && data()">

    <!-- KPI Row -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
      <div class="bg-white dark:bg-[#162033] rounded-2xl p-5 border border-[#DDE1EA] dark:border-slate-800">
        <div class="text-3xl font-bold text-[#4BBBA0]">{{ streak() }}</div>
        <div class="text-xs text-[#8892a0] mt-1">ستريك (أيام متتالية)</div>
      </div>
      <div class="bg-white dark:bg-[#162033] rounded-2xl p-5 border border-[#DDE1EA] dark:border-slate-800">
        <div class="text-3xl font-bold text-[#F59239]">{{ totalQuizzes() }}</div>
        <div class="text-xs text-[#8892a0] mt-1">كويزات أكملتها</div>
      </div>
      <div class="bg-white dark:bg-[#162033] rounded-2xl p-5 border border-[#DDE1EA] dark:border-slate-800">
        <div class="text-3xl font-bold text-[#183764] dark:text-white">{{ avgScore() }}%</div>
        <div class="text-xs text-[#8892a0] mt-1">متوسط درجاتك</div>
      </div>
      <div class="bg-white dark:bg-[#162033] rounded-2xl p-5 border border-[#DDE1EA] dark:border-slate-800">
        <div class="text-3xl font-bold text-purple-500">{{ achievements() }}</div>
        <div class="text-xs text-[#8892a0] mt-1">إنجازات</div>
      </div>
    </div>

    <!-- Charts Row 1 -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

      <!-- Scores vs Average -->
      <div class="bg-white dark:bg-[#162033] rounded-2xl p-6 border border-[#DDE1EA] dark:border-slate-800">
        <h3 class="text-sm font-bold text-[#183764] dark:text-white mb-4">درجاتك مقارنةً بالمتوسط</h3>
        <div style="height:260px; position:relative">
          <canvas #vsAvgChart></canvas>
        </div>
      </div>

      <!-- Progress Over Time -->
      <div class="bg-white dark:bg-[#162033] rounded-2xl p-6 border border-[#DDE1EA] dark:border-slate-800">
        <h3 class="text-sm font-bold text-[#183764] dark:text-white mb-4">تطور مستواك في الكويزات</h3>
        <div style="height:260px; position:relative">
          <canvas #progressChart></canvas>
        </div>
      </div>
    </div>

    <!-- Charts Row 2 -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

      <!-- Login Hours -->
      <div class="bg-white dark:bg-[#162033] rounded-2xl p-6 border border-[#DDE1EA] dark:border-slate-800">
        <h3 class="text-sm font-bold text-[#183764] dark:text-white mb-4">أوقات دخولك للمنصة (بالساعة)</h3>
        <div style="height:240px; position:relative">
          <canvas #loginChart></canvas>
        </div>
      </div>

      <!-- Quiz Speed -->
      <div class="bg-white dark:bg-[#162033] rounded-2xl p-6 border border-[#DDE1EA] dark:border-slate-800">
        <h3 class="text-sm font-bold text-[#183764] dark:text-white mb-4">سرعة الحل في الكويزات (ثانية/سؤال)</h3>
        <div style="height:240px; position:relative">
          <canvas #speedChart></canvas>
        </div>
      </div>
    </div>

    <!-- Attempts to Pass -->
    <div *ngIf="attemptsRows().length > 0"
         class="bg-white dark:bg-[#162033] rounded-2xl p-6 border border-[#DDE1EA] dark:border-slate-800">
      <h3 class="text-sm font-bold text-[#183764] dark:text-white mb-4">عدد المحاولات للنجاح في كل كويز</h3>
      <div class="space-y-3">
        <div *ngFor="let row of attemptsRows()" class="flex items-center gap-3">
          <span class="text-xs text-[#8892a0] w-40 truncate text-right">{{ row.quizName }}</span>
          <div class="flex-1 h-2 bg-[#F5F6FA] dark:bg-white/5 rounded-full overflow-hidden">
            <div class="h-full rounded-full transition-all"
                 [style.width.%]="Math.min(row.attempts * 20, 100)"
                 [style.background]="row.attempts <= 1 ? '#4BBBA0' : row.attempts <= 3 ? '#F59239' : '#ef4444'">
            </div>
          </div>
          <span class="text-xs font-bold w-6 text-center"
                [class.text-[#4BBBA0]]="row.attempts <= 1"
                [class.text-[#F59239]]="row.attempts > 1 && row.attempts <= 3"
                [class.text-red-500]="row.attempts > 3">{{ row.attempts }}</span>
        </div>
      </div>
    </div>

    <!-- Achievements -->
    <div *ngIf="achievementsList().length > 0"
         class="bg-white dark:bg-[#162033] rounded-2xl p-6 border border-[#DDE1EA] dark:border-slate-800">
      <h3 class="text-sm font-bold text-[#183764] dark:text-white mb-4">إنجازاتك 🏆</h3>
      <div class="flex flex-wrap gap-3">
        <div *ngFor="let a of achievementsList()"
             class="flex items-center gap-2 px-4 py-2 rounded-xl bg-[#4BBBA0]/10 border border-[#4BBBA0]/30">
          <span class="text-lg">{{ a.icon }}</span>
          <span class="text-sm font-medium text-[#183764] dark:text-white">{{ a.label }}</span>
        </div>
      </div>
    </div>

  </ng-container>
</div>
  `,
  styles: []
})
export class StudentAnalyticsComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChildren('vsAvgChart')   vsAvgRefs!:   QueryList<ElementRef<HTMLCanvasElement>>;
  @ViewChildren('progressChart') progressRefs!: QueryList<ElementRef<HTMLCanvasElement>>;
  @ViewChildren('loginChart')   loginRefs!:   QueryList<ElementRef<HTMLCanvasElement>>;
  @ViewChildren('speedChart')   speedRefs!:   QueryList<ElementRef<HTMLCanvasElement>>;

  loading = signal(true);
  error   = signal('');
  data    = signal<any>(null);

  streak         = signal(0);
  totalQuizzes   = signal(0);
  avgScore       = signal(0);
  achievements   = signal(0);
  attemptsRows   = signal<any[]>([]);
  achievementsList = signal<any[]>([]);

  readonly Math = Math;

  private charts: Chart<any>[] = [];
  private viewReady = false;
  private dataReady = false;

  constructor(private api: StudentApiService) {}

  ngOnInit() { this.load(); }

  ngAfterViewInit() {
    this.viewReady = true;
    if (this.dataReady) this.buildCharts();
  }

  ngOnDestroy() { this.destroyCharts(); }

  load() {
    this.loading.set(true);
    this.error.set('');
    this.dataReady = false;
    this.api.getStudentAnalytics().subscribe({
      next: (d: any) => {
        this.data.set(d);
        this.parseKpis(d);
        this.loading.set(false);
        this.dataReady = true;
        if (this.viewReady) {
          setTimeout(() => this.buildCharts(), 50);
        }
      },
      error: (e: any) => {
        this.error.set('تعذّر تحميل الإحصائيات. تأكد من الاتصال وحاول مرة أخرى.');
        this.loading.set(false);
      }
    });
  }

  private parseKpis(d: any) {
    // Streak
    this.streak.set(d.streak?.currentStreak ?? 0);

    // Quiz count from progress
    const prog: any[] = d.progressOverTime ?? [];
    this.totalQuizzes.set(prog.length);

    // Avg score from vsAvg
    const vsAvg: any[] = d.vsAvg ?? [];
    if (vsAvg.length) {
      const sum = vsAvg.reduce((acc: number, r: any) => acc + (r.myScore ?? r.myAvg ?? 0), 0);
      this.avgScore.set(Math.round(sum / vsAvg.length));
    }

    // Achievements
    const ach: any[] = d.achievements ?? [];
    const unlockedAch = ach.filter((a: any) => a.unlocked !== false);
    this.achievements.set(unlockedAch.length);
    this.achievementsList.set(unlockedAch.map((a: any) => ({
      label: a.label ?? a.name ?? a,
      icon: a.icon ?? '🏅'
    })));

    // Attempts
    const att: any[] = d.attemptsToPass ?? [];
    this.attemptsRows.set(att.map((r: any) => ({
      quizName: r.quiz ?? r.quizTitle ?? r.quizName ?? 'كويز',
      attempts: r.attempts ?? r.avgAttempts ?? 1
    })));
  }

  private destroyCharts() {
    this.charts.forEach(c => c.destroy());
    this.charts = [];
  }

  private buildCharts() {
    this.destroyCharts();
    const d = this.data();
    if (!d) return;

    const dark = document.documentElement.classList.contains('dark');
    const textColor = dark ? '#8892a0' : '#64748b';
    const gridColor = dark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)';

    const baseOpts: any = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        x: { ticks: { color: textColor, font: { size: 10 } }, grid: { color: gridColor } },
        y: { ticks: { color: textColor, font: { size: 10 } }, grid: { color: gridColor } }
      }
    };

    // 1. Scores vs Average
    const vsAvg: any[] = d.vsAvg ?? [];
    if (vsAvg.length && this.vsAvgRefs.first) {
      const c = new Chart(this.vsAvgRefs.first.nativeElement, {
        type: 'bar',
        data: {
          labels: vsAvg.map((r: any) => r.course ?? r.courseName ?? r.label ?? ''),
          datasets: [
            {
              label: 'درجاتك',
              data: vsAvg.map((r: any) => r.myScore ?? r.myAvg ?? 0),
              backgroundColor: '#4BBBA0',
              borderRadius: 6
            },
            {
              label: 'متوسط السنتر',
              data: vsAvg.map((r: any) => r.centerAvg ?? 0),
              backgroundColor: '#F59239',
              borderRadius: 6
            },
            {
              label: 'متوسط الكل',
              data: vsAvg.map((r: any) => r.allAvg ?? 0),
              backgroundColor: '#183764',
              borderRadius: 6
            }
          ]
        },
        options: {
          ...baseOpts,
          plugins: {
            legend: { display: true, labels: { color: textColor, font: { size: 10 }, boxWidth: 12 } }
          }
        }
      });
      this.charts.push(c);
    }

    // 2. Progress over time
    const prog: any[] = d.progressOverTime ?? [];
    if (prog.length && this.progressRefs.first) {
      const c = new Chart(this.progressRefs.first.nativeElement, {
        type: 'line',
        data: {
          labels: prog.map((r: any) => r.label ?? r.attemptDate ?? r.date ?? ''),
          datasets: [{
            label: 'الدرجة',
            data: prog.map((r: any) => r.avgScore ?? r.score ?? r.percentage ?? 0),
            borderColor: '#4BBBA0',
            backgroundColor: 'rgba(75,187,160,0.1)',
            tension: 0.4,
            fill: true,
            pointRadius: 3
          }]
        },
        options: baseOpts
      });
      this.charts.push(c);
    }

    // 3. Login hours — backend returns {byHour: number[24], byDay: number[7]}
    let hours: number[] = Array(24).fill(0);
    const lh = d.loginHours;
    if (lh && Array.isArray(lh.byHour)) {
      hours = lh.byHour;
    } else if (Array.isArray(lh)) {
      (lh as any[]).forEach((r: any) => {
        const h = r.hour ?? r.loginHour ?? 0;
        if (h >= 0 && h < 24) hours[h] = r.count ?? r.loginCount ?? 0;
      });
    }
    if (this.loginRefs.first) {
      const c = new Chart(this.loginRefs.first.nativeElement, {
        type: 'bar',
        data: {
          labels: Array.from({ length: 24 }, (_, i) => `${i}:00`),
          datasets: [{
            label: 'مرات الدخول',
            data: hours,
            backgroundColor: '#183764',
            borderRadius: 4
          }]
        },
        options: baseOpts
      });
      this.charts.push(c);
    }

    // 4. Quiz speed
    const speed: any[] = d.quizSpeed ?? [];
    if (speed.length && this.speedRefs.first) {
      const c = new Chart(this.speedRefs.first.nativeElement, {
        type: 'bar',
        data: {
          labels: speed.map((r: any) => r.quiz ?? r.quizTitle ?? r.quizName ?? 'كويز'),
          datasets: [{
            label: 'ث',
            data: speed.map((r: any) => r.avgSeconds ?? r.avgSecondsPerQuestion ?? r.speed ?? 0),
            backgroundColor: '#a855f7',
            borderRadius: 6
          }]
        },
        options: {
          ...baseOpts,
          indexAxis: 'y' as const
        }
      });
      this.charts.push(c);
    }
  }
}
