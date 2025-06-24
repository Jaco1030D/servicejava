package com.magmatranslation.xliffconverter.BodyRequests;

public class CreateXliffFromDocx {
    public String langSource;
    public String langTarget;

    public CreateXliffFromDocx() {}

    public String getLangSource() {
        return langSource;
    }

    public void setLangSource(String langSource) {
        this.langSource = langSource;
    }

    public String getLangTarget() {
        return langTarget;
    }

    public void setLangTarget(String langTarget) {
        this.langTarget = langTarget;
    }
}

// langSource, langTarget, typeFile