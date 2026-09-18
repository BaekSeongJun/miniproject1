import type { ReactNode } from "react";
import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import { TooltipProvider } from "@/components/ui/tooltip";
import { AdminSidebar } from "@/components/admin/admin-sidebar";

export default function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <div className="dark">
      <TooltipProvider>
        <SidebarProvider>
          <AdminSidebar />
          <main className="flex-1 overflow-x-auto bg-background text-foreground">
            <div className="sticky top-0 z-20 flex h-12 items-center gap-2 border-b border-border/70 bg-background/95 px-3 backdrop-blur supports-backdrop-filter:bg-background/80">
              <SidebarTrigger />
            </div>
            <div className="mx-auto w-full max-w-[1400px]">{children}</div>
          </main>
        </SidebarProvider>
      </TooltipProvider>
    </div>
  );
}
