package org.bahmni.module.hip.web.service;

import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.openmrs.DrugOrder;
import org.openmrs.EncounterProvider;
import org.openmrs.module.fhir2.api.translators.MedicationRequestTranslator;
import org.openmrs.module.fhir2.api.translators.MedicationTranslator;
import org.openmrs.module.fhir2.api.translators.PatientTranslator;
import org.openmrs.module.fhir2.api.translators.impl.EncounterTranslatorImpl;
import org.openmrs.module.fhir2.api.translators.impl.PractitionerTranslatorProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FHIRResourceMapper {

    private final PatientTranslator patientTranslator;
    private final PractitionerTranslatorProviderImpl practitionerTranslatorProvider;
    private final MedicationRequestTranslator medicationRequestTranslator;
    private final MedicationTranslator medicationTranslator;
    private final EncounterTranslatorImpl encounterTranslator;

    @Autowired
    public FHIRResourceMapper(PatientTranslator patientTranslator, PractitionerTranslatorProviderImpl practitionerTranslatorProvider, MedicationRequestTranslator medicationRequestTranslator, MedicationTranslator medicationTranslator, EncounterTranslatorImpl encounterTranslator) {
        this.patientTranslator = patientTranslator;
        this.practitionerTranslatorProvider = practitionerTranslatorProvider;
        this.medicationRequestTranslator = medicationRequestTranslator;
        this.medicationTranslator = medicationTranslator;
        this.encounterTranslator = encounterTranslator;
    }

    public Encounter mapToEncounter(org.openmrs.Encounter emrEncounter) {
        return encounterTranslator.toFhirResource(emrEncounter);
    }

    public Patient mapToPatient(org.openmrs.Patient emrPatient) {
        return patientTranslator.toFhirResource(emrPatient);
    }

    public Practitioner mapToPractitioner(EncounterProvider encounterProvider) {
        return practitionerTranslatorProvider.toFhirResource(encounterProvider.getProvider());
    }

    private String displayName(Object object) {
        if (object == null)
            return "";
        return object.toString() + " ";

    }

    public MedicationRequest mapToMedicationRequest(DrugOrder order) {
        String dosingInstrutions = displayName(order.getDose()) +
                displayName(order.getDoseUnits() == null ? "" : order.getDoseUnits().getName()) +
                displayName(order.getFrequency()) +
                displayName(order.getRoute() == null ? "" : order.getRoute().getName()) +
                displayName(order.getDuration()) +
                displayName(order.getDurationUnits() == null ? "" : order.getDurationUnits().getName());
        MedicationRequest medicationRequest = medicationRequestTranslator.toFhirResource(order);
        Dosage dosage = medicationRequest.getDosageInstruction().get(0);
        dosage.setText(dosingInstrutions);
        return medicationRequest;
    }

    public Medication mapToMedication(DrugOrder order) {
        if (order.getDrug() == null) {
            return null;
        }
        return medicationTranslator.toFhirResource(order.getDrug());
    }
}
