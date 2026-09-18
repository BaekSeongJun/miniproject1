import { NoticeBanner } from "@/components/notice-banner";

export function Footer() {
  return (
    <footer className="mt-auto border-t-2 border-border bg-card">
      <NoticeBanner />
      <p className="px-4 py-3 text-center text-xs text-muted-foreground">
        © {new Date().getFullYear()} 우리동네약값
      </p>
    </footer>
  );
}
