/**
 * Generic Spring Data PageResponse representation.
 * Maps directly to org.springframework.data.domain.Page JSON serialization.
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
