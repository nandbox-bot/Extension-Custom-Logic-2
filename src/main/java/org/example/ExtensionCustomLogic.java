package org.example;

import com.nandbox.bots.api.Nandbox;
import com.nandbox.bots.api.NandboxClient;
import com.nandbox.bots.api.data.*;
import com.nandbox.bots.api.inmessages.*;
import com.nandbox.bots.api.outmessages.*;
import com.nandbox.bots.api.util.*;
import com.nandbox.bots.api.test.*;

import net.minidev.json.*;
import net.minidev.json.parser.JSONParser;

import org.example.CallbackAdapter;
import java.io.FileInputStream;
import java.io.IOException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Properties;

public class ExtensionCustomLogic extends CallbackAdapter {
    private Nandbox.Api api;

    private static final String DEFAULT_CITY = "Cairo";
    private static final String OPENWEATHER_ENDPOINT = "https://api.openweathermap.org/data/2.5/weather";
    private static final String DEFAULT_UNITS = "metric";

    private String openWeatherApiKey;

    public static void main(String[] args) throws Exception {
        String TOKEN = getRequiredToken(args);
        NandboxClient client = NandboxClient.get();
        client.connect(TOKEN, new ExtensionCustomLogic());
    }

    private static String getRequiredToken(String[] args) throws Exception {
        if (args != null && args.length > 0 && args[0] != null && args[0].trim().length() > 0) {
            return args[0].trim();
        }
        String env = System.getenv("NANDBOX_TOKEN");
        if (env != null && env.trim().length() > 0) {
            return env.trim();
        }
        Properties p = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream("bot.properties");
            p.load(in);
            String t = p.getProperty("TOKEN");
            if (t != null && t.trim().length() > 0) {
                return t.trim();
            }
        } catch (IOException e) {
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException e2) {}
            }
        }
        throw new Exception("Missing bot token. Provide as first argument, or set NANDBOX_TOKEN, or add bot.properties with TOKEN=<your_token>.");
    }

    @Override
    public void onConnect(Nandbox.Api api) {
        this.api = api;
        this.openWeatherApiKey = loadOpenWeatherApiKey();
    }

    private String loadOpenWeatherApiKey() {
        String env = System.getenv("OPENWEATHER_API_KEY");
        if (env != null && env.trim().length() > 0) {
            return env.trim();
        }
        Properties p = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream("bot.properties");
            p.load(in);
            String k = p.getProperty("OPENWEATHER_API_KEY");
            if (k != null && k.trim().length() > 0) {
                return k.trim();
            }
        } catch (IOException e) {
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException e2) {}
            }
        }
        return null;
    }

    @Override
    public void onReceive(IncomingMessage incomingMsg) {
        if (incomingMsg == null || api == null) {
            return;
        }

        String chatId = incomingMsg.getChat() != null ? incomingMsg.getChat().getId() : null;
        String text = incomingMsg.getText();
        String reference = Utils.getUniqueId();
        String userId = incomingMsg.getFrom() != null ? incomingMsg.getFrom().getId() : null;
        String appId = incomingMsg.getAppId();
        Integer chatSettings = incomingMsg.getChatSettings();

        if (chatId == null || userId == null || appId == null) {
            return;
        }

        String cleaned = text == null ? "" : text.trim();
        if (cleaned.length() == 0) {
            sendText(chatId, "Type /weather to get the current Cairo weather. Example: /weather or /weather Cairo", reference, userId, chatSettings, appId);
            return;
        }

        String lower = cleaned.toLowerCase(Locale.ENGLISH);

        if (lower.equals("/start") || lower.equals("start") || lower.equals("help") || lower.equals("/help")) {
            String help = "Your Cairo Weather Companion!\n\nCommands:\n" +
                    "/weather  - current weather in Cairo\n" +
                    "/weather <city> - current weather for a city\n" +
                    "/ping - health check";
            sendText(chatId, help, reference, userId, chatSettings, appId);
            return;
        }

        if (lower.equals("/ping") || lower.equals("ping")) {
            sendText(chatId, "pong", reference, userId, chatSettings, appId);
            return;
        }

        if (lower.startsWith("/weather") || lower.startsWith("weather")) {
            String city = extractCity(cleaned);
            if (city == null || city.trim().length() == 0) {
                city = DEFAULT_CITY;
            }

            if (openWeatherApiKey == null || openWeatherApiKey.trim().length() == 0) {
                sendText(chatId,
                        "Weather API key is not configured. Set environment variable OPENWEATHER_API_KEY or add bot.properties with OPENWEATHER_API_KEY=<your_key>.",
                        reference, userId, chatSettings, appId);
                return;
            }

            try {
                WeatherResult r = fetchCurrentWeather(city, openWeatherApiKey);
                String msg = formatWeatherMessage(r);
                sendText(chatId, msg, reference, userId, chatSettings, appId);
            } catch (Exception ex) {
                String safe = ex.getMessage() == null ? "Unknown error" : ex.getMessage();
                sendText(chatId, "Unable to get weather right now: " + safe, reference, userId, chatSettings, appId);
            }
            return;
        }

        sendText(chatId, "Unknown command. Type /help", reference, userId, chatSettings, appId);
    }

    private void sendText(String chatId, String msg, String reference, String userId, Integer chatSettings, String appId) {
        api.sendText(
                chatId,
                msg,
                reference,
                null,
                userId,
                0,
                false,
                chatSettings,
                null,
                null,
                null,
                appId
        );
    }

    private String extractCity(String cleaned) {
        String s = cleaned.trim();
        if (s.length() == 0) return null;

        String[] parts = splitBySpace(s);
        if (parts.length == 0) return null;

        String cmd = parts[0].toLowerCase(Locale.ENGLISH);
        if (!(cmd.equals("/weather") || cmd.equals("weather"))) {
            return null;
        }
        if (parts.length == 1) {
            return DEFAULT_CITY;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            if (parts[i] == null || parts[i].length() == 0) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(parts[i]);
        }
        return sb.toString().trim();
    }

    private String[] splitBySpace(String s) {
        int len = s.length();
        java.util.ArrayList tokens = new java.util.ArrayList();
        StringBuffer current = new StringBuffer();
        boolean inToken = false;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                if (inToken) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    inToken = false;
                }
            } else {
                current.append(c);
                inToken = true;
            }
        }
        if (inToken) {
            tokens.add(current.toString());
        }
        String[] arr = new String[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            arr[i] = (String) tokens.get(i);
        }
        return arr;
    }

    private WeatherResult fetchCurrentWeather(String city, String apiKey) throws Exception {
        String q = urlEncode(city);
        String u = OPENWEATHER_ENDPOINT + "?q=" + q + "&appid=" + urlEncode(apiKey) + "&units=" + urlEncode(DEFAULT_UNITS);

        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            URL url = new URL(u);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            if (status >= 200 && status < 300) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }

            String body = readAll(is);
            if (status < 200 || status >= 300) {
                String m = parseOpenWeatherError(body);
                throw new Exception(m);
            }

            JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
            Object parsed = parser.parse(body);
            if (!(parsed instanceof JSONObject)) {
                throw new Exception("Invalid response from weather service");
            }
            JSONObject obj = (JSONObject) parsed;

            WeatherResult r = new WeatherResult();
            r.requestedCity = city;
            r.cityName = getString(obj, "name");

            JSONObject sys = getObject(obj, "sys");
            r.country = sys != null ? getString(sys, "country") : null;

            JSONObject main = getObject(obj, "main");
            r.temp = main != null ? getDouble(main, "temp") : null;
            r.feelsLike = main != null ? getDouble(main, "feels_like") : null;
            r.humidity = main != null ? getLong(main, "humidity") : null;

            JSONObject wind = getObject(obj, "wind");
            r.windSpeed = wind != null ? getDouble(wind, "speed") : null;

            JSONArray weatherArr = getArray(obj, "weather");
            if (weatherArr != null && weatherArr.size() > 0 && weatherArr.get(0) instanceof JSONObject) {
                JSONObject w0 = (JSONObject) weatherArr.get(0);
                r.description = getString(w0, "description");
                r.condition = getString(w0, "main");
            }

            if (r.cityName == null || r.cityName.trim().length() == 0) {
                r.cityName = city;
            }

            return r;
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException e) {}
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String parseOpenWeatherError(String body) {
        if (body == null || body.trim().length() == 0) {
            return "Weather service error";
        }
        try {
            JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
            Object parsed = parser.parse(body);
            if (parsed instanceof JSONObject) {
                JSONObject obj = (JSONObject) parsed;
                String msg = getString(obj, "message");
                if (msg != null && msg.trim().length() > 0) {
                    return "Weather service error: " + msg;
                }
            }
        } catch (Exception e) {
        }
        String trimmed = body.trim();
        if (trimmed.length() > 180) trimmed = trimmed.substring(0, 180);
        return "Weather service error: " + trimmed;
    }

    private String formatWeatherMessage(WeatherResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Weather for ");
        sb.append(r.cityName != null ? r.cityName : r.requestedCity);
        if (r.country != null && r.country.trim().length() > 0) {
            sb.append(", ").append(r.country);
        }
        sb.append(":\n");

        if (r.description != null && r.description.trim().length() > 0) {
            sb.append("Condition: ").append(capitalize(r.description)).append("\n");
        } else if (r.condition != null && r.condition.trim().length() > 0) {
            sb.append("Condition: ").append(r.condition).append("\n");
        }

        if (r.temp != null) {
            sb.append("Temperature: ").append(format1(r.temp)).append(" °C\n");
        }
        if (r.feelsLike != null) {
            sb.append("Feels like: ").append(format1(r.feelsLike)).append(" °C\n");
        }
        if (r.humidity != null) {
            sb.append("Humidity: ").append(r.humidity).append("%\n");
        }
        if (r.windSpeed != null) {
            sb.append("Wind: ").append(format1(r.windSpeed)).append(" m/s\n");
        }

        return sb.toString().trim();
    }

    private String format1(Double d) {
        if (d == null) return "";
        double v = d.doubleValue();
        long rounded = Math.round(v * 10.0);
        double one = rounded / 10.0;
        String s = String.valueOf(one);
        if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        return s;
    }

    private String capitalize(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.length() == 0) return s;
        char first = s.charAt(0);
        char up = Character.toUpperCase(first);
        if (s.length() == 1) return String.valueOf(up);
        return up + s.substring(1);
    }

    private String urlEncode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, "UTF-8");
    }

    private String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private JSONObject getObject(JSONObject obj, String key) {
        if (obj == null) return null;
        Object v = obj.get(key);
        if (v instanceof JSONObject) return (JSONObject) v;
        return null;
    }

    private JSONArray getArray(JSONObject obj, String key) {
        if (obj == null) return null;
        Object v = obj.get(key);
        if (v instanceof JSONArray) return (JSONArray) v;
        return null;
    }

    private String getString(JSONObject obj, String key) {
        if (obj == null) return null;
        Object v = obj.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private Double getDouble(JSONObject obj, String key) {
        if (obj == null) return null;
        Object v = obj.get(key);
        if (v == null) return null;
        if (v instanceof Number) return new Double(((Number) v).doubleValue());
        try {
            return new Double(Double.parseDouble(String.valueOf(v)));
        } catch (Exception e) {
            return null;
        }
    }

    private Long getLong(JSONObject obj, String key) {
        if (obj == null) return null;
        Object v = obj.get(key);
        if (v == null) return null;
        if (v instanceof Number) return new Long(((Number) v).longValue());
        try {
            return new Long(Long.parseLong(String.valueOf(v)));
        } catch (Exception e) {
            return null;
        }
    }

    private static class WeatherResult {
        String requestedCity;
        String cityName;
        String country;
        String condition;
        String description;
        Double temp;
        Double feelsLike;
        Long humidity;
        Double windSpeed;
    }

    @Override
    public void onReceive(JSONObject obj) {
        if (obj != null) {
            Object t = obj.get("type");
            if (t != null && String.valueOf(t).toLowerCase(Locale.ENGLISH).indexOf("message") >= 0) {
                return;
            }
            if (obj.get("message") != null || obj.get("chat") != null || obj.get("from") != null) {
                return;
            }
        }
    }

    @Override
    public void onClose() {}

    @Override
    public void onError() {}

    @Override
    public void onChatMenuCallBack(ChatMenuCallback chatMenuCallback) {}

    @Override
    public void onInlineMessageCallback(InlineMessageCallback inlineMsgCallback) {}

    @Override
    public void onMessagAckCallback(MessageAck msgAck) {}

    @Override
    public void onUserJoinedBot(User user) {}

    @Override
    public void onChatMember(ChatMember chatMember) {}

    @Override
    public void onChatAdministrators(ChatAdministrators chatAdministrators) {}

    @Override
    public void userStartedBot(User user) {}

    @Override
    public void onMyProfile(User user) {}

    @Override
    public void onProductDetail(ProductItemResponse productItem) {}

    @Override
    public void onCollectionProduct(GetProductCollectionResponse collectionProduct) {}

    @Override
    public void listCollectionItemResponse(ListCollectionItemResponse collections) {}

    @Override
    public void onUserDetails(User user, String appId) {}

    @Override
    public void userStoppedBot(User user) {}

    @Override
    public void userLeftBot(User user) {}

    @Override
    public void permanentUrl(PermanentUrl permenantUrl) {}

    @Override
    public void onChatDetails(Chat chat, String appId) {}

    @Override
    public void onInlineSearh(InlineSearch inlineSearch) {}

    @Override
    public void onBlackListPattern(Pattern pattern) {}

    @Override
    public void onWhiteListPattern(Pattern pattern) {}

    @Override
    public void onBlackList(BlackList blackList) {}

    @Override
    public void onDeleteBlackList(List_ak blackList) {}

    @Override
    public void onWhiteList(WhiteList whiteList) {}

    @Override
    public void onDeleteWhiteList(List_ak whiteList) {}

    @Override
    public void onScheduleMessage(IncomingMessage incomingScheduleMsg) {}

    @Override
    public void onWorkflowDetails(WorkflowDetails workflowDetails) {}

    @Override
    public void onCreateChat(Chat chat) {}

    @Override
    public void onMenuCallBack(MenuCallback menuCallback) {}
}
