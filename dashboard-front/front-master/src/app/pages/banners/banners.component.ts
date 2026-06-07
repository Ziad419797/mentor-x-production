import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Banner } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-banners',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in pb-10">
      
      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-white font-bold text-2xl">البانرات والإعلانات</h2>
          <p class="text-slate-500 text-sm mt-1">إدارة الصور الإعلانية التي تظهر للطلاب في الصفحة الرئيسية</p>
        </div>
        <button (click)="openModal()" class="btn-primary">
          <span class="material-icons-round">add_photo_alternate</span>
          بانر جديد
        </button>
      </div>

      <!-- Banners Grid -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        <div *ngFor="let b of banners()" class="edu-card p-0 overflow-hidden group border-slate-800">
           <!-- Preview -->
           <div class="aspect-video bg-slate-900 relative overflow-hidden">
              <img [src]="b.imageUrl" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" [alt]="b.title">
              <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity flex items-end p-4">
                 <p class="text-white text-xs font-medium">{{ b.title }}</p>
              </div>
              
              <!-- Badges -->
              <div class="absolute top-3 right-3 flex flex-col gap-2">
                 <span [class.badge-success]="b.active" [class.badge-gray]="!b.active">
                    {{ b.active ? 'نشط' : 'معطل' }}
                 </span>
                 <span class="bg-indigo-500 text-white text-[9px] font-bold px-2 py-0.5 rounded shadow-lg">ترتيب: {{ b.displayOrder || 1 }}</span>
              </div>
           </div>

           <!-- Content & Actions -->
           <div class="p-4 flex items-center justify-between">
              <div>
                 <h4 class="text-white font-bold text-sm truncate max-w-[150px]">{{ b.title }}</h4>
                 <p class="text-[10px] text-slate-500 mt-1 italic">{{ b.linkUrl || 'لا يوجد رابط' }}</p>
              </div>
              <div class="flex items-center gap-1">
                 <button (click)="toggleStatus(b)" class="btn-icon h-9 w-9">
                    <span class="material-icons-round text-sm">{{ b.active ? 'visibility_off' : 'visibility' }}</span>
                 </button>
                 <button (click)="openModal(b)" class="btn-icon h-9 w-9 text-indigo-400">
                    <span class="material-icons-round text-sm">edit</span>
                 </button>
                 <button (click)="deleteBanner(b.id)" class="btn-icon h-9 w-9 text-red-400">
                    <span class="material-icons-round text-sm">delete</span>
                 </button>
              </div>
           </div>
        </div>

        <!-- Empty State -->
        <div *ngIf="banners().length === 0 && !isLoading()" class="col-span-full py-32 flex flex-col items-center justify-center text-slate-700 bg-slate-900/20 border-2 border-dashed border-slate-800 rounded-3xl">
           <span class="material-icons-round text-6xl mb-4 opacity-10">collections</span>
           <h3 class="text-slate-400 font-bold">لا توجد بانرات مضافة</h3>
           <button (click)="openModal()" class="btn-secondary mt-4 text-xs">إضافة أول بانر</button>
        </div>
        
        <div *ngIf="isLoading()" class="col-span-full py-32 flex justify-center">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-500"></div>
        </div>
      </div>

      <!-- Banner Modal -->
      <div *ngIf="showModal()" class="modal-overlay">
         <div class="modal-box max-w-lg">
            <div class="modal-header">
               <h3 class="text-white font-bold">{{ editingBanner() ? 'تعديل بانر' : 'إضافة بانر جديد' }}</h3>
               <button (click)="closeModal()" class="btn-icon"><span class="material-icons-round">close</span></button>
            </div>
            <div class="modal-body space-y-4">
               <div>
                  <label class="edu-label">عنوان البانر *</label>
                  <input type="text" [(ngModel)]="bannerForm.title" class="edu-input" placeholder="مثال: خصومات الصيف">
               </div>
               
               <div>
                  <label class="edu-label">صورة البانر *</label>
                  <div (click)="fileInput.click()" class="h-40 border-2 border-dashed border-slate-700 rounded-2xl mb-2 flex flex-col items-center justify-center cursor-pointer hover:border-indigo-500 hover:bg-indigo-500/5 transition-all overflow-hidden relative">
                    <input #fileInput type="file" (change)="onFileSelected($event)" accept="image/jpeg,image/png,image/gif,image/webp" class="hidden">
                    <img *ngIf="imagePreview()" [src]="imagePreview()" class="w-full h-full object-cover">
                    <div *ngIf="!imagePreview()" class="text-center">
                       <span class="material-icons-round text-4xl text-slate-600 mb-1">upload</span>
                       <p class="text-xs text-slate-500">اختر صورة البانر (16:9) <br> JPG, PNG, GIF</p>
                    </div>
                  </div>
                  <input type="text" [(ngModel)]="bannerForm.imageUrl" class="edu-input text-left ltr h-9 text-[10px]" placeholder="أو ضع رابط مباشر للصورة هنا">
               </div>

               <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="edu-label">رابط التوجيه (Link)</label>
                    <input type="text" [(ngModel)]="bannerForm.linkUrl" class="edu-input text-left ltr" placeholder="/courses/1 أو https://...">
                  </div>
                  <div>
                    <label class="edu-label">رقم الترتيب</label>
                    <input type="number" [(ngModel)]="bannerForm.displayOrder" class="edu-input" placeholder="1">
                  </div>
               </div>
               
               <div>
                 <label class="edu-label">تاريخ البدء (اختياري)</label>
                 <input type="datetime-local" [(ngModel)]="bannerForm.startDate" class="edu-input">
               </div>
               
               <div>
                 <label class="edu-label">تاريخ الانتهاء (اختياري)</label>
                 <input type="datetime-local" [(ngModel)]="bannerForm.endDate" class="edu-input">
               </div>
            </div>
            <div class="modal-footer">
               <button (click)="closeModal()" class="btn-secondary">إلغاء</button>
               <button (click)="saveBanner()" [disabled]="!bannerForm.title || isSaving()" class="btn-primary px-10">
                 {{ isSaving() ? 'جاري الحفظ...' : 'حفظ البانر' }}
               </button>
            </div>
         </div>
      </div>

    </div>
  `
})
export class BannersComponent implements OnInit {
  banners = signal<Banner[]>([]);
  isLoading = signal(false);
  isSaving = signal(false);
  
  showModal = signal(false);
  editingBanner = signal<Banner | null>(null);
  imagePreview = signal<string | null>(null);
  selectedFile: File | null = null;
  
  bannerForm: any = {
    title: '',
    imageUrl: '',
    linkUrl: '',
    displayOrder: 1,
    startDate: '',
    endDate: '',
    active: true
  };

  constructor(private api: ApiService, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.loadBanners();
  }
  
  loadBanners() {
    this.isLoading.set(true);
    this.api.getBanners().pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: (banners) => {
        this.banners.set(banners || []);
      },
      error: (err) => {
        console.error('Error loading banners:', err);
        if (err.status === 500) {
          this.toastr.error('خدمة البانرات غير متاحة حالياً');
        } else {
          this.toastr.error('فشل تحميل البانرات');
        }
      }
    });
  }

  openModal(banner?: Banner) {
    if (banner) {
      this.editingBanner.set(banner);
      this.bannerForm = {
        title: banner.title || '',
        imageUrl: banner.imageUrl || '',
        linkUrl: banner.linkUrl || '',
        displayOrder: banner.displayOrder || 1,
        startDate: banner.startDate ? new Date(banner.startDate).toISOString().slice(0, 16) : '',
        endDate: banner.endDate ? new Date(banner.endDate).toISOString().slice(0, 16) : '',
        active: banner.active !== undefined ? banner.active : true
      };
      this.imagePreview.set(banner.imageUrl ?? null);
    } else {
      this.editingBanner.set(null);
      this.bannerForm = {
        title: '',
        imageUrl: '',
        linkUrl: '',
        displayOrder: this.banners().length + 1,
        startDate: '',
        endDate: '',
        active: true
      };
      this.imagePreview.set(null);
    }
    this.selectedFile = null;
    this.showModal.set(true);
  }

  closeModal() {
    this.showModal.set(false);
    this.editingBanner.set(null);
    this.selectedFile = null;
    this.imagePreview.set(null);
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        this.toastr.error('حجم الصورة كبير جداً (الأقصى 5 ميجابايت)');
        return;
      }
      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = () => this.imagePreview.set(reader.result as string);
      reader.readAsDataURL(file);
      this.bannerForm.imageUrl = '';
    }
  }

  saveBanner() {
    if (!this.bannerForm.title) {
      this.toastr.error('يرجى إدخال عنوان البانر');
      return;
    }
    
    this.isSaving.set(true);
    const formData = new FormData();
    formData.append('title', this.bannerForm.title);
    formData.append('linkUrl', this.bannerForm.linkUrl || '');
    formData.append('displayOrder', this.bannerForm.displayOrder?.toString() || '1');
    
    if (this.bannerForm.startDate) {
      formData.append('startDate', new Date(this.bannerForm.startDate).toISOString());
    }
    if (this.bannerForm.endDate) {
      formData.append('endDate', new Date(this.bannerForm.endDate).toISOString());
    }
    
    if (this.selectedFile) {
      formData.append('imageFile', this.selectedFile);
    } else if (this.bannerForm.imageUrl) {
      formData.append('imageUrl', this.bannerForm.imageUrl);
    }
    
    const request = this.editingBanner()
      ? this.api.updateBanner(this.editingBanner()!.id, formData)
      : this.api.createBanner(formData);
    
    request.pipe(
      finalize(() => this.isSaving.set(false))
    ).subscribe({
      next: () => {
        this.toastr.success(this.editingBanner() ? 'تم تحديث البانر' : 'تم إضافة البانر');
        this.closeModal();
        this.loadBanners();
      },
      error: (err) => {
        console.error('Error saving banner:', err);
        const errorMsg = err.error?.message || 'فشل حفظ البانر';
        this.toastr.error(errorMsg);
      }
    });
  }

  toggleStatus(banner: Banner) {
    this.isLoading.set(true);
    this.api.toggleBannerStatus(banner.id).pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: () => {
        this.toastr.success(banner.active ? 'تم تعطيل البانر' : 'تم تفعيل البانر');
        this.loadBanners();
      },
      error: (err) => {
        console.error('Error toggling banner:', err);
        this.toastr.error('فشل تغيير حالة البانر');
      }
    });
  }

  deleteBanner(id: number) {
    if (!confirm('هل أنت متأكد من حذف هذا البانر؟')) return;
    
    this.isLoading.set(true);
    this.api.deleteBanner(id).pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: () => {
        this.toastr.success('تم حذف البانر');
        this.loadBanners();
      },
      error: (err) => {
        console.error('Error deleting banner:', err);
        this.toastr.error('فشل حذف البانر');
      }
    });
  }
}
