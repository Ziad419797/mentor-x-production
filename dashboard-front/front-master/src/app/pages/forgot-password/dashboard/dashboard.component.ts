import { Component, OnInit, signal, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../../services/api.service';
import { DashboardAnalytics, Student } from '../../../models/models';
import { BaseChartDirective, provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { ToastrService } from 'ngx-toastr';
import { RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { CategoryStateService } from '../../../services/category-state.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, BaseChartDirective, RouterLink],
  providers: [provideCharts(withDefaultRegisterables())],
  template: `
    <div class="space-y-8 animate-fade-in pb-10">
      
      <!-- Command Center Header -->
      <div class="edu-card bg-slate-900/40 p-0 overflow-hidden border-b-4 border-indigo-600 shadow-2xl">
         <div class="flex flex-col lg:flex-row">
            <div class="p-8 flex-1 space-y-4">
               <div class="flex items-center gap-3">
                  <div class="w-12 h-12 rounded-2xl bg-indigo-600 flex items-center justify-center text-white shadow-lg shadow-indigo-600/20">
                     <span class="material-icons-round">terminal</span>
                  </div>
                  <div>
                     <h2 class="text-white font-black text-2xl tracking-tight">مركز قيادة المعلم</h2>
                     <p class="text-slate-500 text-sm mt-0.5">نظام إدارة المحتوى التعليمي المتكامل</p>
                  </div>
               </div>
               
               <div class="flex flex-wrap items-center gap-6 pt-2">
                  <div class="flex items-center gap-3 bg-slate-950/40 px-4 py-2 rounded-2xl border border-slate-800">
                     <span class="text-slate-500 text-[10px] uppercase font-bold tracking-widest">المرحلة الحالية:</span>
                     <span class="text-indigo-400 font-black">{{ categoryState.currentCategory?.name || 'كل المراحل' }}</span>
                  </div>
                  <div class="flex items-center gap-3 bg-slate-950/40 px-4 py-2 rounded-2xl border border-slate-800">
                     <span class="text-slate-500 text-[10px] uppercase font-bold tracking-widest">المادة التخصصية:</span>
                     <span class="text-white font-black">الفيزياء والرياضيات</span>
                  </div>
               </div>
            </div>
            
            <div class="bg-indigo-600/5 p-8 border-r border-slate-800 flex flex-col justify-center">
               <div class="grid grid-cols-2 gap-x-12 gap-y-4">
                  <div>
                     <span class="block text-[10px] text-slate-500 font-bold uppercase mb-1">الطلاب النشطين</span>
                     <span class="text-white font-black text-2xl">{{ stats()?.activeStudents || 0 }}</span>
                  </div>
                  <div>
                     <span class="block text-[10px] text-slate-500 font-bold uppercase mb-1">نسبة النجاح</span>
                     <span class="text-emerald-400 font-black text-2xl">{{ stats()?.quizPassRate || 0 }}%</span>
                  </div>
               </div>
            </div>
         </div>
      </div>

      <!-- Quick Operational Focus -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div class="edu-card relative group hover:border-indigo-500/50 transition-all">
          <div class="flex items-center gap-4">
             <div class="w-12 h-12 rounded-2xl bg-indigo-500/10 flex items-center justify-center text-indigo-400 group-hover:bg-indigo-500 group-hover:text-white transition-all">
                <span class="material-icons-round">groups</span>
             </div>
             <div>
                <p class="text-slate-500 text-[10px] uppercase font-bold">الطلاب الجدد</p>
                <h3 class="text-white font-black text-lg">{{ stats()?.newStudentsThisWeek || 0 }}</h3>
             </div>
          </div>
        </div>

        <div class="edu-card relative group hover:border-amber-500/50 transition-all">
          <div class="flex items-center gap-4">
             <div class="w-12 h-12 rounded-2xl bg-amber-500/10 flex items-center justify-center text-amber-400 group-hover:bg-amber-500 group-hover:text-white transition-all">
                <span class="material-icons-round">person_search</span>
             </div>
             <div>
                <p class="text-slate-500 text-[10px] uppercase font-bold">طلبات القبول</p>
                <h3 class="text-white font-black text-lg">{{ stats()?.pendingStudents || 0 }}</h3>
             </div>
          </div>
        </div>

        <div class="edu-card relative group hover:border-emerald-500/50 transition-all">
          <div class="flex items-center gap-4">
             <div class="w-12 h-12 rounded-2xl bg-emerald-500/10 flex items-center justify-center text-emerald-400 group-hover:bg-emerald-500 group-hover:text-white transition-all">
                <span class="material-icons-round">quiz</span>
             </div>
             <div>
                <p class="text-slate-500 text-[10px] uppercase font-bold">إجمالي الاختبارات</p>
                <h3 class="text-white font-black text-lg">{{ stats()?.quizAttempts || 0 }}</h3>
             </div>
          </div>
        </div>

        <div class="edu-card relative group hover:border-purple-500/50 transition-all">
          <div class="flex items-center gap-4">
             <div class="w-12 h-12 rounded-2xl bg-purple-500/10 flex items-center justify-center text-purple-400 group-hover:bg-purple-500 group-hover:text-white transition-all">
                <span class="material-icons-round">account_balance_wallet</span>
             </div>
             <div>
                <p class="text-slate-500 text-[10px] uppercase font-bold">مبيعات اليوم</p>
                <h3 class="text-white font-black text-lg">14,200 <small class="text-[8px]">ج.م</small></h3>
             </div>
          </div>
        </div>
      </div>

      <!-- Main Operational Layout -->
      <div class="dash-grid">
        
        <!-- Left: Analytics & Map -->
        <div class="dash-main">
           
           <!-- Registration Flow -->
           <div class="edu-card p-6">
              <div class="flex items-center justify-between mb-8">
                 <div>
                    <h4 class="text-white font-bold">معدل الانضمام للمنهج</h4>
                    <p class="text-slate-500 text-[10px] mt-1">تتبع تسجيل الطلاب الجدد خلال الأسبوع الأخير</p>
                 </div>
                 <div class="flex items-center gap-4">
                    <div class="flex items-center gap-2">
                       <span class="w-2 h-2 rounded-full bg-indigo-500"></span>
                       <span class="text-[9px] text-slate-400 font-bold">طالب مسجل</span>
                    </div>
                 </div>
              </div>
              <div class="h-[280px]">
                 <canvas baseChart [data]="barChartData" [options]="barChartOptions" [type]="'bar'"></canvas>
              </div>
           </div>

           <!-- Geography -->
           <div class="edu-card p-0 overflow-hidden relative min-h-[400px] border-slate-800">
              <div class="absolute top-6 right-6 z-[1000] bg-slate-950/80 backdrop-blur-md px-5 py-3 rounded-2xl border border-slate-800 shadow-2xl">
                 <h4 class="text-white font-bold text-sm">التغطية الجغرافية</h4>
                 <p class="text-slate-500 text-[10px] mt-0.5">توزع الطلاب على مستوى المحافظات</p>
              </div>
              <div id="map" class="w-full h-full min-h-[400px]"></div>
           </div>

        </div>

        <!-- Right: Actions & Tables -->
        <div class="dash-side">
           
           <!-- Pending Students -->
           <div class="edu-card p-6 flex flex-col h-full max-h-[480px]">
              <div class="flex items-center justify-between mb-6">
                 <h4 class="text-white font-bold">طلبات بانتظار الاعتماد</h4>
                 <a routerLink="/students" class="text-indigo-400 text-[10px] font-bold hover:underline">المزيد</a>
              </div>
              
              <div class="flex-1 space-y-3 overflow-y-auto custom-scrollbar pr-2">
                 <div *ngFor="let student of pendingStudents()" class="flex items-center justify-between p-3 rounded-2xl bg-slate-950/40 border border-slate-800 hover:border-slate-700 transition-all group">
                    <div class="flex items-center gap-3">
                       <div class="w-10 h-10 rounded-xl bg-slate-800 flex items-center justify-center text-slate-400 font-bold text-xs">
                          {{ student.fullName ? student.fullName[0] : "?" }}
                       </div>
                       <div class="flex flex-col">
                          <span class="text-slate-200 text-xs font-bold truncate max-w-[100px]">{{ student.fullName }}</span>
                          <span class="text-[9px] text-slate-500">{{ student.phone }}</span>
                       </div>
                    </div>
                    <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                       <button (click)="approveStudent(student.id)" class="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 hover:bg-emerald-500 transition-all">
                          <span class="material-icons-round text-sm">check</span>
                       </button>
                    </div>
                 </div>

                 <div *ngIf="pendingStudents().length === 0" class="flex flex-col items-center justify-center py-12 text-slate-700">
                    <span class="material-icons-round text-5xl mb-3 opacity-20">verified</span>
                    <p class="text-xs">كل الطلاب معتمدون</p>
                 </div>
              </div>
           </div>

           <!-- Leaderboard -->
           <div class="edu-card p-6">
              <h4 class="text-white font-bold mb-6 flex items-center gap-2">
                 <span class="material-icons-round text-amber-400">emoji_events</span>
                 نخبة الطلاب
              </h4>
              <div class="space-y-4">
                 <div *ngFor="let s of topStudents().slice(0, 5)" class="flex items-center justify-between p-2 rounded-xl border border-transparent hover:border-slate-800 hover:bg-slate-900/40 transition-all">
                    <div class="flex items-center gap-3">
                       <span class="w-6 h-6 rounded-lg bg-slate-900 border border-slate-800 flex items-center justify-center text-[10px] font-black text-indigo-400">{{ s.rank }}</span>
                       <span class="text-slate-300 text-xs truncate max-w-[120px]">{{ s.name }}</span>
                    </div>
                    <span class="text-emerald-400 font-black text-xs">{{ s.score }}%</span>
                 </div>
              </div>
           </div>

        </div>

      </div>

    </div>
  `
})
export class DashboardComponent implements OnInit, AfterViewInit {
  stats = signal<DashboardAnalytics | null>(null);
  pendingStudents = signal<Student[]>([]);
  topStudents = signal<any[]>([]);
  map: any;
  private categorySub?: Subscription;

  public barChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'التسجيلات',
        backgroundColor: '#6366f1',
        borderRadius: 8,
        hoverBackgroundColor: '#818cf8',
      }
    ]
  };

  public barChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      y: { grid: { color: '#1e293b' }, ticks: { color: '#64748b' } },
      x: { grid: { display: false }, ticks: { color: '#64748b' } }
    }
  };

  constructor(
    private api: ApiService, 
    private toastr: ToastrService,
    public categoryState: CategoryStateService
  ) { }

  ngOnInit(): void {
    this.loadData();
    this.categorySub = this.categoryState.selectedCategory$.subscribe(() => {
      this.loadData();
    });
  }

  ngOnDestroy(): void {
    this.categorySub?.unsubscribe();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.initMap(), 800);
  }

  loadData() {
    this.api.getDashboardAnalytics().subscribe({
      next: (res: any) => {
        const data = res?.data;
        this.stats.set({
          activeStudents: data?.totalActiveStudents || 0,
          pendingStudents: data?.pendingStudents || 0,
          newStudentsThisWeek: data?.newStudentsThisWeek || 0,
          quizAttempts: data?.totalQuizAttempts || 0,
          quizPassRate: data?.quizPassRate || 0
        } as any);

        this.topStudents.set(
          (data?.topStudents || []).map((s: any, index: number) => ({
            rank: index + 1,
            name: s.studentName,
            score: s.avgPercentage
          }))
        );

        const weekly = data?.weeklyRegistrations || [];
        this.barChartData = {
          labels: weekly.map((r: any) => r.date),
          datasets: [{
            ...this.barChartData.datasets[0],
            data: weekly.map((r: any) => r.count)
          }]
        };
      }
    });

    this.api.getPendingStudents(0, 3).subscribe({
      next: (res: any) => {
        const list = res?.content ?? res?.data?.content ?? [];
        this.pendingStudents.set(list);
      }
    });
  }

  initMap() {
    const container = document.getElementById('map');
    if (!container) return;

    if (this.map) {
      this.map.remove();
      this.map = null;
    }

    this.map = L.map('map', {
      center: [26.8206, 30.8025],
      zoom: 6,
      zoomControl: false
    });

    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    L.control.zoom({ position: 'bottomleft' }).addTo(this.map);

    const locations = [
      { name: 'أحمد علي', lat: 30.0444, lng: 31.2357, city: 'القاهرة' },
      { name: 'سارة محمد', lat: 31.2001, lng: 29.9187, city: 'الإسكندرية' },
      { name: 'محمود حسن', lat: 27.1783, lng: 31.1859, city: 'أسيوط' },
      { name: 'ليلى يوسف', lat: 24.0889, lng: 32.8998, city: 'أسوان' },
    ];

    locations.forEach(loc => {
      L.circleMarker([loc.lat, loc.lng], {
        radius: 8,
        fillColor: '#6366f1',
        color: '#fff',
        weight: 2,
        fillOpacity: 0.8
      })
        .addTo(this.map)
        .bindPopup(`
        <div class="p-2 text-right">
          <strong>${loc.name}</strong><br/>
          <span>${loc.city}</span>
        </div>
      `);
    });
  }

  approveStudent(id: number) {
    this.api.approveStudent(id).subscribe({
      next: () => {
        this.toastr.success('تم قبول الطالب');
        this.loadData();
      }
    });
  }

  rejectStudent(id: number) {
    const reason = prompt('سبب الرفض:');
    if (!reason) return;

    this.api.rejectStudent(id, reason).subscribe({
      next: () => {
        this.toastr.success('تم رفض الطالب');
        this.loadData();
      }
    });
  }
}
