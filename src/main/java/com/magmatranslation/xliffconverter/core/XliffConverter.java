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

            System.err.println("Erro ao ler o arquivo XLIFF: " + e.getMessage());

            return null;

        } catch (java.util.regex.PatternSyntaxException e) {

            System.err.println("Erro no padrão de regex: " + e.getMessage());

            return null;

        } catch (java.lang.IllegalStateException e) {

            System.err.println("Erro ao processar o matcher: " + e.getMessage());

            return null;
        }

    }

    public static void updateDocxWithXLIFF(List<Event> eventListXliff, List<Event> eventListDocx, String langTarget) {
        LocaleId trgLoc = LocaleId.fromString(langTarget);

        Map<String, ITextUnit> docxMap = new HashMap<>();

        for (Event docxEvent : eventListDocx) {
            
            if (docxEvent.isTextUnit()) {
                
                ITextUnit docxTU = docxEvent.getTextUnit();

                //Como adicionei o itextunit aqui, eu consigo alterar o valor dele dentro de docxmap que será refletido nele
                docxMap.put(docxTU.getId(), docxTU);
            }
        }

        for (Event xliffEvent : eventListXliff) {
            if (!xliffEvent.isTextUnit()) continue;

            ITextUnit xliffTU = xliffEvent.getTextUnit();

            String id = xliffTU.getId();
            
            ITextUnit docxTU = docxMap.get(id);

            if (docxTU != null) {

                docxTU.setTarget(trgLoc, xliffTU.getTarget(trgLoc));
            
            }
        }

    }
}
