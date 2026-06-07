import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Course, Category } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { CategoryStateService } from '../../services/category-state.service';
import { Subscription } from 'rxjs';
import { extractList, extractPage } from '../../core/api-response.model';

@Component({
  selector: 'app-courses',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">
      
      <!-- Header -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 class="text-white font-bold text-2xl tracking-tight">إدارة المستويات الدراسية</h2>
          <div class="flex items-center gap-2 mt-1">
            <p class="text-slate-500 text-sm">تخصيص مستويات المنهج والمسارات التعليمية</p>
            <span *ngIf="activeCategoryName()" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-indigo-500/15 border border-indigo-500/30 text-indigo-400 text-[10px] font-bold">
              <span class="material-icons-round text-[10px]">filter_alt</span>
              {{ activeCategoryName() }}
            </span>
          </div>
        </div>
        <button (click)="openCourseModal()" class="btn-primary">
          <span class="material-icons-round">add</span>
          مستوى جديد
        </button>
      </div>

      <!-- Educational Levels Navigator -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
        
        <!-- Level Card -->
        <div *ngFor="let course of courses()" class="curriculum-card group">
           <div class="curriculum-card-inner">
              
              <!-- Left: Curriculum Info -->
              <div class="p-6 flex-1 space-y-4">
                 <div class="flex items-center gap-3">
                    <div class="w-12 h-12 rounded-2xl bg-indigo-500/10 flex items-center justify-center text-indigo-400 group-hover:bg-indigo-500 group-hover:text-white transition-all duration-500 shadow-inner">
                       <span class="material-icons-round">account_tree</span>
                    </div>
                    <div>
                       <h3 class="text-white font-bold text-lg tracking-tight">{{ course.title }}</h3>
                       <p class="text-slate-500 text-xs mt-0.5">مستوى تعليمي نشط ضمن المنهج</p>
                    </div>
                 </div>

                 <p class="text-slate-400 text-sm leading-relaxed line-clamp-2">{{ course.description || 'لا يوجد وصف للمنهج في هذا المستوى' }}</p>

                 <div class="flex items-center gap-6 pt-2">
                    <div class="flex flex-col">
                       <span class="text-white font-black text-lg">{{ course.sessionsCount || 0 }}</span>
                       <span class="text-[10px] text-slate-500 uppercase tracking-widest">وحدة دراسية</span>
                    </div>
                    <div class="w-px h-8 bg-slate-800"></div>
                    <div class="flex flex-col">
                       <span class="text-white font-black text-lg">{{ course.enrolledStudentsCount || 0 }}</span>
                       <span class="text-[10px] text-slate-500 uppercase tracking-widest">طالب ملتحق</span>
                    </div>
                    <div class="w-px h-8 bg-slate-800"></div>
                    <div class="flex flex-col">
                       <span class="text-emerald-400 font-black text-lg">{{ course.price }} <small class="text-[8px]">ج.م</small></span>
                       <span class="text-[10px] text-slate-500 uppercase tracking-widest">قيمة الاشتراك</span>
                    </div>
                 </div>

                 <!-- Action Bar -->
                 <div class="flex items-center gap-3 pt-4 border-t border-slate-800/50">
                    <button (click)="openCourseModal(course)" class="flex-1 h-10 rounded-xl bg-slate-800/50 text-slate-300 text-xs font-bold hover:bg-slate-700 hover:text-white transition-all flex items-center justify-center gap-2 border border-slate-700/50">
                       <span class="material-icons-round text-sm">tune</span>
                       إدارة المحتوى
                    </button>
                    <button (click)="toggleStatus(course)" class="h-10 px-4 rounded-xl border border-slate-700/50 text-slate-400 hover:text-indigo-400 transition-all">
                       <span class="material-icons-round text-sm">{{ course.active ? 'pause' : 'play_arrow' }}</span>
                    </button>
                    <button (click)="deleteCourse(course.id)" class="h-10 px-4 rounded-xl border border-slate-700/50 text-slate-400 hover:text-red-400 transition-all">
                       <span class="material-icons-round text-sm">delete_outline</span>
                    </button>
                 </div>
              </div>

              <!-- Right: Visual Identity -->
              <div class="hidden sm:block w-48 relative overflow-hidden">
                 <img *ngIf="course.imageUrl" [src]="course.imageUrl" class="w-full h-full object-cover opacity-40 group-hover:opacity-100 group-hover:scale-110 transition-all duration-700">
                 <div *ngIf="!course.imageUrl" class="w-full h-full bg-slate-900/50 flex items-center justify-center text-slate-800">
                    <span class="material-icons-round text-6xl">school</span>
                 </div>
                 <div class="absolute inset-0 bg-gradient-to-r from-slate-950 via-transparent to-transparent"></div>
                 
                 <!-- Order Badge -->
                 <div class="absolute top-4 left-4 w-8 h-8 rounded-full bg-slate-950/80 border border-slate-800 flex items-center justify-center text-xs font-black text-indigo-400">
                    {{ course.sessionsCount ?? '' }}
                 </div>
              </div>

           </div>
        </div>

        <!-- Create Level Placeholder -->
        <div (click)="openCourseModal()" class="curriculum-card-placeholder group">
           <div class="w-16 h-16 rounded-3xl bg-slate-900 border-2 border-dashed border-slate-800 flex items-center justify-center text-slate-700 group-hover:border-indigo-500 group-hover:text-indigo-400 transition-all duration-500">
              <span class="material-icons-round text-3xl">add</span>
           </div>
           <div class="text-center mt-4">
              <h4 class="text-slate-400 font-bold group-hover:text-white transition-colors">إضافة مستوى تعليمي جديد</h4>
              <p class="text-slate-600 text-xs mt-1">توسيع المنهج الدراسي بمسار جديد</p>
           </div>
        </div>

      </div>

      <!-- Pagination -->
      <div *ngIf="totalPages() > 1" class="flex justify-center gap-2 pt-6">
         <button *ngFor="let p of [].constructor(totalPages()); let i = index" 
                 (click)="loadCourses(i)"
                 [class.bg-indigo-600]="currentPage() === i"
                 class="w-10 h-10 rounded-lg bg-slate-800 border border-slate-700 text-white font-bold hover:bg-indigo-600 transition-colors">
            {{ i + 1 }}
         </button>
      </div>

      <!-- Course Modal -->
      <div *ngIf="showModal()" class="modal-overlay">
        <div class="modal-box max-w-2xl">
          <div class="modal-header">
            <h3 class="text-white font-bold">{{ editingCourse() ? 'تعديل الكورس' : 'إنشاء كورس جديد' }}</h3>
            <button (click)="showModal.set(false)" class="btn-icon"><span class="material-icons-round">close</span></button>
          </div>
          <div class="modal-body max-h-[75vh] overflow-y-auto custom-scrollbar pr-2">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

              <!-- Left: Details -->
              <div class="space-y-4">
                <div>
                  <label class="edu-label">عنوان الكورس <span class="text-red-400">*</span></label>
                  <input type="text" [(ngModel)]="courseForm.title"
                         [class.border-red-500]="formSubmitted() && !courseForm.title?.trim()"
                         class="edu-input" placeholder="مثال: رياضيات أولى ثانوي">
                  <p *ngIf="formSubmitted() && !courseForm.title?.trim()" class="text-red-400 text-[10px] mt-1">عنوان الكورس مطلوب</p>
                </div>

                <div>
                  <label class="edu-label">وصف الكورس <span class="text-red-400">*</span></label>
                  <textarea [(ngModel)]="courseForm.description"
                            [class.border-red-500]="formSubmitted() && !courseForm.description?.trim()"
                            class="edu-input min-h-[90px]" placeholder="اكتب وصفاً مختصراً للكورس"></textarea>
                  <p *ngIf="formSubmitted() && !courseForm.description?.trim()" class="text-red-400 text-[10px] mt-1">وصف الكورس مطلوب</p>
                </div>

                <div>
                  <label class="edu-label">سعر الكورس (ج.م) <span class="text-red-400">*</span></label>
                  <input type="number" [(ngModel)]="courseForm.price"
                         [class.border-red-500]="formSubmitted() && courseForm.price == null"
                         class="edu-input" placeholder="0" min="0">
                  <p *ngIf="formSubmitted() && courseForm.price == null" class="text-red-400 text-[10px] mt-1">سعر الكورس مطلوب</p>
                </div>

                <div>
                  <label class="edu-label">نوع التدريس <span class="text-red-400">*</span></label>
                  <div class="flex gap-2"
                       [class.ring-1]="formSubmitted() && !courseForm.teachingType"
                       [class.ring-red-500]="formSubmitted() && !courseForm.teachingType"
                       [class.rounded-xl]="formSubmitted() && !courseForm.teachingType">
                    <button *ngFor="let t of teachingTypes"
                            (click)="courseForm.teachingType = t.value"
                            [class.bg-indigo-600]="courseForm.teachingType === t.value"
                            [class.border-indigo-500]="courseForm.teachingType === t.value"
                            [class.text-white]="courseForm.teachingType === t.value"
                            class="flex-1 py-2 rounded-xl border border-slate-700 text-xs font-bold text-slate-400 transition-all">
                      {{ t.label }}
                    </button>
                  </div>
                  <p *ngIf="formSubmitted() && !courseForm.teachingType" class="text-red-400 text-[10px] mt-1">نوع التدريس مطلوب</p>
                </div>

                <div>
                  <label class="edu-label">ترتيب المحتوى <span class="text-red-400">*</span></label>
                  <div class="space-y-2"
                       [class.ring-1]="formSubmitted() && !courseForm.contentOrder"
                       [class.ring-red-500]="formSubmitted() && !courseForm.contentOrder"
                       [class.rounded-xl]="formSubmitted() && !courseForm.contentOrder">
                    <button *ngFor="let o of contentOrderOptions"
                            (click)="courseForm.contentOrder = o.value"
                            [class.bg-indigo-600]="courseForm.contentOrder === o.value"
                            [class.border-indigo-500]="courseForm.contentOrder === o.value"
                            [class.text-white]="courseForm.contentOrder === o.value"
                            class="w-full px-3 py-2 rounded-xl border border-slate-700 text-xs font-bold text-slate-400 text-right transition-all">
                      {{ o.label }}
                    </button>
                  </div>
                  <p *ngIf="formSubmitted() && !courseForm.contentOrder" class="text-red-400 text-[10px] mt-1">ترتيب المحتوى مطلوب</p>
                </div>

                <div>
                  <label class="edu-label">ربط بفئة (اختياري)</label>
                  <div class="flex flex-wrap gap-2 max-h-28 overflow-y-auto p-2 bg-slate-900/50 rounded-lg border border-slate-700">
                    <button *ngFor="let cat of categories()"
                            (click)="toggleCategory(cat.id)"
                            [class.bg-indigo-600]="isCategorySelected(cat.id)"
                            [class.border-indigo-500]="isCategorySelected(cat.id)"
                            class="px-3 py-1.5 rounded-lg border border-slate-700 text-[10px] text-slate-300 transition-all">
                       {{ cat.name }}
                    </button>
                    <span *ngIf="categories().length === 0" class="text-[10px] text-slate-600 italic p-1">لا توجد فئات — أضفها أولاً</span>
                  </div>
                </div>
              </div>

              <!-- Right: Image Upload -->
              <div class="space-y-4">
                <div>
                  <label class="edu-label">صورة الغلاف *</label>
                  <div (click)="fileInput.click()"
                       [class.border-red-500]="!imagePreview() && formSubmitted()"
                       class="h-[220px] border-2 border-dashed border-slate-700 rounded-2xl flex flex-col items-center justify-center cursor-pointer hover:border-indigo-500 hover:bg-indigo-500/5 transition-all group relative overflow-hidden">
                    <input #fileInput type="file" (change)="onFileSelected($event)" accept="image/*" class="hidden">
                    <img *ngIf="imagePreview()" [src]="imagePreview()" class="w-full h-full object-cover">
                    <div *ngIf="!imagePreview()" class="flex flex-col items-center text-slate-500 group-hover:text-indigo-400">
                      <span class="material-icons-round text-5xl mb-2">cloud_upload</span>
                      <span class="text-sm font-bold">اسحب الصورة أو اضغط هنا</span>
                      <span class="text-[10px] mt-1">PNG, JPG حتى 5MB</span>
                    </div>
                    <div *ngIf="imagePreview()" class="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                      <span class="text-white text-xs font-bold">تغيير الصورة</span>
                    </div>
                  </div>
                  <p *ngIf="!imagePreview() && formSubmitted()" class="text-red-400 text-[10px] mt-1">صورة الغلاف مطلوبة</p>
                </div>

                <div>
                  <label class="edu-label">رقم الترتيب</label>
                  <input type="number" [(ngModel)]="courseForm.orderNumber" class="edu-input" placeholder="1">
                </div>
              </div>

            </div>
          </div>
          <div class="modal-footer">
            <button (click)="showModal.set(false)" class="btn-secondary">إلغاء</button>
            <button (click)="saveCourse()" class="btn-primary px-10">حفظ الكورس</button>
          </div>
        </div>
      </div>

    </div>
  `
})
export class CoursesComponent implements OnInit, OnDestroy {
  courses = signal<Course[]>([]);
  categories = signal<Category[]>([]);
  totalPages = signal(0);
  currentPage = signal(0);
  activeCategoryName = signal<string | null>(null);

  showModal = signal(false);
  editingCourse = signal<Course | null>(null);
  imagePreview = signal<string | null>(null);
  formSubmitted = signal(false);
  selectedFile: File | null = null;

  teachingTypes = [
    { value: 'ONLINE', label: 'أونلاين' },
    { value: 'CENTER', label: 'سنتر' },
    { value: 'BOTH',   label: 'أونلاين + سنتر' }
  ];

  contentOrderOptions = [
    { value: 'NONE',             label: 'لا — وصول حر بدون ترتيب' },
    { value: 'LOCK_BY_SESSION',  label: 'قفل حصة بحصة' },
    { value: 'LOCK_BY_ELEMENT',  label: 'قفل عنصر عنصر' }
  ];

  courseForm: any = {
    title: '',
    description: '',
    price: 0,
    teachingType: '',
    contentOrder: '',
    orderNumber: 1,
    categoryIds: [],
    status: 'ACTIVE'
  };

  private categorySub?: Subscription;

  constructor(
    private api: ApiService,
    private toastr: ToastrService,
    public categoryState: CategoryStateService
  ) {}

  ngOnInit(): void {
    this.api.getCategories().subscribe({
      next: (res) => { this.categories.set(extractList<Category>(res)); }
    });
    this.categorySub = this.categoryState.selectedCategory$.subscribe(cat => {
      this.activeCategoryName.set(cat?.name ?? null);
      this.loadCourses(0, cat?.id ?? null);
    });
  }

  ngOnDestroy(): void {
    this.categorySub?.unsubscribe();
  }

  loadCourses(page = 0, categoryId?: number | null) {
    this.api.getCourses(page, categoryId ?? undefined).subscribe({
      next: (res: any) => {
        const p = extractPage<Course>(res);
        this.courses.set(p.content);
        this.totalPages.set(p.totalPages);
        this.currentPage.set(page);
      },
      error: () => this.toastr.error('خطأ في تحميل الكورسات')
    });
  }

  openCourseModal(course?: Course) {
    this.editingCourse.set(course ?? null);
    this.formSubmitted.set(false);
    this.selectedFile = null;
    this.imagePreview.set(course?.imageUrl ?? null);
    if (course) {
      this.courseForm = {
        title: course.title,
        description: course.description ?? '',
        price: course.price ?? 0,
        teachingType: course.teachingType ?? '',
        contentOrder: course.contentOrder ?? '',
        orderNumber: course.orderNumber ?? 1,
        categoryIds: [],
        status: course.active !== false ? 'ACTIVE' : 'INACTIVE'
      };
    } else {
      this.courseForm = { title: '', description: '', price: 0, teachingType: '', contentOrder: '', orderNumber: 1, categoryIds: [], status: 'ACTIVE' };
    }
    this.showModal.set(true);
  }

  onFileSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.selectedFile = file;
    const reader = new FileReader();
    reader.onload = e => this.imagePreview.set(e.target?.result as string);
    reader.readAsDataURL(file);
  }

  toggleCategory(id: number) {
    const ids: number[] = this.courseForm.categoryIds;
    const idx = ids.indexOf(id);
    if (idx >= 0) ids.splice(idx, 1); else ids.push(id);
  }

  isCategorySelected(id: number): boolean {
    return this.courseForm.categoryIds.includes(id);
  }

  saveCourse() {
    this.formSubmitted.set(true);
    const f = this.courseForm;
    if (!f.title?.trim() || !f.description?.trim() || f.price == null || !f.teachingType || !f.contentOrder) return;

    const editing = this.editingCourse();

    if (editing) {
      const formData = new FormData();
      formData.append('title', f.title);
      formData.append('description', f.description);
      formData.append('price', String(f.price));
      formData.append('teachingType', f.teachingType);
      formData.append('contentOrder', f.contentOrder);
      formData.append('orderNumber', String(f.orderNumber ?? 1));
      if (this.selectedFile) formData.append('image', this.selectedFile);
      this.api.updateCourse(editing.id, formData).subscribe({
        next: () => { this.toastr.success('تم تحديث الكورس'); this.showModal.set(false); this.loadCourses(this.currentPage()); },
        error: (e: any) => this.toastr.error(e?.error?.message || 'خطأ في التحديث')
      });
    } else {
      if (!this.selectedFile) { this.toastr.warning('يرجى رفع صورة الغلاف'); return; }
      const formData = new FormData();
      formData.append('title', f.title);
      formData.append('description', f.description);
      formData.append('price', String(f.price));
      formData.append('teachingType', f.teachingType);
      formData.append('contentOrder', f.contentOrder);
      formData.append('orderNumber', String(f.orderNumber ?? 1));
      formData.append('image', this.selectedFile);
      if (f.categoryIds?.length) f.categoryIds.forEach((id: number) => formData.append('categoryIds', String(id)));
      this.api.createCourse(formData).subscribe({
        next: () => { this.toastr.success('تم إنشاء الكورس'); this.showModal.set(false); this.loadCourses(); },
        error: (e: any) => this.toastr.error(e?.error?.message || 'خطأ في الإنشاء')
      });
    }
  }

  toggleStatus(course: Course) {
    this.api.toggleCourseStatus(course.id).subscribe({
      next: () => {
        this.courses.set(this.courses().map(c => c.id === course.id ? { ...c, active: !c.active } : c));
        this.toastr.success(course.active ? 'تم إيقاف الكورس' : 'تم تفعيل الكورس');
      },
      error: () => this.toastr.error('خطأ في تغيير الحالة')
    });
  }

  deleteCourse(id: number) {
    if (!confirm('هل أنت متأكد من حذف هذا الكورس؟')) return;
    this.api.deleteCourse(id).subscribe({
      next: () => { this.toastr.success('تم الحذف'); this.loadCourses(this.currentPage()); },
      error: (e: any) => this.toastr.error(e?.error?.message || 'خطأ في الحذف')
    });
  }
}
