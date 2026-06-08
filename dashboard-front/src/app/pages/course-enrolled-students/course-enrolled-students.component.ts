import { Component, OnInit, signal } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Enrollment } from '../../models/models';
import { ToastrService } from 'ngx-toastr';

type TypeFilter = '' | 'online' | 'center';

interface EnrichedEnrollment extends Enrollment {
  governorate?: string;
  centerName?: string;
  online?: boolean;
  studentCode?: string;
  parentPhone?: string;
  groupName?: string;
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
          <input type="text" [(ngModel)]="searchQuery" (ngModelChange)="onFiltersChanged()"
            class="filter-search" placeholder="اسم / كود / هاتف الطالب...">
        </div>
        <select [(ngModel)]="filterGovernorate" (ngModelChange)="onFiltersChanged()" class="filter-input" style="width:95px">
          <option value="">المحافظة</option>
          <option *ngFor="let g of governorateNames" [value]="g">{{ g }}</option>
        </select>
        <select [(ngModel)]="filterType" (ngModelChange)="onTypeChanged()" class="filter-input" style="width:80px">
          <option value="">النوع</option>
          <option value="online">أونلاين</option>
          <option value="center">سنتر</option>
        </select>
        <select [(ngModel)]="filterCenter" (ngModelChange)="onCenterChanged()" class="filter-input" style="width:95px">
          <option value="">السنتر</option>
          <option *ngFor="let c of centers" [value]="c.name">{{ c.name }}</option>
        </select>
        <select *ngIf="filterCenter" [(ngModel)]="filterGroup" (ngModelChange)="onFiltersChanged()" class="filter-input" style="width:95px">
          <option value="">الجروب</option>
          <option *ngFor="let g of groupsForSelectedCenter" [value]="g.title || g.name">{{ g.title || g.name }}</option>
        </select>
        <button *ngIf="searchQuery || filterGovernorate || filterType || filterCenter || filterGroup"
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
                <th class="px-3 py-3 text-right font-bold text-slate-400">كود الطالب</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 min-w-[160px]">الطالب</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden md:table-cell">الهاتف</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden md:table-cell">هاتف ولي الأمر</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell">المحافظة</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400">النوع</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell">السنتر</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell">الجروب</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400">تاريخ الاشتراك</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngIf="loading()">
                <td colspan="9" class="text-center py-10 text-slate-500">
                  <span class="material-icons-round animate-spin text-2xl">refresh</span>
                </td>
              </tr>
              <tr *ngIf="!loading() && filteredEnrollments().length === 0">
                <td colspan="9" class="text-center py-14 text-slate-500">
                  <span class="material-icons-round text-4xl opacity-20 block mb-2">people_outline</span>
                  لا يوجد طلبة مشتركين في هذا الكورس
                </td>
              </tr>
              <tr *ngFor="let e of filteredEnrollments()"
                class="border-b border-slate-800/40 hover:bg-slate-800/20 transition-colors">

                <td class="px-3 py-2.5">
                  <span class="text-slate-400 font-bold" dir="ltr">{{ e.studentCode || '—' }}</span>
                </td>

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

                <td class="px-3 py-2.5 hidden md:table-cell">
                  <span class="text-slate-400" dir="ltr">{{ e.parentPhone || '—' }}</span>
                </td>

                <td class="px-3 py-2.5 hidden lg:table-cell">
                  <span class="text-slate-400">{{ e.governorate || '—' }}</span>
                </td>

                <td class="px-3 py-2.5">
                  <span class="px-2 py-0.5 rounded-full text-[10px] font-bold"
                        [ngClass]="e.online ? 'bg-sky-500/15 text-sky-400' : 'bg-violet-500/15 text-violet-400'">
                    {{ e.online ? 'أونلاين' : 'سنتر' }}
                  </span>
                </td>

                <td class="px-3 py-2.5 hidden lg:table-cell">
                  <span class="text-slate-400">{{ e.centerName || '—' }}</span>
                </td>

                <td class="px-3 py-2.5 hidden lg:table-cell">
                  <span class="text-slate-400">{{ !e.online ? (e.groupName || '—') : '—' }}</span>
                </td>

                <td class="px-3 py-2.5">
                  <span class="text-slate-400" dir="ltr">{{ (e.enrolledAt) | date:'d/M/yy - h:mm a' }}</span>
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
  filterGovernorate = '';
  filterType: TypeFilter = '';
  filterCenter = '';
  filterGroup = '';

