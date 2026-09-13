import { Component, OnInit, AfterViewInit, OnDestroy, ViewChild, ElementRef, NgZone, inject, HostBinding } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { LogoComponent } from '../../shared/components/logo/logo.component';

export interface FloatingWaterIcon {
  src: string;
  alt: string;
  top: string;
  left?: string;
  right?: string;
  size: 'sm' | 'md' | 'lg';
  animClass: string;
  delay: string;
}

export interface FeatureItem {
  icon: SafeHtml;
  title: string;
  description: string;
}

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink, LogoComponent],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.css',
})
export class LandingComponent implements OnInit, AfterViewInit, OnDestroy {
  @HostBinding('class.dark-theme') get isDarkTheme(): boolean {
    return this.isDark;
  }

  private router = inject(Router);
  private ngZone = inject(NgZone);
  private sanitizer = inject(DomSanitizer);

  @ViewChild('waterCanvas', { static: true }) canvasRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('heroSection', { static: true }) heroRef!: ElementRef<HTMLElement>;

  private animationFrameId: number = 0;
  private gl: WebGLRenderingContext | null = null;
  private program: WebGLProgram | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private pointerMoveHandler: (e: PointerEvent) => void = () => { };
  private pointerDownHandler: (e: PointerEvent) => void = () => { };
  private pointerEnterHandler: (e: PointerEvent) => void = () => { };
  private pointerLeaveHandler: (e: PointerEvent) => void = () => { };
  private touchStartHandler: (e: TouchEvent) => void = () => { };
  private touchMoveHandler: (e: TouchEvent) => void = () => { };
  private touchEndHandler: (e: TouchEvent) => void = () => { };

  isDark = false;
  currentYear = new Date().getFullYear();

  /* Floating icons in water */
  floatingIcons: FloatingWaterIcon[] = [
    {
      src: '/icons/customer-support.svg',
      alt: 'Customer Support',
      top: '12%',
      left: '6%',
      size: 'lg',
      animClass: 'float-1',
      delay: '0s',
    },
    {
      src: '/icons/ai-triage.svg',
      alt: 'AI Triage',
      top: '16%',
      right: '7%',
      size: 'lg',
      animClass: 'float-2',
      delay: '1.2s',
    },
    {
      src: '/icons/agent-workspace.svg',
      alt: 'Agent Workspace',
      top: '44%',
      left: '4%',
      size: 'md',
      animClass: 'float-3',
      delay: '2.5s',
    },
    {
      src: '/icons/live-analytics.svg',
      alt: 'Live Analytics',
      top: '42%',
      right: '5%',
      size: 'lg',
      animClass: 'float-4',
      delay: '0.8s',
    },
    {
      src: '/icons/enterprise-security.svg',
      alt: 'Enterprise Security',
      top: '74%',
      left: '7%',
      size: 'md',
      animClass: 'float-5',
      delay: '1.8s',
    },
    {
      src: '/icons/smart-auto-responses.svg',
      alt: 'Smart Responses',
      top: '70%',
      right: '9%',
      size: 'lg',
      animClass: 'float-6',
      delay: '0.5s',
    },
    {
      src: '/icons/ai-assistant.svg',
      alt: 'AI Assistant',
      top: '26%',
      left: '18%',
      size: 'sm',
      animClass: 'float-7',
      delay: '3.0s',
    },
    {
      src: '/icons/project-management.svg',
      alt: 'Project Management',
      top: '24%',
      right: '20%',
      size: 'sm',
      animClass: 'float-8',
      delay: '1.6s',
    },
    {
      src: '/icons/mobile-app.svg',
      alt: 'Mobile & Omnichannel',
      top: '84%',
      left: '19%',
      size: 'sm',
      animClass: 'float-1',
      delay: '2.2s',
    },
    {
      src: '/icons/optimization.svg',
      alt: 'Continuous Optimization',
      top: '82%',
      right: '22%',
      size: 'sm',
      animClass: 'float-3',
      delay: '0.9s',
    },
    {
      src: '/icons/email-support.svg',
      alt: 'Email Support',
      top: '36%',
      left: '12%',
      size: 'sm',
      animClass: 'float-4',
      delay: '1.4s',
    },
    {
      src: '/icons/social-support.svg',
      alt: 'Social Support',
      top: '58%',
      right: '16%',
      size: 'sm',
      animClass: 'float-2',
      delay: '2.8s',
    },
  ];

