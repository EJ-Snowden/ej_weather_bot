package com.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import io.github.cdimascio.dotenv.Dotenv;

public class WeatherBot extends TelegramLongPollingBot {

    private final Dotenv dotenv = Dotenv.load();
    @Override
    public String getBotUsername() {
        return "ej_weather_bot";
    }

    @Override
    public String getBotToken() {
        return dotenv.get("BOT_TOKEN");
    }

    @Override
    public void onUpdateReceived(Update update) {

    }
}
