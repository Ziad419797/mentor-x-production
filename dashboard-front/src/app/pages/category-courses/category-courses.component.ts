import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Course, Category } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-category-courses',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './category-courses.component.html'
})
export class CategoryCoursesComponent implements OnInit {
  levelId     = signal(0);
  categoryId  = signal(0);
  levelName   = signal('');
  categoryName = signal('');

  courses          = signal<Course[]>([]);
  displayedCourses = signal<Course[]>([]);
  allLevelCategories = signal<Category[]>([]);
  otherCategories    = signal<Category[]>([]);

  loading  = signal(false);
  moving   = signal(false);
  searchQuery = '';
  statusFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');

  showMoveModal  = signal(false);
  movingCourse   = signal<Course | null>(null);
  selectedTargetCategoryId = 0;

  // Edit
  showEditModal  = signal(false);
  editingCourse  = signal<Course | null>(null);
  editImagePreview  = signal<string | null>(null);
  editSelectedFile: File | null = null;
  savingEdit = signal(false);

  editForm = {
    title:           '',
    description:     '',
    price:           null as number | null,
    discountedPrice: null as number | null,
    studentPoints:   null as number | null,
    accessDays:      null as number | null,
    accessExpiresAt: '',
    orderNumber:     null as number | null,
    contentOrder:    '',
    trackAttendance: false,
    featured:        false,
    pinned:          false,
  };

  editErrors = {
    title: false, description: false,
    price: false, contentOrder: false,
  };

  contentOrderOptions = [
    { value: 'NONE',            icon: 'shuffle',     label: 'لا',            description: 'يمكن للطلاب الوصول لأي محتوى بحرية بدون ترتيب' },
    { value: 'LOCK_BY_SESSION', icon: 'lock_clock',  label: 'قفل حصة بحصة', description: 'يجب إكمال الحصة الحالية قبل الانتقال للحصة التالية' },
    { value: 'LOCK_BY_ELEMENT', icon: 'lock',        label: 'قفل عنصر عنصر',description: 'يجب إكمال كل عنصر قبل الانتقال للعنصر التالي' },
  ];

  // Sort
  showSortModal = signal(false);
  sortList      = signal<Course[]>([]);
  savingSort    = signal(false);

  // Enrolled

