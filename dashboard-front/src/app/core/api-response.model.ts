// =============================================================
// Standard API Response Models — Mentor-X Platform
// Backend may return any of these shapes; always use the
// helper functions below to extract data safely.
// =============================================================

/** Backend envelope for single-item and action responses */
export interface ApiResponse<T = unknown> {
  success: boolean;
  message?: string;
  data?: T;
}

/** Spring-style paginated response */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;   // current page (0-indexed)
  size: number;
}

// -----------------------------------------------------------------
// Safe extraction helpers
// Call these inside .pipe(map(res => extractList(res))) chains
// -----------------------------------------------------------------

/**
 * Extracts an array from any backend response shape:
 *   - T[]                              → returned as-is
 *   - { content: T[] }                 → Spring Page
 *   - { data: T[] }                    → custom envelope
 *   - { data: { content: T[] } }       → nested envelope
 *   - anything else                    → []
 */
export function extractList<T>(res: unknown): T[] {
  if (!res) return [];
  if (Array.isArray(res)) return res as T[];

  const r = res as Record<string, unknown>;

  // Spring Page: { content: [...] }
  if (Array.isArray(r['content'])) return r['content'] as T[];

  // Envelope: { data: [...] }
  if (Array.isArray(r['data'])) return r['data'] as T[];

  // Nested: { data: { content: [...] } }
  const nested = r['data'];
  if (nested && typeof nested === 'object') {
    const n = nested as Record<string, unknown>;
    if (Array.isArray(n['content'])) return n['content'] as T[];
    if (Array.isArray(n['data']))    return n['data'] as T[];
  }

  return [];
}

/**
 * Extracts a PageResponse from any backend shape.
 * Falls back to wrapping an array as a synthetic page.
 */
export function extractPage<T>(res: unknown): PageResponse<T> {
  if (!res) return emptyPage<T>();

  const r = res as Record<string, unknown>;

  // Proper Spring Page
  if (Array.isArray(r['content'])) {
    return {
      content:       r['content'] as T[],
      totalElements: (r['totalElements'] as number) || 0,
      totalPages:    (r['totalPages'] as number)    || 1,
      number:        (r['number'] as number)        || 0,
      size:          (r['size'] as number)          || 20,
    };
  }

  // Envelope wrapping a Page: { data: { content: [...] } }
  const inner = r['data'];
  if (inner && typeof inner === 'object') {
    const n = inner as Record<string, unknown>;
    if (Array.isArray(n['content'])) {
      return {
        content:       n['content'] as T[],
        totalElements: (n['totalElements'] as number) || 0,
        totalPages:    (n['totalPages'] as number)    || 1,
        number:        (n['number'] as number)        || 0,
        size:          (n['size'] as number)          || 20,
      };
    }
    if (Array.isArray(inner)) {
      return syntheticPage(inner as T[]);
    }
  }

  // Envelope wrapping a plain array: { data: [...] }
  if (Array.isArray(r['data'])) return syntheticPage(r['data'] as T[]);

  // Already an array
  if (Array.isArray(res)) return syntheticPage(res as T[]);

  return emptyPage<T>();
}

/**
 * Extracts a single item from an envelope response.
 * Returns null if the response is empty or on error.
 */
export function extractItem<T>(res: unknown): T | null {
  if (!res) return null;
  const r = res as Record<string, unknown>;
  if ('data' in r) return (r['data'] ?? null) as T | null;
  return res as T;
}

// -----------------------------------------------------------------
// Internal helpers
// -----------------------------------------------------------------
function emptyPage<T>(): PageResponse<T> {
  return { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 };
}

function syntheticPage<T>(arr: T[]): PageResponse<T> {
  return { content: arr, totalElements: arr.length, totalPages: 1, number: 0, size: arr.length };
}
