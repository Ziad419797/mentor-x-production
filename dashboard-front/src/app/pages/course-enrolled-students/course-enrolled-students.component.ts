import { Component, OnInit, signal } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Enrollment } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

type TypeFilter = '' | 'online' | 'center';

interface EnrichedEnrollment extends Enrollment {
  governorate?: string;
  centerName?: string;
  online?: boolean;
  studentCode?: string;
  parentPhone?: string;
  groupName?: string;
  enrollmentType?: string;
  createdBy?: string;
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
        <!-- Action Buttons -->
        <div class="flex items-center gap-2">
          <button (click)="exportExcel()"
            class="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold transition-colors">
            <span class="material-icons-round text-sm">download</span> تصدير Excel
          </button>
          <button (click)="showAddModal = true"
            class="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold transition-colors">
            <span class="material-icons-round text-sm">person_add</span> إضافة طالب
          </button>
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
        <button *ngIf="searchQuery || filterGovernorate || filterType || filterCenter || filterGroup || filterAdminOnly"
          (click)="clearFilters()" title="مسح الفلاتر"
          class="flex items-center justify-center rounded-lg bg-slate-800 text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-colors border border-slate-700"
          style="height:30px;width:30px;flex-shrink:0">
          <span class="material-icons-round" style="font-size:14px">filter_alt_off</span>
        </button>
        <button (click)="filterAdminOnly = !filterAdminOnly; applyFilters()"
          [class]="filterAdminOnly ? 'bg-amber-500/20 text-amber-400 border-amber-500/40' : 'bg-slate-800 text-slate-400 border-slate-700'"
          class="flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-bold border transition-colors"
          style="height:30px;flex-shrink:0">
          <span class="material-icons-round" style="font-size:13px">admin_panel_settings</span>
          مضافين يدوياً
        </button>
        <span class="text-slate-500" style="font-size:11px">{{ filteredEnrollments().length }}</span>
      </div>

      <!-- Table -->
      <div class="edu-card p-0 overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full text-xs">
            <thead class="bg-slate-800/70 border-b border-slate-700">
              <tr>
                <th class="px-3 py-3 text-right font-bold text-slate-400 cursor-pointer select-none hover:text-white transition-colors" (click)="sortBy('studentCode')">
                  كود الطالب <span class="text-[10px] opacity-60">{{ sortKey==='studentCode' ? (sortDir==='asc'?'▲':'▼') : '⇅' }}</span>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 min-w-[160px] cursor-pointer select-none hover:text-white transition-colors" (click)="sortBy('studentName')">
                  الطالب <span class="text-[10px] opacity-60">{{ sortKey==='studentName' ? (sortDir==='asc'?'▲':'▼') : '⇅' }}</span>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden md:table-cell cursor-pointer select-none hover:text-white transition-colors" (click)="sortBy('studentPhone')">
                  الهاتف <span class="text-[10px] opacity-60">{{ sortKey==='studentPhone' ? (sortDir==='asc'?'▲':'▼') : '⇅' }}</span>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden md:table-cell cursor-pointer select-none hover:text-white transition-colors" (click)="sortBy('parentPhone')">
                  هاتف ولي الأمر <span class="text-[10px] opacity-60">{{ sortKey==='parentPhone' ? (sortDir==='asc'?'▲':'▼') : '⇅' }}</span>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell cursor-pointer select-none hover:text-white transition-colors" (click)="sortBy('governorate')">
                  المحافظة <span class="text-[10px] opacity-60">{{ sortKey==='governorate' ? (sortDir==='asc'?'▲':'▼') : '⇅' }}</span>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 cursor-pointer select-none hover:text-white transition-colors" (click)="sortBy('online')">
                  النوع <span class="text-[10px] opacity-60">{{ sortKey==='online' ? (sortDir==='asc'?'▲':'▼') : '⇅' }}</span>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell cursor-pointer select-none hover:text-white transition-colors" (click)="sortBy('centerName')">
                  السنتر <span class="text-[10px] opacity-60">{{ sortKey==='centerName' ? (sortDir==='asc'?'▲':'▼') : '⇅' }}</span>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell cursor-pointer select-none hover:text-white transition-colors" (click)="sortBy('groupName')">
                  الجروب <span class="text-[10px] opacity-60">{{ sortKey==='groupName' ? (sortDir==='asc'?'▲':'▼') : '⇅' }}</span>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 cursor-pointer select-none hover:text-white transition-colors" (click)="sortBy('enrolledAt')">
                  تاريخ الاشتراك <span class="text-[10px] opacity-60">{{ sortKey==='enrolledAt' ? (sortDir==='asc'?'▲':'▼') : '⇅' }}</span>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400">إجراءات</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngIf="loading()">
                <td colspan="10" class="text-center py-10 text-slate-500">
                  <span class="material-icons-round animate-spin text-2xl">refresh</span>
                </td>
              </tr>
              <tr *ngIf="!loading() && filteredEnrollments().length === 0">
                <td colspan="10" class="text-center py-14 text-slate-500">
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
                <td class="px-3 py-2.5">
                  <div class="flex items-center gap-1">
                    <span *ngIf="e.enrollmentType === 'ADMIN_GRANT'"
                      class="px-1.5 py-0.5 rounded text-[9px] font-bold bg-amber-500/15 text-amber-400 border border-amber-500/20">
                      يدوي
                    </span>
                    <button (click)="removeEnrollment(e)" [disabled]="removingId === e.id"
                      class="flex items-center justify-center w-7 h-7 rounded-lg bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors disabled:opacity-40"
                      title="حذف من الكورس">
                      <span class="material-icons-round" style="font-size:14px">{{ removingId === e.id ? 'refresh' : 'person_remove' }}</span>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- ══════════════════════════════════════════════
         ADD STUDENTS MODAL
    ══════════════════════════════════════════════ -->
    <div *ngIf="showAddModal"
         class="fixed inset-0 z-50 flex items-center justify-center p-4"
         style="background:rgba(0,0,0,0.7)" (click)="closeAddModal($event)">
      <div class="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-4xl max-h-[90vh] overflow-y-auto shadow-2xl" (click)="$event.stopPropagation()">

