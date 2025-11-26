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
import com.magmatranslation.xliffconverter.services.storage.StorageFileNotFoundException;
import com.magmatranslation.xliffconverter.services.storage.StorageService;


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

		storageService.store(file);

		String filePath = "upload-dir" + File.separator + file.getOriginalFilename();

		String[] customArgs = {
            "DOCX",                           // index 0 - typeFile
            "config/custom.fprm",           // index 1 - filePathParams
            "config/custom.srx",            // index 2 - filePathSegmentRules
            events.getLangSource(),                        // index 3 - langSource
            events.getLangTarget(),                        // index 4 - langTarget
            filePath,         // index 5 - getFilePath
            "files",            // index 6 - filePathOutput
            "CREATEFILEXLIFF"               // index 7 - action
        };

        System.out.println("O metodo de criar XLIFF foi chamado");

		Resource resource = DocxMain.createXLIFF(customArgs);
		
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalFilename() + ".xlf\"")
			.body(resource);


	}


    @PostMapping("/docx/translate")
    public ResponseEntity<Resource> translateDocxWithXLIFF(
        @RequestParam("file") MultipartFile file, 
	    CreateXliffFromDocx events
    ) {

        System.out.println(events.getLangSource());

		storageService.store(file);

		String filePath = "upload-dir" + File.separator + file.getOriginalFilename();

		String[] customArgs = {
            "XLIFF",                           // index 0 - typeFile
            "config/custom.fprm",           // index 1 - filePathParams
            "config/custom.srx",            // index 2 - filePathSegmentRules
            events.getLangSource(),                        // index 3 - langSource
            events.getLangTarget(),                        // index 4 - langTarget
            filePath,         // index 5 - getFilePath
			"files" + File.separator + "DOCX" + File.separator + file.getOriginalFilename() + ".docx",            // index 6 - filePathOutput
            "CREATEFILEXLIFF"               // index 7 - action
        };
        System.out.println("O metodo translateDocxWithXLIFF foi chamado");
        System.out.println(customArgs);

		Resource resource = DocxMain.translateDocxWithXLIFF(customArgs, filePath);
		
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getOriginalFilename() + ".docx\"")
			.body(resource);
    }


    @ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
		return ResponseEntity.notFound().build();
	}


}
