import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Level } from '../../models/models';

interface Channel {
  id?: number;
  groupName: string;
  grade: string | null;
  label: string;
  type: 'WHATSAPP' | 'TELEGRAM' | 'LINK';
  value: string;
  displayOrder: number;
}

@Component({
  selector: 'app-support-channels',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div class="p-6 max-w-3xl mx-auto space-y-6">

  <!-- Header -->
  <div class="flex items-center justify-between">
    <div>
      <h1 class="text-2xl font-bold text-white">قنوات الدعم</h1>
      <p class="text-slate-400 text-sm mt-0.5">أضف قنوات تواصل لطلابك مصنفة حسب الصف الدراسي</p>
    </div>
    <button (click)="openAdd()" class="btn-primary flex items-center gap-2">
      <span class="material-icons-round text-sm">add</span>
      إضافة قناة
    </button>
  </div>

  <!-- Loading -->
  <div *ngIf="loading()" class="flex justify-center py-20">
    <span class="material-icons-round animate-spin text-indigo-400" style="font-size:40px">refresh</span>
  </div>

  <!-- Empty -->
  <div *ngIf="!loading() && channels().length === 0" class="edu-card text-center py-14">
    <span class="material-icons-round text-slate-600 mb-3" style="font-size:56px">support_agent</span>
    <p class="text-slate-300 font-bold">لا توجد قنوات دعم بعد</p>
    <p class="text-slate-500 text-sm mt-1">أضف قنوات تواصل لطلابك</p>
  </div>

  <!-- Groups -->
  <div *ngIf="!loading() && channels().length > 0" class="space-y-5">
    <div *ngFor="let group of groups()" class="edu-card">
      <h2 class="text-white font-bold text-base mb-4 flex items-center gap-2">
        <span class="material-icons-round text-indigo-400 text-lg">folder</span>
        {{ group.name }}
      </h2>
      <div class="space-y-2">
        <div *ngFor="let ch of group.channels"
             class="flex items-center gap-3 bg-slate-800/50 rounded-xl p-3">
          <!-- Icon -->
          <div class="w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0"
               [ngClass]="ch.type==='WHATSAPP'?'bg-green-500/20':ch.type==='TELEGRAM'?'bg-blue-500/20':'bg-indigo-500/20'">
            <span class="material-icons-round text-sm"
                  [ngClass]="ch.type==='WHATSAPP'?'text-green-400':ch.type==='TELEGRAM'?'text-blue-400':'text-indigo-400'">
              {{ ch.type==='WHATSAPP'?'chat':ch.type==='TELEGRAM'?'send':'link' }}
            </span>
          </div>
          <!-- Info -->
          <div class="flex-1 min-w-0">
            <p class="text-white text-sm font-semibold truncate">{{ ch.label }}</p>
            <p class="text-slate-400 text-xs truncate">{{ ch.value }}</p>
          </div>
          <!-- Grade badge -->
          <span class="text-[11px] px-2 py-0.5 rounded-full flex-shrink-0"
                [ngClass]="ch.grade ? 'bg-indigo-500/20 text-indigo-300' : 'bg-slate-700 text-slate-400'">
            {{ ch.grade || 'كل الصفوف' }}
          </span>
          <!-- Actions -->
          <button (click)="openEdit(ch)"
                  class="w-8 h-8 rounded-lg bg-indigo-500/10 text-indigo-400 hover:bg-indigo-500/30 transition-colors flex items-center justify-center flex-shrink-0">
            <span class="material-icons-round text-sm">edit</span>
          </button>
          <button (click)="delete(ch)"
                  class="w-8 h-8 rounded-lg bg-red-500/10 text-red-400 hover:bg-red-500/30 transition-colors flex items-center justify-center flex-shrink-0">
            <span class="material-icons-round text-sm">delete</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</div>

<!-- ═══ Modal ═══ -->
<div *ngIf="showModal()" class="modal-overlay" (click)="closeModal()">
  <div class="modal-box max-w-md w-full" (click)="$event.stopPropagation()">
    <div class="modal-header">
      <h3 class="text-white font-bold">{{ editing()?.id ? 'تعديل القناة' : 'إضافة قناة جديدة' }}</h3>
      <button (click)="closeModal()" class="modal-close">
        <span class="material-icons-round">close</span>
      </button>
    </div>
    <div class="modal-body">

      <!-- Group name -->
      <div>
        <label class="field-label">اسم المجموعة <span class="text-slate-500">(مثال: دعم علمي، دعم أكاديمي)</span></label>
        <input [(ngModel)]="form.groupName" class="field-input" placeholder="دعم علمي">
      </div>

      <!-- Channel label -->
      <div>
        <label class="field-label">اسم القناة</label>
        <input [(ngModel)]="form.label" class="field-input" placeholder="مثال: واتساب المجموعة">
      </div>

      <!-- Type -->
      <div>
        <label class="field-label">النوع</label>
        <div class="grid grid-cols-3 gap-2">
          <button *ngFor="let t of types" (click)="form.type = t.value"
                  class="flex flex-col items-center gap-1 p-3 rounded-xl border transition-all"
                  [ngClass]="form.type===t.value
                    ? 'border-indigo-500 bg-indigo-500/20 text-white'
                    : 'border-slate-700 bg-slate-800/50 text-slate-400 hover:border-slate-500'">
            <span class="material-icons-round text-lg">{{ t.icon }}</span>
            <span class="text-xs font-bold">{{ t.label }}</span>
          </button>
        </div>
      </div>

      <!-- Value -->
      <div>
        <label class="field-label">
          {{ form.type==='LINK' ? 'الرابط' : form.type==='WHATSAPP' ? 'رقم الواتساب (مثال: 201XXXXXXXXX)' : 'يوزرنيم التليجرام' }}
        </label>
        <input [(ngModel)]="form.value" class="field-input"
               [placeholder]="form.type==='LINK'?'https://...':form.type==='WHATSAPP'?'201XXXXXXXXX':'@username'">
      </div>

      <!-- Grade dropdown -->
      <div>
        <label class="field-label">الصف الدراسي</label>
        <select [(ngModel)]="form.grade" class="field-input">
          <option [ngValue]="null">جميع الصفوف الدراسية</option>
          <option *ngFor="let l of levels()" [value]="l.name">{{ l.name }}</option>
        </select>
      </div>

      <!-- Order -->
      <div>
        <label class="field-label">الترتيب</label>
        <input type="number" [(ngModel)]="form.displayOrder" class="field-input" placeholder="0">
      </div>

    </div>
    <div class="modal-footer">
      <button (click)="save()" [disabled]="saving() || !form.groupName || !form.label || !form.value"
              class="btn-primary flex items-center gap-2 disabled:opacity-50">
        <span *ngIf="saving()" class="material-icons-round animate-spin text-sm">refresh</span>
        {{ saving() ? 'جاري الحفظ...' : (editing()?.id ? 'حفظ التعديلات' : 'إضافة القناة') }}
      </button>
      <button (click)="closeModal()" class="btn-secondary">إلغاء</button>
    </div>
  </div>
</div>
  `
})
export class SupportChannelsComponent implements OnInit {
  channels  = signal<Channel[]>([]);
  levels    = signal<Level[]>([]);
  loading   = signal(true);
  showModal = signal(false);
  saving    = signal(false);
  editing   = signal<Channel | null>(null);
  form: Channel = this.blank();

  types = [
    { value: 'WHATSAPP' as const, label: 'واتساب',  icon: 'chat' },
    { value: 'TELEGRAM' as const, label: 'تليجرام', icon: 'send' },
    { value: 'LINK'     as const, label: 'رابط',    icon: 'link' },
  ];

  groups = computed(() => {
    const map = new Map<string, Channel[]>();
    for (const ch of this.channels()) {
      const g = ch.groupName || 'عام';
      if (!map.has(g)) map.set(g, []);
      map.get(g)!.push(ch);
    }
    return [...map.entries()].map(([name, channels]) => ({ name, channels }));
  });

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.getLevels().subscribe({ next: l => this.levels.set(l), error: () => {} });
    this.load();
  }

  load() {
    this.loading.set(true);
    this.api.getSupportChannels().subscribe({
      next: (data: any) => { this.channels.set(data || []); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  blank(): Channel { return { groupName: '', grade: null, label: '', type: 'WHATSAPP', value: '', displayOrder: 0 }; }

  openAdd()             { this.editing.set(null); this.form = this.blank(); this.showModal.set(true); }
  openEdit(ch: Channel) { this.editing.set(ch); this.form = { ...ch }; this.showModal.set(true); }
  closeModal()          { this.showModal.set(false); }

  save() {
    this.saving.set(true);
    const payload = { ...this.form, grade: this.form.grade || null };
    const obs = this.editing()?.id
      ? this.api.updateSupportChannel(this.editing()!.id!, payload)
      : this.api.createSupportChannel(payload);
    obs.subscribe({
      next: () => { this.saving.set(false); this.closeModal(); this.load(); },
      error: () => this.saving.set(false)
    });
  }

  delete(ch: Channel) {
    if (!confirm('حذف هذه القناة؟')) return;
    this.api.deleteSupportChannel(ch.id!).subscribe({ next: () => this.load() });
  }
}
