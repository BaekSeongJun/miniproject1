import { DrugAutocomplete } from "@/components/drug-autocomplete";

export default function Home() {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-6 px-4 py-16 text-center md:gap-8 md:py-24">
      <div>
        <h1 className="text-2xl font-semibold md:text-4xl">우리 동네 약 가격, 한눈에</h1>
        <p className="mt-1 text-sm text-muted-foreground md:mt-2 md:text-base">
          찾는 약을 검색하면 근처 약국 가격을 비교해드려요.
        </p>
      </div>
      <DrugAutocomplete />
    </div>
  );
}
