package org.bahmni.module.hip.web.model;

import org.bahmni.module.hip.web.service.FHIRResourceMapper;
import org.bahmni.module.hip.web.service.FHIRUtils;
import org.hl7.fhir.r4.model.*;
import org.openmrs.module.bahmniemrapi.laborder.contract.LabOrderResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class FhirLabResult {

    private final Patient patient;
    private final Encounter encounter;
    private final Date encounterTime;
    private final String panelName;
    private final DiagnosticReport report;
    private final List<Observation> results;

    public FhirLabResult(Patient patient, String panelName, Encounter encounter, Date encounterTime, DiagnosticReport report, List<Observation> results) {
        this.patient = patient;
        this.panelName = panelName;
        this.encounter = encounter;
        this.encounterTime = encounterTime;
        this.report = report;
        this.results = results;
    }

    public Bundle bundleLabResults (String webUrl, FHIRResourceMapper fhirResourceMapper) {
        String bundleID = String.format("LR-%s", encounter.getId());

        Bundle bundle = FHIRUtils.createBundle(encounterTime, bundleID, webUrl);

        FHIRUtils.addToBundleEntry(bundle, compositionFrom(webUrl), false);

        FHIRUtils.addToBundleEntry(bundle, patient, false);
        FHIRUtils.addToBundleEntry(bundle, report, false);
        FHIRUtils.addToBundleEntry(bundle, results, false);
        return bundle;

    }

    public static FhirLabResult fromOpenMrsLabResults(OpenMrsLabResults labresult, FHIRResourceMapper fhirResourceMapper) {
        Patient patient = fhirResourceMapper.mapToPatient(labresult.getPatient());

        DiagnosticReport reports = new DiagnosticReport();
        LabOrderResult firstresult = labresult.getLabOrderResults().get(0);

        reports.setId(firstresult.getOrderUuid());
        reports.setCode(new CodeableConcept().setText(firstresult.getPanelName()).addCoding(new Coding().setDisplay(firstresult.getPanelName())));
        reports.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        reports.setSubject(FHIRUtils.getReferenceToResource(patient));

        List<Observation> results = new ArrayList<>();

        labresult.getLabOrderResults().stream().forEach( result -> FhirLabResult.mapToObsFromLabResult(result, patient, reports, results) );

        FhirLabResult fhirLabResult = new FhirLabResult(fhirResourceMapper.mapToPatient( labresult.getPatient() ), labresult.getLabOrderResults().get(0).getPanelName(),
                fhirResourceMapper.mapToEncounter( labresult.getEncounter() ),
                labresult.getEncounter().getEncounterDatetime(), reports, results);

        return fhirLabResult;
    }

    private static void mapToObsFromLabResult(LabOrderResult result, Patient patient, DiagnosticReport report, List<Observation> observations) {

        Observation obs = new Observation();

        obs.setId(result.getTestUuid());
        obs.setCode(new CodeableConcept().setText( result.getTestName( )));
        try {
            Float f = Float.parseFloat(result.getResult());
            obs.setValue(new Quantity().setValue(f).setUnit(result.getTestUnitOfMeasurement()));
        } catch (NumberFormatException ex ) {
            obs.setValue(new StringType().setValue(result.getResult()));
        }
        obs.setStatus(Observation.ObservationStatus.FINAL);

        report.addResult(FHIRUtils.getReferenceToResource(obs));

        observations.add(obs);
    }

    private Composition compositionFrom(String webURL) {
        Composition composition = initializeComposition(encounterTime, webURL);
        Composition.SectionComponent compositionSection = composition.addSection();
        Reference patientReference = FHIRUtils.getReferenceToResource(patient);

        composition
                .setEncounter(FHIRUtils.getReferenceToResource(encounter))
                .setSubject(patientReference);

        compositionSection
                .setTitle("Diagnostic Report")
                .setCode(FHIRUtils.getDiagnosticReportType());

        compositionSection.addEntry(FHIRUtils.getReferenceToResource(report));

        return composition;
    }

    private Composition initializeComposition(Date encounterTimestamp, String webURL) {
        Composition composition = new Composition();

        composition.setId(UUID.randomUUID().toString());
        composition.setDate(encounterTimestamp);
        composition.setIdentifier(FHIRUtils.getIdentifier(composition.getId(), webURL, "document"));
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.setType(FHIRUtils.getDiagnosticReportType());
        composition.setTitle("Diagnostic Report");
        return composition;
    }
}
