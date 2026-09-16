export function formatPrice(price: number): string {
  return `${new Intl.NumberFormat("ko-KR").format(price)}원`;
}

export function formatDistance(meters: number): string {
  if (meters < 1000) return `${Math.round(meters)}m`;
  return `${(meters / 1000).toFixed(1)}km`;
}

export function formatRelativeDate(date: Date | string): string {
  const target = typeof date === "string" ? new Date(date) : date;
  const diffDays = Math.floor((Date.now() - target.getTime()) / 86_400_000);

  if (diffDays <= 0) return "오늘";
  if (diffDays === 1) return "1일 전";
  if (diffDays < 30) return `${diffDays}일 전`;
  if (diffDays < 365) return `${Math.floor(diffDays / 30)}개월 전`;
  return `${Math.floor(diffDays / 365)}년 전`;
}
