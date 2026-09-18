import Link from "next/link";
import { apiFetch } from "@/lib/api";
import { formatPrice } from "@/lib/format";
import { SearchResults } from "@/components/search-results";
import { SortToggle } from "@/components/sort-toggle";
import { SearchDrugSwitcher } from "@/components/search-drug-switcher";
import { SearchLocationSync } from "@/components/search-location-sync";
import { EmptyState } from "@/components/empty-state";
import { ErrorState } from "@/components/error-state";
import { toUserMessage } from "@/lib/error-message";
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
    return <ErrorState message={toUserMessage(err, "검색 결과를 불러오지 못했습니다.")} />;
  }

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-5 px-4 py-6">
      <SearchLocationSync />

      {data.dataSource !== "USER" && (
        <p className="rounded-lg bg-muted px-3 py-2 text-xs text-muted-foreground">
          이 검색 결과는 학습용 예시 데이터를 포함합니다.
        </p>
      )}

      <SearchDrugSwitcher currentDrugName={data.drug.displayName} />

      <div>
        <h1 className="text-xl font-bold tracking-tight">{data.drug.displayName}</h1>
        {data.drug.packageUnit && (
          <p className="mt-0.5 text-sm text-muted-foreground">{data.drug.packageUnit}</p>
        )}
      </div>

      <SortToggle />

      {data.summary.resultCount === 0 ? (
        <EmptyResult drugId={drugId} lat={lat} lng={lng} regionCode={regionCode} sort={sort} suggestion={data.suggestion} />
      ) : (
        <>
          <p className="text-sm text-muted-foreground">
            반경 {formatRadius(data.query.radius)} 내 <span className="font-medium text-foreground">{data.summary.resultCount}곳</span>
            {data.summary.maxSaving ? (
              <>
                {" "}
                · 최대 <span className="font-semibold text-primary">{formatPrice(data.summary.maxSaving)}</span> 절약 가능
              </>
            ) : (
              ""
            )}
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
    <div className="flex flex-col items-center gap-4 py-16 text-center">
      <p className="text-sm text-muted-foreground">
        반경을 {formatRadius(suggestion.recommendedRadius)}로 넓히면 {suggestion.estimatedCount}곳이 있습니다.
      </p>
      <Link
        href={`/search?${params.toString()}`}
        className="rounded-full border border-primary px-4 py-2 text-sm font-medium text-primary transition-colors hover:bg-accent"
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
