export function LogoMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 32 32" fill="none" className={className} aria-hidden="true">
      <rect x="2" y="2" width="28" height="28" rx="8" fill="var(--primary)" />
      <path
        d="M16 8v16M8 16h16"
        stroke="var(--primary-foreground)"
        strokeWidth="4.5"
        strokeLinecap="round"
      />
    </svg>
  );
}