        <!-- Modal Header -->
        <div class="flex items-center justify-between p-5 border-b border-slate-700">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-indigo-600 flex items-center justify-center">
              <span class="material-icons-round text-white text-lg">person_add</span>
            </div>
            <div>
              <h3 class="text-white font-black text-base">إضافة طالب للكورس</h3>
              <p class="text-slate-500 text-xs">إضافة طالب يدوياً أو عن طريق ملف إكسل</p>
            </div>
          </div>
          <button (click)="showAddModal = false" class="w-8 h-8 rounded-lg bg-slate-800 flex items-center justify-center text-slate-400 hover:text-white transition-colors">
            <span class="material-icons-round text-base">close</span>
          </button>
        </div>

        <!-- Modal Body — 3 columns -->
        <div class="p-5 grid grid-cols-1 lg:grid-cols-3 gap-4">

          <!-- ── Col 1: Add by Code (ID) ── -->
          <div class="bg-slate-800/50 border border-slate-700 rounded-xl p-4 space-y-3">
            <div class="flex items-center gap-2 mb-1">
              <span class="material-icons-round text-orange-400 text-lg">badge</span>
              <div>
                <p class="text-white text-sm font-bold">إضافة بكود الطالب (ID)</p>
                <p class="text-slate-500 text-[10px]">أدخل كود الطالب</p>
              </div>
            </div>
            <div>
              <label class="text-[10px] text-slate-400 block mb-1">كود الطالب (ID)</label>
              <input type="text" [(ngModel)]="addByCode" placeholder="أدخل كود الطالب"
                class="w-full px-3 py-2 bg-slate-900 border border-slate-600 rounded-lg text-white text-xs outline-none focus:border-indigo-500 transition-colors" dir="ltr">
            </div>
            <!-- Preview -->
            <div *ngIf="previewByCode" class="bg-slate-900 border border-slate-600 rounded-lg p-2.5 text-xs">
              <p class="text-white font-bold">{{ previewByCode.firstName }} {{ previewByCode.secondName }}</p>
              <p class="text-emerald-400 text-[10px] font-mono">{{ previewByCode.studentCode || previewByCode.code }}</p>
              <p class="text-slate-400" dir="ltr">{{ previewByCode.phone }}</p>
              <p class="text-slate-500">{{ previewByCode.grade }}</p>
            </div>
            <p *ngIf="errByCode" class="text-red-400 text-[10px]">{{ errByCode }}</p>
            <div class="flex gap-2">
              <button (click)="searchByCode()" [disabled]="!addByCode || loadingByCode"
                class="flex-1 py-2 bg-slate-700 hover:bg-slate-600 text-white text-xs font-bold rounded-lg transition-colors disabled:opacity-40">
                <span *ngIf="loadingByCode" class="material-icons-round animate-spin text-xs">refresh</span>
                <span *ngIf="!loadingByCode">بحث</span>
              </button>
              <button (click)="enrollByCode()" [disabled]="!previewByCode || loadingByCode"
                class="flex-1 py-2 bg-orange-600 hover:bg-orange-500 text-white text-xs font-bold rounded-lg transition-colors disabled:opacity-40">
                + إضافة
              </button>
            </div>
          </div>

