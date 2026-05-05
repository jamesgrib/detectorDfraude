import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

type Status = "approved" | "rejected" | "pending";

const labels: Record<Status, string> = {
  approved: "Aprobada",
  rejected: "Rechazada",
  pending: "Pendiente",
};

export const StatusBadge = ({ status }: { status: Status }) => {
  return (
    <Badge
      className={cn(
        "text-xs font-semibold px-3 py-1 border-0",
        status === "approved" && "bg-status-approved-bg text-status-approved",
        status === "rejected" && "bg-status-rejected-bg text-status-rejected",
        status === "pending" && "bg-status-pending-bg text-status-pending"
      )}
    >
      {labels[status]}
    </Badge>
  );
};
