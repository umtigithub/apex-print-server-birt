package com.birtprintserver.birtprintserver.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.Base64;

@Service
public class ReportService {

    private static final Path REPORT_ENGINE_DIR =
            Paths.get("repo", "birt-runtime-4.21.0", "ReportEngine").toAbsolutePath();
    private static final Path REPORT_SCRIPT = REPORT_ENGINE_DIR.resolve("genReport.sh");
    private static final Path REPORT_LIB_DIR = REPORT_ENGINE_DIR.resolve("lib");
    private static final Path REPORT_ADDONS_DIR = REPORT_ENGINE_DIR.resolve("addons");

    public byte[] generatePdf(MultipartFile templateFile, MultipartFile xmlData) throws Exception {
        validateRuntime();

        Path tempDir = Files.createTempDirectory("birt-report-");
        Path templatePath = tempDir.resolve("template.rptdesign");
        Path xmlPath = tempDir.resolve("data.xml");
        Path outputPdf = tempDir.resolve("report.pdf");

        try {
            Files.copy(templateFile.getInputStream(), templatePath, StandardCopyOption.REPLACE_EXISTING);
            byte[] xmlBytes = xmlData.getBytes();
            Files.write(xmlPath, xmlBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            syncXmlFileListReferences(templatePath, xmlBytes);

            runReportGenerator(templatePath, xmlPath, outputPdf);

            if (!Files.exists(outputPdf)) {
                throw new IllegalStateException("Geração do PDF não produziu arquivo de saída");
            }

            return Files.readAllBytes(outputPdf);
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    private void validateRuntime() {
        if (!Files.isDirectory(REPORT_ENGINE_DIR)) {
            throw new IllegalStateException("Diretório do BIRT Runtime não encontrado em " + REPORT_ENGINE_DIR);
        }
        if (!Files.isExecutable(REPORT_SCRIPT)) {
            REPORT_SCRIPT.toFile().setExecutable(true);
        }
        syncAddonJars();
    }

    private void runReportGenerator(Path templatePath, Path xmlPath, Path outputPdf) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "bash",
                REPORT_SCRIPT.toString(),
                "-f", "PDF",
                "-o", outputPdf.toString(),
                "-p", "xmlPath=" + xmlPath.toString(),
                "-c", "resourceDir=" + templatePath.getParent().toString(),
                templatePath.toString()
        );

        pb.directory(REPORT_ENGINE_DIR.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder log = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Falha ao executar ReportRunner (código " + exitCode + "):\n" + log);
        }
    }

    private void syncAddonJars() {
        if (!Files.isDirectory(REPORT_ADDONS_DIR) || !Files.isDirectory(REPORT_LIB_DIR)) {
            return;
        }

        try (Stream<Path> jars = Files.list(REPORT_ADDONS_DIR)) {
            jars.filter(path -> path.toString().endsWith(".jar"))
                    .forEach(path -> {
                        Path target = REPORT_LIB_DIR.resolve(path.getFileName().toString());
                        try {
                            Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new IllegalStateException("Falha ao sincronizar addon " + path.getFileName(), e);
                        }
                    });
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível listar os addons do BIRT", e);
        }
    }

    private void syncXmlFileListReferences(Path templatePath, byte[] xmlBytes) throws IOException {
        List<String> fileRefs = extractFileListReferences(templatePath);
        if (fileRefs.isEmpty()) {
            return;
        }

        Path baseDir = templatePath.getParent();
        for (String ref : fileRefs) {
            Path target = baseDir.resolve(ref);
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.write(target, xmlBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private List<String> extractFileListReferences(Path templatePath) throws IOException {
        List<String> refs = new ArrayList<>();
        try (var in = Files.newInputStream(templatePath)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            var builder = factory.newDocumentBuilder();
            Document document = builder.parse(in);
            NodeList propertyNodes = document.getElementsByTagNameNS("*", "property");
            for (int i = 0; i < propertyNodes.getLength(); i++) {
                Element property = (Element) propertyNodes.item(i);
                if (!"FILELIST".equals(property.getAttribute("name"))) {
                    continue;
                }
                String text = property.getTextContent();
                if (text == null || text.isBlank()) {
                    continue;
                }
                for (String entry : text.split(";")) {
                    String filename = entry.trim();
                    if (!filename.isEmpty() && !refs.contains(filename)) {
                        refs.add(filename);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível analisar o template BIRT: " + templatePath, e);
        }
        return refs;
    }

    private void cleanupTempDir(Path tempDir) {
        if (tempDir == null) {
            return;
        }

        try (Stream<Path> paths = Files.walk(tempDir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    public byte[] generateWithBase64(String templateBase64, String xmlText) throws Exception {
        validateRuntime();
    
        Path tempDir = Files.createTempDirectory("birt-report-");
        Path templatePath = tempDir.resolve("template.rptdesign");
        Path xmlPath = tempDir.resolve("data.xml");
        Path outputPdf = tempDir.resolve("report.pdf");
    
        try {
            // Decodifica o Base64 do template
            byte[] templateBytes = Base64.getDecoder().decode(templateBase64);
    
            // Salva o template em disco
            Files.write(templatePath, templateBytes, StandardOpenOption.CREATE);
    
            // Salva o XML
            byte[] xmlBytes = xmlText.getBytes(StandardCharsets.UTF_8);
            Files.write(xmlPath, xmlBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    
            // Sincroniza FILELIST se existir
            syncXmlFileListReferences(templatePath, xmlBytes);
    
            // Gera o PDF
            runReportGenerator(templatePath, xmlPath, outputPdf);
    
            // Retorna os bytes do PDF gerado
            return Files.readAllBytes(outputPdf);
        } finally {
            cleanupTempDir(tempDir);
        }
    }
}
