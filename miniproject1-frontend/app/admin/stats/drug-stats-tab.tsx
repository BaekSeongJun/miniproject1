"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { apiFetch } from "@/lib/api";
import { DrugAutocomplete, type DrugSummary } from "@/components/drug-autocomplete";
import { ErrorState } from "@/components/error-state";
import { LoadingSkeleton } from "@/components/loading-skeleton";
import { formatPrice } from "@/lib/format";
import { toUserMessage } from "@/lib/error-message";
import type { AdminDrugStats } from "@/types/admin";

export function DrugStatsTab() {
  const [drug, setDrug] = useState<DrugSummary | null>(null);

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["admin", "stats", "drug", drug?.id],
    queryFn: () => apiFetch<AdminDrugStats>(`/api/v1/admin/stats/drugs/${drug!.id}`, { auth: true }),
    enabled: drug !== null,
  });

  return (
    <div className="flex flex-col gap-4">
      <DrugAutocomplete onSelect={setDrug} />

      {!drug && <p className="py-12 text-center text-sm text-muted-foreground">약품을 선택하면 가격 분포를 볼 수 있습니다.</p>}

      {drug && isLoading && <LoadingSkeleton rows={5} />}

      {drug && isError && (
        <ErrorState message={toUserMessage(error, "약품 통계를 불러오지 못했습니다.")} onRetry={() => refetch()} />
      )}

      {data && (
        <>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
            <StatCard label="평균" value={formatPrice(Math.round(data.national.avg))} />
            <StatCard label="중앙값" value={formatPrice(Math.round(data.national.median))} />
            <StatCard label="최저가" value={formatPrice(data.national.min)} />
            <StatCard label="최고가" value={formatPrice(data.national.max)} />
            <StatCard label="표준편차" value={formatPrice(Math.round(data.national.stdDev))} />
          </div>

          <div className="card-elevated rounded-xl border border-border bg-card p-5">
            <p className="admin-section-title mb-4">가격 분포</p>
            <div className="h-56 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={data.distribution.map((b) => ({ ...b, label: `${formatPrice(b.bucketFrom)}~` }))}
                  margin={{ top: 8, right: 8, left: 8, bottom: 0 }}
                >
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border)" />
                  <XAxis
                    dataKey="label"
                    tick={{ fontSize: 10, fill: "var(--muted-foreground)" }}
                    tickMargin={8}
                  />
                  <YAxis
                    tick={{ fontSize: 11, fill: "var(--muted-foreground)" }}
                    width={32}
                    allowDecimals={false}
                  />
                  <Tooltip />
                  <Bar dataKey="count" fill="var(--primary)" isAnimationActive={false} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="card-elevated rounded-xl border border-border bg-card p-5">
            <p className="admin-section-title mb-4">지역별 평균가</p>
            <div className="h-56 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data.byRegion} margin={{ top: 8, right: 8, left: 8, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border)" />
                  <XAxis
                    dataKey="sigungu"
                    tick={{ fontSize: 10, fill: "var(--muted-foreground)" }}
                    tickMargin={8}
                  />
                  <YAxis
                    tick={{ fontSize: 11, fill: "var(--muted-foreground)" }}
                    width={48}
                    tickFormatter={(v: number) => formatPrice(v)}
                  />
                  <Tooltip content={<AvgPriceTooltip />} />
                  <Bar dataKey="avgPrice" fill="var(--primary)" isAnimationActive={false} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function AvgPriceTooltip({
  active,
  payload,
}: {
  active?: boolean;
  payload?: { payload: AdminDrugStats["byRegion"][number] }[];
}) {
  if (!active || !payload?.length) return null;
  const point = payload[0]!.payload;
  return (
    <div className="card-elevated rounded-lg border border-border bg-popover px-3 py-2 text-xs text-popover-foreground">
      <p className="font-medium">{point.sigungu}</p>
      <p className="font-mono tabular-nums">{formatPrice(point.avgPrice)}</p>
    </div>
  );
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="card-elevated rounded-xl border border-border bg-card p-4 transition-all hover:-translate-y-0.5 hover:border-primary/40">
      <p className="admin-eyebrow">{label}</p>
      <p className="mt-1.5 font-mono text-lg font-semibold tracking-tight tabular-nums">{value}</p>
    </div>
  );
}
