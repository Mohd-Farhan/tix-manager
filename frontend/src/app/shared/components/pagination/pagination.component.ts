import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export type PageSizeOption = number | 'ALL';

/**
 * ==============================================================================================
 * REUSABLE COMPONENT: PaginationComponent (app-pagination)
 * ==============================================================================================
 *
 * Reusable table & list pagination toolbar adhering strictly to the Azure Professional
 * design token specifications for Light and Dark modes.
 *
 * Provides:
 * - Dynamic rows per page selector (e.g. 10, 20, 50, 100, 'ALL').
 * - Exact range computation ("Showing 1 to 20 of 145 items" or "Showing 1 to 145 of 145 items").
 * - Boundary-safe Next/Previous navigation with disabled states.
 */
@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pagination.component.html',
  styleUrl: './pagination.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PaginationComponent {
  @Input() currentPage: number = 1;
  @Input() pageSize: PageSizeOption = 10;
  @Input() totalItems: number = 0;
  @Input() pageSizeOptions: PageSizeOption[] = [10, 20, 50, 100, 'ALL'];
  @Input() itemLabel: string = 'items';

  @Output() pageChange = new EventEmitter<number>();
  @Output() pageSizeChange = new EventEmitter<PageSizeOption>();

  get totalPages(): number {
    if (this.pageSize === 'ALL') return 1;
    return Math.max(1, Math.ceil(this.totalItems / (Number(this.pageSize) || 10)));
  }

  get startItemIndex(): number {
    if (this.totalItems === 0) return 0;
    if (this.pageSize === 'ALL') return 1;
    return (this.currentPage - 1) * Number(this.pageSize) + 1;
  }

  get endItemIndex(): number {
    if (this.pageSize === 'ALL') return this.totalItems;
    return Math.min(this.currentPage * Number(this.pageSize), this.totalItems);
  }

  onSelectPageSize(size: string | number): void {
    const selected: PageSizeOption = size === 'ALL' ? 'ALL' : Number(size);
    if (selected !== this.pageSize) {
      this.pageSizeChange.emit(selected);
    }
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages && page !== this.currentPage) {
      this.pageChange.emit(page);
    }
  }

  prevPage(): void {
    if (this.currentPage > 1) {
      this.goToPage(this.currentPage - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.goToPage(this.currentPage + 1);
    }
  }
}
