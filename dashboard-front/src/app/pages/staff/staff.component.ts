import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Staff } from '../../models/models';
import { ToastrService } from 'ngx-toastr';
import { extractList } from '../../core/api-response.model';

@Component({
  selector: 'app-staff',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: ` <div class="space-y-6 animate-fade-in pb-10">
      
      <!-- Header -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-white font-bold text-2xl tracking-tight">إدارة فريق العمل</h2>
          <p class="text-slate-500 text-sm mt-1">إضافة موظفين وتحديد صلاحياتهم في المنصة</p>
        </div>
        <button (click)="openStaffModal()" class="btn-primary">
          <span class="material-icons-round">person_add</span>
          عضو جديد
        </button>
      </div>

      <!-- Staff Table -->
      <div class="edu-card p-0 overflow-hidden shadow-xl">
        <table class="edu-table">
          <thead>
            <tr>
              <th>الموظف</th>
              <th>رقم الهاتف</th>
              <th>الصلاحيات</th>
              <th>الحالة</th>
              <th class="text-center">الإجراءات</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let s of staffList()">
              <td>
                <div class="flex items-center gap-3">
                   <div class="w-9 h-9 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-xs font-bold text-slate-400">
                     {{ s.fullName ? s.fullName[0] : '?' }}
                   </div>
                   <div class="flex flex-col">
                     <span class="text-slate-200 font-bold text-xs">{{ s.fullName }}</span>
                     <span class="text-[10px] text-slate-500 italic">{{ s.notes || 'لا توجد ملاحظات' }}</span>
                   </div>
                </div>
              </td>
              <td class="text-xs ltr text-right text-slate-400">{{ s.phone }}</td>
              <td>
                <div class="flex flex-wrap gap-1 max-w-[300px]">
                  <span *ngFor="let p of (s.permissions || []).slice(0, 3)" class="text-[9px] bg-indigo-500/10 text-indigo-400 px-2 py-0.5 rounded border border-indigo-500/20">
                    {{ p }}
                  </span>
                  <span *ngIf="(s.permissions.length || 0) > 3" class="text-[9px] bg-slate-800 text-slate-500 px-2 py-0.5 rounded">
                    +{{ (s.permissions.length || 0) - 3 }} أخرى
                  </span>
                </div>
              </td>
              <td>
                 <button (click)="toggleStatus(s)" 
                         [class.text-emerald-400]="s.status === 'ACTIVE'"
                         [class.text-slate-600]="s.status !== 'ACTIVE'"
                         class="flex items-center gap-2 text-xs font-medium hover:opacity-80 transition-opacity">
                   <span class="w-1.5 h-1.5 rounded-full" [class.bg-emerald-500]="s.status === 'ACTIVE'" [class.bg-slate-700]="s.status !== 'ACTIVE'"></span>
                   {{ s.status === 'ACTIVE' ? 'نشط' : 'معطل' }}
                 </button>
              </td>
              <td>
                <div class="flex items-center justify-center gap-2">
                  <button (click)="openStaffModal(s)" class="btn-icon text-indigo-400" title="تعديل">
                    <span class="material-icons-round text-sm">edit</span>
                  </button>
                  <button (click)="deleteStaff(s.id)" class="btn-icon text-red-400" title="حذف">
                    <span class="material-icons-round text-sm">delete</span>
                  </button>
                </div>
              </td>
            </tr>
            <tr *ngIf="staffList().length === 0">
               <td colspan="5" class="text-center py-20 text-slate-500 italic">لا يوجد موظفون مضافون بعد</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Staff Modal -->
      <div *ngIf="showModal()" class="modal-overlay">
        <div class="modal-box max-w-2xl flex flex-col h-[85vh]">
          <div class="modal-header">
            <h3 class="text-white font-bold">{{ editingStaff() ? 'تعديل بيانات عضو' : 'إضافة عضو جديد' }}</h3>
            <button (click)="showModal.set(false)" class="btn-icon"><span class="material-icons-round">close</span></button>
          </div>
          <div class="modal-body flex-1 overflow-y-auto custom-scrollbar p-6">
            <div class="space-y-6">
              <!-- Basic Info -->
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="edu-label">الاسم الكامل *</label>
                  <input type="text" [(ngModel)]="staffForm.fullName" class="edu-input" placeholder="اسم الموظف">
                </div>
                <div>
                  <label class="edu-label">رقم الهاتف *</label>
                  <input type="tel" [(ngModel)]="staffForm.phone" class="edu-input ltr text-right" placeholder="01xxxxxxxxx">
                </div>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="edu-label">كلمة المرور *</label>
                  <input type="password" [(ngModel)]="staffForm.password" class="edu-input" placeholder="مثال: سكرتارية السنتر">
                </div>
                <div>
                  <label class="edu-label">ملاحظات</label>
                  <input type="text" [(ngModel)]="staffForm.notes" class="edu-input" placeholder="مثال: سكرتارية السنتر">
                </div>
              </div>

              <!-- Permissions Grid -->
              <div class="pt-4 border-t border-slate-800">
                <h4 class="text-slate-300 font-bold text-sm mb-4">الصلاحيات الممنوحة</h4>
                <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
                   <label *ngFor="let p of allPermissions()" class="flex items-center gap-3 p-3 bg-slate-800/40 rounded-xl border border-slate-700/50 cursor-pointer hover:bg-indigo-600/10 hover:border-indigo-500/30 transition-all group">
                      <input type="checkbox" [checked]="hasPermission(p)" (change)="togglePermission(p)" class="w-4 h-4 accent-indigo-500">
                      <span class="text-[10px] text-slate-400 group-hover:text-slate-200">{{ p }}</span>
                   </label>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button (click)="showModal.set(false)" class="btn-secondary">إلغاء</button>
            <button (click)="saveStaff()" [disabled]="!staffForm.fullName || !staffForm.phone" class="btn-primary px-10">حفظ العضو</button>
          </div>
        </div>
      </div>

    </div>`
})
export class StaffComponent implements OnInit {

