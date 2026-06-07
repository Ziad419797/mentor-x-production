import { Component, OnInit, OnDestroy, signal, computed, NgZone, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { Material } from '../../models/models';

declare const YT: any;

@Component({
  selector: 'app-video-player',
  standalone: true,
  imports: [CommonModule, FormsModule],
  styles: [`
    * { box-sizing: border-box; }
    :host { display: block; }
    .root {
      position: fixed; inset: 0; background: #000;
      display: flex; flex-direction: column;
      user-select: none; font-family: 'Cairo','Segoe UI',sans-serif;
      direction: ltr;
    }
    .top-bar {
      position: absolute; top: 0; left: 0; right: 0; z-index: 40;
      padding: 12px 18px; display: flex; align-items: center; justify-content: space-between;
      background: #0f0f0f;
      transition: opacity .3s; direction: rtl;
    }
    .root.hide-ui .top-bar { opacity: 0; pointer-events: none; }
    .video-area { flex: 1; position: relative; background: #000; overflow: hidden; cursor: none; }
    .root.show-cursor .video-area { cursor: default; }
    #yt-player { position: absolute; inset: 0; width: 100%; height: 100%; }
    #yt-player iframe { position: absolute !important; inset: 0 !important; width: 100% !important; height: 100% !important; border: none !important; pointer-events: none !important; }
    .click-guard { position: absolute; inset: 0; z-index: 5; pointer-events: all; }
    .wm {
      position: absolute; z-index: 20; pointer-events: none;
      font-size: 12px; font-weight: 700; padding: 3px 10px; border-radius: 5px;
      transition: top 7s ease-in-out, left 7s ease-in-out, right 7s ease-in-out, opacity 4s ease-in-out;
      white-space: nowrap;
    }
    .controls {
      position: absolute; bottom: 0; left: 0; right: 0; z-index: 30; padding: 0 16px 12px;
      background: #0f0f0f;
      transition: opacity .3s;
    }
    .root.hide-ui .controls { opacity: 0; pointer-events: none; }
    .prog-track {
      position: relative; width: 100%; height: 4px;
      background: rgba(255,255,255,.25); border-radius: 2px;
      cursor: pointer; margin-bottom: 10px; transition: height .15s;
    }
    .prog-track:hover { height: 6px; }
    .prog-buf { position: absolute; top: 0; left: 0; height: 100%; background: rgba(255,255,255,.35); border-radius: 2px; pointer-events: none; }
    .prog-fill { position: absolute; top: 0; left: 0; height: 100%; background: #f97316; border-radius: 2px; pointer-events: none; }
    .prog-thumb {
      position: absolute; top: 50%; transform: translate(-50%,-50%);
      width: 12px; height: 12px; border-radius: 50%; background: #f97316;
      pointer-events: none; opacity: 0; transition: opacity .15s;
    }
    .prog-track:hover .prog-thumb { opacity: 1; }
    .ctrl-row { display: flex; align-items: center; gap: 8px; direction: ltr; }
    .ctrl-btn {
      background: none; border: none; cursor: pointer; color: #fff;
      display: flex; align-items: center; justify-content: center;
      padding: 4px; border-radius: 4px; transition: background .15s; flex-shrink: 0;
    }
    .ctrl-btn:hover { background: rgba(255,255,255,.15); }
    .ctrl-btn .material-icons-round { font-size: 20px; }
    .time-txt { color: rgba(255,255,255,.85); font-size: 12px; font-weight: 700; white-space: nowrap; }
    .vol-wrap { display: flex; align-items: center; gap: 4px; }
    .vol-slider {
      -webkit-appearance: none; appearance: none; width: 70px; height: 4px;
      background: rgba(255,255,255,.3); border-radius: 2px; cursor: pointer; outline: none;
    }
    .vol-slider::-webkit-slider-thumb { -webkit-appearance: none; width: 12px; height: 12px; border-radius: 50%; background: #fff; cursor: pointer; }
    .ctrl-select {
      background: rgba(255,255,255,.1); border: 1px solid rgba(255,255,255,.2);
      color: #fff; font-size: 11px; font-weight: 700; border-radius: 6px;
      padding: 3px 6px; cursor: pointer; outline: none; font-family: inherit;
    }
    .ctrl-select option { background: #1e293b; color: #fff; }
    .sp { flex: 1; }
    .spinner { position: absolute; inset: 0; z-index: 25; display: flex; align-items: center; justify-content: center; }
  `],
  template: `
    <div class="root"
         [class.hide-ui]="hideUi()"
         [class.show-cursor]="!hideUi()"
         (contextmenu)="$event.preventDefault()"
         (mousemove)="onMouseMove()"
         (keydown)="onKey($event)"
         tabindex="0"
         #rootEl>

      <!-- Top bar -->
      <div class="top-bar">
        <button (click)="goBack()"
                style="display:flex;align-items:center;gap:6px;padding:6px 12px;border-radius:8px;background:rgba(255,255,255,.12);border:none;color:#fff;font-size:13px;font-weight:700;cursor:pointer">
          <span class="material-icons-round" style="font-size:18px">arrow_forward</span>
          رجوع
        </button>
        <span style="color:#fff;font-weight:700;font-size:14px;flex:1;text-align:center;padding:0 16px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
          {{ material()?.fileName || '' }}
        </span>
        <div style="display:flex;align-items:center;gap:8px;padding:6px 12px;border-radius:8px;background:rgba(255,255,255,.12);color:#fff;font-size:12px;font-weight:700;flex-shrink:0;">
          <span class="material-icons-round" style="font-size:16px;color:#f97316">account_circle</span>
          {{ userName() }}
          <span style="color:rgba(255,255,255,.45)">#{{ userId() }}</span>
        </div>
      </div>

      <!-- Video area -->
      <div class="video-area" (click)="togglePlay()">
        <div id="yt-player"></div>
        <div class="click-guard" (click)="togglePlay()" (contextmenu)="$event.preventDefault()"></div>
        <div *ngIf="buffering()" class="spinner">
          <span class="material-icons-round animate-spin" style="font-size:40px;color:rgba(255,255,255,.5)">refresh</span>
        </div>
        <div *ngIf="showPlayFlash()" class="spinner" style="pointer-events:none">
          <div style="background:rgba(0,0,0,.5);border-radius:50%;padding:16px;display:flex">
            <span class="material-icons-round" style="font-size:48px;color:#fff">{{ isPlaying() ? 'play_arrow' : 'pause' }}</span>
          </div>
        </div>
        <div class="wm" [style.top]="wm1Top()" [style.left]="wm1Left()" [style.color]="wmColor1()" [style.background]="wmBg()">{{ watermarkText() }}</div>
        <div class="wm" [style.top]="wm2Top()" [style.right]="wm2Right()" [style.color]="wmColor2()" [style.background]="wmBg()">{{ watermarkText() }}</div>
      </div>

      <!-- Controls -->
      <div class="controls" (click)="$event.stopPropagation()">
        <div class="prog-track" (click)="seek($event)">
          <div class="prog-buf"  [style.width]="(bufferedPct() * 100) + '%'"></div>
          <div class="prog-fill" [style.width]="(progressPct() * 100) + '%'"></div>
          <div class="prog-thumb" [style.left]="(progressPct() * 100) + '%'"></div>
        </div>
        <div class="ctrl-row">
          <button class="ctrl-btn" (click)="togglePlay()">
            <span class="material-icons-round">{{ isPlaying() ? 'pause' : 'play_arrow' }}</span>
          </button>
          <button class="ctrl-btn" (click)="skipBy(-10)" title="-10">
            <span class="material-icons-round">replay_10</span>
          </button>
          <button class="ctrl-btn" (click)="skipBy(10)" title="+10">
            <span class="material-icons-round">forward_10</span>
          </button>
          <div class="vol-wrap">
            <button class="ctrl-btn" (click)="toggleMute()">
              <span class="material-icons-round">{{ muted() ? 'volume_off' : volume() < 30 ? 'volume_down' : 'volume_up' }}</span>
            </button>
            <input type="range" class="vol-slider" min="0" max="100" [value]="volume()" (input)="setVolume($event)">
          </div>
          <span class="time-txt">{{ formatTime(currentTime()) }} / {{ formatTime(duration()) }}</span>
          <div class="sp"></div>
          <select class="ctrl-select" [value]="speed()" (change)="setSpeed($event)">
            <option value="0.5">0.5x</option>
            <option value="0.75">0.75x</option>
            <option value="1">عادي</option>
            <option value="1.25">1.25x</option>
            <option value="1.5">1.5x</option>
            <option value="2">2x</option>
          </select>
          <select class="ctrl-select" [value]="quality()" (change)="setQuality($event)">
            <option value="auto">تلقائي</option>
            <option value="hd1080">1080p</option>
            <option value="hd720">720p</option>
            <option value="large">480p</option>
            <option value="medium">360p</option>
            <option value="small">240p</option>
          </select>
          <button class="ctrl-btn" (click)="toggleFullscreen()">
            <span class="material-icons-round">{{ isFullscreen() ? 'fullscreen_exit' : 'fullscreen' }}</span>
          </button>
        </div>
      </div>

    </div>
  `
})
export class VideoPlayerComponent implements OnInit, OnDestroy {
  material    = signal<Material | null>(null);
  isPlaying   = signal(false);
  buffering   = signal(true);
  currentTime = signal(0);
  duration    = signal(0);
  bufferedPct = signal(0);
  progressPct = signal(0);
  volume      = signal(80);
  muted       = signal(false);
  speed       = signal('1');
  quality     = signal('auto');
  isFullscreen = signal(false);
  hideUi      = signal(false);
  showPlayFlash = signal(false);

  wm1Top  = signal('18%'); wm1Left  = signal('12%');
  wm2Top  = signal('62%'); wm2Right = signal('10%');
  wmColor1 = signal('rgba(255,255,255,0.35)');
  wmColor2 = signal('rgba(255,200,80,0.35)');
  wmBg     = signal('rgba(0,0,0,0.22)');

  userName = computed(() => this.auth.currentUser()?.name ?? 'مستخدم');
  userId   = computed(() => (this.auth.currentUser() as any)?.id ?? '—');
  watermarkText = computed(() => `${this.userName()} · #${this.userId()}`);

  private player: any = null;
  private progressTimer: any;
  private wmTimer: any;
  private hideTimer: any;
  private flashTimer: any;

  constructor(
    private route: ActivatedRoute,
    private api:   ApiService,
    private auth:  AuthService,
    private zone:  NgZone,
    private elRef: ElementRef
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('materialId'));
    this.api.getMaterialById(id).subscribe({
      next: m => { this.material.set(m); this.initYouTubeApi(m); },
      error: () => this.buffering.set(false)
    });
    this.startWatermarkDrift();
  }

  ngOnDestroy() {
    clearInterval(this.progressTimer);
    clearInterval(this.wmTimer);
    clearTimeout(this.hideTimer);
    clearTimeout(this.flashTimer);
    if (this.player?.destroy) this.player.destroy();
    delete (window as any).onYouTubeIframeAPIReady;
  }

  private initYouTubeApi(m: Material) {
    const vid = this.extractVideoId(m.fileUrl ?? m.youtubeVideoId ?? '');
    if (!vid) { this.buffering.set(false); return; }

    const createPlayer = () => {
      this.player = new YT.Player('yt-player', {
        videoId: vid,
        width: '100%',
        height: '100%',
        playerVars: {
          controls: 0, rel: 0, modestbranding: 1, showinfo: 0,
          iv_load_policy: 3, disablekb: 1, autoplay: 1,
          origin: window.location.origin, enablejsapi: 1, fs: 0,
        },
        events: {
          onReady: (e: any) => this.zone.run(() => {
            e.target.setVolume(this.volume());
            this.buffering.set(false);
            this.startProgressPolling();
          }),
          onStateChange: (e: any) => this.zone.run(() => {
            const S = YT.PlayerState;
            if (e.data === S.PLAYING)   { this.isPlaying.set(true);  this.buffering.set(false); }
            if (e.data === S.PAUSED)    { this.isPlaying.set(false); }
            if (e.data === S.BUFFERING) { this.buffering.set(true); }
            if (e.data === S.ENDED)     { this.isPlaying.set(false); }
          }),
        }
      });
    };

    if (typeof YT !== 'undefined' && YT.Player) {
      createPlayer();
    } else {
      (window as any).onYouTubeIframeAPIReady = createPlayer;
      if (!document.querySelector('script[src*="youtube.com/iframe_api"]')) {
        const s = document.createElement('script');
        s.src = 'https://www.youtube.com/iframe_api';
        document.head.appendChild(s);
      }
    }
  }

  private startProgressPolling() {
    this.progressTimer = setInterval(() => {
      if (!this.player) return;
      this.zone.run(() => {
        const cur = this.player.getCurrentTime?.() ?? 0;
        const dur = this.player.getDuration?.()    ?? 0;
        const buf = this.player.getVideoLoadedFraction?.() ?? 0;
        this.currentTime.set(cur);
        this.duration.set(dur);
        this.bufferedPct.set(buf);
        this.progressPct.set(dur > 0 ? cur / dur : 0);
      });
    }, 300);
  }

  togglePlay() {
    if (!this.player) return;
    if (this.isPlaying()) this.player.pauseVideo();
    else                  this.player.playVideo();
    this.flashPlay();
  }

  skipBy(sec: number) {
    if (!this.player) return;
    this.player.seekTo(Math.max(0, (this.player.getCurrentTime() ?? 0) + sec), true);
  }

  seek(e: MouseEvent) {
    if (!this.player || !this.duration()) return;
    const rect  = (e.currentTarget as HTMLElement).getBoundingClientRect();
    const ratio = (e.clientX - rect.left) / rect.width;
    this.player.seekTo(ratio * this.duration(), true);
  }

  toggleMute() {
    if (!this.player) return;
    if (this.muted()) { this.player.unMute(); this.muted.set(false); }
    else              { this.player.mute();   this.muted.set(true);  }
  }

  setVolume(e: Event) {
    const v = Number((e.target as HTMLInputElement).value);
    this.volume.set(v);
    this.player?.setVolume(v);
    if (v === 0) this.muted.set(true);
    else { this.muted.set(false); this.player?.unMute(); }
  }

  setSpeed(e: Event) {
    const v = (e.target as HTMLSelectElement).value;
    this.speed.set(v);
    this.player?.setPlaybackRate(Number(v));
  }

  setQuality(e: Event) {
    const v = (e.target as HTMLSelectElement).value;
    this.quality.set(v);
    if (v === 'auto') this.player?.setPlaybackQualityRange?.('small', 'hd2160');
    else              this.player?.setPlaybackQuality(v);
  }

  toggleFullscreen() {
    const el = this.elRef.nativeElement.querySelector('.root') as HTMLElement;
    if (!document.fullscreenElement) {
      el.requestFullscreen().then(() => this.isFullscreen.set(true)).catch(() => {});
    } else {
      document.exitFullscreen().then(() => this.isFullscreen.set(false)).catch(() => {});
    }
  }

  onMouseMove() {
    this.hideUi.set(false);
    clearTimeout(this.hideTimer);
    this.hideTimer = setTimeout(() => {
      if (this.isPlaying()) this.zone.run(() => this.hideUi.set(true));
    }, 3000);
  }

  onKey(e: KeyboardEvent) {
    if (e.code === 'Space')      { e.preventDefault(); this.togglePlay(); }
    if (e.code === 'ArrowRight') this.skipBy(10);
    if (e.code === 'ArrowLeft')  this.skipBy(-10);
    if (e.code === 'ArrowUp')    { const v = Math.min(100, this.volume() + 10); this.volume.set(v); this.player?.setVolume(v); }
    if (e.code === 'ArrowDown')  { const v = Math.max(0,   this.volume() - 10); this.volume.set(v); this.player?.setVolume(v); }
    if (e.code === 'KeyF')       this.toggleFullscreen();
    if (e.code === 'KeyM')       this.toggleMute();
  }

  goBack() { if (window.opener || window.history.length <= 1) window.close(); else window.history.back(); }

  private flashPlay() {
    this.showPlayFlash.set(true);
    clearTimeout(this.flashTimer);
    this.flashTimer = setTimeout(() => this.zone.run(() => this.showPlayFlash.set(false)), 600);
  }

  private startWatermarkDrift() {
    const rand = (a: number, b: number) => Math.floor(Math.random() * (b - a + 1) + a);
    const ops  = ['0.28', '0.34', '0.40', '0.32'];
    const move = () => {
      this.wm1Top.set(`${rand(8, 45)}%`);   this.wm1Left.set(`${rand(3, 50)}%`);
      this.wm2Top.set(`${rand(52, 85)}%`);  this.wm2Right.set(`${rand(3, 50)}%`);
      this.wmColor1.set(`rgba(255,255,255,${ops[rand(0, ops.length-1)]})`);
      this.wmColor2.set(`rgba(255,200,80,${ops[rand(0, ops.length-1)]})`);
      this.wmBg.set(`rgba(0,0,0,${rand(18, 30) / 100})`);
    };
    move();
    this.wmTimer = setInterval(move, 7000);
  }

  formatTime(sec: number): string {
    if (!sec || isNaN(sec)) return '0:00';
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    const s = Math.floor(sec % 60);
    if (h > 0) return `${h}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;
    return `${m}:${String(s).padStart(2,'0')}`;
  }

  private extractVideoId(url: string): string {
    if (!url) return '';
    const m1 = url.match(/\/embed\/([^?&"]+)/);
    const m2 = url.match(/[?&]v=([^&]+)/);
    const m3 = url.match(/youtu\.be\/([^?]+)/);
    if (m1) return m1[1];
    if (m2) return m2[1];
    if (m3) return m3[1];
    if (/^[a-zA-Z0-9_-]{11}$/.test(url.trim())) return url.trim();
    return '';
  }
}
