import { formatPrice } from "@/lib/format";

export function PriceTag({ price }: { price: number }) {
  return <span className="text-xl font-bold tabular-nums text-foreground">{formatPrice(price)}</span>;
}
