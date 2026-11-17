/**
 * 입장 커튼의 천: 모양 계산과 그리기 (EntranceCurtain에서 쓴다).
 *
 * 천 위의 한 점은 s(안쪽 끝에서 바깥쪽으로 잰 거리, px)와 v(0 = 레일, 1 = 밑단)로 본다.
 * 레일이 먼저 움직이고 아래쪽은 스프링으로 한 단계씩 늦게 따라와서, 걷힐 때 밑단이 뒤처졌다가 찰랑인다.
 * 계산은 왼쪽 폭 기준이고 오른쪽 폭은 좌우를 뒤집어 그린다.
 *
 * 매 장면마다 꼭짓점 위치만 새로 계산해 WebGL로 그린다 (화면이 커도 한 장면에 드는 일이 거의 같다).
 * 주름 색은 아래 색 함수로 만든 표(텍스처)에서 읽는데, 캔버스가 준비되기 전에 보이는 CSS 커튼도
 * 같은 함수로 만들어서 둘이 겹쳐도 티가 나지 않는다.
 *
 * 주름 폭·끝단 띠 두께는 globals.css의 --ec-fold / --ec-trim 과 같은 식이어야 한다.
 */

type RGB = readonly [number, number, number];
/** -1 왼쪽 폭, 1 오른쪽 폭 */
export type Side = -1 | 1;

const TAU = Math.PI * 2;
const clamp = (x: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, x));
const mix = (a: RGB, b: RGB, t: number): RGB => [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t];
const css = (c: RGB) => `rgb(${Math.round(c[0])},${Math.round(c[1])},${Math.round(c[2])})`;

/* ── 색: globals.css 토큰과 같은 값 ─────────────────────────────── */
const DEEP: RGB = [0, 16, 52]; // 주름 골짜기 (navy보다 한 단계 어둡게)
const NAVY: RGB = [0, 36, 94]; // --color-navy
const CAU: RGB = [0, 76, 151]; // --color-cau
const BLUE: RGB = [33, 85, 164]; // --color-primary-strong
const PEACH: RGB = [255, 207, 178]; // 로그인 배경 그라데이션의 노을 쪽 끝 (#ffcfb2)
const ORANGE: RGB = [255, 96, 0]; // --color-orange
const ORANGE_DEEP: RGB = [203, 75, 0]; // --color-orange-badge
const ORANGE_LIGHT: RGB = [255, 150, 84];
const SHADE: RGB = [0, 8, 30]; // 위쪽 그늘
const SHADE_TOP = 0.46;
const SHADE_BOTTOM = 0.18;

/** 닫혀 있을 때 주름의 깊이감 (걷히며 주름이 모일수록 1에 가까워진다) */
const REST_DEPTH = 0.78;
/** 주름 무늬가 되풀이되는 단위 (주름 몇 폭마다). CSS 그라데이션의 반복 주기와 같다 */
const CYCLE = 4;
/** 끝단 띠가 놓인 자리 (주름 폭 대비) */
const TRIM_AT = 0.06;

/**
 * 안쪽 끝에서 주름 n폭만큼 떨어진 자리의 위상. 손으로 잡은 주름처럼 간격이 조금씩 다르게
 * 느린 물결을 더한다 (CYCLE폭마다 되풀이).
 */
function phase(n: number): number {
  const c = (TAU * n) / CYCLE;
  return TAU * n + 0.5 * Math.sin(c + 1.1) + 0.2 * Math.sin(3 * c + 0.4);
}

/** 밝기 0~1. 볼록한 면은 넓고 밝게, 골은 좁고 어둡게. 빛은 두 폭 모두 화면 오른쪽(노을 쪽)에서 온다 */
function light(n: number, side: Side, depth: number): number {
  const c = (TAU * n) / CYCLE;
  const face = Math.pow(0.5 + 0.5 * Math.cos(phase(n) - side * 0.6), 0.7);
  // 주름마다 깊이와 밝기가 조금씩 다르다
  const own = depth * (1 + 0.18 * Math.sin(c + 2.3));
  return clamp(0.5 + (face - 0.56) * own + 0.03 * Math.sin(2 * c + 0.7), 0, 1);
}

