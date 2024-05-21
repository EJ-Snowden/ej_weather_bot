package com.example;

public class Main {
    public static void main(String[] args) {
        WeatherBot bot = new WeatherBot();
        Thread botThread = new Thread(new BotRunner(bot));
        botThread.start();

        try {
            botThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}