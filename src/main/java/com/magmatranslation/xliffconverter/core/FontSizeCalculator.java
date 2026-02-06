package com.magmatranslation.xliffconverter.core;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class FontSizeCalculator {
    
    private static final Logger LOGGER = Logger.getLogger(FontSizeCalculator.class.getName());
    
    // Constantes para valores padrão
    private static final String DEFAULT_FONT = "Arial";
    private static final double DEFAULT_BASE_SIZE = 12.0;
    private static final double DEFAULT_MIN_MULTIPLIER = 0.5;  // Não reduzir mais que 50%
    private static final double DEFAULT_MAX_MULTIPLIER = 1.5;  // Não aumentar mais que 50%
    
    // Tolerância para considerar textos com largura similar (1% de diferença)
    private static final double WIDTH_TOLERANCE = 0.01;
    private static final double WIDTH_MAX_TOLERANCE = 0.30;
    
    // Sistema de logger JSON
    private static final String LOG_DIRECTORY = "logs";
    private static final String LOG_FILE_NAME = "font_size_calculator_log.json";
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Object LOG_LOCK = new Object();

    private static final ThreadLocal<GraphicsContext> GRAPHICS_CONTEXT = 
        ThreadLocal.withInitial(GraphicsContext::new);
    
    // Construtor privado para utility class
    private FontSizeCalculator() {
        throw new AssertionError("Utility class - não deve ser instanciada");
    }
    
    
    public static double    calculateFontSizeMultiplier(String source, String target, 
                                                     String fontName, double baseFontSize,
                                                     double minMultiplier, double maxMultiplier) {
        // Validações
        validateInputs(source, target, fontName, baseFontSize, minMultiplier, maxMultiplier);
        
        // Se algum texto estiver vazio, não há como calcular proporção
        if (source.isEmpty() || target.isEmpty()) {
            LOGGER.fine("Texto vazio detectado - retornando multiplicador 1.0");
            logToJson(source, target, fontName, baseFontSize, minMultiplier, maxMultiplier, 
                     0.0, 0.0, 1.0, 1.0, "TEXTO_VAZIO");
            return 1.0;
        }
        
        try {
            // Calcula a largura visual de cada texto com a mesma fonte e tamanho
            double sourceWidth = calculateTextWidth(source, fontName, baseFontSize);
            double targetWidth = calculateTextWidth(target, fontName, baseFontSize);
            
            // Log das medições (útil para debug)
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format(
                    "Medições - Source: %.2fpx, Target: %.2fpx, Fonte: %s %.1fpt",
                    sourceWidth, targetWidth, fontName, baseFontSize
                ));
            }
            
            // Proteção contra divisão por zero (não deveria acontecer, mas...)
            if (targetWidth == 0) {
                LOGGER.warning("Target width é zero - retornando multiplicador 1.0");
                logToJson(source, target, fontName, baseFontSize, minMultiplier, maxMultiplier, 
                         sourceWidth, targetWidth, 1.0, 1.0, "TARGET_WIDTH_ZERO");
                return 1.0;
            }
            
            // CÁLCULO PRINCIPAL:
            // ratio = espaço_disponível / espaço_necessário
            double ratio = sourceWidth / targetWidth;
            
            // Se a diferença for muito pequena (< 1%), considera igual
            if (Math.abs(ratio - 1.0) < WIDTH_TOLERANCE || Math.abs(ratio - 1.0) > WIDTH_MAX_TOLERANCE) {
                LOGGER.fine("Textos com largura similar - sem ajuste necessário");
                logToJson(source, target, fontName, baseFontSize, minMultiplier, maxMultiplier, 
                         sourceWidth, targetWidth, ratio, 1.0, "TOLERANCIA");
                return 1.0;
            }
            
            // Aplica limites configuráveis (importante para manter legibilidade)
            double multiplier = Math.max(minMultiplier, Math.min(maxMultiplier, ratio));
            
            // Arredonda para 2 casas decimais (precisão suficiente)
            multiplier = Math.round(multiplier * 100.0) / 100.0;
            
            // Log do resultado
            if (LOGGER.isLoggable(Level.FINE)) {
                String action = multiplier < 1.0 ? "REDUZIR" : 
                               multiplier > 1.0 ? "AUMENTAR" : "MANTER";
                LOGGER.fine(String.format(
                    "Resultado - Ação: %s, Multiplicador: %.2f (ratio bruto: %.2f)",
                    action, multiplier, ratio
                ));
            }
            
            // Registra no log JSON
            logToJson(source, target, fontName, baseFontSize, minMultiplier, maxMultiplier, 
                     sourceWidth, targetWidth, ratio, multiplier, "SUCESSO");
            
            return multiplier;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erro ao calcular multiplicador de fonte", e);
            logToJson(source, target, fontName, baseFontSize, minMultiplier, maxMultiplier, 
                     0.0, 0.0, 1.0, 1.0, "ERRO: " + e.getMessage());
            return 1.0; // Fallback seguro: mantém fonte original
        }
    }
    
    /**
     * Versão simplificada com valores padrão para uso rápido.
     * 
     * <p>Usa: Arial, 12pt, min=0.5 (pode reduzir até 50%), max=1.5 (pode aumentar até 50%)</p>
     * 
     * @param source Texto fonte original
     * @param target Texto traduzido
     * @return Multiplicador da fonte
     */
    public static double calculateFontSizeMultiplier(String source, String target) {
        return calculateFontSizeMultiplier(source, target, DEFAULT_FONT, 
                                          DEFAULT_BASE_SIZE, DEFAULT_MIN_MULTIPLIER, 
                                          DEFAULT_MAX_MULTIPLIER);
    }
    
    public static double calculateFontSizeMultiplier(String source, String target,
                                                     double minMultiplier, double maxMultiplier) {
        return calculateFontSizeMultiplier(source, target, DEFAULT_FONT, 
                                          DEFAULT_BASE_SIZE, minMultiplier, maxMultiplier);
    }

    private static double calculateTextWidth(String text, String fontName, double fontSize) {
        GraphicsContext context = GRAPHICS_CONTEXT.get();
        Graphics2D g2d = context.getGraphics();
        
        // Cria a fonte com o tamanho especificado
        Font font = new Font(fontName, Font.PLAIN, (int) Math.round(fontSize));
        g2d.setFont(font);
        
        // Obtém as métricas da fonte
        FontMetrics fm = g2d.getFontMetrics();
        
        // Retorna a largura do texto em pixels
        return fm.stringWidth(text);
    }
    

    private static void validateInputs(String source, String target, String fontName,
                                       double baseFontSize, double minMultiplier, 
                                       double maxMultiplier) {
        Objects.requireNonNull(source, "Parâmetro 'source' não pode ser null");
        Objects.requireNonNull(target, "Parâmetro 'target' não pode ser null");
        Objects.requireNonNull(fontName, "Parâmetro 'fontName' não pode ser null");
        
        if (fontName.trim().isEmpty()) {
            throw new IllegalArgumentException("Parâmetro 'fontName' não pode ser vazio");
        }
        
        if (baseFontSize <= 0) {
            throw new IllegalArgumentException(
                "Parâmetro 'baseFontSize' deve ser positivo, valor fornecido: " + baseFontSize
            );
        }
        
        if (minMultiplier <= 0 || maxMultiplier <= 0) {
            throw new IllegalArgumentException(
                "Multiplicadores devem ser positivos. Min: " + minMultiplier + ", Max: " + maxMultiplier
            );
        }
        
        if (minMultiplier > maxMultiplier) {
            throw new IllegalArgumentException(
                "minMultiplier (" + minMultiplier + ") deve ser <= maxMultiplier (" + maxMultiplier + ")"
            );
        }
    }
    

    /**
     * Registra uma chamada do método calculateFontSizeMultiplier em arquivo JSON.
     */
    private static void logToJson(String source, String target, String fontName, 
                                  double baseFontSize, double minMultiplier, double maxMultiplier,
                                  double sourceWidth, double targetWidth, double ratio, double multiplier,
                                  String status) {
        synchronized (LOG_LOCK) {
            try {
                // Cria diretório se não existir
                Path logDir = Paths.get(LOG_DIRECTORY);
                if (!Files.exists(logDir)) {
                    Files.createDirectories(logDir);
                }
                
                // Caminho completo do arquivo de log
                Path logFile = logDir.resolve(LOG_FILE_NAME);
                
                // Carrega ou cria o array JSON
                ArrayNode logArray;
                if (Files.exists(logFile)) {
                    try {
                        logArray = (ArrayNode) JSON_MAPPER.readTree(logFile.toFile());
                    } catch (Exception e) {
                        // Se houver erro ao ler, cria novo array
                        logArray = JSON_MAPPER.createArrayNode();
                    }
                } else {
                    logArray = JSON_MAPPER.createArrayNode();
                }
                
                // Cria objeto de log
                ObjectNode logEntry = JSON_MAPPER.createObjectNode();
                logEntry.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                logEntry.put("source", source);
                logEntry.put("target", target);
                logEntry.put("fontName", fontName);
                logEntry.put("baseFontSize", baseFontSize);
                logEntry.put("minMultiplier", minMultiplier);
                logEntry.put("maxMultiplier", maxMultiplier);
                logEntry.put("sourceWidth", sourceWidth);
                logEntry.put("targetWidth", targetWidth);
                logEntry.put("ratio", ratio);
                logEntry.put("multiplier", multiplier);
                logEntry.put("status", status != null ? status : "SUCESSO");
                
                // Adiciona ao array
                logArray.add(logEntry);
                
                // Salva no arquivo
                JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(logFile.toFile(), logArray);
                
            } catch (Exception e) {
                // Não interrompe o fluxo principal se houver erro no log
                LOGGER.log(Level.WARNING, "Erro ao registrar log JSON: " + e.getMessage(), e);
            }
        }
    }
    
    public static void cleanup() {
        GraphicsContext context = GRAPHICS_CONTEXT.get();
        if (context != null) {
            context.dispose();
            GRAPHICS_CONTEXT.remove();
        }
        LOGGER.fine("Recursos gráficos liberados");
    }
    
    /**
     * Classe interna para encapsular o contexto gráfico reutilizável.
     */
    private static class GraphicsContext {
        private final BufferedImage image;
        private final Graphics2D graphics;
        
        GraphicsContext() {
            // Cria uma imagem mínima apenas para obter o Graphics2D
            this.image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            this.graphics = image.createGraphics();
            
            // Configura renderização de alta qualidade
            graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING, 
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );
            graphics.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON
            );
        }
        
        Graphics2D getGraphics() {
            return graphics;
        }
        
        void dispose() {
            graphics.dispose();
        }
    }
}