function fabricColor(n: number, side: Side, depth: number): RGB {
  const l = light(n, side, depth);
  const base = l < 0.3 ? mix(DEEP, NAVY, l / 0.3) : l < 0.8 ? mix(NAVY, CAU, (l - 0.3) / 0.5) : mix(CAU, BLUE, ((l - 0.8) / 0.2) * 0.8);
  // 노을빛이 스치는 쪽 주름에만 살짝 따뜻한 기운
  const warm = 0.12 * Math.pow(Math.max(0, Math.cos(phase(n) - side * 1.15)), 4);
  return mix(base, PEACH, warm);
}

function trimColor(n: number, side: Side, depth: number): RGB {
  const l = light(n, side, depth);
  return l < 0.7 ? mix(ORANGE_DEEP, ORANGE, l / 0.7) : mix(ORANGE, ORANGE_LIGHT, (l - 0.7) / 0.3);
}

/** 캔버스가 준비되기 전(서버가 보낸 화면)에 보이는 닫힌 커튼 한 폭의 배경 */
export function restBackground(side: Side): string {
  const to = side < 0 ? "to left" : "to right";
  const steps = CYCLE * 16;
  const folds = Array.from({ length: steps + 1 }, (_, k) => {
    const n = (CYCLE * k) / steps;
    return `${css(fabricColor(n, side, REST_DEPTH))} calc(var(--ec-fold) * ${n.toFixed(4)})`;
  });
  const band = css(trimColor(TRIM_AT, side, REST_DEPTH));
  const t = "var(--ec-trim)";
  const shade = SHADE.join(",");
  return [
    `linear-gradient(rgba(${shade},${SHADE_TOP}), rgba(${shade},0) 24%, rgba(${shade},0) 80%, rgba(${shade},${SHADE_BOTTOM}))`,
    `linear-gradient(${to}, ${band} ${t}, transparent ${t} calc(${t} * 1.7), ${band} calc(${t} * 1.7) calc(${t} * 2), transparent calc(${t} * 2))`,
    `repeating-linear-gradient(${to}, ${folds.join(", ")})`,
  ].join(", ");
}

/* ── 색 표(텍스처) ──────────────────────────────────────────────── */
const TEX_W = 512;
/** 주름 깊이를 몇 단계로 나눠 담을지 */
const LEVELS = 8;
/** 가로: 주름 CYCLE폭. 세로: [왼쪽 천, 왼쪽 띠, 오른쪽 천, 오른쪽 띠] × 깊이 LEVELS단계 */
const TEX_H = LEVELS * 4;

function colorTable(): Uint8Array {
  const data = new Uint8Array(TEX_W * TEX_H * 4);
  for (let row = 0; row < TEX_H; row++) {
    const block = Math.floor(row / LEVELS);
    const side: Side = block < 2 ? -1 : 1;
    const depth = REST_DEPTH + ((1 - REST_DEPTH) * (row % LEVELS)) / (LEVELS - 1);
    for (let x = 0; x < TEX_W; x++) {
      const n = (CYCLE * (x + 0.5)) / TEX_W;
      const c = block % 2 === 0 ? fabricColor(n, side, depth) : trimColor(n, side, depth);
      data.set([Math.round(c[0]), Math.round(c[1]), Math.round(c[2]), 255], (row * TEX_W + x) * 4);
    }
  }
  return data;
}

const VERT = `
attribute vec2 a_pos;
attribute vec2 a_fab;
uniform vec2 u_view;
uniform float u_flip;
varying vec2 v_fab;
varying float v_top;
void main() {
  float x = mix(a_pos.x, u_view.x - a_pos.x, u_flip);
  v_fab = a_fab;
  v_top = a_pos.y / u_view.y;
  gl_Position = vec4(x / u_view.x * 2.0 - 1.0, 1.0 - a_pos.y / u_view.y * 2.0, 0.0, 1.0);
}`;