  /* Animated hero stats */
  stats = [
    { value: '< 2 min', label: 'Avg First Response' },
    { value: '85%', label: 'Faster Resolution' },
    { value: '99.9%', label: 'Platform Reliability' },
    { value: '4.9/5', label: 'Customer Satisfaction' },
  ];

  get features(): FeatureItem[] {
    return [
      {
        icon: this.sanitizer.bypassSecurityTrustHtml(`<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>`),
        title: 'Intelligent Ticket Triage',
        description: 'Automatically classifies, prioritizes, and routes incoming tickets to the right specialists so critical customer issues are handled first.',
      },
      {
        icon: this.sanitizer.bypassSecurityTrustHtml(`<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3Z"/><path d="M19 3v4"/><path d="M21 5h-4"/></svg>`),
        title: 'AI Copilot & Smart Replies',
        description: 'Generates instant conversation summaries, context-aware reply drafts, and recommended solutions to help agents resolve tickets in seconds.',
      },
      {
        icon: this.sanitizer.bypassSecurityTrustHtml(`<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>`),
        title: 'Self-Service Customer Portal',
        description: 'Delivers a transparent, user-friendly portal where customers can submit tickets, track live progress, and message support directly.',
      },
      {
        icon: this.sanitizer.bypassSecurityTrustHtml(`<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>`),
        title: 'SLA Tracking & Timers',
        description: 'Protects response and resolution commitments with active countdowns, automated escalation alerts, and breach prevention rules.',
      },
      {
        icon: this.sanitizer.bypassSecurityTrustHtml(`<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>`),
        title: 'Real-Time Insights & Reports',
        description: 'Visualize team productivity, ticket velocity, peak support hours, and customer satisfaction metrics with actionable dashboards.',
      },
      {
        icon: this.sanitizer.bypassSecurityTrustHtml(`<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`),
        title: 'Role-Based Collaboration',
        description: 'Tailored workspaces for Customers, Support Agents, and Admins with internal notes, ticket reassignment, and status auditing.',
      },
    ];
  }

  ngOnInit(): void {
    const savedTheme = localStorage.getItem('tix-theme');
    this.isDark = savedTheme === 'dark';
    this.applyTheme();
  }

  toggleTheme(): void {
    this.isDark = !this.isDark;
    this.applyTheme();
    localStorage.setItem('tix-theme', this.isDark ? 'dark' : 'light');
  }

  private applyTheme(): void {
    document.documentElement.setAttribute('data-theme', this.isDark ? 'dark' : 'light');
    if (this.isDark) {
      document.body.classList.add('dark-theme');
    } else {
      document.body.classList.remove('dark-theme');
    }
  }

  ngAfterViewInit(): void {
    this.ngZone.runOutsideAngular(() => {
      this.initWaterEffect();
    });
  }

