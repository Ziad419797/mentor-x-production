import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-add-category',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="max-w-3xl mx-auto animate-fade-in pb-10">

      <!-- Page Header -->
      <div class="edu-card p-5 mb-6 flex items-center gap-4">
        <div class="w-10 h-10 rounded-xl bg-emerald-500/10 flex items-center justify-center">
          <span class="material-icons-round text-emerald-400">add_circle</span>
        </div>
        <div>
          <h2 class="text-white font-bold text-lg">إضافة تصنيف جديد</h2>
          <p class="text-slate-500 text-xs mt-0.5">إنشاء تصنيف جديد للنظام</p>
        </div>
      </div>

      <!-- Form Card -->
      <div class="edu-card p-8 space-y-6">

        <!-- Row 1: Name + Status -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

          <!-- اسم التصنيف -->
          <div>
            <label class="flex items-center gap-2 text-slate-300 font-bold text-sm mb-2">
              <span class="material-icons-round text-orange-400 text-base">label</span>
              اسم التصنيف
            </label>
            <input
              type="text"
              [(ngModel)]="form.name"
              class="edu-input"
              placeholder="أدخل اسم التصنيف"
            >
          </div>

          <!-- حالة التصنيف -->
          <div>
            <label class="flex items-center gap-2 text-slate-300 font-bold text-sm mb-2">
              <span class="w-4 h-4 rounded-full flex items-center justify-center" [class.bg-emerald-500]="form.active" [class.bg-slate-600]="!form.active">
                <span class="w-2 h-2 rounded-full bg-white"></span>
              </span>
              حالة التصنيف
            </label>
            <div class="edu-input flex items-center justify-between cursor-pointer" (click)="form.active = !form.active">
              <span class="text-slate-300 text-sm">{{ form.active ? 'نشط' : 'غير نشط' }}</span>
              <div class="relative w-12 h-6">
                <div class="w-12 h-6 rounded-full transition-colors" [class.bg-emerald-500]="form.active" [class.bg-slate-600]="!form.active"></div>
                <div class="absolute top-0.5 w-5 h-5 rounded-full bg-white shadow transition-all" [class.right-0.5]="!form.active" [class.left-0.5]="form.active"
                     [style.transform]="form.active ? 'translateX(24px)' : 'translateX(0)'"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- الوصف -->
        <div>
          <label class="flex items-center gap-2 text-slate-300 font-bold text-sm mb-2">
            <span class="material-icons-round text-slate-400 text-base">subject</span>
            الوصف
          </label>
          <textarea
            [(ngModel)]="form.description"
            rows="4"
            class="edu-input resize-none"
            placeholder="أدخل وصف التصنيف"
          ></textarea>
        </div>

        <!-- السنة الدراسية (readonly) -->
        <div>
          <label class="flex items-center gap-2 text-slate-300 font-bold text-sm mb-2">
            <span class="material-icons-round text-indigo-400 text-base">school</span>
            السنة الدراسية
          </label>
          <div class="edu-input bg-slate-800/50 text-slate-400 cursor-default select-none">
            {{ levelName() || 'جاري التحميل...' }}
          </div>
        </div>

        <!-- Buttons -->
        <div class="flex items-center gap-3 pt-4 border-t border-slate-800">
          <button
            (click)="submit()"
            [disabled]="!form.name || saving()"
            class="btn-primary gap-2 px-8">
            <span class="material-icons-round text-base">save</span>
            {{ saving() ? 'جاري الحفظ...' : 'إنشاء التصنيف' }}
          </button>
          <button (click)="goBack()" class="btn-secondary gap-2 px-6">
            <span class="material-icons-round text-base">arrow_forward</span>
            العودة للقائمة
          </button>
        </div>

      </div>

    </div>
  `
})
export class AddCategoryComponent implements OnInit {
  levelId = signal(0);
  levelName = signal('');
  saving = signal(false);

  form = {
    name: '',
    description: '',
    active: true
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: ApiService,
    private toastr: ToastrService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('levelId'));
    this.levelId.set(id);
    this.api.getLevels().subscribe({
      next: levels => {
        const l = levels.find(x => x.id === id);
        this.levelName.set(l?.name ?? '');
      }
    });
  }

  submit() {
    if (!this.form.name || this.saving()) return;
    this.saving.set(true);
    this.api.createCategory({
      name: this.form.name,
      description: this.form.description || undefined,
      levelId: this.levelId()
    }).subscribe({
      next: () => {
        this.toastr.success('تم إنشاء التصنيف بنجاح');
        this.router.navigate(['/level', this.levelId(), 'categories']);
      },
      error: (err: any) => {
        this.toastr.error(err?.error?.message || 'حدث خطأ أثناء الإنشاء');
        this.saving.set(false);
      }
    });
  }

  goBack() {
    this.router.navigate(['/level', this.levelId(), 'categories']);
  }
}
