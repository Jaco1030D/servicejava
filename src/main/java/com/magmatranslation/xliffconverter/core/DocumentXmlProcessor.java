package com.magmatranslation.xliffconverter.core;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;


import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Classe responsável por processar o document.xml do arquivo DOCX.
 * Lê o JSON de segmentos, remove IDs e ajusta tamanhos de fonte.
 */
public class DocumentXmlProcessor {
    
    private String jsonFilePath;
    private String docxFilePath;
    private ObjectMapper objectMapper;
    
    /**
     * Construtor.
     * 
     * @param jsonFilePath Caminho do arquivo JSON com os segmentos
     * @param docxFilePath Caminho do arquivo DOCX a ser processado
     */
    public DocumentXmlProcessor(String jsonFilePath, String docxFilePath) {
        this.jsonFilePath = jsonFilePath;
        this.docxFilePath = docxFilePath;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Método central que inicia todos os processos.
     */
    public void process() {
        System.out.println("Iniciando processamento do documento DOCX...");
        
        // 1. Lê o JSON e transforma em HashMap
        Map<String, SegmentData> segmentMap = loadJsonToMap();
        
        if (segmentMap == null || segmentMap.isEmpty()) {
            System.err.println("Nenhum segmento encontrado no JSON ou erro ao carregar.");
            return;
        }
        
        System.out.println("JSON carregado com " + segmentMap.size() + " segmentos.");
        
        // 2. Processa o document.xml e remove os IDs
        processDocument(segmentMap);
        
        System.out.println("Processamento concluído.");
    }
    
    /**
     * Classe interna para armazenar dados do segmento.
     */
    private static class SegmentData {
        String id;
        String source;
        String target;
        
        SegmentData(String id, String source, String target) {
            this.id = id;
            this.source = source;
            this.target = target;
        }
    }
    
    /**
     * Lê o arquivo JSON e transforma em HashMap.
     * 
     * @return HashMap com os segmentos
     */
    private Map<String, SegmentData> loadJsonToMap() {
        Map<String, SegmentData> segmentMap = new HashMap<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(new java.io.File(jsonFilePath));
            
            if (rootNode.isArray()) {
                for (JsonNode segment : rootNode) {
                    String id = segment.get("id").asText();
                    String source = segment.has("source") ? segment.get("source").asText() : "";
                    String target = segment.has("target") ? segment.get("target").asText() : "";
                    
                    segmentMap.put(id, new SegmentData(id, source, target));
                }
            }
            
            System.out.println("JSON convertido para HashMap com " + segmentMap.size() + " entradas.");
            
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        
        return segmentMap;
    }
    
    /**
     * Calcula o fator multiplicativo para o tamanho da fonte baseado na 
     * comparação VISUAL entre source e target usando FontMetrics.
     * 
     * @param source Texto fonte
     * @param target Texto traduzido
     * @return Fator multiplicativo para aplicar na fonte atual
     */
    private double calculateFontSize(String source, String target, String fontFamily, double fontSize) {
        // Usa o FontSizeCalculator para cálculo visual preciso
        // Limites: mínimo 0.5 (reduz até 50%), máximo 1.5 (aumenta até 50%)
        double multiplier = FontSizeCalculator.calculateFontSizeMultiplier(
            source, target, fontFamily, fontSize, 0.5, 1.5
        );
        
        // Log apenas se houver mudança significativa (> 5%)
        if (Math.abs(multiplier - 1.0) > 0.05) {
            System.out.println(String.format(
                "Ajuste de fonte necessário - Multiplicador: %.3f (%.0f%%)",
                multiplier, (multiplier - 1.0) * 100
            ));
        }
        
        return multiplier;
    }
    
    /**
     * Processa o documento DOCX completo, incluindo casos especiais.
     * 
     * @param segmentMap HashMap com os segmentos
     */
    private void processDocument(Map<String, SegmentData> segmentMap) {
        try (FileInputStream fis = new FileInputStream(docxFilePath);
             XWPFDocument document = new XWPFDocument(fis)) {
            
            int totalProcessed = 0;
            
            // Cria lista dos IDs e ordena por tamanho decrescente
            List<String> ids = new ArrayList<>(segmentMap.keySet());
            Collections.sort(ids, Comparator.comparingInt(String::length).reversed());
            
            // Cria o pattern regex para encontrar qualquer ID
            String regexPattern = "\\[(" + String.join("|", ids.stream()
                .map(Pattern::quote)
                .toArray(String[]::new)) + ")\\]";
            
            Pattern pattern = Pattern.compile(regexPattern);
            
            System.out.println("Pattern criado para buscar IDs.");
            
            // 1. Processar parágrafos normais do documento (inclui listas e títulos)
            totalProcessed += processRegularParagraphs(document, pattern, segmentMap);
            
            // 2. Processar tabelas
            totalProcessed += processTables(document, pattern, segmentMap);
            
            // 3. Processar cabeçalhos
            totalProcessed += processHeaders(document, pattern, segmentMap);
            
            // 4. Processar rodapés
            totalProcessed += processFooters(document, pattern, segmentMap);
            
            // 5. Processar textboxes e shapes usando XPath
            totalProcessed += processTextboxes(document, pattern, segmentMap);
            
            // Salva o documento modificado
            try (FileOutputStream fos = new FileOutputStream(docxFilePath)) {
                document.write(fos);
            }
            
            System.out.println("Total de IDs processados: " + totalProcessed);
            
        } catch (Exception e) {
            System.err.println("Erro ao processar documento DOCX: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Processa parágrafos normais do documento.
     * Isso inclui: texto normal, listas, títulos, sumários.
     */
    private int processRegularParagraphs(XWPFDocument document, Pattern pattern, 
                                         Map<String, SegmentData> segmentMap) {
        int count = 0;
        
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            // Detecta tipos especiais para logging
            String paragraphType = getParagraphType(paragraph);
            
            int processed = processParagraphRuns(paragraph, pattern, segmentMap);
            
            if (processed > 0 && !paragraphType.equals("normal")) {
                System.out.println("Processado em " + paragraphType + ": " + processed + " ID(s)");
            }
            
            count += processed;
        }
        
        System.out.println("Total de parágrafos (normal, listas, títulos, sumários): " + count);
        return count;
    }
    
    /**
     * Identifica o tipo de parágrafo para logging.
     */
    private String getParagraphType(XWPFParagraph paragraph) {
        String style = paragraph.getStyle();
        
        if (style != null) {
            // Títulos (Heading1, Heading2, etc.)
            if (style.toLowerCase().contains("heading") || style.toLowerCase().contains("titulo")) {
                return "título (" + style + ")";
            }
            // Sumário (TOC)
            if (style.toLowerCase().contains("toc")) {
                return "sumário";
            }
            // Lista
            if (style.toLowerCase().contains("list")) {
                return "lista";
            }
        }
        
        // Verifica se é uma lista numerada/com marcadores
        if (paragraph.getNumID() != null) {
            return "lista numerada";
        }
        
        return "normal";
    }
    
    /**
     * Processa tabelas do documento.
     */
    private int processTables(XWPFDocument document, Pattern pattern, 
                              Map<String, SegmentData> segmentMap) {
        int count = 0;
        int tableCount = 0;
        
        for (XWPFTable table : document.getTables()) {
            tableCount++;
            int tableProcessed = 0;
            
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        tableProcessed += processParagraphRuns(paragraph, pattern, segmentMap);
                    }
                }
            }
            
            if (tableProcessed > 0) {
                System.out.println("Tabela " + tableCount + ": " + tableProcessed + " ID(s) processados");
            }
            
            count += tableProcessed;
        }
        
        System.out.println("Total de tabelas processadas: " + tableCount + " (" + count + " IDs)");
        return count;
    }
    
