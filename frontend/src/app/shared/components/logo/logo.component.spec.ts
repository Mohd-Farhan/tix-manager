import { TestBed } from '@angular/core/testing';
import { LogoComponent } from './logo.component';

describe('LogoComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LogoComponent],
    }).compileComponents();
  });

  it('should create the logo component', () => {
    const fixture = TestBed.createComponent(LogoComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it('should render full logo image by default', () => {
    const fixture = TestBed.createComponent(LogoComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const img = compiled.querySelector('img.logo-image');
    expect(img).toBeTruthy();
  });

  it('should render icon container when variant is icon', () => {
    const fixture = TestBed.createComponent(LogoComponent);
    fixture.componentRef.setInput('variant', 'icon');
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const icon = compiled.querySelector('.logo-icon-box');
    expect(icon).toBeTruthy();
  });
});
