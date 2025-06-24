package com.magmatranslation.xliffconverter.cli;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import com.magmatranslation.xliffconverter.config.AppConfig;
import com.magmatranslation.xliffconverter.config.FileProcessorConfig;
import com.magmatranslation.xliffconverter.core.Base64Handler;
import com.magmatranslation.xliffconverter.core.FileReaderWithOkapi;
import com.magmatranslation.xliffconverter.core.XmlHandler;
import com.magmatranslation.xliffconverter.io.DocxHandler;
import com.magmatranslation.xliffconverter.io.XliffHandler;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.filters.openxml.OpenXMLFilter;

public class DocxMain {
    
    static public Resource createXLIFF(String[] customArgs) {

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

            return resource;

		}


    }

    static public Resource translateDocxWithXLIFF(String[] customArgs, String filePath) {
        AppConfig config = new AppConfig(customArgs);

		try (IFilter filter = new OpenXMLFilter()){
        
			File fileXLIFF = new File(filePath);
        
            FileProcessorConfig fileProcessorConfig = new FileProcessorConfig(config, filter, null, false, fileXLIFF);

			FileReaderWithOkapi fileReader = new FileReaderWithOkapi();

            XmlHandler xmlHandler = new XmlHandler(fileProcessorConfig.fileXLIFF.getAbsolutePath());

            String base64 = xmlHandler.extractContentByTag("//internal-file");

            String fileName = xmlHandler.extractContentByTag("//file/@original");

            File originalFile = Base64Handler.createFileFromBase64(base64, "document/docx/" + UUID.randomUUID().toString() + fileName);

            fileProcessorConfig.file = originalFile;

            List<Event> eventsDocx = fileReader.extractFileEvents(fileProcessorConfig);

            DocxHandler.saveDocx(eventsDocx, fileProcessorConfig);

            Path DocxToSend = Paths.get(fileProcessorConfig.filePathOutput);
			
			Resource resource;
			try {
				resource = new UrlResource(DocxToSend.toUri());
			
			} catch (MalformedURLException e) {

				throw new RuntimeException("Invalid file path for XLIFF resource", e);
			}

            return resource;
		}
    }
}