// v_fab: 천일 때는 (안쪽 끝에서의 거리, 밑단에서의 거리) px, 그림자일 때는 (세기 0~1, 0)
const FRAG = `
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
uniform sampler2D u_table;
uniform float u_fold;
uniform float u_trim;
uniform float u_row;
uniform float u_soft;
uniform float u_shadow;
varying vec2 v_fab;
varying float v_top;

float band(float x, float from, float to, float soft) {
  return smoothstep(from - soft, from + soft, x) * (1.0 - smoothstep(to - soft, to + soft, x));
}

void main() {
  if (u_shadow > 0.0) {
    float a = u_shadow * v_fab.x * v_fab.x;
    gl_FragColor = vec4(vec3(0.0, 14.0, 44.0) / 255.0 * a, a);
    return;
  }
  float n = v_fab.x / u_fold / ${CYCLE.toFixed(1)};
  vec3 color = texture2D(u_table, vec2(n, u_row)).rgb;
  // 안쪽 끝단과 밑단의 주황 띠: 굵은 줄 + 가는 줄
  float edge = band(v_fab.x, -99.0, u_trim, u_soft) + band(v_fab.x, u_trim * 1.7, u_trim * 2.0, u_soft);
  color = mix(color, texture2D(u_table, vec2(${(TRIM_AT / CYCLE).toFixed(4)}, u_row + 0.25)).rgb, min(1.0, edge));
  float hb = u_trim * 1.25;
  float hem = band(v_fab.y, -99.0, hb, 0.7) + band(v_fab.y, hb * 1.7, hb * 2.0, 0.7);
  color = mix(color, texture2D(u_table, vec2(n, u_row + 0.25)).rgb, min(1.0, hem));
  // 위쪽 그늘
  float shade = ${SHADE_TOP} * clamp(1.0 - v_top / 0.24, 0.0, 1.0) + ${SHADE_BOTTOM} * clamp((v_top - 0.8) / 0.2, 0.0, 1.0);
  color = mix(color, vec3(${SHADE.map((c) => c.toFixed(1)).join(", ")}) / 255.0, shade);
  gl_FragColor = vec4(color, 1.0);
}`;

function build(gl: WebGLRenderingContext): WebGLProgram | null {
  const program = gl.createProgram();
  if (!program) return null;
  for (const [type, source] of [
    [gl.VERTEX_SHADER, VERT],
    [gl.FRAGMENT_SHADER, FRAG],
  ] as const) {
    const shader = gl.createShader(type);
    if (!shader) return null;
    gl.shaderSource(shader, source);
    gl.compileShader(shader);
    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) return null;
    gl.attachShader(program, shader);
  }
  gl.bindAttribLocation(program, 0, "a_pos");
  gl.bindAttribLocation(program, 1, "a_fab");
  gl.linkProgram(program);
  return gl.getProgramParameter(program, gl.LINK_STATUS) ? program : null;
}

/* ── 움직임 ─────────────────────────────────────────────────────── */
/** 가운데에서 두 폭이 겹치는 양(px). globals.css의 폭 계산(50% + 1px)과 같다 */
const LAP = 1;
/** 세로로 나눈 칸 수 */
const ROWS = 20;
/** 레일에서 밑단까지 움직임이 전해지는 단계 수 */
const STAGES = 4;
/** 단계 사이 스프링: 세기(rad/s)와 감쇠비. 감쇠비가 1보다 작아 살짝 지나쳤다 돌아온다 */
const OMEGA = 30;
const ZETA = 0.5;
const STEP = 1 / 480;
/** 레일이 끝까지 걷히는 시간(초). 밑단은 이보다 조금 늦게 따라 나간다 */
const OPEN_SEC = 1.45;
/** 걷기 전에 살짝 당기는 양 (전체 거리 대비) */
const TUG = 0.06;
/** 천이 비스듬해질 때 밑단이 들리는 정도 */
const LIFT = 1.2;

/** 레일이 열린 정도 0~1. 살짝 당겼다가(tug) 한 번에 걷는다 */
function pull(x: number): number {
  if (x <= 0) return 0;
  if (x >= 1) return 1;
  const tug = TUG * (1 - Math.pow(1 - Math.min(1, x / 0.13), 3));
  // 화면 밖으로 나가는 끝에서 머뭇거리지 않게 가감속 곡선의 80%까지만 쓴다
  const y = clamp((x - 0.17) / 0.83, 0, 1) * 0.8;
  const ease = y < 0.5 ? 2 * y * y : 1 - 2 * (1 - y) * (1 - y);
  return tug + (1 - TUG) * (ease / 0.92);
}

