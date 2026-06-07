import {
  Component, Input, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef,
  forwardRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { Subscription, filter } from 'rxjs';

export interface NavTreeNode {
  icon?: string;
  label: string;
  route?: string;
  children?: NavTreeNode[];
  permission?: string;
  teacherOnly?: boolean;
  /** depth level, set automatically */
  _depth?: number;
}

@Component({
  selector: 'app-nav-tree-item',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, forwardRef(() => NavTreeItemComponent)],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="nav-tree-item-wrapper">
      <!-- Branch node (has children) -->
      <button *ngIf="node.children?.length"
              type="button"
              class="nav-branch"
              [class.active-parent]="isAnyChildActive"
              [style.padding-right.px]="16 + depth * 12"
              [style.padding-left.px]="collapsed ? 0 : 8"
              (click)="toggle()">

        <!-- Left accent bar -->
        <span class="accent-bar" [class.accent-bar--active]="isAnyChildActive"></span>

        <!-- Icon -->
        <span *ngIf="node.icon" class="nav-icon material-icons-round">{{ node.icon }}</span>

        <!-- Label & arrow -->
        <span *ngIf="!collapsed" class="nav-label">{{ node.label }}</span>
        <span *ngIf="!collapsed" class="arrow material-icons-round" [class.arrow--open]="isOpen">
          chevron_left
        </span>
      </button>

      <!-- Leaf node (no children) -->
      <a *ngIf="!node.children?.length && node.route"
         [routerLink]="node.route"
         routerLinkActive="nav-leaf--active"
         class="nav-leaf"
         [style.padding-right.px]="16 + depth * 12"
         [style.padding-left.px]="collapsed ? 0 : 8"
         [title]="collapsed ? node.label : ''">

        <span class="accent-bar"></span>
        <span *ngIf="node.icon" class="nav-icon material-icons-round">{{ node.icon }}</span>
        <span *ngIf="!collapsed" class="nav-label">{{ node.label }}</span>
      </a>

      <!-- Children container with slide animation -->
      <div *ngIf="node.children?.length && !collapsed"
           class="children-wrap"
           [class.children-wrap--open]="isOpen">
        <div class="children-inner">
          <!-- Vertical guide line -->
          <span class="guide-line" [style.right.px]="22 + depth * 12"></span>

          <app-nav-tree-item
            *ngFor="let child of node.children"
            [node]="child"
            [depth]="depth + 1"
            [collapsed]="collapsed">
          </app-nav-tree-item>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; }

    /* ── Branch ─────────────────────────────────── */
    .nav-branch {
      display: flex;
      align-items: center;
      gap: 10px;
      width: 100%;
      padding-top: 9px;
      padding-bottom: 9px;
      border-radius: 10px;
      color: #94a3b8;
      background: transparent;
      border: none;
      cursor: pointer;
      position: relative;
      transition: background 0.18s, color 0.18s;
      font-size: 13.5px;
      font-weight: 500;
      text-align: right;
      direction: rtl;
    }
    .nav-branch:hover {
      background: rgba(99, 102, 241, 0.08);
      color: #e2e8f0;
    }
    .nav-branch.active-parent {
      color: #a5b4fc;
    }

    /* ── Leaf ────────────────────────────────────── */
    .nav-leaf {
      display: flex;
      align-items: center;
      gap: 10px;
      width: 100%;
      padding-top: 9px;
      padding-bottom: 9px;
      border-radius: 10px;
      color: #94a3b8;
      text-decoration: none;
      position: relative;
      transition: background 0.18s, color 0.18s;
      font-size: 13.5px;
      font-weight: 500;
      direction: rtl;
    }
    .nav-leaf:hover {
      background: rgba(99, 102, 241, 0.08);
      color: #e2e8f0;
    }
    .nav-leaf--active {
      background: rgba(99, 102, 241, 0.14) !important;
      color: #a5b4fc !important;
      font-weight: 600;
    }
    .nav-leaf--active .accent-bar {
      opacity: 1;
      transform: scaleY(1);
    }
    .nav-leaf--active .nav-icon {
      color: #818cf8;
    }

    /* ── Shared elements ─────────────────────────── */
    .nav-icon {
      font-size: 18px;
      flex-shrink: 0;
      transition: color 0.18s, transform 0.18s;
    }
    .nav-label {
      flex: 1;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    /* Active left accent bar */
    .accent-bar {
      position: absolute;
      left: 6px;
      top: 50%;
      transform: translateY(-50%) scaleY(0);
      width: 3px;
      height: 60%;
      background: linear-gradient(180deg, #6366f1, #8b5cf6);
      border-radius: 99px;
      opacity: 0;
      transition: opacity 0.18s, transform 0.18s;
    }
    .accent-bar--active {
      opacity: 1;
      transform: translateY(-50%) scaleY(1);
    }

    /* Arrow chevron */
    .arrow {
      font-size: 16px;
      flex-shrink: 0;
      margin-left: auto;
      transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
      color: #475569;
    }
    .arrow--open {
      transform: rotate(-90deg);
      color: #818cf8;
    }

    /* ── Children container ──────────────────────── */
    .children-wrap {
      display: grid;
      grid-template-rows: 0fr;
      transition: grid-template-rows 0.28s cubic-bezier(0.4, 0, 0.2, 1);
      overflow: hidden;
    }
    .children-wrap--open {
      grid-template-rows: 1fr;
    }
    .children-inner {
      min-height: 0;
      position: relative;
      padding-top: 2px;
      padding-bottom: 2px;
    }

    /* Vertical guide line */
    .guide-line {
      position: absolute;
      top: 6px;
      bottom: 6px;
      width: 1px;
      background: linear-gradient(180deg, #334155 0%, transparent 100%);
      pointer-events: none;
    }
  `]
})
export class NavTreeItemComponent implements OnInit, OnDestroy {
  @Input() node!: NavTreeNode;
  @Input() depth = 0;
  @Input() collapsed = false;

  isOpen = false;
  isAnyChildActive = false;

  private routerSub!: Subscription;

  constructor(private router: Router, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.checkActiveState();
    this.routerSub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => {
        this.checkActiveState();
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
  }

  toggle(): void {
    this.isOpen = !this.isOpen;
  }

  private checkActiveState(): void {
    const url = this.router.url;
    this.isAnyChildActive = this.hasActiveDescendant(this.node, url);
    if (this.isAnyChildActive) {
      this.isOpen = true;
    }
  }

  private hasActiveDescendant(node: NavTreeNode, url: string): boolean {
    if (node.route && url.startsWith(node.route)) return true;
    return (node.children || []).some(c => this.hasActiveDescendant(c, url));
  }
}
