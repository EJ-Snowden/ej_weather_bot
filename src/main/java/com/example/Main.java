package com.example;

public class Main {
    public static void main(String[] args) {
        WeatherBot bot = new WeatherBot();
        bot.sendDailyWeatherUpdate(YOUR_CHAT_ID);
    }
}