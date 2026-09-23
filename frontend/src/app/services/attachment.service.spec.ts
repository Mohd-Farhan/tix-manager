import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AttachmentService } from './attachment.service';
import { AttachmentResponse } from '../models/attachment.model';
import { environment } from '../../environments/environment';

describe('AttachmentService', () => {
  let service: AttachmentService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/tickets`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AttachmentService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AttachmentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('uploadAttachment — should perform multipart POST request', () => {
    const dummyFile = new File(['log data'], 'error.log', { type: 'text/plain' });
    const mockResponse: AttachmentResponse = {
      id: 1,
      ticketId: 10,
      messageId: 20,
      fileName: 'error.log',
      contentType: 'text/plain',
      fileSize: 8,
      fileSizeFormatted: '8 B',
      uploaderId: 3,
      uploaderName: 'Alice',
      createdAt: '2026-09-19T22:00:00',
      previewUrl: '/api/tickets/10/attachments/1/preview',
      downloadUrl: '/api/tickets/10/attachments/1/download',
    };

    service.uploadAttachment(10, dummyFile, 20).subscribe((res) => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(`${baseUrl}/10/attachments`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    req.flush(mockResponse);
  });

  it('getTicketAttachments — should perform GET request', () => {
    const mockList: AttachmentResponse[] = [
      {
        id: 1,
        ticketId: 10,
        fileName: 'screenshot.png',
        contentType: 'image/png',
        fileSize: 1024,
        fileSizeFormatted: '1.0 KB',
        uploaderId: 3,
        uploaderName: 'Alice',
        createdAt: '2026-09-19T22:00:00',
        previewUrl: '/api/tickets/10/attachments/1/preview',
        downloadUrl: '/api/tickets/10/attachments/1/download',
      },
    ];

    service.getTicketAttachments(10).subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].fileName).toBe('screenshot.png');
    });

    const req = httpMock.expectOne(`${baseUrl}/10/attachments`);
    expect(req.request.method).toBe('GET');
    req.flush(mockList);
  });

  it('deleteAttachment — should perform DELETE request', () => {
    service.deleteAttachment(10, 1).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/10/attachments/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('isImage & isPdf — should identify content types accurately', () => {
    const imgAtt: AttachmentResponse = {
      id: 1, ticketId: 10, fileName: 'shot.png', contentType: 'image/png',
      fileSize: 100, fileSizeFormatted: '100 B', uploaderId: 1, uploaderName: 'A',
      createdAt: '', previewUrl: '', downloadUrl: ''
    };
    const pdfAtt: AttachmentResponse = {
      id: 2, ticketId: 10, fileName: 'doc.pdf', contentType: 'application/pdf',
      fileSize: 200, fileSizeFormatted: '200 B', uploaderId: 1, uploaderName: 'A',
      createdAt: '', previewUrl: '', downloadUrl: ''
    };
    const logAtt: AttachmentResponse = {
      id: 3, ticketId: 10, fileName: 'dump.log', contentType: 'text/plain',
      fileSize: 300, fileSizeFormatted: '300 B', uploaderId: 1, uploaderName: 'A',
      createdAt: '', previewUrl: '', downloadUrl: ''
    };

    expect(service.isImage(imgAtt)).toBe(true);
    expect(service.isImage(pdfAtt)).toBe(false);
    expect(service.isPdf(pdfAtt)).toBe(true);
    expect(service.isPdf(logAtt)).toBe(false);
  });
});
