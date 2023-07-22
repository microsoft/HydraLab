package com.microsoft.hydralab.center.intelligence;

public interface PromptComposer {
    String compose(String prompt, String[] args);
}
