import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { ApiService } from '../../services/api.service';
import { TeacherProfile } from '../../models/models';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="space-y-8 animate-fade-in max-w-4xl pb-10">

      <div class="grid grid-cols-1 lg:grid-cols-12 gap-8">

        <!-- Sidebar: Stats & Image -->
        <div class="lg:col-span-4 space-y-6">
           <div class="edu-card p-8 flex flex-col items-center text-center">
              <div class="relative group mb-6">
                 <div class="w-32 h-32 rounded-3xl overflow-hidden bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-5xl font-black text-white shadow-2xl shadow-indigo-500/20">
                   <img *ngIf="profile()?.profileImageUrl" [src]="profile()?.profileImageUrl" alt="" class="w-full h-full object-cover">
                   <span *ngIf="!profile()?.profileImageUrl">{{ profile()?.name?.[0] }}</span>
                 </div>
                 <input #fileInput type="file" accept="image/*" class="hidden" (change)="onImageSelected($event)">
                 <button type="button" (click)="fileInput.click()" [disabled]="uploadingImage()"
                         class="absolute -bottom-2 -right-2 w-10 h-10 rounded-xl bg-slate-800 border border-slate-700 text-white flex items-center justify-center hover:bg-indigo-600 transition-colors shadow-lg disabled:opacity-50">
                    <span *ngIf="!uploadingImage()" class="material-icons-round text-lg">photo_camera</span>
                    <span *ngIf="uploadingImage()" class="material-icons-round text-lg animate-spin">refresh</span>
                 </button>
              </div>

              <h3 class="text-white font-black text-2xl">{{ profile()?.name }}</h3>
              <p class="text-indigo-400 font-bold text-sm">{{ profile()?.subject || 'مدرس' }}</p>
           </div>

           <div class="edu-card p-6 bg-slate-900/40 space-y-4">
              <h4 class="text-white font-bold text-sm">الأمان والخصوصية</h4>
              <button (click)="showPassModal.set(true)" class="btn-secondary w-full justify-between h-11 px-4">
                 <span class="flex items-center gap-3">
                   <span class="material-icons-round text-sm">lock</span>
                   تغيير كلمة المرور
                 </span>
                 <span class="material-icons-round text-sm opacity-30">chevron_left</span>
              </button>
              <button class="btn-secondary w-full justify-between h-11 px-4 text-red-400 hover:bg-red-500/10 hover:border-red-500/20 transition-all">
                 <span class="flex items-center gap-3">
                   <span class="material-icons-round text-sm">logout</span>
                   تسجيل الخروج
                 </span>
              </button>
           </div>
        </div>

        <!-- Main Panel: Edit Form -->
        <div class="lg:col-span-8">
           <div class="edu-card p-8">
              <div class="flex items-center justify-between mb-8">
                 <h4 class="text-white font-bold text-xl flex items-center gap-3">
                   <span class="w-1.5 h-8 bg-indigo-500 rounded-full"></span>
                   البيانات الأساسية
                 </h4>
              </div>

              <form [formGroup]="profileForm" (ngSubmit)="saveProfile()" class="space-y-6">
                 <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                       <label class="edu-label">الاسم الكامل *</label>
                       <input type="text" formControlName="name" class="edu-input" placeholder="أدخل اسمك بالكامل">
                    </div>
                    <div>
                       <label class="edu-label">رقم الهاتف</label>
                       <input type="tel" formControlName="phone" class="edu-input bg-slate-800/30 opacity-60 cursor-not-allowed text-left ltr" readonly>
                    </div>
                 </div>

                 <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div class="md:col-span-2">
                       <label class="edu-label">المادة / التخصص</label>
                       <input type="text" formControlName="subject" class="edu-input" placeholder="مثال: مدرس أول لغة عربية">
                    </div>
                 </div>

                 <div>
                    <label class="edu-label">اقتباس تحفيزي <span class="text-slate-500 text-xs font-normal">(يظهر في صفحة الطالب الرئيسية)</span></label>
                    <textarea formControlName="quote" rows="2" class="edu-input min-h-[70px]" placeholder='مثال: "رحلة التفوق تبدأ هنا.. كن من أوائل الجمهورية معنا"'></textarea>
                 </div>

                 <div class="pt-4 flex justify-end">
                    <button type="submit" [disabled]="saving() || profileForm.invalid" class="btn-primary px-12 h-12 shadow-lg shadow-indigo-600/20 active:scale-95 transition-all">
                       <span *ngIf="!saving()">حفظ التغييرات</span>
                       <span *ngIf="saving()" class="flex items-center gap-2 animate-pulse">جاري الحفظ...</span>
                    </button>
                 </div>
              </form>
           </div>
        </div>

      </div>

      <!-- Change Password Modal -->
      <div *ngIf="showPassModal()" class="modal-overlay">
         <div class="modal-box max-sm:w-full max-w-sm">
            <div class="modal-header">
               <h3 class="text-white font-bold">تغيير كلمة المرور</h3>
               <button (click)="showPassModal.set(false)" class="btn-icon"><span class="material-icons-round">close</span></button>
            </div>
            <form [formGroup]="passForm" (ngSubmit)="changePassword()" class="modal-body space-y-4">
               <div>
                  <label class="edu-label">كلمة المرور الجديدة</label>
                  <input type="password" formControlName="newPassword" class="edu-input" placeholder="⬢⬢⬢⬢⬢⬢⬢⬢">
               </div>
               <div>
                  <label class="edu-label">تأكيد كلمة المرور</label>
                  <input type="password" formControlName="confirmPassword" class="edu-input" placeholder="⬢⬢⬢⬢⬢⬢⬢⬢">
               </div>
               <div *ngIf="passForm.errors?.['mismatch'] && passForm.get('confirmPassword')?.touched" class="edu-error">كلمتا المرور غير متطابقتين</div>
            </form>
            <div class="modal-footer">
               <button (click)="showPassModal.set(false)" class="btn-secondary">إلغاء</button>
               <button (click)="changePassword()" [disabled]="passForm.invalid || changingPass()" class="btn-primary px-8">تحديث</button>
            </div>
         </div>
      </div>

    </div>
  `
})
export class ProfileComponent implements OnInit {
  profile = signal<TeacherProfile | null>(null);
  saving = signal(false);
  showPassModal = signal(false);
  changingPass = signal(false);
  uploadingImage = signal(false);

  profileForm = this.fb.group({
    name: ['', Validators.required],
    phone: [{ value: '', disabled: true }],
    subject: [''],
    quote: ['']
  });

  passForm = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', Validators.required]
  }, { validators: this.passwordMatchValidator });

  constructor(private api: ApiService, private fb: FormBuilder, private toastr: ToastrService) {}

  ngOnInit(): void {
    this.api.getProfile().subscribe({
      next: p => {
        this.profile.set(p);
        this.profileForm.patchValue({ name: p.name, phone: p.phone, subject: p.subject || '', quote: p.quote || '' });
      }
    });
  }

  passwordMatchValidator(g: any) {
    return g.get('newPassword')?.value === g.get('confirmPassword')?.value ? null : { mismatch: true };
  }

  saveProfile() {
    if (this.profileForm.invalid) return;
    this.saving.set(true);
    const data = this.profileForm.getRawValue();
    this.api.updateProfile({ name: data.name!, subject: data.subject!, quote: data.quote! } as any).subscribe({
      next: (updated) => { this.profile.set(updated); this.toastr.success('تم تحديث الملف الشخصي'); this.saving.set(false); },
      error: () => { this.toastr.error('فشل التحديث'); this.saving.set(false); }
    });
  }

  onImageSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.uploadingImage.set(true);
    this.api.uploadProfileImage(file).subscribe({
      next: (updated) => { this.profile.set(updated); this.toastr.success('تم تحديث صورة البروفايل'); this.uploadingImage.set(false); },
      error: () => { this.toastr.error('فشل رفع الصورة'); this.uploadingImage.set(false); }
    });
  }

  changePassword() {
    if (this.passForm.invalid) return;
    this.changingPass.set(true);
    this.api.resetPassword(this.profile()!.phone, this.passForm.value.newPassword!).subscribe({
      next: () => {
        this.toastr.success('تم تغيير كلمة المرور');
        this.showPassModal.set(false);
        this.passForm.reset();
        this.changingPass.set(false);
      },
      error: () => { this.toastr.error('حدث خطأ'); this.changingPass.set(false); }
    });
  }
}
