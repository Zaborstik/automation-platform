package com.zaborstik.platform.agent.client;

import java.io.Serial;

/**
 * Исключение, возникающее при работе с агентом.
 */
public class AgentException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public AgentException(String message) {
        super(message);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}

