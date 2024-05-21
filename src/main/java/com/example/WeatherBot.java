package com.example;

import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

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
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equalsIgnoreCase("/start")) {
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                message.setText("Welcome! You will receive daily weather updates.");
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getWeatherData() {
        String location = "YOUR_LOCATION";
        String urlString = "https://api.open-meteo.com/v1/forecast?latitude=35.6895&longitude=139.6917&current_weather=true";

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            // Check if the response is successful
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HttpResponseCode: " + responseCode);
            }

            Scanner sc = new Scanner(url.openStream());
            StringBuilder inline = new StringBuilder();
            while (sc.hasNext()) {
                inline.append(sc.nextLine());
            }
            sc.close();

            JSONObject json = new JSONObject(inline.toString());
            JSONObject currentWeather = json.getJSONObject("current_weather");

            String temperature = currentWeather.getString("temperature");
            String weatherDescription = currentWeather.getString("weather_description");

            return "Current temperature: " + temperature + "°C\nWeather: " + weatherDescription;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unable to fetch weather data.";
    }

    public void sendDailyWeatherUpdate(long chatId) {
        String weatherData = getWeatherData();
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(weatherData);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
