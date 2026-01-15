package com.magmatranslation.xliffconverter.core;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;

public class XliffConverter {

    public static String extractFileFromXLiff(File fileXLIFF) {

        try {
            String contentFile = Files.readString(Paths.get(fileXLIFF.getAbsolutePath()));

            Pattern pattern = Pattern.compile(
                    "<internal-file\\s+form=\"base64\">\\s*(.*?)\\s*</internal-file>",
                    Pattern.DOTALL
            );

            Matcher matcher = pattern.matcher(contentFile);

            return matcher.group(1).trim();

        } catch (java.io.IOException e) {

            System.err.println("Erro na class XliffConverter ao ler o arquivo XLIFF: " + e.getMessage());

            return null;

        } catch (java.util.regex.PatternSyntaxException e) {

            System.err.println("Erro no padrão de regex: " + e.getMessage());

            return null;

        } catch (java.lang.IllegalStateException e) {

            System.err.println("Erro ao processar o matcher: " + e.getMessage());

            return null;
        }

    }

    public static String updateDocxWithXLIFF(List<Event> eventListXliff, List<Event> eventListDocx, String langTarget, boolean reduceFont) {
        LocaleId trgLoc = LocaleId.fromString(langTarget);

        Map<String, ITextUnit> docxMap = new HashMap<>();

        for (Event docxEvent : eventListDocx) {
            
            if (docxEvent.isTextUnit()) {
                
                ITextUnit docxTU = docxEvent.getTextUnit();

                docxMap.put(docxTU.getId(), docxTU);
            }
        }

        // Cria o JsonHandler apenas se reduceFont estiver ativado
        JsonHandler jsonHandler = reduceFont ? new JsonHandler("document/json") : null;

        for (Event xliffEvent : eventListXliff) {
            if (!xliffEvent.isTextUnit()) continue;

            ITextUnit xliffTU = xliffEvent.getTextUnit();

            String id = xliffTU.getId();

            ITextUnit docxTU = docxMap.get(id);

            if (docxTU != null) {
                // Obtém source e target como texto puro (sem tags XML)
                TextContainer xliffSource = xliffTU.getSource();

                TextContainer xliffTarget = xliffTU.getTarget(trgLoc);

                
                String sourceText = "";
                String targetText = "";
                
                if (xliffSource != null) {
                    sourceText = xliffSource.toString().replaceAll("<[^>]+>", "").trim();
                }
                
                if (xliffTarget != null) {
                    targetText = xliffTarget.toString().replaceAll("<[^>]+>", "").trim();
                }
                
                String xliffTargetString = xliffTarget != null ? xliffTarget.toString() : "";

                if (xliffTargetString != null && !xliffTargetString.isEmpty()) {

                    if (reduceFont) {
                        // Adiciona ao JSON para processamento posterior
                        jsonHandler.addSegment(id, sourceText, targetText);
                
                        // Adiciona o ID ao conteúdo para remoção posterior
                        TextFragment textFragment = new TextFragment("[" + id + "]" + xliffTarget.toString());
                        xliffTarget.setContent(textFragment);
                    }
                    // Se reduceFont = false, apenas usa o target sem modificações

                    docxTU.setTarget(trgLoc, xliffTarget);
                }
                
            
            }
        }
        
        // Salva o JSON apenas se reduceFont estiver ativado
        if (reduceFont && jsonHandler != null) {
            jsonHandler.save();
            return jsonHandler.getFileName();
        }
        
        // Retorna null se não houver redução de fonte
        return null;

    }
}