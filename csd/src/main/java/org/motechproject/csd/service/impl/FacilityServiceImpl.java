package org.motechproject.csd.service.impl;

import org.joda.time.DateTime;
import org.motechproject.csd.domain.Facility;
import org.motechproject.csd.mds.FacilityDataService;
import org.motechproject.csd.service.FacilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service("facilityService")
public class FacilityServiceImpl implements FacilityService {

    @Autowired
    private FacilityDataService facilityDataService;

    @Override
    public List<Facility> allFacilities() {
        return facilityDataService.retrieveAll();
    }

    @Override
    public void deleteAll() {
        facilityDataService.deleteAll();
    }

    @Override
    public Facility getFacilityByEntityID(String entityID) {
        return facilityDataService.findByEntityID(entityID);
    }

    @Override
    public void update(Facility facility) {
        delete(facility.getEntityID());
        facilityDataService.create(facility);
    }

    @Override
    public void delete(String entityID) {
        Facility facility = getFacilityByEntityID(entityID);
        if (facility != null) {
            facilityDataService.delete(facility);
        }
    }

    @Override
    public void update(Set<Facility> facilities) {
        for (Facility facility : facilities) {
            update(facility);
        }
    }

    @Override
    public Set<Facility> getModifiedAfter(DateTime date) {
        List<Facility> allFacilities = allFacilities();
        Set<Facility> modifiedFacilities = new HashSet<>();

        for (Facility facility : allFacilities) {
            if (facility.getRecord().getUpdated().isAfter(date)) {
                modifiedFacilities.add(facility);
            }
        }
        return modifiedFacilities;
    }
}
