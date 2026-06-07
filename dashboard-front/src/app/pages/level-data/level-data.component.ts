import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastrService } from 'ngx-toastr';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-level-data',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">

      <!-- Header -->
      <div class="flex items-center justify-between flex-wrap gap-3">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-orange-500/10 flex items-center justify-center">
            <span class="material-icons-round text-orange-400">school</span>
          </div>
          <div>
            <h2 class="text-white font-bold text-xl">{{ levelName() }}</h2>
            <p class="text-slate-500 text-xs mt-0.5">نظرة عامة على الصف الدراسي</p>
          </div>
        </div>
      </div>

      <!-- Stats Row 1 -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="edu-card p-4 flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-orange-500/10 flex items-center justify-center shrink-0">
            <span class="material-icons-round text-orange-400 text-lg">label</span>
          </div>
          <div>
            <p class="text-slate-500 text-xs">التصنيفات</p>
            <p class="text-white font-bold text-xl">{{ stats()?.categoriesCount ?? '—' }}</p>
            <p class="text-slate-600 text-[10px]">عدد التصنيفات</p>
          </div>
        </div>
        <div class="edu-card p-4 flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-indigo-500/10 flex items-center justify-center shrink-0">
            <span class="material-icons-round text-indigo-400 text-lg">library_books</span>
          </div>
          <div>
            <p class="text-slate-500 text-xs">الكورسات</p>
            <p class="text-white font-bold text-xl">{{ stats()?.coursesCount ?? '—' }}</p>
            <p class="text-slate-600 text-[10px]">إجمالي الكورسات</p>
          </div>
        </div>
        <div class="edu-card p-4 flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-purple-500/10 flex items-center justify-center shrink-0">
            <span class="material-icons-round text-purple-400 text-lg">play_circle</span>
          </div>
          <div>
            <p class="text-slate-500 text-xs">المحاضرات</p>
            <p class="text-white font-bold text-xl">{{ stats()?.sessionsCount ?? '—' }}</p>
            <p class="text-slate-600 text-[10px]">إجمالي المحاضرات</p>
          </div>
        </div>
        <div class="edu-card p-4 flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-emerald-500/10 flex items-center justify-center shrink-0">
            <span class="material-icons-round text-emerald-400 text-lg">groups</span>
          </div>
          <div>
            <p class="text-slate-500 text-xs">الطلاب</p>
            <p class="text-white font-bold text-xl">{{ stats()?.studentsCount ?? '—' }}</p>
            <p class="text-slate-600 text-[10px]">الطلاب المسجلون</p>
          </div>
        </div>
      </div>

      <!-- Stats Row 2 -->
      <div class="grid grid-cols-2 md:grid-cols-3 gap-4">
        <div class="edu-card p-4 flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-sky-500/10 flex items-center justify-center shrink-0">
            <span class="material-icons-round text-sky-400 text-lg">videocam</span>
          </div>
          <div>
            <p class="text-slate-500 text-xs">الفيديوهات</p>
            <p class="text-white font-bold text-xl">{{ stats()?.videosCount ?? '—' }}</p>
            <p class="text-slate-600 text-[10px]">مقاطع الفيديو</p>
          </div>
        </div>
        <div class="edu-card p-4 flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-amber-500/10 flex items-center justify-center shrink-0">
            <span class="material-icons-round text-amber-400 text-lg">quiz</span>
          </div>
          <div>
            <p class="text-slate-500 text-xs">الاختبارات</p>
            <p class="text-white font-bold text-xl">{{ stats()?.quizzesCount ?? '—' }}</p>
            <p class="text-slate-600 text-[10px]">إجمالي الكويزات</p>
          </div>
        </div>
        <div class="edu-card p-4 flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-rose-500/10 flex items-center justify-center shrink-0">
            <span class="material-icons-round text-rose-400 text-lg">help_outline</span>
          </div>
          <div>
            <p class="text-slate-500 text-xs">الأسئلة</p>
            <p class="text-white font-bold text-xl">{{ stats()?.questionsCount ?? '—' }}</p>
            <p class="text-slate-600 text-[10px]">إجمالي الأسئلة</p>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="edu-card bg-slate-900/30 p-4 flex items-center gap-3 flex-wrap">
        <a [routerLink]="['/level', levelId(), 'categories']" class="btn-primary gap-2">
          <span class="material-icons-round text-base">label</span>
          التصنيفات
        </a>
        <a [routerLink]="['/level', levelId(), 'students']" class="btn-secondary gap-2">
          <span class="material-icons-round text-base">groups</span>
          الطلاب
        </a>
        <a [routerLink]="['/level', levelId(), 'add-course']" class="btn-secondary gap-2">
          <span class="material-icons-round text-base">add</span>
          إضافة كورس
        </a>
        <a [routerLink]="['/level', levelId(), 'add-category']" class="btn-secondary gap-2">
          <span class="material-icons-round text-base">create_new_folder</span>
          إضافة تصنيف
        </a>
      </div>

      <!-- Groups Section -->
      <div class="space-y-3">
        <div class="flex items-center justify-between">
          <h3 class="text-white font-bold text-base flex items-center gap-2">
            <span class="material-icons-round text-orange-400 text-lg">groups</span>
            جروبات السنتر الخاصة بهذا الصف
          </h3>
          <button (click)="openAddGroupModal()" class="btn-primary gap-2">
            <span class="material-icons-round text-base">add</span>
            إضافة جروب
          </button>
        </div>

        <div class="edu-card p-0 overflow-hidden border-slate-800 shadow-xl overflow-x-auto">
          <table class="w-full text-sm min-w-[640px]">
            <thead>
              <tr class="bg-slate-900/80 border-b border-slate-800">
                <th class="text-right px-4 py-3 text-slate-500 font-bold text-xs w-12">#</th>
                <th class="text-right px-4 py-3 text-slate-500 font-bold text-xs">اسم الجروب</th>
                <th class="text-center px-4 py-3 text-slate-500 font-bold text-xs w-24">الأعضاء</th>
                <th class="text-center px-4 py-3 text-slate-500 font-bold text-xs w-36">السنتر</th>
                <th class="text-right px-4 py-3 text-slate-500 font-bold text-xs w-40">تاريخ الإنشاء</th>
                <th class="text-center px-4 py-3 text-slate-500 font-bold text-xs w-32">الإجراءات</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let g of groups()"
                  class="border-b border-slate-800/50 hover:bg-slate-800/20 transition-colors">
                <td class="px-4 py-3 text-slate-600 font-mono text-xs">{{ g.id }}</td>
                <td class="px-4 py-3">
                  <div>
                    <span class="text-slate-200 font-bold text-sm">{{ g.title }}</span>
                    <p *ngIf="g.description" class="text-slate-500 text-xs mt-0.5 truncate max-w-[200px]">{{ g.description }}</p>
                  </div>
                </td>
                <td class="px-4 py-3 text-center">
                  <span class="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-indigo-900/20 border border-indigo-800/30 text-indigo-400 text-[10px] font-bold">
                    <span class="material-icons-round text-[11px]">person</span>
                    {{ g.membersCount ?? 0 }}
                  </span>
                </td>
                <td class="px-4 py-3 text-center">
                  <span class="text-slate-400 text-xs">{{ g.centerName || '—' }}</span>
                </td>
                <td class="px-4 py-3 text-slate-500 text-xs">{{ formatDate(g.createdAt) }}</td>
                <td class="px-4 py-3">
                  <div class="flex items-center justify-center gap-1">
                    <button (click)="openEditGroupModal(g)"
                            class="flex items-center gap-1 px-2 py-1 rounded-lg bg-indigo-900/20 text-indigo-400 border border-indigo-800/30 text-[10px] font-bold hover:opacity-80 transition-colors">
                      <span class="material-icons-round text-[11px]">edit</span>
                      تعديل
                    </button>
                    <button (click)="deleteGroup(g)"
                            class="flex items-center gap-1 px-2 py-1 rounded-lg bg-red-900/20 text-red-400 border border-red-800/30 text-[10px] font-bold hover:opacity-80 transition-colors">
                      <span class="material-icons-round text-[11px]">delete</span>
                      حذف
                    </button>
                  </div>
                </td>
              </tr>
              <tr *ngIf="groups().length === 0 && !loadingGroups()">
                <td colspan="6" class="text-center py-16">
                  <div class="flex flex-col items-center gap-3 text-slate-600">
                    <span class="material-icons-round text-5xl opacity-20">group_off</span>
                    <p class="text-sm font-medium">لا توجد جروبات لهذا الصف</p>
                    <button (click)="openAddGroupModal()" class="btn-primary mt-2">إضافة أول جروب</button>
                  </div>
                </td>
              </tr>
              <tr *ngIf="loadingGroups()">
                <td colspan="6" class="text-center py-10">
                  <span class="material-icons-round animate-spin text-slate-500">refresh</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Add / Edit Group Modal -->
      <div *ngIf="showGroupModal()" class="modal-overlay">
        <div class="modal-box max-w-md">
          <div class="modal-header">
            <h3 class="text-white font-bold">{{ editingGroup() ? 'تعديل الجروب' : 'إضافة جروب جديد' }}</h3>
            <button (click)="showGroupModal.set(false)" class="btn-icon">
              <span class="material-icons-round">close</span>
            </button>
          </div>
          <div class="modal-body space-y-4">
            <div>
              <label class="edu-label">اسم الجروب <span class="text-red-400">*</span></label>
              <input type="text" [(ngModel)]="groupForm.title" class="edu-input" placeholder="مثال: مجموعة السبت 10 صباحاً">
            </div>
            <div>
              <label class="edu-label">الوصف (اختياري)</label>
              <textarea [(ngModel)]="groupForm.description" rows="2" class="edu-input resize-none" placeholder="وصف مختصر للجروب"></textarea>
            </div>
            <div>
              <label class="edu-label">السنتر <span class="text-red-400">*</span></label>
              <select [(ngModel)]="groupForm.centerId" (ngModelChange)="onCenterChange()" class="edu-input">
                <option [ngValue]="null">— اختر السنتر —</option>
                <option *ngFor="let c of centers()" [ngValue]="c.id">{{ c.name }}{{ c.governorate ? ' — ' + c.governorate : '' }}</option>
              </select>
            </div>
            <div *ngIf="!groupForm.centerId">
              <label class="edu-label">أو اكتب اسم السنتر يدوياً</label>
              <input type="text" [(ngModel)]="groupForm.centerName" class="edu-input" placeholder="اسم السنتر">
            </div>
          </div>
          <div class="modal-footer">
            <button (click)="showGroupModal.set(false)" class="btn-secondary">إلغاء</button>
            <button (click)="saveGroup()" [disabled]="!groupForm.title.trim() || savingGroup()"
                    class="btn-primary gap-2">
              <span *ngIf="savingGroup()" class="material-icons-round animate-spin text-base">refresh</span>
              {{ savingGroup() ? 'جاري الحفظ...' : (editingGroup() ? 'حفظ التعديل' : 'إضافة') }}
            </button>
          </div>
        </div>
      </div>

    </div>
  `
})
export class LevelDataComponent implements OnInit, OnDestroy {
  levelId   = signal(0);
  levelName = signal('');
  stats     = signal<any>(null);
  groups    = signal<any[]>([]);
  centers   = signal<any[]>([]);

  loadingGroups  = signal(false);
  showGroupModal = signal(false);
  savingGroup    = signal(false);
  editingGroup   = signal<any>(null);

  groupForm: { title: string; description: string; centerId: number | null; centerName: string } =
    { title: '', description: '', centerId: null, centerName: '' };

  private paramsSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private api: ApiService,
    private toastr: ToastrService
  ) {}

  ngOnInit() {
    this.paramsSub = this.route.params.subscribe(p => {
      const id = Number(p['levelId']);
      this.levelId.set(id);
      this.loadAll(id);
    });
    this.loadCenters();
  }

  ngOnDestroy() { this.paramsSub?.unsubscribe(); }

  loadAll(id: number) {
    this.api.getLevelStats(id).subscribe({
      next: s => { if (s) { this.levelName.set(s.levelName ?? ''); this.stats.set(s); } }
    });
    this.loadGroups(id);
  }

  loadGroups(id: number) {
    this.loadingGroups.set(true);
    this.api.getMyGroups(id).subscribe({
      next: gs => { this.groups.set(gs || []); this.loadingGroups.set(false); },
      error: () => { this.groups.set([]); this.loadingGroups.set(false); }
    });
  }

  loadCenters() {
    this.api.getCenters().subscribe({
      next: (cs: any) => this.centers.set(cs || []),
      error: () => {}
    });
  }

  onCenterChange() {
    if (this.groupForm.centerId) {
      const c = this.centers().find((x: any) => x.id === this.groupForm.centerId);
      this.groupForm.centerName = c?.name ?? '';
    } else {
      this.groupForm.centerName = '';
    }
  }

  openAddGroupModal() {
    this.editingGroup.set(null);
    this.groupForm = { title: '', description: '', centerId: null, centerName: '' };
    this.showGroupModal.set(true);
  }

  openEditGroupModal(g: any) {
    this.editingGroup.set(g);
    this.groupForm = {
      title: g.title,
      description: g.description || '',
      centerId: g.centerId ?? null,
      centerName: g.centerName || ''
    };
    this.showGroupModal.set(true);
  }

  saveGroup() {
    if (!this.groupForm.title.trim()) return;
    this.savingGroup.set(true);

    const payload: any = {
      title: this.groupForm.title,
      description: this.groupForm.description || undefined,
      centerId: this.groupForm.centerId || undefined,
      centerName: !this.groupForm.centerId ? (this.groupForm.centerName || undefined) : undefined,
      levelId: this.levelId()
    };

    const editing = this.editingGroup();
    const req$ = editing
      ? this.api.updateGroup(editing.id, payload)
      : this.api.createGroup(payload);

    req$.subscribe({
      next: (g: any) => {
        if (editing) {
          this.groups.set(this.groups().map(x => x.id === editing.id ? g : x));
          this.toastr.success('تم تعديل الجروب');
        } else {
          this.groups.set([g, ...this.groups()]);
          this.toastr.success('تم إضافة الجروب');
        }
        this.showGroupModal.set(false);
        this.savingGroup.set(false);
      },
      error: (err: any) => {
        this.toastr.error(err?.error?.message || 'حدث خطأ');
        this.savingGroup.set(false);
      }
    });
  }

  deleteGroup(g: any) {
    if (!confirm(`حذف الجروب "${g.title}"؟`)) return;
    this.api.deleteGroup(g.id).subscribe({
      next: () => {
        this.groups.set(this.groups().filter(x => x.id !== g.id));
        this.toastr.success('تم حذف الجروب');
      },
      error: () => this.toastr.error('حدث خطأ')
    });
  }

  formatDate(d?: string): string {
    if (!d) return '—';
    try { return new Date(d).toLocaleDateString('ar-EG', { year: 'numeric', month: 'short', day: 'numeric' }); }
    catch { return '—'; }
  }
}
