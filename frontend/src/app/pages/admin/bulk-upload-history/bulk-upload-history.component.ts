import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UserService } from '../../../services/user.service';
import { BulkUploadHistoryItem, BulkUploadResult } from '../../../models/user.model';
import { ToastService } from '../../../services/toast.service';
import { SearchBoxComponent } from '../../../shared/components/search-box/search-box.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { ModalShellComponent } from '../../../shared/components/modal-shell/modal-shell.component';

@Component({
  selector: 'app-bulk-upload-history',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    SearchBoxComponent,
    EmptyStateComponent,
    ModalShellComponent
  ],
  templateUrl: './bulk-upload-history.component.html',
  styleUrl: './bulk-upload-history.component.css'
})
export class BulkUploadHistoryComponent implements OnInit {
  private userService = inject(UserService);
  private toast = inject(ToastService);
  private cdr = inject(ChangeDetectorRef);

  historyItems: BulkUploadHistoryItem[] = [];
  filteredItems: BulkUploadHistoryItem[] = [];
  isLoading = false;

  // Filter & search
  searchTerm = '';
  statusFilter = 'ALL';

  // Details Modal
  showErrorModal = false;
  selectedItem: BulkUploadHistoryItem | null = null;

  // Bulk Upload Modal
  showUploadModal = false;
  selectedFile: File | null = null;
  isUploading = false;
  uploadResult: BulkUploadResult | null = null;
  uploadError = '';

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    this.isLoading = true;
    this.userService.getBulkUploadHistory().subscribe({
      next: (data) => {
        this.historyItems = data || [];
        this.applyFilters();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
        this.toast.error(err.error?.message || 'Failed to load bulk upload history.');
        this.cdr.detectChanges();
      }
    });
  }

  applyFilters(): void {
    const term = this.searchTerm.trim().toLowerCase();
    this.filteredItems = this.historyItems.filter((item) => {
      const matchesSearch =
        !term ||
        item.fileName.toLowerCase().includes(term) ||
        item.uploadedBy.toLowerCase().includes(term) ||
        item.id.toString().includes(term);

      const matchesStatus =
        this.statusFilter === 'ALL' || item.status === this.statusFilter;

      return matchesSearch && matchesStatus;
    });
  }

  onSearch(term: string): void {
    this.searchTerm = term;
    this.applyFilters();
  }

  onStatusChange(): void {
    this.applyFilters();
  }

  // Summary Metrics
  get totalBatches(): number {
    return this.historyItems.length;
  }

  get totalUsersCreated(): number {
    return this.historyItems.reduce((acc, curr) => acc + (curr.successCount || 0), 0);
  }

  get totalFailedRows(): number {
    return this.historyItems.reduce((acc, curr) => acc + (curr.failureCount || 0), 0);
  }

  get overallSuccessRate(): string {
    const total = this.historyItems.reduce((acc, curr) => acc + (curr.totalRows || 0), 0);
    if (total === 0) return '100%';
    const rate = ((this.totalUsersCreated / total) * 100).toFixed(1);
    return `${rate}%`;
  }

  // Detail Modal Actions
  openErrorModal(item: BulkUploadHistoryItem): void {
    this.selectedItem = item;
    this.showErrorModal = true;
  }

  closeErrorModal(): void {
    this.showErrorModal = false;
    this.selectedItem = null;
  }

  // Upload Modal Actions
  openUploadModal(): void {
    this.showUploadModal = true;
    this.selectedFile = null;
    this.uploadResult = null;
    this.uploadError = '';
  }

  closeUploadModal(): void {
    this.showUploadModal = false;
    this.selectedFile = null;
    this.uploadResult = null;
    this.uploadError = '';
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.uploadResult = null;
      this.uploadError = '';
    }
  }

  executeUpload(): void {
    if (!this.selectedFile) return;

    this.isUploading = true;
    this.uploadError = '';
    this.uploadResult = null;

    this.userService.bulkUploadUsers(this.selectedFile).subscribe({
      next: (res) => {
        this.isUploading = false;
        this.uploadResult = res;
        this.toast.success(`Import complete: ${res.successCount} created, ${res.failureCount} failed.`);
        this.loadHistory();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isUploading = false;
        this.uploadError = err.error?.message || err.error || 'Upload failed.';
        this.cdr.detectChanges();
      }
    });
  }

  downloadSampleCsv(): void {
    const csvContent =
      'username,email,password,role\n' +
      'priya_agent,priya@tixmanager.com,,SUPPORT_AGENT\n' +
      'alex_dev,alex@example.com,,CUSTOMER\n';
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'sample_users.csv';
    link.click();
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'SUCCESS':
        return 'badge-success';
      case 'PARTIAL_SUCCESS':
        return 'badge-warning';
      case 'FAILED':
        return 'badge-error';
      default:
        return 'badge-neutral';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'SUCCESS':
        return 'Success';
      case 'PARTIAL_SUCCESS':
        return 'Partial Success';
      case 'FAILED':
        return 'Failed';
      default:
        return status;
    }
  }
}
