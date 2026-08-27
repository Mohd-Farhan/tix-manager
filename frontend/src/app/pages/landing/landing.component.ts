import { Component, OnInit, AfterViewInit, OnDestroy, ViewChild, ElementRef, NgZone, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { LogoComponent } from '../../shared/components/logo/logo.component';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink, LogoComponent],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.css',
})
export class LandingComponent implements OnInit, AfterViewInit, OnDestroy {
  private router = inject(Router);
  private ngZone = inject(NgZone);

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

  /* Animated hero stats */
  stats = [
    { value: '99.9%', label: 'Uptime SLA' },
    { value: '< 2min', label: 'Avg Response' },
    { value: '50K+', label: 'Tickets Resolved' },
    { value: '4.9★', label: 'Customer Rating' },
  ];

  features = [
    {
      icon: `<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a4 4 0 0 0-4 4c0 2 1 3.5 4 5.5C15 9.5 16 8 16 6a4 4 0 0 0-4-4Z"/><path d="M12 11.5c-3 2-4 3.5-4 5.5a4 4 0 0 0 8 0c0-2-1-3.5-4-5.5Z"/><line x1="12" y1="2" x2="12" y2="22"/></svg>`,
      title: 'AI-Powered Triage',
      description: 'Gemini AI automatically classifies, prioritizes, and routes every incoming ticket — so your team focuses on what matters.',
    },
    {
      icon: `<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><path d="M8 10h.01"/><path d="M12 10h.01"/><path d="M16 10h.01"/></svg>`,
      title: 'Smart Auto-Responses',
      description: 'Retrieval-augmented generation searches your knowledge base and drafts instant, accurate replies for common inquiries.',
    },
    {
      icon: `<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18"/><path d="M9 21V9"/></svg>`,
      title: 'Agent Workspace',
      description: 'A unified dashboard with ticket queues, real-time chat, AI-summarized threads, and suggested replies — all in one place.',
    },
    {
      icon: `<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>`,
      title: 'Live Analytics',
      description: 'Track SLA compliance, agent performance, sentiment trends, and resolution metrics with beautifully crafted dashboards.',
    },
    {
      icon: `<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/><circle cx="12" cy="16" r="1"/></svg>`,
      title: 'Enterprise Security',
      description: 'JWT authentication, role-based access control, and audit logging keep your support operations locked down.',
    },
    {
      icon: `<svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M12 1v6"/><path d="M12 17v6"/><path d="M5.6 5.6l4.25 4.25"/><path d="M14.15 14.15l4.25 4.25"/><path d="M1 12h6"/><path d="M17 12h6"/><path d="M5.6 18.4l4.25-4.25"/><path d="M14.15 9.85l4.25-4.25"/></svg>`,
      title: 'Microservices Ready',
      description: 'Architected to scale — seamlessly transition from monolith to distributed microservices with API Gateway & RabbitMQ.',
    },
  ];

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

    const RIPPLE_INTENSITY = 2.25;
    const RIPPLE_LIFETIME = 5.2;
    const RIPPLE_SPEED = 0.55;
    const POINTER_DISTANCE_THRESHOLD = 0.008;
    const POINTER_INTERVAL = 0.055;
    const MAX_WAVES = 14;

