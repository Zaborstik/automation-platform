package com.zaborstik.platform.agent.client;

/**
 * Исключение, возникающее при работе с агентом.
 */
public class AgentException extends Exception {
    private static final long serialVersionUID = 1L;

    public AgentException(String message) {
        super(message);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}