  staffList = signal<Staff[]>([]);
  allPermissions = signal<string[]>([]);

  showModal = signal(false);
  editingStaff = signal<Staff | null>(null);

  staffForm: any = {
    fullName: '',
    phone: '',
    password: '',
    notes: '',
    permissions: []
  };

  constructor(
    private api: ApiService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadStaff();

    this.api.getStaffPermissionsList().subscribe({
      next: (res) => {
        const list = extractList<string>(res);
        if (list.length > 0) {
          this.allPermissions.set(list);
        } else if (res && typeof res === 'object') {
          // If it's an object { PERM: 'Label' }, extract keys
          const raw = (res as any).data ?? res;
          this.allPermissions.set(Object.keys(raw));
        }
      },
      error: () => this.allPermissions.set([])
    });
  }

  loadStaff() {
    this.api.getStaff().subscribe({
      next: (list) => this.staffList.set(list),  // getStaff() already uses extractList
      error: () => this.staffList.set([])
    });
  }

  openStaffModal(staff?: Staff) {
    if (staff) {
      this.editingStaff.set(staff);

      this.staffForm = {
        fullName: staff.fullName,
        phone: staff.phone,
        password: '',
        notes: staff.notes || '',
        permissions: [...staff.permissions]
      };
    } else {
      this.editingStaff.set(null);

      this.staffForm = {
        fullName: '',
        phone: '',
        password: '',
        notes: '',
        permissions: []
      };
    }

    this.showModal.set(true);
  }

  hasPermission(p: string) {
    return this.staffForm.permissions.includes(p);
  }

  togglePermission(p: string) {
    const idx = this.staffForm.permissions.indexOf(p);

    if (idx > -1) {
      this.staffForm.permissions.splice(idx, 1);
    } else {
      this.staffForm.permissions.push(p);
    }
  }

  saveStaff() {
    const obs = this.editingStaff()
      ? this.api.updateStaff(this.editingStaff()!.id, this.staffForm)
      : this.api.createStaff(this.staffForm);

    obs.subscribe({
      next: () => {
        this.toastr.success('تم الحفظ بنجاح');
        this.showModal.set(false);
        this.loadStaff();
      },
      error: (err) => {
        console.log(err);
        this.toastr.error('فشل في حفظ البيانات');
      }
    });
  }

  toggleStatus(s: Staff) {
    this.api.toggleStaffStatus(s.id).subscribe({
      next: () => this.loadStaff()
    });
  }

  deleteStaff(id: number) {
    if (!confirm('هل أنت متأكد من حذف هذا الموظف؟')) return;

    this.api.deleteStaff(id).subscribe({
      next: () => {
        this.toastr.success('تم الحذف');
        this.loadStaff();
      }
    });
  }
}
