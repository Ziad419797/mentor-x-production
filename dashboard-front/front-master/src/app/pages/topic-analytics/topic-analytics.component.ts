import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-topic-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">

      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-white font-bold text-2xl">تحليل نقاط الضعف</h2>
          <p class="text-slate-500 text-sm mt-1">النقاط التي يواجه فيها الطلاب صعوبة — مرتبة بنسبة الأخطاء</p>
        </div>
        <button (click)="load()" class="btn-secondary gap-2">
          <span class="material-icons-round text-base">refresh</span>
          تحديث
        </button>
      </div>

      <!-- Loading -->
      <div *ngIf="loading()" class="flex justify-center py-20">
        <span class="material-icons-round animate-spin text-indigo-400 text-4xl">refresh</span>
      </div>

      <!-- Empty -->
      <div *ngIf="!loading() && data().length === 0"
           class="py-32 text-center text-slate-700 border-2 border-dashed border-slate-800 rounded-3xl">
        <span class="material-icons-round text-5xl block mb-3 opacity-20">analytics</span>
        <p class="text-sm">لا توجد بيانات بعد — ربّط الأسئلة بالجزئيات أولاً</p>
      </div>

      <!-- Stats cards -->
      <div *ngIf="!loading() && data().length > 0" class="grid grid-cols-3 gap-4">
        <div class="edu-card p-4 text-center">
          <p class="text-slate-400 text-xs mb-1">إجمالي الجزئيات</p>
          <p class="text-white font-black text-2xl">{{ data().length }}</p>
        </div>
        <div class="edu-card p-4 text-center">
          <p class="text-slate-400 text-xs mb-1">أعلى نسبة خطأ</p>
          <p class="text-red-400 font-black text-2xl">{{ data()[0]?.wrongPct ?? 0 }}%</p>
        </div>
        <div class="edu-card p-4 text-center">
          <p class="text-slate-400 text-xs mb-1">متوسط الأخطاء</p>
          <p class="text-orange-400 font-black text-2xl">{{ avgPct() }}%</p>
        </div>
      </div>

      <!-- Table -->
      <div *ngIf="!loading() && data().length > 0" class="edu-card overflow-hidden">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-slate-800">
              <th class="text-right text-slate-400 font-bold px-4 py-3">#</th>
              <th class="text-right text-slate-400 font-bold px-4 py-3">الجزئية / النقطة</th>
              <th class="text-right text-slate-400 font-bold px-4 py-3">إجمالي الإجابات</th>
              <th class="text-right text-slate-400 font-bold px-4 py-3">إجابات خاطئة</th>
              <th class="text-right text-slate-400 font-bold px-4 py-3 w-48">نسبة الخطأ</th>
              <th class="text-right text-slate-400 font-bold px-4 py-3">مستوى الصعوبة</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let row of data(); let i = index"
                class="border-b border-slate-800/50 hover:bg-slate-800/30 transition-colors">
              <td class="px-4 py-3 text-slate-500 font-mono text-xs">{{ i + 1 }}</td>
              <td class="px-4 py-3">
                <span class="text-slate-200 font-bold">{{ row.topicName }}</span>
              </td>
              <td class="px-4 py-3 text-slate-400 font-mono">{{ row.totalAnswers }}</td>
              <td class="px-4 py-3 text-red-400 font-mono font-bold">{{ row.wrongAnswers }}</td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-3">
                  <div class="flex-1 h-2 bg-slate-800 rounded-full overflow-hidden">
                    <div class="h-full rounded-full transition-all duration-500"
                         [style.width]="row.wrongPct + '%'"
                         [ngClass]="row.wrongPct >= 70 ? 'bg-red-500'
                                  : row.wrongPct >= 40 ? 'bg-orange-500'
                                  : 'bg-emerald-500'">
                    </div>
                  </div>
                  <span class="text-xs font-black w-10 text-left"
                        [ngClass]="row.wrongPct >= 70 ? 'text-red-400'
                                 : row.wrongPct >= 40 ? 'text-orange-400'
                                 : 'text-emerald-400'">
                    {{ row.wrongPct }}%
                  </span>
                </div>
              </td>
              <td class="px-4 py-3">
                <span class="text-xs font-bold px-2 py-1 rounded-full"
                      [ngClass]="row.wrongPct >= 70
                        ? 'bg-red-500/10 text-red-400 border border-red-500/20'
                        : row.wrongPct >= 40
                          ? 'bg-orange-500/10 text-orange-400 border border-orange-500/20'
                          : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'">
                  {{ row.wrongPct >= 70 ? '🔴 صعب جداً'
                   : row.wrongPct >= 40 ? '🟠 متوسط'
                   : '🟢 سهل' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

    </div>
  `
})
export class TopicAnalyticsComponent implements OnInit {
  loading = signal(true);
  data    = signal<any[]>([]);

  avgPct = computed(() => {
    const d = this.data();
    if (!d.length) return 0;
    return Math.round(d.reduce((s, r) => s + r.wrongPct, 0) / d.length * 10) / 10;
  });

  constructor(private api: ApiService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.api.getTopicWeakness().subscribe({
      next: d => { this.data.set(d); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}
