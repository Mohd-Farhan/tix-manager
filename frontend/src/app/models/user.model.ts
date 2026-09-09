export enum UserRole {
  CUSTOMER = 'CUSTOMER',
  SUPPORT_AGENT = 'SUPPORT_AGENT',
  ADMIN = 'ADMIN',
  SYSTEM_ADMIN = 'SYSTEM_ADMIN',
}

export interface User {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  deleted?: boolean;
  status?: 'active' | 'inactive';
  createdAt?: string;
}

export interface BulkUploadResult {
  totalRows: number;
  successCount: number;
  failureCount: number;
  errors: string[];
}

