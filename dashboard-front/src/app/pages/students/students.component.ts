import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Student, WalletTransaction, AttendanceRecord, Enrollment, QuizAttempt } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { extractPage } from '../../core/api-response.model';
import { RouterModule } from '@angular/router';

type SortDir = 'asc' | 'desc' | null;
type TabType = 'PENDING' | 'ACTIVE' | 'BLOCKED' | 'REJECTED';
type PageMode = 'requests' | 'manage';

@Component({
  selector: 'app-students',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="space-y-4 animate-fade-in pb-10" dir="rtl">

      <!-- Header -->
      <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
        <div class="flex items-center gap-3">
          <h2 class="text-white font-black text-xl">{{ pageMode() === 'requests' ? 'طلبات التسجيل' : 'إدارة الطلاب' }}</h2>
          <span *ngIf="levelName()" class="bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 text-xs font-bold px-2 py-1 rounded-full">{{ levelName() }}</span>
          <span class="bg-slate-700 text-slate-300 text-xs font-bold px-2 py-1 rounded-full">{{ totalElements() }}</span>
        </div>
        <div class="flex items-center gap-2">
          <button *ngIf="pageMode() === 'requests' && activeTab() === 'PENDING' && displayedStudents().length > 0"
            (click)="autoApproveAll()"
            class="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl transition-colors border border-emerald-500/50 shadow-lg shadow-emerald-900/30">
            <span class="material-icons-round text-sm">auto_awesome</span>
            الموافقة على الكل ({{ displayedStudents().length }})
          </button>
          <button *ngIf="displayedStudents().length > 0"
            (click)="exportToExcel()"
            class="flex items-center gap-2 px-4 py-2 bg-teal-700 hover:bg-teal-600 text-white text-xs font-bold rounded-xl transition-colors border border-teal-600/50">
            <span class="material-icons-round text-sm">download</span>
            تصدير Excel ({{ displayedStudents().length }})
          </button>
        </div>
      </div>

      <!-- Tabs Row -->
      <div class="flex items-center gap-1 p-1 bg-slate-900/50 rounded-xl border border-slate-700/50 w-fit">
        <!-- requests mode: PENDING + REJECTED -->
        <ng-container *ngIf="pageMode() === 'requests'">
          <button (click)="switchTab('PENDING')"
            [class]="activeTab()==='PENDING' ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30' : 'text-slate-400 hover:text-slate-200'"
            class="px-4 py-2 rounded-lg text-xs font-bold transition-all">
            طلبات التسجيل
            <span *ngIf="activeTab()==='PENDING' && totalElements() > 0"
              class="mr-1 bg-amber-500 text-black text-[9px] font-black px-1.5 py-0.5 rounded-full">{{ totalElements() }}</span>
          </button>
          <button (click)="switchTab('REJECTED')"
            [class]="activeTab()==='REJECTED' ? 'bg-slate-500/20 text-slate-300 border border-slate-500/30' : 'text-slate-400 hover:text-slate-200'"
            class="px-4 py-2 rounded-lg text-xs font-bold transition-all">
            مرفوضون
            <span *ngIf="activeTab()==='REJECTED' && totalElements() > 0"
              class="mr-1 bg-slate-500 text-white text-[9px] font-black px-1.5 py-0.5 rounded-full">{{ totalElements() }}</span>
          </button>
        </ng-container>
        <!-- manage mode: ACTIVE / BLOCKED -->
        <ng-container *ngIf="pageMode() === 'manage'">
          <button (click)="switchTab('ACTIVE')"
            [class]="activeTab()==='ACTIVE' ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'text-slate-400 hover:text-slate-200'"
            class="px-4 py-2 rounded-lg text-xs font-bold transition-all">
            نشطون
            <span *ngIf="activeTab()==='ACTIVE' && totalElements() > 0"
              class="mr-1 bg-emerald-500 text-black text-[9px] font-black px-1.5 py-0.5 rounded-full">{{ totalElements() }}</span>
          </button>
          <button (click)="switchTab('BLOCKED')"
            [class]="activeTab()==='BLOCKED' ? 'bg-red-500/20 text-red-400 border border-red-500/30' : 'text-slate-400 hover:text-slate-200'"
            class="px-4 py-2 rounded-lg text-xs font-bold transition-all">محظورون</button>
        </ng-container>
      </div>

      <!-- Search + Filters (one row, compact) -->
      <div class="flex items-center gap-1.5 flex-wrap">
        <div class="relative">
          <span class="material-icons-round absolute right-2 top-1/2 -translate-y-1/2 text-slate-500" style="font-size:13px;pointer-events:none">search</span>
          <input type="text" [(ngModel)]="searchQuery" (ngModelChange)="triggerFilter()"
            class="filter-search" placeholder="اسم / كود / هاتف...">
        </div>
        <!-- لو صفحة مستوى: الصف ثابت مش قابل للتغيير -->
        <span *ngIf="levelName()"
          class="flex items-center gap-1 px-3 text-xs font-bold text-indigo-300 bg-indigo-500/10 border border-indigo-500/30 rounded-lg"
          style="height:30px">
          <span class="material-icons-round" style="font-size:13px">school</span>
          {{ levelName() }}
        </span>
        <select *ngIf="!levelName()" [(ngModel)]="filterGrade" (ngModelChange)="triggerFilter()" class="filter-input" style="width:95px">
          <option value="">الصف</option>
          <option *ngFor="let g of grades" [value]="g">{{ g }}</option>
        </select>
        <select [(ngModel)]="filterGovernorate" (ngModelChange)="triggerFilter()" class="filter-input" style="width:95px">
          <option value="">المحافظة</option>
          <option *ngFor="let g of governorateNames" [value]="g">{{ g }}</option>
        </select>
        <select [(ngModel)]="filterCenter" (ngModelChange)="triggerFilter()" class="filter-input" style="width:95px">
          <option value="">السنتر</option>
          <option *ngFor="let c of centers" [value]="c.name">{{ c.name }}</option>
        </select>
        <select [(ngModel)]="filterType" (ngModelChange)="triggerFilter()" class="filter-input" style="width:80px">
          <option value="">النوع</option>
          <option value="online">أونلاين</option>
          <option value="center">سنتر</option>
        </select>
        <button *ngIf="searchQuery || filterGrade || filterGovernorate || filterCenter || filterType"
          (click)="clearFilters()" title="مسح الفلاتر"
          class="flex items-center justify-center rounded-lg bg-slate-800 text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-colors border border-slate-700"
          style="height:30px;width:30px;flex-shrink:0">
          <span class="material-icons-round" style="font-size:14px">filter_alt_off</span>
        </button>
        <span class="text-slate-500" style="font-size:11px">{{ displayedStudents().length }}</span>
      </div>

      <!-- Table -->
      <div class="edu-card p-0 overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full text-xs">
            <thead class="bg-slate-800/70 border-b border-slate-700">
              <tr>
                <th class="px-3 py-3 text-right font-bold text-slate-400">
                  <button (click)="sortBy('studentCode')" class="flex items-center gap-0.5 hover:text-white transition-colors group">
                    الكود <span class="material-icons-round text-[10px] opacity-50 group-hover:opacity-100">{{ sortIcon('studentCode') }}</span>
                  </button>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 min-w-[160px]">
                  <button (click)="sortBy('fullName')" class="flex items-center gap-0.5 hover:text-white transition-colors group">
                    الطالب <span class="material-icons-round text-[10px] opacity-50 group-hover:opacity-100">{{ sortIcon('fullName') }}</span>
                  </button>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden md:table-cell">
                  <button (click)="sortBy('phone')" class="flex items-center gap-0.5 hover:text-white transition-colors group">
                    هاتف الطالب <span class="material-icons-round text-[10px] opacity-50 group-hover:opacity-100">{{ sortIcon('phone') }}</span>
                  </button>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell">
                  <button (click)="sortBy('parentPhone')" class="flex items-center gap-0.5 hover:text-white transition-colors group">
                    هاتف الوالد <span class="material-icons-round text-[10px] opacity-50 group-hover:opacity-100">{{ sortIcon('parentPhone') }}</span>
                  </button>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell">
                  <button (click)="sortBy('governorate')" class="flex items-center gap-0.5 hover:text-white transition-colors group">
                    المحافظة <span class="material-icons-round text-[10px] opacity-50 group-hover:opacity-100">{{ sortIcon('governorate') }}</span>
                  </button>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden xl:table-cell">
                  <button (click)="sortBy('centerName')" class="flex items-center gap-0.5 hover:text-white transition-colors group">
                    السنتر <span class="material-icons-round text-[10px] opacity-50 group-hover:opacity-100">{{ sortIcon('centerName') }}</span>
                  </button>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400">
                  <button (click)="sortBy('studyType')" class="flex items-center gap-0.5 hover:text-white transition-colors group">
                    النوع <span class="material-icons-round text-[10px] opacity-50 group-hover:opacity-100">{{ sortIcon('studyType') }}</span>
                  </button>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden xl:table-cell">
                  <button (click)="sortBy('grade')" class="flex items-center gap-0.5 hover:text-white transition-colors group">
                    الصف <span class="material-icons-round text-[10px] opacity-50 group-hover:opacity-100">{{ sortIcon('grade') }}</span>
                  </button>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden lg:table-cell">البطاقة</th>
                <th class="px-3 py-3 text-right font-bold text-slate-400 hidden xl:table-cell">
                  <button (click)="sortBy('createdAt')" class="flex items-center gap-0.5 hover:text-white transition-colors group">
                    التسجيل <span class="material-icons-round text-[10px] opacity-50 group-hover:opacity-100">{{ sortIcon('createdAt') }}</span>
                  </button>
                </th>
                <th class="px-3 py-3 text-right font-bold text-slate-400">الحالة</th>
                <th class="px-3 py-3 text-center font-bold text-slate-400">إجراءات</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngIf="loading()">
                <td colspan="12" class="text-center py-10 text-slate-500">
                  <span class="material-icons-round animate-spin text-2xl">refresh</span>
                </td>
              </tr>
              <tr *ngIf="!loading() && displayedStudents().length === 0">
                <td colspan="12" class="text-center py-10 text-slate-500">لا يوجد طلاب</td>
              </tr>
              <tr *ngFor="let s of displayedStudents()"
                class="border-b border-slate-800/40 hover:bg-slate-800/20 transition-colors">

                <td class="px-3 py-2.5">
                  <code class="text-[10px] text-amber-400 font-mono bg-amber-500/10 px-1.5 py-0.5 rounded">{{ s.studentCode || '—' }}</code>
                </td>

                <td class="px-3 py-2.5">
                  <div class="flex items-center gap-2 min-w-[150px]">
                    <div class="w-8 h-8 rounded-full flex-shrink-0 overflow-hidden border border-slate-700 cursor-pointer"
                      (click)="s.profileImageUrl ? openLightbox(s.profileImageUrl) : null">
                      <img *ngIf="s.profileImageUrl" [src]="s.profileImageUrl" class="w-full h-full object-cover">
                      <div *ngIf="!s.profileImageUrl"
                        class="w-full h-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-xs font-black text-white">
                        {{ (s.fullName || '?')[0] }}
                      </div>
                    </div>
                    <button (click)="viewDetails(s)" class="text-slate-200 font-semibold hover:text-indigo-400 transition-colors text-right leading-tight text-xs">
                      {{ s.fullName }}
                    </button>
                  </div>
                </td>

                <td class="px-3 py-2.5 hidden md:table-cell">
                  <span class="text-slate-300 font-mono ltr">{{ s.phone }}</span>
                </td>

                <td class="px-3 py-2.5 hidden lg:table-cell">
                  <span class="text-slate-400 font-mono ltr">{{ s.parentPhone || '—' }}</span>
                </td>

                <td class="px-3 py-2.5 hidden lg:table-cell">
                  <span class="text-slate-300">{{ s.governorate || '—' }}</span>
                </td>

                <td class="px-3 py-2.5 hidden xl:table-cell">
                  <span class="text-slate-400 max-w-[100px] truncate block">{{ s.centerName || '—' }}</span>
                </td>

                <td class="px-3 py-2.5">
                  <span class="text-[10px] font-bold px-2 py-0.5 rounded-full"
                    [class]="s.online ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30' : 'bg-orange-500/20 text-orange-400 border border-orange-500/30'">
                    {{ s.online ? 'أونلاين' : 'سنتر' }}
                  </span>
                </td>

                <td class="px-3 py-2.5 hidden xl:table-cell">
                  <span class="text-slate-300 text-xs">{{ s.grade || '—' }}</span>
                </td>

                <td class="px-3 py-2.5 hidden lg:table-cell">
                  <button *ngIf="s.identityDocumentUrl" (click)="openLightbox(s.identityDocumentUrl!)"
                    class="w-9 h-9 rounded-lg overflow-hidden border border-slate-600 hover:border-indigo-400 transition-colors">
                    <img [src]="s.identityDocumentUrl" class="w-full h-full object-cover">
                  </button>
                  <span *ngIf="!s.identityDocumentUrl" class="text-slate-600">—</span>
                </td>

                <td class="px-3 py-2.5 hidden xl:table-cell">
                  <div class="text-slate-400">{{ s.createdAt | date:'d/M/yy' }}</div>
                  <div class="text-slate-500 text-[10px]">{{ s.createdAt | date:'HH:mm' }}</div>
                </td>

                <td class="px-3 py-2.5">
                  <span class="text-[10px] font-bold px-2 py-1 rounded-full" [class]="statusClass(s.status)">
                    {{ statusLabel(s.status) }}
                  </span>
                </td>

                <td class="px-3 py-2.5">
                  <div class="flex items-center justify-center gap-1">
                    <button (click)="viewDetails(s)"
                      class="h-7 w-7 rounded-lg flex items-center justify-center bg-indigo-500/20 text-indigo-400 hover:bg-indigo-500/40 transition-colors" title="تفاصيل الطالب">
                      <span class="material-icons-round text-sm">visibility</span>
                    </button>
                    <button (click)="openWalletHistory(s)"
                      class="h-7 w-7 rounded-lg flex items-center justify-center bg-yellow-500/20 text-yellow-400 hover:bg-yellow-500/40 transition-colors" title="سجل المحفظة">
                      <span class="material-icons-round text-sm">account_balance_wallet</span>
                    </button>
                    <button (click)="openEditModal(s)"
                      class="h-7 w-7 rounded-lg flex items-center justify-center bg-blue-500/20 text-blue-400 hover:bg-blue-500/40 transition-colors" title="تعديل البيانات">
                      <span class="material-icons-round text-sm">edit</span>
                    </button>
                    <!-- قبول: PENDING في كلا الوضعين + REJECTED في requests -->
                    <button *ngIf="activeTab() === 'PENDING' || (pageMode() === 'requests' && activeTab() === 'REJECTED')" (click)="approveStudent(s.id)"
                      class="h-7 w-7 rounded-lg flex items-center justify-center bg-emerald-500/20 text-emerald-400 hover:bg-emerald-500/40 transition-colors" title="قبول الطالب">
                      <span class="material-icons-round text-sm">check_circle</span>
                    </button>
                    <!-- رفض: PENDING في كلا الوضعين -->
                    <button *ngIf="activeTab() === 'PENDING'" (click)="openRejectModal(s.id)"
                      class="h-7 w-7 rounded-lg flex items-center justify-center bg-orange-500/20 text-orange-400 hover:bg-orange-500/40 transition-colors" title="رفض الطالب">
                      <span class="material-icons-round text-sm">cancel</span>
                    </button>
                    <!-- فتح جهاز: ACTIVE في manage فقط -->
                    <button *ngIf="pageMode() === 'manage' && activeTab() === 'ACTIVE'" (click)="clearStudentDevice(s.id)"
                      class="h-7 w-7 rounded-lg flex items-center justify-center bg-purple-500/20 text-purple-400 hover:bg-purple-500/40 transition-colors" title="فتح جهاز — تنظيف الـ Device ID">
                      <span class="material-icons-round text-sm">phonelink_erase</span>
                    </button>
                    <!-- حظر: ACTIVE في manage فقط -->
                    <button *ngIf="pageMode() === 'manage' && activeTab() === 'ACTIVE'" (click)="openBlockModal(s.id)"
                      class="h-7 w-7 rounded-lg flex items-center justify-center bg-red-500/20 text-red-400 hover:bg-red-500/40 transition-colors" title="حظر الطالب">
                      <span class="material-icons-round text-sm">block</span>
                    </button>
                    <!-- فك الحظر: BLOCKED في manage فقط -->
                    <button *ngIf="pageMode() === 'manage' && activeTab() === 'BLOCKED'" (click)="unblockStudent(s.id)"
                      class="h-7 w-7 rounded-lg flex items-center justify-center bg-emerald-500/20 text-emerald-400 hover:bg-emerald-500/40 transition-colors" title="فك الحظر">
                      <span class="material-icons-round text-sm">lock_open</span>
                    </button>
                    <!-- حذف: REJECTED في manage فقط -->
                    <button *ngIf="pageMode() === 'manage' && activeTab() === 'REJECTED'" (click)="deleteStudent(s.id)"
                      class="h-7 w-7 rounded-lg flex items-center justify-center bg-slate-700/50 text-slate-500 hover:bg-red-500/20 hover:text-red-400 transition-colors" title="حذف نهائي">
                      <span class="material-icons-round text-sm">delete</span>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div *ngIf="totalPages() > 1" class="flex items-center justify-between px-4 py-3 border-t border-slate-800 bg-slate-900/40">
          <span class="text-[10px] text-slate-500">صفحة {{ currentPage() + 1 }} / {{ totalPages() }}</span>
          <div class="flex gap-1">
            <button (click)="loadStudents(currentPage() - 1)" [disabled]="currentPage() === 0"
              class="h-7 w-7 rounded-lg bg-slate-800 text-slate-400 flex items-center justify-center disabled:opacity-30">
              <span class="material-icons-round text-sm">chevron_right</span>
            </button>
            <button *ngFor="let p of pageRange()" (click)="loadStudents(p)"
              [class]="p === currentPage() ? 'bg-indigo-600 text-white' : 'bg-slate-800 text-slate-400 hover:bg-slate-700'"
              class="h-7 w-7 rounded-lg text-[10px] font-bold transition-colors">{{ p + 1 }}</button>
            <button (click)="loadStudents(currentPage() + 1)" [disabled]="currentPage() >= totalPages() - 1"
              class="h-7 w-7 rounded-lg bg-slate-800 text-slate-400 flex items-center justify-center disabled:opacity-30">
              <span class="material-icons-round text-sm">chevron_left</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Lightbox -->
      <div *ngIf="lightboxUrl()" (click)="lightboxUrl.set(null)"
        class="fixed inset-0 z-[100] bg-black/90 flex items-center justify-center p-4 animate-fade-in">
        <button (click)="lightboxUrl.set(null)" class="absolute top-4 left-4 btn-icon bg-slate-800 text-white">
          <span class="material-icons-round">close</span>
        </button>
        <img [src]="lightboxUrl()!" (click)="$event.stopPropagation()"
          class="max-w-full max-h-[90vh] rounded-2xl shadow-2xl border border-slate-700">
      </div>

      <!-- Student Details Modal -->
      <div *ngIf="selectedStudent()" class="fixed inset-0 z-[80] flex items-center justify-center p-4">
        <div (click)="selectedStudent.set(null)" class="absolute inset-0 bg-black/70 backdrop-blur-sm animate-fade-in"></div>
        <div class="relative w-full max-w-2xl bg-slate-900 rounded-2xl shadow-2xl border border-slate-700/50 z-10 flex flex-col max-h-[90vh]" dir="rtl">
          <!-- Modal Header -->
          <div class="p-4 border-b border-slate-800 flex items-center justify-between flex-shrink-0">
            <div class="flex items-center gap-3">
              <div class="w-12 h-12 rounded-xl overflow-hidden border-2 border-slate-600 flex-shrink-0 cursor-pointer"
                (click)="selectedStudent()?.profileImageUrl ? openLightbox(selectedStudent()!.profileImageUrl!) : null">
                <img *ngIf="selectedStudent()?.profileImageUrl" [src]="selectedStudent()?.profileImageUrl" class="w-full h-full object-cover">
                <div *ngIf="!selectedStudent()?.profileImageUrl"
                  class="w-full h-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-xl font-black text-white">
                  {{ (selectedStudent()?.fullName || '?')[0] }}
                </div>
              </div>
              <div>
                <h3 class="text-white font-bold">{{ selectedStudent()?.fullName }}</h3>
                <div class="flex items-center gap-2 mt-0.5">
                  <code class="text-amber-400 text-[10px] font-mono bg-amber-500/10 px-1.5 py-0.5 rounded">{{ selectedStudent()?.studentCode || 'بدون كود' }}</code>
                  <span class="text-[9px] font-bold px-2 py-0.5 rounded-full" [class]="statusClass(selectedStudent()?.status)">{{ statusLabel(selectedStudent()?.status) }}</span>
                </div>
              </div>
            </div>
            <button (click)="selectedStudent.set(null)" class="btn-icon bg-slate-800"><span class="material-icons-round">close</span></button>
          </div>
          <!-- Detail Tabs -->
          <div class="flex border-b border-slate-800 overflow-x-auto flex-shrink-0">
            <button *ngFor="let tab of detailTabs" (click)="activeDetailTab.set(tab)"
              [class.text-indigo-400]="activeDetailTab() === tab"
              [class.border-indigo-500]="activeDetailTab() === tab"
              class="px-4 py-3 text-xs font-bold text-slate-500 border-b-2 border-transparent transition-all hover:text-slate-300 whitespace-nowrap">{{ tab }}</button>
          </div>
          <!-- Detail Body -->
          <div class="overflow-y-auto flex-1 p-4 custom-scrollbar">
            <!-- الملف الشخصي -->
            <div *ngIf="activeDetailTab() === 'الملف الشخصي'" class="space-y-3 animate-fade-in">
              <div *ngIf="selectedStudent()?.identityDocumentUrl"
                class="rounded-xl overflow-hidden border border-slate-700 cursor-pointer group"
                (click)="openLightbox(selectedStudent()!.identityDocumentUrl!)">
                <img [src]="selectedStudent()?.identityDocumentUrl" class="w-full object-cover max-h-40 group-hover:opacity-80 transition-opacity">
                <div class="px-3 py-1.5 bg-slate-800 text-[10px] text-slate-400 flex items-center gap-2">
                  <span class="material-icons-round text-sm">badge</span> بطاقة الهوية — اضغط للتكبير
                </div>
              </div>
              <div class="grid grid-cols-2 gap-2">
                <div class="bg-slate-800/50 rounded-xl p-3 border border-slate-700/40">
                  <p class="text-[9px] text-slate-500 mb-1">كود الطالب</p>
                  <p class="text-amber-400 text-xs font-mono font-bold">{{ selectedStudent()?.studentCode || '—' }}</p>
                </div>
                <div class="bg-slate-800/50 rounded-xl p-3 border border-slate-700/40">
                  <p class="text-[9px] text-slate-500 mb-1">رقم الهاتف</p>
                  <p class="text-slate-200 text-xs font-mono ltr text-right">{{ selectedStudent()?.phone }}</p>
                </div>
                <div class="bg-slate-800/50 rounded-xl p-3 border border-slate-700/40">
                  <p class="text-[9px] text-slate-500 mb-1">هاتف الوالد</p>
                  <p class="text-slate-200 text-xs font-mono ltr text-right">{{ selectedStudent()?.parentPhone || '—' }}</p>
                </div>
                <div class="bg-slate-800/50 rounded-xl p-3 border border-slate-700/40">
                  <p class="text-[9px] text-slate-500 mb-1">المحافظة</p>
                  <p class="text-slate-200 text-xs font-bold">{{ selectedStudent()?.governorate || '—' }}</p>
                </div>
                <div class="bg-slate-800/50 rounded-xl p-3 border border-slate-700/40">
                  <p class="text-[9px] text-slate-500 mb-1">المنطقة</p>
                  <p class="text-slate-200 text-xs font-bold">{{ selectedStudent()?.area || '—' }}</p>
                </div>
                <div class="bg-slate-800/50 rounded-xl p-3 border border-slate-700/40">
                  <p class="text-[9px] text-slate-500 mb-1">الصف الدراسي</p>
                  <p class="text-slate-200 text-xs font-bold">{{ selectedStudent()?.grade || '—' }}</p>
                </div>
                <!-- نوع الدراسة -->
                <div class="bg-slate-800/50 rounded-xl p-3 border border-slate-700/40"
                     [class.col-span-2]="selectedStudent()?.online">
                  <p class="text-[9px] text-slate-500 mb-1">نوع الدراسة</p>
                  <div class="flex items-center gap-2">
                    <p class="text-slate-200 text-xs font-bold">{{ selectedStudent()?.online ? 'أونلاين' : 'سنتر' }}</p>
                    <span *ngIf="!selectedStudent()?.online" class="text-slate-500 text-[10px]">—</span>
                    <p *ngIf="!selectedStudent()?.online" class="text-slate-200 text-xs font-bold">{{ selectedStudent()?.centerName || '—' }}</p>
                  </div>
                </div>
                <!-- الجروب: سنتر فقط -->
                <div *ngIf="!selectedStudent()?.online" class="bg-indigo-500/10 rounded-xl p-3 border border-indigo-500/30">
                  <p class="text-[9px] text-indigo-400 mb-1">الجروب</p>
                  <p class="text-indigo-300 text-xs font-bold">{{ selectedStudent()?.groupName || 'غير مسجل في جروب' }}</p>
                </div>
                <div class="bg-slate-800/50 rounded-xl p-3 border border-slate-700/40">
                  <p class="text-[9px] text-slate-500 mb-1">المدرسة</p>
                  <p class="text-slate-200 text-xs font-bold">{{ selectedStudent()?.schoolName || '—' }}</p>
                </div>
                <div class="bg-slate-800/50 rounded-xl p-3 border border-slate-700/40">
                  <p class="text-[9px] text-slate-500 mb-1">إدارة التعليم</p>
                  <p class="text-slate-200 text-xs font-bold">{{ selectedStudent()?.educationDepartment || '—' }}</p>
                </div>
                <div class="bg-slate-800/50 rounded-xl p-3 border border-slate-700/40 col-span-2">
                  <p class="text-[9px] text-slate-500 mb-1">تاريخ التسجيل</p>
                  <p class="text-slate-200 text-xs font-bold">{{ selectedStudent()?.createdAt | date:'EEEE d MMMM yyyy - HH:mm' }}</p>
                </div>
              </div>
              <div *ngIf="selectedStudent()?.rejectionReason" class="bg-red-500/10 border border-red-500/30 rounded-xl p-3">
                <p class="text-[10px] text-red-400 mb-1 font-bold">سبب الرفض / الحظر</p>
                <p class="text-red-300 text-xs">{{ selectedStudent()?.rejectionReason }}</p>
              </div>
              <div class="bg-indigo-500/10 border border-indigo-500/30 rounded-xl p-3 flex items-start gap-2">
                <span class="material-icons-round text-indigo-400 text-base mt-0.5">smart_toy</span>
                <div>
                  <p class="text-indigo-300 text-xs font-bold mb-0.5">تحليل AI لبطاقة الطالب</p>
                  <p class="text-slate-400 text-[11px]">سيتم ربط نموذج الذكاء الاصطناعي لتحليل بيانات بطاقة الهوية قريباً.</p>
                </div>
              </div>
              <div class="flex flex-wrap gap-2 pt-2 border-t border-slate-800">
                <!-- قبول: PENDING دايمًا + REJECTED في requests فقط -->
                <button *ngIf="selectedStudent()?.status === 'PENDING' || (pageMode() === 'requests' && selectedStudent()?.status === 'REJECTED')"
                  (click)="approveStudent(selectedStudent()!.id); selectedStudent.set(null)"
                  class="flex items-center gap-1 px-3 py-1.5 bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded-lg text-xs font-bold hover:bg-emerald-500/30 transition-colors">
                  <span class="material-icons-round text-sm">check_circle</span> قبول
                </button>
                <button (click)="openEditModal(selectedStudent()!); selectedStudent.set(null)"
                  class="flex items-center gap-1 px-3 py-1.5 bg-blue-500/20 text-blue-400 border border-blue-500/30 rounded-lg text-xs font-bold hover:bg-blue-500/30 transition-colors">
                  <span class="material-icons-round text-sm">edit</span> تعديل
                </button>
                <!-- رفض: PENDING في كلا الوضعين -->
                <button *ngIf="selectedStudent()?.status === 'PENDING'"
                  (click)="openRejectModal(selectedStudent()!.id); selectedStudent.set(null)"
                  class="flex items-center gap-1 px-3 py-1.5 bg-orange-500/20 text-orange-400 border border-orange-500/30 rounded-lg text-xs font-bold hover:bg-orange-500/30 transition-colors">
                  <span class="material-icons-round text-sm">cancel</span> رفض
                </button>
                <!-- فتح جهاز: ACTIVE manage -->
                <button *ngIf="pageMode() === 'manage' && selectedStudent()?.status === 'ACTIVE'"
                  (click)="clearStudentDevice(selectedStudent()!.id)"
                  class="flex items-center gap-1 px-3 py-1.5 bg-purple-500/20 text-purple-400 border border-purple-500/30 rounded-lg text-xs font-bold hover:bg-purple-500/30 transition-colors">
                  <span class="material-icons-round text-sm">phonelink_erase</span> فتح جهاز
                </button>
                <!-- حظر: فقط في ACTIVE manage -->
                <button *ngIf="pageMode() === 'manage' && selectedStudent()?.status === 'ACTIVE'"
                  (click)="openBlockModal(selectedStudent()!.id); selectedStudent.set(null)"
                  class="flex items-center gap-1 px-3 py-1.5 bg-red-500/20 text-red-400 border border-red-500/30 rounded-lg text-xs font-bold hover:bg-red-500/30 transition-colors">
                  <span class="material-icons-round text-sm">block</span> حظر
                </button>
                <!-- فك الحظر -->
                <button *ngIf="selectedStudent()?.status === 'BLOCKED'"
                  (click)="unblockStudent(selectedStudent()!.id); selectedStudent.set(null)"
                  class="flex items-center gap-1 px-3 py-1.5 bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded-lg text-xs font-bold hover:bg-emerald-500/30 transition-colors">
                  <span class="material-icons-round text-sm">lock_open</span> فك الحظر
                </button>
                <!-- حذف: فقط في REJECTED manage -->
                <button *ngIf="pageMode() === 'manage' && selectedStudent()?.status === 'REJECTED'"
                  (click)="deleteStudent(selectedStudent()!.id); selectedStudent.set(null)"
                  class="flex items-center gap-1 px-3 py-1.5 bg-slate-700/50 text-red-400 border border-red-500/20 rounded-lg text-xs font-bold hover:bg-red-500/20 transition-colors">
                  <span class="material-icons-round text-sm">delete</span> حذف
                </button>
              </div>
            </div>
            <!-- المحفظة -->
            <div *ngIf="activeDetailTab() === 'المحفظة'" class="space-y-3 animate-fade-in">
              <div class="p-4 bg-gradient-to-l from-indigo-600/20 to-transparent rounded-xl border border-indigo-500/20">
                <p class="text-indigo-300 text-xs font-bold mb-1">الرصيد الحالي</p>
                <h4 class="text-2xl font-black text-white">{{ selectedStudent()?.walletBalance || 0 }} <span class="text-sm font-medium">ج.م</span></h4>
              </div>
              <div *ngIf="transactions().length === 0" class="text-center py-6 text-slate-500 text-xs">لا توجد عمليات</div>
              <div *ngFor="let tx of transactions()" class="p-3 bg-slate-800/40 rounded-xl flex items-center justify-between border border-slate-800">
                <div class="flex items-center gap-2">
                  <span class="material-icons-round text-base" [class.text-emerald-400]="tx.type==='TOP_UP'" [class.text-red-400]="tx.type!=='TOP_UP'">
                    {{ tx.type === 'TOP_UP' ? 'add_circle' : 'remove_circle' }}
                  </span>
                  <div>
                    <p class="text-slate-200 font-bold text-xs">{{ tx.description || tx.type }}</p>
                    <p class="text-[10px] text-slate-500">{{ tx.createdAt | date:'short' }}</p>
                  </div>
                </div>
                <span [class.text-emerald-400]="tx.type==='TOP_UP'" [class.text-red-400]="tx.type!=='TOP_UP'" class="font-black">
                  {{ tx.type === 'TOP_UP' ? '+' : '-' }}{{ tx.amount }}
                </span>
              </div>
            </div>
            <!-- الحضور -->
            <div *ngIf="activeDetailTab() === 'الحضور'" class="space-y-2 animate-fade-in">
              <div *ngIf="attendance().length === 0" class="text-center py-6 text-slate-500 text-xs">لا توجد سجلات حضور</div>
              <div *ngFor="let att of attendance()" class="p-3 bg-slate-800/40 rounded-xl border border-slate-800 flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="material-icons-round text-base" [class.text-emerald-400]="att.status==='PRESENT'" [class.text-red-400]="att.status!=='PRESENT'">
                    {{ att.status === 'PRESENT' ? 'event_available' : 'event_busy' }}
                  </span>
                  <div>
                    <p class="text-white font-bold text-xs">{{ att.weekTitle || att.sessionTitle }}</p>
                    <p class="text-[10px] text-slate-500">{{ att.scanTime | date:'short' }}</p>
                  </div>
                </div>
                <span class="text-xs font-bold" [class.text-emerald-400]="att.status==='PRESENT'" [class.text-red-400]="att.status!=='PRESENT'">
                  {{ att.status === 'PRESENT' ? 'حاضر' : 'غائب' }}
                </span>
              </div>
            </div>
            <!-- الاشتراكات -->
            <div *ngIf="activeDetailTab() === 'الاشتراكات'" class="space-y-2 animate-fade-in">
              <div *ngIf="enrollments().length === 0" class="text-center py-6 text-slate-500 text-xs">لا توجد اشتراكات</div>
              <div *ngFor="let en of enrollments()" class="p-3 bg-slate-800/40 rounded-xl border border-slate-800 flex items-center justify-between">
                <div>
                  <p class="text-white font-bold text-xs">{{ en.courseName || en.courseTitle }}</p>
                  <p class="text-[10px] text-slate-500">ينتهي: {{ (en.expiresAt || en.expiryDate) | date:'mediumDate' }}</p>
                </div>
                <span [class.badge-success]="en.status==='ACTIVE'" [class.badge-danger]="en.status!=='ACTIVE'" class="text-[10px]">
                  {{ en.status === 'ACTIVE' ? 'نشط' : en.status }}
                </span>
              </div>
            </div>
            <!-- الاختبارات -->
            <div *ngIf="activeDetailTab() === 'الاختبارات'" class="space-y-2 animate-fade-in">
              <div *ngIf="quizAttempts().length === 0" class="text-center py-6 text-slate-500 text-xs">لا توجد محاولات</div>
              <div *ngFor="let q of quizAttempts()" class="p-3 bg-slate-800/40 rounded-xl border border-slate-800 flex items-center justify-between">
                <div>
                  <p class="text-white font-bold text-xs">محاولة اختبار</p>
                  <p class="text-[10px] text-slate-500">{{ q.startedAt | date:'short' }}</p>
                </div>
                <div class="flex items-center gap-3">
                  <span class="text-white font-black">{{ q.score }}</span>
                  <span [class.badge-success]="q.passed" [class.badge-danger]="!q.passed" class="text-[10px]">{{ q.passed ? 'ناجح' : 'راسب' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Edit Modal -->
      <div *ngIf="editingStudent()" class="fixed inset-0 z-[90] flex items-center justify-center p-4">
        <div (click)="editingStudent.set(null)" class="absolute inset-0 bg-black/70 backdrop-blur-sm"></div>
        <div class="relative w-full max-w-xl bg-slate-900 rounded-2xl shadow-2xl border border-slate-700/50 z-10 max-h-[92vh] overflow-y-auto custom-scrollbar" dir="rtl">
          <div class="p-4 border-b border-slate-800 flex items-center justify-between sticky top-0 bg-slate-900 z-10">
            <h3 class="text-white font-bold text-sm">تعديل بيانات الطالب — {{ editingStudent()?.fullName }}</h3>
            <button (click)="editingStudent.set(null)" class="btn-icon bg-slate-800"><span class="material-icons-round">close</span></button>
          </div>
          <div class="p-4 space-y-4">

            <!-- الاسم الرباعي -->
            <div>
              <p class="text-[11px] text-slate-400 font-bold mb-2 flex items-center gap-1"><span class="material-icons-round text-sm">person</span>الاسم الرباعي</p>
              <div class="grid grid-cols-2 gap-2">
                <div><label class="text-[10px] text-slate-500 block mb-1">الأول <span class="text-red-400">*</span></label><input [(ngModel)]="editForm.firstName" class="edu-input text-xs h-8" (input)="filterArabicInput($event, 'firstName')"></div>
                <div><label class="text-[10px] text-slate-500 block mb-1">الثاني <span class="text-red-400">*</span></label><input [(ngModel)]="editForm.secondName" class="edu-input text-xs h-8" (input)="filterArabicInput($event, 'secondName')"></div>
                <div><label class="text-[10px] text-slate-500 block mb-1">الثالث <span class="text-red-400">*</span></label><input [(ngModel)]="editForm.thirdName" class="edu-input text-xs h-8" (input)="filterArabicInput($event, 'thirdName')"></div>
                <div><label class="text-[10px] text-slate-500 block mb-1">الرابع <span class="text-red-400">*</span></label><input [(ngModel)]="editForm.fourthName" class="edu-input text-xs h-8" (input)="filterArabicInput($event, 'fourthName')"></div>
              </div>
            </div>

            <!-- الهواتف -->
            <div>
              <p class="text-[11px] text-slate-400 font-bold mb-2 flex items-center gap-1"><span class="material-icons-round text-sm">phone</span>أرقام الهاتف</p>
              <div class="grid grid-cols-2 gap-2">
                <div><label class="text-[10px] text-slate-500 block mb-1">هاتف الطالب <span class="text-red-400">*</span></label><input [(ngModel)]="editForm.phone" class="edu-input text-xs h-8 font-mono" dir="ltr"></div>
                <div><label class="text-[10px] text-slate-500 block mb-1">هاتف ولي الأمر <span class="text-red-400">*</span></label><input [(ngModel)]="editForm.parentPhone" class="edu-input text-xs h-8 font-mono" dir="ltr"></div>
              </div>
            </div>

            <!-- كلمة المرور -->
            <div>
              <p class="text-[11px] text-slate-400 font-bold mb-2 flex items-center gap-1"><span class="material-icons-round text-sm">lock</span>كلمة المرور</p>
              <div class="grid grid-cols-2 gap-2">
                <div><label class="text-[10px] text-slate-500 block mb-1">كلمة المرور الجديدة</label><input type="password" [(ngModel)]="editForm.newPassword" class="edu-input text-xs h-8" placeholder="اتركها فارغة للإبقاء"></div>
                <div><label class="text-[10px] text-slate-500 block mb-1">تأكيد كلمة المرور</label><input type="password" [(ngModel)]="editForm.confirmPassword" class="edu-input text-xs h-8" placeholder="أعد كتابة كلمة المرور"></div>
              </div>
              <p *ngIf="editForm.newPassword && editForm.newPassword !== editForm.confirmPassword" class="text-red-400 text-[10px] mt-1">كلمتا المرور غير متطابقتين</p>
            </div>

            <!-- الموقع -->
            <div>
              <p class="text-[11px] text-slate-400 font-bold mb-2 flex items-center gap-1"><span class="material-icons-round text-sm">location_on</span>الموقع الجغرافي</p>
              <div class="grid grid-cols-2 gap-2">
                <div>
                  <label class="text-[10px] text-slate-500 block mb-1">المحافظة <span class="text-red-400">*</span></label>
                  <select [(ngModel)]="editForm.governorate" (ngModelChange)="onEditGovernorateChange()" class="filter-input w-full" style="height:34px;font-size:12px;padding:0 8px">
                    <option value="">اختر المحافظة</option>
                    <option *ngFor="let g of governorateNames" [value]="g">{{ g }}</option>
                  </select>
                </div>
                <div>
                  <label class="text-[10px] text-slate-500 block mb-1">المنطقة <span class="text-red-400">*</span></label>
                  <select [(ngModel)]="editForm.area" class="filter-input w-full" style="height:34px;font-size:12px;padding:0 8px" [disabled]="editAreasLoading || editAreas.length === 0">
                    <option value="">{{ editAreasLoading ? 'جاري التحميل...' : 'اختر المنطقة' }}</option>
                    <option *ngFor="let a of editAreas" [value]="a">{{ a }}</option>
                  </select>
                </div>
              </div>
            </div>

            <!-- الدراسة -->
            <div>
              <p class="text-[11px] text-slate-400 font-bold mb-2 flex items-center gap-1"><span class="material-icons-round text-sm">school</span>المعلومات الأكاديمية</p>
              <div class="grid grid-cols-2 gap-2">
                <div>
                  <label class="text-[10px] text-slate-500 block mb-1">الصف الدراسي <span class="text-red-400">*</span></label>
                  <select [(ngModel)]="editForm.grade" class="filter-input w-full" style="height:34px;font-size:12px;padding:0 8px">
                    <option value="">اختر الصف</option>
                    <option *ngFor="let g of grades" [value]="g">{{ g }}</option>
                  </select>
                </div>
                <div>
                  <label class="text-[10px] text-slate-500 block mb-1">نوع الدراسة <span class="text-red-400">*</span></label>
                  <select [(ngModel)]="editForm.studyType" (ngModelChange)="editForm.groupId=null; editCenterGroups=[]; editForm.centerName && loadEditGroups()" class="filter-input w-full" style="height:34px;font-size:12px;padding:0 8px">
                    <option value="ONLINE">أونلاين</option>
                    <option value="CENTER">سنتر</option>
                  </select>
                </div>
                <div>
                  <label class="text-[10px] text-slate-500 block mb-1">اسم المدرسة</label>
                  <input [(ngModel)]="editForm.schoolName" class="edu-input text-xs h-8">
                </div>
                <div>
                  <label class="text-[10px] text-slate-500 block mb-1">الإدارة التعليمية</label>
                  <input [(ngModel)]="editForm.educationDepartment" class="edu-input text-xs h-8">
                </div>
              </div>
              <!-- السنتر والجروب — يظهران لو CENTER -->
              <div *ngIf="editForm.studyType === 'CENTER'" class="mt-2 grid grid-cols-2 gap-2">
                <div>
                  <label class="text-[10px] text-slate-500 block mb-1">اسم السنتر</label>
                  <select [(ngModel)]="editForm.centerName" (ngModelChange)="loadEditGroups()" class="filter-input w-full" style="height:34px;font-size:12px;padding:0 8px">
                    <option value="">اختر السنتر</option>
                    <option *ngFor="let c of centers" [value]="c.name">{{ c.name }}</option>
                  </select>
                </div>
                <div>
                  <label class="text-[10px] text-slate-500 block mb-1">الجروب</label>
                  <select [(ngModel)]="editForm.groupId" class="filter-input w-full" style="height:34px;font-size:12px;padding:0 8px"
                    [disabled]="editGroupsLoading || editCenterGroups.length === 0">
                    <option [ngValue]="null">{{ editGroupsLoading ? 'جاري التحميل...' : (editCenterGroups.length === 0 ? 'لا توجد جروبات' : 'اختر الجروب') }}</option>
                    <option *ngFor="let g of editCenterGroups" [ngValue]="g.id">{{ g.title || g.name }}</option>
                  </select>
                </div>
              </div>
            </div>

            <!-- الصور -->
            <div class="border-t border-slate-800 pt-3">
              <p class="text-[11px] text-slate-400 font-bold mb-2 flex items-center gap-1"><span class="material-icons-round text-sm">photo_camera</span>الصور</p>
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label class="text-[10px] text-slate-500 block mb-1">الصورة الشخصية</label>
                  <div *ngIf="editForm.profileImageUrl" class="w-full h-20 rounded-xl overflow-hidden border border-slate-600 mb-2 cursor-pointer" (click)="openLightbox(editForm.profileImageUrl)">
                    <img [src]="editForm.profileImageUrl" class="w-full h-full object-cover">
                  </div>
                  <label class="flex items-center gap-2 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-bold rounded-lg cursor-pointer border border-slate-700 transition-colors">
                    <span class="material-icons-round text-sm">upload</span>
                    {{ editForm.profileImageUrl ? 'تغيير' : 'رفع صورة' }}
                    <input type="file" accept="image/*" class="hidden" (change)="onImageUpload($event, 'profile')">
                  </label>
                  <span *ngIf="uploadingProfile" class="text-[10px] text-indigo-400 mt-1 block">جاري الرفع...</span>
                </div>
                <div>
                  <label class="text-[10px] text-slate-500 block mb-1">صورة البطاقة</label>
                  <div *ngIf="editForm.identityDocumentUrl" class="w-full h-20 rounded-xl overflow-hidden border border-slate-600 mb-2 cursor-pointer" (click)="openLightbox(editForm.identityDocumentUrl)">
                    <img [src]="editForm.identityDocumentUrl" class="w-full h-full object-cover">
                  </div>
                  <label class="flex items-center gap-2 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-bold rounded-lg cursor-pointer border border-slate-700 transition-colors">
                    <span class="material-icons-round text-sm">badge</span>
                    {{ editForm.identityDocumentUrl ? 'تغيير' : 'رفع بطاقة' }}
                    <input type="file" accept="image/*" class="hidden" (change)="onImageUpload($event, 'id')">
                  </label>
                  <span *ngIf="uploadingId" class="text-[10px] text-indigo-400 mt-1 block">جاري الرفع...</span>
                </div>
              </div>
            </div>
          </div>
          <div class="p-4 border-t border-slate-800 flex justify-end gap-3 sticky bottom-0 bg-slate-900">
            <button (click)="editingStudent.set(null)" class="btn-secondary text-xs h-9 px-5">إلغاء</button>
            <button (click)="saveEdit()" [disabled]="uploadingProfile || uploadingId || (editForm.newPassword && editForm.newPassword !== editForm.confirmPassword)"
              class="btn-primary text-xs h-9 px-5 disabled:opacity-50">حفظ التعديلات</button>
          </div>
        </div>
      </div>

      <!-- Reject Modal -->
      <div *ngIf="rejectingId()" class="fixed inset-0 z-[90] flex items-center justify-center p-4">
        <div (click)="rejectingId.set(0)" class="absolute inset-0 bg-black/70 backdrop-blur-sm"></div>
        <div class="relative w-full max-w-sm bg-slate-900 rounded-2xl shadow-2xl border border-slate-700/50 z-10" dir="rtl">
          <div class="p-4 border-b border-slate-800 flex items-center justify-between">
            <h3 class="text-white font-bold text-sm">رفض الطلب</h3>
            <button (click)="rejectingId.set(0)" class="btn-icon bg-slate-800"><span class="material-icons-round">close</span></button>
          </div>
          <div class="p-4">
            <label class="edu-label text-xs">سبب الرفض <span class="text-red-400">*</span></label>
            <textarea [(ngModel)]="rejectReason" class="edu-input min-h-[80px] text-xs" placeholder="مثال: الصورة غير واضحة..."></textarea>
          </div>
          <div class="p-4 pt-0 flex justify-end gap-3">
            <button (click)="rejectingId.set(0)" class="btn-secondary text-xs h-9 px-4">إلغاء</button>
            <button (click)="confirmReject()" class="px-4 h-9 bg-orange-600 hover:bg-orange-500 text-white text-xs font-bold rounded-xl transition-colors">رفض نهائي</button>
          </div>
        </div>
      </div>

      <!-- Block Modal -->
      <div *ngIf="blockingId()" class="fixed inset-0 z-[90] flex items-center justify-center p-4">
        <div (click)="blockingId.set(0)" class="absolute inset-0 bg-black/70 backdrop-blur-sm"></div>
        <div class="relative w-full max-w-sm bg-slate-900 rounded-2xl shadow-2xl border border-slate-700/50 z-10" dir="rtl">
          <div class="p-4 border-b border-slate-800 flex items-center justify-between">
            <h3 class="text-white font-bold text-sm">حظر الطالب</h3>
            <button (click)="blockingId.set(0)" class="btn-icon bg-slate-800"><span class="material-icons-round">close</span></button>
          </div>
          <div class="p-4">
            <label class="edu-label text-xs">سبب الحظر</label>
            <textarea [(ngModel)]="blockReason" class="edu-input min-h-[70px] text-xs" placeholder="اختياري..."></textarea>
          </div>
          <div class="p-4 pt-0 flex justify-end gap-3">
            <button (click)="blockingId.set(0)" class="btn-secondary text-xs h-9 px-4">إلغاء</button>
            <button (click)="confirmBlock()" class="flex items-center gap-1 px-4 h-9 bg-red-600 hover:bg-red-500 text-white text-xs font-bold rounded-xl transition-colors">
              <span class="material-icons-round text-sm">block</span> حظر
            </button>
          </div>
        </div>
      </div>

    </div>
  `
})
export class StudentsComponent implements OnInit {
  private router = inject(Router);
  pageMode = signal<PageMode>('requests');
  activeTab = signal<TabType>('PENDING');
  students = signal<Student[]>([]);
  totalPages = signal(0);
  totalElements = signal(0);
  currentPage = signal(0);
  loading = signal(false);

  // Filters
  searchQuery = '';
  filterGrade = '';
  filterGovernorate = '';
  filterCenter = '';
  filterType = '';
  _filterTick = signal(0); // trigger computed re-evaluation

  // Sorting
  sortCol = signal<string>('');
  sortDir = signal<SortDir>(null);

  displayedStudents = computed(() => {
    this._filterTick(); // depend on tick
    let list = [...this.students()];
    const q = this.searchQuery.toLowerCase();
    if (q) list = list.filter(s =>
      (s.fullName || '').toLowerCase().includes(q) ||
      (s.studentCode || '').toLowerCase().includes(q) ||
      (s.phone || '').includes(q) ||
      (s.parentPhone || '').includes(q)
    );
    if (this.filterGrade)  list = list.filter(s => s.grade === this.filterGrade);
    if (this.filterCenter) list = list.filter(s => (s.centerName || '') === this.filterCenter);
    if (this.filterGovernorate) list = list.filter(s => (s.governorate || '') === this.filterGovernorate);
    if (this.filterType === 'online')  list = list.filter(s => s.online === true);
    if (this.filterType === 'center')  list = list.filter(s => s.online === false || s.online === null || s.online === undefined);
    const col = this.sortCol(), dir = this.sortDir();
    if (col && dir) {
      list.sort((a, b) => {
        const av = String((a as any)[col] ?? '').toLowerCase();
        const bv = String((b as any)[col] ?? '').toLowerCase();
        return av < bv ? (dir === 'asc' ? -1 : 1) : av > bv ? (dir === 'asc' ? 1 : -1) : 0;
      });
    }
    return list;
  });

  centers: any[] = [];
  grades: string[] = [];
  levelsData: any[] = []; // كامل مع id + name
  governorateNames: string[] = [];
  editAreas: string[] = [];
  editAreasLoading = false;
  editCenterGroups: any[] = [];
  editGroupsLoading = false;

  selectedStudent = signal<Student | null>(null);
  activeDetailTab = signal('الملف الشخصي');
  detailTabs = ['الملف الشخصي', 'المحفظة', 'الحضور', 'الاشتراكات', 'الاختبارات'];
  transactions = signal<WalletTransaction[]>([]);
  attendance = signal<AttendanceRecord[]>([]);
  enrollments = signal<Enrollment[]>([]);
  quizAttempts = signal<QuizAttempt[]>([]);

  editingStudent = signal<Student | null>(null);
  editForm: any = {};
  uploadingProfile = false;
  uploadingId = false;

  lightboxUrl = signal<string | null>(null);
  rejectingId = signal(0);
  rejectReason = '';
  blockingId = signal(0);
  blockReason = '';

  private route = inject(ActivatedRoute);
  levelName = signal('');

  constructor(private api: ApiService, private toastr: ToastrService) {}

  private applyLevelFromParam(levelId: string | null) {
    if (levelId && this.levelsData.length) {
      const level = this.levelsData.find((l: any) => String(l.id) === levelId);
      if (level) {
        this.filterGrade = level.name;
        this.levelName.set(level.name);
      }
    } else if (!levelId) {
      this.filterGrade = '';
      this.levelName.set('');
    }
    this.activeTab.set(this.router.url.startsWith('/students') || this.router.url.includes('/level/') ? 'ACTIVE' : 'PENDING');
    this.currentPage.set(0);
    this._filterTick.update(v => v + 1);
    this.loadStudents();
  }

  ngOnInit(): void {
    const isManage = this.router.url.startsWith('/students') || this.router.url.includes('/level/');
    this.pageMode.set(isManage ? 'manage' : 'requests');
    this.activeTab.set(isManage ? 'ACTIVE' : 'PENDING');

    this.api.getCenters().subscribe({ next: (r: any) => this.centers = Array.isArray(r) ? r : (r?.data || []), error: () => {} });
    this.api.getGovernorateNames().subscribe({ next: (r: any) => this.governorateNames = Array.isArray(r) ? r : [], error: () => {} });

    // تابع تغييرات الـ params عشان لما المستخدم ينتقل بين /level/1 و /level/2
    this.route.paramMap.subscribe(params => {
      const levelId = params.get('levelId');
      const isManageMode = this.router.url.startsWith('/students') || this.router.url.includes('/level/');
      this.pageMode.set(isManageMode ? 'manage' : 'requests');
      if (this.levelsData.length) {
        this.applyLevelFromParam(levelId);
      }
      // لو الـ levels لسه متحملتش، هيتعالج في next اللي جوا getLevels
    });

    this.api.getLevels().subscribe({
      next: (levels: any[]) => {
        this.levelsData = levels || [];
        this.grades = (levels || []).map((l: any) => l.name).filter(Boolean);
        // لو في levelId في الـ URL، افلتر الصف تلقائياً
        const levelId = this.route.snapshot.paramMap.get('levelId');
        if (levelId) {
          const level = (levels || []).find((l: any) => String(l.id) === levelId);
          if (level) {
            this.filterGrade = level.name;
            this.levelName.set(level.name);
            this._filterTick.update(v => v + 1);
          }
        }
        this.loadStudents();
      },
      error: () => { this.loadStudents(); }
    });
  }

  switchTab(tab: TabType) {
    this.activeTab.set(tab); this.currentPage.set(0);
    this.searchQuery = ''; this.filterGovernorate = ''; this.filterCenter = ''; this.filterType = '';
    // لو في صفحة مستوى مقفول، لا نمسح فلتر الصف
    if (!this.levelName()) this.filterGrade = '';
    this._filterTick.update(v => v + 1);
    this.loadStudents();
  }

  triggerFilter() { this._filterTick.update(v => v + 1); }
  clearFilters() {
    this.searchQuery = ''; this.filterGovernorate = ''; this.filterCenter = ''; this.filterType = '';
    // لا نمسح الصف لو مقفول من الـ URL
    if (!this.levelName()) this.filterGrade = '';
    this._filterTick.update(v => v + 1);
  }

  loadStudents(page = 0) {
    this.loading.set(true);
    const tab = this.activeTab();
    const grade = this.levelName() ? this.filterGrade : undefined;
    const obs = tab === 'PENDING' ? this.api.getPendingStudents(page, 200, grade) :
                tab === 'ACTIVE'  ? this.api.getActiveStudents(page, 200, '', grade) :
                tab === 'BLOCKED' ? this.api.getBannedStudents(page, 200, grade) :
                                    this.api.getRejectedStudents(page, 200);
    obs.subscribe({
      next: (res) => {
        const pg = extractPage<Student>(res);
        this.students.set(pg.content || []);
        this.totalPages.set(pg.totalPages || 0);
        this.totalElements.set(pg.totalElements || 0);
        this.currentPage.set(pg.number || 0);
        this.loading.set(false);
      },
      error: () => { this.loading.set(false); }
    });
  }

  pageRange(): number[] {
    const t = this.totalPages(), c = this.currentPage();
    const s = Math.max(0, c - 2), e = Math.min(t - 1, c + 2);
    const r: number[] = [];
    for (let i = s; i <= e; i++) r.push(i);
    return r;
  }

  sortBy(col: string) {
    if (this.sortCol() === col) {
      const next: SortDir = this.sortDir() === 'asc' ? 'desc' : this.sortDir() === 'desc' ? null : 'asc';
      this.sortDir.set(next);
      if (!next) this.sortCol.set('');
    } else { this.sortCol.set(col); this.sortDir.set('asc'); }
  }

  sortIcon(col: string): string {
    if (this.sortCol() !== col) return 'unfold_more';
    return this.sortDir() === 'asc' ? 'arrow_upward' : 'arrow_downward';
  }

  statusClass(status?: string): string {
    return status === 'ACTIVE'   ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' :
           status === 'PENDING'  ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30' :
           status === 'BLOCKED'  ? 'bg-red-500/20 text-red-400 border border-red-500/30' :
           status === 'REJECTED' ? 'bg-slate-500/20 text-slate-400 border border-slate-500/30' : '';
  }

  statusLabel(status?: string): string {
    return status === 'ACTIVE' ? 'نشط' : status === 'PENDING' ? 'معلق' :
           status === 'BLOCKED' ? 'محظور' : status === 'REJECTED' ? 'مرفوض' : '—';
  }

  openLightbox(url: string) { this.lightboxUrl.set(url); }

  openWalletHistory(student: Student) {
    this.router.navigate(['/wallet-history', student.id]);
  }

  viewDetails(student: Student) {
    this.selectedStudent.set(student);
    this.activeDetailTab.set('الملف الشخصي');
    this.transactions.set([]); this.attendance.set([]); this.enrollments.set([]); this.quizAttempts.set([]);
    this.api.getStudentWalletTransactions(student.id).subscribe({ next: r => this.transactions.set(r || []), error: () => {} });
    this.api.getStudentAttendance(student.id).subscribe({ next: r => this.attendance.set(r || []), error: () => {} });
    this.api.getStudentEnrollments(student.id).subscribe({ next: r => this.enrollments.set(r || []), error: () => {} });
    this.api.getStudentQuizAttempts(student.id).subscribe({ next: r => this.quizAttempts.set(r || []), error: () => {} });
  }

  openEditModal(student: Student) {
    this.editingStudent.set(student);
    this.editAreas = [];
    this.editCenterGroups = [];
    this.editForm = {
      firstName: student.firstName || '', secondName: student.secondName || '',
      thirdName: student.thirdName || '', fourthName: student.fourthName || '',
      phone: student.phone || '', parentPhone: student.parentPhone || '',
      newPassword: '', confirmPassword: '',
      governorate: student.governorate || '', area: student.area || '',
      grade: student.grade || '', schoolName: student.schoolName || '',
      educationDepartment: (student as any).educationDepartment || '',
      studyType: student.online ? 'ONLINE' : 'CENTER',
      centerName: student.centerName || '',
      groupId: (student as any).groupId || null,
      profileImageUrl: student.profileImageUrl || '',
      identityDocumentUrl: student.identityDocumentUrl || ''
    };
    if (student.governorate) {
      this.onEditGovernorateChange();
    }
    if (!student.online) {
      this.loadEditGroups();
    }
  }

  loadEditGroups() {
    this.editGroupsLoading = true;
    // جيب الـ levelId من اسم الصف
    const gradeName = this.editForm.grade;
    const level = this.levelsData.find((l: any) => l.name === gradeName);
    const levelId = level?.id;
    this.api.getMyGroups(levelId).subscribe({
      next: (groups: any[]) => {
        const centerName = this.editForm.centerName;
        this.editCenterGroups = centerName
          ? (groups || []).filter((g: any) => g.centerName === centerName || g.center?.name === centerName)
          : (groups || []);
        this.editGroupsLoading = false;
      },
      error: () => { this.editCenterGroups = []; this.editGroupsLoading = false; }
    });
  }

  onEditGovernorateChange() {
    const gov = this.editForm.governorate;
    if (!gov) { this.editAreas = []; return; }
    this.editAreasLoading = true;
    this.api.getAreaNames(gov).subscribe({
      next: (areas: string[]) => { this.editAreas = areas || []; this.editAreasLoading = false; },
      error: () => { this.editAreas = []; this.editAreasLoading = false; }
    });
  }

  onImageUpload(event: Event, type: 'profile' | 'id') {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    if (type === 'profile') {
      this.uploadingProfile = true;
      this.api.uploadFile(file).subscribe({
        next: (url: string) => { this.editForm.profileImageUrl = url; this.uploadingProfile = false; },
        error: () => { this.toastr.error('فشل رفع الصورة الشخصية'); this.uploadingProfile = false; }
      });
    } else {
      this.uploadingId = true;
      this.api.uploadFile(file).subscribe({
        next: (url: string) => { this.editForm.identityDocumentUrl = url; this.uploadingId = false; },
        error: () => { this.toastr.error('فشل رفع صورة البطاقة'); this.uploadingId = false; }
      });
    }
  }

  saveEdit() {
    const id = this.editingStudent()?.id;
    if (!id) return;
    if (this.editForm.newPassword && this.editForm.newPassword !== this.editForm.confirmPassword) {
      this.toastr.error('كلمتا المرور غير متطابقتين'); return;
    }
    const payload: any = {
      firstName: this.editForm.firstName,
      secondName: this.editForm.secondName,
      thirdName: this.editForm.thirdName,
      fourthName: this.editForm.fourthName,
      phone: this.editForm.phone,
      parentPhone: this.editForm.parentPhone,
      grade: this.editForm.grade,
      governorate: this.editForm.governorate,
      area: this.editForm.area,
      schoolName: this.editForm.schoolName,
      educationDepartment: this.editForm.educationDepartment,
      studyType: this.editForm.studyType,
      centerName: this.editForm.centerName || null,
      attendanceGroupId: this.editForm.groupId || null,
      profileImageUrl: this.editForm.profileImageUrl || null,
      identityDocumentUrl: this.editForm.identityDocumentUrl || null,
    };
    if (this.editForm.newPassword) payload.password = this.editForm.newPassword;
    this.api.updateStudent(id, payload).subscribe({
      next: () => {
        this.toastr.success('تم حفظ التعديلات بنجاح');
        this.editingStudent.set(null);
        this.loadStudents(this.currentPage());
      },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ أثناء الحفظ')
    });
  }

  approveStudent(id: number) {
    this.api.approveStudent(id).subscribe({
      next: () => { this.toastr.success('تم قبول الطالب'); this.loadStudents(this.currentPage()); },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  openRejectModal(id: number) { this.rejectReason = ''; this.rejectingId.set(id); }

  confirmReject() {
    const id = this.rejectingId();
    if (!id) return;
    if (!this.rejectReason.trim()) { this.toastr.warning('يرجى كتابة سبب الرفض'); return; }
    this.api.rejectStudent(id, this.rejectReason).subscribe({
      next: () => { this.toastr.success('تم رفض الطالب'); this.rejectingId.set(0); this.loadStudents(this.currentPage()); },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  openBlockModal(id: number) { this.blockReason = ''; this.blockingId.set(id); }

  confirmBlock() {
    const id = this.blockingId();
    if (!id) return;
    this.api.blockStudent(id, this.blockReason).subscribe({
      next: () => { this.toastr.success('تم حظر الطالب'); this.blockingId.set(0); this.loadStudents(this.currentPage()); },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  unblockStudent(id: number) {
    if (!confirm('هل تريد رفع الحظر عن هذا الطالب؟')) return;
    this.api.unblockStudent(id).subscribe({
      next: () => { this.toastr.success('تم رفع الحظر'); this.loadStudents(this.currentPage()); },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  deleteStudent(id: number) {
    if (!confirm('هل تريد حذف هذا الطالب نهائيا؟ لا يمكن التراجع.')) return;
    this.api.deleteStudent(id).subscribe({
      next: () => { this.toastr.success('تم الحذف'); this.loadStudents(this.currentPage()); },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  clearStudentDevice(id: number) {
    if (!confirm('هل تريد تنظيف الجهاز المسجل لهذا الطالب؟\nسيتمكن من تسجيل الدخول من جهاز جديد.')) return;
    this.api.clearStudentDevice(id).subscribe({
      next: () => this.toastr.success('تم تنظيف الجهاز — يمكن للطالب الدخول من جهاز جديد'),
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  autoApproveAll() {
    const pending = this.displayedStudents();
    if (!pending.length) return;
    if (!confirm(`هل تريد قبول ${pending.length} طالب دفعة واحدة؟`)) return;
    let done = 0, failed = 0;
    pending.forEach(s => {
      this.api.approveStudent(s.id).subscribe({
        next: () => { done++; if (done + failed === pending.length) { this.toastr.success(`تم قبول ${done} طالب`); this.loadStudents(); } },
        error: () => { failed++; if (done + failed === pending.length) { this.toastr.success(`تم قبول ${done}`); if (failed) this.toastr.error(`فشل ${failed}`); this.loadStudents(); } }
      });
    });
  }

  exportToExcel() {
    const students = this.displayedStudents();
    if (!students.length) return;
    const tab = this.activeTab();
    const now = new Date();
    const date = `${now.getFullYear()}${String(now.getMonth()+1).padStart(2,'0')}${String(now.getDate()).padStart(2,'0')}`;
    const time = `${String(now.getHours()).padStart(2,'0')}${String(now.getMinutes()).padStart(2,'0')}${String(now.getSeconds()).padStart(2,'0')}`;
    const isRequests = this.pageMode() === 'requests';
    const headers = isRequests
      ? ['الكود','الاسم','هاتف الطالب','هاتف الوالد','المحافظة','المنطقة','الصف','السنتر','نوع الدراسة','المدرسة','الحالة','تاريخ التسجيل']
      : ['الكود','الاسم','هاتف الطالب','هاتف الوالد','المحافظة','المنطقة','الصف','السنتر','الجروب','نوع الدراسة','المدرسة','الحالة','تاريخ التسجيل'];
    const rows = students.map((s: any) => isRequests
      ? [s.studentCode||'',s.fullName||'',s.phone||'',s.parentPhone||'',s.governorate||'',s.area||'',s.grade||'',s.centerName||'',s.online?'أونلاين':'سنتر',s.schoolName||'',s.status||'',s.createdAt?new Date(s.createdAt).toLocaleString('ar-EG'):'']
      : [s.studentCode||'',s.fullName||'',s.phone||'',s.parentPhone||'',s.governorate||'',s.area||'',s.grade||'',s.centerName||'',s.groupName||'',s.online?'أونلاين':'سنتر',s.schoolName||'',s.status||'',s.createdAt?new Date(s.createdAt).toLocaleString('ar-EG'):'']
    );
    const csv = '\ufeff' + [headers, ...rows].map((r: any[]) => r.map((c: any) => `"${String(c).replace(/"/g,'""')}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = `students_${tab}_${date}_${time}.csv`; a.click();
    URL.revokeObjectURL(url);
  }

  filterArabicInput(event: Event, field: string) {
    const input = event.target as HTMLInputElement;
    const filtered = input.value.replace(/[^\u0600-\u06FF\s]/g, '');
    if (input.value !== filtered) {
      input.value = filtered;
      this.editForm[field] = filtered;
    }
  }
}
