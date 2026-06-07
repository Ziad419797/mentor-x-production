import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-books-codes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="p-6 max-w-6xl mx-auto">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-gray-800 dark:text-white">أماكن بيع الكتب والأكواد</h1>
        <button (click)="openAdd()" class="flex items-center gap-2 bg-[#183764] text-white px-5 py-2.5 rounded-xl font-bold hover:bg-[#122a4e] transition-colors">
          <span class="text-lg">+</span> إضافة مكان
        </button>
      </div>

      <!-- Add/Edit Form -->
      @if (showForm()) {
        <div class="bg-white dark:bg-gray-800 rounded-2xl shadow p-6 mb-6 border border-gray-200 dark:border-gray-700">
          <h2 class="text-lg font-bold text-gray-800 dark:text-white mb-4">{{ editId() ? 'تعديل مكان' : 'إضافة مكان جديد' }}</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">الاسم *</label>
              <input [(ngModel)]="form.name" type="text" placeholder="اسم المكان"
                class="w-full border border-gray-300 dark:border-gray-600 rounded-xl px-4 py-2.5 bg-white dark:bg-gray-700 text-gray-800 dark:text-white focus:outline-none focus:ring-2 focus:ring-[#183764]" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">النوع *</label>
              <select [(ngModel)]="form.type"
                class="w-full border border-gray-300 dark:border-gray-600 rounded-xl px-4 py-2.5 bg-white dark:bg-gray-700 text-gray-800 dark:text-white focus:outline-none focus:ring-2 focus:ring-[#183764]">
                <option value="CENTER">سنتر</option>
                <option value="LIBRARY">مكتبة</option>
                <option value="OTHER">أخرى</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">العنوان</label>
              <input [(ngModel)]="form.address" type="text" placeholder="العنوان"
                class="w-full border border-gray-300 dark:border-gray-600 rounded-xl px-4 py-2.5 bg-white dark:bg-gray-700 text-gray-800 dark:text-white focus:outline-none focus:ring-2 focus:ring-[#183764]" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">رقم التليفون</label>
              <input [(ngModel)]="form.phone" type="text" placeholder="رقم التليفون"
                class="w-full border border-gray-300 dark:border-gray-600 rounded-xl px-4 py-2.5 bg-white dark:bg-gray-700 text-gray-800 dark:text-white focus:outline-none focus:ring-2 focus:ring-[#183764]" />
            </div>
            <div class="md:col-span-2">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">ملاحظات</label>
              <input [(ngModel)]="form.notes" type="text" placeholder="ملاحظات إضافية"
                class="w-full border border-gray-300 dark:border-gray-600 rounded-xl px-4 py-2.5 bg-white dark:bg-gray-700 text-gray-800 dark:text-white focus:outline-none focus:ring-2 focus:ring-[#183764]" />
            </div>
            <div class="flex items-center gap-6 md:col-span-2">
              <label class="flex items-center gap-3 cursor-pointer select-none">
                <input type="checkbox" [(ngModel)]="form.sellsBooks" class="w-5 h-5 rounded accent-[#183764]" />
                <span class="text-gray-700 dark:text-gray-300 font-medium">يبيع كتب 📚</span>
              </label>
              <label class="flex items-center gap-3 cursor-pointer select-none">
                <input type="checkbox" [(ngModel)]="form.sellsCodes" class="w-5 h-5 rounded accent-[#4BBBA0]" />
                <span class="text-gray-700 dark:text-gray-300 font-medium">يبيع أكواد 🔑</span>
              </label>
            </div>
          </div>
          <div class="flex gap-3 mt-5">
            <button (click)="save()" [disabled]="saving()"
              class="bg-[#183764] text-white px-6 py-2.5 rounded-xl font-bold hover:bg-[#122a4e] transition-colors disabled:opacity-60">
              {{ saving() ? 'جاري الحفظ...' : 'حفظ' }}
            </button>
            <button (click)="cancel()" class="bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 px-6 py-2.5 rounded-xl font-bold hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors">
              إلغاء
            </button>
          </div>
        </div>
      }

      <!-- Loading -->
      @if (loading()) {
        <div class="flex justify-center py-16">
          <div class="w-10 h-10 border-4 border-[#183764] border-t-transparent rounded-full animate-spin"></div>
        </div>
      } @else if (locations().length === 0) {
        <div class="text-center py-16 text-gray-400 dark:text-gray-500">
          <p class="text-5xl mb-4">🗺️</p>
          <p class="text-lg">لا توجد أماكن مضافة بعد</p>
        </div>
      } @else {
        <div class="bg-white dark:bg-gray-800 rounded-2xl shadow overflow-hidden border border-gray-200 dark:border-gray-700">
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="bg-[#183764] text-white">
                <tr>
                  <th class="text-right px-4 py-3 font-semibold">الاسم</th>
                  <th class="text-right px-4 py-3 font-semibold">النوع</th>
                  <th class="text-right px-4 py-3 font-semibold">العنوان</th>
                  <th class="text-right px-4 py-3 font-semibold">التليفون</th>
                  <th class="text-center px-4 py-3 font-semibold">كتب 📚</th>
                  <th class="text-center px-4 py-3 font-semibold">أكواد 🔑</th>
                  <th class="text-right px-4 py-3 font-semibold">ملاحظات</th>
                  <th class="text-center px-4 py-3 font-semibold">إجراءات</th>
                </tr>
              </thead>
              <tbody>
                @for (loc of locations(); track loc.id) {
                  <tr class="border-t border-gray-100 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-750 transition-colors">
                    <td class="px-4 py-3 font-medium text-gray-800 dark:text-white">{{ loc.name }}</td>
                    <td class="px-4 py-3">
                      <span [class]="typeClass(loc.type)" class="px-2.5 py-1 rounded-lg text-xs font-bold">
                        {{ typeLabel(loc.type) }}
                      </span>
                    </td>
                    <td class="px-4 py-3 text-gray-600 dark:text-gray-400">{{ loc.address || '—' }}</td>
                    <td class="px-4 py-3 text-gray-600 dark:text-gray-400">{{ loc.phone || '—' }}</td>
                    <td class="px-4 py-3 text-center">
                      <span [class]="loc.sellsBooks ? 'text-green-500' : 'text-gray-300 dark:text-gray-600'" class="text-xl">
                        {{ loc.sellsBooks ? '✓' : '✗' }}
                      </span>
                    </td>
                    <td class="px-4 py-3 text-center">
                      <span [class]="loc.sellsCodes ? 'text-green-500' : 'text-gray-300 dark:text-gray-600'" class="text-xl">
                        {{ loc.sellsCodes ? '✓' : '✗' }}
                      </span>
                    </td>
                    <td class="px-4 py-3 text-gray-500 dark:text-gray-400 text-xs">{{ loc.notes || '—' }}</td>
                    <td class="px-4 py-3 text-center">
                      <div class="flex items-center justify-center gap-2">
                        <button (click)="openEdit(loc)" class="text-[#183764] dark:text-blue-400 hover:text-[#122a4e] font-medium text-xs px-2 py-1 rounded hover:bg-blue-50 dark:hover:bg-blue-900/20 transition-colors">تعديل</button>
                        <button (click)="remove(loc.id)" class="text-red-500 hover:text-red-700 font-medium text-xs px-2 py-1 rounded hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors">حذف</button>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      }
    </div>
  `
})
export class BooksCodesComponent implements OnInit {
  locations = signal<any[]>([]);
  loading = signal(true);
  showForm = signal(false);
  saving = signal(false);
  editId = signal<number | null>(null);

  form: any = this.emptyForm();

  constructor(private api: ApiService, private toastr: ToastrService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.api.getBooksCodesLocations().subscribe({
      next: data => { this.locations.set(data); this.loading.set(false); },
      error: () => { this.toastr.error('فشل تحميل البيانات'); this.loading.set(false); }
    });
  }

  emptyForm() {
    return { name: '', type: 'CENTER', address: '', phone: '', sellsBooks: false, sellsCodes: false, notes: '' };
  }

  openAdd() { this.form = this.emptyForm(); this.editId.set(null); this.showForm.set(true); }

  openEdit(loc: any) {
    this.form = { name: loc.name, type: loc.type, address: loc.address || '', phone: loc.phone || '', sellsBooks: !!loc.sellsBooks, sellsCodes: !!loc.sellsCodes, notes: loc.notes || '' };
    this.editId.set(loc.id);
    this.showForm.set(true);
  }

  cancel() { this.showForm.set(false); this.editId.set(null); }

  save() {
    if (!this.form.name?.trim()) { this.toastr.warning('الاسم مطلوب'); return; }
    this.saving.set(true);
    const id = this.editId();
    const call = id ? this.api.updateBooksCodesLocation(id, this.form) : this.api.addBooksCodesLocation(this.form);
    call.subscribe({
      next: () => {
        this.toastr.success(id ? 'تم التعديل بنجاح' : 'تم الإضافة بنجاح');
        this.saving.set(false);
        this.showForm.set(false);
        this.editId.set(null);
        this.load();
      },
      error: () => { this.toastr.error('فشل الحفظ'); this.saving.set(false); }
    });
  }

  remove(id: number) {
    if (!confirm('هل تريد حذف هذا المكان؟')) return;
    this.api.deleteBooksCodesLocation(id).subscribe({
      next: () => { this.toastr.success('تم الحذف'); this.load(); },
      error: () => this.toastr.error('فشل الحذف')
    });
  }

  typeLabel(type: string) {
    return type === 'CENTER' ? 'سنتر' : type === 'LIBRARY' ? 'مكتبة' : 'أخرى';
  }

  typeClass(type: string) {
    return type === 'CENTER' ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400'
      : type === 'LIBRARY' ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
      : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300';
  }
}
