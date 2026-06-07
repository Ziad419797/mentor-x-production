// Safe pagination helper to replace [].constructor(totalPages()) pattern
export function getPageNumbers(totalPages: number): number[] {
  const pages: number[] = [];
  for (let i = 0; i < totalPages; i++) {
    pages.push(i);
  }
  return pages;
}

export function trackByIndex(index: number): number {
  return index;
}

export function trackById<T extends { id?: number | string }>(index: number, item: T): number | string {
  return item?.id ?? index;
}
