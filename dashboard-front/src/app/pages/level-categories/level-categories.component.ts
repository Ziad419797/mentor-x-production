import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { Category } from '../../models/models';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-level-categories',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">

      <!-- Header -->
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-orange-500/10 flex items-center justify-center">
            <span class="material-icons-round text-orange-400">label</span>
          </div>
          <div>
            <h2 class="text-white font-bold text-xl">التصنيفات</h2>
            <p class="text-slate-500 text-xs mt-0.5">{{ levelName() }}</p>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <span class="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-slate-800 border border-slate-700 text-slate-400 text-xs font-bold">
            <span class="material-icons-round text-[14px]">format_list_numbered</span>
            اجمالي: {{ categories().length }}
          </span>
          <span class="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-emerald-900/30 border border-emerald-800/30 text-emerald-400 text-xs font-bold">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
            نشطة: {{ activeCount() }}
          </span>
          <span class="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-yellow-900/30 border border-yellow-800/30 text-yellow-400 text-xs font-bold">
            <span class="w-1.5 h-1.5 rounded-full bg-yellow-500"></span>
            غير نشطة: {{ inactiveCount() }}
          </span>
        </div>
      </div>

      <!-- Toolbar -->
      <div class="edu-card bg-slate-900/30 p-4 flex items-center gap-3 flex-wrap">
        <button [routerLink]="['/level', levelId(), 'add-category']" class="btn-primary gap-2">
          <span class="material-icons-round text-base">add</span>
          اضافة تصنيف
        </button>
        <button (click)="openSortModal()" class="btn-secondary gap-2">
          <span class="material-icons-round text-base">drag_indicator</span>
          ترتيب التصنيفات
        </button>
        <div class="flex-1 relative min-w-[180px]">
          <span class="material-icons-round absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 text-lg">search</span>
          <input type="text" [(ngModel)]="searchQuery" (input)="filterCategories()" placeholder="ابحث عن تصنيف..." class="edu-input pr-10">
        </div>
        <div class="flex items-center gap-1 rounded-xl border border-slate-700 p-1 bg-slate-900">
          <button (click)="setFilter('ALL')"
                  [ngClass]="statusFilter() === 'ALL' ? 'bg-slate-700 text-white' : 'text-slate-400'"
                  class="px-3 py-1 rounded-lg text-xs font-bold transition-colors">الكل</button>
          <button (click)="setFilter('ACTIVE')"
                  [ngClass]="statusFilter() === 'ACTIVE' ? 'bg-slate-700 text-emerald-400' : 'text-slate-400'"
                  class="px-3 py-1 rounded-lg text-xs font-bold transition-colors">النشطة</button>
          <button (click)="setFilter('INACTIVE')"
                  [ngClass]="statusFilter() === 'INACTIVE' ? 'bg-slate-700 text-yellow-400' : 'text-slate-400'"
                  class="px-3 py-1 rounded-lg text-xs font-bold transition-colors">غير النشطة</button>
        </div>
      </div>

      <!-- Table -->
      <div class="edu-card p-0 overflow-hidden border-slate-800 shadow-xl overflow-x-auto">
        <table class="w-full text-sm min-w-[900px]">
          <thead>
            <tr class="bg-slate-900/80 border-b border-slate-800">
              <th class="text-right px-4 py-3 text-slate-500 font-bold text-xs w-12">#</th>
              <th class="text-right px-4 py-3 text-slate-500 font-bold text-xs">اسم التصنيف</th>
              <th class="text-center px-4 py-3 text-slate-500 font-bold text-xs w-24">الحالة</th>
              <th class="text-center px-4 py-3 text-slate-500 font-bold text-xs w-24">الكورسات</th>
              <th class="text-right px-4 py-3 text-slate-500 font-bold text-xs w-44">تاريخ الانشاء</th>
              <th class="text-center px-4 py-3 text-slate-500 font-bold text-xs">الاجراءات</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let cat of displayedCategories(); trackBy: trackById"
                [ngClass]="cat.active ? '' : 'opacity-60'"
                class="border-b border-slate-800/50 hover:bg-slate-800/20 transition-colors">

              <!-- ID -->
              <td class="px-4 py-3">
                <span class="text-slate-600 font-mono text-xs">{{ cat.id }}</span>
              </td>

              <!-- اسم التصنيف -->
              <td class="px-4 py-3">
                <div class="flex items-center gap-2">
                  <div [ngClass]="cat.active ? 'bg-orange-900/30' : 'bg-slate-800'"
                       class="w-7 h-7 rounded-lg flex items-center justify-center">
                    <span [ngClass]="cat.active ? 'text-orange-400' : 'text-slate-600'"
                          class="material-icons-round text-xs">label</span>
                  </div>
                  <div>
                    <span class="text-slate-200 font-bold text-sm">{{ cat.name }}</span>
                    <p *ngIf="cat.description" class="text-slate-500 text-xs mt-0.5 truncate max-w-[200px]">{{ cat.description }}</p>
                  </div>
                </div>
              </td>

              <!-- الحالة -->
              <td class="px-4 py-3 text-center">
                <span [ngClass]="cat.active
                        ? 'bg-emerald-900/30 text-emerald-400 border-emerald-800/30'
                        : 'bg-slate-800 text-slate-500 border-slate-700'"
                      class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full border text-[10px] font-bold">
                  <span class="w-1.5 h-1.5 rounded-full"
                        [ngClass]="cat.active ? 'bg-emerald-500' : 'bg-slate-500'"></span>
                  {{ cat.active ? 'نشط' : 'معطل' }}
                </span>
              </td>

              <!-- عدد الكورسات -->
              <td class="px-4 py-3 text-center">
                <button [routerLink]="['/level', levelId(), 'categories', cat.id, 'courses']"
                        class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-indigo-900/20 border border-indigo-800/30 text-indigo-400 text-[10px] font-bold hover:bg-indigo-900/40 transition-colors">
                  <span class="material-icons-round text-[11px]">school</span>
                  {{ cat.coursesCount ?? 0 }}
                </button>
              </td>

              <!-- تاريخ الانشاء -->
              <td class="px-4 py-3">
                <span class="text-slate-500 text-xs">{{ formatDate(cat.createdAt) }}</span>
              </td>

              <!-- الاجراءات -->
              <td class="px-4 py-3">
                <div class="flex items-center justify-center gap-1 flex-wrap">

                  <!-- الكورسات -->
                  <button [routerLink]="['/level', levelId(), 'categories', cat.id, 'courses']"
                          class="flex items-center gap-1 px-2 py-1 rounded-lg bg-indigo-900/20 text-indigo-400 border border-indigo-800/30 text-[10px] font-bold hover:opacity-80 transition-colors">
                    <span class="material-icons-round text-[11px]">library_books</span>
                    الكورسات
                  </button>

                  <!-- نشر / الغاء النشر -->
                  <button (click)="toggleStatus(cat)"
                          [ngClass]="cat.active
                            ? 'bg-yellow-900/20 text-yellow-400 border-yellow-800/30'
                            : 'bg-emerald-900/20 text-emerald-400 border-emerald-800/30'"
                          class="flex items-center gap-1 px-2 py-1 rounded-lg border text-[10px] font-bold hover:opacity-80 transition-colors">
                    <span class="material-icons-round text-[11px]">{{ cat.active ? 'visibility_off' : 'visibility' }}</span>
                    {{ cat.active ? 'إلغاء النشر' : 'نشر' }}
                  </button>

                  <!-- تعديل -->
                  <button (click)="openEditModal(cat)"
                          class="flex items-center gap-1 px-2 py-1 rounded-lg bg-slate-700 text-slate-300 border border-slate-600 text-[10px] font-bold hover:bg-slate-600 transition-colors">
                    <span class="material-icons-round text-[11px]">edit</span>
                    تعديل
                  </button>

                  <!-- حذف -->
                  <button (click)="deleteCategory(cat)"
                          class="flex items-center gap-1 px-2 py-1 rounded-lg bg-red-900/20 text-red-400 border border-red-800/30 text-[10px] font-bold hover:opacity-80 transition-colors">
                    <span class="material-icons-round text-[11px]">delete</span>
                    حذف
                  </button>

                </div>
              </td>
            </tr>

            <tr *ngIf="displayedCategories().length === 0">
              <td colspan="6" class="text-center py-20">
                <div class="flex flex-col items-center gap-3 text-slate-600">
                  <span class="material-icons-round text-5xl opacity-20">label_off</span>
                  <p class="text-sm font-medium">لا توجد تصنيفات</p>
                  <button *ngIf="!searchQuery" [routerLink]="['/level', levelId(), 'add-category']" class="btn-primary mt-2">
                    اضافة اول تصنيف
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Edit Modal -->
      <div *ngIf="showEditModal()" class="modal-overlay">
        <div class="modal-box max-w-md">
          <div class="modal-header">
            <h3 class="text-white font-bold">تعديل التصنيف</h3>
            <button (click)="showEditModal.set(false)" class="btn-icon">
              <span class="material-icons-round">close</span>
            </button>
          </div>
          <div class="modal-body space-y-4">
            <div>
              <label class="edu-label">اسم التصنيف</label>
              <input type="text" [(ngModel)]="editForm.name" class="edu-input" placeholder="اسم التصنيف">
            </div>
            <div>
              <label class="edu-label">الوصف (اختياري)</label>
              <textarea [(ngModel)]="editForm.description" class="edu-input min-h-[80px]" placeholder="وصف التصنيف"></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button (click)="showEditModal.set(false)" class="btn-secondary">الغاء</button>
            <button (click)="saveEdit()" [disabled]="!editForm.name" class="btn-primary">حفظ</button>
          </div>
        </div>
      </div>

      <!-- Sort Modal -->
      <div *ngIf="showSortModal()" class="modal-overlay">
        <div class="modal-box max-w-lg">
          <div class="modal-header">
            <h3 class="text-white font-bold flex items-center gap-2">
              <span class="material-icons-round text-orange-400">drag_indicator</span>
              ترتيب التصنيفات
            </h3>
            <button (click)="showSortModal.set(false)" class="btn-icon">
              <span class="material-icons-round">close</span>
            </button>
          </div>
          <div class="modal-body space-y-2 max-h-[60vh] overflow-y-auto">
            <p class="text-slate-500 text-xs mb-3">استخدم الأسهم لتغيير ترتيب التصنيفات، ثم اضغط حفظ.</p>
            <div *ngFor="let cat of sortList(); let i = index"
                 class="flex items-center gap-3 p-3 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-colors">
              <div class="flex flex-col gap-0.5">
                <button (click)="moveUp(i)" [disabled]="i === 0"
                        class="w-6 h-5 flex items-center justify-center rounded text-slate-500 hover:text-white hover:bg-slate-700 disabled:opacity-20 transition-colors">
                  <span class="material-icons-round text-sm leading-none">expand_less</span>
                </button>
                <button (click)="moveDown(i)" [disabled]="i === sortList().length - 1"
                        class="w-6 h-5 flex items-center justify-center rounded text-slate-500 hover:text-white hover:bg-slate-700 disabled:opacity-20 transition-colors">
                  <span class="material-icons-round text-sm leading-none">expand_more</span>
                </button>
              </div>
              <span class="w-6 h-6 rounded-lg bg-orange-500/10 text-orange-400 text-xs font-bold flex items-center justify-center shrink-0">{{ i + 1 }}</span>
              <div class="flex-1 min-w-0">
                <p class="text-white font-semibold text-sm truncate">{{ cat.name }}</p>
                <p *ngIf="cat.description" class="text-slate-500 text-[10px] truncate">{{ cat.description }}</p>
              </div>
              <span class="text-xs px-2 py-0.5 rounded-full" [ngClass]="cat.active ? 'bg-emerald-900/30 text-emerald-400' : 'bg-slate-800 text-slate-500'">
                {{ cat.active ? 'نشط' : 'مخفي' }}
              </span>
            </div>
          </div>
          <div class="modal-footer">
            <button (click)="showSortModal.set(false)" class="btn-secondary">إلغاء</button>
            <button (click)="saveSortOrder()" [disabled]="savingSort()" class="btn-primary gap-2">
              <span *ngIf="savingSort()" class="material-icons-round animate-spin text-base">refresh</span>
              {{ savingSort() ? 'جاري الحفظ...' : 'حفظ الترتيب' }}
            </button>
          </div>
        </div>
      </div>

    </div>
  `
})
export class LevelCategoriesComponent implements OnInit, OnDestroy {
  private paramsSub?: Subscription;
  categories = signal<Category[]>([]);
  displayedCategories = signal<Category[]>([]);
  levelName = signal('');
  levelId = signal(0);
  loading = signal(false);
  statusFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  searchQuery = '';
  showEditModal = signal(false);
  editingCategory = signal<Category | null>(null);
  editForm = { name: '', description: '' };
  showSortModal = signal(false);
  sortList = signal<Category[]>([]);
  savingSort = signal(false);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: ApiService,
    public toastr: ToastrService
  ) {}


  ngOnInit() {
    this.paramsSub = this.route.params.subscribe(params => {
      const id = Number(params['levelId']);
      this.levelId.set(id);
      this.searchQuery = '';
      this.statusFilter.set('ALL');
      this.api.getLevels().subscribe({
        next: levels => { const l = levels.find(x => x.id === id); this.levelName.set(l?.name ?? ''); }
      });
      this.loadCategories(id);
    });
  }

  ngOnDestroy() {
    this.paramsSub?.unsubscribe();
  }

  loadCategories(levelId: number) {
    this.loading.set(true);
    // استخدام الـ endpoint العام (public) — الـ inactive categories تتظهر عبر الـ local state بعد التبديل
    this.api.getCategoriesByLevel(levelId).subscribe({
      next: page => { this.categories.set(page.content || []); this.applyFilters(); this.loading.set(false); },
      error: () => { this.categories.set([]); this.displayedCategories.set([]); this.loading.set(false); }
    });
  }

  applyFilters() {
    let items = this.categories();
    if (this.statusFilter() === 'ACTIVE')   items = items.filter(c => c.active);
    if (this.statusFilter() === 'INACTIVE') items = items.filter(c => !c.active);
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      items = items.filter(c => c.name?.toLowerCase().includes(q));
    }
    this.displayedCategories.set(items);
  }

  filterCategories() { this.applyFilters(); }
  setFilter(f: 'ALL' | 'ACTIVE' | 'INACTIVE') { this.statusFilter.set(f); this.applyFilters(); }
  activeCount()   { return this.categories().filter(c =>  c.active).length; }
  inactiveCount() { return this.categories().filter(c => !c.active).length; }

  toggleStatus(cat: Category) {
    this.api.toggleCategoryStatus(cat.id).subscribe({
      next: () => {
        const updated = this.categories().map(c =>
          c.id === cat.id ? { ...c, active: !c.active } : c
        );
        this.categories.set(updated);
        this.applyFilters();
        this.toastr.success(cat.active ? 'تم إلغاء النشر' : 'تم النشر');
      },
      error: () => this.toastr.error('حدث خطأ')
    });
  }

  openEditModal(cat: Category) {
    this.editingCategory.set(cat);
    this.editForm = { name: cat.name, description: cat.description || '' };
    this.showEditModal.set(true);
  }

  saveEdit() {
    const cat = this.editingCategory();
    if (!cat) return;
    this.api.updateCategory(cat.id, {
      name: this.editForm.name,
      description: this.editForm.description,
      levelId: this.levelId()
    }).subscribe({
      next: () => {
        this.toastr.success('تم التعديل');
        this.showEditModal.set(false);
        this.loadCategories(this.levelId());
      },
      error: () => this.toastr.error('حدث خطأ')
    });
  }

  honorStudents(cat: Category) {
    this.toastr.info('قريباً — صفحة تكريم الطلاب لـ ' + cat.name);
  }

  assignCourses(cat: Category) {
    this.router.navigate(['/level', this.levelId(), 'categories', cat.id, 'courses']);
  }

  formatDate(d?: string): string {
    if (!d) return '-';
    try {
      const date = new Date(d);
      const datePart = date.toLocaleDateString('ar-EG', { year: 'numeric', month: 'short', day: 'numeric' });
      const timePart = date.toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit', hour12: true });
      return datePart + ' — ' + timePart;
    } catch { return '-'; }
  }

  deleteCategory(cat: Category) {
    if (!confirm(`هل أنت متأكد من حذف التصنيف "${cat.name}"؟\nسيتم حذف جميع الكورسات المرتبطة به.`)) return;
    this.api.deleteCategory(cat.id).subscribe({
      next: () => {
        this.categories.set(this.categories().filter(c => c.id !== cat.id));

        this.applyFilters();
        this.toastr.success('تم حذف التصنيف بنجاح');
      },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ أثناء الحذف')
    });
  }

  openSortModal() {
    this.sortList.set([...this.categories()]);
    this.showSortModal.set(true);
  }

  moveUp(i: number) {
    const list = [...this.sortList()];
    if (i === 0) return;
    [list[i - 1], list[i]] = [list[i], list[i - 1]];
    this.sortList.set(list);
  }

  moveDown(i: number) {
    const list = [...this.sortList()];
    if (i >= list.length - 1) return;
    [list[i], list[i + 1]] = [list[i + 1], list[i]];
    this.sortList.set(list);
  }

  saveSortOrder() {
    this.savingSort.set(true);
    const orders = this.sortList().map((cat, idx) => ({ id: cat.id, sortOrder: idx }));
    this.api.reorderCategories(orders).subscribe({
      next: () => {
        this.categories.set(this.sortList());
        this.applyFilters();
        this.showSortModal.set(false);
        this.savingSort.set(false);
        this.toastr.success('تم حفظ الترتيب بنجاح');
      },
      error: () => { this.toastr.error('حدث خطأ'); this.savingSort.set(false); }
    });
  }

  trackById(_: number, cat: Category): number { return cat.id; }
}