  ngOnDestroy(): void {
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
    }
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
    }
    const hero = this.heroRef?.nativeElement;
    if (hero) {
      hero.removeEventListener('pointerenter', this.pointerEnterHandler as any);
      hero.removeEventListener('pointermove', this.pointerMoveHandler as any);
      hero.removeEventListener('pointerleave', this.pointerLeaveHandler as any);
      hero.removeEventListener('pointerdown', this.pointerDownHandler as any);
      hero.removeEventListener('touchstart', this.touchStartHandler as any);
      hero.removeEventListener('touchmove', this.touchMoveHandler as any);
      hero.removeEventListener('touchend', this.touchEndHandler as any);
      hero.removeEventListener('touchcancel', this.touchEndHandler as any);
    }
  }

  private initWaterEffect(): void {
    const canvas = this.canvasRef?.nativeElement;
    const hero = this.heroRef?.nativeElement;
    if (!canvas || !hero) return;

    const RIPPLE_INTENSITY = 2.2;
    const RIPPLE_LIFETIME = 2.6;
    const RIPPLE_SPEED = 0.50;
    const HOVER_WAVE_INTERVAL = 0.30;
    const HOVER_DISTANCE_THRESHOLD = 50;
    const MAX_WAVES = 24;

    const gl = canvas.getContext('webgl', {
      alpha: true,
      antialias: true,
      depth: false,
      stencil: false,
      premultipliedAlpha: true,
      preserveDrawingBuffer: false,
      powerPreference: 'high-performance'
    });

    if (!gl) {
      console.error('WebGL is not supported.');
      return;
    }
    this.gl = gl;

    const vertexShaderSource = `
      attribute vec2 aPosition;
      varying vec2 vUv;
      void main() {
        vUv = aPosition * 0.5 + 0.5;
        gl_Position = vec4(aPosition, 0.0, 1.0);
      }
    `;

    const fragmentShaderSource = `
      precision highp float;
      varying vec2 vUv;
      uniform vec2 uResolution;
      uniform float uTime;
      uniform vec3 uWaterColor;
      uniform float uIsDark;
      uniform vec2 uPointer;
      uniform float uPointerActive;
      const int MAX_WAVES = ${MAX_WAVES};
      uniform vec4 uWaves[MAX_WAVES];
      uniform float uWaveCount;

      void main() {
        float aspect = uResolution.x / uResolution.y;
        vec2 p = (vUv - vec2(0.5));
        p.x *= aspect;

        vec2 slope = vec2(0.0);
        float crestSum = 0.0;
        float cursorRepel = 0.0;

        // 1. Direct interactive water repelling meniscus under cursor
        if (uPointerActive > 0.5) {
          vec2 toCursor = p - uPointer;
          float curDist = length(toCursor);
          float curRadius = 0.18;
          if (curDist < curRadius && curDist > 0.001) {
            float nd = curDist / curRadius;
            float repelForce = (1.0 - nd * nd);
            repelForce = repelForce * repelForce;
            vec2 cDir = toCursor / curDist;
            slope += cDir * (repelForce * 1.5);
            cursorRepel = repelForce;
          }
        }

        // 2. Propagating clear water ripple waves
        for (int i = 0; i < MAX_WAVES; i++) {
          if (float(i) >= uWaveCount) break;
          vec4 w = uWaves[i];
          if (w.z < 0.0) continue;
          float age = uTime - w.z;
          if (age <= 0.0 || age > ${RIPPLE_LIFETIME.toFixed(2)}) continue;

          float radius = age * ${RIPPLE_SPEED.toFixed(3)};
          vec2 toPoint = p - w.xy;
          float d = length(toPoint);
          float front = d - radius;

          // Crisp realistic water wave packet (thin primary crest + subtle concentric echoes)
          float primary = exp(-front * front * 4200.0);
          float secondary = sin(front * 140.0) * exp(-front * front * 1400.0) * 0.42;
          float waveH = primary + secondary;

          float ageFade = pow(clamp(1.0 - (age / ${RIPPLE_LIFETIME.toFixed(2)}), 0.0, 1.0), 1.25);
          vec2 screenUV = vUv;
          float edgeFade = smoothstep(0.0, 0.04, min(min(screenUV.x, 1.0 - screenUV.x), min(screenUV.y, 1.0 - screenUV.y)));
          float waveScale = ageFade * edgeFade * w.w;

          if (d > 0.001) {
            vec2 dir = toPoint / d;
            float dFront = -2.0 * 4200.0 * front * primary + 140.0 * cos(front * 140.0) * exp(-front * front * 1400.0) * 0.42;
            slope += dir * dFront * (waveScale * 0.0032);
          }

          if (waveH > 0.0) {
            crestSum += waveH * waveScale;
          }
        }

        float totalActivity = length(slope) + crestSum + cursorRepel;
        if (totalActivity < 0.002) {
          gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
          return;
        }

        // 3. Realistic Water Refraction, Caustic Highlights & Fresnel Glint
        vec3 N = normalize(vec3(-slope * 1.6, 1.0));
        vec3 L = normalize(vec3(-0.35, 0.55, 0.75));
        vec3 V = vec3(0.0, 0.0, 1.0);
        vec3 H = normalize(L + V);
        float NdotH = max(dot(N, H), 0.0);
        float specular = pow(NdotH, 36.0);
        float fresnel = pow(1.0 - max(dot(N, V), 0.0), 2.5);

        // Light mode: Crystal-clear azure water surface
        vec3 lightSheen = vec3(0.72, 0.90, 1.0);
        vec3 lightGlint = vec3(1.0, 1.0, 1.0);
        vec3 lightColor = mix(lightSheen, lightGlint, clamp(specular * 1.4 + cursorRepel * 0.3, 0.0, 1.0));
        float lightAlpha = clamp(crestSum * 0.28 + cursorRepel * 0.20 + specular * 0.42 + fresnel * 0.22, 0.0, 0.45);

        // Dark mode: Deep bioluminescent azure glow
        vec3 darkSheen = vec3(0.35, 0.68, 1.0);
        vec3 darkGlint = vec3(0.85, 0.95, 1.0);
        vec3 darkColor = mix(darkSheen, darkGlint, clamp(specular * 1.3, 0.0, 1.0));
        float darkAlpha = clamp(crestSum * 0.32 + cursorRepel * 0.25 + specular * 0.48 + fresnel * 0.26, 0.0, 0.52);

        vec3 finalColor = mix(lightColor, darkColor, uIsDark);
        float alpha = mix(lightAlpha, darkAlpha, uIsDark);

        gl_FragColor = vec4(finalColor * alpha, alpha);
      }
    `;

    const compileShader = (type: number, source: string) => {
      const shader = gl.createShader(type);
      if (!shader) throw new Error('Could not create shader.');
      gl.shaderSource(shader, source);
      gl.compileShader(shader);
      if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
        const info = gl.getShaderInfoLog(shader);
        gl.deleteShader(shader);
        throw new Error(info || 'Shader compilation failed.');
      }
      return shader;
    };

    const program = gl.createProgram();
    if (!program) throw new Error('Unable to create program.');
    try {
      const vertexShader = compileShader(gl.VERTEX_SHADER, vertexShaderSource);
      const fragmentShader = compileShader(gl.FRAGMENT_SHADER, fragmentShaderSource);
      gl.attachShader(program, vertexShader);
      gl.attachShader(program, fragmentShader);
      gl.linkProgram(program);
      if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
        throw new Error(gl.getProgramInfoLog(program) || 'Program linking failed.');
      }
      gl.deleteShader(vertexShader);
      gl.deleteShader(fragmentShader);
    } catch (e) {
      console.error('Water shader error:', e);
      return;
    }
    this.program = program;
    gl.useProgram(program);

    const vertices = new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]);
    const buffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
    gl.bufferData(gl.ARRAY_BUFFER, vertices, gl.STATIC_DRAW);
    const positionLocation = gl.getAttribLocation(program, 'aPosition');
    gl.enableVertexAttribArray(positionLocation);
    gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 0, 0);

    const uResolution = gl.getUniformLocation(program, 'uResolution') as WebGLUniformLocation;
    const uTime = gl.getUniformLocation(program, 'uTime') as WebGLUniformLocation;
    const uWaterColor = gl.getUniformLocation(program, 'uWaterColor') as WebGLUniformLocation;
    const uIsDark = gl.getUniformLocation(program, 'uIsDark') as WebGLUniformLocation;
    const uWaveCount = gl.getUniformLocation(program, 'uWaveCount') as WebGLUniformLocation;
    const uPointer = gl.getUniformLocation(program, 'uPointer') as WebGLUniformLocation;
    const uPointerActive = gl.getUniformLocation(program, 'uPointerActive') as WebGLUniformLocation;

    const waveLocations: WebGLUniformLocation[] = [];
    for (let i = 0; i < MAX_WAVES; i++) {
      waveLocations.push(gl.getUniformLocation(program, `uWaves[${i}]`) as WebGLUniformLocation);
    }

    const waves: any[] = [
      { x: 0.08, y: 0.06, start: performance.now() / 1000 - 0.4, strength: RIPPLE_INTENSITY * 0.85 },
      { x: -0.12, y: -0.04, start: performance.now() / 1000 - 1.4, strength: RIPPLE_INTENSITY * 0.70 }
    ];
    let width = 1;
    let height = 1;

    const resize = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const rect = hero.getBoundingClientRect();
      width = Math.max(1, Math.floor(rect.width * dpr));
      height = Math.max(1, Math.floor(rect.height * dpr));
      if (canvas.width !== width || canvas.height !== height) {
        canvas.width = width;
        canvas.height = height;
        gl.viewport(0, 0, width, height);
      }
    };

    this.resizeObserver = new ResizeObserver(() => resize());
    this.resizeObserver.observe(hero);
    resize();

    const pointerToSimulation = (clientX: number, clientY: number) => {
      const rect = hero.getBoundingClientRect();
      const screenX = (clientX - rect.left) / rect.width;
      const screenY = (clientY - rect.top) / rect.height;
      let x = screenX - 0.5;
      const y = 0.5 - screenY;
      const aspect = rect.width / rect.height;
      x *= aspect;
      return { x, y };
    };

    const addWave = (clientX: number, clientY: number, strength: number) => {
      const position = pointerToSimulation(clientX, clientY);
      waves.push({ x: position.x, y: position.y, start: performance.now() / 1000, strength });
      while (waves.length > MAX_WAVES) waves.shift();
    };

    // Pod repulsion physics: icons smoothly push away from pointer
    const REPEL_RADIUS = 260;
    const MAX_PUSH = 95;

    interface PodRepelState {
      element: HTMLElement;
      currentX: number;
      currentY: number;
      targetX: number;
      targetY: number;
    }

    const podElements = Array.from(hero.querySelectorAll<HTMLElement>('.pod-repel-wrapper'));
    const podStates: PodRepelState[] = podElements.map(el => ({
      element: el,
      currentX: 0,
      currentY: 0,
      targetX: 0,
      targetY: 0,
    }));

    const pointerState = {
      x: -9999,
      y: -9999,
      simX: 0,
      simY: 0,
      active: false,
    };

    const updatePodsPhysics = () => {
      for (let i = 0; i < podStates.length; i++) {
        const pod = podStates[i];
        if (pointerState.active) {
          const rect = pod.element.getBoundingClientRect();
          const centerX = rect.left + rect.width / 2 - pod.currentX;
          const centerY = rect.top + rect.height / 2 - pod.currentY;

          const dx = centerX - pointerState.x;
          const dy = centerY - pointerState.y;
          const dist = Math.sqrt(dx * dx + dy * dy);

          if (dist < REPEL_RADIUS && dist > 1.0) {
            const factor = Math.pow(1.0 - dist / REPEL_RADIUS, 1.35);
            pod.targetX = (dx / dist) * factor * MAX_PUSH;
            pod.targetY = (dy / dist) * factor * MAX_PUSH;
          } else {
            pod.targetX = 0;
            pod.targetY = 0;
          }
        } else {
          pod.targetX = 0;
          pod.targetY = 0;
        }

        // Fluid spring interpolation (butter-smooth at 60fps)
        pod.currentX += (pod.targetX - pod.currentX) * 0.14;
        pod.currentY += (pod.targetY - pod.currentY) * 0.14;

        const isDisplaced = Math.abs(pod.currentX) > 0.1 || Math.abs(pod.currentY) > 0.1;
        if (isDisplaced) {
          pod.element.style.transform = `translate3d(${pod.currentX.toFixed(2)}px, ${pod.currentY.toFixed(2)}px, 0) scale(${1.0 + Math.min(0.08, Math.hypot(pod.currentX, pod.currentY) / 1000)})`;
          if (Math.hypot(pod.currentX, pod.currentY) > 8) {
            pod.element.classList.add('repelled');
          } else {
            pod.element.classList.remove('repelled');
          }
        } else if (pod.element.style.transform !== '') {
          pod.element.style.transform = '';
          pod.element.classList.remove('repelled');
        }
      }
    };

    let lastPointerX: number | null = null;
    let lastPointerY: number | null = null;
    let lastWaveTime = 0;

    const handlePointerMove = (clientX: number, clientY: number) => {
      const now = performance.now();
      pointerState.x = clientX;
      pointerState.y = clientY;
      const sim = pointerToSimulation(clientX, clientY);
      pointerState.simX = sim.x;
      pointerState.simY = sim.y;
      pointerState.active = true;

      if (lastPointerX === null || lastPointerY === null) {
        lastPointerX = clientX;
        lastPointerY = clientY;
        return;
      }
      const dx = clientX - lastPointerX;
      const dy = clientY - lastPointerY;
      const distance = Math.sqrt(dx * dx + dy * dy);
      if (distance >= HOVER_DISTANCE_THRESHOLD && now - lastWaveTime >= HOVER_WAVE_INTERVAL * 1000) {
        const velocity = Math.min(1.5, distance / 60);
        const strength = RIPPLE_INTENSITY * (0.35 + velocity * 0.20);
        addWave(clientX, clientY, strength);
        lastPointerX = clientX;
        lastPointerY = clientY;
        lastWaveTime = now;
      }
    };

    this.pointerEnterHandler = (event: PointerEvent) => {
      pointerState.x = event.clientX;
      pointerState.y = event.clientY;
      const sim = pointerToSimulation(event.clientX, event.clientY);
      pointerState.simX = sim.x;
      pointerState.simY = sim.y;
      pointerState.active = true;
      lastPointerX = event.clientX;
      lastPointerY = event.clientY;
      lastWaveTime = performance.now();
    };

    this.pointerMoveHandler = (event: PointerEvent) => {
      handlePointerMove(event.clientX, event.clientY);
    };

    this.pointerLeaveHandler = () => {
      lastPointerX = null;
      lastPointerY = null;
      pointerState.active = false;
    };

    this.pointerDownHandler = (event: PointerEvent) => {
      pointerState.x = event.clientX;
      pointerState.y = event.clientY;
      const sim = pointerToSimulation(event.clientX, event.clientY);
      pointerState.simX = sim.x;
      pointerState.simY = sim.y;
      pointerState.active = true;
      lastPointerX = event.clientX;
      lastPointerY = event.clientY;
      lastWaveTime = performance.now();
      addWave(event.clientX, event.clientY, RIPPLE_INTENSITY * 2.2);
    };

    let activeTouch: number | null = null;
    let lastTouchX: number | null = null;
    let lastTouchY: number | null = null;

    this.touchStartHandler = (event: TouchEvent) => {
      const touch = event.touches[0];
      if (!touch) return;
      activeTouch = touch.identifier;
      lastTouchX = touch.clientX;
      lastTouchY = touch.clientY;
      pointerState.x = touch.clientX;
      pointerState.y = touch.clientY;
      const sim = pointerToSimulation(touch.clientX, touch.clientY);
      pointerState.simX = sim.x;
      pointerState.simY = sim.y;
      pointerState.active = true;
      lastPointerX = touch.clientX;
      lastPointerY = touch.clientY;
      lastWaveTime = performance.now();
      addWave(touch.clientX, touch.clientY, RIPPLE_INTENSITY * 2.0);
    };

    this.touchMoveHandler = (event: TouchEvent) => {
      if (activeTouch === null) return;
      let touch: Touch | null = null;
      for (let i = 0; i < event.touches.length; i++) {
        if (event.touches[i].identifier === activeTouch) {
          touch = event.touches[i];
          break;
        }
      }
      if (!touch) return;
      handlePointerMove(touch.clientX, touch.clientY);
    };

    this.touchEndHandler = () => {
      activeTouch = null;
      lastTouchX = null;
      lastTouchY = null;
      pointerState.active = false;
    };

    // Attach pointer and touch events
    hero.addEventListener('pointerenter', this.pointerEnterHandler as any, { passive: true });
    hero.addEventListener('pointermove', this.pointerMoveHandler as any, { passive: true });
    hero.addEventListener('pointerleave', this.pointerLeaveHandler as any, { passive: true });
    hero.addEventListener('pointerdown', this.pointerDownHandler as any, { passive: true });

    hero.addEventListener('touchstart', this.touchStartHandler as any, { passive: true });
    hero.addEventListener('touchmove', this.touchMoveHandler as any, { passive: true });
    hero.addEventListener('touchend', this.touchEndHandler as any, { passive: true });
    hero.addEventListener('touchcancel', this.touchEndHandler as any, { passive: true });

    const startTime = performance.now() / 1000;

    const getWaterColor = () => {
      if (!this.isDark) {
        return [24 / 255, 142 / 255, 230 / 255];
      }
      
      const bg = getComputedStyle(document.documentElement).getPropertyValue('--background').trim();
      if (bg.startsWith('#')) {
        const r = parseInt(bg.substring(1, 3), 16) / 255;
        const g = parseInt(bg.substring(3, 5), 16) / 255;
        const b = parseInt(bg.substring(5, 7), 16) / 255;
        return [r * 0.85 + 0.03, g * 0.85 + 0.08, b * 0.85 + 0.18];
      }
      return [14 / 255, 42 / 255, 82 / 255];
    };

    const render = () => {
      const elapsed = performance.now() / 1000 - startTime;
      for (let i = waves.length - 1; i >= 0; i--) {
        if (elapsed - (waves[i].start - startTime) > RIPPLE_LIFETIME) {
          waves.splice(i, 1);
        }
      }

      // Smoothly update floating icon pod repulsion physics every frame
      updatePodsPhysics();

      gl.uniform2f(uResolution, width, height);
      gl.uniform1f(uTime, elapsed);

      const waterColor = getWaterColor();
      gl.uniform3f(uWaterColor, waterColor[0], waterColor[1], waterColor[2]);
      gl.uniform1f(uIsDark, this.isDark ? 1.0 : 0.0);
      gl.uniform1f(uWaveCount, waves.length);

      // Direct pointer meniscus repelling field
      gl.uniform2f(uPointer, pointerState.simX, pointerState.simY);
      gl.uniform1f(uPointerActive, pointerState.active ? 1.0 : 0.0);

      for (let i = 0; i < MAX_WAVES; i++) {
        if (i < waves.length) {
          const wave = waves[i];
          gl.uniform4f(waveLocations[i], wave.x, wave.y, wave.start - startTime, wave.strength);
        } else {
          gl.uniform4f(waveLocations[i], 0, 0, -1000, 0);
        }
      }

      gl.clearColor(0, 0, 0, 0);
      gl.clear(gl.COLOR_BUFFER_BIT);
      gl.drawArrays(gl.TRIANGLES, 0, 6);

      this.animationFrameId = requestAnimationFrame(render);
    };

    render();
  }
}
