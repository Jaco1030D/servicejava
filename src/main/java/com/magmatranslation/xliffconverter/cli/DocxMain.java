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
import com.magmatranslation.xliffconverter.utils.WrapperConfigProcessor;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.filters.openxml.OpenXMLFilter;

public class DocxMain {
    
    static public Resource createXLIFF(String[] customArgs) {

        
        try (IFilter filter = new OpenXMLFilter()){
            
            //-----------------------------------------------------------------------------------------------
            // Configurando a class FileProcessorConfig que servira para centralizar  as informações

            WrapperConfigProcessor wrapperConfigProcessor = Main.configProcessor(customArgs, filter, false);

            FileProcessorConfig fileProcessorConfig = wrapperConfigProcessor.getFileProcessorConfig();
            
            FileReaderWithOkapi fileReader = wrapperConfigProcessor.getFileReader();

            //-----------------------------------------------------------------------------------------------
            // Extrair os eventos do arquivo

			ExtractionResult result = fileReader.extractFileEvents(fileProcessorConfig);

			List<Event> eventsDocx = result.getEvents();

            //-----------------------------------------------------------------------------------------------
            // Criar o arquivo XLIFF

			XliffHandler xliffHandler = new XliffHandler();
                    
            String pathXLIFF = xliffHandler.createXLIFF(fileProcessorConfig, eventsDocx);

			Path XLIFFToSend = Paths.get(pathXLIFF);
			
            //-----------------------------------------------------------------------------------------------
            // Retornar o arquivo XLIFF
            
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
        
		try (IFilter filter = new OpenXMLFilter()){

            //-----------------------------------------------------------------------------------------------
            // Configurando a class FileProcessorConfig que servira para centralizar  as informações

            WrapperConfigProcessor wrapperConfigProcessor = Main.configProcessor(customArgs, filter, true);

            FileProcessorConfig fileProcessorConfig = wrapperConfigProcessor.getFileProcessorConfig();
            
            FileReaderWithOkapi fileReader = wrapperConfigProcessor.getFileReader();

            //-----------------------------------------------------------------------------------------------
            // Lendo o arquivo XLIFF e extraindo o conteudo base64 e o nome do arquivo original

            XmlHandler xmlHandler = new XmlHandler(fileProcessorConfig.fileXLIFF.getAbsolutePath()); //necessario para poder ler o arquivo XLIFF e remover caracteres que geram bugs

            String base64 = xmlHandler.extractContentByTag("//internal-file");
            
            String fileName = xmlHandler.extractContentByTag("//file/@original");

            File originalFile = Base64Handler.createFileFromBase64(base64, "document/docx/" + UUID.randomUUID().toString() + fileName);

            fileProcessorConfig.file = originalFile;

            //-----------------------------------------------------------------------------------------------
            // Extraindo os eventos do arquivo DOCX

            ExtractionResult result = fileReader.extractFileEvents(fileProcessorConfig, reduceFont);

            List<Event> eventsDocx = result.getEvents();
            
            String jsonFileName = result.getJsonFileName();

            DocxHandler.saveDocx(eventsDocx, fileProcessorConfig);

            Path DocxToSend = Paths.get(fileProcessorConfig.filePathOutput);

            System.out.println("DocxToSend: " + DocxToSend);
            
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
