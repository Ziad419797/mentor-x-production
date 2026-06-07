import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastrService } from 'ngx-toastr';
import { Subscription } from 'rxjs';
import { Student } from '../../models/models';
import { extractPage } from '../../core/api-response.model';

@Component({
  selector: 'app-level-students',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">

      <!-- Header -->
      <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div class="flex items-center gap-3">
          <a [routerLink]="['/level', levelId(), 'data']"
             class="flex items-center gap-1 text-slate-500 hover:text-slate-300 text-sm transition-colors">
            <span class="material-icons-round text-base">arrow_forward</span>
          </a>
          <div class="w-10 h-10 rounded-xl bg-emerald-500/10 flex items-center justify-center">
            <span class="material-icons-round text-emerald-400">groups</span>
          </div>
          <div>
            <h2 class="text-white font-bold text-xl">طلاب {{ levelName() }}</h2>
            <p class="text-slate-500 text-xs mt-0.5">إدارة وقبول طلاب هذا الصف الدراسي</p>
          </div>
        </div>

        <!-- Tabs -->
        <div class="flex items-center gap-1 p-1 bg-slate-900/50 rounded-xl border border-slate-700/50">
          <button (click)="switchTab('PENDING')" [class.active]="activeTab() === 'PENDING'" class="edu-tab px-5 text-xs">
            معلقون
            <span *ngIf="pendingCount() > 0" class="ml-1 bg-amber-500 text-white text-[9px] rounded-full px-1.5 py-0.5">{{ pendingCount() }}</span>
          </button>
          <button (click)="switchTab('ACTIVE')" [class.active]="activeTab() === 'ACTIVE'" class="edu-tab px-5 text-xs">نشطون</button>
          <button (click)="switchTab('BLOCKED')" [class.active]="activeTab() === 'BLOCKED'" class="edu-tab px-5 text-xs">محظورون</button>
        </div>
      </div>

      <!-- Search (active/blocked only) -->
      <div *ngIf="activeTab() !== 'PENDING'" class="relative max-w-sm">
        <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-base">search</span>
        <input type="text" [(ngModel)]="searchQuery" (input)="onSearch()"
               class="edu-input pr-10" placeholder="ابحث بالاسم أو الكود...">
      </div>

      <!-- PENDING — Card Grid -->
      <div *ngIf="activeTab() === 'PENDING'">
        <div *ngIf="loading()" class="flex justify-center py-16">
          <span class="material-icons-round animate-spin text-slate-500 text-3xl">refresh</span>
        </div>

        <div *ngIf="!loading() && students().length === 0" class="edu-card text-center py-16">
          <span class="material-icons-round text-5xl text-slate-700 block mb-3">person_search</span>
          <p class="text-slate-500">لا يوجد طلاب معلقون في هذا الصف</p>
        </div>

        <div *ngIf="!loading()" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          <div *ngFor="let s of students()" class="edu-card p-0 overflow-hidden group hover:border-slate-700 transition-colors">
            <div class="h-28 bg-slate-900 relative overflow-hidden">
              <img *ngIf="s.profileImageUrl" [src]="s.profileImageUrl" class="w-full h-full object-cover opacity-60">
              <div *ngIf="!s.profileImageUrl" class="w-full h-full flex items-center justify-center text-slate-700">
                <span class="material-icons-round text-5xl">person</span>
              </div>
              <div class="absolute bottom-2 right-3 flex gap-1">
                <span class="badge-warning text-[10px]">{{ s.online ? 'أونلاين' : 'سنتر' }}</span>
              </div>
            </div>
            <div class="p-4 space-y-3">
              <div>
                <h4 class="text-white font-bold truncate">{{ s.fullName }}</h4>
                <p class="text-slate-500 text-[10px] ltr text-right mt-0.5">{{ s.phone }}</p>
              </div>
              <div class="flex items-center gap-1.5 text-[10px] text-slate-400">
                <span class="material-icons-round text-sm">location_on</span>
                {{ s.governorate }} - {{ s.area }}
              </div>
              <div *ngIf="s.centerName" class="flex items-center gap-1.5 text-[10px] text-slate-400">
                <span class="material-icons-round text-sm">corporate_fare</span>
                {{ s.centerName }}
              </div>
              <div class="flex gap-2 pt-1">
                <button (click)="approveStudent(s)" class="btn-success flex-1 h-9 text-xs justify-center gap-1">
                  <span class="material-icons-round text-sm">check</span> قبول
                </button>
                <button (click)="openRejectModal(s)" class="btn-danger flex-1 h-9 text-xs justify-center gap-1">
                  <span class="material-icons-round text-sm">close</span> رفض
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div *ngIf="totalPages() > 1" class="flex justify-center gap-2 mt-6">
          <button (click)="prevPage()" [disabled]="currentPage() === 0" class="btn-secondary px-3 py-1.5 text-xs">
            <span class="material-icons-round text-sm">navigate_next</span>
          </button>
          <span class="text-slate-400 text-xs flex items-center px-3">{{ currentPage() + 1 }} / {{ totalPages() }}</span>
          <button (click)="nextPage()" [disabled]="currentPage() + 1 >= totalPages()" class="btn-secondary px-3 py-1.5 text-xs">
            <span class="material-icons-round text-sm">navigate_before</span>
          </button>
        </div>
      </div>

      <!-- ACTIVE / BLOCKED — Table -->
      <div *ngIf="activeTab() !== 'PENDING'">
        <div *ngIf="loading()" class="flex justify-center py-16">
          <span class="material-icons-round animate-spin text-slate-500 text-3xl">refresh</span>
        </div>
        <div *ngIf="!loading()" class="edu-card p-0 overflow-hidden">
          <table class="edu-table">
            <thead>
              <tr>
                <th>الطالب</th>
                <th>الكود</th>
                <th>المحافظة</th>
                <th>السنتر</th>
                <th>الحضور</th>
                <th class="text-center">إجراءات</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let s of students()">
                <td>
                  <div class="flex items-center gap-3">
                    <div class="w-9 h-9 rounded-full bg-slate-800 overflow-hidden flex items-center justify-center shrink-0">
                      <img *ngIf="s.profileImageUrl" [src]="s.profileImageUrl" class="w-full h-full object-cover">
                      <span *ngIf="!s.profileImageUrl" class="material-icons-round text-slate-500 text-lg">person</span>
                    </div>
                    <div>
                      <p class="text-white font-semibold text-sm">{{ s.fullName }}</p>
                      <p class="text-slate-500 text-[10px] ltr">{{ s.phone }}</p>
                    </div>
                  </div>
                </td>
                <td><span class="font-mono text-indigo-400 text-xs">{{ s.studentCode }}</span></td>
                <td class="text-slate-400 text-xs">{{ s.governorate }}</td>
                <td class="text-slate-400 text-xs">{{ s.centerName || '—' }}</td>
                <td>
                  <span class="text-slate-400 text-xs flex items-center gap-1">
                    <span class="material-icons-round text-sm text-emerald-500">how_to_reg</span>
                    {{ s.attendanceCount ?? 0 }}
                  </span>
                </td>
                <td class="text-center">
                  <div class="flex justify-center gap-1">
                    <button *ngIf="activeTab() === 'ACTIVE'" (click)="blockStudent(s)"
                            class="flex items-center gap-1 px-2 py-1 rounded-lg bg-red-900/20 text-red-400 border border-red-800/30 text-[10px] font-bold hover:opacity-80 transition-colors">
                      <span class="material-icons-round text-[11px]">block</span> حظر
                    </button>
                    <button *ngIf="activeTab() === 'BLOCKED'" (click)="unblockStudent(s)"
                            class="flex items-center gap-1 px-2 py-1 rounded-lg bg-emerald-900/20 text-emerald-400 border border-emerald-800/30 text-[10px] font-bold hover:opacity-80 transition-colors">
                      <span class="material-icons-round text-[11px]">lock_open</span> رفع الحظر
                    </button>
                  </div>
                </td>
              </tr>
              <tr *ngIf="students().length === 0">
                <td colspan="6" class="text-center py-16 text-slate-600">لا يوجد طلاب</td>
              </tr>
            </tbody>
          </table>

          <!-- Pagination -->
          <div *ngIf="totalPages() > 1" class="flex justify-center gap-2 p-4 border-t border-slate-800">
            <button (click)="prevPage()" [disabled]="currentPage() === 0" class="btn-secondary px-3 py-1.5 text-xs">
              <span class="material-icons-round text-sm">navigate_next</span>
            </button>
            <span class="text-slate-400 text-xs flex items-center px-3">{{ currentPage() + 1 }} / {{ totalPages() }}</span>
            <button (click)="nextPage()" [disabled]="currentPage() + 1 >= totalPages()" class="btn-secondary px-3 py-1.5 text-xs">
              <span class="material-icons-round text-sm">navigate_before</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Reject Modal -->
      <div *ngIf="showRejectModal()" class="modal-overlay">
        <div class="modal-box max-w-sm">
          <div class="modal-header">
            <h3 class="text-white font-bold">رفض الطالب</h3>
            <button (click)="showRejectModal.set(false)" class="btn-icon">
              <span class="material-icons-round">close</span>
            </button>
          </div>
          <div class="modal-body space-y-3">
            <p class="text-slate-400 text-sm">سيتم رفض طلب تسجيل <span class="text-white font-bold">{{ rejectTarget()?.fullName }}</span></p>
            <div>
              <label class="edu-label">سبب الرفض <span class="text-red-400">*</span></label>
              <textarea [(ngModel)]="rejectReason" rows="3" class="edu-input resize-none" placeholder="اكتب سبب الرفض..."></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button (click)="showRejectModal.set(false)" class="btn-secondary">إلغاء</button>
            <button (click)="confirmReject()" [disabled]="!rejectReason.trim()" class="btn-danger gap-2">
              تأكيد الرفض
            </button>
          </div>
        </div>
      </div>

    </div>
  `
})
export class LevelStudentsComponent implements OnInit, OnDestroy {
  levelId   = signal(0);
  levelName = signal('');
  activeTab = signal<'PENDING' | 'ACTIVE' | 'BLOCKED'>('PENDING');
  students  = signal<Student[]>([]);
  loading   = signal(false);
  currentPage = signal(0);
  totalPages  = signal(0);
  pendingCount = signal(0);
  searchQuery = '';

  showRejectModal = signal(false);
  rejectTarget    = signal<Student | null>(null);
  rejectReason    = '';

  private grade = '';
  private paramsSub?: Subscription;
  private searchTimer: any;

  constructor(
    private route: ActivatedRoute,
    private api: ApiService,
    private toastr: ToastrService
  ) {}

  ngOnInit() {
    this.paramsSub = this.route.params.subscribe(p => {
      this.levelId.set(Number(p['levelId']));
      // Load level name
      this.api.getLevelById(this.levelId()).subscribe({
        next: (lvl: any) => {
          const name = lvl?.name || lvl?.data?.name || '';
          this.levelName.set(name);
          this.grade = name;
          this.loadStudents();
          this.loadPendingCount();
        },
        error: () => { this.grade = ''; this.loadStudents(); }
      });
    });
  }

  ngOnDestroy() {
    this.paramsSub?.unsubscribe();
    clearTimeout(this.searchTimer);
  }

  switchTab(tab: 'PENDING' | 'ACTIVE' | 'BLOCKED') {
    this.activeTab.set(tab);
    this.currentPage.set(0);
    this.searchQuery = '';
    this.loadStudents();
  }

  loadStudents() {
    this.loading.set(true);
    const page = this.currentPage();
    const tab  = this.activeTab();
    const grade = this.grade;
    const search = this.searchQuery;

    const req$ = tab === 'PENDING'
      ? this.api.getPendingStudents(page, 12, grade)
      : tab === 'ACTIVE'
        ? this.api.getActiveStudents(page, 20, search, grade)
        : this.api.getBannedStudents(page, 20, grade);

    req$.subscribe({
      next: (pg: any) => {
        const p = extractPage<Student>(pg);
        this.students.set(p.content ?? []);
        this.totalPages.set(p.totalPages ?? 0);
        this.loading.set(false);
      },
      error: () => { this.students.set([]); this.loading.set(false); }
    });
  }

  loadPendingCount() {
    this.api.getPendingStudents(0, 1, this.grade).subscribe({
      next: (pg: any) => {
        const p = extractPage<Student>(pg);
        this.pendingCount.set(p.totalElements ?? 0);
      },
      error: () => {}
    });
  }

  onSearch() {
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => {
      this.currentPage.set(0);
      this.loadStudents();
    }, 400);
  }

  prevPage() { if (this.currentPage() > 0) { this.currentPage.set(this.currentPage() - 1); this.loadStudents(); } }
  nextPage() { if (this.currentPage() + 1 < this.totalPages()) { this.currentPage.set(this.currentPage() + 1); this.loadStudents(); } }

  approveStudent(s: Student) {
    this.api.approveStudent(s.id).subscribe({
      next: () => {
        this.students.set(this.students().filter(x => x.id !== s.id));
        this.pendingCount.set(Math.max(0, this.pendingCount() - 1));
        this.toastr.success('تم قبول الطالب');
      },
      error: () => this.toastr.error('حدث خطأ')
    });
  }

  openRejectModal(s: Student) {
    this.rejectTarget.set(s);
    this.rejectReason = '';
    this.showRejectModal.set(true);
  }

  confirmReject() {
    const s = this.rejectTarget();
    if (!s || !this.rejectReason.trim()) return;
    this.api.rejectStudent(s.id, this.rejectReason).subscribe({
      next: () => {
        this.students.set(this.students().filter(x => x.id !== s.id));
        this.pendingCount.set(Math.max(0, this.pendingCount() - 1));
        this.toastr.success('تم رفض الطالب');
        this.showRejectModal.set(false);
      },
      error: () => this.toastr.error('حدث خطأ')
    });
  }

  blockStudent(s: Student) {
    if (!confirm(`حظر الطالب "${s.fullName}"؟`)) return;
    this.api.blockStudent(s.id).subscribe({
      next: () => {
        this.students.set(this.students().filter(x => x.id !== s.id));
        this.toastr.success('تم حظر الطالب');
      },
      error: () => this.toastr.error('حدث خطأ')
    });
  }

  unblockStudent(s: Student) {
    this.api.unblockStudent(s.id).subscribe({
      next: () => {
        this.students.set(this.students().filter(x => x.id !== s.id));
        this.toastr.success('تم رفع الحظر');
      },
      error: () => this.toastr.error('حدث خطأ')
    });
  }
}
