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
  assignedAgentId?: number;
  assignedAgentName?: string;
  createdAt: string;
  updatedAt?: string;
  deleted: boolean;
  history?: TicketStatusHistory[];
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
