import { Component, HostListener } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet />'
})
export class AppComponent {
  @HostListener('window:wheel', ['$event'])
  onWindowWheel(event: WheelEvent): void {
    const active = document.activeElement as HTMLElement | null;
    if (active && active.tagName === 'INPUT' && (active as HTMLInputElement).type === 'number') {
      active.blur();
    }
  }
}
