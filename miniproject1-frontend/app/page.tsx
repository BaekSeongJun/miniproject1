import Link from "next/link";
import { DrugAutocomplete } from "@/components/drug-autocomplete";
import { Button } from "@/components/ui/button";

export default function Home() {
  return (
    <div className="flex-1 bg-secondary/40">
      <div className="mx-auto flex w-full max-w-2xl flex-col items-center gap-8 px-4 py-16 text-center md:py-24">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-balance md:text-5xl">
            우리 동네 약 가격, 한눈에
          </h1>
          <p className="mt-3 text-base text-muted-foreground md:text-lg">
            찾는 약을 검색하면 근처 약국 가격을 비교해드려요.
          </p>
        </div>
        <DrugAutocomplete />

        <div className="mt-4 flex w-full max-w-md items-center justify-between gap-4 rounded-2xl border-2 border-border bg-card px-6 py-4 text-left shadow-sm">
          <div>
            <p className="text-sm font-semibold">내가 산 약, 가격 공유하기</p>
            <p className="mt-0.5 text-xs text-muted-foreground">30초면 충분해요. 다른 이웃에게 도움이 돼요.</p>
          </div>
          <Button nativeButton={false} render={<Link href="/reports/new" />} className="shrink-0">
            제보하기
          </Button>
        </div>
      </div>
    </div>
  );
}
