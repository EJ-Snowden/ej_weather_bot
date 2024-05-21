package com.example;

public class BotRunner implements Runnable {
    private final WeatherBot myBot;

    public BotRunner(WeatherBot myBot) {
        this.myBot = myBot;
    }

    @Override
    public void run() {
        try {
            myBot.botConnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

