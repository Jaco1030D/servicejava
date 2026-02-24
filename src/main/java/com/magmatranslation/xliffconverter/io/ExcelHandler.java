package com.magmatranslation.xliffconverter.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.magmatranslation.xliffconverter.config.FileProcessorConfig;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.ISegmenter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;

//Essa class so aceita a versão mais recente do excel, o xlsx, as outras versões devem ser convertidas para xlsx antes de ser processado.
public class ExcelHandler {

    public List<Event> readExcelFile(FileProcessorConfig config) {
        List<Event> eventList = new ArrayList<>();
        System.out.println("Lendo arquivo Excel: " + config.file.getName());
        
        // Verifica se o arquivo é CSV e converte para XLSX se necessário
        if (isCsvFile(config.file)) {
            System.out.println("Arquivo CSV detectado. Convertendo para XLSX...");
            convertCsvToXlsx(config);
        }
        
        // Verifica se o arquivo é XLS e converte para XLSX se necessário
        if (isXlsFile(config.file)) {
            System.out.println("Arquivo XLS detectado. Convertendo para XLSX...");
            convertXlsToXlsx(config);
        }
        
        LocaleId srcLoc = LocaleId.fromString(config.langSource);
        LocaleId trgLoc = LocaleId.fromString(config.langTarget);

        try (
            RawDocument rawDocument = new RawDocument(config.file.toURI(), "UTF-8", srcLoc, trgLoc)
            ) {

            System.out.println(rawDocument);

            config.filter.open(rawDocument);


            if (config.param && config.filePathParams != null) {
                FilesHandlersUtils.configFilter(config.filter, config.filePathParams);
            }

            ISegmenter segmenter = FilesHandlersUtils.getSegmenter(config.filePathSegmentRules, srcLoc);
            
            System.out.println("Passou do segmenter");
            while (config.filter.hasNext()) {
                System.out.println("Passou do hasNext");

                System.out.println(config.filter.getDisplayName());
                
                Event event = config.filter.next();

                System.out.println(event);
                
                if (event.isTextUnit()) {

                    ITextUnit textUnit = event.getTextUnit();

                    TextContainer sourceContainer = textUnit.getSource();

                    if (config.filePathSegmentRules != null) {

                        if (segmenter != null) {
                            segmenter.computeSegments(sourceContainer);

                            sourceContainer.getSegments().create(segmenter.getRanges());

                            textUnit.setSource(sourceContainer);
                        }
                    }

                }

                eventList.add(event);
            }

            config.filter.close();
        } catch (Exception e) {
            
            System.err.println("Erro ao Ler o arquivo excel: " + e.getMessage());
        }

        return eventList;
    }

