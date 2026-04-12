package com.mmy.nxsh.service;

import com.mmy.nxsh.controller.dto.LastLocationDTO;
import com.mmy.nxsh.controller.dto.LocationReportRequest;
import com.mmy.nxsh.controller.dto.SosReportResponse;

public interface LocationSafetyService {

    void reportShare(LocationReportRequest req);

    void reportActive(LocationReportRequest req);

    SosReportResponse reportSos(LocationReportRequest req);

    LastLocationDTO getLastLocationForFamily(Long familyId, Long elderlyId);

    java.util.List<LastLocationDTO> listSosLocationsForFamily(Long familyId, Long elderlyId);
}
