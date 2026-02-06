package com.magmatranslation.xliffconverter.controllers;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.magmatranslation.xliffconverter.BodyRequests.CreateXliffFromDocx;
import com.magmatranslation.xliffconverter.cli.DocxMain;
import com.magmatranslation.xliffconverter.exceptions.InvalidFileTypeException;
import com.magmatranslation.xliffconverter.services.storage.StorageFileNotFoundException;
import com.magmatranslation.xliffconverter.services.storage.StorageService;
import com.magmatranslation.xliffconverter.validators.FileValidator;
import com.magmatranslation.xliffconverter.validators.FileValidator.ValidationResult;


@RestController
@RequestMapping("/api")
public class DocxController {
    private final StorageService storageService;

    @Autowired
	public DocxController(StorageService storageService) {
		this.storageService = storageService;
	}

    
    @PostMapping("/docx/create-xliff")
    public ResponseEntity<Resource> handleFileUpload(
	@RequestParam("file") MultipartFile file, 
	CreateXliffFromDocx events
	) {

		String[] customArgs = {
            "DOCX",                           // index 0 - typeFile
            "src/main/resource/p.fprm",           // index 1 - filePathParams
            "src/main/resource/p.srx",            // index 2 - filePathSegmentRules
            events.getLangSource(),                        // index 3 - langSource
            events.getLangTarget(),                        // index 4 - langTarget
            "",         // index 5 - getFilePath (será preenchido depois)
            "files",            // index 6 - filePathOutput
            "CREATEFILEXLIFF"               // index 7 - action
        };

		// Validação do tipo de arquivo
		ValidationResult validationResult = FileValidator.validateFileType(
			customArgs[0], 
			file.getOriginalFilename()
		);

		if (!validationResult.isValid()) {
			throw new InvalidFileTypeException(validationResult.getMessage());
		}

		storageService.store(file);

		String filePath = "upload-dir" + File.separator + file.getOriginalFilename();
		customArgs[5] = filePath; // Atualiza o filePath após validação

        System.out.println("O metodo de criar XLIFF foi chamado");

        //metodo principal
		Resource resource = DocxMain.createXLIFF(customArgs);
		
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalFilename() + ".xlf\"")
			.body(resource);


	}


    @PostMapping("/docx/translate")
    public ResponseEntity<Resource> translateDocxWithXLIFF(
        @RequestParam("file") MultipartFile file,
        // @RequestParam(value = "reduceFont", defaultValue = "true") boolean reduceFont,
	    CreateXliffFromDocx events
    ) {

		boolean reduceFont = events.getReduceFont();
        System.out.println(events.getLangTarget());
        System.out.println("Redução de fonte ativada: " + reduceFont);

		String[] customArgs = {
            "XLIFF",                           // index 0 - typeFile
            "src/main/resource/p.fprm",           // index 1 - filePathParams
            "src/main/resource/p.srx",            // index 2 - filePathSegmentRules
            events.getLangSource(),                        // index 3 - langSource
            events.getLangTarget(),                        // index 4 - langTarget
            "",         // index 5 - getFilePath (será preenchido depois)
			"files" + File.separator + "DOCX" + File.separator + file.getOriginalFilename() + ".docx",            // index 6 - filePathOutput
            "CREATEFILEXLIFF"               // index 7 - action
        };

		// Validação do tipo de arquivo
		ValidationResult validationResult = FileValidator.validateFileType(
			customArgs[0], 
			file.getOriginalFilename()
		);

		if (!validationResult.isValid()) {
			throw new InvalidFileTypeException(validationResult.getMessage());
		}

		storageService.store(file);

		String filePath = "upload-dir" + File.separator + file.getOriginalFilename();
		customArgs[5] = filePath; // Atualiza o filePath após validação

        System.out.println("O metodo translateDocxWithXLIFF foi chamado");
        System.out.println(customArgs);

        //metodo principal
		Resource resource = DocxMain.translateDocxWithXLIFF(customArgs, filePath, reduceFont);
		
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalFilename() + ".docx\"")
			.body(resource);
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
