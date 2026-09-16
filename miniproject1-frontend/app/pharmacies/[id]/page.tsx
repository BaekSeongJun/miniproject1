import { apiFetch, ApiError } from "@/lib/api";
import { EmptyState } from "@/components/empty-state";
import { DistanceBadge } from "@/components/distance-badge";
import { BusinessHours } from "@/components/business-hours";
import { DrugPriceRow } from "@/components/drug-price-row";
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
    const message = err instanceof ApiError && err.status === 404
      ? "약국 정보를 찾을 수 없습니다."
      : "약국 정보를 불러오지 못했습니다.";
    return <EmptyState message={message} />;
  }

  const directionsUrl = `https://map.kakao.com/link/to/${encodeURIComponent(pharmacy.name)},${pharmacy.lat},${pharmacy.lng}`;

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-6 px-4 py-6">
      <div>
        <div className="flex items-start justify-between gap-2">
          <div>
            <h1 className="text-lg font-semibold">{pharmacy.name}</h1>
            <p className="text-sm text-muted-foreground">{pharmacy.addressRoad}</p>
          </div>
          {pharmacy.distanceM != null && <DistanceBadge meters={pharmacy.distanceM} />}
        </div>

        <div className="mt-3 flex flex-wrap gap-2">
          {pharmacy.phone && (
            <Button variant="outline" size="sm" nativeButton={false} render={<a href={`tel:${pharmacy.phone}`} />}>
              {pharmacy.phone}
            </Button>
          )}
          <Button
            variant="outline"
            size="sm"
            nativeButton={false}
            render={<a href={directionsUrl} target="_blank" rel="noreferrer" />}
          >
            길찾기
          </Button>
        </div>
      </div>

      {pharmacy.businessHours && (
        <section>
          <h2 className="mb-2 text-sm font-semibold">영업시간</h2>
          <BusinessHours hours={pharmacy.businessHours} />
        </section>
      )}

      <section>
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-sm font-semibold">취급 약품 가격</h2>
          <Button size="sm" disabled>
            가격 제보하기
          </Button>
        </div>
        {pharmacy.drugPrices.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">등록된 가격 정보가 없습니다.</p>
        ) : (
          <ul className="rounded-lg border">
            {pharmacy.drugPrices.map((drugPrice) => (
              <DrugPriceRow key={drugPrice.drugId} pharmacyId={pharmacy.id} drugPrice={drugPrice} />
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function first(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}
