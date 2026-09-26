export enum TicketStatus {
  OPEN = 'OPEN',
  IN_PROGRESS = 'IN_PROGRESS',
  RESOLVED = 'RESOLVED',
}

export enum TicketPriority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
}

export interface Ticket {
  id: number;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  customerId: number;
  customerUsername?: string;
  assignedAgentId?: number;
  assignedAgentName?: string;
  createdAt: string;
  updatedAt?: string;
  deleted: boolean;
  history?: TicketStatusHistory[];
  slaDueAt?: string;
  slaBreached?: boolean;
  escalated?: boolean;
  resolvedAt?: string;
  slaStatus?: 'OK' | 'WARNING' | 'BREACHED' | 'RESOLVED_MET' | 'RESOLVED_BREACHED';
  remainingSeconds?: number;
}

export interface TicketStatusHistory {
  id: number;
  ticketId: number;
  previousStatus: TicketStatus;
  newStatus: TicketStatus;
  changedById: number;
  changedByUsername: string;
  changedAt: string;
}

export interface SlaMetrics {
  totalActive: number;
  withinSla: number;
  nearBreach: number;
  breached: number;
  totalResolved: number;
  resolvedWithinSla: number;
  complianceRatePercent: number;
}
