import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, TopbarComponent],
  styles: [`
    .sidebar-wrap {
      position: fixed;
      top: 0; right: 0; bottom: 0;
      width: 260px;
      z-index: 50;
      transition: transform 0.3s cubic-bezier(.4,0,.2,1);
    }
    .sidebar-wrap.hidden-sidebar {
      transform: translateX(260px);
    }
    .content-area {
      transition: margin-right 0.3s cubic-bezier(.4,0,.2,1);
    }
  `],
  template: `
    <div class="dashboard-wrapper h-screen bg-slate-950 flex overflow-hidden dir-rtl">

      <!-- Sidebar — always in DOM, slides in/out -->
      <div class="sidebar-wrap" [class.hidden-sidebar]="sidebarCollapsed()">
        <app-sidebar />
      </div>

      <!-- Main Content Area -->
      <div class="content-area flex flex-col flex-1 min-w-0 h-full"
           [style.margin-right]="sidebarCollapsed() ? '0px' : '260px'">

        <app-topbar (menuToggle)="toggleSidebar()" />

        <!-- Scrollable content -->
        <main class="flex-1 overflow-y-auto overflow-x-hidden p-4 md:p-6 lg:p-8 custom-scrollbar">
          <div class="max-w-[1600px] mx-auto">
            <router-outlet />
          </div>
        </main>
      </div>

      <!-- Mobile overlay -->
      <div *ngIf="!sidebarCollapsed()"
           (click)="sidebarCollapsed.set(true)"
           class="lg:hidden fixed inset-0 bg-black/60 backdrop-blur-sm z-40">
      </div>
    </div>
  `
})
export class ShellComponent {
  sidebarCollapsed = signal(false);
  toggleSidebar() { this.sidebarCollapsed.set(!this.sidebarCollapsed()); }
}