/** 단계별 값(0 = 레일 … STAGES = 밑단)을 높이 v에서 부드럽게 이어 읽는다 (Catmull-Rom) */
function along(p: Float32Array, v: number): number {
  const x = v * STAGES;
  const k = Math.min(STAGES - 1, Math.floor(x));
  const f = x - k;
  const p0 = p[Math.max(0, k - 1)];
  const p1 = p[k];
  const p2 = p[k + 1];
  const p3 = p[Math.min(STAGES, k + 2)];
  return p1 + 0.5 * f * (p2 - p0 + f * (2 * p0 - 5 * p1 + 4 * p2 - p3 + f * (3 * (p1 - p2) + p3 - p0)));
}

interface Geo {
  w: number;
  h: number;
  half: number;
  /** 주름 한 폭(px) */
  fold: number;
  /** 끝단 주황 띠 두께(px) */
  trim: number;
  /** 화면 바깥으로 넘겨 둔 천(px). 흔들려도 가장자리가 비지 않는다 */
  over: number;
  /** 닫혔을 때 한 폭의 길이(px) */
  len: number;
  /** 다 모였을 때의 폭 (len 대비) */
  bunch: number;
  /** 안쪽 끝이 마지막에 가는 자리 (len 대비, 화면 밖) */
  exit: number;
  /** 가로로 나눈 칸의 너비(px)와 칸 수 */
  ds: number;
  cols: number;
  topPad: number;
  /** 세로 한 칸의 길이(px) */
  seg: number;
  scallop: number;
}

function measure(w: number, h: number): Geo {
  const fold = clamp(w * 0.052, 44, 76);
  const trim = clamp(w * 0.007, 5, 9);
  const over = fold * 0.75;
  const half = w / 2;
  const len = half + LAP + over;
  // 밑단의 물결이 매끄럽게 보일 만큼만 나눈다 (주름 한 폭에 열 칸)
  const ds = fold / 10;
  const scallop = fold * 0.14;
  const topPad = 24;
  // 가만히 있을 때는 밑단(물결 + 띠)이 화면 아래에 숨어 있다
  const hemPad = scallop + trim * 3 + 4;
  return {
    w,
    h,
    half,
    fold,
    trim,
    over,
    len,
    bunch: clamp((len / fold) * 12, 40, len * 0.4) / len,
    exit: (over - fold) / len,
    ds,
    cols: Math.ceil(len / ds),
    topPad,
    seg: (h + topPad + hemPad) / ROWS,
    scallop,
  };
}

export interface Fabric {
  /** 화면 크기에 맞춘다 (CSS px). 캔버스의 픽셀 크기는 부르는 쪽에서 먼저 정해 둔다 */
  fit(w: number, h: number): void;
  /** 걷는 동작을 dt초만큼 진행한다 */
  open(dt: number): void;
  /** 지금 모습을 그린다. idle: 처음 그린 뒤로 흐른 시간(초) */
  draw(idle: number): void;
  /** 두 폭이 모두 화면 밖으로 나갔는지 */
  cleared(): boolean;
}

