package org.bahmni.module.hip.web.controller;

import org.bahmni.module.hip.web.client.ClientError;
import org.bahmni.module.hip.web.model.BundledDiagnosticReportResponse;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.DiagnosticReportBundle;
import org.bahmni.module.hip.web.service.DiagnosticReportService;
import org.bahmni.module.hip.web.service.ValidationService;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.List;

import static org.bahmni.module.hip.web.utils.DateUtils.parseDate;

@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/hip/diagnosticReports")
@RestController
public class DiagnosticReportController extends BaseRestController {
    private final DiagnosticReportService diagnosticReportService;
    private final ValidationService validationService;

    @Autowired
    public DiagnosticReportController(DiagnosticReportService diagnosticReportService, ValidationService validationService) {
        this.diagnosticReportService = diagnosticReportService;
        this.validationService = validationService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/visit", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<?> get(@RequestParam String patientId,
                          @RequestParam String visitType,
                          @RequestParam String fromDate,
                          @RequestParam String toDate) throws ParseException {
        if (patientId == null || patientId.isEmpty())
            return ResponseEntity.badRequest().body(ClientError.noPatientIdProvided());
        if (visitType == null || visitType.isEmpty())
            return ResponseEntity.badRequest().body(ClientError.noVisitTypeProvided());
        if (!validationService.isValidVisit(visitType))
            return ResponseEntity.badRequest().body(ClientError.invalidVisitType());
        if (!validationService.isValidPatient(patientId))
            return ResponseEntity.badRequest().body(ClientError.invalidPatientId());
        List<DiagnosticReportBundle> diagnosticReportBundle =
                diagnosticReportService.getDiagnosticReportsForVisit(patientId, new DateRange(parseDate(fromDate), parseDate(toDate)), visitType);

        diagnosticReportBundle.addAll( diagnosticReportService.getLabResultsForVisits(patientId, new DateRange(parseDate(fromDate), parseDate(toDate)), visitType ));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(new BundledDiagnosticReportResponse(diagnosticReportBundle));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/program", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<?> get(
            @RequestParam String patientId,
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam String programName,
            @RequestParam String programEnrollmentId
    ) throws ParseException, UnsupportedEncodingException {
        programName = URLDecoder.decode(programName, "UTF-8");
        if (patientId == null || patientId.isEmpty())
            return ResponseEntity.badRequest().body(ClientError.noPatientIdProvided());
        if (programName == null || programName.isEmpty())
            return ResponseEntity.badRequest().body(ClientError.noVisitTypeProvided());
        if (!validationService.isValidProgram(programName))
            return ResponseEntity.badRequest().body(ClientError.invalidProgramName());
        if (!validationService.isValidPatient(patientId))
            return ResponseEntity.badRequest().body(ClientError.invalidPatientId());
        List<DiagnosticReportBundle> diagnosticReportBundles =
                diagnosticReportService.getDiagnosticReportsForProgram(patientId, new DateRange(parseDate(fromDate),
                        parseDate(toDate)), programName, programEnrollmentId);

        diagnosticReportBundles.addAll(
                diagnosticReportService.getLabResultsForPrograms(patientId, new DateRange(parseDate(fromDate),
                        parseDate(toDate)), programName, programEnrollmentId) );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(new BundledDiagnosticReportResponse(diagnosticReportBundles));
    }
}
