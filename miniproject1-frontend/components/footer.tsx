import { NoticeBanner } from "@/components/notice-banner";

export function Footer() {
  return (
    <footer className="mt-auto">
      <NoticeBanner />
      <p className="px-4 py-3 text-center text-xs text-muted-foreground">
        © {new Date().getFullYear()} 약가비교
      </p>
    </footer>
  );
}