  // معاينة صورة الكورس
  showImagePreview   = signal(false);
  previewImageUrl    = signal('');
  previewCourseTitle = signal('');

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: ApiService,
    private toastr: ToastrService
  ) {}

  ngOnInit() {
    const lvl = Number(this.route.snapshot.paramMap.get('levelId'));
    const cat = Number(this.route.snapshot.paramMap.get('categoryId'));
    this.levelId.set(lvl);
    this.categoryId.set(cat);

    this.api.getLevels().subscribe({
      next: levels => { const l = levels.find(x => x.id === lvl); this.levelName.set(l?.name ?? ''); }
    });

    this.api.getCategoriesByLevel(lvl).subscribe({
      next: page => {
        const all = page.content || [];
        this.allLevelCategories.set(all);
        const current = all.find(c => c.id === cat);
        this.categoryName.set(current?.name ?? '');
      },
      error: () => {}
    });

    this.loadCourses();
  }

  loadCourses() {
    this.loading.set(true);
    this.api.getCoursesByCategory(this.categoryId()).subscribe({
      next: page => {
        const list = page.content || [];
        this.courses.set(list);
        this.filterCourses();
        this.loading.set(false);
        this.loadEnrolledCounts(list);
      },
      error: () => { this.courses.set([]); this.displayedCourses.set([]); this.loading.set(false); }
    });
  }

  // عدد المشتركين الحقيقي لكل كورس — الباك ميرجعش enrolledStudentsCount دايماً
  // فبنجيبه بشكل مباشر من /api/enrollments/course/{id} ونحدّث الكروت
  loadEnrolledCounts(list: Course[]) {
    if (!list.length) return;
    const calls = list.map(c => this.api.getEnrollmentsByCourse(c.id).pipe(catchError(() => of([]))));
    forkJoin(calls).subscribe(results => {
      const updated = list.map((c, i) => ({ ...c, enrolledStudentsCount: (results[i] || []).length }));
      this.courses.set(updated);
      this.filterCourses();
    });
  }

  setFilter(f: 'ALL' | 'ACTIVE' | 'INACTIVE') {
    this.statusFilter.set(f);
    this.filterCourses();
  }

  filterCourses() {
    let items = this.courses();
    if (this.statusFilter() === 'ACTIVE')   items = items.filter(c => c.active !== false);
    if (this.statusFilter() === 'INACTIVE') items = items.filter(c => c.active === false);
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      items = items.filter(c => c.title?.toLowerCase().includes(q));
    }
    this.displayedCourses.set(items);
  }

  toggleCourseStatus(course: Course) {
    this.api.toggleCourseStatus(course.id).subscribe({
      next: () => {
        const updated = this.courses().map(c =>
          c.id === course.id ? { ...c, active: !c.active } : c
        );
        this.courses.set(updated);
        this.filterCourses();
        this.toastr.success(course.active !== false ? 'تم إيقاف الكورس' : 'تم نشر الكورس');
      },
      error: () => this.toastr.error('حدث خطأ')
    });
  }

  deleteCourse(course: Course) {
    if (!confirm('هل أنت متأكد من حذف كورس "' + course.title + '"؟')) return;
    this.api.deleteCourse(course.id).subscribe({
      next: () => {
        this.courses.set(this.courses().filter(c => c.id !== course.id));
        this.filterCourses();
        this.toastr.success('تم حذف الكورس');
      },
      error: (err: any) => this.toastr.error(err?.error?.message || 'حدث خطأ')
    });
  }

  openEditModal(course: Course) {
    this.editingCourse.set(course);
    this.editSelectedFile = null;
    this.editImagePreview.set(course.imageUrl ?? null);
    this.editErrors = { title: false, description: false, price: false, contentOrder: false };
    this.editForm = {
      title:           course.title ?? '',
      description:     (course as any).description ?? '',
      price:           course.price ?? null,
      discountedPrice: (course as any).discountedPrice ?? null,
      studentPoints:   (course as any).studentPoints ?? null,
      accessDays:      (course as any).accessDays ?? null,
      accessExpiresAt: (course as any).accessExpiresAt ? (course as any).accessExpiresAt.substring(0, 10) : '',
      orderNumber:     course.orderNumber ?? null,
      contentOrder:    (course as any).contentOrder ?? '',
      trackAttendance: (course as any).trackAttendance ?? false,
      featured:        (course as any).featured ?? false,
      pinned:          (course as any).pinned ?? false,
    };
    this.showEditModal.set(true);
  }

  onEditFileSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.editSelectedFile = file;
    const reader = new FileReader();
    reader.onload = e => this.editImagePreview.set(e.target?.result as string);
    reader.readAsDataURL(file);
  }

  saveEdit() {
    const f = this.editForm;
    this.editErrors.title        = !f.title.trim();
    this.editErrors.description  = !f.description.trim();
    this.editErrors.price        = f.price === null || f.price === undefined || (f.price as any) === '';
    this.editErrors.contentOrder = !f.contentOrder;
    if (Object.values(this.editErrors).some(e => e)) { this.toastr.error('يرجى تعبئة جميع الحقول المطلوبة'); return; }

    const course = this.editingCourse();
    if (!course) return;
    this.savingEdit.set(true);

    const fd = new FormData();
    fd.append('title',           f.title);
    fd.append('description',     f.description);
    fd.append('price',           String(f.price ?? 0));
    fd.append('contentOrder',    f.contentOrder);
    fd.append('trackAttendance', String(f.trackAttendance));
    fd.append('featured',        String(f.featured));
    fd.append('pinned',          String(f.pinned));
    if (f.discountedPrice != null) fd.append('discountedPrice', String(f.discountedPrice));
    if (f.studentPoints   != null) fd.append('studentPoints',   String(f.studentPoints));
    if (f.accessDays      != null) fd.append('accessDays',      String(f.accessDays));
    if (f.accessExpiresAt)         fd.append('accessExpiresAt', f.accessExpiresAt);
    if (f.orderNumber     != null) fd.append('orderNumber',     String(f.orderNumber));
    if (this.editSelectedFile)     fd.append('image', this.editSelectedFile);

    this.api.updateCourse(course.id, fd).subscribe({
      next: () => {
        this.toastr.success('تم تحديث الكورس');
        this.showEditModal.set(false);
        this.savingEdit.set(false);
        this.loadCourses();
      },
      error: (e: any) => {
        this.toastr.error(e?.error?.message || 'خطأ في التحديث');
        this.savingEdit.set(false);
      }
    });
  }

  openMoveModal(course: Course) {
    this.movingCourse.set(course);
    this.selectedTargetCategoryId = 0;
    this.otherCategories.set(
      this.allLevelCategories().filter(c => c.id !== this.categoryId() && c.active !== false)
    );
    this.showMoveModal.set(true);
  }

  moveCourse() {
    const course = this.movingCourse();
    if (!course || this.selectedTargetCategoryId <= 0 || this.moving()) return;
    this.moving.set(true);
    this.api.changeCourseCategory(course.id, this.selectedTargetCategoryId).subscribe({
      next: () => {
        this.toastr.success('تم نقل الكورس بنجاح');
        this.showMoveModal.set(false);
        this.moving.set(false);
        this.courses.set(this.courses().filter(c => c.id !== course.id));
        this.filterCourses();
      },
      error: (err: any) => {
        this.toastr.error(err?.error?.message || 'حدث خطأ أثناء النقل');
        this.moving.set(false);
      }
    });
  }

  openSortModal() {
    this.sortList.set([...this.courses()]);
    this.showSortModal.set(true);
  }

  moveSortUp(i: number) {
    const list = [...this.sortList()];
    if (i === 0) return;
    [list[i - 1], list[i]] = [list[i], list[i - 1]];
    this.sortList.set(list);
  }

  moveSortDown(i: number) {
    const list = [...this.sortList()];
    if (i >= list.length - 1) return;
    [list[i], list[i + 1]] = [list[i + 1], list[i]];
    this.sortList.set(list);
  }

  saveSortOrder() {
    this.savingSort.set(true);
    // حفظ الترتيب في الـ local state فقط (الـ backend مش عنده endpoint للـ reorder في الكورسات)
    this.courses.set(this.sortList());
    this.filterCourses();
    this.showSortModal.set(false);
    this.savingSort.set(false);
    this.toastr.success('تم حفظ الترتيب');
  }

  openImagePreview(course: Course) {
    if (!course.imageUrl) return;
    this.previewImageUrl.set(course.imageUrl);
    this.previewCourseTitle.set(course.title);
    this.showImagePreview.set(true);
  }

  closeImagePreview() {
    this.showImagePreview.set(false);
  }

  goToEnrolledStudents(course: Course) {
    this.router.navigate(['/courses', course.id, 'enrolled-students'], {
      queryParams: { title: course.title }
    });
  }

  trackById(_: number, item: { id: number }) { return item.id; }
}
