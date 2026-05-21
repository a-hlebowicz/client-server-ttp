import { TestBed } from '@angular/core/testing';

import { ClientApi } from './client-api';

describe('ClientApi', () => {
  let service: ClientApi;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ClientApi);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
