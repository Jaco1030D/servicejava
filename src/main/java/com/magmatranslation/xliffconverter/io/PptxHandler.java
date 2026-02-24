package com.magmatranslation.xliffconverter.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextRun;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import com.magmatranslation.xliffconverter.config.FileProcessorConfig;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.ISegmenter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;

//Essa class so aceita a versão mais recente do powerpoint, o pptx, as outras versões devem ser convertidas para pptx antes de ser processado.
public class PptxHandler {

    public List<Event> readPptxFile(FileProcessorConfig config) {
        List<Event> eventList = new ArrayList<>();
        System.out.println("Lendo arquivo PowerPoint: " + config.file.getName());
        
        // Verifica se o arquivo é PPT e converte para PPTX se necessário
        if (isPptFile(config.file)) {
            System.out.println("Arquivo PPT detectado. Convertendo para PPTX...");
            convertPptToPptx(config);
        }
        
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
            System.err.println("Erro ao Ler o arquivo PowerPoint: " + e.getMessage());
            e.printStackTrace();
        }

        return eventList;
    }

    /**
     * Verifica se o arquivo é um arquivo PPT (formato antigo do PowerPoint) baseado na extensão
     * 
     * @param file Arquivo a ser verificado
     * @return true se o arquivo é PPT, false caso contrário
     */
    public static boolean isPptFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".ppt");
    }

    /**
     * Converte um arquivo PPT (formato antigo) para PPTX usando Apache POI
     * Atualiza o config.file com o novo arquivo PPTX criado
     * 
     * @param config Configuração do processamento de arquivo
     */
    public static void convertPptToPptx(FileProcessorConfig config) {
        File pptFile = config.file;
        File pptxFile = null;
        
        try {
            // Cria um arquivo temporário PPTX
            String pptFileName = pptFile.getName();
            String baseName = pptFileName.substring(0, pptFileName.lastIndexOf('.'));
            pptxFile = File.createTempFile(baseName + "_", ".pptx");
            
            System.out.println("Convertendo PPT para PPTX: " + pptFile.getAbsolutePath() + " -> " + pptxFile.getAbsolutePath());
            
            // Lê o arquivo PPT antigo usando HSLF
            HSLFSlideShow hssfSlideShow = null;
            try (FileInputStream fis = new FileInputStream(pptFile)) {
                hssfSlideShow = new HSLFSlideShow(fis);
            }
            
            // Cria o novo slide show PPTX usando XSLF
            XMLSlideShow xslfSlideShow = new XMLSlideShow();
            
            // Obtém o tamanho do slide original
            java.awt.Dimension dimension = hssfSlideShow.getPageSize();
            xslfSlideShow.setPageSize(dimension);
            
            // Copia todos os slides do PPT para o PPTX
            List<HSLFSlide> hssfSlides = hssfSlideShow.getSlides();
            
            for (HSLFSlide hssfSlide : hssfSlides) {
                // Cria um novo slide no PPTX
                XSLFSlide xslfSlide = xslfSlideShow.createSlide();
                
                // Copia o conteúdo de texto do slide
                for (org.apache.poi.sl.usermodel.Shape<?,?> shape : hssfSlide.getShapes()) {
                    if (shape instanceof HSLFTextShape) {
                        HSLFTextShape hssfTextShape = (HSLFTextShape) shape;
                        
                        // Cria uma nova forma de texto no slide PPTX
                        XSLFTextShape xslfTextShape = xslfSlide.createTextBox();
                        
                        // Copia o texto e formatação
                        List<HSLFTextParagraph> hssfParagraphs = hssfTextShape.getTextParagraphs();
                        
                        for (HSLFTextParagraph hssfParagraph : hssfParagraphs) {
                            XSLFTextParagraph xslfParagraph = xslfTextShape.addNewTextParagraph();
                            
                            // Copia os runs de texto
                            List<HSLFTextRun> hssfRuns = hssfParagraph.getTextRuns();
                            for (HSLFTextRun hssfRun : hssfRuns) {
                                XSLFTextRun xslfRun = xslfParagraph.addNewTextRun();
                                xslfRun.setText(hssfRun.getRawText());
                                
                                // Copia formatação básica se disponível
                                if (hssfRun.getFontSize() > 0) {
                                    xslfRun.setFontSize(hssfRun.getFontSize());
                                }
                                String fontName = hssfRun.getFontFamily();
                                if (fontName != null && !fontName.isEmpty()) {
                                    xslfRun.setFontFamily(fontName);
                                }
                                if (hssfRun.isBold()) {
                                    xslfRun.setBold(true);
                                }
                                if (hssfRun.isItalic()) {
                                    xslfRun.setItalic(true);
                                }
                            }
                        }
                        
                        // Copia posição e tamanho da forma
                        java.awt.geom.Rectangle2D anchor = hssfTextShape.getAnchor();
                        if (anchor != null) {
                            xslfTextShape.setAnchor(anchor);
                        }
                    }
                }
            }
            
            // Fecha o slide show PPT antigo
            hssfSlideShow.close();
            
            // Escreve o arquivo PPTX
            try (FileOutputStream fileOutputStream = new FileOutputStream(pptxFile)) {
                xslfSlideShow.write(fileOutputStream);
            }
            
            // Fecha o slide show PPTX
            xslfSlideShow.close();
            
            // Atualiza o config.file com o novo arquivo PPTX
            config.file = pptxFile;
            
            System.out.println("Conversão concluída com sucesso. Arquivo PPTX criado: " + pptxFile.getAbsolutePath());
            
        } catch (Exception ex) {
            System.err.println("Erro ao converter PPT para PPTX: " + ex.getMessage());
            ex.printStackTrace();
            
            // Em caso de erro, tenta limpar o arquivo temporário se foi criado
            if (pptxFile != null && pptxFile.exists()) {
                pptxFile.delete();
            }
            
            throw new RuntimeException("Falha ao converter PPT para PPTX: " + ex.getMessage(), ex);
        }
    }
}
