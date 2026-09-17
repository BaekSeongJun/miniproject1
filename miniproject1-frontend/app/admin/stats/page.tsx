"use client";

import { useRequireAdmin } from "@/hooks/use-require-admin";
import { Tabs, TabsIndicator, TabsList, TabsPanel, TabsTab } from "@/components/ui/tabs";
import { RegionStatsTab } from "./region-stats-tab";
import { DrugStatsTab } from "./drug-stats-tab";
import { PriceGapsTab } from "./price-gaps-tab";

export default function AdminStatsPage() {
  const { ready } = useRequireAdmin();

  if (!ready) return null;

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 px-4 py-6">
      <h1 className="text-lg font-semibold">통계</h1>

      <Tabs defaultValue="region">
        <TabsList>
          <TabsIndicator />
          <TabsTab value="region">지역별 통계</TabsTab>
          <TabsTab value="drug">약품별 분포</TabsTab>
          <TabsTab value="gaps">가격 격차 Top 10</TabsTab>
        </TabsList>

        <TabsPanel value="region">
          <RegionStatsTab />
        </TabsPanel>
        <TabsPanel value="drug">
          <DrugStatsTab />
        </TabsPanel>
        <TabsPanel value="gaps">
          <PriceGapsTab />
        </TabsPanel>
      </Tabs>
    </div>
  );
}