/** WebGL을 쓸 수 없으면 null */
export function createFabric(canvas: HTMLCanvasElement): Fabric | null {
  const gl = canvas.getContext("webgl", { alpha: true, antialias: true, depth: false, stencil: false });
  const program = gl && build(gl);
  if (!gl || !program) return null;

  const N = ROWS + 1;
  const at = (name: string) => gl.getUniformLocation(program, name);
  const uni = { view: at("u_view"), flip: at("u_flip"), fold: at("u_fold"), trim: at("u_trim"), row: at("u_row"), soft: at("u_soft"), shadow: at("u_shadow") };
  const buf = { pos: gl.createBuffer(), fab: gl.createBuffer(), index: gl.createBuffer(), shadowPos: gl.createBuffer(), shadowFab: gl.createBuffer() };

  gl.useProgram(program);
  gl.enableVertexAttribArray(0);
  gl.enableVertexAttribArray(1);
  gl.enable(gl.BLEND);
  gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA);
  gl.clearColor(0, 0, 0, 0);

  gl.bindTexture(gl.TEXTURE_2D, gl.createTexture());
  gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, TEX_W, TEX_H, 0, gl.RGBA, gl.UNSIGNED_BYTE, colorTable());
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.REPEAT);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);

  // 그림자 띠의 세기: 천 쪽 1 → 바깥쪽 0
  const shadowFab = new Float32Array(N * 4);
  for (let j = 0; j < N; j++) shadowFab[j * 4] = 1;
  gl.bindBuffer(gl.ARRAY_BUFFER, buf.shadowFab);
  gl.bufferData(gl.ARRAY_BUFFER, shadowFab, gl.STATIC_DRAW);
  const shadowPos = new Float32Array(N * 4);

  let geo = measure(1, 1);
  let pos = new Float32Array(0);
  let indexCount = 0;
  const lead = new Float32Array(N);
  const width = new Float32Array(N);
  // 단계별 안쪽 끝(e)·바깥쪽 끝(a)의 위치와 속도. 닫힌 폭을 1로 본 값이라 화면 크기가 바뀌어도 이어진다
  const e = new Float32Array(STAGES + 1).fill(1);
  const a = new Float32Array(STAGES + 1);
  const ve = new Float32Array(STAGES + 1);
  const va = new Float32Array(STAGES + 1);
  let t = 0;
  let target = 0;

  const K = OMEGA * OMEGA;
  const C = 2 * ZETA * OMEGA;

  const point = (posBuffer: WebGLBuffer | null, fabBuffer: WebGLBuffer | null) => {
    gl.bindBuffer(gl.ARRAY_BUFFER, posBuffer);
    gl.vertexAttribPointer(0, 2, gl.FLOAT, false, 0, 0);
    gl.bindBuffer(gl.ARRAY_BUFFER, fabBuffer);
    gl.vertexAttribPointer(1, 2, gl.FLOAT, false, 0, 0);
  };

  /** 한 폭의 꼭짓점 위치를 계산해 그린다 */
  const panel = (side: Side, idle: number) => {
    const { len, fold, cols, ds, seg } = geo;
    // 지금 레일이 움직이는 빠르기 (평균을 1로). 빠를수록 천이 잘게 떤다
    const speed = t > 0 ? (Math.abs(pull((t + 0.02) / OPEN_SEC) - pull(t / OPEN_SEC)) / 0.02) * OPEN_SEC : 0;
    const flutter = fold * 0.14 * Math.min(1, speed / 1.4);
    // 걷기 전에는 바람에 숨 쉬듯 두 폭이 같은 쪽으로 아주 조금 흔들린다
    const breath = (1 - Math.min(1, t / 0.25)) * fold * 0.07 * Math.sin(idle * 3.4);

    for (let j = 0; j <= ROWS; j++) {
      const v = j / ROWS;
      const ej = along(e, v);
      width[j] = Math.max(8, (ej - along(a, v)) * len);
      lead[j] = -geo.over + ej * len - side * breath * v * v;
    }

    for (let i = 0; i <= cols; i++) {
      const s = Math.min(len, i * ds);
      const u = s / len;
      let y = -geo.topPad;
      let prev = 0;
      for (let j = 0; j <= ROWS; j++) {
        const v = j / ROWS;
        const x = lead[j] - u * width[j] + flutter * v * Math.sin(t * 13 - v * 4 + u * 6 + side * 0.9);
        if (j > 0) {
          // 비스듬히 끌려가는 만큼 세로 길이가 줄어 밑단이 들린다
          const q = ((x - prev) * ROWS) / len;
          y += seg * (1 - Math.min(0.42, LIFT * q * q));
        }
        pos[(i * N + j) * 2] = x;
        pos[(i * N + j) * 2 + 1] = y;
        prev = x;
      }
      // 앞으로 나온 주름은 낮게, 들어간 주름은 높게: 밑단이 물결 모양이 된다
      pos[(i * N + ROWS) * 2 + 1] -= geo.scallop * (0.5 - 0.5 * Math.cos(phase(s / fold)));
    }

    const mid = ROWS >> 1;
    const squeeze = width[mid] / len;
    const gather = clamp((1 - squeeze) / (1 - geo.bunch), 0, 1);
    gl.uniform1f(uni.flip, side > 0 ? 1 : 0);
    gl.uniform1f(uni.row, ((side > 0 ? 2 : 0) * LEVELS + 0.5 + gather * (LEVELS - 1)) / TEX_H);
    // 천이 모일수록 화면의 1px이 천 위에서는 더 긴 거리다
    gl.uniform1f(uni.soft, 0.75 / squeeze);

    // 벌어진 틈으로 뒤 화면에 떨어지는 그림자
    const gap = geo.half - lead[mid];
    if (gap > 1) {
      for (let j = 0; j <= ROWS; j++) {
        shadowPos.set([pos[j * 2] - 2, pos[j * 2 + 1], pos[j * 2] + fold * 0.7, pos[j * 2 + 1]], j * 4);
      }
      gl.bindBuffer(gl.ARRAY_BUFFER, buf.shadowPos);
      gl.bufferData(gl.ARRAY_BUFFER, shadowPos, gl.DYNAMIC_DRAW);
      point(buf.shadowPos, buf.shadowFab);
      gl.uniform1f(uni.shadow, 0.42 * Math.min(1, gap / 48));
      gl.drawArrays(gl.TRIANGLE_STRIP, 0, N * 2);
    }

    gl.bindBuffer(gl.ARRAY_BUFFER, buf.pos);
    gl.bufferData(gl.ARRAY_BUFFER, pos, gl.DYNAMIC_DRAW);
    point(buf.pos, buf.fab);
    gl.uniform1f(uni.shadow, 0);
    gl.drawElements(gl.TRIANGLES, indexCount, gl.UNSIGNED_SHORT, 0);
  };

  return {
    fit(w, h) {
      geo = measure(w, h);
      const { cols, ds, len, seg } = geo;
      pos = new Float32Array((cols + 1) * N * 2);
      const fab = new Float32Array((cols + 1) * N * 2);
      const index = new Uint16Array(cols * ROWS * 6);
      for (let i = 0; i <= cols; i++) {
        for (let j = 0; j <= ROWS; j++) {
          fab[(i * N + j) * 2] = Math.min(len, i * ds);
          fab[(i * N + j) * 2 + 1] = (ROWS - j) * seg;
          if (i < cols && j < ROWS) {
            const p = i * N + j;
            index.set([p, p + N, p + 1, p + 1, p + N, p + N + 1], (i * ROWS + j) * 6);
          }
        }
      }
      indexCount = index.length;
      gl.bindBuffer(gl.ARRAY_BUFFER, buf.fab);
      gl.bufferData(gl.ARRAY_BUFFER, fab, gl.STATIC_DRAW);
      gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, buf.index);
      gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, index, gl.STATIC_DRAW);
      gl.viewport(0, 0, canvas.width, canvas.height);
      gl.uniform2f(uni.view, w, h);
      gl.uniform1f(uni.fold, geo.fold);
      gl.uniform1f(uni.trim, geo.trim);
    },
    open(dt) {
      target += dt;
      while (t + STEP <= target) {
        t += STEP;
        e[0] = 1 + (geo.exit - 1) * pull(t / OPEN_SEC);
        // 바깥쪽 끝은 화면 가장자리에 걸려 있다가, 천이 다 모이면 같이 밀려 나간다
        a[0] = Math.min(0, e[0] - geo.bunch);
        for (let k = 1; k <= STAGES; k++) {
          ve[k] += (K * (e[k - 1] - e[k]) - C * ve[k]) * STEP;
          e[k] += ve[k] * STEP;
          va[k] += (K * (a[k - 1] - a[k]) - C * va[k]) * STEP;
          a[k] += va[k] * STEP;
        }
      }
    },
    draw(idle) {
      gl.clear(gl.COLOR_BUFFER_BIT);
      // 오른쪽을 먼저 그리고, 왼쪽이 가운데에서 살짝 덮는다
      panel(1, idle);
      panel(-1, idle);
    },
    cleared() {
      if (t > OPEN_SEC + 1 || gl.isContextLost()) return true;
      let last = -Infinity;
      for (let k = 0; k <= STAGES; k++) last = Math.max(last, e[k]);
      return -geo.over + last * geo.len < -geo.fold * 0.4;
    },
  };
}
