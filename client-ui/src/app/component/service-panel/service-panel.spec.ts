import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServicePanel } from './service-panel';

describe('ServicePanel', () => {
  let component: ServicePanel;
  let fixture: ComponentFixture<ServicePanel>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServicePanel]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ServicePanel);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
