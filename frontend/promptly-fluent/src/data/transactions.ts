export interface Transaction {
  id: string;
  date: string;
  fromAccount: string;
  toAccount: string;
  amount: number;
  status: "approved" | "rejected" | "pending";
  fromName: string;
  toName: string;
}

export const MOCK_TRANSACTIONS: Transaction[] = [
  { id: "TXN-001", date: "2026-03-25 14:32", fromAccount: "1000-0002", toAccount: "1000-0045", amount: 1250.0, status: "approved", fromName: "María García", toName: "Juan Pérez" },
  { id: "TXN-002", date: "2026-03-25 10:15", fromAccount: "1000-0002", toAccount: "1000-0078", amount: 500.5, status: "pending", fromName: "María García", toName: "Ana López" },
  { id: "TXN-003", date: "2026-03-24 16:45", fromAccount: "1000-0002", toAccount: "1000-0012", amount: 3200.0, status: "rejected", fromName: "María García", toName: "Pedro Ruiz" },
  { id: "TXN-004", date: "2026-03-24 09:20", fromAccount: "1000-0003", toAccount: "1000-0002", amount: 750.0, status: "approved", fromName: "Luis Torres", toName: "María García" },
  { id: "TXN-005", date: "2026-03-23 11:00", fromAccount: "1000-0005", toAccount: "1000-0009", amount: 4100.0, status: "pending", fromName: "Sofia Martín", toName: "Diego Flores" },
  { id: "TXN-006", date: "2026-03-22 15:30", fromAccount: "1000-0008", toAccount: "1000-0001", amount: 980.25, status: "approved", fromName: "Elena Ruiz", toName: "Carlos Admin" },
  { id: "TXN-007", date: "2026-03-22 08:10", fromAccount: "1000-0010", toAccount: "1000-0003", amount: 2150.0, status: "pending", fromName: "Roberto Díaz", toName: "Luis Torres" },
];