    /**
     * Verifica se o arquivo é um arquivo CSV baseado na extensão
     * 
     * @param file Arquivo a ser verificado
     * @return true se o arquivo é CSV, false caso contrário
     */
    public static boolean isCsvFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".csv");
    }

    /**
     * Converte um arquivo CSV para XLSX usando Apache POI
     * Atualiza o config.file com o novo arquivo XLSX criado
     * 
     * @param config Configuração do processamento de arquivo
     */
    public static void convertCsvToXlsx(FileProcessorConfig config) {
        File csvFile = config.file;
        File xlsxFile = null;
        
        try {
            // Cria um arquivo temporário XLSX
            String csvFileName = csvFile.getName();
            String baseName = csvFileName.substring(0, csvFileName.lastIndexOf('.'));
            xlsxFile = File.createTempFile(baseName + "_", ".xlsx");
            
            System.out.println("Convertendo CSV para XLSX: " + csvFile.getAbsolutePath() + " -> " + xlsxFile.getAbsolutePath());
            
            // Cria o workbook e a sheet
            XSSFWorkbook workBook = new XSSFWorkbook();
            XSSFSheet sheet = workBook.createSheet("sheet1");
            
            // Lê o arquivo CSV linha por linha
            String currentLine = null;
            int rowNum = 0;
            
            try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                while ((currentLine = br.readLine()) != null) {
                    // Divide a linha por vírgula
                    String[] str = currentLine.split(",");
                    
                    // Cria uma nova linha na planilha
                    XSSFRow currentRow = sheet.createRow(rowNum);
                    
                    // Preenche as células com os valores da linha CSV
                    for (int i = 0; i < str.length; i++) {
                        currentRow.createCell(i).setCellValue(str[i]);
                    }
                    
                    rowNum++;
                }
            }
            
            // Escreve o arquivo XLSX
            try (FileOutputStream fileOutputStream = new FileOutputStream(xlsxFile)) {
                workBook.write(fileOutputStream);
            }
            
            // Fecha o workbook
            workBook.close();
            
            // Atualiza o config.file com o novo arquivo XLSX
            config.file = xlsxFile;
            
            System.out.println("Conversão concluída com sucesso. Arquivo XLSX criado: " + xlsxFile.getAbsolutePath());
            
        } catch (Exception ex) {
            System.err.println("Erro ao converter CSV para XLSX: " + ex.getMessage());
            ex.printStackTrace();
            
            // Em caso de erro, tenta limpar o arquivo temporário se foi criado
            if (xlsxFile != null && xlsxFile.exists()) {
                xlsxFile.delete();
            }
            
            throw new RuntimeException("Falha ao converter CSV para XLSX: " + ex.getMessage(), ex);
        }
    }

    /**
     * Verifica se o arquivo é um arquivo XLS (formato antigo do Excel) baseado na extensão
     * 
     * @param file Arquivo a ser verificado
     * @return true se o arquivo é XLS, false caso contrário
     */
    public static boolean isXlsFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".xls");
    }

    /**
     * Converte um arquivo XLS (formato antigo) para XLSX usando Apache POI
     * Atualiza o config.file com o novo arquivo XLSX criado
     * 
     * @param config Configuração do processamento de arquivo
     */
    public static void convertXlsToXlsx(FileProcessorConfig config) {
        File xlsFile = config.file;
        File xlsxFile = null;
        
        try {
            // Cria um arquivo temporário XLSX
            String xlsFileName = xlsFile.getName();
            String baseName = xlsFileName.substring(0, xlsFileName.lastIndexOf('.'));
            xlsxFile = File.createTempFile(baseName + "_", ".xlsx");
            
            System.out.println("Convertendo XLS para XLSX: " + xlsFile.getAbsolutePath() + " -> " + xlsxFile.getAbsolutePath());
            
            // Lê o arquivo XLS antigo
            HSSFWorkbook hssfWorkbook = null;
            try (FileInputStream fis = new FileInputStream(xlsFile)) {
                hssfWorkbook = new HSSFWorkbook(fis);
            }
            
            // Cria o novo workbook XLSX
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
            
            // Copia todas as sheets do XLS para o XLSX
            for (int i = 0; i < hssfWorkbook.getNumberOfSheets(); i++) {
                HSSFSheet hssfSheet = hssfWorkbook.getSheetAt(i);
                String sheetName = hssfSheet.getSheetName();
                XSSFSheet xssfSheet = xssfWorkbook.createSheet(sheetName);
                
                // Copia todas as linhas
                for (int rowNum = 0; rowNum <= hssfSheet.getLastRowNum(); rowNum++) {
                    HSSFRow hssfRow = hssfSheet.getRow(rowNum);
                    if (hssfRow == null) {
                        continue;
                    }
                    
                    XSSFRow xssfRow = xssfSheet.createRow(rowNum);
                    
                    // Copia todas as células da linha
                    for (int cellNum = 0; cellNum < hssfRow.getLastCellNum(); cellNum++) {
                        HSSFCell hssfCell = hssfRow.getCell(cellNum);
                        if (hssfCell == null) {
                            continue;
                        }
                        
                        XSSFCell xssfCell = xssfRow.createCell(cellNum);
                        
                        // Copia o valor da célula baseado no tipo
                        CellType cellType = hssfCell.getCellType();
                        if (cellType == CellType.STRING) {
                            xssfCell.setCellValue(hssfCell.getStringCellValue());
                        } else if (cellType == CellType.NUMERIC) {
                            xssfCell.setCellValue(hssfCell.getNumericCellValue());
                        } else if (cellType == CellType.BOOLEAN) {
                            xssfCell.setCellValue(hssfCell.getBooleanCellValue());
                        } else if (cellType == CellType.FORMULA) {
                            xssfCell.setCellFormula(hssfCell.getCellFormula());
                        } else if (cellType == CellType.BLANK) {
                            xssfCell.setBlank();
                        } else {
                            // Para outros tipos, tenta converter para string
                            try {
                                xssfCell.setCellValue(hssfCell.toString());
                            } catch (Exception e) {
                                xssfCell.setCellValue("");
                            }
                        }
                    }
                }
            }
            
            // Fecha o workbook XLS antigo
            hssfWorkbook.close();
            
            // Escreve o arquivo XLSX
            try (FileOutputStream fileOutputStream = new FileOutputStream(xlsxFile)) {
                xssfWorkbook.write(fileOutputStream);
            }
            
            // Fecha o workbook XLSX
            xssfWorkbook.close();
            
            // Atualiza o config.file com o novo arquivo XLSX
            config.file = xlsxFile;
            
            System.out.println("Conversão concluída com sucesso. Arquivo XLSX criado: " + xlsxFile.getAbsolutePath());
            
        } catch (Exception ex) {
            System.err.println("Erro ao converter XLS para XLSX: " + ex.getMessage());
            ex.printStackTrace();
            
            // Em caso de erro, tenta limpar o arquivo temporário se foi criado
            if (xlsxFile != null && xlsxFile.exists()) {
                xlsxFile.delete();
            }
            
            throw new RuntimeException("Falha ao converter XLS para XLSX: " + ex.getMessage(), ex);
        }
    }
}
