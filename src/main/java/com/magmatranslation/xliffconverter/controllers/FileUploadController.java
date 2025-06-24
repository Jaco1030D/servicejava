package com.magmatranslation.xliffconverter.controllers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import com.magmatranslation.xliffconverter.BodyRequests.CreateXliffFromDocx;
import com.magmatranslation.xliffconverter.config.AppConfig;
import com.magmatranslation.xliffconverter.config.FileProcessorConfig;
import com.magmatranslation.xliffconverter.core.FileReaderWithOkapi;
import com.magmatranslation.xliffconverter.io.XliffHandler;
import com.magmatranslation.xliffconverter.services.storage.StorageFileNotFoundException;
import com.magmatranslation.xliffconverter.services.storage.StorageService;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.filters.openxml.OpenXMLFilter;

@RestController
public class FileUploadController {
    private final StorageService storageService;

	@Autowired
	public FileUploadController(StorageService storageService) {
		this.storageService = storageService;
	}

	@GetMapping("/")
	public String listUploadedFiles(Model model) throws IOException {

		model.addAttribute("files", storageService.loadAll().map(
				path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
						"serveFile", path.getFileName().toString()).build().toUri().toString())
				.collect(Collectors.toList()));

		return "uploadForm";
	}

	@GetMapping("/files/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

		Resource file = storageService.loadAsResource(filename);

		if (file == null)
			return ResponseEntity.notFound().build();

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + file.getFilename() + "\"").body(file);
	}

    //Grande parte do codigo que criei é para o upload dos arquivos, acredito que a formas de simplificar tudo isso
	@PostMapping("/")
	public ResponseEntity<Resource> handleFileUpload(
	@RequestParam("file") MultipartFile file, 
	CreateXliffFromDocx events
	) {

		System.out.println(events.getLangSource());

		storageService.store(file);

		// Path path = storageService.load(file.getOriginalFilename());

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

	@ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
		return ResponseEntity.notFound().build();
	}
}
