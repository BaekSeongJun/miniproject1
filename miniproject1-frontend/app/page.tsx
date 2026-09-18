import Link from "next/link";
import { MapPinned, ReceiptText, SearchCheck, TrendingDown } from "lucide-react";
import { DrugAutocomplete } from "@/components/drug-autocomplete";
import { Button } from "@/components/ui/button";

const STEPS = [
  {
    icon: SearchCheck,
    title: "약 이름을 검색해요",
    body: (
      <>
        타이레놀, 판콜에이처럼
        <br />
        찾는 약 이름만 입력하면 돼요.
      </>
    ),
  },
  {
    icon: MapPinned,
    title: "근처 약국을 비교해요",
    body: (
      <>
        내 위치 기준 반경 안
        <br />
        약국들의 가격을 나란히 보여드려요.
      </>
    ),
  },
  {
    icon: ReceiptText,
    title: "가격을 나눠요",
    body: (
      <>
        영수증 속 가격을 제보하면
        <br />
        다음 이웃에게 도움을 드릴 수 있습니다.
      </>
    ),
  },
];

const STATS = [
  { value: "2,481", label: "누적 가격 제보" },
  { value: "643곳", label: "등록된 약국" },
  { value: "18.4%", label: "같은 약, 최대 가격 차이" },
];

export default function Home() {
  return (
    <div className="relative flex-1 overflow-hidden bg-secondary/40">
      {/* 핵심 행동: 검색은 항상 최상단, 스크롤 없이 즉시 보이고 방해받지 않음 */}
      <section className="relative mx-auto flex w-full max-w-2xl flex-col items-center gap-9 px-4 pt-16 pb-14 text-center md:pt-24 md:pb-20">
        <div>
          <h1 className="text-4xl leading-[1.08] font-bold tracking-[-0.03em] text-balance md:text-6xl">
            우리 동네 약 가격,
            <br className="hidden sm:block" /> 한눈에
          </h1>
          <p className="mt-4 text-base text-muted-foreground md:text-lg">
            찾는 약을 검색하면 근처 약국 가격을 비교해드려요.
          </p>
        </div>
        <DrugAutocomplete />
      </section>

      {/* 가치 제안: 서비스가 뭘 하는지 3단계로 설명 (동일한 카드 반복이 아니라 연결된 흐름) */}
      <section className="relative border-t border-border/70 bg-card/60">
        <div className="mx-auto w-full max-w-5xl px-4 py-14 md:py-18">
          <h2 className="text-xl font-bold tracking-tight md:text-2xl">이렇게 가격을 비교해요</h2>
          <ol className="mt-8 grid gap-8 md:mt-10 md:grid-cols-3 md:gap-0">
            {STEPS.map((step, i) => (
              <li key={step.title} className="relative flex gap-4 md:flex-col md:gap-0 md:px-6">
                <div className="flex size-11 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary md:size-12">
                  <step.icon className="size-5 md:size-6" strokeWidth={1.75} />
                </div>
                <div className="md:mt-5">
                  <p className="font-semibold">
                    <span className="tabular-nums text-muted-foreground">{String(i + 1).padStart(2, "0")}</span>{" "}
                    {step.title}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">{step.body}</p>
                </div>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {/* 신뢰: 크라우드소싱 통계 + 제보 유도를 한 섹션으로 결합 */}
      <section className="relative border-t border-border/70">
        <div className="mx-auto grid w-full max-w-5xl gap-10 px-4 py-14 md:grid-cols-[1.1fr_0.9fr] md:items-center md:gap-16 md:py-20">
          <div>
            <div className="inline-flex items-center gap-1.5 text-sm font-medium text-primary">
              <TrendingDown className="size-4" strokeWidth={2} />
              이웃들이 직접 만든 가격 정보예요
            </div>
            <p className="mt-3 text-2xl font-bold tracking-tight text-balance md:text-3xl">
              같은 약도 약국마다
              <br className="hidden sm:block" /> 가격이 다르게 붙어요
            </p>
            <p className="mt-3 text-sm text-muted-foreground md:text-base">
              공식 가격 데이터베이스가 없는 일반의약품 특성상,
              <br />
              실제 구매 영수증을 나눈 이웃들의 제보가 가장 정확해요.
            </p>
            <dl className="mt-8 grid grid-cols-3 gap-4 border-t border-border pt-6">
              {STATS.map((stat) => (
                <div key={stat.label}>
                  <dt className="sr-only">{stat.label}</dt>
                  <dd className="text-2xl font-bold tracking-tight tabular-nums md:text-3xl">{stat.value}</dd>
                  <p className="mt-1 text-xs text-muted-foreground">{stat.label}</p>
                </div>
              ))}
            </dl>
          </div>

          <div className="flex flex-col gap-4 rounded-3xl border border-border bg-card p-6 text-left card-elevated md:p-8">
            <p className="text-sm font-semibold">내가 산 약, 가격 공유하기</p>
            <p className="text-sm text-muted-foreground">
              영수증만 있으면 30초면 충분해요.
              <br />
              내가 남긴 제보가 다음 이웃의 검색 결과가 돼요.
            </p>
            <Button
              nativeButton={false}
              render={<Link href="/reports/new" />}
              className="mt-2 self-start"
            >
              제보하기
            </Button>
          </div>
        </div>
      </section>
    </div>
  );
}
