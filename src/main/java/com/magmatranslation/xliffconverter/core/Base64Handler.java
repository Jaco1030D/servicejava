package com.magmatranslation.xliffconverter.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class Base64Handler {
    public static String createBase64(File file) {
        try {
            // 1. Ler o conteúdo do arquivo como bytes
            byte[] fileBytes = Files.readAllBytes(file.toPath());

            // 2. Codificar em Base64
            String base64String = Base64.getEncoder().encodeToString(fileBytes);

            // 3. Salvar o Base64 em um novo arquivo
            return base64String;

        } catch (IOException e) {

            return null;
        }

    }

    public static File createFileFromBase64(String base64String, String outputPath) {
        try {
            // 1. Decodificar a string Base64 para bytes
            byte[] decodedBytes = Base64.getDecoder().decode(base64String);
    
            // 2. Criar o arquivo de saída
            File outputFile = new File(outputPath);
            Files.write(outputFile.toPath(), decodedBytes);
    
            // 3. Retornar o objeto File
            return outputFile;
    
        } catch (IOException e) {
            
            return null;
        }
    }
}
