package com.magmatranslation.xliffconverter.core;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHpsMeasure;
import java.math.BigInteger;

public class FontExtractor {
    
    public static String getFontFamily(XWPFRun run, XWPFParagraph paragraph) {
        if (run == null) {
            return "Arial";
        }
        
        // Tenta obter da run diretamente
        String fontFamily = run.getFontFamily();
        if (fontFamily != null && !fontFamily.isEmpty()) {
            return fontFamily;
        }
        
        // Busca no estilo
        CTRPr styleRPr = getStyleRunProperties(run, paragraph);
        if (styleRPr != null && styleRPr.sizeOfRFontsArray() > 0) {
            CTFonts fonts = styleRPr.getRFontsArray(0);
            if (fonts != null) {
                String ascii = fonts.getAscii();
                if (ascii != null && !ascii.isEmpty()) {
                    return ascii;
                }
            }
        }
        
        return "Arial";
    }
    
    public static Double getFontSize(XWPFRun run, XWPFParagraph paragraph) {
        if (run == null) {
            return 11.0;
        }
        
        // Tenta obter da run diretamente
        Double size = run.getFontSizeAsDouble();
        if (size != null && size > 0) {
            return size;
        }
        
        // Busca no estilo
        CTRPr styleRPr = getStyleRunProperties(run, paragraph);
        if (styleRPr != null && styleRPr.sizeOfSzArray() > 0) {
            CTHpsMeasure sz = styleRPr.getSzArray(0);
            if (sz != null && sz.getVal() != null) {
                try {
                    // O tamanho está em half-points, então divide por 2
                    // getVal() retorna Object, precisa fazer cast para BigInteger
                    Object valObj = sz.getVal();
                    if (valObj instanceof BigInteger) {
                        BigInteger val = (BigInteger) valObj;
                        double fontSize = val.doubleValue() / 2.0;
                        if (fontSize > 0) {
                            return fontSize;
                        }
                    } else if (valObj instanceof Number) {
                        double fontSize = ((Number) valObj).doubleValue() / 2.0;
                        if (fontSize > 0) {
                            return fontSize;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao converter tamanho da fonte do estilo: " + e.getMessage());
                }
            }
        }
        
        return 11.0;
    }
    
    private static CTRPr getStyleRunProperties(XWPFRun run, XWPFParagraph paragraph) {
        // Usa o parágrafo passado como parâmetro, ou tenta obter do run
        if (paragraph == null && run != null) {
            try {
                paragraph = run.getParagraph();
            } catch (Exception e) {
                // Ignora se não conseguir obter
            }
        }
        
        if (paragraph == null) {
            return null;
        }
        
        String styleId = paragraph.getStyleID();
        if (styleId == null || styleId.isEmpty()) {
            return null;
        }
        
        XWPFDocument document = paragraph.getDocument();
        if (document == null) {
            return null;
        }
        
        XWPFStyles styles = document.getStyles();
        if (styles == null) {
            return null;
        }
        
        XWPFStyle style = styles.getStyle(styleId);
        if (style == null) {
            return null;
        }
        
        CTStyle ctStyle = style.getCTStyle();
        if (ctStyle != null && ctStyle.getRPr() != null) {
            return ctStyle.getRPr();
        }
        
        return null;
    }
}