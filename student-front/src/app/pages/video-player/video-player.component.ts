import { Component, OnInit, OnDestroy, signal, NgZone, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { StudentApiService } from '../../services/student-api.service';
import { StudentAuthService } from '../../services/student-auth.service';

declare const YT: any;

@Component({
  selector: 'app-video-player',
  standalone: true,
  imports: [CommonModule],
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
      background: #0f0f0f; transition: opacity .3s; direction: rtl;
    }
    .root.hide-ui .top-bar { opacity: 0; pointer-events: none; }
    .video-area { flex: 1; position: relative; background: #000; overflow: hidden; cursor: none; }
    .root.show-cursor .video-area { cursor: default; }
    #yt-player-s { position: absolute; inset: 0; width: 100%; height: 100%; }
    #yt-player-s iframe { position: absolute !important; inset: 0 !important; width: 100% !important; height: 100% !important; border: none !important; pointer-events: none !important; }
    .click-guard { position: absolute; inset: 0; z-index: 5; pointer-events: all; }
    .wm-float {
      position: absolute; z-index: 20; pointer-events: none;
      font-size: 12px; font-weight: 700; color: rgba(24,55,100,0.55);
      white-space: nowrap; letter-spacing: 2px; font-family: monospace;
      transition: top 10s ease-in-out, left 10s ease-in-out;
    }
    .controls {
      position: absolute; bottom: 0; left: 0; right: 0; z-index: 30; padding: 0 16px 12px;
      background: #0f0f0f; transition: opacity .3s;
    }
    .root.hide-ui .controls { opacity: 0; pointer-events: none; }
    .prog-track {
      position: relative; width: 100%; height: 4px;
      background: rgba(255,255,255,.25); border-radius: 2px;
      cursor: pointer; margin-bottom: 10px; transition: height .15s;
    }
    .prog-track:hover { height: 6px; }
    .prog-fill { position: absolute; top: 0; left: 0; height: 100%; background: #183764; border-radius: 2px; pointer-events: none; }
    .prog-thumb {
      position: absolute; top: 50%; transform: translate(-50%,-50%);
      width: 12px; height: 12px; border-radius: 50%; background: #183764;
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
          {{ material()?.fileName || material()?.title || '' }}
        </span>
        <div style="display:flex;align-items:center;gap:8px;padding:6px 12px;border-radius:8px;background:rgba(255,255,255,.12);color:#fff;font-size:12px;font-weight:700;font-family:monospace;flex-shrink:0;letter-spacing:2px;">
          <span class="material-icons-round" style="font-size:16px;color:#4BBBA0">badge</span>
          {{ studentCode }}
        </div>
      </div>

      <!-- Video area -->
      <div class="video-area" (click)="togglePlay()">
        <div id="yt-player-s"></div>
        <div class="click-guard" (click)="togglePlay()" (contextmenu)="$event.preventDefault()"></div>
        <div *ngIf="buffering()" class="spinner">
          <span class="material-icons-round animate-spin" style="font-size:40px;color:rgba(255,255,255,.5)">refresh</span>
        </div>
        <!-- Floating watermarks -->
        <span class="wm-float" [style.top]="wm[0].y" [style.left]="wm[0].x">{{ studentCode }}</span>
        <span class="wm-float" [style.top]="wm[1].y" [style.left]="wm[1].x">{{ studentCode }}</span>
        <span class="wm-float" [style.top]="wm[2].y" [style.left]="wm[2].x">{{ studentCode }}</span>
      </div>

      <!-- Controls -->
      <div class="controls" (click)="$event.stopPropagation()">
        <div class="prog-track" (click)="seek($event)">
          <div class="prog-fill" [style.width]="(progressPct() * 100) + '%'"></div>
          <div class="prog-thumb" [style.left]="(progressPct() * 100) + '%'"></div>
        </div>
        <div class="ctrl-row">
          <button class="ctrl-btn" (click)="togglePlay()">
            <span class="material-icons-round">{{ isPlaying() ? 'pause' : 'play_arrow' }}</span>
          </button>
          <button class="ctrl-btn" (click)="skipBy(-10)">
            <span class="material-icons-round">replay_10</span>
          </button>
          <button class="ctrl-btn" (click)="skipBy(10)">
            <span class="material-icons-round">forward_10</span>
          </button>
          <div class="vol-wrap">
            <button class="ctrl-btn" (click)="toggleMute()">
              <span class="material-icons-round">{{ muted() ? 'volume_off' : 'volume_up' }}</span>
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
          <button class="ctrl-btn" (click)="toggleFullscreen()">
            <span class="material-icons-round">{{ isFullscreen() ? 'fullscreen_exit' : 'fullscreen' }}</span>
          </button>
        </div>
      </div>

    </div>
  `
})
export class VideoPlayerComponent implements OnInit, OnDestroy {
  material    = signal<any>(null);
  isPlaying   = signal(false);
  buffering   = signal(true);
  currentTime = signal(0);
  duration    = signal(0);
  progressPct = signal(0);
  volume      = signal(80);
  muted       = signal(false);
  speed       = signal('1');
  isFullscreen = signal(false);
  hideUi      = signal(false);

  studentCode = localStorage.getItem('s_studentCode') || '';
  wm = [{x:'10%',y:'10%'},{x:'50%',y:'50%'},{x:'30%',y:'75%'}];
  private wmTimer: any;

  private player: any = null;
  private progressTimer: any;
  private hideTimer: any;

  constructor(
    private route: ActivatedRoute,
    private api: StudentApiService,
    private auth: StudentAuthService,
    private zone: NgZone,
    private elRef: ElementRef
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getMaterialById(id).subscribe({
      next: m => { this.material.set(m); this.initYouTubeApi(m); },
      error: () => this.buffering.set(false)
    });
    // Load student code for watermark (refresh from API in case localStorage is stale)
    this.api.getMe().subscribe({
      next: (me: any) => {
        if (me?.studentCode) { this.studentCode = me.studentCode; }
      }
    });
    // Start floating watermark
    this.moveWatermark();
    this.wmTimer = setInterval(() => this.moveWatermark(), 5000);
  }

  private moveWatermark() {
    this.wm = this.wm.map(() => ({
      x: (Math.floor(Math.random() * 75) + 5) + '%',
      y: (Math.floor(Math.random() * 75) + 5) + '%',
    }));
  }

  ngOnDestroy() {
    clearInterval(this.progressTimer);
    clearInterval(this.wmTimer);
    clearTimeout(this.hideTimer);
    if (this.player?.destroy) this.player.destroy();
    delete (window as any).onYouTubeIframeAPIReady;
  }

  private initYouTubeApi(m: any) {
    const vid = this.extractVideoId(m.fileUrl ?? m.youtubeVideoId ?? '');
    if (!vid) { this.buffering.set(false); return; }

    const createPlayer = () => {
      this.player = new YT.Player('yt-player-s', {
        videoId: vid,
        width: '100%', height: '100%',
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
            if (e.data === S.PAUSED)    this.isPlaying.set(false);
            if (e.data === S.BUFFERING) this.buffering.set(true);
            if (e.data === S.ENDED)     this.isPlaying.set(false);
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
        this.currentTime.set(cur);
        this.duration.set(dur);
        this.progressPct.set(dur > 0 ? cur / dur : 0);
      });
    }, 300);
  }

  togglePlay() {
    if (!this.player) return;
    if (this.isPlaying()) this.player.pauseVideo();
    else                  this.player.playVideo();
  }

  skipBy(sec: number) {
    if (!this.player) return;
    this.player.seekTo(Math.max(0, (this.player.getCurrentTime() ?? 0) + sec), true);
  }

  seek(e: MouseEvent) {
    if (!this.player || !this.duration()) return;
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    this.player.seekTo(((e.clientX - rect.left) / rect.width) * this.duration(), true);
  }

  toggleMute() {
    if (this.muted()) { this.player?.unMute(); this.muted.set(false); }
    else              { this.player?.mute();   this.muted.set(true);  }
  }

  setVolume(e: Event) {
    const v = Number((e.target as HTMLInputElement).value);
    this.volume.set(v);
    this.player?.setVolume(v);
    this.muted.set(v === 0);
  }

  setSpeed(e: Event) {
    const v = (e.target as HTMLSelectElement).value;
    this.speed.set(v);
    this.player?.setPlaybackRate(Number(v));
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
    }, 8000);
  }

  onKey(e: KeyboardEvent) {
    if (e.code === 'Space')      { e.preventDefault(); this.togglePlay(); }
    if (e.code === 'ArrowRight') this.skipBy(10);
    if (e.code === 'ArrowLeft')  this.skipBy(-10);
    if (e.code === 'KeyF')       this.toggleFullscreen();
    if (e.code === 'KeyM')       this.toggleMute();
  }

  goBack() { window.history.back(); }

  formatTime(sec: number): string {
    if (!sec || isNaN(sec)) return '0:00';
    const m = Math.floor((sec % 3600) / 60);
    const s = Math.floor(sec % 60);
    return `${m}:${String(s).padStart(2, '0')}`;
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
