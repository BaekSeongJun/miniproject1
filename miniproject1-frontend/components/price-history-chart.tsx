"use client";

import { useQuery } from "@tanstack/react-query";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { apiFetch } from "@/lib/api";
import { formatPrice } from "@/lib/format";
import type { DrugPriceHistory, PriceHistoryPoint } from "@/types/pharmacy";

export function PriceHistoryChart({ pharmacyId, drugId }: { pharmacyId: number; drugId: number }) {
  const { data, isPending, error } = useQuery({
    queryKey: ["price-history", pharmacyId, drugId],
    queryFn: () =>
      apiFetch<DrugPriceHistory>(`/api/v1/pharmacies/${pharmacyId}/drugs/${drugId}/history`),
  });

  if (isPending) return <p className="py-6 text-center text-sm text-muted-foreground">이력을 불러오는 중...</p>;
  if (error) return <p className="py-6 text-center text-sm text-muted-foreground">이력을 불러오지 못했습니다.</p>;
  if (data.points.length === 0) {
    return <p className="py-6 text-center text-sm text-muted-foreground">가격 이력이 없습니다.</p>;
  }

  return (
    <div className="h-48 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data.points} margin={{ top: 8, right: 8, left: 8, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} />
          <XAxis dataKey="purchasedAt" tick={{ fontSize: 11 }} tickMargin={8} />
          <YAxis
            tick={{ fontSize: 11 }}
            width={56}
            tickFormatter={(value: number) => formatPrice(value)}
            domain={["auto", "auto"]}
          />
          <Tooltip content={<HistoryTooltip />} />
          <Line
            type="monotone"
            dataKey="price"
            stroke="var(--color-foreground)"
            strokeWidth={2}
            connectNulls
            dot={(props) => <HistoryDot key={props.index} {...props} />}
            isAnimationActive={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

function HistoryDot({ cx, cy, payload, index }: { cx?: number; cy?: number; payload?: PriceHistoryPoint; index?: number }) {
  if (cx == null || cy == null || !payload) return null;
  const flagged = payload.flagged;
  return (
    <circle
      key={index}
      cx={cx}
      cy={cy}
      r={4}
      fill={flagged ? "var(--color-muted-foreground)" : "var(--color-foreground)"}
      stroke={flagged ? "var(--color-muted-foreground)" : "none"}
      strokeDasharray={flagged ? "2 2" : undefined}
    />
  );
}

function HistoryTooltip({
  active,
  payload,
}: {
  active?: boolean;
  payload?: { payload: PriceHistoryPoint }[];
}) {
  if (!active || !payload?.length) return null;
  const point = payload[0]!.payload;
  return (
    <div className="rounded-lg border bg-background px-3 py-2 text-xs shadow-sm">
      <p className="font-medium">{point.purchasedAt}</p>
      <p className="tabular-nums">{formatPrice(point.price)}</p>
      {point.flagged && <p className="mt-1 text-muted-foreground">통계에서 제외된 제보</p>}
    </div>
  );
}
