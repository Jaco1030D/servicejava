package com.magmatranslation.xliffconverter.config;

import java.io.File;

import com.magmatranslation.xliffconverter.cli.AppConfig;

import net.sf.okapi.common.filters.IFilter;

public class FileProcessorConfig {
    public IFilter filter;
    public File file;
    public File fileXLIFF;
    public boolean param;
    public String langSource;
    public String langTarget;
    public String filePathParams;
    public String filePathSegmentRules;
    public String typeFile;
    public String filePathOutput;

    // Construtor para inicializar os valores
    public FileProcessorConfig(AppConfig config, IFilter filter, File file, boolean param, File fileXLIFF) {
        this.filter = filter;
        this.file = file;
        this.fileXLIFF = fileXLIFF;
        this.param = param;
        this.langSource = config.getLangSource();
        this.langTarget = config.getLangTarget();
        this.filePathParams = config.getFilePathParams();
        this.filePathSegmentRules = config.getFilePathSegmentRules();
        this.typeFile = config.getTypeFile();
        this.filePathOutput = config.getFilePathOutput(); 
    }
}