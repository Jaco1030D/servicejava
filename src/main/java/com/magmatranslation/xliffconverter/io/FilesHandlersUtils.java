package com.magmatranslation.xliffconverter.io;


import java.io.File;
import net.sf.okapi.common.ISegmenter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.filters.openxml.ConditionalParameters;
import net.sf.okapi.lib.segmentation.SRXDocument;

public class FilesHandlersUtils {

    public static void configFilter(IFilter filter, String filePath) {
    
        File paramFile = new File(filePath);
    
        System.out.println("Caminho do arquivo de parâmetros: " + paramFile.getPath());
    
        if (!paramFile.exists()) {
            System.out.println("Erro: O arquivo de parâmetros não foi encontrado! Verifique o caminho: " + paramFile.getAbsolutePath());
            return;
        }

        try {
            ConditionalParameters params = new ConditionalParameters();
            
            params.load(paramFile.toURI().toURL(), false);

            filter.setParameters(params);

        } catch (java.net.MalformedURLException e) {
            System.out.println("Erro: URL malformada ao carregar o arquivo de parâmetros: " + e.getMessage());
            
        } catch (Exception e) {
            System.out.println("Erro inesperado ao carregar o arquivo de parâmetros: " + e.getMessage());
            
        }
    }

    public static ISegmenter getSegmenter(String filePath, LocaleId locale) {
        
        try {
            File srxFile = new File(filePath);

            if (!srxFile.exists()) {
                System.err.println("Arquivo SRX não encontrado: " + srxFile.getAbsolutePath());
                return null;
            }
            
            SRXDocument doc = new SRXDocument();
        
            doc.loadRules(filePath);

            // Verifica se alguma regra foi carregada
            // O novo SRX usa mapeamento automático baseado no locale
            if (doc.getAllLanguageRules() == null || doc.getAllLanguageRules().isEmpty()) {
                System.err.println("Nenhuma regra de segmentação foi carregada do arquivo SRX.");
                return null;
            }

            // Compila as regras apropriadas para o locale
            // O SRXDocument usa o <maprules> para determinar quais regras aplicar
            ISegmenter segmenter = doc.compileLanguageRules(locale, null);

            if (segmenter == null) {
                System.err.println("Falha ao compilar regras de segmentação para o locale: " + locale.toString());
                return null;
            }
            
            return segmenter;
            
        } catch (Exception e) {
            System.err.println("Erro ao carregar regras de segmentação: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

    }

}
