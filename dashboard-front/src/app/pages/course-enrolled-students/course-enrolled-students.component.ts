import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Enrollment } from '../../models/models';
import { ToastrService } from 'ngx-toastr';

type StatusFilter = '' | 'ACTIVE' | 'COMPLETED' | 'EXPIRED' | 'CANCELLED' | 'SUSPENDED';

interface EnrichedEnrollment extends Enrollment {
  governorate?: string;
  centerName?: string;
}

@Component({
  selector: 'app-course-enrolled-students',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="space-y-4 animate-fade-in pb-10" dir="rtl">

      <!-- Header -->
      <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
        <div class="flex items-center gap-3">
          <button (click)="goBack()"
                  class="w-9 h-9 rounded-xl bg-slate-800 flex items-center justify-center text-slate-400 hover:text-white hover:bg-slate-700 transition-colors">
            <span class="material-icons-round text-base">arrow_forward</span>
          </button>
          <div>
            <h2 class="text-white font-black text-xl">المشتركين في الكورس</h2>
            <p class="text-slate-500 text-xs mt-0.5">{{ courseTitle() }}</p>
          </div>
          <span class="bg-slate-700 text-slate-300 text-xs font-bold px-2 py-1 rounded-full">{{ filteredEnrollments().length }}</span>
        </div>
      </div>

      <!-- Search + Filters -->
      <div class="flex items-center gap-1.5 flex-wrap">
        <div class="relative">
          <span class="material-icons-round absolute right-2 top-1/2 -translate-y-1/2 text-slate-500" style="font-size:13px;pointer-events:none">search</span>
          <input type="text" [(ngModel)]="searchQuery"
            class="filter-search" placeholder="اسم / كود / هاتف الطالب...">
        </div>
        <select [(ngModel)]="filterStatus" class="filter-input" style="width:90px">
          <option value="">الحالة</option>
          <option value="ACTIVE">نشط</option>
          <option value="COMPLETED">مكتمل</option>
          <option value="EXPIRED">منتهي</option>
          <option value="CANCELLED">ملغي</option>
          <option value="SUSPENDED">موقوف</option>
        </select>
        <select [(ngModel)]="filterGovernorate" class="filter-input" style="width:95px">
          <option value="">المحافظة</option>
          <option *ngFor="let g of governorateNames" [value]="g">{{ g }}</option>
        </select>
        <select [(ngModel)]="filterCenter" class="filter-input" style="width:95px">
          <option value="">السنتر</option>
          <option *ngFor="let c of centers" [value]="c.name">{{ c.name }}</option>
        </select>
        <button *ngIf="searchQuery || filterStatus || filterGovernorate || filterCenter"
          (click)="clearFilters()" title="مسح الفلاتر"
          class="flex items-center justify-center rounded-lg bg-slate-800 text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-colors border border-slate-700"
          style="height:30px;width:30px;flex-shrink:0">
          <span class="material-icons-round" style="font-size:14px">filter_alt_off</span>
        </button>
        <span class="text-slate-500" style="font-size:11px">{{ filteredEnrollments().length }}</span>
      </div>

      <!-- Table -->
      <div class="edu-card p-0 overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full text-xs">
            <thead class="bg-slate-800/70 border-b border-slate-700">
              <tr>
                <th class="px-3 py-3 text-right font-bold text-slate-400 min-w-[160px]">الطالب</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden md:table-cell">الهاتف</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell">المحافظة</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell">السنتر</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400">الحالة</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400">تاريخ الاشتراك</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden md:table-cell">تاريخ الانتهاء</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngIf="loading()">
                <td colspan="7" class="text-center py-10 text-slate-500">
                  <span class="material-icons-round animate-spin text-2xl">refresh</span>
                </td>
              </tr>
              <tr *ngIf="!loading() && filteredEnrollments().length === 0">
                <td colspan="7" class="text-center py-14 text-slate-500">
                  <span class="material-icons-round text-4xl opacity-20 block mb-2">people_outline</span>
                  لا يوجد طلبة مشتركين في هذا الكورس
                </td>
              </tr>
              <tr *ngFor="let e of filteredEnrollments()"
                class="border-b border-slate-800/40 hover:bg-slate-800/20 transition-colors">

                <td class="px-3 py-2.5">
                  <div class="flex items-center gap-2 min-w-[150px]">
                    <div class="w-8 h-8 rounded-full flex-shrink-0 bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-xs font-black text-white">
                      {{ (e.studentName || '?')[0] }}
                    </div>
                    <span class="text-slate-200 font-semibold text-xs">{{ e.studentName || '—' }}</span>
                  </div>
                </td>

                <td class="px-3 py-2.5 hidden md:table-cell">
                  <span class="text-slate-400" dir="ltr">{{ e.studentPhone || '—' }}</span>
                </td>

                <td class="px-3 py-2.5 hidden lg:table-cell">
                  <span class="text-slate-400">{{ e.governorate || '—' }}</span>
                </td>

                <td class="px-3 py-2.5 hidden lg:table-cell">
                  <span class="text-slate-400">{{ e.centerName || '—' }}</span>
                </td>

                <td class="px-3 py-2.5">
                  <span [ngClass]="statusClass(e.status)"
                        class="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full border">
                    {{ statusLabel(e.status) }}
                  </span>
                </td>

                <td class="px-3 py-2.5">
                  <span class="text-slate-400">{{ (e.enrolledAt) | date:'d/M/yy' }}</span>
                </td>

                <td class="px-3 py-2.5 hidden md:table-cell">
                  <span class="text-slate-500">{{ (e.expiresAt || e.expiryDate) ? ((e.expiresAt || e.expiryDate) | date:'d/M/yy') : 'بدون انتهاء' }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

    </div>
  `
})
export class CourseEnrolledStudentsComponent implements OnInit {
  courseId    = signal(0);
  courseTitle = signal('');
  loading     = signal(false);
  enrollments = signal<EnrichedEnrollment[]>([]);

  searchQuery = '';
  filterStatus: StatusFilter = '';
  filterGovernorate = '';
  filterCenter = '';

  governorateNames: string[] = [];
  centers: any[] = [];

  filteredEnrollments = computed(() => {
    const q = this.searchQuery.trim().toLowerCase();
    const status = this.filterStatus;
    const gov = this.filterGovernorate;
    const center = this.filterCenter;
    return this.enrollments().filter(e => {
      if (status && e.status !== status) return false;
      if (gov && (e.governorate || '') !== gov) return false;
      if (center && (e.centerName || '') !== center) return false;
      if (q) {
        const hay = `${e.studentName ?? ''} ${e.studentPhone ?? ''}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  });

  constructor(
    private route: ActivatedRoute,
    private location: Location,
    private api: ApiService,
    private toastr: ToastrService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('courseId'));
    this.courseId.set(id);
    this.courseTitle.set(this.route.snapshot.queryParamMap.get('title') || '');

    this.loadFilterOptions();
    this.loadEnrollments();
  }

  goBack() { this.location.back(); }

  clearFilters() {
    this.searchQuery = '';
    this.filterStatus = '';
    this.filterGovernorate = '';
    this.filterCenter = '';
  }

  loadFilterOptions() {
    this.api.getCenters().subscribe({ next: (r: any) => this.centers = Array.isArray(r) ? r : (r?.data || []), error: () => {} });
    this.api.getGovernorateNames().subscribe({ next: (r: any) => this.governorateNames = Array.isArray(r) ? r : [], error: () => {} });
  }

  statusLabel(status?: string): string {
    switch (status) {
      case 'ACTIVE': return 'نشط';
      case 'COMPLETED': return 'مكتمل';
      case 'EXPIRED': return 'منتهي';
      case 'CANCELLED': return 'ملغي';
      case 'SUSPENDED': return 'موقوف';
      default: return status || '—';
    }
  }

  statusClass(status?: string): string {
    switch (status) {
      case 'ACTIVE': return 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30';
      case 'COMPLETED': return 'bg-indigo-500/20 text-indigo-400 border-indigo-500/30';
      case 'EXPIRED': return 'bg-slate-700 text-slate-400 border-slate-600';
      case 'CANCELLED': return 'bg-red-500/20 text-red-400 border-red-500/30';
      case 'SUSPENDED': return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30';
      default: return 'bg-slate-700 text-slate-400 border-slate-600';
    }
  }

  loadEnrollments() {
    this.loading.set(true);
    this.enrollments.set([]);
    this.api.getEnrollmentsByCourse(this.courseId()).subscribe({
      next: (data: any) => {
        const list: EnrichedEnrollment[] = Array.isArray(data) ? data : (data?.content || []);
        this.enrollments.set(list);
        this.loading.set(false);
        this.enrichWithStudentInfo(list);
      },
      error: () => { this.loading.set(false); this.toastr.error('تعذر تحميل بيانات المشتركين'); }
    });
  }

  // إثراء بيانات المشتركين بالمحافظة و السنتر — مش موجودة في رد الـ enrollments
  // فبنجيبها من قائمة الطلبة و نربطها بالـ studentId
  enrichWithStudentInfo(list: EnrichedEnrollment[]) {
    if (!list.length) return;
    this.api.getActiveStudents(0, 1000).subscribe({
      next: (page: any) => {
        const students: any[] = page?.content || [];
        if (!students.length) return;
        const byId = new Map<number, any>();
        for (const s of students) { if (s.id != null) byId.set(s.id, s); }
        const updated = list.map(e => {
          const match = e.studentId != null ? byId.get(e.studentId) : null;
          if (!match) return e;
          return { ...e, governorate: match.governorate, centerName: match.centerName };
        });
        this.enrollments.set(updated);
      },
      error: () => {}
    });
  }
}
