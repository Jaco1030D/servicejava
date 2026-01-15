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
import com.magmatranslation.xliffconverter.core.DocumentXmlProcessor;
import com.magmatranslation.xliffconverter.core.ExtractionResult;
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

			ExtractionResult result = fileReader.extractFileEvents(fileProcessorConfig);
			List<Event> eventsDocx = result.getEvents();

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

    static public Resource translateDocxWithXLIFF(String[] customArgs, String filePath, boolean reduceFont) {
        System.out.println("O metodo translateDocxWithXLIFF foi chamado");
		System.out.println(customArgs[0]);
        System.out.println("Redução de fonte: " + reduceFont);
        
        AppConfig config = new AppConfig(customArgs);
        System.out.println(config.getTypeFile());

		try (IFilter filter = new OpenXMLFilter()){
        
			File fileXLIFF = new File(filePath);
        
            // Define param como true para aplicar os parâmetros do filtro ao salvar
            FileProcessorConfig fileProcessorConfig = new FileProcessorConfig(config, filter, null, true, fileXLIFF);

			FileReaderWithOkapi fileReader = new FileReaderWithOkapi();

            XmlHandler xmlHandler = new XmlHandler(fileProcessorConfig.fileXLIFF.getAbsolutePath());

            String base64 = xmlHandler.extractContentByTag("//internal-file");

            System.out.println("base64: extraido com sucesso");

            String fileName = xmlHandler.extractContentByTag("//file/@original");

            File originalFile = Base64Handler.createFileFromBase64(base64, "document/docx/" + UUID.randomUUID().toString() + fileName);

            fileProcessorConfig.file = originalFile;

            ExtractionResult result = fileReader.extractFileEvents(fileProcessorConfig, reduceFont);
            List<Event> eventsDocx = result.getEvents();
            String jsonFileName = result.getJsonFileName();

            DocxHandler.saveDocx(eventsDocx, fileProcessorConfig);

            Path DocxToSend = Paths.get(fileProcessorConfig.filePathOutput);

            System.out.println("DocxToSend: " + DocxToSend);
            // Processa o document.xml para remover os IDs e ajustar fontes (se reduceFont = true)
            if (reduceFont && jsonFileName != null) {
                String jsonFilePath = "document/json/" + jsonFileName;
                DocumentXmlProcessor processor = new DocumentXmlProcessor(jsonFilePath, DocxToSend.toString());
                processor.process();
            }
			
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
