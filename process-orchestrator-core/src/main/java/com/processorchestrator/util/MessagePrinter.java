package com.processorchestrator.util;

/**
 * Simple Java application that prints a message to the console.
 * Usage: java MessagePrinter "your message here"
 */
public class MessagePrinter {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java MessagePrinter \"your message here\"");
            System.exit(1);
        }
        
        String message = args[0];
        System.out.println(message);
        
        // Exit with success code
        System.exit(0);
    }
}
