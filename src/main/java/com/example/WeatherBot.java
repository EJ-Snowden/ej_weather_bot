package com.example;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
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
    private double[] temperaturesArr = new double[24];
    private String[] timesArr = new String[24];
    private double[] windSpeedsArr = new double[24];
    private double[] humiditiesArr = new double[24];
    private double[] precipitationsArr = new double[24];
    double highestTemperature = Double.MIN_VALUE;
    double lowestTemperature = Double.MAX_VALUE;
    double avgTemperature = Double.MAX_VALUE;
    double highestWindSpeed = Double.MIN_VALUE;
    double lowestWindSpeed = Double.MAX_VALUE;
    double highestHumidity = Double.MIN_VALUE;
    double lowestHumidity = Double.MAX_VALUE;

    double highestPrecipitation = -1;
    double totalPrecipitation = 0;

    String highestTemperatureTime = "";
    String lowestTemperatureTime = "";
    String highestWindSpeedTime = "";
    String lowestWindSpeedTime = "";
    String highestHumidityTime = "";
    String lowestHumidityTime = "";
    String highestPrecipitationTime = "";
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
                if (messageText.equalsIgnoreCase("/start")) {
                    sendLocationRequest(chatId);
                } else if (messageText.equalsIgnoreCase("/location")) {
                    sendLocationRequest(chatId);
                }
            } else if (update.getMessage().hasLocation()) {
                latitude = update.getMessage().getLocation().getLatitude();
                longitude = update.getMessage().getLocation().getLongitude();

                saveChatIdToFile();
                handleLocation(chatId, latitude, longitude);
                sendWelcomeMessage(chatId);
            }
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
                    chatId = Long.parseLong(parts[0]);
                    latitude = Double.parseDouble(parts[1]);
                    longitude = Double.parseDouble(parts[2]);
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

    public String getWeatherData() {
        String urlString = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude + "&hourly=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m&forecast_days=1";

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

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
            JSONObject hourly = json.getJSONObject("hourly");

            JSONArray times = hourly.getJSONArray("time");
            JSONArray temperatures = hourly.getJSONArray("temperature_2m");
            JSONArray humidities = hourly.getJSONArray("relative_humidity_2m");
            JSONArray precipitations = hourly.getJSONArray("precipitation");
            JSONArray windSpeeds = hourly.getJSONArray("wind_speed_10m");

            for (int i = 0; i < temperatures.length(); i++) {
                temperaturesArr[i] = temperatures.getDouble(i);
                String fullTime = times.getString(i);
                String timePart = fullTime.split("T")[1];
                timesArr[i] = timePart;
                windSpeedsArr[i] = windSpeeds.getDouble(i);
                humiditiesArr[i] = humidities.getDouble(i);
                precipitationsArr[i] = precipitations.getDouble(i);
            }

            calculateValues();

            StringBuilder result = new StringBuilder();
            result.append("Weather for today (").append(times.getString(0).split("T")[0]).append(") :\n\n");
            result.append("Average Temperature (6-23): ").append(String.format("%.2f", avgTemperature)).append("°C\n");
            result.append("Highest Temperature (6-23): ").append(String.format("%.2f", highestTemperature)).append("°C at ").append(highestTemperatureTime).append("\n");
            result.append("Lowest Temperature (6-23): ").append(String.format("%.2f", lowestTemperature)).append("°C at ").append(lowestTemperatureTime).append("\n\n");
            result.append("Highest WindSpeed (6-23): ").append(String.format("%.2f", highestWindSpeed)).append("m/s at ").append(highestWindSpeedTime).append("\n");
            result.append("Lowest WindSpeed (6-23): ").append(String.format("%.2f", lowestWindSpeed)).append("m/s at ").append(lowestWindSpeedTime).append("\n\n");
            result.append("Highest Humidity (6-23): ").append(String.format("%.0f", highestHumidity)).append("% at ").append(highestHumidityTime).append("\n");
            result.append("Lowest Humidity (6-23): ").append(String.format("%.0f", lowestHumidity)).append("% at ").append(lowestHumidityTime).append("\n\n");
            result.append("Highest Precipitation (6-23): ").append(String.format("%.0f", highestPrecipitation)).append("mm at ").append(highestPrecipitationTime).append("\n");
            result.append("Total Precipitation for the day (6-23): ").append(String.format("%.0f", totalPrecipitation)).append("mm\n");

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unable to fetch weather data.";
    }

    public void sendTemperatureGraph(long chatId, double[] temperatures, String[] times) {
        try {
            File chartFile = WeatherGraph.createTemperatureChart(temperatures, times);
            SendPhoto photo = new SendPhoto();
            photo.setChatId(String.valueOf(chatId));
            photo.setPhoto(new InputFile(chartFile));
            execute(photo);
        } catch (IOException | TelegramApiException e) {
            e.printStackTrace();
        }
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

        sendTemperatureGraph(chatId, temperaturesArr, timesArr);
    }

    public void calculateValues(){
        double avg = 0;
        for (int i = 6; i <= 23; i++) {
            double temperature = temperaturesArr[i];
            double windSpeed = windSpeedsArr[i];
            double humidity = humiditiesArr[i];
            String time = timesArr[i];
            double precipitation = precipitationsArr[i];

            if (temperature > highestTemperature) {
                highestTemperature = temperature;
                highestTemperatureTime = time;
            }
            if (temperature < lowestTemperature) {
                lowestTemperature = temperature;
                lowestTemperatureTime = time;
            }
            if (windSpeed > highestWindSpeed) {
                highestWindSpeed = windSpeed;
                highestWindSpeedTime = time;
            }
            if (windSpeed < lowestWindSpeed) {
                lowestWindSpeed = windSpeed;
                lowestWindSpeedTime = time;
            }
            if (humidity > highestHumidity) {
                highestHumidity = humidity;
                highestHumidityTime = time;
            }
            if (humidity < lowestHumidity) {
                lowestHumidity = humidity;
                lowestHumidityTime = time;
            }
            if (precipitation > highestPrecipitation) {
                highestPrecipitation = precipitation;
                highestPrecipitationTime = time;
            }
            avg += temperaturesArr[i];
            totalPrecipitation += precipitation;
        }
        avgTemperature = avg / 18;
    }
}
