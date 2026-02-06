package com.magmatranslation.xliffconverter.BodyRequests;

public class CreateXliffFromDocx {
    public String langSource;
    public String langTarget;
    public boolean reduceFont;

    public CreateXliffFromDocx() {}

    public String getLangSource() {
        return langSource;
    }

    public String getLangTarget() {
        return langTarget;
    }

    public boolean getReduceFont() {
        return reduceFont;
    }

    public void setLangSource(String langSource) {
        this.langSource = langSource;
    }

    public void setLangTarget(String langTarget) {
        this.langTarget = langTarget;
    }

    public void setReduceFont(boolean reduceFont) {
        this.reduceFont = reduceFont;
    }

}

// langSource, langTarget, typeFile