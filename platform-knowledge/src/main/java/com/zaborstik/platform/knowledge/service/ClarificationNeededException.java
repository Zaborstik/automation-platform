package com.zaborstik.platform.knowledge.service;

public class ClarificationNeededException extends RuntimeException {

    private final String question;

    public ClarificationNeededException(String question) {
        super(question);
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }
}
