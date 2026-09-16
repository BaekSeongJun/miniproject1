"use client";

import { useState } from "react";
import { useAppDispatch } from "@/lib/hooks";
import { regionSelected } from "@/lib/slices/location-slice";
import { useRegions } from "@/hooks/use-regions";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

interface RegionPickerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function RegionPicker({ open, onOpenChange }: RegionPickerProps) {
  const dispatch = useAppDispatch();
  const { data, isLoading, isError, refetch } = useRegions();
  const [selectedSido, setSelectedSido] = useState<string | null>(null);
  const [selectedSigunguCode, setSelectedSigunguCode] = useState<string | null>(null);

  const sigungus = data?.find((g) => g.sido === selectedSido)?.sigungus ?? [];
  const selectedSigungu = sigungus.find((s) => s.code === selectedSigunguCode);

  function handleConfirm() {
    if (!selectedSigungu) return;
    dispatch(
      regionSelected({
        lat: selectedSigungu.centerLat,
        lng: selectedSigungu.centerLng,
        regionCode: selectedSigungu.code,
      }),
    );
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>지역 선택</DialogTitle>
        </DialogHeader>

        {isLoading && <Skeleton className="h-20 w-full" />}

        {isError && (
          <div className="flex flex-col items-center gap-2 py-4 text-sm text-muted-foreground">
            <p>지역 목록을 불러오지 못했습니다.</p>
            <Button variant="outline" size="sm" onClick={() => refetch()}>
              다시 시도
            </Button>
          </div>
        )}

        {data && (
          <div className="flex flex-col gap-3">
            <Select
              value={selectedSido}
              onValueChange={(value) => {
                setSelectedSido(value);
                setSelectedSigunguCode(null);
              }}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder="시/도 선택" />
              </SelectTrigger>
              <SelectContent>
                {data.map((group) => (
                  <SelectItem key={group.sido} value={group.sido}>
                    {group.sido}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select
              value={selectedSigunguCode}
              onValueChange={setSelectedSigunguCode}
              disabled={!selectedSido}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder="시/군/구 선택" />
              </SelectTrigger>
              <SelectContent>
                {sigungus.map((s) => (
                  <SelectItem key={s.code} value={s.code} disabled={s.pharmacyCount === 0}>
                    {s.sigungu}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        )}

        <DialogFooter>
          <Button onClick={handleConfirm} disabled={!selectedSigungu}>
            확인
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
