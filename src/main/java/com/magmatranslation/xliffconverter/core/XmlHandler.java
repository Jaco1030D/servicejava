package com.magmatranslation.xliffconverter.core;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

public class XmlHandler {

    private final Document doc;

    public XmlHandler(String pathFile) {

        this.doc = loadDocument(pathFile);
    
    }

    private Document loadDocument(String path) {
        try {
            return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new File(path));

        } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException | java.io.IOException e) {
            
            return null;
        }
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
