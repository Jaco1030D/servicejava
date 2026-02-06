package com.magmatranslation.xliffconverter.controllers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.magmatranslation.xliffconverter.BodyRequests.CreateXliffFromDocx;
import com.magmatranslation.xliffconverter.cli.DocxMain;
import com.magmatranslation.xliffconverter.cli.Main;
import com.magmatranslation.xliffconverter.exceptions.InvalidFileTypeException;
import com.magmatranslation.xliffconverter.services.storage.StorageFileNotFoundException;
import com.magmatranslation.xliffconverter.services.storage.StorageService;
import com.magmatranslation.xliffconverter.validators.FileValidator;
import com.magmatranslation.xliffconverter.validators.FileValidator.ValidationResult;

@RestController
@RequestMapping("/api")
public class MainController {
    
    private final StorageService storageService;
    private static final Map<String, String> FILE_TYPE_MAP = new HashMap<>();
    
    static {
        FILE_TYPE_MAP.put(".docx", "DOCX");
        FILE_TYPE_MAP.put(".doc", "DOCX");
        FILE_TYPE_MAP.put(".xlsx", "EXCEL");
        FILE_TYPE_MAP.put(".xls", "EXCEL");
        FILE_TYPE_MAP.put(".pptx", "PPTX");
        FILE_TYPE_MAP.put(".ppt", "PPTX");
    }

    @Autowired
    public MainController(StorageService storageService) {
        this.storageService = storageService;
    }
    
    @GetMapping("/")
    public Map<String, String> isWorking() {
        return Map.of("mensagem", "A API esta rodando!");
    }

    /**
     * Endpoint genérico para criar XLIFF a partir de qualquer arquivo Office
     * Suporta: DOCX, EXCEL, PPTX, etc.
     */
    @PostMapping("/create-xliff")
    public ResponseEntity<Resource> createXliff(
        @RequestParam("file") MultipartFile file,
        CreateXliffFromDocx events
    ) {
        // Detecta o tipo de arquivo automaticamente pela extensão
        String fileType = detectFileType(file.getOriginalFilename());
        
        if (fileType == null) {
            throw new InvalidFileTypeException("Tipo de arquivo não suportado. Formatos aceitos: DOCX, EXCEL, PPTX");
        }

        String[] customArgs = {
            fileType,                              // index 0 - typeFile (detectado automaticamente)
            "src/main/resource/p.fprm",           // index 1 - filePathParams
            "src/main/resource/p.srx",            // index 2 - filePathSegmentRules
            events.getLangSource(),                // index 3 - langSource
            events.getLangTarget(),                // index 4 - langTarget
            "",                                    // index 5 - getFilePath (será preenchido depois)
            "files",                               // index 6 - filePathOutput
            "CREATEFILEXLIFF"                      // index 7 - action
        };

        // Validação do tipo de arquivo
        ValidationResult validationResult = FileValidator.validateFileType(
            fileType,
            file.getOriginalFilename()
        );

        if (!validationResult.isValid()) {
            throw new InvalidFileTypeException(validationResult.getMessage());
        }

        storageService.store(file);

        String filePath = "upload-dir" + File.separator + file.getOriginalFilename();
        customArgs[5] = filePath;

        System.out.println("Criando XLIFF para arquivo tipo: " + fileType);

        // Método principal - usa DocxMain que funciona para todos os tipos OpenXML
        Resource resource = Main.createXLIFF(customArgs);
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalFilename() + ".xlf\"")
            .body(resource);
    }

    /**
     * Endpoint genérico para traduzir XLIFF e gerar arquivo traduzido
     * Recebe arquivo XLIFF e retorna o arquivo original traduzido
     */
    @PostMapping("/translate")
    public ResponseEntity<Resource> translate(
        @RequestParam("file") MultipartFile file,
        CreateXliffFromDocx events
    ) {
        boolean reduceFont = events.getReduceFont();
        
        String[] customArgs = {
            "XLIFF",                              // index 0 - typeFile
            "src/main/resource/p.fprm",           // index 1 - filePathParams
            "src/main/resource/p.srx",            // index 2 - filePathSegmentRules
            events.getLangSource(),                // index 3 - langSource
            events.getLangTarget(),                // index 4 - langTarget
            "",                                    // index 5 - getFilePath (será preenchido depois)
            "files" + File.separator + "OUTPUT" + File.separator + extractOriginalFileName(file.getOriginalFilename()), // index 6 - filePathOutput
            "CREATEFILEXLIFF"                      // index 7 - action
        };

        // Validação do tipo de arquivo (deve ser XLIFF)
        ValidationResult validationResult = FileValidator.validateFileType(
            "XLIFF",
            file.getOriginalFilename()
        );

        if (!validationResult.isValid()) {
            throw new InvalidFileTypeException(validationResult.getMessage());
        }

        storageService.store(file);

        String filePath = "upload-dir" + File.separator + file.getOriginalFilename();
        customArgs[5] = filePath;

        System.out.println("Traduzindo XLIFF com redução de fonte: " + reduceFont);

        // Método principal
        Resource resource = DocxMain.translateDocxWithXLIFF(customArgs, filePath, reduceFont);
        
        // Detecta a extensão do arquivo original a partir do XLIFF
        String originalExtension = detectOriginalExtensionFromXliff(file.getOriginalFilename());
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + extractOriginalFileName(file.getOriginalFilename()) + originalExtension + "\"")
            .body(resource);
    }

    /**
     * Detecta o tipo de arquivo pela extensão
     */
    private String detectFileType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return null;
        }
        
        String extension = filename.substring(lastDotIndex).toLowerCase();
        return FILE_TYPE_MAP.get(extension);
    }

    /**
     * Extrai o nome do arquivo original sem extensão
     */
    private String extractOriginalFileName(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "translated_file";
        }
        
        // Remove extensões .xlf ou .xliff
        String name = filename;
        if (name.endsWith(".xlf")) {
            name = name.substring(0, name.length() - 4);
        } else if (name.endsWith(".xliff")) {
            name = name.substring(0, name.length() - 6);
        }
        
        return name;
    }

    /**
     * Tenta detectar a extensão original do arquivo a partir do nome do XLIFF
     * Por padrão retorna .docx, mas pode ser melhorado para detectar outros tipos
     */
    private String detectOriginalExtensionFromXliff(String xliffFilename) {
        // Por enquanto retorna .docx como padrão
        // Isso pode ser melhorado lendo o XLIFF para detectar o tipo original
        return ".docx";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<String> handleInvalidFileType(InvalidFileTypeException exc) {
        return ResponseEntity.badRequest()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"error\": \"" + exc.getMessage() + "\"}");
    }
}