    const gl = canvas.getContext('webgl', {
      alpha: true,
      antialias: true,
      depth: false,
      stencil: false,
      premultipliedAlpha: false,
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
      const int MAX_WAVES = ${MAX_WAVES};
      uniform vec4 uWaves[MAX_WAVES];
      uniform float uWaveCount;

      float hash(vec2 p) {
        return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
      }

      float noise(vec2 p) {
        vec2 i = floor(p);
        vec2 f = fract(p);
        f = f * f * (3.0 - 2.0 * f);
        float a = hash(i);
        float b = hash(i + vec2(1.0, 0.0));
        float c = hash(i + vec2(0.0, 1.0));
        float d = hash(i + vec2(1.0, 1.0));
        return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
      }

      float baseWater(vec2 p) {
        float h = 0.0;
        h += sin(p.x * 2.8 + uTime * 0.16) * 0.005;
        h += sin(p.y * 4.4 - uTime * 0.12) * 0.004;
        h += sin(p.x * 12.0 + p.y * 5.0 + uTime * 0.18) * 0.0018;
        h += sin(p.y * 15.0 - p.x * 4.0 - uTime * 0.14) * 0.0015;
        h += (noise(p * 3.5 + vec2(uTime * 0.02, -uTime * 0.016)) - 0.5) * 0.004;
        return h;
      }

      float singleWave(vec2 p, vec4 wave) {
        if (wave.z < 0.0) return 0.0;
        float age = uTime - wave.z;
        if (age <= 0.0 || age > ${RIPPLE_LIFETIME.toFixed(2)}) return 0.0;
        float radius = age * ${RIPPLE_SPEED.toFixed(3)};
        float d = distance(p, wave.xy);
        float front = d - radius;
        float crest = exp(-front * front * 520.0);
        float trough = exp(-(front + 0.018) * (front + 0.018) * 700.0) * 0.22;
        float ageFade = 1.0 - smoothstep(0.0, ${RIPPLE_LIFETIME.toFixed(2)}, age);
        ageFade = pow(ageFade, 1.15);
        vec2 screenUV = vUv;
        float nearestEdge = min(min(screenUV.x, 1.0 - screenUV.x), min(screenUV.y, 1.0 - screenUV.y));
        float edgeFade = smoothstep(0.0, 0.06, nearestEdge);
        return (crest - trough) * ageFade * edgeFade * wave.w;
      }

      float waterHeight(vec2 p) {
        float height = baseWater(p);
        for (int i = 0; i < MAX_WAVES; i++) {
          if (float(i) >= uWaveCount) break;
          height += singleWave(p, uWaves[i]) * 0.035;
        }
        return height;
      }

      vec3 getNormal(vec2 p) {
        float pixelX = 1.25 / uResolution.y;
        float pixelY = 1.25 / uResolution.y;
        float left = waterHeight(p - vec2(pixelX, 0.0));
        float right = waterHeight(p + vec2(pixelX, 0.0));
        float down = waterHeight(p - vec2(0.0, pixelY));
        float up = waterHeight(p + vec2(0.0, pixelY));
        float dx = (right - left) * 10.0;
        float dy = (up - down) * 10.0;
        return normalize(vec3(-dx, -dy, 1.0));
      }

      float caustics(vec2 p) {
        vec2 uv = p * 5.5;
        uv += vec2(uTime * 0.08, -uTime * 0.06);
        float a = sin(uv.x + sin(uv.y * 1.4));
        float b = sin(uv.y * 1.2 + cos(uv.x * 0.8));
        float c = sin((uv.x + uv.y) * 1.4);
        return pow(abs(a * b * c), 5.0);
      }

      vec3 skyReflection(vec3 N) {
        float horizon = 1.0 - abs(N.y);
        horizon = smoothstep(0.15, 1.0, horizon);
        vec3 deepSky = vec3(0.035, 0.16, 0.25);
        vec3 brightSky = vec3(0.48, 0.84, 0.91);
        return mix(deepSky, brightSky, horizon);
      }

      void main() {
        float aspect = uResolution.x / uResolution.y;
        vec2 p = (vUv - vec2(0.5));
        p.x *= aspect;
        vec3 N = getNormal(p);
        vec3 V = normalize(vec3(0.0, 0.0, 1.0));
        vec3 L = normalize(vec3(-0.42, 0.48, 0.76));
        vec3 L2 = normalize(vec3(0.52, 0.16, 0.63));
        float diffuse = max(dot(N, L), 0.0);
        float fill = max(dot(N, L2), 0.0);
        float cosView = clamp(dot(N, V), 0.0, 1.0);
        float fresnel = pow(1.0 - cosView, 5.0);
        vec3 water = uWaterColor;
        float depth = smoothstep(0.0, 1.0, vUv.y);
        water *= mix(0.70, 1.06, depth);
        vec2 refractedUV = vUv + N.xy * 0.045;
        float refractNoise = noise(refractedUV * 14.0 + uTime * 0.015);
        vec3 refracted = water;
        refracted += vec3(0.014, 0.055, 0.050) * refractNoise;
        float caustic = caustics(p);
        caustic *= 1.0 - smoothstep(0.25, 1.0, length(p));
        vec3 causticLight = vec3(0.15, 0.90, 0.70) * caustic * 0.24;
        vec3 reflection = skyReflection(N);
        vec3 H = normalize(L + V);
        float sharpSpecular = pow(max(dot(N, H), 0.0), 320.0) * 1.4;
        float broadSpecular = pow(max(dot(N, H), 0.0), 40.0) * 0.25;
        
        vec3 color = refracted * (0.52 + diffuse * 0.40 + fill * 0.12);
        color = mix(color, reflection, fresnel * 0.62);
        color += causticLight;
        color += vec3(1.0, 0.97, 0.86) * (sharpSpecular * 1.15 + broadSpecular);
        float shimmer = noise(vUv * 100.0 + vec2(uTime * 0.04, -uTime * 0.025));
        color += vec3((shimmer - 0.5) * 0.012);
        color += mix(vec3(0.0, 0.045, 0.055), vec3(0.035, 0.09, 0.105), vUv.y) * 0.15;
        
        color = color / (color + vec3(0.82));
        color = pow(color, vec3(0.88));
        color *= 1.10;
        
        // Calculate alpha based on surface disturbance
        float normalDist = length(N.xy);
        float specularLight = sharpSpecular * 1.15 + broadSpecular;
        float alpha = clamp(normalDist * 4.5 + specularLight * 3.0 + caustic * 2.5, 0.0, 1.0);
        
        gl_FragColor = vec4(clamp(color, 0.0, 1.0), alpha);
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
    const uWaveCount = gl.getUniformLocation(program, 'uWaveCount') as WebGLUniformLocation;
    const waveLocations: WebGLUniformLocation[] = [];
    for (let i = 0; i < MAX_WAVES; i++) {
      waveLocations.push(gl.getUniformLocation(program, `uWaves[${i}]`) as WebGLUniformLocation);
    }

    const waves: any[] = [];
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

    let lastPointerX: number | null = null;
    let lastPointerY: number | null = null;
    let lastWaveTime = 0;

    this.pointerEnterHandler = (event: PointerEvent) => {
      addWave(event.clientX, event.clientY, RIPPLE_INTENSITY);
      lastPointerX = event.clientX;
      lastPointerY = event.clientY;
      lastWaveTime = performance.now();
    };

    this.pointerMoveHandler = (event: PointerEvent) => {
      const now = performance.now();
      if (lastPointerX === null || lastPointerY === null) {
        lastPointerX = event.clientX;
        lastPointerY = event.clientY;
        return;
      }
      const dx = event.clientX - lastPointerX;
      const dy = event.clientY - lastPointerY;
      const distance = Math.sqrt(dx * dx + dy * dy);
      const threshold = Math.max(3, Math.min(12, window.innerWidth * 0.004));
      const enoughDistance = distance >= threshold;
      const enoughTime = now - lastWaveTime >= POINTER_INTERVAL * 1000;
      if (enoughDistance || enoughTime) {
        const velocity = Math.min(1.6, distance / 45);
        const strength = RIPPLE_INTENSITY * (0.84 + velocity * 0.30);
        addWave(event.clientX, event.clientY, strength);
        lastPointerX = event.clientX;
        lastPointerY = event.clientY;
        lastWaveTime = now;
      }
    };

    this.pointerLeaveHandler = () => {
      lastPointerX = null;
      lastPointerY = null;
    };

    this.pointerDownHandler = (event: PointerEvent) => {
      addWave(event.clientX, event.clientY, RIPPLE_INTENSITY * 1.30);
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
      addWave(touch.clientX, touch.clientY, RIPPLE_INTENSITY * 1.35);
      lastWaveTime = performance.now();
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
      if (lastTouchX === null || lastTouchY === null) {
        lastTouchX = touch.clientX;
        lastTouchY = touch.clientY;
        return;
      }
      const dx = touch.clientX - lastTouchX;
      const dy = touch.clientY - lastTouchY;
      const distance = Math.sqrt(dx * dx + dy * dy);
      const now = performance.now();
      if (distance >= 4 || now - lastWaveTime >= 65) {
        const velocity = Math.min(1.6, distance / 40);
        addWave(touch.clientX, touch.clientY, RIPPLE_INTENSITY * (0.90 + velocity * 0.32));
        lastTouchX = touch.clientX;
        lastTouchY = touch.clientY;
        lastWaveTime = now;
      }
    };

    this.touchEndHandler = () => {
      activeTouch = null;
      lastTouchX = null;
      lastTouchY = null;
    };

    // Attach both pointer and mouse events for maximum compatibility
    hero.addEventListener('pointerenter', this.pointerEnterHandler as any, { passive: true });
    hero.addEventListener('pointermove', this.pointerMoveHandler as any, { passive: true });
    hero.addEventListener('pointerleave', this.pointerLeaveHandler as any, { passive: true });
    hero.addEventListener('pointerdown', this.pointerDownHandler as any, { passive: true });

    hero.addEventListener('mouseenter', this.pointerEnterHandler as any, { passive: true });
    hero.addEventListener('mousemove', this.pointerMoveHandler as any, { passive: true });
    hero.addEventListener('mouseleave', this.pointerLeaveHandler as any, { passive: true });
    hero.addEventListener('mousedown', this.pointerDownHandler as any, { passive: true });

    hero.addEventListener('touchstart', this.touchStartHandler as any, { passive: true });
    hero.addEventListener('touchmove', this.touchMoveHandler as any, { passive: true });
    hero.addEventListener('touchend', this.touchEndHandler as any, { passive: true });
    hero.addEventListener('touchcancel', this.touchEndHandler as any, { passive: true });

    const startTime = performance.now() / 1000;

    const getWaterColor = () => {
      if (!this.isDark) {
        // Transparent blue water for light mode (Azure blue-500: #2170e4)
        return [33 / 255, 112 / 255, 228 / 255];
      }
      
      const bg = getComputedStyle(document.documentElement).getPropertyValue('--background').trim();
      if (bg.startsWith('#')) {
        const r = parseInt(bg.substring(1, 3), 16) / 255;
        const g = parseInt(bg.substring(3, 5), 16) / 255;
        const b = parseInt(bg.substring(5, 7), 16) / 255;
        return [r, g, b];
      }
      return [7/255, 13/255, 24/255];
    };

    const render = () => {
      const elapsed = performance.now() / 1000 - startTime;
      for (let i = waves.length - 1; i >= 0; i--) {
        if (elapsed - (waves[i].start - startTime) > RIPPLE_LIFETIME) {
          waves.splice(i, 1);
        }
      }

      gl.uniform2f(uResolution, width, height);
      gl.uniform1f(uTime, elapsed);

      const waterColor = getWaterColor();
      gl.uniform3f(uWaterColor, waterColor[0], waterColor[1], waterColor[2]);
      gl.uniform1f(uWaveCount, waves.length);

      for (let i = 0; i < MAX_WAVES; i++) {
        if (i < waves.length) {
          const wave = waves[i];
          gl.uniform4f(waveLocations[i], wave.x, wave.y, wave.start - startTime, wave.strength);
        } else {
          gl.uniform4f(waveLocations[i], 0, 0, -1000, 0);
        }
      }

      gl.clearColor(0, 0, 0, 0); // Transparent clear color
      gl.clear(gl.COLOR_BUFFER_BIT);
      gl.drawArrays(gl.TRIANGLES, 0, 6);

      this.animationFrameId = requestAnimationFrame(render);
    };

    render();
  }
}
