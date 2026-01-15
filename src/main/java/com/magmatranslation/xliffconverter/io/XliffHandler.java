package com.magmatranslation.xliffconverter.io;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.magmatranslation.xliffconverter.config.FileProcessorConfig;
import com.magmatranslation.xliffconverter.core.Base64Handler;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filterwriter.XLIFFWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.xliff.XLIFFFilter;

public class XliffHandler {

    public String createXLIFF(FileProcessorConfig config, List<Event> eventList) {
        LocaleId srcLoc = LocaleId.fromString(config.langSource);
        LocaleId trgLoc = LocaleId.fromString(config.langTarget);

        String pathXLIFF = config.filePathOutput + "\\XLIFF\\" + config.file.getName() + ".xlf";
        
        try (XLIFFWriter writer = new XLIFFWriter()) {
            
            writer.create(pathXLIFF, null, srcLoc, trgLoc, null, "word/document.xml", null);
            
            String fileBase64 = Base64Handler.createBase64(config.file);
            
            writer.writeStartFile(config.file.getName(), "x-docx", null, "<reference><internal-file form=\"base64\"> " + fileBase64 + " </internal-file></reference>");

            writer.writeEndFile();
            
            for (Event event : eventList) {

                if (event.isTextUnit()) {

                    ITextUnit textUnit = event.getTextUnit();

                    writer.writeTextUnit(textUnit);

                }
            }

            writer.close();

            return pathXLIFF;
        
        } catch (Exception e) {

            System.err.println("Erro na class XliffHandler ao criar o arquivo XLIFF: " + e.getMessage());
            
            return null;
        }
    }
    
    public static List<Event> XliffReader(FileProcessorConfig config) {
        LocaleId srcLoc = LocaleId.fromString(config.langSource);
        LocaleId trgLoc = LocaleId.fromString(config.langTarget);
        
        List<Event> eventList = new ArrayList<>();
        
        File fixedXliffFile = null;

        try {
            // Corrige o arquivo XLIFF antes de fazer o parse
            fixedXliffFile = fixXliffFile(config.fileXLIFF);
            
            try (   
                XLIFFFilter filter = new XLIFFFilter();
                RawDocument rawDocument = new RawDocument(fixedXliffFile.toURI(), "UTF-8", srcLoc, trgLoc)
                ) {
        
                filter.open(rawDocument);
                
                while (filter.hasNext()) {
                    Event event = filter.next();

                    if (!event.isTextUnit()) continue;

                    ITextUnit textUnit = event.getTextUnit();

                    if (textUnit.getTarget(trgLoc) != null) {
                        eventList.add(event);
                    }
                }

                filter.close();
            }
            
        } catch (Exception e) {
            System.err.println("Erro na class XliffHandler ao ler o arquivo XLIFF: " + e.getMessage());
        } finally {
            // Remove o arquivo temporário corrigido
            if (fixedXliffFile != null && fixedXliffFile.exists() && !fixedXliffFile.equals(config.fileXLIFF)) {
                fixedXliffFile.delete();
            }
        }

        return eventList;
    }
    
    /**
     * Corrige os '&' que não são parte de entidades XML válidas no arquivo XLIFF
     */
    private static File fixXliffFile(File originalFile) throws Exception {
        // Lê o conteúdo do arquivo
        String xliffContent = Files.readString(originalFile.toPath());
        
        // Corrige os '&' inválidos
        String fixedContent = fixInvalidAmpersands(xliffContent);
        
        // Se não houve mudanças, retorna o arquivo original
        if (fixedContent.equals(xliffContent)) {
            return originalFile;
        }
        
        // Cria um arquivo temporário com o conteúdo corrigido
        File tempFile = File.createTempFile("xliff_fixed_", ".xlf");
        Files.writeString(tempFile.toPath(), fixedContent);
        
        return tempFile;
    }
    
    /**
     * Corrige os '&' que não são parte de entidades XML válidas
     * Entidades válidas: &lt; &gt; &amp; &quot; &apos; &#decimal; &#xhex;
     */
    private static String fixInvalidAmpersands(String xml) {
        // Pattern para identificar '&' seguido de algo que NÃO é uma entidade válida
        Pattern pattern = Pattern.compile("&(?!(lt|gt|amp|quot|apos|#\\d+|#x[0-9a-fA-F]+);)");
        
        Matcher matcher = pattern.matcher(xml);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            // Substitui '&' inválido por '&amp;'
            matcher.appendReplacement(result, "&amp;");
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
}
