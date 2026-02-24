package com.magmatranslation.xliffconverter.utils;

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.magmatranslation.xliffconverter.config.FileProcessorConfig;
import com.magmatranslation.xliffconverter.io.FilesHandlersUtils;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.RawDocument;

public class FileUtils {
    
    public static String getOriginalFileName(String fileName) {
        String[] fileNameArray = fileName.split("\\.");
        return fileNameArray[0];
    }

    public static String getOriginalFileExtension(String fileName) {
        String[] fileNameArray = fileName.split("\\.");
        return fileNameArray[fileNameArray.length - 1];
    }

    public static void saveGenericFile(List<Event> eventList, FileProcessorConfig config) {
        LocaleId srcLoc = LocaleId.fromString(config.langSource);
        LocaleId trgLoc = LocaleId.fromString(config.langTarget);
        
        if (config.file == null) {
            System.err.println("Erro: Arquivo original não foi definido para salvar");
            return;
        }

        try (
            RawDocument rawDocument = new RawDocument(config.file.toURI(), "UTF-8", srcLoc, trgLoc)
        ) {
            // Abre o filtro com o documento original para preservar a estrutura
            config.filter.open(rawDocument);
            
            // Aplica os parâmetros do filtro se necessário
            if (config.param && config.filePathParams != null) {
                
                FilesHandlersUtils.configFilter(config.filter, config.filePathParams);
            
            }
            
            // Cria o writer após o filtro estar aberto e configurado
            try (IFilterWriter writer = config.filter.createFilterWriter()) {
                
                writer.setOutput(config.filePathOutput);
                
                writer.setOptions(trgLoc, "UTF-8");

                // Processa todos os eventos na ordem correta
                for (Event event : eventList) {

                    writer.handleEvent(event);
                    
                }

                writer.close();
            }
            
            config.filter.close();

        } catch (Exception e) {
            System.err.println("Erro ao salvar o arquivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lê um arquivo XLF (XLIFF) e retorna o atributo 'original' do segundo elemento <file>
     * 
     * @param xlfFilePath caminho para o arquivo XLF
     * @return o valor do atributo 'original' do segundo <file>, ou null se não encontrado
     */
    public static String getSecondFileOriginalAttribute(String xlfFilePath) {
        try {
            File xlfFile = new File(xlfFilePath);
            
            if (!xlfFile.exists()) {
                System.err.println("Erro: Arquivo XLF não encontrado: " + xlfFilePath);
                return null;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xlfFile);

            // Normaliza o documento XML
            document.getDocumentElement().normalize();

            // Busca todos os elementos <file>
            NodeList fileNodes = document.getElementsByTagName("file");

            // Verifica se existe pelo menos 2 elementos <file>
            if (fileNodes.getLength() < 2) {
                System.err.println("Erro: O arquivo XLF não contém um segundo elemento <file>");
                return null;
            }

            // Pega o segundo elemento <file> (índice 1)
            Element secondFileElement = (Element) fileNodes.item(1);

            // Retorna o atributo 'original'
            String originalAttribute = secondFileElement.getAttribute("original");
            
            if (originalAttribute == null || originalAttribute.isEmpty()) {
                System.err.println("Aviso: O segundo elemento <file> não possui o atributo 'original'");
                return null;
            }

            return originalAttribute;

        } catch (Exception e) {
            System.err.println("Erro ao ler o arquivo XLF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}