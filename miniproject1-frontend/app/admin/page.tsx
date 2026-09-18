"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, MapPin, Pill, Radar, Target, Trophy } from "lucide-react";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { apiFetch } from "@/lib/api";
import { useRequireAdmin } from "@/hooks/use-require-admin";
import { ErrorState } from "@/components/error-state";
import { LoadingSkeleton } from "@/components/loading-skeleton";
import { toUserMessage } from "@/lib/error-message";
import type { AdminOverview, AdminTopPharmacies } from "@/types/admin";
import type { ComponentType } from "react";

function KpiCard({
  label,
  value,
  icon: Icon,
  href,
  accent = false,
}: {
  label: string;
  value: string;
  icon: ComponentType<{ className?: string; strokeWidth?: number }>;
  href?: string;
  accent?: boolean;
}) {
  const content = (
    <div className="card-elevated group flex w-44 shrink-0 flex-col gap-3 rounded-xl border border-border bg-card p-4 transition-all sm:w-auto hover:-translate-y-0.5 hover:border-primary/40">
      <div className="flex items-center justify-between gap-2">
        <span className="admin-eyebrow truncate">{label}</span>
        <span
          className={
            accent
              ? "flex size-7 shrink-0 items-center justify-center rounded-md bg-primary/15 text-primary"
              : "flex size-7 shrink-0 items-center justify-center rounded-md bg-muted text-muted-foreground"
          }
        >
          <Icon className="size-3.5" strokeWidth={1.75} />
        </span>
      </div>
      <p className="truncate font-mono text-xl leading-none font-semibold tracking-tight tabular-nums sm:text-2xl lg:text-[1.75rem]">
        {value}
      </p>
    </div>
  );
  return href ? (
    <Link href={href} className="rounded-xl focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50">
      {content}
    </Link>
  ) : (
    content
  );
}

function TrendTooltip({
  active,
  payload,
}: {
  active?: boolean;
  payload?: { payload: { date: string; reportCount: number } }[];
}) {
  if (!active || !payload?.length) return null;
  const point = payload[0]!.payload;
  return (
    <div className="card-elevated rounded-lg border border-border bg-popover px-3 py-2 text-xs text-popover-foreground">
      <p className="font-medium">{point.date}</p>
      <p className="mt-0.5 font-mono tabular-nums">제보 {point.reportCount.toLocaleString()}건</p>
    </div>
  );
}

export default function AdminDashboardPage() {
  const { ready } = useRequireAdmin();

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["admin", "stats", "overview"],
    queryFn: () => apiFetch<AdminOverview>("/api/v1/admin/stats/overview", { auth: true }),
    enabled: ready,
  });

  const topPharmacies = useQuery({
    queryKey: ["admin", "stats", "top-pharmacies", "overview"],
    queryFn: () => apiFetch<AdminTopPharmacies>("/api/v1/admin/stats/top-pharmacies?period=WEEK&limit=5", { auth: true }),
    enabled: ready,
  });

  if (!ready) return <LoadingSkeleton rows={5} />;

  return (
    <div className="flex flex-col gap-8 px-6 py-6">
      <div>
        <h1 className="admin-page-title">개요</h1>
        <p className="mt-1 text-sm text-muted-foreground">서비스 전반의 핵심 지표와 최근 제보 추이를 확인합니다.</p>
      </div>

      {isLoading && <LoadingSkeleton rows={5} />}

      {isError && (
        <ErrorState message={toUserMessage(error, "대시보드 데이터를 불러오지 못했습니다.")} onRetry={() => refetch()} />
      )}

      {data && (
        <>
          <div className="flex gap-3 overflow-x-auto pb-1 sm:grid sm:grid-cols-5 sm:overflow-visible sm:pb-0">
            <KpiCard label="약국 수" value={data.totals.pharmacyCount.toLocaleString()} icon={MapPin} />
            <KpiCard label="약품 수" value={data.totals.drugCount.toLocaleString()} icon={Pill} />
            <KpiCard label="제보 수" value={data.totals.reportCount.toLocaleString()} icon={Target} />
            <KpiCard label="커버리지" value={`${(data.coverageRate * 100).toFixed(0)}%`} icon={Radar} />
            <KpiCard
              label="이상치 제보"
              value={data.flaggedReportCount.toLocaleString()}
              icon={AlertTriangle}
              href="/admin/reports"
              accent
            />
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <div className="card-elevated rounded-xl border border-border bg-card p-5">
              <p className="admin-section-title mb-4">최근 7일 제보 추이</p>
              <div className="h-56 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={data.recentTrend} margin={{ top: 8, right: 8, left: 8, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border)" />
                    <XAxis
                      dataKey="date"
                      tick={{ fontSize: 11, fill: "var(--foreground)" }}
                      tickMargin={8}
                    />
                    <YAxis
                      tick={{ fontSize: 11, fill: "var(--foreground)" }}
                      width={32}
                      allowDecimals={false}
                    />
                    <Tooltip content={<TrendTooltip />} />
                    <Line
                      type="monotone"
                      dataKey="reportCount"
                      stroke="var(--primary)"
                      strokeWidth={2}
                      dot
                      isAnimationActive={false}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="card-elevated rounded-xl border border-border bg-card p-5">
              <p className="admin-section-title mb-4 flex items-center gap-1.5">
                <Trophy className="size-4 text-muted-foreground" strokeWidth={1.75} />
                이번 주 제보 많은 약국
              </p>

              {topPharmacies.isLoading && <LoadingSkeleton rows={3} />}

              {topPharmacies.data && topPharmacies.data.rows.length === 0 && (
                <p className="py-6 text-center text-sm text-muted-foreground">표시할 데이터가 없습니다.</p>
              )}

              {topPharmacies.data && topPharmacies.data.rows.length > 0 && (
                <ol className="divide-y divide-border">
                  {topPharmacies.data.rows.map((row, i) => (
                    <li
                      key={row.pharmacy.id}
                      className="flex items-center justify-between gap-2 rounded-md px-1.5 py-2 text-sm transition-colors hover:bg-muted/50"
                    >
                      <span className="flex items-center gap-2 truncate">
                        <span
                          className={
                            i === 0
                              ? "w-5 shrink-0 text-center font-mono text-xs font-semibold text-primary"
                              : "w-5 shrink-0 text-center font-mono text-xs text-muted-foreground"
                          }
                        >
                          {i + 1}
                        </span>
                        <span className="truncate">{row.pharmacy.name}</span>
                      </span>
                      <span className="shrink-0 font-mono tabular-nums">{row.reportCount.toLocaleString()}</span>
                    </li>
                  ))}
                </ol>
              )}

              <Link href="/admin/stats" className="mt-3 block text-xs text-primary hover:underline">
                전체 랭킹 보기 →
              </Link>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
