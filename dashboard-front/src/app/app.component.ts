import { Component, HostListener } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet />`
})
export class AppComponent {

  // ── منع تغيير قيمة حقول الأرقام بالسكرول ──────────────────────
  // المتصفح بشكل افتراضي بيغيّر قيمة <input type="number"> لما المستخدم
  // يعمل سكرول وهو واقف على الحقل بالماوس، وده بيسبب تغيير غير مقصود
  // في القيم (زي السعر أو النقاط). الحل: نشيل الفوكس عن الحقل فور بدء
  // السكرول، عشان السكرول يفضل بيحرك الصفحة عادي من غير ما يغيّر الرقم.
  @HostListener('window:wheel', ['$event'])
  onWindowWheel(event: WheelEvent): void {
    const active = document.activeElement as HTMLElement | null;
    if (active && active.tagName === 'INPUT' && (active as HTMLInputElement).type === 'number') {
      active.blur();
    }
  }
}
