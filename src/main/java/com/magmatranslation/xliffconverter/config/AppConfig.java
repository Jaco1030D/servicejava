package com.magmatranslation.xliffconverter.config;

public class AppConfig {
    private final String getFilePath;
    // private final String filePathXliff;
    private final String filePathParams;
    private final String filePathOutput;
    // private final String filePathOutputXML;
    private final String filePathSegmentRules;
    private final String langSource;
    private final String langTarget;
    private final String action;
    private final String typeFile;
    
    // Construtor
    public AppConfig(String[] args) {
        boolean create = true;
        this.filePathParams = setValue(args, "params\\p.fprm", 1);
        this.filePathSegmentRules = setValue(args, "params\\p.srx", 2);
        this.langSource = setValue(args, "en-US", 3);
        this.langTarget = setValue(args, "pt-BR", 4);
        if (create) {

            this.getFilePath = setValue(args, "document\\docx\\a.docx", 5);
            this.filePathOutput = setValue(args, "files", 6);
            this.action = setValue(args, "CREATEFILEXLIFF", 7);
            // this.filePathXliff = setValue(args, "", 8);
            this.typeFile = setValue(args, "DOCX", 0);
            // this.filePathOutputXML = setValue(args, "C:\\Users\\jacoa\\Desktop\\java\\filtersexercices\\src\\main\\java\\br\\com\\jaco\\", 10);
            
        } else {
            this.getFilePath = setValue(args, "files\\XLIFF\\a.docx.xlf", 5);
            this.filePathOutput = setValue(args, "files\\DOCX\\a.docx", 6);
            this.action = setValue(args, "TRANSLATEFILE", 7);
            // this.filePathXliff = setValue(args, "", 8);
            this.typeFile = setValue(args, "XLIFF", 0);
            // this.filePathOutputXML = setValue(args, "C:\\Users\\jacoa\\Desktop\\java\\filtersexercices\\src\\main\\java\\br\\com\\jaco\\", 10);
            
        }
        
    }

    // Getters
    public String getFilePath() { return getFilePath; }
    // public String getFilePathXliff() { return filePathXliff; }
    public String getFilePathParams() { return filePathParams; }
    public String getFilePathOutput() { return filePathOutput; }
    // public String getFilePathOutputXML() { return filePathOutputXML; }
    public String getFilePathSegmentRules() { return filePathSegmentRules; }
    public String getLangSource() { return langSource; }
    public String getLangTarget() { return langTarget; }
    public String getAction() { return action; }
    public String getTypeFile() { return typeFile; }

    private String setValue(String[] args, String defaultValue, int index) {
        return (args.length > index && args[index] != null && !args[index].isEmpty())
            ? args[index]
            : defaultValue;
    }
}
