package com.example;

import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class WeatherBot extends TelegramLongPollingBot {

    private final Dotenv dotenv = Dotenv.load();
    public long chatId;
    private double latitude;
    private double longitude;
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
        if (update.hasMessage()) {
            String messageText = update.getMessage().getText();
            chatId = update.getMessage().getChatId();

            if (update.getMessage().hasText()) {
                // Handle commands
                if (messageText.equalsIgnoreCase("/start")) {
                    sendLocationRequest(chatId);
                } else if (messageText.equalsIgnoreCase("/location")) {
                    sendLocationRequest(chatId);
                }
            } else if (update.getMessage().hasLocation()) {
                // This block will handle location updates
                latitude = update.getMessage().getLocation().getLatitude();
                longitude = update.getMessage().getLocation().getLongitude();

                saveChatIdToFile();
                handleLocation(chatId, latitude, longitude);
                sendWelcomeMessage(chatId);
                sendDailyWeatherUpdate(chatId);
            }
        }
    }

    private void sendWelcomeMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Welcome! You will receive daily weather updates.");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private void handleLocation(long chatId, double latitude, double longitude) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Received your location: Latitude = " + latitude + ", Longitude = " + longitude);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void botConnect() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            botsApi.registerBot(this);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        File file = new File("chat_ids.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            List<String> entries = Files.readAllLines(Paths.get("chat_ids.txt"));
            for (String entry : entries) {
                String[] parts = entry.trim().split(" ");
                if (parts.length == 3) {
                    long chatId = Long.parseLong(parts[0]);
                    latitude = Double.parseDouble(parts[1]);
                    longitude = Double.parseDouble(parts[2]);
                    System.out.println("Sending update to: " + chatId);
                    sendDailyWeatherUpdate(chatId);
                } else {
                    System.out.println("Invalid entry format: " + entry);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveChatIdToFile() {
        String filePath = "chat_ids.txt";
        try {
            List<String> allLines = Files.readAllLines(Paths.get(filePath));
            if (!allLines.contains(String.valueOf(chatId))) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true));
                writer.write(chatId + " " + latitude + " " + longitude + "\n");
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendLocationRequest(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Please share your location.");

        // Create a keyboard with one button that requests location
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        KeyboardButton locationButton = new KeyboardButton("Share Location");
        locationButton.setRequestLocation(true); // This is what makes the button request location

        row.add(locationButton);
        keyboard.add(row);

        replyKeyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(replyKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public String getWeatherData() {
        String urlString = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude + "&hourly=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m&forecast_days=1";

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

            // Log the raw JSON response for debugging
            System.out.println("Raw JSON response: " + inline.toString());

            // Parse the JSON response
            JSONObject json = new JSONObject(inline.toString());

            // Extract required parameters
            JSONObject hourly = json.getJSONObject("hourly");

            // Assuming we want the first hour's data
            double temperature = hourly.getJSONArray("temperature_2m").getDouble(0);
            double humidity = hourly.getJSONArray("relative_humidity_2m").getDouble(0);
            double precipitation = hourly.getJSONArray("precipitation").getDouble(0);
            double windSpeed = hourly.getJSONArray("wind_speed_10m").getDouble(0);

            return "Temperature: " + temperature + "°C\n" +
                    "Humidity: " + humidity + "%\n" +
                    "Precipitation: " + precipitation + "mm\n" +
                    "Wind Speed: " + windSpeed + "m/s";

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