          <!-- ── Col 2: Add by Phone ── -->
          <div class="bg-slate-800/50 border border-slate-700 rounded-xl p-4 space-y-3">
            <div class="flex items-center gap-2 mb-1">
              <span class="material-icons-round text-blue-400 text-lg">phone</span>
              <div>
                <p class="text-white text-sm font-bold">إضافة برقم الموبايل (Phone)</p>
                <p class="text-slate-500 text-[10px]">أدخل رقم هاتف الطالب</p>
              </div>
            </div>
            <div>
              <label class="text-[10px] text-slate-400 block mb-1">رقم الموبايل (PHONE)</label>
              <input type="text" [(ngModel)]="addByPhone" placeholder="أدخل رقم الموبايل"
                class="w-full px-3 py-2 bg-slate-900 border border-slate-600 rounded-lg text-white text-xs outline-none focus:border-indigo-500 transition-colors" dir="ltr">
            </div>
            <!-- Preview -->
            <div *ngIf="previewByPhone" class="bg-slate-900 border border-slate-600 rounded-lg p-2.5 text-xs">
              <p class="text-white font-bold">{{ previewByPhone.firstName }} {{ previewByPhone.secondName }}</p>
              <p class="text-emerald-400 text-[10px] font-mono">{{ previewByPhone.studentCode || previewByPhone.code }}</p>
              <p class="text-slate-400" dir="ltr">{{ previewByPhone.phone }}</p>
              <p class="text-slate-500">{{ previewByPhone.grade }}</p>
            </div>
            <p *ngIf="errByPhone" class="text-red-400 text-[10px]">{{ errByPhone }}</p>
            <div class="flex gap-2">
              <button (click)="searchByPhone()" [disabled]="!addByPhone || loadingByPhone"
                class="flex-1 py-2 bg-slate-700 hover:bg-slate-600 text-white text-xs font-bold rounded-lg transition-colors disabled:opacity-40">
                <span *ngIf="loadingByPhone" class="material-icons-round animate-spin text-xs">refresh</span>
                <span *ngIf="!loadingByPhone">بحث</span>
              </button>
              <button (click)="enrollByPhone()" [disabled]="!previewByPhone || loadingByPhone"
                class="flex-1 py-2 bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold rounded-lg transition-colors disabled:opacity-40">
                + إضافة
              </button>
            </div>
          </div>

          <!-- ── Col 3: Add Entire Group ── -->
          <div class="bg-slate-800/50 border border-slate-700 rounded-xl p-4 space-y-3">
            <div class="flex items-center gap-2 mb-1">
              <span class="material-icons-round text-green-400 text-lg">groups</span>
              <div>
                <p class="text-white text-sm font-bold">إضافة مجموعة كاملة</p>
                <p class="text-slate-500 text-[10px]">إضافة جميع طلاب المجموعة</p>
              </div>
            </div>
            <div>
              <label class="text-[10px] text-slate-400 block mb-1">اختر المجموعة</label>
              <select [(ngModel)]="addGroupId" (ngModelChange)="onAddGroupChange()"
                class="w-full px-3 py-2 bg-slate-900 border border-slate-600 rounded-lg text-white text-xs outline-none focus:border-indigo-500 transition-colors">
                <option value="">-- اختر المجموعة --</option>
                <option *ngFor="let g of groupsForLevel" [value]="g.id">{{ (g.centerName ? g.centerName + ' — ' : '') + (g.title || g.name) }}</option>
              </select>
            </div>
            <div *ngIf="addGroupMembersCount !== null" class="bg-slate-900 border border-slate-600 rounded-lg p-2.5 text-xs text-center">
              <p class="text-slate-300">عدد الطلاب في المجموعة</p>
              <p class="text-white font-black text-xl">{{ addGroupMembersCount }}</p>
            </div>
            <p *ngIf="errByGroup" class="text-red-400 text-[10px]">{{ errByGroup }}</p>
            <button (click)="enrollGroup()" [disabled]="!addGroupId || loadingGroup"
              class="w-full py-2 bg-green-600 hover:bg-green-500 text-white text-xs font-bold rounded-lg transition-colors disabled:opacity-40 flex items-center justify-center gap-1.5">
              <span *ngIf="loadingGroup" class="material-icons-round animate-spin text-xs">refresh</span>
              <span class="material-icons-round text-sm" *ngIf="!loadingGroup">groups</span>
              إضافة المجموعة
            </button>
          </div>
        </div>

