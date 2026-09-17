import Link from "next/link";
import { DrugAutocomplete } from "@/components/drug-autocomplete";
import { Button } from "@/components/ui/button";

export default function Home() {
  return (
    <div className="mx-auto grid w-full max-w-4xl flex-1 grid-cols-1 gap-6 px-4 py-16 md:grid-cols-2 md:py-24">
      <div className="flex flex-col items-center justify-center gap-6 text-center md:gap-8">
        <div>
          <h1 className="text-2xl font-semibold md:text-4xl">우리 동네 약 가격, 한눈에</h1>
          <p className="mt-1 text-sm text-muted-foreground md:mt-2 md:text-base">
            찾는 약을 검색하면 근처 약국 가격을 비교해드려요.
          </p>
        </div>
        <DrugAutocomplete />
      </div>

      <div className="flex flex-col items-center justify-center gap-6 rounded-xl border border-dashed border-border px-4 py-10 text-center md:gap-8">
        <div>
          <h2 className="text-xl font-semibold md:text-2xl">가격 제보하기</h2>
          <p className="mt-1 text-sm text-muted-foreground md:mt-2 md:text-base">
            내가 산 약 가격을 공유하면 다른 사람에게 도움이 돼요.
          </p>
        </div>
        <Button nativeButton={false} render={<Link href="/reports/new" />}>
          제보하러 가기
        </Button>
      </div>
    </div>
  );
}
