import { Component, OnInit, AfterViewInit, signal, ElementRef, ViewChildren, QueryList } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div dir="rtl" class="space-y-6">

      <div class="flex items-center justify-between">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">📊 تحليلات المنصة</h1>
        <button (click)="load()" [disabled]="loading()"
                class="flex items-center gap-2 px-4 py-2 bg-[#183764] text-white rounded-xl text-sm font-bold hover:bg-[#0f2548] transition disabled:opacity-50">
          <span class="material-icons-round text-base" [class.animate-spin]="loading()">refresh</span>
          تحديث
        </button>
      </div>

      <!-- Loading -->
      <div *ngIf="loading()" class="grid grid-cols-2 lg:grid-cols-3 gap-4">
        <div *ngFor="let i of [1,2,3,4,5,6]"
             class="h-64 bg-gray-100 dark:bg-slate-800 rounded-2xl animate-pulse"></div>
      </div>

      <ng-container *ngIf="!loading() && data()">

        <!-- KPI row -->
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div class="bg-white dark:bg-[#162033] rounded-2xl p-4 border border-gray-100 dark:border-slate-800 text-center">
            <div class="text-3xl font-black text-[#183764] dark:text-[#4BBBA0]">{{ data().locations?.length || 0 }}</div>
            <div class="text-xs text-gray-500 dark:text-slate-400 mt-1 font-semibold">محافظة</div>
          </div>
          <div class="bg-white dark:bg-[#162033] rounded-2xl p-4 border border-gray-100 dark:border-slate-800 text-center">
            <div class="text-3xl font-black text-[#F59239]">{{ totalSales() }}</div>
            <div class="text-xs text-gray-500 dark:text-slate-400 mt-1 font-semibold">إجمالي المبيعات</div>
          </div>
          <div class="bg-white dark:bg-[#162033] rounded-2xl p-4 border border-gray-100 dark:border-slate-800 text-center">
            <div class="text-3xl font-black text-[#4BBBA0]">{{ data().avgPlatformTime?.avgHours || 0 }}</div>
            <div class="text-xs text-gray-500 dark:text-slate-400 mt-1 font-semibold">متوسط ساعات الدراسة</div>
          </div>
          <div class="bg-white dark:bg-[#162033] rounded-2xl p-4 border border-gray-100 dark:border-slate-800 text-center">
            <div class="text-3xl font-black text-purple-500">{{ data().avgAttemptsToPass?.avgAttempts || 0 }}</div>
            <div class="text-xs text-gray-500 dark:text-slate-400 mt-1 font-semibold">متوسط محاولات الكويز</div>
          </div>
        </div>

        <!-- Row 1: Locations + Sales type -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div class="bg-white dark:bg-[#162033] rounded-2xl p-5 border border-gray-100 dark:border-slate-800">
            <h3 class="font-bold text-gray-800 dark:text-white mb-4 text-sm">📍 توزيع الطلاب بالمحافظات</h3>
            <canvas #locationsChart style="max-height:280px;"></canvas>
          </div>
          <div class="bg-white dark:bg-[#162033] rounded-2xl p-5 border border-gray-100 dark:border-slate-800">
            <h3 class="font-bold text-gray-800 dark:text-white mb-4 text-sm">🛒 ماذا بيتباع أكتر؟</h3>
            <canvas #salesTypeChart style="max-height:280px;"></canvas>
          </div>
        </div>

        <!-- Row 2: Purchase hours + days -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div class="bg-white dark:bg-[#162033] rounded-2xl p-5 border border-gray-100 dark:border-slate-800">
            <h3 class="font-bold text-gray-800 dark:text-white mb-4 text-sm">🕐 أوقات الشراء بالساعة</h3>
            <canvas #purchaseHoursChart style="max-height:220px;"></canvas>
          </div>
          <div class="bg-white dark:bg-[#162033] rounded-2xl p-5 border border-gray-100 dark:border-slate-800">
            <h3 class="font-bold text-gray-800 dark:text-white mb-4 text-sm">📅 أوقات الشراء بالأسبوع</h3>
            <canvas #purchaseDaysChart style="max-height:220px;"></canvas>
          </div>
        </div>

        <!-- Row 3: Login heatmap -->
        <div class="bg-white dark:bg-[#162033] rounded-2xl p-5 border border-gray-100 dark:border-slate-800">
          <h3 class="font-bold text-gray-800 dark:text-white mb-4 text-sm">🔥 هيت ماب دخول الطلاب (ساعة × يوم)</h3>
          <div class="overflow-x-auto">
            <table class="w-full text-xs" dir="rtl">
              <thead>
                <tr>
                  <th class="p-1 text-gray-400 font-semibold w-16">الساعة</th>
                  <th *ngFor="let d of dayLabels" class="p-1 text-gray-400 font-semibold text-center w-12">{{d}}</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let hour of hours">
                  <td class="p-1 text-gray-500 dark:text-slate-400 text-center font-mono">{{hour}}:00</td>
                  <td *ngFor="let day of [1,2,3,4,5,6,7]" class="p-0.5">
                    <div class="w-full h-8 rounded-md flex items-center justify-center text-white text-xs font-bold"
                         [style.background]="heatColor((heatmapData[hour] || {})[day] || 0)"
                         [title]="((heatmapData[hour] || {})[day] || 0) + ' دخول'">
                      {{ ((heatmapData[hour] || {})[day] || 0) > 0 ? ((heatmapData[hour] || {})[day]) : '' }}
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Row 4: Course scores + Center scores -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div class="bg-white dark:bg-[#162033] rounded-2xl p-5 border border-gray-100 dark:border-slate-800">
            <h3 class="font-bold text-gray-800 dark:text-white mb-4 text-sm">📈 متوسط درجات الطلاب لكل كورس</h3>
            <canvas #courseScoresChart style="max-height:280px;"></canvas>
          </div>
          <div class="bg-white dark:bg-[#162033] rounded-2xl p-5 border border-gray-100 dark:border-slate-800">
            <h3 class="font-bold text-gray-800 dark:text-white mb-4 text-sm">🏫 متوسط الدرجات لكل سنتر</h3>
            <canvas #centerScoresChart style="max-height:280px;"></canvas>
          </div>
        </div>

        <!-- Row 5: Hardest topics -->
        <div class="bg-white dark:bg-[#162033] rounded-2xl p-5 border border-gray-100 dark:border-slate-800">
          <h3 class="font-bold text-gray-800 dark:text-white mb-4 text-sm">🧠 أصعب التوبيكس (% الأخطاء)</h3>
          <div *ngIf="data().hardestTopics?.length; else noTopics" class="space-y-3">
            <div *ngFor="let t of data().hardestTopics; let i = index" class="flex items-center gap-3">
              <span class="text-xs font-bold text-gray-400 dark:text-slate-500 w-5 text-center">{{i+1}}</span>
              <div class="flex-1">
                <div class="flex justify-between items-center mb-1">
                  <span class="text-sm font-semibold text-gray-800 dark:text-white">{{t.topic}}</span>
                  <span class="text-xs font-bold"
                        [class]="t.errorRate > 60 ? 'text-red-500' : t.errorRate > 40 ? 'text-orange-500' : 'text-green-500'">
                    {{t.errorRate}}% خطأ
                  </span>
                </div>
                <div class="h-2 bg-gray-100 dark:bg-slate-700 rounded-full overflow-hidden">
                  <div class="h-full rounded-full transition-all duration-700"
                       [style.width]="t.errorRate + '%'"
                       [style.background]="t.errorRate > 60 ? '#ef4444' : t.errorRate > 40 ? '#F59239' : '#4BBBA0'"></div>
                </div>
              </div>
              <span class="text-xs text-gray-400 dark:text-slate-500 w-20 text-left">{{t.totalAnswers}} إجابة</span>
            </div>
          </div>
          <ng-template #noTopics>
            <p class="text-sm text-gray-400 dark:text-slate-500 text-center py-8">لا توجد بيانات كافية للتوبيكس</p>
          </ng-template>
        </div>

      </ng-container>
    </div>
  `
})
export class AnalyticsComponent implements OnInit, AfterViewInit {
  @ViewChildren('locationsChart,salesTypeChart,purchaseHoursChart,purchaseDaysChart,courseScoresChart,centerScoresChart')
  canvases!: QueryList<ElementRef<HTMLCanvasElement>>;

  loading = signal(true);
  data    = signal<any>(null);
  heatmapData: Record<number, Record<number, number>> = {};
  dayLabels = ['الأحد','الاثنين','الثلاثاء','الأربعاء','الخميس','الجمعة','السبت'];
  hours = Array.from({length: 24}, (_, i) => i);

  private charts: Chart<any>[] = [];

  constructor(private api: ApiService) {}

  ngOnInit(): void { this.load(); }
  ngAfterViewInit(): void {}

  load(): void {
    this.loading.set(true);
    this.destroyCharts();
    this.api.getAnalyticsAll().subscribe({
      next: d => {
        this.data.set(d);
        this.buildHeatmap(d.loginHeatmap || []);
        this.loading.set(false);
        setTimeout(() => this.buildCharts(), 100);
      },
      error: () => this.loading.set(false)
    });
  }

  totalSales(): number {
    return (this.data()?.salesByType || []).reduce((s: number, t: any) => s + (t.count || 0), 0);
  }

  buildHeatmap(rows: any[]): void {
    this.heatmapData = {};
    for (const r of rows) {
      const h = r.hour, d = r.dayOfWeek;
      if (!this.heatmapData[h]) this.heatmapData[h] = {};
      this.heatmapData[h][d] = r.count;
    }
  }

  heatColor(count: number): string {
    if (!count) return 'transparent';
    const max = 100; // normalise
    const ratio = Math.min(count / max, 1);
    const r = Math.round(24  + ratio * (75 - 24));
    const g = Math.round(187 + ratio * (100 - 187));
    const b = Math.round(160 + ratio * (20 - 160));
    const alpha = 0.2 + ratio * 0.8;
    return `rgba(${r},${g},${b},${alpha})`;
  }

  buildCharts(): void {
    const d = this.data();
    if (!d) return;
    const isDark = document.documentElement.classList.contains('dark');
    const labelColor = isDark ? '#8892a0' : '#64748b';
    const gridColor  = isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)';

    const els = this.canvases.toArray();
    if (els.length < 6) return;

    // 1) Locations — horizontal bar
    this.addChart(new Chart(els[0].nativeElement, {
      type: 'bar',
      data: {
        labels: d.locations?.map((x: any) => x.governorate) || [],
        datasets: [{ label: 'الطلاب', data: d.locations?.map((x: any) => x.count) || [],
          backgroundColor: '#4BBBA0', borderRadius: 6 }]
      },
      options: { indexAxis: 'y', plugins: { legend: { display: false } },
        scales: { x: { ticks: { color: labelColor }, grid: { color: gridColor } },
                  y: { ticks: { color: labelColor }, grid: { display: false } } } }
    }));

    // 2) Sales type — doughnut
    this.addChart(new Chart(els[1].nativeElement, {
      type: 'doughnut',
      data: {
        labels: d.salesByType?.map((x: any) => x.label) || [],
        datasets: [{ data: d.salesByType?.map((x: any) => x.count) || [],
          backgroundColor: ['#183764','#4BBBA0','#F59239','#a855f7'] }]
      },
      options: { plugins: { legend: { position: 'bottom', labels: { color: labelColor } } } }
    }));

    // 3) Purchase hours — line
    const hLabels = Array.from({length: 24}, (_, i) => `${i}:00`);
    const hCounts = Array.from({length: 24}, (_, i) => d.purchaseHours?.find((x: any) => x.hour === i)?.count || 0);
    this.addChart(new Chart(els[2].nativeElement, {
      type: 'line',
      data: {
        labels: hLabels,
        datasets: [{ label: 'مشتريات', data: hCounts, borderColor: '#F59239', backgroundColor: 'rgba(245,146,57,.1)',
          fill: true, tension: 0.4, pointRadius: 3 }]
      },
      options: { plugins: { legend: { display: false } },
        scales: { x: { ticks: { color: labelColor, maxTicksLimit: 12 }, grid: { color: gridColor } },
                  y: { ticks: { color: labelColor }, grid: { color: gridColor } } } }
    }));

    // 4) Purchase days — bar
    this.addChart(new Chart(els[3].nativeElement, {
      type: 'bar',
      data: {
        labels: d.purchaseDays?.map((x: any) => x.day) || [],
        datasets: [{ label: 'مشتريات', data: d.purchaseDays?.map((x: any) => x.count) || [],
          backgroundColor: '#183764', borderRadius: 6 }]
      },
      options: { plugins: { legend: { display: false } },
        scales: { x: { ticks: { color: labelColor }, grid: { display: false } },
                  y: { ticks: { color: labelColor }, grid: { color: gridColor } } } }
    }));

    // 5) Course scores
    this.addChart(new Chart(els[4].nativeElement, {
      type: 'bar',
      data: {
        labels: d.avgScoreByCourse?.map((x: any) => x.course?.substring(0, 20)) || [],
        datasets: [{ label: 'متوسط الدرجة', data: d.avgScoreByCourse?.map((x: any) => x.avgScore) || [],
          backgroundColor: '#4BBBA0', borderRadius: 6 }]
      },
      options: { plugins: { legend: { display: false } },
        scales: { x: { ticks: { color: labelColor }, grid: { display: false } },
                  y: { max: 100, ticks: { color: labelColor }, grid: { color: gridColor } } } }
    }));

    // 6) Center scores
    this.addChart(new Chart(els[5].nativeElement, {
      type: 'bar',
      data: {
        labels: d.avgScoreByCenter?.map((x: any) => x.center) || [],
        datasets: [{ label: 'متوسط الدرجة', data: d.avgScoreByCenter?.map((x: any) => x.avgScore) || [],
          backgroundColor: '#F59239', borderRadius: 6 }]
      },
      options: { plugins: { legend: { display: false } },
        scales: { x: { ticks: { color: labelColor }, grid: { display: false } },
                  y: { max: 100, ticks: { color: labelColor }, grid: { color: gridColor } } } }
    }));
  }

  private addChart(c: Chart<any>): void { this.charts.push(c); }

  private destroyCharts(): void {
    this.charts.forEach(c => c.destroy());
    this.charts = [];
  }
}
