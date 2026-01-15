package com.magmatranslation.xliffconverter.core;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class XmlHandler {

    private final Document doc;

    public XmlHandler(String pathFile) {

        this.doc = loadDocument(pathFile);
    
    }

    private Document loadDocument(String path) {
        try {
            // Lê o conteúdo do arquivo como string
            File file = new File(path);
            String xmlContent = Files.readString(file.toPath());
            
            // Corrige os '&' que não são entidades válidas
            String fixedXml = fixInvalidAmpersands(xmlContent);
            
            // Faz o parse do XML corrigido
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            return factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(fixedXml)));

        } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException | java.io.IOException e) {
            System.err.println("Erro ao carregar XML: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Corrige os '&' que não são parte de entidades XML válidas
     * Entidades válidas: &lt; &gt; &amp; &quot; &apos; &#decimal; &#xhex;
     */
    private String fixInvalidAmpersands(String xml) {
        // Pattern para identificar '&' seguido de algo que NÃO é uma entidade válida
        // Entidades válidas: &lt; &gt; &amp; &quot; &apos; &#digits; &#xhex;
        Pattern pattern = Pattern.compile("&(?!(lt|gt|amp|quot|apos|#\\d+|#x[0-9a-fA-F]+);)");
        
        Matcher matcher = pattern.matcher(xml);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            // Substitui '&' inválido por '&amp;'
            matcher.appendReplacement(result, "&amp;");
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    // Método que extrai o conteúdo com base em uma expressão XPath
    public String extractContentByTag(String xpathTag) {
        if (doc == null) return null;

        try {
            
            XPath xpath = XPathFactory.newInstance().newXPath();
            
            XPathExpression expr = xpath.compile(xpathTag);
            
            return expr.evaluate(doc).trim();
        
        } catch (javax.xml.xpath.XPathExpressionException | java.lang.NullPointerException e) {
            
            return null;
        }
    }
}
