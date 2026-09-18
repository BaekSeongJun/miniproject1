"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "cn";
import { Button } from "@/components/ui/button";

const BLOCK_SIZE = 10;

export function Pagination({
  page,
  totalPages,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;

  const blockStart = Math.floor(page / BLOCK_SIZE) * BLOCK_SIZE;
  const blockEnd = Math.min(blockStart + BLOCK_SIZE, totalPages);
  const pages = Array.from({ length: blockEnd - blockStart }, (_, i) => blockStart + i);

  return (
    <div className="flex flex-wrap items-center justify-center gap-1">
      <Button
        variant="outline"
        size="icon-sm"
        aria-label="이전 블록"
        disabled={blockStart === 0}
        onClick={() => onPageChange(blockStart - 1)}
      >
        <ChevronLeft />
      </Button>
      {pages.map((p) => (
        <Button
          key={p}
          variant={p === page ? "default" : "ghost"}
          size="icon-sm"
          className={cn(
            "font-mono tabular-nums",
            p === page ? "font-semibold" : "text-muted-foreground hover:text-foreground",
          )}
          onClick={() => onPageChange(p)}
        >
          {p + 1}
        </Button>
      ))}
      <Button
        variant="outline"
        size="icon-sm"
        aria-label="다음 블록"
        disabled={blockEnd >= totalPages}
        onClick={() => onPageChange(blockEnd)}
      >
        <ChevronRight />
      </Button>
    </div>
  );
}