        <!-- ── Excel Upload Row ── -->
        <div class="px-5 pb-5">
          <div class="bg-slate-800/50 border border-slate-700 rounded-xl p-4">
            <div class="flex items-center gap-2 mb-3">
              <span class="material-icons-round text-emerald-400 text-lg">table_view</span>
              <div>
                <p class="text-white text-sm font-bold">إضافة طلاب من ملف Excel</p>
                <p class="text-slate-500 text-[10px]">ارفع ملف XLS أو XLSX يحتوي على أكواد أو أرقام هواتف الطلاب</p>
              </div>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3 items-end">
              <!-- Template Downloads -->
              <div class="space-y-2">
                <p class="text-[10px] text-slate-400">تحميل ملف نموذجي:</p>
                <div class="flex gap-2">
                  <button (click)="downloadTemplate('code')"
                    class="flex-1 flex items-center justify-center gap-1 py-2 bg-green-700 hover:bg-green-600 text-white text-[10px] font-bold rounded-lg transition-colors">
                    <span class="material-icons-round text-xs">download</span> تحميل نموذج ID
                  </button>
                  <button (click)="downloadTemplate('phone')"
                    class="flex-1 flex items-center justify-center gap-1 py-2 bg-blue-700 hover:bg-blue-600 text-white text-[10px] font-bold rounded-lg transition-colors">
                    <span class="material-icons-round text-xs">download</span> تحميل نموذج Phone
                  </button>
                </div>
              </div>
              <!-- File Input -->
              <div>
                <label class="text-[10px] text-slate-400 block mb-1">رفع ملف XLS أو XLSX</label>
                <label class="flex items-center gap-2 px-3 py-2 bg-slate-900 border border-slate-600 rounded-lg cursor-pointer hover:border-indigo-500 transition-colors">
                  <span class="material-icons-round text-slate-400 text-sm">attach_file</span>
                  <span class="text-slate-400 text-xs flex-1 truncate">{{ excelFile ? excelFile.name : 'اختر ملف...' }}</span>
                  <input type="file" accept=".xlsx,.xls,.csv" class="hidden" (change)="onExcelFileSelected($event)">
                </label>
              </div>
              <!-- Upload Actions -->
              <div class="space-y-2">
                <div class="flex gap-2">
                  <select [(ngModel)]="excelMode" class="flex-1 px-2 py-2 bg-slate-900 border border-slate-600 rounded-lg text-white text-xs outline-none">
                    <option value="code">بالكود (ID)</option>
                    <option value="phone">بالهاتف (Phone)</option>
                  </select>
                  <button (click)="uploadExcel()" [disabled]="!excelFile || loadingExcel"
                    class="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-lg transition-colors disabled:opacity-40">
                    <span *ngIf="loadingExcel" class="material-icons-round animate-spin text-xs">refresh</span>
                    <span *ngIf="!loadingExcel">رفع الملف</span>
                  </button>
                </div>
                <p *ngIf="excelResult" class="text-[10px]" [ngClass]="excelResult.includes('خطأ') ? 'text-red-400' : 'text-emerald-400'">{{ excelResult }}</p>
              </div>
            </div>
          </div>
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
  get groupsForLevel() { return !this.courseLevelId ? this.groups : this.groups.filter(g => !g.levelId || g.levelId === this.courseLevelId); }
  groupsForSelectedCenter: any[] = [];
  courseLevelId: number | null = null;

  private _filtered = signal<EnrichedEnrollment[]>([]);
  filteredEnrollments() { return this._filtered(); }

