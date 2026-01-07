package com.zaborstik.platform;

import com.zaborstik.platform.agent.example.AgentExample;

/**
 * Главный класс platform-agent.
 * 
 * Для запуска примера использования агента:
 * 1. Убедитесь, что Playwright сервер запущен:
 *    cd src/main/resources && npm install && node playwright-server.js
 * 2. Запустите этот класс
 * 
 * Main class for platform-agent.
 * 
 * To run agent usage example:
 * 1. Make sure Playwright server is running:
 *    cd src/main/resources && npm install && node playwright-server.js
 * 2. Run this class
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
        // Run example
        AgentExample.main(args);
    }
}