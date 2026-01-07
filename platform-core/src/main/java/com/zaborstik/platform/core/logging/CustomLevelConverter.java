package com.zaborstik.platform.core.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ANSIConstants;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

/**
 * Кастомный конвертер для цветов уровней логирования:
 * - INFO - обычный текст (без цвета)
 * - WARN - желтый
 * - ERROR - красный
 */
public class CustomLevelConverter extends ForegroundCompositeConverterBase<ILoggingEvent> {

    @Override
    protected String getForegroundColorCode(ILoggingEvent event) {
        Level level = event.getLevel();
        switch (level.levelInt) {
            case Level.ERROR_INT:
                return ANSIConstants.RED_FG;
            case Level.WARN_INT:
                return ANSIConstants.YELLOW_FG;
            case Level.INFO_INT:
                return ANSIConstants.DEFAULT_FG; // обычный текст
            case Level.DEBUG_INT:
                return ANSIConstants.CYAN_FG;
            case Level.TRACE_INT:
                return ANSIConstants.MAGENTA_FG;
            default:
                return ANSIConstants.DEFAULT_FG;
        }
    }
}

