package com.magmatranslation.xliffconverter.core;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Classe responsável por calcular o multiplicador de tamanho de fonte
 * baseado na comparação visual entre texto fonte e traduzido.
 */
public class FontSizeCalculator {
    
    /**
     * Calcula o multiplicador de tamanho de fonte baseado na comparação
     * do comprimento visual entre source e target.
     * 
     * @param source Texto fonte original
     * @param target Texto traduzido
     * @param fontName Nome da fonte (ex: "Arial", "Times New Roman")
     * @param baseFontSize Tamanho base da fonte para cálculo (em pontos)
     * @param minMultiplier Multiplicador mínimo permitido (ex: 0.7 = não reduzir mais que 30%)
     * @param maxMultiplier Multiplicador máximo permitido (ex: 1.3 = não aumentar mais que 30%)
     * @return Multiplicador da fonte (ex: 1.2 para aumentar 20%, 0.8 para diminuir 20%)
     */
    public static double calculateFontSizeMultiplier(String source, String target, 
                                                     String fontName, double baseFontSize,
                                                     double minMultiplier, double maxMultiplier) {
        if (source == null || target == null || source.isEmpty() || target.isEmpty()) {
            return 1.0; // Sem mudança
        }
        
        // Calcula a largura visual de cada texto
        double sourceWidth = calculateTextWidth(source, fontName, baseFontSize);
        double targetWidth = calculateTextWidth(target, fontName, baseFontSize);
        
        // Se o target for mais longo que o source, precisamos reduzir a fonte
        // Se o target for mais curto que o source, podemos aumentar a fonte
        double ratio = sourceWidth / targetWidth;
        
        // Limita o multiplicador aos valores mín/máx
        double multiplier = Math.max(minMultiplier, Math.min(maxMultiplier, ratio));
        
        // Arredonda para 2 casas decimais
        multiplier = Math.round(multiplier * 100.0) / 100.0;
        
        return multiplier;
    }
    
    /**
     * Versão simplificada que usa valores padrão.
     * 
     * @param source Texto fonte original
     * @param target Texto traduzido
     * @return Multiplicador da fonte
     */
    public static double calculateFontSizeMultiplier(String source, String target) {
        return calculateFontSizeMultiplier(source, target, "Arial", 12, 0.5, 1.5);
    }
    
    /**
     * Calcula a largura visual de um texto em pixels.
     * Usa FontMetrics para obter medidas precisas.
     * 
     * @param text Texto a ser medido
     * @param fontName Nome da fonte
     * @param fontSize Tamanho da fonte (em pontos)
     * @return Largura do texto em pixels
     */
    private static double calculateTextWidth(String text, String fontName, double fontSize) {
        // Cria uma imagem temporária para obter o Graphics2D
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        // Configura renderização de alta qualidade
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Cria a fonte com tamanho em float
        Font font = new Font(fontName, Font.PLAIN, (int) Math.round(fontSize));
        g2d.setFont(font);
        
        // Obtém as métricas da fonte
        FontMetrics fm = g2d.getFontMetrics();
        
        // Calcula a largura do texto
        double width = fm.stringWidth(text);
        
        // Libera recursos
        g2d.dispose();
        
        return width;
    }
    
}