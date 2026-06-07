import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Course, Session, Week, Material } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { CategoryStateService } from '../../services/category-state.service';


@Component({
  selector: 'app-materials',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-8 animate-fade-in pb-10">
      
      <!-- Top Context Bar -->
      <div class="edu-card bg-slate-900/40 p-6 flex flex-col lg:flex-row items-center gap-6 border-b-2 border-indigo-500/20">
         <div class="flex items-center gap-5 flex-1">
            <div class="w-14 h-14 rounded-2xl bg-indigo-500/10 flex items-center justify-center text-indigo-400 shadow-inner">
               <span class="material-icons-round text-3xl">inventory_2</span>
            </div>
            <div class="flex-1">
               <h2 class="text-white font-black text-2xl tracking-tight">مكتبة المحتوى التعليمي</h2>
               <div class="flex flex-wrap items-center gap-x-4 gap-y-2 mt-1">
                  <div class="flex items-center gap-1">
                     <span class="text-slate-500 text-[10px] uppercase font-bold tracking-tighter">المستوى:</span>
                     <select (change)="onCourseChange($event)" class="bg-transparent text-indigo-400 text-xs font-bold border-none p-0 focus:ring-0 cursor-pointer" [ngModel]="selectedCourseId()">
                        <option [value]="0">اختر المستوى</option>
                        <option *ngFor="let c of courses()" [value]="c.id">{{ c.title }}</option>
                     </select>
                  </div>
                  <span class="w-1 h-1 rounded-full bg-slate-700"></span>
                  <div class="flex items-center gap-1">
                     <span class="text-slate-500 text-[10px] uppercase font-bold tracking-tighter">الوحدة:</span>
                     <select (change)="onSessionChange($event)" class="bg-transparent text-indigo-400 text-xs font-bold border-none p-0 focus:ring-0 cursor-pointer" [disabled]="selectedCourseId() === 0" [ngModel]="selectedSessionId()">
                        <option [value]="0">اختر الوحدة</option>
                        <option *ngFor="let s of sessions()" [value]="s.id">{{ s.title }}</option>
                     </select>
                  </div>
                  <span class="w-1 h-1 rounded-full bg-slate-700"></span>
                  <div class="flex items-center gap-1">
                     <span class="text-slate-500 text-[10px] uppercase font-bold tracking-tighter">الدرس:</span>
                     <select (change)="onWeekChange($event)" class="bg-transparent text-indigo-400 text-xs font-bold border-none p-0 focus:ring-0 cursor-pointer" [disabled]="selectedSessionId() === 0" [ngModel]="selectedWeekId()">
                        <option [value]="0">اختر الدرس</option>
                        <option *ngFor="let w of weeks()" [value]="w.id">{{ w.title }}</option>
                     </select>
                  </div>
               </div>
            </div>
         </div>
         <button (click)="openModal()" [disabled]="selectedWeekId() === 0" class="btn-primary h-12 px-8 shadow-lg shadow-indigo-500/20">
            <span class="material-icons-round">cloud_upload</span>
            رفع محتوى جديد
         </button>
      </div>

      <!-- Assets Grid -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        
        <!-- Asset Card -->
        <div *ngFor="let m of materials(); trackBy: trackByMaterial" class="asset-card group">
           <div class="h-36 bg-slate-950 flex items-center justify-center relative overflow-hidden">
              <!-- Visual feedback based on type -->
              <div class="absolute inset-0 bg-gradient-to-t from-slate-950 via-transparent to-transparent opacity-80"></div>
              
              <div [ngClass]="getTypeClass(m?.materialType)" class="w-16 h-16 rounded-3xl flex items-center justify-center shadow-2xl group-hover:scale-110 transition-transform duration-500">
                 <span class="material-icons-round text-3xl">{{ getTypeIcon(m?.materialType) }}</span>
              </div>

              <!-- Top Left: Order -->
              <div class="absolute top-3 left-3 w-6 h-6 rounded-lg bg-slate-900/80 border border-slate-800 flex items-center justify-center text-[10px] font-black text-slate-500">
                 {{ m.orderNumber }}
              </div>

              <!-- Top Right: Status -->
              <div class="absolute top-3 right-3">
                 <span [class.bg-emerald-500]="m.status === 'ACTIVE'" [class.bg-slate-700]="m.status !== 'ACTIVE'" class="w-2 h-2 rounded-full block shadow-[0_0_10px_rgba(0,0,0,0.5)]"></span>
              </div>
           </div>

           <div class="p-4 space-y-4">
              <div class="h-10">
                 <h4 class="text-white font-bold text-xs line-clamp-2 leading-relaxed">{{ m.fileName }}</h4>
              </div>
              
              <div class="flex items-center justify-between pt-3 border-t border-slate-800/50">
                 <span class="text-[9px] text-slate-500 font-black uppercase tracking-widest">{{ m.materialType }}</span>
                 <div class="flex items-center gap-1">
                    <button (click)="openModal(m)" class="btn-icon h-8 w-8 text-slate-500 hover:text-indigo-400 transition-colors">
                       <span class="material-icons-round text-sm">edit</span>
                    </button>
                    <button (click)="deleteMaterial(m.id)" class="btn-icon h-8 w-8 text-slate-500 hover:text-red-400 transition-colors">
                       <span class="material-icons-round text-sm">delete_outline</span>
                    </button>
                 </div>
              </div>
           </div>
        </div>

        <!-- Create CTA Placeholder -->
        <div *ngIf="selectedWeekId() > 0" (click)="openModal()" class="asset-card-placeholder group">
           <div class="w-12 h-12 rounded-2xl bg-slate-900 border-2 border-dashed border-slate-800 flex items-center justify-center text-slate-700 group-hover:border-indigo-500 group-hover:text-indigo-400 transition-all duration-500">
              <span class="material-icons-round text-2xl">add</span>
           </div>
           <p class="text-slate-500 font-bold text-[10px] mt-3 group-hover:text-white transition-colors">إضافة محتوى للدرس</p>
        </div>

        <!-- No Selection State -->
        <div *ngIf="selectedWeekId() === 0" class="col-span-full py-32 flex flex-col items-center justify-center text-center opacity-50">
           <div class="w-20 h-20 rounded-full bg-slate-900 border-2 border-dashed border-slate-800 flex items-center justify-center text-slate-800 mb-6">
              <span class="material-icons-round text-4xl">folder_zip</span>
           </div>
           <h3 class="text-slate-500 font-medium">يرجى تحديد (المستوى > الوحدة > الدرس) بالأعلى لعرض المحتوى</h3>
        </div>

      </div>

      <!-- Material Modal -->
      <div *ngIf="showModal()" class="modal-overlay">
        <div class="modal-box max-w-lg">
          <div class="modal-header">
            <h3 class="text-white font-bold">{{ editingMaterial() ? 'تعديل مادة' : 'إضافة مادة جديدة' }}</h3>
            <button (click)="showModal.set(false)" class="btn-icon"><span class="material-icons-round">close</span></button>
          </div>
          <div class="modal-body space-y-4">
             <div>
               <label class="edu-label">عنوان العنصر التعليمي *</label>
               <input type="text" [(ngModel)]="materialForm.fileName" class="edu-input" placeholder="مثال: فيديو شرح الدرس">
             </div>
             <div class="grid grid-cols-2 gap-4">
               <div>
                 <label class="edu-label">نوع المادة</label>
                 <select [(ngModel)]="materialForm.materialType" class="edu-select">
                    <option value="VIDEO">فيديو مباشر</option>
                    <option value="YOUTUBE">يوتيوب</option>
                    <option value="PDF">PDF</option>
                    <option value="DOC">مستند Word</option>
                    <option value="PPT">عرض تقديمي</option>
                    <option value="IMAGE">صورة</option>
                    <option value="AUDIO">صوت</option>
                    <option value="ARCHIVE">ملف مضغوط</option>
                    <option value="OTHER">أخرى / رابط</option>
                 </select>
               </div>
               <div>
                 <label class="edu-label">رقم الترتيب</label>
                 <input type="number" [(ngModel)]="materialForm.orderNumber" class="edu-input" placeholder="1">
               </div>
             </div>

             <!-- رابط المادة — مطلوب دائماً -->
             <div>
                <label class="edu-label">
                  {{ materialForm.materialType === 'YOUTUBE' ? 'رابط يوتيوب' : (materialForm.materialType === 'PDF' || materialForm.materialType === 'DOC' || materialForm.materialType === 'PPT' || materialForm.materialType === 'ARCHIVE' ? 'رابط الملف' : 'رابط المحتوى') }}
                </label>
                <input type="url" [(ngModel)]="materialForm.fileUrl" class="edu-input text-left ltr" placeholder="https://...">
                <p *ngIf="materialForm.materialType === 'PDF' || materialForm.materialType === 'DOC' || materialForm.materialType === 'ARCHIVE'" class="text-[10px] text-slate-500 mt-1 italic">ملاحظة: يمكنك رفع الملفات إلى Google Drive أو Dropbox ووضع الرابط هنا.</p>
             </div>
          </div>
          <div class="modal-footer">
            <button (click)="showModal.set(false)" class="btn-secondary">إلغاء</button>
            <button (click)="saveMaterial()" [disabled]="!materialForm.fileName || !materialForm.fileUrl" class="btn-primary px-10">حفظ المادة</button>
          </div>
        </div>
      </div>

    </div>
  `
})
export class MaterialsComponent implements OnInit {
  courses = signal<Course[]>([]);
  sessions = signal<Session[]>([]);
  weeks = signal<Week[]>([]);
  materials = signal<Material[]>([]);

  selectedCourseId = signal(0);
  selectedSessionId = signal(0);
  selectedWeekId = signal(0);

  showModal = signal(false);
  editingMaterial = signal<Material | null>(null);
  materialForm: any = {
    fileName: '',
    materialType: 'VIDEO',
    fileUrl: '',
    orderNumber: 1
  };

  constructor(
    private api: ApiService, 
    private toastr: ToastrService,
    public categoryState: CategoryStateService
  ) {}

  ngOnInit(): void {
    // Load courses
    this.api.getCourses(0, 100).subscribe({
      next: (pg) => {
        this.courses.set(pg.content || []);
        // Auto-select from global state if available
        const globalCourse = this.categoryState.currentCourse;
        if (globalCourse) {
          this.selectedCourseId.set(globalCourse.id);
          this.loadSessions(globalCourse.id);
        }
      },
      error: () => this.courses.set([])
    });

    // 3. Sync with Unit (Session)
    this.categoryState.selectedSession$.subscribe(session => {
      if (session && session.id !== this.selectedSessionId()) {
        this.selectedSessionId.set(session.id);
        this.loadWeeks(session.id);
      }
    });
  }

  onCourseChange(event: any) {
    const id = Number(event.target.value);
    const course = this.courses().find(c => c.id === id) || null;
    this.selectedCourseId.set(id);
    this.categoryState.selectCourse(course);
    this.selectedSessionId.set(0);
    this.selectedWeekId.set(0);
    this.sessions.set([]);
    this.weeks.set([]);
    this.materials.set([]);
    if (id > 0) {
      this.loadSessions(id);
    }
  }

  loadSessions(courseId: number) {
    this.api.getSessionsByCourse(courseId).subscribe({
      next: (res) => {
        this.sessions.set(Array.isArray(res) ? res : []);
        // Auto-select session if global
        const globalSession = this.categoryState.currentSession;
        if (globalSession && globalSession.courseId === courseId) {
          this.selectedSessionId.set(globalSession.id);
          this.loadWeeks(globalSession.id);
        }
      }
    });
  }

  onSessionChange(event: any) {
    const id = Number(event.target.value);
    const session = this.sessions().find(s => s.id === id) || null;
    this.selectedSessionId.set(id);
    this.categoryState.selectSession(session);
    this.selectedWeekId.set(0);
    this.weeks.set([]);
    this.materials.set([]);
    if (id > 0) {
      this.loadWeeks(id);
    }
  }

  loadWeeks(sessionId: number) {
    this.api.getWeeksBySession(sessionId).subscribe({
      next: (res) => this.weeks.set(Array.isArray(res) ? res : []),
      error: () => this.weeks.set([])
    });
  }

  onWeekChange(event: any) {
    const id = Number(event.target.value);
    this.selectedWeekId.set(id);
    this.loadMaterials();
  }

  loadMaterials() {
    if (this.selectedWeekId() > 0) {
      this.api.getMaterialsByWeek(this.selectedWeekId()).subscribe({
        next: (m) => this.materials.set(m || []),
        error: (err) => {
          console.error('Failed to load materials:', err);
          this.materials.set([]);
        }
      });
    }
  }

  openModal(m?: Material) {
    if (m) {
      this.editingMaterial.set(m);
      this.materialForm = {
        fileName: m.fileName || '',
        materialType: m.materialType || 'VIDEO',
        fileUrl: m.fileUrl || '',
        orderNumber: m.orderNumber || 1
      };
    } else {
      this.editingMaterial.set(null);
      this.materialForm = {
        fileName: '',
        materialType: 'VIDEO',
        fileUrl: '',
        orderNumber: this.materials().length + 1
      };
    }
    this.showModal.set(true);
  }

  saveMaterial() {
    if (!this.materialForm.fileName) {
      this.toastr.error('يرجى إدخال عنوان المادة');
      return;
    }
    if (!this.materialForm.fileUrl) {
      this.toastr.error('يرجى إدخال رابط المحتوى');
      return;
    }

    // Backend expects: materialType, fileUrl, fileName, weekIds (Set<Long>), orderNumber
    const data = {
      materialType: this.materialForm.materialType,
      fileUrl: this.materialForm.fileUrl,
      fileName: this.materialForm.fileName,
      orderNumber: this.materialForm.orderNumber || 1,
      weekIds: [this.selectedWeekId()]  // ✅ مصفوفة مش مفرد
    };

    const obs = this.editingMaterial()
      ? this.api.updateMaterial(this.editingMaterial()!.id, data)
      : this.api.createMaterial(data);

    obs.subscribe({
      next: () => {
        this.toastr.success('تم حفظ المادة بنجاح');
        this.showModal.set(false);
        this.loadMaterials();
      },
      error: (err) => {
        console.error('Failed to save material:', err);
        this.toastr.error(err?.error?.message || 'حدث خطأ أثناء الحفظ');
      }
    });
  }

  deleteMaterial(id: number) {
    if (!id || !confirm('هل أنت متأكد من حذف هذه المادة؟')) return;
    this.api.deleteMaterial(id).subscribe({
      next: () => {
        this.toastr.success('تم الحذف');
        this.loadMaterials();
      },
      error: (err) => {
        console.error('Failed to delete material:', err);
        this.toastr.error('حدث خطأ أثناء الحذف');
      }
    });
  }

  trackByMaterial(index: number, m: Material): number {
    return m?.id || index;
  }

  getTypeIcon(type?: string): string {
    switch (type) {
      case 'VIDEO':   return 'play_circle';
      case 'YOUTUBE': return 'smart_display';
      case 'PDF':     return 'picture_as_pdf';
      case 'DOC':     return 'description';
      case 'PPT':     return 'slideshow';
      case 'IMAGE':   return 'image';
      case 'AUDIO':   return 'headphones';
      case 'ARCHIVE': return 'folder_zip';
      case 'OTHER':   return 'link';
      default:        return 'help_outline';
    }
  }

  getTypeClass(type?: string): string {
    switch (type) {
      case 'VIDEO':   return 'bg-red-500/10 text-red-400 border-red-500/20';
      case 'YOUTUBE': return 'bg-red-600/10 text-red-500 border-red-600/20';
      case 'PDF':     return 'bg-blue-500/10 text-blue-400 border-blue-500/20';
      case 'DOC':     return 'bg-sky-500/10 text-sky-400 border-sky-500/20';
      case 'PPT':     return 'bg-orange-500/10 text-orange-400 border-orange-500/20';
      case 'IMAGE':   return 'bg-purple-500/10 text-purple-400 border-purple-500/20';
      case 'AUDIO':   return 'bg-pink-500/10 text-pink-400 border-pink-500/20';
      case 'ARCHIVE': return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20';
      case 'OTHER':   return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
      default:        return 'bg-slate-500/10 text-slate-400';
    }
  }
}
