package com.birtprintserver.birtprintserver.controller;

import com.birtprintserver.birtprintserver.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    // ============================
    //   ENDPOINT 1 - MULTIPART
    // ============================
    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> generateReport(
            @RequestPart("templateFile") MultipartFile templateFile,
            @RequestPart("xmlData") MultipartFile xmlData) {

        System.out.println("Received template file: " + templateFile.getOriginalFilename());
        try {
            byte[] pdfBytes = reportService.generatePdf(templateFile, xmlData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }


    // ============================
    //   ENDPOINT 2 - BASE64 + TEXTO
    // ============================
    @PostMapping(
            value = "/generatewithbase64",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<byte[]> generateReportBase64(
            @RequestParam("templateFile") String templateBase64,
            @RequestParam("xmlData") String xmlDataText
    ) {

        try {
            byte[] pdfBytes = reportService.generateWithBase64(templateBase64, xmlDataText);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }

    
}
