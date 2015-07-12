package com.jetbrains.idear.actions.recognition;

import org.jetbrains.annotations.NotNull;

public class InlineActionRecognizer implements ActionRecognizer {

    @Override
    public boolean isMatching(@NotNull String sentence) {
        return sentence.contains("inline");
    }

    @Override
    public ActionCallInfo getActionInfo(@NotNull String sentence) {
        if (!isMatching(sentence)) return null;
        return new ActionCallInfo("Inline", null);
    }
}