  // ── Sorting ──────────────────────────────────────────────
  filterAdminOnly = false;
  sortKey = '';
  sortDir: 'asc' | 'desc' = 'asc';

  sortBy(key: string) {
    this.sortKey === key ? (this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc') : (this.sortKey = key, this.sortDir = 'asc');
    this.applyFilters();
  }

  // ── Add Modal ─────────────────────────────────────────────
  showAddModal = false;

  // by code
  addByCode = '';
  previewByCode: any = null;
  errByCode = '';
  loadingByCode = false;

  // by phone
  addByPhone = '';
  previewByPhone: any = null;
  errByPhone = '';
  loadingByPhone = false;

  // by group
  addGroupId: any = '';
  addGroupMembers: any[] = [];
  addGroupMembersCount: number | null = null;
  errByGroup = '';
  loadingGroup = false;

  // excel
  excelFile: File | null = null;
  excelMode: 'code' | 'phone' = 'code';
  loadingExcel = false;
  excelResult = '';

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
    this.loadEnrollments();
    this.api.getCourseLevelId(id).subscribe({
      next: (r: any) => {
        this.courseLevelId = r?.levelId ?? null;
        this.loadFilterOptions();
      },
      error: () => { this.loadFilterOptions(); }
    });
  }

  goBack() { this.location.back(); }
  closeAddModal(e: Event) { if (e.target === e.currentTarget) this.showAddModal = false; }

  // ── Filter logic ──────────────────────────────────────────
  onTypeChanged() {
    if (this.filterType === 'online') { this.filterCenter = ''; this.filterGroup = ''; this.groupsForSelectedCenter = []; }
    this.applyFilters();
  }
  onCenterChanged() { this.filterGroup = ''; this.refreshGroupsForCenter(); this.applyFilters(); }
  onFiltersChanged() { this.applyFilters(); }
  clearFilters() {
    this.searchQuery = ''; this.filterGovernorate = ''; this.filterType = '';
    this.filterCenter = ''; this.filterGroup = ''; this.groupsForSelectedCenter = []; this.filterAdminOnly = false;
    this.applyFilters();
  }

  refreshGroupsForCenter() {
    if (!this.filterCenter) { this.groupsForSelectedCenter = []; return; }
    const center = this.centers.find(c => c.name === this.filterCenter);
    this.groupsForSelectedCenter = this.groups.filter(g => {
      const matchesCenter = (center && (g.centerId === center.id || g.centerId === center.centerId)) || g.centerName === this.filterCenter;
      const matchesLevel  = !this.courseLevelId || !g.levelId || g.levelId === this.courseLevelId;
      return matchesCenter && matchesLevel;
    });
  }

  loadFilterOptions() {
    this.api.getCenters().subscribe({ next: (r: any) => this.centers = Array.isArray(r) ? r : (r?.data || []), error: () => {} });
    this.api.getGovernorateNames().subscribe({ next: (r: any) => this.governorateNames = Array.isArray(r) ? r : [], error: () => {} });
    this.api.getMyGroups(this.courseLevelId ?? undefined).subscribe({ next: (r: any) => { this.groups = Array.isArray(r) ? r : []; }, error: () => {} });
  }

