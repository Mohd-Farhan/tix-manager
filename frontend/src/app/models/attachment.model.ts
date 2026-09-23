export interface AttachmentResponse {
  id: number;
  ticketId: number;
  messageId?: number | null;
  fileName: string;
  contentType: string;
  fileSize: number;
  fileSizeFormatted: string;
  uploaderId: number;
  uploaderName: string;
  createdAt: string;
  previewUrl: string;
  downloadUrl: string;
}
