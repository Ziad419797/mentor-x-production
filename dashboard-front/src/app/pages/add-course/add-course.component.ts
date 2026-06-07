import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-add-course',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './add-course.component.html'
})
export class AddCourseComponent implements OnInit {

  levelName    = signal('');
  levelId      = signal(0);
  categories   = signal<{ id: number; name: string }[]>([]);
  submitting   = signal(false);
  imagePreview = signal<string | null>(null);
  selectedFile: File | null = null;

  form = {
    title:            '',
    description:      '',
    categoryId:       0,
    price:            null as number | null,
    discountedPrice:  null as number | null,
    studentPoints:    null as number | null,
    accessDays:       null as number | null,
    accessExpiresAt:  '',
    orderNumber:      null as number | null,
    contentOrder:     '' as string,
    trackAttendance:  false,
    featured:         false,
    pinned:           false,
  };

  errors = {
    title:        false,
    description:  false,
    categoryId:   false,
    price:        false,
    contentOrder: false,
  };

  contentOrderOptions = [
    {
      value: 'NONE',
      label: 'لا',
      icon: 'shuffle',
      description: 'يمكن للطلاب الوصول لأي محتوى بحرية بدون ترتيب'
    },
    {
      value: 'LOCK_BY_SESSION',
      label: 'قفل حصة بحصة',
      icon: 'lock_clock',
      description: 'يجب إكمال الحصة الحالية قبل الانتقال للحصة التالية'
    },
    {
      value: 'LOCK_BY_ELEMENT',
      label: 'قفل عنصر عنصر',
      icon: 'lock',
      description: 'يجب إكمال كل عنصر قبل الانتقال للعنصر التالي'
    },
  ];

  constructor(
    private api: ApiService,
    private router: Router,
    private route: ActivatedRoute,
    private toastr: ToastrService
  ) {}

  ngOnInit() {
    const levelId = Number(this.route.snapshot.paramMap.get('levelId'));
    this.levelId.set(levelId);
    if (levelId) {
      this.api.getLevels().subscribe({
        next: levels => {
          const l = levels.find(x => x.id === levelId);
          this.levelName.set(l?.name ?? '');
        }
      });
      this.api.getCategoriesByLevel(levelId).subscribe({
        next: page => this.categories.set(page.content || [])
      });
    }
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file  = input.files?.[0];
    if (!file) return;
    this.selectedFile = file;
    const reader = new FileReader();
    reader.onload = e => this.imagePreview.set(e.target?.result as string);
    reader.readAsDataURL(file);
  }

  goBack() {
    this.router.navigate(['/level', this.levelId(), 'categories']);
  }

  submit() {
    this.errors.title        = !this.form.title.trim();
    this.errors.description  = !this.form.description.trim();
    this.errors.categoryId   = !this.form.categoryId;
    this.errors.price        = this.form.price === null || this.form.price === undefined || (this.form.price as any) === '';
    this.errors.contentOrder = !this.form.contentOrder;

    const hasError = Object.values(this.errors).some(e => e);
    if (hasError) {
      this.toastr.error('يرجى تعبئة جميع الحقول المطلوبة');
      return;
    }

    const fd = new FormData();
    fd.append('title',           this.form.title);
    fd.append('description',     this.form.description);
    fd.append('categoryIds',     String(this.form.categoryId));
    fd.append('categoryId',      String(this.form.categoryId));
    fd.append('price',           String(this.form.price ?? 0));
    if (this.form.discountedPrice != null) fd.append('discountedPrice', String(this.form.discountedPrice));
    if (this.form.studentPoints   != null) fd.append('studentPoints',   String(this.form.studentPoints));
    if (this.form.accessDays      != null) fd.append('accessDays',      String(this.form.accessDays));
    if (this.form.accessExpiresAt)         fd.append('accessExpiresAt', this.form.accessExpiresAt);
    if (this.form.orderNumber     != null) fd.append('orderNumber',     String(this.form.orderNumber));
    fd.append('contentOrder',    this.form.contentOrder);
    fd.append('trackAttendance', String(this.form.trackAttendance));
    fd.append('featured',        String(this.form.featured));
    fd.append('pinned',          String(this.form.pinned));
    if (this.selectedFile) fd.append('image', this.selectedFile);

    this.submitting.set(true);
    this.api.createCourse(fd).subscribe({
      next: () => {
        this.toastr.success('تم إضافة الكورس بنجاح');
        this.router.navigate(['/level', this.levelId(), 'categories', this.form.categoryId, 'courses']);
      },
      error: (err: any) => {
        this.toastr.error(err?.error?.message || 'حدث خطأ أثناء الإضافة');
        this.submitting.set(false);
      }
    });
  }
}
