import Link from "next/link";
import { Clock, MapPin, Navigation, Phone } from "lucide-react";
import { apiFetch, ApiError } from "@/lib/api";
import { EmptyState } from "@/components/empty-state";
import { ErrorState } from "@/components/error-state";
import { toUserMessage } from "@/lib/error-message";
import { DistanceBadge } from "@/components/distance-badge";
import { BusinessHours } from "@/components/business-hours";
import { DrugPriceList } from "@/components/drug-price-list";
import { PharmacyMiniMap } from "@/components/pharmacy-mini-map";
import { Button } from "@/components/ui/button";
import type { PharmacyDetail } from "@/types/pharmacy";

export default async function PharmacyDetailPage(props: PageProps<"/pharmacies/[id]">) {
  const { id } = await props.params;
  const sp = await props.searchParams;
  const lat = first(sp.lat);
  const lng = first(sp.lng);

  const params = new URLSearchParams();
  if (lat && lng) {
    params.set("lat", lat);
    params.set("lng", lng);
  }
  const query = params.toString();

  let pharmacy: PharmacyDetail;
  try {
    pharmacy = await apiFetch<PharmacyDetail>(`/api/v1/pharmacies/${id}${query ? `?${query}` : ""}`, {
      cache: "no-store",
    });
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) {
      return <EmptyState message="약국 정보를 찾을 수 없습니다." />;
    }
    return <ErrorState message={toUserMessage(err, "약국 정보를 불러오지 못했습니다.")} />;
  }

  const directionsUrl = `https://map.kakao.com/link/to/${encodeURIComponent(pharmacy.name)},${pharmacy.lat},${pharmacy.lng}`;

  return (
    <div className="mx-auto w-full max-w-5xl px-4 py-8 md:py-12">
      <div className="grid gap-6 md:grid-cols-[19rem_1fr] md:items-start md:gap-10">
        {/* 요약 카드: 데스크탑에서 좌측 고정, 이름/주소/거리/영업시간/액션을 한 곳에 모음 */}
        <aside className="flex flex-col gap-5 rounded-3xl border border-border bg-card p-6 card-elevated md:sticky md:top-28">
          <div>
            <div className="flex items-start justify-between gap-2">
              <h1 className="text-xl font-bold tracking-tight text-balance">{pharmacy.name}</h1>
              {pharmacy.distanceM != null && <DistanceBadge meters={pharmacy.distanceM} />}
            </div>
            <p className="mt-2 flex items-start gap-1.5 text-sm text-muted-foreground">
              <MapPin className="mt-0.5 size-4 shrink-0" strokeWidth={1.75} />
              <span>
                {pharmacy.addressRoad}
                {pharmacy.addressJibun && (
                  <span className="block text-xs text-muted-foreground/70">
                    ({pharmacy.addressJibun})
                  </span>
                )}
              </span>
            </p>
          </div>

          <PharmacyMiniMap lat={pharmacy.lat} lng={pharmacy.lng} />

          <div className="flex flex-col gap-2">
            <Button
              nativeButton={false}
              render={<a href={directionsUrl} target="_blank" rel="noreferrer" />}
              className="justify-start"
            >
              <Navigation className="size-4" strokeWidth={1.75} />
              길찾기
            </Button>
            {pharmacy.phone && (
              <Button
                variant="outline"
                nativeButton={false}
                render={<a href={`tel:${pharmacy.phone}`} />}
                className="justify-start"
              >
                <Phone className="size-4" strokeWidth={1.75} />
                {pharmacy.phone}
              </Button>
            )}
          </div>

          <div className="border-t border-border pt-5">
            <h2 className="mb-3 flex items-center gap-1.5 text-sm font-semibold">
              <Clock className="size-4 text-muted-foreground" strokeWidth={1.75} />
              영업시간
            </h2>
            {pharmacy.businessHours ? (
              <BusinessHours hours={pharmacy.businessHours} />
            ) : (
              <p className="text-sm text-muted-foreground">등록된 영업시간 정보가 없습니다.</p>
            )}
          </div>
        </aside>

        {/* 약품 가격: 우측 넓은 영역에 스캔하기 쉽게 배치 */}
        <section className="min-w-0">
          <div className="mb-3 flex items-center justify-between gap-2">
            <div>
              <h2 className="text-lg font-bold tracking-tight">취급 약품 가격</h2>
              <p className="mt-0.5 text-sm text-muted-foreground">
                {pharmacy.drugPrices.length > 0 ? `${pharmacy.drugPrices.length}개 약품 등록됨` : "학습용 예시 데이터를 포함합니다"}
              </p>
            </div>
            <Button size="sm" nativeButton={false} render={<Link href={`/reports/new?pharmacyId=${pharmacy.id}`} />}>
              가격 제보하기
            </Button>
          </div>
          {pharmacy.drugPrices.length === 0 ? (
            <p className="rounded-2xl border border-dashed border-border py-12 text-center text-sm text-muted-foreground">
              등록된 가격 정보가 없습니다.
            </p>
          ) : (
            <DrugPriceList pharmacyId={pharmacy.id} drugPrices={pharmacy.drugPrices} />
          )}
        </section>
      </div>
    </div>
  );
}

function first(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
