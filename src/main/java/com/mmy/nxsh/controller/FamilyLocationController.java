package com.mmy.nxsh.controller;

import com.mmy.nxsh.common.ApiResponse;
import com.mmy.nxsh.controller.dto.EmergencyContactSaveRequest;
import com.mmy.nxsh.controller.dto.LastLocationDTO;
import com.mmy.nxsh.entity.EmergencyContact;
import com.mmy.nxsh.service.EmergencyContactService;
import com.mmy.nxsh.service.LocationSafetyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/family")
public class FamilyLocationController {

    private final LocationSafetyService locationSafetyService;
    private final EmergencyContactService emergencyContactService;

    public FamilyLocationController(LocationSafetyService locationSafetyService,
                                    EmergencyContactService emergencyContactService) {
        this.locationSafetyService = locationSafetyService;
        this.emergencyContactService = emergencyContactService;
    }

    @GetMapping("/location/last")
    public ApiResponse<LastLocationDTO> lastLocation(@RequestParam Long familyId,
                                                     @RequestParam Long elderlyId) {
        return ApiResponse.success(locationSafetyService.getLastLocationForFamily(familyId, elderlyId));
    }

    @GetMapping("/location/sos/list")
    public ApiResponse<List<LastLocationDTO>> sosLocationList(@RequestParam Long familyId,
                                                              @RequestParam Long elderlyId) {
        return ApiResponse.success(locationSafetyService.listSosLocationsForFamily(familyId, elderlyId));
    }

    @GetMapping("/emergency-contact/list")
    public ApiResponse<List<EmergencyContact>> listContacts(@RequestParam Long familyId,
                                                            @RequestParam Long elderlyId) {
        return ApiResponse.success(emergencyContactService.listForElderly(familyId, elderlyId));
    }

    @PostMapping("/emergency-contact")
    public ApiResponse<Long> addContact(@Valid @RequestBody EmergencyContactSaveRequest req) {
        return ApiResponse.success(emergencyContactService.addContact(req));
    }

    @PutMapping("/emergency-contact/{id}")
    public ApiResponse<Void> updateContact(@PathVariable Long id,
                                           @Valid @RequestBody EmergencyContactSaveRequest req) {
        emergencyContactService.updateContact(id, req);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/emergency-contact/{id}")
    public ApiResponse<Void> removeContact(@RequestParam Long familyId, @PathVariable Long id) {
        emergencyContactService.removeContact(familyId, id);
        return ApiResponse.success(null);
    }
}
