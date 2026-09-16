import { formatPrice } from "@/lib/format";

export function PriceTag({ price }: { price: number }) {
  return <span className="font-semibold tabular-nums">{formatPrice(price)}</span>;
}
