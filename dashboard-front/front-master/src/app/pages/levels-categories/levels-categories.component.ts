import { Component, OnInit, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Level, Category } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { extractList } from '../../core/api-response.model';

@Component({
  selector: 'app-levels-categories',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">
      
      <!-- Tabs -->
      <div class="flex items-center gap-2 p-1 bg-slate-900/50 rounded-xl border border-slate-700/50 w-fit">
        <button (click)="activeTab.set('levels')" 
                [class.active]="activeTab() === 'levels'"
                class="edu-tab px-6">المراحل الدراسية</button>
        <button (click)="activeTab.set('categories')" 
                [class.active]="activeTab() === 'categories'"
                class="edu-tab px-6">الفئات والتصنيفات</button>
      </div>

      <!-- Levels Tab -->
      <div *ngIf="activeTab() === 'levels'" class="space-y-4 animate-slide-up">
        <div class="flex items-center justify-between">
          <h2 class="text-white font-bold text-lg">إدارة المراحل الدراسية</h2>
          <button (click)="openLevelModal()" class="btn-primary">
            <span class="material-icons-round">add</span>
            مرحلة جديدة
          </button>
        </div>

        <div class="edu-card p-0 overflow-hidden shadow-xl border-slate-800">
          <table class="edu-table">
            <thead>
              <tr>
                <th>اسم المرحلة</th>
                <th class="w-24 text-center">الإجراءات</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let level of levels(); trackBy: trackByLevel">
                <td>{{ level.name }}</td>
                <td>
                  <div class="flex items-center justify-center gap-2">
                    <button (click)="openLevelModal(level)" class="btn-icon hover:text-indigo-400">
                      <span class="material-icons-round text-sm">edit</span>
                    </button>
                    <button (click)="deleteLevel(level.id)" class="btn-icon hover:text-red-400">
                      <span class="material-icons-round text-sm">delete</span>
                    </button>
                  </div>
                </td>
              </tr>
              <tr *ngIf="levels().length === 0">
                <td colspan="2" class="text-center py-10 text-slate-500 italic">لا توجد مراحل مضافة بعد</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Categories Tab -->
      <div *ngIf="activeTab() === 'categories'" class="space-y-4 animate-slide-up">
        <div class="flex items-center justify-between">
          <h2 class="text-white font-bold text-lg">إدارة الفئات</h2>
          <button (click)="openCategoryModal()" class="btn-primary">
            <span class="material-icons-round">add</span>
            فئة جديدة
          </button>
        </div>

        <div class="edu-card p-0 overflow-hidden shadow-xl border-slate-800">
          <table class="edu-table">
            <thead>
              <tr>
                <th>اسم الفئة</th>
                <th>المرحلة المرتبطة</th>
                <th class="text-center">الحالة</th>
                <th class="w-24 text-center">الإجراءات</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let cat of categories(); trackBy: trackByCategory">
                <td>{{ $any(cat).displayName || cat.name }}</td>
                <td>{{ cat.levelName || 'غير محدد' }}</td>
                <td>
                  <div class="flex justify-center">
                    <button (click)="toggleCategory(cat)" 
                            [class.text-emerald-400]="cat.status === 'ACTIVE'"
                            [class.text-slate-500]="cat.status !== 'ACTIVE'"
                            class="flex items-center gap-2 text-xs font-medium hover:opacity-80 transition-opacity">
                      <span class="w-2 h-2 rounded-full" [class.bg-emerald-500]="cat.status === 'ACTIVE'" [class.bg-slate-600]="cat.status !== 'ACTIVE'"></span>
                      {{ cat.status === 'ACTIVE' ? 'نشط' : 'معطل' }}
                    </button>
                  </div>
                </td>
                <td>
                  <div class="flex items-center justify-center gap-2">
                    <button (click)="openCategoryModal(cat)" class="btn-icon hover:text-indigo-400">
                      <span class="material-icons-round text-sm">edit</span>
                    </button>
                    <button (click)="deleteCategory(cat.id)" class="btn-icon hover:text-red-400">
                      <span class="material-icons-round text-sm">delete</span>
                    </button>
                  </div>
                </td>
              </tr>
              <tr *ngIf="categories().length === 0">
                <td colspan="4" class="text-center py-10 text-slate-500 italic">لا توجد فئات مضافة بعد</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Level Modal -->
      <div *ngIf="showLevelModal()" class="modal-overlay">
        <div class="modal-box max-w-md">
          <div class="modal-header">
            <h3 class="text-white font-bold">{{ editingLevel() ? 'تعديل مرحلة' : 'إضافة مرحلة جديدة' }}</h3>
            <button (click)="showLevelModal.set(false)" class="btn-icon"><span class="material-icons-round">close</span></button>
          </div>
          <div class="modal-body">
            <div>
              <label class="edu-label">اسم المرحلة الدراسية</label>
              <input type="text" [(ngModel)]="levelForm.name" class="edu-input" placeholder="مثال: الثانوية العامة">
            </div>
          </div>
          <div class="modal-footer">
            <button (click)="showLevelModal.set(false)" class="btn-secondary">إلغاء</button>
            <button (click)="saveLevel()" [disabled]="!levelForm.name" class="btn-primary">حفظ</button>
          </div>
        </div>
      </div>

      <!-- Category Modal -->
      <div *ngIf="showCategoryModal()" class="modal-overlay">
        <div class="modal-box max-w-md">
          <div class="modal-header">
            <h3 class="text-white font-bold">{{ editingCategory() ? 'تعديل فئة' : 'إضافة فئة جديدة' }}</h3>
            <button (click)="showCategoryModal.set(false)" class="btn-icon"><span class="material-icons-round">close</span></button>
          </div>
          <div class="modal-body">
            <div class="space-y-4">
              <div>
                <label class="edu-label">اسم الفئة</label>
                <input type="text" [(ngModel)]="categoryForm.name" class="edu-input" placeholder="مثال: مراجعات نهائية">
              </div>
              <div>
                <label class="edu-label">المرحلة المرتبطة</label>
                <select [(ngModel)]="categoryForm.levelId" class="edu-select">
                  <option [value]="0" disabled>اختر المرحلة</option>
                  <option *ngFor="let l of levels()" [value]="l.id">{{ l.name }}</option>
                </select>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button (click)="showCategoryModal.set(false)" class="btn-secondary">إلغاء</button>
            <button (click)="saveCategory()" [disabled]="!categoryForm.name || !categoryForm.levelId" class="btn-primary">حفظ</button>
          </div>
        </div>
      </div>

    </div>
  `
})
export class LevelsCategoriesComponent implements OnInit, OnDestroy {
  activeTab = signal<'levels' | 'categories'>('levels');
  levels = signal<Level[]>([]);
  categories = signal<Category[]>([]);

  showLevelModal = signal(false);
  editingLevel = signal<Level | null>(null);
  levelForm = { name: '' };

  showCategoryModal = signal(false);
  editingCategory = signal<Category | null>(null);
  categoryForm = { name: '', levelId: 0 };

  constructor(private api: ApiService, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.loadLevels();
    this.loadCategories();
  }

  ngOnDestroy(): void {}

  loadLevels() {
    this.api.getLevels().subscribe({
      next: (l: Level[]) => this.levels.set(l || []),
      error: (err) => {
        console.error('Failed to load levels:', err);
        this.levels.set([]);
      }
    });
  }

  loadCategories() {
    this.api.getCategories().subscribe({
      next: (res: any) => {
        const items = extractList<Category>(res);
        const formatted = items.map((cat: any) => ({
          ...cat,
          levelName: cat.levelName || 'غير محدد',
          displayName: cat.name
        }));
        this.categories.set(formatted);
      },
      error: (err) => {
        console.error('Failed to load categories:', err);
        this.categories.set([]);
      }
    });
  }

  openLevelModal(level?: Level) {
    if (level) {
      this.editingLevel.set(level);
      this.levelForm = { name: level.name };
    } else {
      this.editingLevel.set(null);
      this.levelForm = { name: '' };
    }
    this.showLevelModal.set(true);
  }

  saveLevel() {
    const obs = this.editingLevel() 
      ? this.api.updateLevel(this.editingLevel()!.id, this.levelForm.name)
      : this.api.createLevel(this.levelForm.name);

    obs.subscribe({
      next: () => {
        this.toastr.success('تم الحفظ بنجاح');
        this.showLevelModal.set(false);
        this.loadLevels();
      },
      error: () => this.toastr.error('حدث خطأ أثناء الحفظ')
    });
  }

  deleteLevel(id: number) {
    if (!confirm('هل أنت متأكد من حذف هذه المرحلة؟ سيتم فك ارتباط الفئات بها.')) return;
    this.api.deleteLevel(id).subscribe({
      next: () => {
        this.toastr.success('تم الحذف بنجاح');
        this.loadLevels();
      }
    });
  }

  openCategoryModal(cat?: Category) {
    if (cat) {
      this.editingCategory.set(cat);
      this.categoryForm = { name: cat.name, levelId: cat.levelId ?? 0 };
    } else {
      this.editingCategory.set(null);
      this.categoryForm = { name: '', levelId: 0 };
    }
    this.showCategoryModal.set(true);
  }

  saveCategory() {
    const obs = this.editingCategory()
      ? this.api.updateCategory(this.editingCategory()!.id, this.categoryForm)
      : this.api.createCategory(this.categoryForm);

    obs.subscribe({
      next: () => {
        this.toastr.success('تم الحفظ بنجاح');
        this.showCategoryModal.set(false);
        this.loadCategories();
      },
      error: () => this.toastr.error('حدث خطأ أثناء الحفظ')
    });
  }

  toggleCategory(cat: Category) {
    this.api.toggleCategoryStatus(cat.id).subscribe({
      next: () => {
        this.toastr.success('تم تغيير الحالة');
        this.loadCategories();
      }
    });
  }

  deleteCategory(id: number) {
    if (!confirm('هل أنت متأكد من حذف هذه الفئة؟')) return;
    this.api.deleteCategory(id).subscribe({
      next: () => {
        this.toastr.success('تم الحذف بنجاح');
        this.loadCategories();
      },
      error: (err) => {
        console.error('Failed to delete category:', err);
        this.toastr.error('حدث خطأ أثناء الحذف');
      }
    });
  }

  trackByLevel(index: number, level: Level): number { return level.id; }
  trackByCategory(index: number, cat: Category): number { return cat.id; }
}
