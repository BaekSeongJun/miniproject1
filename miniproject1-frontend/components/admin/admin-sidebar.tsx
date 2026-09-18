"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { BarChart3, Flag, LayoutDashboard, Pill, ArrowLeft, Cross } from "lucide-react";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar";

const MENU_ITEMS = [
  { href: "/admin", label: "대시보드", icon: LayoutDashboard },
  { href: "/admin/reports", label: "제보 관리", icon: Flag },
  { href: "/admin/drugs", label: "약품 승인", icon: Pill },
  { href: "/admin/stats", label: "통계", icon: BarChart3 },
] as const;

export function AdminSidebar() {
  const pathname = usePathname();

  return (
    <Sidebar>
      <SidebarHeader className="gap-0 px-1 pt-3 pb-2">
        <div className="flex items-center gap-2 px-2 py-1">
          <span className="flex size-7 shrink-0 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
            <Cross className="size-4" strokeWidth={2.25} />
          </span>
          <div className="flex min-w-0 flex-col leading-none">
            <span className="truncate text-sm font-semibold tracking-tight text-sidebar-foreground">
              우리동네약값
            </span>
            <span className="truncate text-[0.6875rem] text-sidebar-foreground/55">관리자 콘솔</span>
          </div>
        </div>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel className="text-sidebar-foreground/45">메뉴</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu className="gap-0.5">
              {MENU_ITEMS.map((item) => {
                const isActive = item.href === "/admin" ? pathname === "/admin" : pathname.startsWith(item.href);
                return (
                  <SidebarMenuItem key={item.href}>
                    <SidebarMenuButton
                      isActive={isActive}
                      render={<Link href={item.href} />}
                      className={
                        isActive
                          ? "relative bg-sidebar-accent font-semibold text-sidebar-accent-foreground before:absolute before:top-1.5 before:bottom-1.5 before:left-0 before:w-0.5 before:rounded-full before:bg-sidebar-primary"
                          : "text-sidebar-foreground/65 transition-colors hover:text-sidebar-foreground"
                      }
                    >
                      <item.icon strokeWidth={1.75} />
                      <span>{item.label}</span>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                );
              })}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
      <SidebarFooter className="border-t border-sidebar-border pt-2">
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton render={<Link href="/" />} className="text-sidebar-foreground/65 hover:text-sidebar-foreground">
              <ArrowLeft strokeWidth={1.75} />
              <span>사용자 화면으로</span>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
    </Sidebar>
  );
}
