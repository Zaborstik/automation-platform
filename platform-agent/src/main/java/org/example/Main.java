package org.example;

import org.example.agent.example.AgentExample;

/**
 * Главный класс platform-agent.
 * 
 * Для запуска примера использования агента:
 * 1. Убедитесь, что Playwright сервер запущен:
 *    cd src/main/resources && npm install && node playwright-server.js
 * 2. Запустите этот класс
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Platform Agent - UI Agent for Automation Platform");
        System.out.println("==================================================");
        System.out.println();
        System.out.println("Make sure Playwright server is running:");
        System.out.println("  cd platform-agent/src/main/resources");
        System.out.println("  npm install");
        System.out.println("  node playwright-server.js");
        System.out.println();
        
        // Запускаем пример
        AgentExample.main(args);
    }
}