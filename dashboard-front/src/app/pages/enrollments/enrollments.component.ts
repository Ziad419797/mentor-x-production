import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Enrollment, Course, Student } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { extractItem } from '../../core/api-response.model';

@Component({
  selector: 'app-enrollments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">
      
      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-white font-bold text-2xl">الاشتراكات</h2>
          <p class="text-slate-500 text-sm mt-1">إدارة اشتراكات الطلاب في المستويات يدوياً</p>
        </div>
        <button (click)="openEnrollModal()" class="btn-primary">
          <span class="material-icons-round">how_to_reg</span>
          اشتراك يدوي جديد
        </button>
      </div>

      <!-- Filters -->
      <div class="edu-card bg-slate-900/40 p-4 grid grid-cols-1 md:grid-cols-3 gap-4">
        <select (change)="onCourseChange($event)" class="edu-select">
           <option value="0">كل المستويات</option>
           <option *ngFor="let c of courses()" [value]="c.id">{{ c.title }}</option>
        </select>
        <div class="relative">
           <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500">search</span>
           <input type="text" [(ngModel)]="searchQuery" (input)="onSearch()" class="edu-input pr-10" placeholder="ابحث باسم الطالب أو رقم الهاتف...">
        </div>
        <select (change)="onStatusChange($event)" class="edu-select">
           <option value="ALL">كل الحالات</option>
           <option value="ACTIVE">نشط</option>
           <option value="EXPIRED">منتهي</option>
        </select>
      </div>

      <!-- Enrollments Table -->
      <div class="edu-card p-0 overflow-hidden shadow-xl">
        <table class="edu-table">
          <thead>
            <tr>
              <th>الطالب</th>
              <th>المستوى</th>
              <th>تاريخ الاشتراك</th>
              <th>تاريخ الانتهاء</th>
              <th>طريقة الدفع</th>
              <th class="text-center">الحالة</th>
              <th class="text-center">إجراءات</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let e of enrollments(); trackBy: trackByEnrollment">
              <td>
                <div class="flex flex-col">
                  <span class="text-slate-200 font-bold text-xs">{{ e.studentName }}</span>
                  <span class="text-[10px] text-slate-500 ltr text-right">#{{ e.studentId }}</span>
                </div>
              </td>
              <td>
                 <span class="text-xs text-slate-300 font-medium">{{ e.courseName }}</span>
              </td>
              <td class="text-[10px] text-slate-500">{{ e?.enrolledAt | date:'mediumDate' }}</td>
              <td class="text-[10px] text-slate-500">{{ e?.expiryDate | date:'mediumDate' }}</td>
              <td>
                 <span class="text-[10px] bg-slate-800 px-2 py-1 rounded border border-slate-700 text-slate-400">
                    {{ e.paymentMethod || 'يدوي' }}
                 </span>
              </td>
              <td>
                 <div class="flex justify-center">
                   <span [class]="e?.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'">
                     {{ e?.status === 'ACTIVE' ? 'نشط' : 'منتهي' }}
                   </span>
                 </div>
              </td>
              <td>
                <div class="flex items-center justify-center gap-2">
                   <button (click)="cancelEnrollment(e?.id || 0)" class="btn-icon text-red-400" title="إلغاء الاشتراك">
                     <span class="material-icons-round text-sm">cancel</span>
                   </button>
                </div>
              </td>
            </tr>
            <tr *ngIf="enrollments().length === 0">
               <td colspan="7" class="text-center py-24 text-slate-600 italic">لا توجد اشتراكات مطابقة للبحث</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div *ngIf="totalPages() > 1" class="flex justify-center gap-2 mt-6">
         <button *ngFor="let p of getPageNumbers(); trackBy: trackByIndex"
                 (click)="loadEnrollments(p)"
                 [class.bg-indigo-600]="currentPage() === p"
                 class="w-10 h-10 rounded-lg bg-slate-800 border border-slate-700 text-white font-bold hover:bg-indigo-600 transition-colors">
            {{ p + 1 }}
         </button>
      </div>

      <!-- Manual Enrollment Modal -->
      <div *ngIf="showModal()" class="modal-overlay">
        <div class="modal-box max-w-md">
           <div class="modal-header">
              <h3 class="text-white font-bold">اشتراك يدوي جديد</h3>
              <button (click)="showModal.set(false)" class="btn-icon"><span class="material-icons-round">close</span></button>
           </div>
           <div class="modal-body space-y-6">
              <!-- Search Student -->
              <div>
                 <label class="edu-label">بحث عن طالب (بالهاتف)</label>
                 <div class="flex gap-2">
                    <input type="tel" [(ngModel)]="searchPhone" class="edu-input ltr text-right" placeholder="01xxxxxxxxx">
                    <button (click)="searchStudent()" class="btn-secondary h-11"><span class="material-icons-round">search</span></button>
                 </div>
              </div>

              <!-- Student Brief -->
              <div *ngIf="foundStudent()" class="p-4 bg-slate-800/40 rounded-xl border border-slate-700 flex items-center justify-between animate-slide-up">
                 <div class="flex items-center gap-3">
                    <div class="w-8 h-8 rounded-full bg-indigo-500/10 flex items-center justify-center text-indigo-400 text-xs font-bold">
                       {{ foundStudent()?.fullName?.[0] }}
                    </div>
                    <span class="text-slate-200 text-xs font-bold">{{ foundStudent()?.fullName }}</span>
                 </div>
                 <span class="material-icons-round text-emerald-400">check_circle</span>
              </div>

              <div>
                <label class="edu-label">اختر المستوى</label>
                <select [(ngModel)]="enrollForm.courseId" class="edu-select">
                  <option [value]="0">اختر المستوى</option>
                  <option *ngFor="let c of courses()" [value]="c.id">{{ c.title }}</option>
                </select>
              </div>

           </div>
           <div class="modal-footer">
              <button (click)="showModal.set(false)" class="btn-secondary">إلغاء</button>
              <button (click)="performEnrollment()" [disabled]="!foundStudent() || !enrollForm.courseId" class="btn-primary px-8">إتمام الاشتراك</button>
           </div>
        </div>
      </div>

    </div>
  `
})
export class EnrollmentsComponent implements OnInit {
  enrollments = signal<Enrollment[]>([]);
  courses = signal<Course[]>([]);
  totalPages = signal(0);
  currentPage = signal(0);
  
  selectedCourseId = 0;
  selectedStatus = 'ALL';
  searchQuery = '';

  showModal = signal(false);
  searchPhone = '';
  foundStudent = signal<Student | null>(null);
  enrollForm = { courseId: 0 };

  constructor(private api: ApiService, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.api.getCourses(0, 100).subscribe({
      next: (pg) => {
        this.courses.set(pg.content || []);
      },
      error: (err) => {
        console.error('Failed to load courses:', err);
        this.courses.set([]);
      }
    });
    this.loadEnrollments();
  }

  loadEnrollments(page = 0) {
    this.api.getEnrollments(page, 20, this.selectedCourseId, this.selectedStatus, this.searchQuery).subscribe({
      next: (pg) => {
        this.enrollments.set(pg.content || []);
        this.totalPages.set(pg.totalPages || 0);
        this.currentPage.set(pg.number || 0);
      },
      error: (err) => {
        console.error('Failed to load enrollments:', err);
        this.enrollments.set([]);
        this.totalPages.set(0);
      }
    });
  }

  onCourseChange(event: any) {
    this.selectedCourseId = Number(event.target.value);
    this.currentPage.set(0);
    this.loadEnrollments();
  }

  onStatusChange(event: any) {
    this.selectedStatus = event.target.value;
    this.currentPage.set(0);
    this.loadEnrollments();
  }

  onSearch() {
    this.currentPage.set(0);
    this.loadEnrollments();
  }

  openEnrollModal() {
    this.foundStudent.set(null);
    this.searchPhone = '';
    this.showModal.set(true);
  }

  searchStudent() {
    if (!this.searchPhone) return;
    this.api.searchStudentByPhone(this.searchPhone).subscribe({
      next: (s) => {
        const item = extractItem<Student>(s);
        this.foundStudent.set(item);
        if (!item) this.toastr.error('الطالب غير موجود');
      },
      error: (err) => {
        console.error('Failed to search student:', err);
        this.toastr.error('الطالب غير موجود');
      }
    });
  }

  getPageNumbers(): number[] {
    const pages = [];
    for (let i = 0; i < this.totalPages(); i++) pages.push(i);
    return pages;
  }

  trackByIndex(index: number): number {
    return index;
  }

  performEnrollment() {
    if (!this.foundStudent() || !this.enrollForm.courseId) return;
    this.api.manualEnroll({
      studentId: this.foundStudent()!.id,
      courseId: this.enrollForm.courseId
    }).subscribe({
      next: () => {
        this.toastr.success('تم الاشتراك بنجاح');
        this.showModal.set(false);
        this.loadEnrollments();
      },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ أثناء الاشتراك')
    });
  }

  cancelEnrollment(id: number) {
    if (!id || !confirm('هل أنت متأكد من إلغاء هذا الاشتراك؟ سيتم منع الطالب من الوصول لمحتوى المستوى.')) return;
    this.api.cancelEnrollment(id).subscribe({
      next: () => {
        this.toastr.success('تم إلغاء الاشتراك');
        this.loadEnrollments(this.currentPage());
      },
      error: (err) => {
        console.error('Failed to cancel enrollment:', err);
        this.toastr.error('حدث خطأ أثناء إلغاء الاشتراك');
      }
    });
  }

  trackByEnrollment(index: number, e: Enrollment): number {
    return e?.id || index;
  }
}
