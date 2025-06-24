package com.magmatranslation.xliffconverter.controllers;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
import com.magmatranslation.xliffconverter.cli.AppConfig;
import com.magmatranslation.xliffconverter.config.FileProcessorConfig;
import com.magmatranslation.xliffconverter.core.Base64Handler;
import com.magmatranslation.xliffconverter.core.FileReaderWithOkapi;
import com.magmatranslation.xliffconverter.core.XmlHandler;
import com.magmatranslation.xliffconverter.io.DocxHandler;
import com.magmatranslation.xliffconverter.io.XliffHandler;
import com.magmatranslation.xliffconverter.services.storage.StorageFileNotFoundException;
import com.magmatranslation.xliffconverter.services.storage.StorageService;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.filters.openxml.OpenXMLFilter;


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

		System.out.println(events.getLangSource());

		storageService.store(file);

		String filePath = "upload-dir\\" + file.getOriginalFilename();

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

		AppConfig config = new AppConfig(customArgs);

		try (IFilter filter = new OpenXMLFilter()){

			File fileDocx = new File(config.getFilePath());

			FileProcessorConfig fileProcessorConfig = new FileProcessorConfig(config, filter, fileDocx, true, null);

			FileReaderWithOkapi fileReader = new FileReaderWithOkapi();

			List<Event> eventsDocx = fileReader.extractFileEvents(fileProcessorConfig);

			XliffHandler xliffHandler = new XliffHandler();
                    
            String pathXLIFF = xliffHandler.createXLIFF(fileProcessorConfig, eventsDocx);

			Path XLIFFToSend = Paths.get(pathXLIFF);
			
			Resource resource;
			try {
				resource = new UrlResource(XLIFFToSend.toUri());
			
			} catch (MalformedURLException e) {

				throw new RuntimeException("Invalid file path for XLIFF resource", e);
			}

			return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileProcessorConfig.file.getName() + ".xlf\"")
				.body(resource);
		 }


	}


    @PostMapping("/docx/translate")
    public ResponseEntity<Resource> translateDocxWithXLIFF(
        @RequestParam("file") MultipartFile file, 
	    CreateXliffFromDocx events
    ) {

        System.out.println(events.getLangSource());

		storageService.store(file);

		String filePath = "upload-dir\\" + file.getOriginalFilename();

		String[] customArgs = {
            "XLIFF",                           // index 0 - typeFile
            "config/custom.fprm",           // index 1 - filePathParams
            "config/custom.srx",            // index 2 - filePathSegmentRules
            events.getLangSource(),                        // index 3 - langSource
            events.getLangTarget(),                        // index 4 - langTarget
            filePath,         // index 5 - getFilePath
            "files\\DOCX\\" + file.getName() + ".docx",            // index 6 - filePathOutput
            "CREATEFILEXLIFF"               // index 7 - action
        };

		AppConfig config = new AppConfig(customArgs);
		try (IFilter filter = new OpenXMLFilter()){
            File fileXLIFF = new File(filePath);
        
            // FileProcessorConfig fileProcessorConfig = new FileProcessorConfig(config, filter, null, true, null);
            FileProcessorConfig fileProcessorConfig = new FileProcessorConfig(config, filter, null, false, fileXLIFF);

            // fileProcessorConfig.param = false;

			FileReaderWithOkapi fileReader = new FileReaderWithOkapi();

            XmlHandler xmlHandler = new XmlHandler(fileProcessorConfig.fileXLIFF.getAbsolutePath());

            String base64 = xmlHandler.extractContentByTag("//internal-file");

            String fileName = xmlHandler.extractContentByTag("//file/@original");

            File originalFile = Base64Handler.createFileFromBase64(base64, "document/docx/" + UUID.randomUUID().toString() + fileName);

            fileProcessorConfig.file = originalFile;

            List<Event> eventsDocx = fileReader.extractFileEvents(fileProcessorConfig);

            DocxHandler.saveDocx(eventsDocx, fileProcessorConfig);

            System.out.println(events);

            Path DocxToSend = Paths.get(fileProcessorConfig.filePathOutput);
			
			Resource resource;
			try {
				resource = new UrlResource(DocxToSend.toUri());
			
			} catch (MalformedURLException e) {

				throw new RuntimeException("Invalid file path for XLIFF resource", e);
			}


            return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileProcessorConfig.file.getName() + ".docx\"")
				.body(resource);
        	}
    }


    @ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
		return ResponseEntity.notFound().build();
	}


}
