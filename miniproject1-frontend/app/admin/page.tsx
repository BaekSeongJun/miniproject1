"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { apiFetch } from "@/lib/api";
import { useRequireAdmin } from "@/hooks/use-require-admin";
import { ErrorState } from "@/components/error-state";
import { LoadingSkeleton } from "@/components/loading-skeleton";
import { toUserMessage } from "@/lib/error-message";
import type { AdminOverview } from "@/types/admin";

function KpiCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border p-4">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 text-2xl font-semibold tabular-nums">{value}</p>
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

  if (!ready) return <LoadingSkeleton rows={5} />;

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 px-4 py-6">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold">관리자 대시보드</h1>
        <div className="flex gap-4 text-sm text-muted-foreground">
          <Link href="/admin/reports" className="hover:text-foreground">
            제보 관리 →
          </Link>
          <Link href="/admin/stats" className="hover:text-foreground">
            통계 →
          </Link>
        </div>
      </div>

      {isLoading && <LoadingSkeleton rows={5} />}

      {isError && (
        <ErrorState message={toUserMessage(error, "대시보드 데이터를 불러오지 못했습니다.")} onRetry={() => refetch()} />
      )}

      {data && (
        <>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
            <KpiCard label="약국 수" value={data.totals.pharmacyCount.toLocaleString()} />
            <KpiCard label="약품 수" value={data.totals.drugCount.toLocaleString()} />
            <KpiCard label="제보 수" value={data.totals.reportCount.toLocaleString()} />
            <KpiCard label="커버리지" value={`${(data.coverageRate * 100).toFixed(0)}%`} />
            <KpiCard label="이상치 제보" value={data.flaggedReportCount.toLocaleString()} />
          </div>

          <div className="rounded-lg border p-4">
            <p className="mb-2 text-sm font-medium">최근 7일 제보 추이</p>
            <div className="h-56 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={data.recentTrend} margin={{ top: 8, right: 8, left: 8, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="date" tick={{ fontSize: 11 }} tickMargin={8} />
                  <YAxis tick={{ fontSize: 11 }} width={32} allowDecimals={false} />
                  <Tooltip />
                  <Line
                    type="monotone"
                    dataKey="reportCount"
                    stroke="var(--color-foreground)"
                    strokeWidth={2}
                    dot
                    isAnimationActive={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
