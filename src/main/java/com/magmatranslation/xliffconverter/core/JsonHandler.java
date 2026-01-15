package com.magmatranslation.xliffconverter.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Classe para manipular arquivos JSON específicos para armazenar informações de tradução.
 * Permite criar, editar, adicionar e apagar objetos JSON.
 */
public class JsonHandler {
    
    private String directoryPath;
    private String fileName;
    private ObjectMapper objectMapper;
    private ArrayNode jsonArray;
    
    /**
     * Construtor que recebe o caminho da pasta para salvamento.
     * Gera um nome único baseado em data e hora.
     * 
     * @param directoryPath Caminho da pasta onde o JSON será salvo (ex: "document/json")
     */
    public JsonHandler(String directoryPath) {
        this.directoryPath = directoryPath;
        this.fileName = generateUniqueFileName();
        this.objectMapper = new ObjectMapper();
        this.jsonArray = objectMapper.createArrayNode();
        
        // Cria o diretório se não existir
        createDirectoryIfNotExists();
    }
    
    /**
     * Gera um nome de arquivo único baseado em data e hora.
     * Formato: translation_data_YYYYMMDDHHmmss.json
     * 
     * @return Nome do arquivo único
     */
    private String generateUniqueFileName() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = now.format(formatter);
        return "translation_data_" + timestamp + ".json";
    }
    
    /**
     * Retorna o nome do arquivo JSON gerado.
     * 
     * @return Nome do arquivo
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * Cria o diretório se ele não existir.
     */
    private void createDirectoryIfNotExists() {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Diretório criado: " + directoryPath);
            }
        } catch (IOException e) {
            System.err.println("Erro ao criar diretório: " + e.getMessage());
        }
    }
    
    /**
     * Adiciona um novo objeto ao array JSON.
     * 
     * @param id ID do segmento
     * @param source Texto fonte (original)
     * @param target Texto traduzido (target)
     */
    public void addSegment(String id, String source, String target) {
        ObjectNode segment = objectMapper.createObjectNode();
        segment.put("id", id);
        segment.put("source", source);
        segment.put("target", target);
        
        jsonArray.add(segment);
    }
    
    /**
     * Salva o array JSON em arquivo.
     * 
     * @return true se salvou com sucesso, false caso contrário
     */
    public boolean save() {
        try {
            String filePath = directoryPath + File.separator + fileName;
            File file = new File(filePath);
            
            // Garante que o diretório existe
            file.getParentFile().mkdirs();
            
            // Escreve o JSON formatado
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, jsonArray);
            
            System.out.println("JSON salvo em: " + filePath);
            return true;
            
        } catch (IOException e) {
            System.err.println("Erro ao salvar JSON: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Carrega um arquivo JSON existente.
     * 
     * @return true se carregou com sucesso, false caso contrário
     */
    public boolean load() {
        try {
            String filePath = directoryPath + File.separator + fileName;
            File file = new File(filePath);
            
            if (!file.exists()) {
                System.out.println("Arquivo JSON não existe, será criado novo.");
                jsonArray = objectMapper.createArrayNode();
                return true;
            }
            
            jsonArray = (ArrayNode) objectMapper.readTree(file);
            System.out.println("JSON carregado de: " + filePath);
            return true;
            
        } catch (IOException e) {
            System.err.println("Erro ao carregar JSON: " + e.getMessage());
            jsonArray = objectMapper.createArrayNode();
            return false;
        }
    }
    
    /**
     * Edita um objeto existente no array pelo ID.
     * 
     * @param id ID do segmento a editar
     * @param source Novo texto fonte (pode ser null para não alterar)
     * @param target Novo texto target (pode ser null para não alterar)
     * @return true se encontrou e editou, false caso contrário
     */
    public boolean editSegment(String id, String source, String target) {
        for (int i = 0; i < jsonArray.size(); i++) {
            ObjectNode segment = (ObjectNode) jsonArray.get(i);
            if (segment.get("id").asText().equals(id)) {
                if (source != null) {
                    segment.put("source", source);
                }
                if (target != null) {
                    segment.put("target", target);
                }
                return true;
            }
        }
        return false;
    }
    
    /**
     * Remove um objeto do array pelo ID.
     * 
     * @param id ID do segmento a remover
     * @return true se encontrou e removeu, false caso contrário
     */
    public boolean deleteSegment(String id) {
        for (int i = 0; i < jsonArray.size(); i++) {
            ObjectNode segment = (ObjectNode) jsonArray.get(i);
            if (segment.get("id").asText().equals(id)) {
                jsonArray.remove(i);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Limpa todo o array JSON.
     */
    public void clear() {
        jsonArray = objectMapper.createArrayNode();
    }
    
    /**
     * Retorna o número de segmentos no array.
     * 
     * @return Número de segmentos
     */
    public int getSize() {
        return jsonArray.size();
    }
    
    /**
     * Retorna o caminho completo do arquivo JSON.
     * 
     * @return Caminho completo do arquivo
     */
    public String getFilePath() {
        return directoryPath + File.separator + fileName;
    }
}

