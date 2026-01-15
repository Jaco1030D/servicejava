package com.magmatranslation.xliffconverter.cli;

import java.io.File;
import java.util.List;
import java.util.UUID;

import com.magmatranslation.xliffconverter.config.AppConfig;
import com.magmatranslation.xliffconverter.config.FileProcessorConfig;
import com.magmatranslation.xliffconverter.core.Base64Handler;
import com.magmatranslation.xliffconverter.core.ExtractionResult;
import com.magmatranslation.xliffconverter.core.FileReaderWithOkapi;
import com.magmatranslation.xliffconverter.core.XmlHandler;
import com.magmatranslation.xliffconverter.io.DocxHandler;
import com.magmatranslation.xliffconverter.io.XliffHandler;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.filters.openxml.OpenXMLFilter;

public class Main 
{
    public static void main( String[] args )
    {
        AppConfig config = new AppConfig(args);

        FileReaderWithOkapi fileReader = new FileReaderWithOkapi();

        try (IFilter filter = new OpenXMLFilter()){

            File file = new File(config.getFilePath());
            
            boolean param = true;

            //Configura os parametros, para não ficar linhas grandes com uns 6 parâmetros
            
            switch (config.getAction()) {
                
                case "CREATEFILEXLIFF" ->  {
                    
                    FileProcessorConfig fileProcessorConfig = new FileProcessorConfig(config, filter, file, param, null);

                    ExtractionResult result = fileReader.extractFileEvents(fileProcessorConfig);
                    List<Event> events = result.getEvents();
                    
                    XliffHandler xliffHandler = new XliffHandler();
                    
                    xliffHandler.createXLIFF(fileProcessorConfig, events);

                }
                
                case "TRANSLATEFILE" ->  {

                    FileProcessorConfig fileProcessorConfig = new FileProcessorConfig(config, filter, null, param, file);

                    fileProcessorConfig.param = false;

                    XmlHandler xmlHandler = new XmlHandler(fileProcessorConfig.fileXLIFF.getAbsolutePath());

                    String base64 = xmlHandler.extractContentByTag("//internal-file");

                    String fileName = xmlHandler.extractContentByTag("//file/@original");

                    File originalFile = Base64Handler.createFileFromBase64(base64, "document/docx/" + UUID.randomUUID().toString() + fileName);

                    fileProcessorConfig.file = originalFile;

                    ExtractionResult result = fileReader.extractFileEvents(fileProcessorConfig);
                    List<Event> events = result.getEvents();

                    DocxHandler.saveDocx(events, fileProcessorConfig);

                    System.out.println(events);


                }
            }

        } catch (Exception e) {
            
            System.err.println("Error initializing OpenXMLFilter: " + e.getMessage());
        
        }
        
        
    }
}
