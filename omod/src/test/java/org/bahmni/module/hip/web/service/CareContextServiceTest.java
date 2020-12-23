package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.api.dao.CareContextRepository;
import org.bahmni.module.hip.model.PatientCareContext;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class CareContextServiceTest {
    private CareContextRepository careContextRepository = mock(CareContextRepository.class);

    private CareContextService careContextServiceObject = new CareContextService(careContextRepository);

    @Test
    public void shouldFetchAllCareContextForPatient() {
        String patientUuid = "c04fa14e-9997-4bfe-80a2-9c474b94dd8a";
        List<PatientCareContext> careContexts = new ArrayList<>();

        when(careContextRepository.getPatientCareContext(patientUuid))
                .thenReturn(careContexts);

        careContextServiceObject.careContextForPatient(patientUuid);

        verify(careContextRepository, times(1))
                .getPatientCareContext(patientUuid);
    }
}