  governorateNames: string[] = [];
  centers: any[] = [];
  groups: any[] = [];
  groupsForSelectedCenter: any[] = [];

  // نسخة من القائمة بعد الفلترة — بنحدّثها يدوياً عند أي تغيير في الفلاتر/البحث
  // (مش computed لأن قيم الفلاتر مش signals وبتتغير عن طريق ngModel)
  private _filtered = signal<EnrichedEnrollment[]>([]);
  filteredEnrollments() { return this._filtered(); }

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

  onTypeChanged() {
    // النوع "أونلاين" مفيهوش سنتر/جروب — بنصفّر اختيارات السنتر والجروب
    if (this.filterType === 'online') {
      this.filterCenter = '';
      this.filterGroup = '';
      this.groupsForSelectedCenter = [];
    }
    this.applyFilters();
  }

  onCenterChanged() {
    this.filterGroup = '';
    this.refreshGroupsForCenter();
    this.applyFilters();
  }

  onFiltersChanged() {
    this.applyFilters();
  }

  clearFilters() {
    this.searchQuery = '';
    this.filterGovernorate = '';
    this.filterType = '';
    this.filterCenter = '';
    this.filterGroup = '';
    this.groupsForSelectedCenter = [];
    this.applyFilters();
  }

  refreshGroupsForCenter() {
    if (!this.filterCenter) { this.groupsForSelectedCenter = []; return; }
    const center = this.centers.find(c => c.name === this.filterCenter);
    this.groupsForSelectedCenter = this.groups.filter(g =>
      (center && g.centerId === center.id) || g.centerName === this.filterCenter
    );
  }

  loadFilterOptions() {
    this.api.getCenters().subscribe({ next: (r: any) => this.centers = Array.isArray(r) ? r : (r?.data || []), error: () => {} });
    this.api.getGovernorateNames().subscribe({ next: (r: any) => this.governorateNames = Array.isArray(r) ? r : [], error: () => {} });
    // جروبات السنة الدراسية الحالية — بتيجي من /api/groups/my (الجروبات النشطة بتاعت المعلم)
    this.api.getMyGroups().subscribe({ next: (r: any) => this.groups = Array.isArray(r) ? r : [], error: () => {} });
  }

  applyFilters() {
    const q = this.searchQuery.trim().toLowerCase();
    const gov = this.filterGovernorate;
    const type = this.filterType;
    const center = this.filterCenter;
    const group = this.filterGroup;
    const filtered = this.enrollments().filter(e => {
      if (gov && (e.governorate || '') !== gov) return false;
      if (type === 'online' && e.online !== true) return false;
      if (type === 'center' && e.online !== false) return false;
      if (center && (e.centerName || '') !== center) return false;
      if (group && (e.groupName || '') !== group) return false;
      if (q) {
        const hay = `${e.studentName ?? ''} ${e.studentCode ?? ''} ${e.studentPhone ?? ''}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
    this._filtered.set(filtered);
  }

  loadEnrollments() {
    this.loading.set(true);
    this.enrollments.set([]);
    this.api.getEnrollmentsByCourse(this.courseId()).subscribe({
      next: (data: any) => {
        const list: EnrichedEnrollment[] = Array.isArray(data) ? data : (data?.content || []);
        this.enrollments.set(list);
        this.loading.set(false);
        this.applyFilters();
        this.enrichWithStudentInfo(list);
      },
      error: () => { this.loading.set(false); this.toastr.error('تعذر تحميل بيانات المشتركين'); }
    });
  }

  // إثراء بيانات المشتركين بكود الطالب وهاتف ولي الأمر والمحافظة والسنتر والجروب ونوع الدراسة (أونلاين/سنتر)
  // مش موجودين في رد الـ enrollments — فبنجيبهم من قائمة الطلبة ونربطهم بالـ studentId
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
          return {
            ...e,
            studentCode: match.studentCode || match.code,
            parentPhone: match.parentPhone,
            governorate: match.governorate,
            centerName:  match.centerName,
            groupName:   match.groupName,
            online:      match.online,
          };
        });
        this.enrollments.set(updated);
        this.applyFilters();
      },
      error: () => {}
    });
  }
}