    /**
     * Processa cabeçalhos do documento.
     */
    private int processHeaders(XWPFDocument document, Pattern pattern, 
                               Map<String, SegmentData> segmentMap) {
        int count = 0;
        int headerCount = 0;
        
        for (XWPFHeader header : document.getHeaderList()) {
            headerCount++;
            int headerProcessed = 0;
            
            // Processa parágrafos do cabeçalho
            for (XWPFParagraph paragraph : header.getParagraphs()) {
                headerProcessed += processParagraphRuns(paragraph, pattern, segmentMap);
            }
            
            // Processa tabelas dentro do cabeçalho
            for (XWPFTable table : header.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            headerProcessed += processParagraphRuns(paragraph, pattern, segmentMap);
                        }
                    }
                }
            }
            
            if (headerProcessed > 0) {
                System.out.println("Cabeçalho " + headerCount + ": " + headerProcessed + " ID(s) processados");
            }
            
            count += headerProcessed;
        }
        
        System.out.println("Total de cabeçalhos processados: " + headerCount + " (" + count + " IDs)");
        return count;
    }
    
    /**
     * Processa rodapés do documento.
     */
    private int processFooters(XWPFDocument document, Pattern pattern, 
                               Map<String, SegmentData> segmentMap) {
        int count = 0;
        int footerCount = 0;
        
        for (XWPFFooter footer : document.getFooterList()) {
            footerCount++;
            int footerProcessed = 0;
            
            // Processa parágrafos do rodapé
            for (XWPFParagraph paragraph : footer.getParagraphs()) {
                footerProcessed += processParagraphRuns(paragraph, pattern, segmentMap);
            }
            
            // Processa tabelas dentro do rodapé
            for (XWPFTable table : footer.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            footerProcessed += processParagraphRuns(paragraph, pattern, segmentMap);
                        }
                    }
                }
            }
            
            if (footerProcessed > 0) {
                System.out.println("Rodapé " + footerCount + ": " + footerProcessed + " ID(s) processados");
            }
            
            count += footerProcessed;
        }
        
        System.out.println("Total de rodapés processados: " + footerCount + " (" + count + " IDs)");
        return count;
    }
    
    /**
     * Processa as runs de um parágrafo.
     */
    private int processParagraphRuns(XWPFParagraph paragraph, Pattern pattern, 
                                     Map<String, SegmentData> segmentMap) {
        int count = 0;
        
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            
            if (text != null && !text.isEmpty()) {
                Matcher matcher = pattern.matcher(text);
                
                if (matcher.find()) {
                    // PASSO 1: Remove os IDs do texto PRIMEIRO
                    String modifiedText = matcher.replaceAll("");
                    run.setText(modifiedText, 0);
                    
                    // PASSO 2: Calcula e aplica o tamanho da fonte baseado no texto SEM ID
                    matcher.reset();
                    while (matcher.find()) {
                        String foundId = matcher.group(1);
                        SegmentData segment = segmentMap.get(foundId);
                        
                        if (segment != null) {
                            // Obtém o tamanho da fonte usando método robusto (verifica run e parágrafo)
                            Double fontSizeResult = FontExtractor.getFontSize(run, paragraph);
                            double currentFontSize = (fontSizeResult != null && fontSizeResult > 0) ? fontSizeResult : 11.0;
                            
                            // Obtém a família da fonte usando método robusto (verifica run e parágrafo)
                            String fontFamily = FontExtractor.getFontFamily(run, paragraph);
                            if (fontFamily == null || fontFamily.isEmpty()) {
                                fontFamily = "Arial";
                            }
                            
                            System.out.print("----------------------------------------------\n");
                            System.out.println("Font size (extraído): " + currentFontSize);
                            
                            // Validação de segurança
                            if (currentFontSize <= 0 || Double.isNaN(currentFontSize) || Double.isInfinite(currentFontSize)) {
                                System.err.println("AVISO: Tamanho de fonte inválido detectado: " + currentFontSize + " - usando fallback 11.0");
                                currentFontSize = 11.0;
                            }
                            
                            // Calcula o fator multiplicativo baseado na discrepância visual
                            double fontSizeFactor = calculateFontSize(segment.source, segment.target, fontFamily, currentFontSize);
                            
                            // Validação do fator
                            if (fontSizeFactor <= 0 || Double.isNaN(fontSizeFactor) || Double.isInfinite(fontSizeFactor)) {
                                System.err.println("AVISO: Fator de fonte inválido detectado: " + fontSizeFactor);
                                System.err.println("  - Source: \"" + segment.source + "\"");
                                System.err.println("  - Target: \"" + segment.target + "\"");
                                System.err.println("  - FontSize atual: " + currentFontSize);
                                System.err.println("  - FontFamily: " + fontFamily);
                                System.err.println("  - Usando fallback 1.0");
                                fontSizeFactor = 1.0;
                            }
                            
                            // Calcula o novo tamanho aplicando o fator
                            double newFontSize = currentFontSize * fontSizeFactor;
                            
                            // Log detalhado se houver problema
                            if (newFontSize < 6.0 && fontSizeFactor > 0) {
                                System.out.println("DEBUG: Cálculo detalhado:");
                                System.out.println("  - Fonte atual: " + currentFontSize + "pt");
                                System.out.println("  - Fator aplicado: " + fontSizeFactor);
                                System.out.println("  - Resultado: " + newFontSize + "pt");
                                System.out.println("  - Source width: " + (segment.source != null ? segment.source.length() : "null"));
                                System.out.println("  - Target width: " + (segment.target != null ? segment.target.length() : "null"));
                            }
                            
                            // Garante um tamanho mínimo de 6pt
                            if (newFontSize < 6.0) {
                                System.out.println("AVISO: Tamanho calculado (" + newFontSize + "pt) abaixo do mínimo, aplicando 6.0pt");
                                newFontSize = 6.0;
                            }
                            
                            System.out.println(String.format(
                                "ID: [%s] - Fonte: %.1fpt -> %.1fpt (fator: %.3f) - Família: %s - Texto: \"%s\"",
                                foundId, currentFontSize, newFontSize, fontSizeFactor, fontFamily,
                                modifiedText.length() > 50 ? modifiedText.substring(0, 50) + "..." : modifiedText
                            ));
                            
                            // Aplica o novo tamanho da fonte
                           
                            run.setFontSize((int) Math.round(newFontSize));
                            
                            count++;
                        }
                    }
                }
            }
        }
        
        return count;
    }
    
    /**
     * Processa textboxes e shapes usando XPath para acessar XML diretamente.
     * Busca TODAS as tags w:t no documento, incluindo as que estão dentro de:
     * - mc:AlternateContent
     * - w:drawing
     * - wps:txbx (textboxes)
     * - a:graphic
     */
    private int processTextboxes(XWPFDocument document, Pattern pattern, 
                                 Map<String, SegmentData> segmentMap) {
        int count = 0;
        
        try {
            // Obtém o XML do documento
            XmlObject xmlObject = XmlObject.Factory.parse(document.getDocument().xmlText());
            
            // Busca TODAS as tags w:t no documento, independente da profundidade
            // Isso captura textboxes dentro de mc:AlternateContent, drawings, etc.
            XmlObject[] textElements = xmlObject.selectPath(
                "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' " +
                "declare namespace wps='http://schemas.microsoft.com/office/word/2010/wordprocessingShape' " +
                "declare namespace mc='http://schemas.openxmlformats.org/markup-compatibility/2006' " +
                ".//w:t"
            );
            
            System.out.println("Tags <w:t> encontradas via XPath (incluindo textboxes): " + textElements.length);
            
            for (XmlObject textObj : textElements) {
                try {
                    CTText ctText = CTText.Factory.parse(textObj.xmlText());
                    String text = ctText.getStringValue();
                    
                    if (text != null && !text.isEmpty()) {
                        Matcher matcher = pattern.matcher(text);
                        
                        if (matcher.find()) {
                            // PASSO 1: Remove os IDs do texto PRIMEIRO
                            String modifiedText = matcher.replaceAll("");
                            ctText.setStringValue(modifiedText);
                            
                            // PASSO 2: Conta os IDs removidos
                            matcher.reset();
                            while (matcher.find()) {
                                String foundId = matcher.group(1);
                                SegmentData segment = segmentMap.get(foundId);
                                
                                if (segment != null) {
                                    System.out.println(String.format(
                                        "ID em elemento XML: [%s] - Texto sem ID: \"%s\"",
                                        foundId,
                                        modifiedText.length() > 50 ? modifiedText.substring(0, 50) + "..." : modifiedText
                                    ));
                                    count++;
                                }
                            }
                            
                            // IMPORTANTE: Atualiza o XML original
                            textObj.set(ctText);
                        }
                    }
                } catch (Exception e) {
                    // Ignora erros de parse em elementos específicos
                    continue;
                }
            }
            
            // Salva as mudanças de volta no documento
            document.getDocument().set(xmlObject);
            
            System.out.println("Total de IDs encontrados em elementos XML (textboxes, shapes, etc): " + count);
            
        } catch (Exception e) {
            System.err.println("Erro ao processar elementos XML: " + e.getMessage());
            e.printStackTrace();
        }
        
        return count;
    }
    
    /**
     * Método robusto para extrair o tamanho da fonte de um run.
     * Tenta múltiplas formas em ordem de prioridade, incluindo verificação no parágrafo pai.
     * 
     * @param run Run do qual extrair o tamanho da fonte
     * @param paragraph Parágrafo pai do run (para verificar herança)
     * @return Tamanho da fonte em pontos (double), ou 11.0 como fallback
     */
    
    private double extractFontSizeRobust(XWPFRun run, XWPFParagraph paragraph) {
        try {
            // 1ª tentativa: getFontSizeAsDouble() - API moderna (no run)
            Double fontSizeDouble = run.getFontSizeAsDouble();
            if (fontSizeDouble != null && fontSizeDouble > 0) {
                return fontSizeDouble;
            }
            
            // 2ª tentativa: Via CTR (baixo nível XML) - busca w:sz no run
            if (run.getCTR() != null && run.getCTR().getRPr() != null) {
                try {
                    XmlObject[] szArray = run.getCTR().getRPr().selectPath(
                        "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:sz/@w:val"
                    );
                    if (szArray != null && szArray.length > 0) {
                        String szValue = szArray[0].getDomNode().getNodeValue();
                        if (szValue != null && !szValue.isEmpty()) {
                            double fontSize = Double.parseDouble(szValue) / 2.0; // half-points para points
                            if (fontSize > 0) {
                                return fontSize;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignora e continua
                }
                
                // Tenta w:szCs (complex scripts) no run
                try {
                    XmlObject[] szCsArray = run.getCTR().getRPr().selectPath(
                        "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:szCs/@w:val"
                    );
                    if (szCsArray != null && szCsArray.length > 0) {
                        String szCsValue = szCsArray[0].getDomNode().getNodeValue();
                        if (szCsValue != null && !szCsValue.isEmpty()) {
                            double fontSizeCs = Double.parseDouble(szCsValue) / 2.0;
                            if (fontSizeCs > 0) {
                                return fontSizeCs;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignora e continua
                }
            }
            
            // 3ª tentativa: getFontSize() - método deprecated mas pode ter valor (no run)
            @SuppressWarnings("deprecation")
            int fontSizeInt = run.getFontSize();
            if (fontSizeInt > 0) {
                return (double) fontSizeInt;
            }
            
            // 4ª tentativa: Verificar no parágrafo (herança)
            if (paragraph != null) {
                try {
                    // Tenta obter do parágrafo via XML (w:pPr/w:rPr/w:sz)
                    if (paragraph.getCTP() != null && paragraph.getCTP().getPPr() != null) {
                        try {
                            XmlObject[] pSzArray = paragraph.getCTP().getPPr().selectPath(
                                "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:rPr/w:sz/@w:val"
                            );
                            if (pSzArray != null && pSzArray.length > 0) {
                                String pSzValue = pSzArray[0].getDomNode().getNodeValue();
                                if (pSzValue != null && !pSzValue.isEmpty()) {
                                    double pFontSize = Double.parseDouble(pSzValue) / 2.0;
                                    if (pFontSize > 0) {
                                        return pFontSize;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Ignora e continua
                        }
                        
                        // Tenta w:szCs no parágrafo
                        try {
                            XmlObject[] pSzCsArray = paragraph.getCTP().getPPr().selectPath(
                                "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:rPr/w:szCs/@w:val"
                            );
                            if (pSzCsArray != null && pSzCsArray.length > 0) {
                                String pSzCsValue = pSzCsArray[0].getDomNode().getNodeValue();
                                if (pSzCsValue != null && !pSzCsValue.isEmpty()) {
                                    double pFontSizeCs = Double.parseDouble(pSzCsValue) / 2.0;
                                    if (pFontSizeCs > 0) {
                                        return pFontSizeCs;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Ignora e continua
                        }
                    }
                } catch (Exception e) {
                    // Ignora e continua
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao extrair tamanho da fonte: " + e.getMessage());
        }
        
        // Fallback final: tamanho padrão do Word
        return 11.0;
    }
    
    /**
     * Método robusto para extrair a família/estilo da fonte de um run.
     * Tenta múltiplas formas em ordem de prioridade, incluindo verificação no parágrafo pai.
     * 
     * @param run Run do qual extrair a família da fonte
     * @param paragraph Parágrafo pai do run (para verificar herança)
     * @return Nome da família da fonte, ou "Arial" como fallback
     */
    private String extractFontFamilyRobust(XWPFRun run, XWPFParagraph paragraph) {
        try {
            // 1ª tentativa: getFontFamily() - API moderna
            String fontFamily = run.getFontFamily();
            if (fontFamily != null && !fontFamily.isEmpty()) {
                return fontFamily;
            }
            
            // 2ª tentativa: Via CTR (baixo nível XML) - busca w:rFonts
            if (run.getCTR() != null && run.getCTR().getRPr() != null) {
                // Tenta w:ascii
                try {
                    XmlObject[] asciiArray = run.getCTR().getRPr().selectPath(
                        "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:rFonts/@w:ascii"
                    );
                    if (asciiArray != null && asciiArray.length > 0) {
                        String asciiFont = asciiArray[0].getDomNode().getNodeValue();
                        if (asciiFont != null && !asciiFont.isEmpty()) {
                            return asciiFont;
                        }
                    }
                } catch (Exception e) {
                    // Ignora e continua
                }
                
                // Tenta w:hAnsi (High ANSI - mais comum)
                try {
                    XmlObject[] hAnsiArray = run.getCTR().getRPr().selectPath(
                        "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:rFonts/@w:hAnsi"
                    );
                    if (hAnsiArray != null && hAnsiArray.length > 0) {
                        String hAnsiFont = hAnsiArray[0].getDomNode().getNodeValue();
                        if (hAnsiFont != null && !hAnsiFont.isEmpty()) {
                            return hAnsiFont;
                        }
                    }
                } catch (Exception e) {
                    // Ignora e continua
                }
                
                // Tenta w:eastAsia
                try {
                    XmlObject[] eastAsiaArray = run.getCTR().getRPr().selectPath(
                        "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:rFonts/@w:eastAsia"
                    );
                    if (eastAsiaArray != null && eastAsiaArray.length > 0) {
                        String eastAsiaFont = eastAsiaArray[0].getDomNode().getNodeValue();
                        if (eastAsiaFont != null && !eastAsiaFont.isEmpty()) {
                            return eastAsiaFont;
                        }
                    }
                } catch (Exception e) {
                    // Ignora e continua
                }
                
                // Tenta w:cs (Complex Script)
                try {
                    XmlObject[] csArray = run.getCTR().getRPr().selectPath(
                        "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:rFonts/@w:cs"
                    );
                    if (csArray != null && csArray.length > 0) {
                        String csFont = csArray[0].getDomNode().getNodeValue();
                        if (csFont != null && !csFont.isEmpty()) {
                            return csFont;
                        }
                    }
                } catch (Exception e) {
                    // Ignora e continua
                }
            }
            
            // 3ª tentativa: Verificar no parágrafo (herança)
            if (paragraph != null) {
                try {
                    // Tenta obter do parágrafo via XML (w:pPr/w:rPr/w:rFonts)
                    if (paragraph.getCTP() != null && paragraph.getCTP().getPPr() != null) {
                        // Tenta w:ascii no parágrafo
                        try {
                            XmlObject[] pAsciiArray = paragraph.getCTP().getPPr().selectPath(
                                "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:rPr/w:rFonts/@w:ascii"
                            );
                            if (pAsciiArray != null && pAsciiArray.length > 0) {
                                String pAsciiFont = pAsciiArray[0].getDomNode().getNodeValue();
                                if (pAsciiFont != null && !pAsciiFont.isEmpty()) {
                                    return pAsciiFont;
                                }
                            }
                        } catch (Exception e) {
                            // Ignora e continua
                        }
                        
                        // Tenta w:hAnsi no parágrafo
                        try {
                            XmlObject[] pHAnsiArray = paragraph.getCTP().getPPr().selectPath(
                                "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:rPr/w:rFonts/@w:hAnsi"
                            );
                            if (pHAnsiArray != null && pHAnsiArray.length > 0) {
                                String pHAnsiFont = pHAnsiArray[0].getDomNode().getNodeValue();
                                if (pHAnsiFont != null && !pHAnsiFont.isEmpty()) {
                                    return pHAnsiFont;
                                }
                            }
                        } catch (Exception e) {
                            // Ignora e continua
                        }
                        
                        // Tenta w:eastAsia no parágrafo
                        try {
                            XmlObject[] pEastAsiaArray = paragraph.getCTP().getPPr().selectPath(
                                "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:rPr/w:rFonts/@w:eastAsia"
                            );
                            if (pEastAsiaArray != null && pEastAsiaArray.length > 0) {
                                String pEastAsiaFont = pEastAsiaArray[0].getDomNode().getNodeValue();
                                if (pEastAsiaFont != null && !pEastAsiaFont.isEmpty()) {
                                    return pEastAsiaFont;
                                }
                            }
                        } catch (Exception e) {
                            // Ignora e continua
                        }
                        
                        // Tenta w:cs no parágrafo
                        try {
                            XmlObject[] pCsArray = paragraph.getCTP().getPPr().selectPath(
                                "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:rPr/w:rFonts/@w:cs"
                            );
                            if (pCsArray != null && pCsArray.length > 0) {
                                String pCsFont = pCsArray[0].getDomNode().getNodeValue();
                                if (pCsFont != null && !pCsFont.isEmpty()) {
                                    return pCsFont;
                                }
                            }
                        } catch (Exception e) {
                            // Ignora e continua
                        }
                    }
                } catch (Exception e) {
                    // Ignora e continua
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erro ao extrair família da fonte: " + e.getMessage());
        }
        
        // Fallback final: Arial (fonte universal)
        return "Arial";
    }
}