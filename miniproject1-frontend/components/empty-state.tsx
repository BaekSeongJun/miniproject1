export function EmptyState({ message }: { message: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 py-12 text-center text-muted-foreground">
      <p className="text-sm">{message}</p>
    </div>
  );
}
