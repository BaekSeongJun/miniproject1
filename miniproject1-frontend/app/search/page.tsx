import Link from "next/link";
import { apiFetch, ApiError } from "@/lib/api";
import { formatPrice } from "@/lib/format";
import { SearchResults } from "@/components/search-results";
import { SortToggle } from "@/components/sort-toggle";
import { EmptyState } from "@/components/empty-state";
import type { SearchResult } from "@/types/search";

export default async function SearchPage(props: PageProps<"/search">) {
  const sp = await props.searchParams;
  const drugId = first(sp.drugId);
  const lat = first(sp.lat);
  const lng = first(sp.lng);
  const regionCode = first(sp.regionCode);
  const radius = first(sp.radius) ?? "2000";
  const sort = first(sp.sort) ?? "SCORE";

  if (!drugId || (!(lat && lng) && !regionCode)) {
    return <EmptyState message="검색 조건이 올바르지 않습니다. 홈에서 다시 검색해주세요." />;
  }

  const params = new URLSearchParams({ drugId, radius, sort });
  if (lat && lng) {
    params.set("lat", lat);
    params.set("lng", lng);
  } else if (regionCode) {
    params.set("regionCode", regionCode);
  }

  let data: SearchResult;
  try {
    data = await apiFetch<SearchResult>(`/api/v1/search?${params.toString()}`, { cache: "no-store" });
  } catch (err) {
    const message = err instanceof ApiError ? err.message : "검색 결과를 불러오지 못했습니다.";
    return <EmptyState message={message} />;
  }

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-4 px-4 py-6">
      {data.dataSource !== "USER" && (
        <p className="rounded-lg bg-muted px-3 py-2 text-xs text-muted-foreground">
          이 검색 결과는 학습용 예시 데이터를 포함합니다.
        </p>
      )}

      <div>
        <h1 className="text-lg font-semibold">{data.drug.displayName}</h1>
        {data.drug.packageUnit && (
          <p className="text-sm text-muted-foreground">{data.drug.packageUnit}</p>
        )}
      </div>

      <SortToggle />

      {data.summary.resultCount === 0 ? (
        <EmptyResult drugId={drugId} lat={lat} lng={lng} regionCode={regionCode} sort={sort} suggestion={data.suggestion} />
      ) : (
        <>
          <p className="text-sm text-muted-foreground">
            반경 {formatRadius(data.query.radius)} 내 {data.summary.resultCount}곳
            {data.summary.maxSaving ? ` · 최대 ${formatPrice(data.summary.maxSaving)} 절약 가능` : ""}
          </p>
          <SearchResults data={data} />
        </>
      )}
    </div>
  );
}

function EmptyResult({
  drugId,
  lat,
  lng,
  regionCode,
  sort,
  suggestion,
}: {
  drugId: string;
  lat?: string;
  lng?: string;
  regionCode?: string;
  sort: string;
  suggestion?: SearchResult["suggestion"];
}) {
  if (!suggestion) {
    return <EmptyState message="반경 내에 결과가 없습니다." />;
  }

  const params = new URLSearchParams({ drugId, radius: String(suggestion.recommendedRadius), sort });
  if (lat && lng) {
    params.set("lat", lat);
    params.set("lng", lng);
  } else if (regionCode) {
    params.set("regionCode", regionCode);
  }

  return (
    <div className="flex flex-col items-center gap-3 py-12 text-center">
      <p className="text-sm text-muted-foreground">
        반경을 {formatRadius(suggestion.recommendedRadius)}로 넓히면 {suggestion.estimatedCount}곳이 있습니다.
      </p>
      <Link
        href={`/search?${params.toString()}`}
        className="rounded-lg border border-foreground px-4 py-2 text-sm font-medium hover:bg-muted"
      >
        반경 확대해서 다시 검색
      </Link>
    </div>
  );
}

function formatRadius(meters: number): string {
  return meters < 1000 ? `${meters}m` : `${meters / 1000}km`;
}

function first(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
