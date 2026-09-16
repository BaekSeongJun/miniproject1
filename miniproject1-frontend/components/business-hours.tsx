import type { BusinessHours as BusinessHoursType } from "@/types/pharmacy";

const DAY_LABEL: Record<keyof BusinessHoursType, string> = {
  mon: "월",
  tue: "화",
  wed: "수",
  thu: "목",
  fri: "금",
  sat: "토",
  sun: "일",
  holiday: "공휴일",
};

export function BusinessHours({ hours }: { hours: BusinessHoursType }) {
  return (
    <dl className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
      {(Object.keys(DAY_LABEL) as (keyof BusinessHoursType)[]).map((day) => (
        <div key={day} className="flex items-center justify-between">
          <dt className="text-muted-foreground">{DAY_LABEL[day]}</dt>
          <dd className="tabular-nums">{hours[day] ? hours[day]!.join(" – ") : "휴무"}</dd>
        </div>
      ))}
    </dl>
  );
}
