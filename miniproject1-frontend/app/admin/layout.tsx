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
            <SidebarTrigger className="m-2" />
            {children}
          </main>
        </SidebarProvider>
      </TooltipProvider>
    </div>
  );
}
