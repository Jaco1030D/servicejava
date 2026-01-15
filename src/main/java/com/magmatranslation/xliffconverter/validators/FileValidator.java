package com.magmatranslation.xliffconverter.validators;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileValidator {
    
    // Mapa com os tipos de arquivo e suas extensões aceitas
    private static final Map<String, List<String>> SUPPORTED_EXTENSIONS = new HashMap<>();
    
    static {
        // Configuração das extensões suportadas por tipo de arquivo
        SUPPORTED_EXTENSIONS.put("DOCX", Arrays.asList(".docx", ".doc"));
        SUPPORTED_EXTENSIONS.put("XLIFF", Arrays.asList(".xlf", ".xliff"));
        SUPPORTED_EXTENSIONS.put("XML", Arrays.asList(".xml"));
        SUPPORTED_EXTENSIONS.put("TXT", Arrays.asList(".txt"));
    }
    
    /**
     * Valida se o arquivo enviado corresponde ao tipo esperado
     * 
     * @param expectedFileType Tipo de arquivo esperado (ex: "DOCX", "XLIFF")
     * @param originalFilename Nome original do arquivo com extensão
     * @return ValidationResult com o resultado da validação
     */
    public static ValidationResult validateFileType(String expectedFileType, String originalFilename) {
        
        // Verifica se o nome do arquivo não é nulo ou vazio
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return new ValidationResult(false, "Nome do arquivo não pode ser vazio.");
        }
        
        // Verifica se o tipo esperado está configurado
        if (expectedFileType == null || !SUPPORTED_EXTENSIONS.containsKey(expectedFileType.toUpperCase())) {
            return new ValidationResult(false, "Tipo de arquivo '" + expectedFileType + "' não é suportado.");
        }
        
        // Extrai a extensão do arquivo
        String extension = extractExtension(originalFilename);
        
        if (extension == null || extension.isEmpty()) {
            return new ValidationResult(false, "Arquivo não possui extensão válida.");
        }
        
        // Obtém as extensões aceitas para o tipo esperado
        List<String> acceptedExtensions = SUPPORTED_EXTENSIONS.get(expectedFileType.toUpperCase());
        
        // Verifica se a extensão do arquivo está na lista de aceitas
        if (acceptedExtensions.contains(extension.toLowerCase())) {
            return new ValidationResult(true, "Arquivo válido.");
        }
        
        // Monta mensagem de erro com as extensões aceitas
        String acceptedExtensionsStr = String.join(", ", acceptedExtensions);
        String message = String.format(
            "Arquivo não suportado. O tipo esperado é '%s' com extensões: %s. Recebido: '%s'",
            expectedFileType,
            acceptedExtensionsStr,
            extension
        );
        
        return new ValidationResult(false, message);
    }
    
    /**
     * Extrai a extensão do arquivo incluindo o ponto (ex: ".docx")
     * 
     * @param filename Nome do arquivo
     * @return Extensão do arquivo com o ponto, ou null se não houver extensão
     */
    private static String extractExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return null;
        }
        
        return filename.substring(lastDotIndex);
    }
    
    /**
     * Classe interna para representar o resultado da validação
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