  applyFilters() {
    const q = this.searchQuery.trim().toLowerCase();
    const filtered = this.enrollments().filter(e => {
      if (this.filterGovernorate && (e.governorate || '') !== this.filterGovernorate) return false;
      if (this.filterType === 'online' && e.online !== true) return false;
      if (this.filterType === 'center' && e.online !== false) return false;
      if (this.filterCenter && (e.centerName || '') !== this.filterCenter) return false;
      if (this.filterAdminOnly && e.enrollmentType !== 'ADMIN_GRANT') return false;
      if (this.filterGroup && (e.groupName || '') !== this.filterGroup) return false;
      if (q) {
        const hay = `${e.studentName ?? ''} ${e.studentCode ?? ''} ${e.studentPhone ?? ''}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
    if (this.sortKey) {
      const dir = this.sortDir === 'asc' ? 1 : -1;
      filtered.sort((a: any, b: any) => {
        let va = a[this.sortKey] ?? '', vb = b[this.sortKey] ?? '';
        if (this.sortKey === 'enrolledAt') {
          va = va ? new Date(va).getTime() : 0;
          vb = vb ? new Date(vb).getTime() : 0;
          return (va - vb) * dir;
        }
        return String(va).localeCompare(String(vb), 'ar') * dir;
      });
    }
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
          return { ...e, studentCode: match.studentCode || match.code, parentPhone: match.parentPhone, governorate: match.governorate, centerName: match.centerName, groupName: match.groupName, online: match.online, enrollmentType: (e as any).enrollmentType, createdBy: (e as any).createdBy };
        });
        this.enrollments.set(updated);
        this.applyFilters();
      },
      error: () => {}
    });
  }

  // ══════════════════════════════════════════════
  // ADD STUDENTS LOGIC
  // ══════════════════════════════════════════════

  searchByCode() {
    this.previewByCode = null; this.errByCode = '';
    if (!this.addByCode.trim()) return;
    this.loadingByCode = true;
    this.api.searchStudentByCode(this.addByCode.trim()).subscribe({
      next: (s: any) => { this.previewByCode = s; this.loadingByCode = false; },
      error: () => { this.errByCode = 'لم يتم العثور على الطالب'; this.loadingByCode = false; }
    });
  }

  enrollByCode() {
    if (!this.previewByCode?.id) return;
    this.loadingByCode = true;
    this.api.manualEnroll({ studentId: this.previewByCode.id, courseId: this.courseId() }).subscribe({
      next: () => {
        this.toastr.success(`تم تسجيل ${this.previewByCode.firstName} بنجاح`);
        this.addByCode = ''; this.previewByCode = null; this.loadingByCode = false;
        this.loadEnrollments();
      },
      error: (e: any) => {
        this.errByCode = e?.error?.message || 'حدث خطأ أثناء التسجيل';
        this.loadingByCode = false;
      }
    });
  }

  searchByPhone() {
    this.previewByPhone = null; this.errByPhone = '';
    if (!this.addByPhone.trim()) return;
    this.loadingByPhone = true;
    this.api.searchStudentByPhone(this.addByPhone.trim()).subscribe({
      next: (s: any) => { this.previewByPhone = s; this.loadingByPhone = false; },
      error: () => { this.errByPhone = 'لم يتم العثور على الطالب'; this.loadingByPhone = false; }
    });
  }

  enrollByPhone() {
    if (!this.previewByPhone?.id) return;
    this.loadingByPhone = true;
    this.api.manualEnroll({ studentId: this.previewByPhone.id, courseId: this.courseId() }).subscribe({
      next: () => {
        this.toastr.success(`تم تسجيل ${this.previewByPhone.firstName} بنجاح`);
        this.addByPhone = ''; this.previewByPhone = null; this.loadingByPhone = false;
        this.loadEnrollments();
      },
      error: (e: any) => {
        this.errByPhone = e?.error?.message || 'حدث خطأ أثناء التسجيل';
        this.loadingByPhone = false;
      }
    });
  }

  onAddGroupChange() {
    this.addGroupMembers = []; this.addGroupMembersCount = null; this.errByGroup = '';
    if (!this.addGroupId) return;
    this.api.getGroupMembers(Number(this.addGroupId)).subscribe({
      next: (members: any[]) => { this.addGroupMembers = members; this.addGroupMembersCount = members.length; },
      error: () => { this.addGroupMembersCount = null; }
    });
  }

  enrollGroup() {
    if (!this.addGroupId || !this.addGroupMembers.length) { this.errByGroup = 'لا يوجد طلاب في هذه المجموعة'; return; }
    this.loadingGroup = true;
    this.errByGroup = '';
    const courseId = this.courseId();
    const requests = this.addGroupMembers
      .filter(m => m.studentId || m.id)
      .map(m => this.api.manualEnroll({ studentId: m.studentId || m.id, courseId }).pipe(catchError(() => of(null))));

    if (!requests.length) { this.errByGroup = 'لا يوجد طلاب صالحين في هذه المجموعة'; this.loadingGroup = false; return; }

    forkJoin(requests).subscribe({
      next: (results: any[]) => {
        const ok = results.filter(r => r !== null).length;
        const skip = results.length - ok;
        this.toastr.success(`تم تسجيل ${ok} طالب${skip > 0 ? ` (${skip} مكرر أو غير صالح)` : ''}`);
        this.addGroupId = ''; this.addGroupMembers = []; this.addGroupMembersCount = null;
        this.loadingGroup = false;
        this.loadEnrollments();
      },
      error: () => { this.errByGroup = 'حدث خطأ أثناء التسجيل'; this.loadingGroup = false; }
    });
  }

  // ── Excel ──────────────────────────────────────────────────
  onExcelFileSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) { this.excelFile = file; this.excelResult = ''; }
  }

  downloadTemplate(type: 'code' | 'phone') {
    const header = type === 'code' ? 'student_code' : 'phone';
    const sample = type === 'code' ? 'ABC123' : '01012345678';
    const csv = `﻿${header}\n${sample}\n`;
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = `template_${type}.csv`; a.click();
    URL.revokeObjectURL(url);
  }

  async uploadExcel() {
    if (!this.excelFile) return;
    this.loadingExcel = true; this.excelResult = '';
    try {
      const text = await this.excelFile.text();
      const lines = text.split('\n').map(l => l.trim()).filter(l => l);
      // skip header row
      const values = lines.slice(1).map(l => l.replace(/[",\r\ufeff]/g, '').trim()).filter(v => v)
        .map(v => (this.excelMode === 'phone' && /^[1-9]\d{9}$/.test(v)) ? '0' + v : v);
      if (!values.length) { this.excelResult = 'خطأ: الملف فارغ أو غير صحيح'; this.loadingExcel = false; return; }

      const courseId = this.courseId();
      const lookups = values.map(v =>
        (this.excelMode === 'code'
          ? this.api.searchStudentByCode(v)
          : this.api.searchStudentByPhone(v)
        ).pipe(catchError(() => of(null)))
      );

      forkJoin(lookups).subscribe({
        next: (students: any[]) => {
          const found = students.filter(s => s?.id);
          if (!found.length) { this.excelResult = 'خطأ: لم يتم العثور على أي طالب من الملف'; this.loadingExcel = false; return; }
          const enrolls = found.map(s => this.api.manualEnroll({ studentId: s.id, courseId }).pipe(catchError(() => of(null))));
          forkJoin(enrolls).subscribe({
            next: (results: any[]) => {
              const ok = results.filter(r => r !== null).length;
              this.excelResult = `تم تسجيل ${ok} من أصل ${values.length} طالب بنجاح`;
              this.excelFile = null; this.loadingExcel = false;
              this.loadEnrollments();
            },
            error: () => { this.excelResult = 'خطأ: حدث خطأ أثناء التسجيل'; this.loadingExcel = false; }
          });
        },
        error: () => { this.excelResult = 'خطأ: تعذر قراءة بيانات الطلاب'; this.loadingExcel = false; }
      });
    } catch {
      this.excelResult = 'خطأ: تعذر قراءة الملف'; this.loadingExcel = false;
    }
  }

  // ── Remove Enrollment ─────────────────────────────────────
  removingId: number | null = null;
  removeEnrollment(e: EnrichedEnrollment) {
    if (!confirm(`هل تريد حذف ${e.studentName} من الكورس؟`)) return;
    const id = (e as any).id;
    this.removingId = id;
    this.api.deleteEnrollment(id).subscribe({
      next: () => {
        this.toastr.success(`تم حذف ${e.studentName} من الكورس`);
        this.removingId = null;
        this.loadEnrollments();
      },
      error: (err: any) => {
        this.toastr.error(err?.error?.message || 'حدث خطأ أثناء الحذف');
        this.removingId = null;
      }
    });
  }

  // ── Export Excel ───────────────────────────────────────────
  exportExcel() {
    const headers = ['كود الطالب','الاسم','الهاتف','هاتف ولي الأمر','المحافظة','النوع','السنتر','الجروب','تاريخ الاشتراك'];
    const rows = this.filteredEnrollments().map(e => [
      e.studentCode || '',
      e.studentName || '',
      e.studentPhone || '',
      e.parentPhone || '',
      e.governorate || '',
      e.online ? 'أونلاين' : 'سنتر',
      e.centerName || '',
      e.groupName || '',
      e.enrolledAt ? new Date(e.enrolledAt as any).toLocaleString('ar-EG') : ''
    ]);
    const csv = '﻿' + [headers, ...rows].map(r => r.map(c => `"${String(c).replace(/"/g,'""')}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    const date = new Date().toISOString().slice(0,10);
    a.href = url; a.download = `enrolled_${this.courseId()}_${date}.csv`; a.click();
    URL.revokeObjectURL(url);
    this.toastr.success(`تم تصدير ${this.filteredEnrollments().length} طالب`);
  }
}
