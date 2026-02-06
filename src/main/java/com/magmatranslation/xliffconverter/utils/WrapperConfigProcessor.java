package com.magmatranslation.xliffconverter.utils;

import com.magmatranslation.xliffconverter.config.FileProcessorConfig;
import com.magmatranslation.xliffconverter.core.FileReaderWithOkapi;

public class WrapperConfigProcessor {
    
    private FileProcessorConfig fileProcessorConfig;
    private FileReaderWithOkapi fileReader;

    public WrapperConfigProcessor(FileProcessorConfig fileProcessorConfig, FileReaderWithOkapi fileReader) {

        this.fileProcessorConfig = fileProcessorConfig;
        this.fileReader = fileReader;
    }

    public FileProcessorConfig getFileProcessorConfig() {

        return fileProcessorConfig;
    }

    public FileReaderWithOkapi getFileReader() {

        return fileReader;
    }
}
