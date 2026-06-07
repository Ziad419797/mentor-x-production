import { Component, OnInit, signal, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StudentApiService } from '../../services/student-api.service';

interface Message { role: 'user' | 'assistant'; content: string; }

@Component({
  selector: 'app-ai-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div dir="rtl" class="max-w-2xl mx-auto pb-10 flex flex-col h-[calc(100vh-8rem)]">

  <!-- Header -->
  <div class="flex items-center gap-3 mb-5">
    <div class="w-12 h-12 rounded-2xl bg-[#e8edf7] dark:bg-white/10 flex items-center justify-center">
      <span class="material-symbols-outlined text-[#183764] dark:text-white"
            style="font-size:26px;font-variation-settings:'FILL' 1,'wght' 400,'GRAD' 0,'opsz' 24">smart_toy</span>
    </div>
    <div>
      <h2 class="font-black text-xl text-[#183764] dark:text-white">المساعد الذكي 🤖</h2>
      <p class="text-[#8892a0] text-xs">يمكنك سؤالي عن أي شيء يخص دراستك</p>
    </div>
  </div>

  <!-- Messages -->
  <div #scrollContainer class="flex-1 overflow-y-auto space-y-4 mb-4 pr-1">

    <!-- Welcome msg -->
    <div *ngIf="messages().length === 0"
         class="flex flex-col items-center justify-center h-full text-center py-10 text-[#8892a0]">
      <span class="material-symbols-outlined mb-3"
            style="font-size:48px;opacity:0.3;font-variation-settings:'FILL' 1,'wght' 400,'GRAD' 0,'opsz' 24">smart_toy</span>
      <p class="font-semibold text-[#183764] dark:text-white">أهلاً! أنا مساعدك الذكي</p>
      <p class="text-sm mt-1">اسألني عن أي موضوع دراسي وسأساعدك</p>
    </div>

    <div *ngFor="let msg of messages()"
         class="flex"
         [class.justify-start]="msg.role === 'assistant'"
         [class.justify-end]="msg.role === 'user'">
      <div class="max-w-[80%] rounded-2xl px-4 py-3 text-sm leading-relaxed"
           [class]="msg.role === 'user'
             ? 'bg-[#183764] text-white rounded-bl-sm'
             : 'bg-white dark:bg-[#162033] border border-[#DDE1EA] dark:border-slate-800 text-[#183764] dark:text-white rounded-br-sm'">
        {{ msg.content }}
      </div>
    </div>

    <div *ngIf="thinking()" class="flex justify-start">
      <div class="bg-white dark:bg-[#162033] border border-[#DDE1EA] dark:border-slate-800 rounded-2xl rounded-br-sm px-4 py-3 flex items-center gap-1.5">
        <span class="w-1.5 h-1.5 bg-[#8892a0] rounded-full animate-bounce" style="animation-delay:0ms"></span>
        <span class="w-1.5 h-1.5 bg-[#8892a0] rounded-full animate-bounce" style="animation-delay:150ms"></span>
        <span class="w-1.5 h-1.5 bg-[#8892a0] rounded-full animate-bounce" style="animation-delay:300ms"></span>
      </div>
    </div>
  </div>

  <!-- Input -->
  <div class="flex gap-3 items-end">
    <textarea [(ngModel)]="input"
              (keydown.enter)="onEnter($event)"
              placeholder="اكتب سؤالك هنا..."
              rows="1"
              class="flex-1 resize-none border-2 border-[#DDE1EA] dark:border-slate-700 rounded-2xl px-4 py-3 text-sm bg-white dark:bg-[#162033] text-[#183764] dark:text-white focus:border-[#183764] focus:outline-none transition-colors"
              style="min-height:48px;max-height:120px"></textarea>
    <button (click)="send()"
            [disabled]="!input.trim() || thinking()"
            class="w-12 h-12 rounded-2xl bg-[#183764] text-white flex items-center justify-center hover:opacity-90 transition-all disabled:opacity-40 flex-shrink-0">
      <span class="material-symbols-outlined" style="font-size:20px;font-variation-settings:'FILL' 1,'wght' 400,'GRAD' 0,'opsz' 24">send</span>
    </button>
  </div>
</div>
  `
})
export class AiChatComponent {
  @ViewChild('scrollContainer') scrollContainer!: ElementRef;

  messages = signal<Message[]>([]);
  thinking = signal(false);
  input    = '';

  constructor(private api: StudentApiService) {}

  onEnter(e: Event) {
    const ke = e as KeyboardEvent;
    if (!ke.shiftKey) { ke.preventDefault(); this.send(); }
  }

  send() {
    const text = this.input.trim();
    if (!text || this.thinking()) return;
    this.input = '';
    this.messages.update(m => [...m, { role: 'user', content: text }]);
    this.thinking.set(true);
    this.scrollToBottom();

    this.api.askAi(text, this.messages().slice(-10)).subscribe({
      next: (res: any) => {
        const reply = res?.reply ?? res?.message ?? res?.answer ?? 'عذراً، لم أفهم سؤالك. حاول مرة أخرى.';
        this.messages.update(m => [...m, { role: 'assistant', content: reply }]);
        this.thinking.set(false);
        this.scrollToBottom();
      },
      error: () => {
        this.messages.update(m => [...m, { role: 'assistant', content: 'حدث خطأ. يرجى المحاولة مرة أخرى.' }]);
        this.thinking.set(false);
        this.scrollToBottom();
      }
    });
  }

  private scrollToBottom() {
    setTimeout(() => {
      const el = this.scrollContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    }, 50);
  }
}
