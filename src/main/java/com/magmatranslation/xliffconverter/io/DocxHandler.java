package com.magmatranslation.xliffconverter.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import com.magmatranslation.xliffconverter.config.FileProcessorConfig;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.ISegmenter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;

public class DocxHandler {

    public List<Event> readDocxFile(FileProcessorConfig config) {
        List<Event> eventList = new ArrayList<>();
        LocaleId srcLoc = LocaleId.fromString(config.langSource);
        LocaleId trgLoc = LocaleId.fromString(config.langTarget);

        try (
            RawDocument rawDocument = new RawDocument(config.file.toURI(), "UTF-8", srcLoc, trgLoc)
            ) {

            config.filter.open(rawDocument);

            if (config.param && config.filePathParams != null) {
                FilesHandlersUtils.configFilter(config.filter, config.filePathParams);
            }

            ISegmenter segmenter = FilesHandlersUtils.getSegmenter(config.filePathSegmentRules, srcLoc);
            
            while (config.filter.hasNext()) {
                Event event = config.filter.next();

                if (event.isTextUnit()) {

                    ITextUnit textUnit = event.getTextUnit();

                    TextContainer sourceContainer = textUnit.getSource();

                    if (config.filePathSegmentRules != null) {

                        if (segmenter != null) {
                            segmenter.computeSegments(sourceContainer);

                            sourceContainer.getSegments().create(segmenter.getRanges());

                            textUnit.setSource(sourceContainer);
                        }
                    }

                }

                eventList.add(event);
            }

            config.filter.close();
        } catch (Exception e) {
            System.err.println("Erro ao Ler o arquivo docx: " + e.getMessage());
        }

        return eventList;
    }

    public static void saveDocx(List<Event> eventList, FileProcessorConfig config) {
        LocaleId srcLoc = LocaleId.fromString(config.langSource);
        LocaleId trgLoc = LocaleId.fromString(config.langTarget);
        
        if (config.file == null) {
            System.err.println("Erro: Arquivo DOCX original não foi definido para salvar");
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
     * Verifica se o arquivo é um arquivo DOC (formato antigo do Word) baseado na extensão
     * 
     * @param file Arquivo a ser verificado
     * @return true se o arquivo é DOC, false caso contrário
     */
    public static boolean isDocFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".doc");
    }

    /**
     * Converte um arquivo DOC (formato antigo) para DOCX usando Apache POI
     * Atualiza o config.file com o novo arquivo DOCX criado
     * 
     * @param config Configuração do processamento de arquivo
     */
    public static void convertDocToDocx(FileProcessorConfig config) {
        File docFile = config.file;
        File docxFile = null;
        
        try {
            // Cria um arquivo temporário DOCX
            String docFileName = docFile.getName();
            String baseName = docFileName.substring(0, docFileName.lastIndexOf('.'));
            docxFile = File.createTempFile(baseName + "_", ".docx");
            
            System.out.println("Convertendo DOC para DOCX: " + docFile.getAbsolutePath() + " -> " + docxFile.getAbsolutePath());
            
            // Lê o arquivo DOC e cria o DOCX
            try (FileInputStream fis = new FileInputStream(docFile);
                 FileOutputStream fos = new FileOutputStream(docxFile)) {
                
                HWPFDocument doc = new HWPFDocument(fis);
                XWPFDocument docx = new XWPFDocument();
                
                Range range = doc.getRange();
                
                // Copia parágrafos do DOC para o DOCX
                for (int i = 0; i < range.numParagraphs(); i++) {
                    Paragraph hwpfPara = range.getParagraph(i);
                    XWPFParagraph xwpfPara = docx.createParagraph();
                    
                    // Copia character runs do parágrafo
                    for (int j = 0; j < hwpfPara.numCharacterRuns(); j++) {
                        CharacterRun cr = hwpfPara.getCharacterRun(j);
                        XWPFRun run = xwpfPara.createRun();
                        
                        // Copia o texto
                        run.setText(cr.text());
                        
                        // Copia formatação básica
                        run.setBold(cr.isBold());
                        run.setItalic(cr.isItalic());
                        
                        // Converte tamanho da fonte (half-points → points)
                        if (cr.getFontSize() > 0) {
                            run.setFontSize((int) cr.getFontSize() / 2);
                        }
                        
                        // Copia nome da fonte se disponível
                        String fontName = cr.getFontName();
                        if (fontName != null && !fontName.isEmpty()) {
                            run.setFontFamily(fontName);
                        }
                    }
                }
                
                // Escreve o arquivo DOCX
                docx.write(fos);
                
                // Fecha os documentos
                doc.close();
                docx.close();
            }
            
            // Atualiza o config.file com o novo arquivo DOCX
            config.file = docxFile;
            
            System.out.println("Conversão concluída com sucesso. Arquivo DOCX criado: " + docxFile.getAbsolutePath());
            
        } catch (Exception ex) {
            System.err.println("Erro ao converter DOC para DOCX: " + ex.getMessage());
            ex.printStackTrace();
            
            // Em caso de erro, tenta limpar o arquivo temporário se foi criado
            if (docxFile != null && docxFile.exists()) {
                docxFile.delete();
            }
            
            throw new RuntimeException("Falha ao converter DOC para DOCX: " + ex.getMessage(), ex);
        }
    }
}
