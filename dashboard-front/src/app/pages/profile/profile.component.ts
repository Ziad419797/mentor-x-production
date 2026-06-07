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

                 <!-- Social Links -->
                 <div class="border-t border-slate-800 pt-6">
                    <h5 class="text-white font-bold text-sm mb-4 flex items-center gap-2">
                      <span class="material-icons-round text-indigo-400 text-sm">share</span>
                      روابط السوشيال ميديا
                    </h5>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label class="edu-label text-red-400">يوتيوب</label>
                        <input type="url" formControlName="youtubeUrl" class="edu-input" placeholder="https://youtube.com/...">
                      </div>
                      <div>
                        <label class="edu-label text-blue-400">فيسبوك</label>
                        <input type="url" formControlName="facebookUrl" class="edu-input" placeholder="https://facebook.com/...">
                      </div>
                      <div>
                        <label class="edu-label text-pink-400">إنستغرام</label>
                        <input type="url" formControlName="instagramUrl" class="edu-input" placeholder="https://instagram.com/...">
                      </div>
                      <div>
                        <label class="edu-label text-slate-300">تيك توك</label>
                        <input type="url" formControlName="tiktokUrl" class="edu-input" placeholder="https://tiktok.com/...">
                      </div>
                      <div>
                        <label class="edu-label text-green-400">واتساب (رقم فقط)</label>
                        <input type="text" formControlName="whatsappNumber" class="edu-input ltr text-left" placeholder="201012345678">
                      </div>
                      <div>
                        <label class="edu-label text-sky-400">تيليغرام</label>
                        <input type="url" formControlName="telegramUrl" class="edu-input" placeholder="https://t.me/...">
                      </div>
                    </div>
                 </div>

                 <div class="pt-4 flex justify-end">
                    <button type="submit" [disabled]="saving() || profileForm.invalid" class="btn-primary px-12 h-12 shadow-lg shadow-indigo-600/20 active:scale-95 transition-all">
                       <span *ngIf="!saving()">حفظ التغييرات</span>
                       <span *ngIf="saving()" class="flex items-center gap-2 animate-pulse">جاري الحفظ...</span>
                    </button>
                 </div>
              </form>
           </div>

           <!-- Home Card Image & Logo Upload -->
           <div class="edu-card p-8 mt-6">
             <h4 class="text-white font-bold text-xl flex items-center gap-3 mb-6">
               <span class="w-1.5 h-8 bg-orange-500 rounded-full"></span>
               كارد الهوم بتاعك
             </h4>
             <p class="text-slate-400 text-sm mb-2">ارفع صورة ديزاين كامل (مع معلوماتك، الصور، والنصوص) — ستظهر لجميع الطلاب في الصفحة الرئيسية</p>
             <p class="text-orange-400 text-xs font-bold mb-6 flex items-center gap-1">
               <span class="material-icons-round text-sm">straighten</span>
               المقاس المطلوب: <span class="bg-orange-500/20 px-2 py-0.5 rounded-lg">795 × 250 بكسل</span>
             </p>

             <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
               <!-- Home card image -->
               <div class="flex flex-col items-center gap-4 p-5 border border-dashed border-slate-700 rounded-2xl hover:border-orange-500/50 transition-colors">
                 <div class="w-full aspect-[4/3] rounded-xl overflow-hidden bg-slate-800/50 flex items-center justify-center">
                   <img *ngIf="profile()?.homeCardImageUrl" [src]="profile()?.homeCardImageUrl" alt="" class="w-full h-full object-cover">
                   <div *ngIf="!profile()?.homeCardImageUrl" class="flex flex-col items-center gap-2 text-slate-600">
                     <span class="material-icons-round text-4xl">image</span>
                     <span class="text-xs">لم يتم رفع صورة بعد</span>
                   </div>
                 </div>
                 <input #cardInput type="file" accept="image/*" class="hidden" (change)="onCardImageSelected($event)">
                 <div class="flex gap-2 w-full">
                   <button type="button" (click)="cardInput.click()" [disabled]="uploadingCard()"
                           class="btn-secondary flex-1 h-10 flex items-center justify-center gap-2 disabled:opacity-50">
                     <span class="material-icons-round text-sm">{{ uploadingCard() ? 'refresh' : 'upload' }}</span>
                     {{ uploadingCard() ? 'جاري الرفع...' : 'رفع صورة الكارد' }}
                   </button>
                   <button *ngIf="profile()?.homeCardImageUrl" type="button" (click)="deleteCardImage()"
                           class="h-10 px-3 rounded-xl bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors flex items-center justify-center" title="حذف الصورة">
                     <span class="material-icons-round text-sm">delete</span>
                   </button>
                 </div>
               </div>

               <!-- Logo -->
               <div class="flex flex-col items-center gap-4 p-5 border border-dashed border-slate-700 rounded-2xl hover:border-indigo-500/50 transition-colors">
                 <p class="text-indigo-400 text-xs font-bold flex items-center gap-1 self-start">
                   <span class="material-icons-round text-sm">straighten</span>
                   المقاس المطلوب: <span class="bg-indigo-500/20 px-2 py-0.5 rounded-lg">795 × 4000 بكسل</span>
                 </p>
                 <div class="w-full aspect-[4/3] rounded-xl overflow-hidden bg-slate-800/50 flex items-center justify-center">
                   <img *ngIf="profile()?.logoUrl" [src]="profile()?.logoUrl" alt="" class="max-h-24 max-w-full object-contain">
                   <div *ngIf="!profile()?.logoUrl" class="flex flex-col items-center gap-2 text-slate-600">
                     <span class="material-icons-round text-4xl">branding_watermark</span>
                     <span class="text-xs">لم يتم رفع لوجو بعد</span>
                   </div>
                 </div>
                 <input #logoInput type="file" accept="image/*" class="hidden" (change)="onLogoSelected($event)">
                 <div class="flex gap-2 w-full">
                   <button type="button" (click)="logoInput.click()" [disabled]="uploadingLogo()"
                           class="btn-secondary flex-1 h-10 flex items-center justify-center gap-2 disabled:opacity-50">
                     <span class="material-icons-round text-sm">{{ uploadingLogo() ? 'refresh' : 'upload' }}</span>
                     {{ uploadingLogo() ? 'جاري الرفع...' : 'رفع اللوجو' }}
                   </button>
                   <button *ngIf="profile()?.logoUrl" type="button" (click)="deleteLogo()"
                           class="h-10 px-3 rounded-xl bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors flex items-center justify-center" title="حذف اللوجو">
                     <span class="material-icons-round text-sm">delete</span>
                   </button>
                 </div>
               </div>

               <!-- Dark Logo -->
               <div class="flex flex-col items-center gap-4 p-5 border border-dashed border-slate-700 rounded-2xl hover:border-indigo-500/50 transition-colors">
                 <p class="text-indigo-400 text-xs font-bold flex items-center gap-1 self-start">
                   <span class="material-icons-round text-sm">dark_mode</span>
                   لوجو الدارك مود — <span class="bg-indigo-500/20 px-2 py-0.5 rounded-lg">795 × 4000 بكسل</span>
                 </p>
                 <div class="w-full aspect-[4/3] rounded-xl overflow-hidden bg-slate-900 flex items-center justify-center">
                   <img *ngIf="profile()?.darkLogoUrl" [src]="profile()?.darkLogoUrl" alt="" class="max-h-24 max-w-full object-contain">
                   <div *ngIf="!profile()?.darkLogoUrl" class="flex flex-col items-center gap-2 text-slate-600">
                     <span class="material-icons-round text-4xl">branding_watermark</span>
                     <span class="text-xs">لم يتم رفع لوجو الدارك مود</span>
                   </div>
                 </div>
                 <input #darkLogoInput type="file" accept="image/*" class="hidden" (change)="onDarkLogoSelected($event)">
                 <div class="flex gap-2 w-full">
                   <button type="button" (click)="darkLogoInput.click()" [disabled]="uploadingDarkLogo()"
                           class="btn-secondary flex-1 h-10 flex items-center justify-center gap-2 disabled:opacity-50">
                     <span class="material-icons-round text-sm">{{ uploadingDarkLogo() ? 'refresh' : 'upload' }}</span>
                     {{ uploadingDarkLogo() ? 'جاري الرفع...' : 'رفع لوجو الدارك مود' }}
                   </button>
                   <button *ngIf="profile()?.darkLogoUrl" type="button" (click)="deleteDarkLogo()"
                           class="h-10 px-3 rounded-xl bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors flex items-center justify-center" title="حذف لوجو الدارك مود">
                     <span class="material-icons-round text-sm">delete</span>
                   </button>
                 </div>
               </div>

               <!-- Teacher Card Light -->
               <div class="flex flex-col items-center gap-4 p-5 border border-dashed border-slate-700 rounded-2xl hover:border-indigo-500/50 transition-colors">
                 <p class="text-slate-300 text-xs font-bold flex items-center gap-1 self-start">
                   <span class="material-icons-round text-sm">style</span>
                   كارد المدرس — لايت مود
                 </p>
                 <div class="w-full rounded-xl overflow-hidden bg-slate-800 flex items-center justify-center" style="aspect-ratio:16/9">
                   <img *ngIf="profile()?.teacherCardUrl" [src]="profile()?.teacherCardUrl" alt="" class="w-full h-full object-contain">
                   <div *ngIf="!profile()?.teacherCardUrl" class="flex flex-col items-center gap-2 text-slate-600 py-8">
                     <span class="material-icons-round text-4xl">image</span>
                     <span class="text-xs">لم يتم رفع الكارد</span>
                   </div>
                 </div>
                 <input #cardLightInput type="file" accept="image/*" class="hidden" (change)="onTeacherCardSelected($event)">
                 <div class="flex gap-2 w-full">
                   <button type="button" (click)="cardLightInput.click()" [disabled]="uploadingCard2()"
                           class="btn-secondary flex-1 h-10 flex items-center justify-center gap-2 disabled:opacity-50">
                     <span class="material-icons-round text-sm">{{ uploadingCard2() ? 'refresh' : 'upload' }}</span>
                     {{ uploadingCard2() ? 'جاري الرفع...' : 'رفع الكارد' }}
                   </button>
                   <button *ngIf="profile()?.teacherCardUrl" type="button" (click)="deleteTeacherCard()"
                           class="h-10 px-3 rounded-xl bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors flex items-center justify-center">
                     <span class="material-icons-round text-sm">delete</span>
                   </button>
                 </div>
               </div>

               <!-- Teacher Card Dark -->
               <div class="flex flex-col items-center gap-4 p-5 border border-dashed border-slate-700 rounded-2xl hover:border-indigo-500/50 transition-colors">
                 <p class="text-indigo-400 text-xs font-bold flex items-center gap-1 self-start">
                   <span class="material-icons-round text-sm">dark_mode</span>
                   كارد المدرس — دارك مود
                 </p>
                 <div class="w-full rounded-xl overflow-hidden bg-slate-900 flex items-center justify-center" style="aspect-ratio:16/9">
                   <img *ngIf="profile()?.teacherCardDarkUrl" [src]="profile()?.teacherCardDarkUrl" alt="" class="w-full h-full object-contain">
                   <div *ngIf="!profile()?.teacherCardDarkUrl" class="flex flex-col items-center gap-2 text-slate-600 py-8">
                     <span class="material-icons-round text-4xl">image</span>
                     <span class="text-xs">لم يتم رفع كارد الدارك مود</span>
                   </div>
                 </div>
                 <input #cardDarkInput type="file" accept="image/*" class="hidden" (change)="onTeacherCardDarkSelected($event)">
                 <div class="flex gap-2 w-full">
                   <button type="button" (click)="cardDarkInput.click()" [disabled]="uploadingCardDark()"
                           class="btn-secondary flex-1 h-10 flex items-center justify-center gap-2 disabled:opacity-50">
                     <span class="material-icons-round text-sm">{{ uploadingCardDark() ? 'refresh' : 'upload' }}</span>
                     {{ uploadingCardDark() ? 'جاري الرفع...' : 'رفع كارد الدارك مود' }}
                   </button>
                   <button *ngIf="profile()?.teacherCardDarkUrl" type="button" (click)="deleteTeacherCardDark()"
                           class="h-10 px-3 rounded-xl bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors flex items-center justify-center">
                     <span class="material-icons-round text-sm">delete</span>
                   </button>
                 </div>
               </div>

             </div>
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
  uploadingCard = signal(false);
  uploadingLogo     = signal(false);
  uploadingDarkLogo    = signal(false);
  uploadingCard2       = signal(false);  // teacher card light
  uploadingCardDark    = signal(false);  // teacher card dark

  profileForm = this.fb.group({
    name: ['', Validators.required],
    phone: [{ value: '', disabled: true }],
    subject: [''],
    quote: [''],
    youtubeUrl: [''],
    facebookUrl: [''],
    instagramUrl: [''],
    tiktokUrl: [''],
    whatsappNumber: [''],
    telegramUrl: ['']
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
        this.profileForm.patchValue({
          name: p.name,
          phone: p.phone,
          subject: p.subject || '',
          quote: p.quote || '',
          youtubeUrl: (p as any).youtubeUrl || '',
          facebookUrl: (p as any).facebookUrl || '',
          instagramUrl: (p as any).instagramUrl || '',
          tiktokUrl: (p as any).tiktokUrl || '',
          whatsappNumber: (p as any).whatsappNumber || '',
          telegramUrl: (p as any).telegramUrl || ''
        });
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
    this.api.updateProfile({
      name: data.name!,
      subject: data.subject!,
      quote: data.quote!,
      youtubeUrl: data.youtubeUrl || '',
      facebookUrl: data.facebookUrl || '',
      instagramUrl: data.instagramUrl || '',
      tiktokUrl: data.tiktokUrl || '',
      whatsappNumber: data.whatsappNumber || '',
      telegramUrl: data.telegramUrl || ''
    } as any).subscribe({
      next: (updated) => {
        this.profile.set(updated);
        this.toastr.success('تم تحديث الملف الشخصي');
        this.saving.set(false);
      },
      error: () => {
        this.toastr.error('فشل التحديث');
        this.saving.set(false);
      }
    });
  }

  onImageSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.uploadingImage.set(true);
    this.api.uploadProfileImage(file).subscribe({
      next: (updated) => {
        this.profile.set(updated);
        this.toastr.success('تم تحديث صورة البروفايل');
        this.uploadingImage.set(false);
      },
      error: () => {
        this.toastr.error('فشل رفع الصورة');
        this.uploadingImage.set(false);
      }
    });
  }

  onCardImageSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.uploadingCard.set(true);
    this.api.uploadHomeCardImage(file).subscribe({
      next: (updated) => { this.profile.set(updated); this.toastr.success('تم رفع صورة الكارد'); this.uploadingCard.set(false); },
      error: () => { this.toastr.error('فشل الرفع'); this.uploadingCard.set(false); }
    });
  }

  onLogoSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.uploadingLogo.set(true);
    this.api.uploadLogo(file).subscribe({
      next: (updated) => { this.profile.set(updated); this.toastr.success('تم رفع اللوجو'); this.uploadingLogo.set(false); },
      error: () => { this.toastr.error('فشل الرفع'); this.uploadingLogo.set(false); }
    });
  }

  deleteCardImage() {
    this.api.deleteHomeCardImage().subscribe({
      next: (updated: any) => {
        this.profile.set(updated?.data ?? updated);
        this.toastr.success('تم حذف صورة الكارد');
      },
      error: () => this.toastr.error('فشل الحذف')
    });
  }

  deleteLogo() {
    this.api.deleteLogo().subscribe({
      next: (updated: any) => {
        this.profile.set(updated?.data ?? updated);
        this.toastr.success('تم حذف اللوجو');
      },
      error: () => this.toastr.error('فشل الحذف')
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
      error: () => {
        this.toastr.error('حدث خطأ');
        this.changingPass.set(false);
      }
    });
  }

  onDarkLogoSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.uploadingDarkLogo.set(true);
    this.api.uploadDarkLogo(file).subscribe({
      next: (updated) => { this.profile.set(updated); this.toastr.success('تم رفع لوجو الدارك مود'); this.uploadingDarkLogo.set(false); },
      error: () => { this.toastr.error('فشل الرفع'); this.uploadingDarkLogo.set(false); }
    });
  }

  deleteDarkLogo() {
    this.api.deleteDarkLogo().subscribe({
      next: (updated) => { this.profile.set(updated); this.toastr.success('تم حذف لوجو الدارك مود'); },
      error: () => this.toastr.error('فشل الحذف')
    });
  }

  onTeacherCardSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.uploadingCard2.set(true);
    this.api.uploadTeacherCard(file).subscribe({
      next: (updated) => { this.profile.set(updated); this.toastr.success('تم رفع الكارد'); this.uploadingCard2.set(false); },
      error: () => { this.toastr.error('فشل الرفع'); this.uploadingCard2.set(false); }
    });
  }

  onTeacherCardDarkSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.uploadingCardDark.set(true);
    this.api.uploadTeacherCardDark(file).subscribe({
      next: (updated) => { this.profile.set(updated); this.toastr.success('تم رفع كارد الدارك مود'); this.uploadingCardDark.set(false); },
      error: () => { this.toastr.error('فشل الرفع'); this.uploadingCardDark.set(false); }
    });
  }

  deleteTeacherCard() {
    this.api.deleteTeacherCard().subscribe({
      next: (updated) => { this.profile.set(updated); this.toastr.success('تم حذف الكارد'); },
      error: () => this.toastr.error('فشل الحذف')
    });
  }

  deleteTeacherCardDark() {
    this.api.deleteTeacherCardDark().subscribe({
      next: (updated) => { this.profile.set(updated); this.toastr.success('تم حذف كارد الدارك مود'); },
      error: () => this.toastr.error('فشل الحذف')
    });
  }
}
