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

public class ExtensionCustomLogic extends CallbackAdapter {
    private Nandbox.Api api;

    private static final String DEFAULT_CITY = "Cairo";
    private static final String API_BASE_URL = "https://api.openweathermap.org";
    private static final String API_KEY = "921fa338a9960aa7629daff7765c5505";

    public static void main(String[] args) throws Exception {
        String TOKEN = "12345678901234567890";
        NandboxClient client = NandboxClient.get();
        client.connect(TOKEN, new ExtensionCustomLogic());
    }

    @Override
    public void onConnect(Nandbox.Api api) {
        this.api = api;
    }

    @Override
    public void onReceive(IncomingMessage incomingMsg) {
        if (incomingMsg == null || api == null) {
            return;
        }
        if (incomingMsg.getChat() == null || incomingMsg.getFrom() == null) {
            return;
        }

        String chatId = incomingMsg.getChat().getId();
        String text = incomingMsg.getText();
        String reference = Utils.getUniqueId();
        String userId = incomingMsg.getFrom().getId();
        String appId = incomingMsg.getAppId();
        Integer chatSettings = incomingMsg.getChatSettings();

        if (chatId == null || userId == null || appId == null) {
            return;
        }
        if (text == null) {
            return;
        }

        String trimmed = text.trim();
        if (trimmed.length() == 0) {
            return;
        }

        try {
            if (equalsAnyIgnoreCase(trimmed, "/start", "start", "/help", "help")) {
                String help = "Cairo Weather at Your Command\n\n" +
                        "Commands:\n" +
                        "- /weather : current weather for Cairo\n" +
                        "- /weather <city> : current weather for a city\n" +
                        "- /forecast : forecast for Cairo (first 8 entries, 3-hour intervals)\n" +
                        "- /forecast <city> : forecast for a city (first 8 entries)\n\n" +
                        "Examples:\n" +
                        "/weather\n" +
                        "/weather Giza\n" +
                        "/forecast Cairo\n";

                api.sendText(chatId, help, reference, null, userId, 0, false, chatSettings, null, null, null, appId);
                return;
            }

            if (startsWithCommand(trimmed, "/weather") || startsWithCommand(trimmed, "weather")) {
                String city = extractArgument(trimmed);
                if (city == null || city.length() == 0) {
                    city = DEFAULT_CITY;
                }
                String responseText = getCurrentWeatherText(city);
                api.sendText(chatId, responseText, reference, null, userId, 0, false, chatSettings, null, null, null, appId);
                return;
            }

            if (startsWithCommand(trimmed, "/forecast") || startsWithCommand(trimmed, "forecast")) {
                String city2 = extractArgument(trimmed);
                if (city2 == null || city2.length() == 0) {
                    city2 = DEFAULT_CITY;
                }
                String responseText2 = getForecastText(city2);
                api.sendText(chatId, responseText2, reference, null, userId, 0, false, chatSettings, null, null, null, appId);
                return;
            }

            String unknown = "Unknown command. Type /help for available commands.";
            api.sendText(chatId, unknown, reference, null, userId, 0, false, chatSettings, null, null, null, appId);
        } catch (Exception e) {
            String err = "Sorry, something went wrong while fetching the weather.";
            try {
                api.sendText(chatId, err, reference, null, userId, 0, false, chatSettings, null, null, null, appId);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onReceive(JSONObject obj) {
        if (obj == null) {
            return;
        }
        Object type = obj.get("type");
        if (type != null) {
            String t = String.valueOf(type).toLowerCase();
            if (t.indexOf("message") >= 0 || t.indexOf("incoming") >= 0 || t.indexOf("chat") >= 0) {
                return;
            }
        }
        if (obj.get("message") != null || obj.get("incomingMessage") != null || obj.get("chat") != null || obj.get("text") != null) {
            return;
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

    private static boolean equalsAnyIgnoreCase(String s, String a, String b, String c, String d) {
        if (s == null) {
            return false;
        }
        return s.equalsIgnoreCase(a) || s.equalsIgnoreCase(b) || s.equalsIgnoreCase(c) || s.equalsIgnoreCase(d);
    }

    private static boolean startsWithCommand(String text, String cmd) {
        if (text == null || cmd == null) {
            return false;
        }
        String t = text.trim();
        String c = cmd.trim();
        if (t.equalsIgnoreCase(c)) {
            return true;
        }
        if (t.length() > c.length() && t.substring(0, c.length()).equalsIgnoreCase(c)) {
            char next = t.charAt(c.length());
            return next == ' ' || next == '\t' || next == '\n' || next == '\r';
        }
        return false;
    }

    private static String extractArgument(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        int space = t.indexOf(' ');
        if (space < 0) {
            space = t.indexOf('\t');
        }
        if (space < 0) {
            return "";
        }
        return t.substring(space + 1).trim();
    }

    private static String getCurrentWeatherText(String city) throws Exception {
        String encodedCity = urlEncode(city);
        String endpoint = API_BASE_URL + "/data/2.5/weather?q=" + encodedCity + "&appid=" + urlEncode(API_KEY) + "&units=metric";
        JSONObject json = httpGetJson(endpoint);
        if (json == null) {
            return "Could not fetch weather data right now.";
        }
        if (json.get("error") != null) {
            return "Weather service error: " + String.valueOf(json.get("error"));
        }

        String name = asString(json.get("name"));
        JSONObject main = asObject(json.get("main"));
        JSONArray weatherArr = asArray(json.get("weather"));
        JSONObject wind = asObject(json.get("wind"));

        String desc = "";
        if (weatherArr != null && weatherArr.size() > 0) {
            JSONObject w0 = asObject(weatherArr.get(0));
            desc = capitalize(asString(w0 != null ? w0.get("description") : null));
        }

        Double temp = asDouble(main != null ? main.get("temp") : null);
        Double feels = asDouble(main != null ? main.get("feels_like") : null);
        Integer humidity = asInt(main != null ? main.get("humidity") : null);
        Double windSpeed = asDouble(wind != null ? wind.get("speed") : null);

        StringBuffer sb = new StringBuffer();
        sb.append("Current weather");
        if (name != null && name.length() > 0) {
            sb.append(" in ").append(name);
        } else {
            sb.append(" for ").append(city);
        }
        sb.append(":\n");

        if (desc != null && desc.length() > 0) {
            sb.append("- ").append(desc).append("\n");
        }
        if (temp != null) {
            sb.append("- Temperature: ").append(format1(temp)).append(" \u00B0C\n");
        }
        if (feels != null) {
            sb.append("- Feels like: ").append(format1(feels)).append(" \u00B0C\n");
        }
        if (humidity != null) {
            sb.append("- Humidity: ").append(humidity).append("%\n");
        }
        if (windSpeed != null) {
            sb.append("- Wind: ").append(format1(windSpeed)).append(" m/s\n");
        }

        String out = sb.toString().trim();
        if (out.length() == 0) {
            return "Weather data is currently unavailable.";
        }
        return out;
    }

    private static String getForecastText(String city) throws Exception {
        String encodedCity = urlEncode(city);
        String endpoint = API_BASE_URL + "/data/2.5/forecast?q=" + encodedCity + "&appid=" + urlEncode(API_KEY) + "&units=metric";
        JSONObject json = httpGetJson(endpoint);
        if (json == null) {
            return "Could not fetch forecast data right now.";
        }
        if (json.get("error") != null) {
            return "Weather service error: " + String.valueOf(json.get("error"));
        }

        JSONObject cityObj = asObject(json.get("city"));
        String name = asString(cityObj != null ? cityObj.get("name") : null);
        String country = asString(cityObj != null ? cityObj.get("country") : null);

        JSONArray list = asArray(json.get("list"));
        if (list == null || list.size() == 0) {
            return "No forecast data available.";
        }

        StringBuffer sb = new StringBuffer();
        sb.append("Forecast");
        if (name != null && name.length() > 0) {
            sb.append(" for ").append(name);
            if (country != null && country.length() > 0) {
                sb.append(", ").append(country);
            }
        } else {
            sb.append(" for ").append(city);
        }
        sb.append(":\n");

        int maxItems = list.size();
        if (maxItems > 8) {
            maxItems = 8;
        }

        for (int i = 0; i < maxItems; i++) {
            JSONObject item = asObject(list.get(i));
            if (item == null) {
                continue;
            }
            String dtTxt = asString(item.get("dt_txt"));
            JSONObject main = asObject(item.get("main"));
            JSONArray weatherArr = asArray(item.get("weather"));
            JSONObject wind = asObject(item.get("wind"));

            Double temp = asDouble(main != null ? main.get("temp") : null);
            Integer humidity = asInt(main != null ? main.get("humidity") : null);
            Double windSpeed = asDouble(wind != null ? wind.get("speed") : null);

            String desc = "";
            if (weatherArr != null && weatherArr.size() > 0) {
                JSONObject w0 = asObject(weatherArr.get(0));
                desc = capitalize(asString(w0 != null ? w0.get("description") : null));
            }

            if (dtTxt == null || dtTxt.length() == 0) {
                dtTxt = "Item " + (i + 1);
            }

            sb.append("- ").append(dtTxt);
            if (desc != null && desc.length() > 0) {
                sb.append(": ").append(desc);
            }
            boolean added = false;
            if (temp != null) {
                sb.append(" | ").append(format1(temp)).append(" \u00B0C");
                added = true;
            }
            if (humidity != null) {
                sb.append(added ? ", " : " | ");
                sb.append("Humidity ").append(humidity).append("%");
                added = true;
            }
            if (windSpeed != null) {
                sb.append(added ? ", " : " | ");
                sb.append("Wind ").append(format1(windSpeed)).append(" m/s");
            }
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    private static JSONObject httpGetJson(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }

            String body = readAll(is);
            if (body == null || body.trim().length() == 0) {
                return null;
            }

            JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
            Object parsed = parser.parse(body);
            if (!(parsed instanceof JSONObject)) {
                return null;
            }

            JSONObject obj = (JSONObject) parsed;
            if (code >= 200 && code < 300) {
                return obj;
            }

            Object msg = obj.get("message");
            if (msg != null) {
                JSONObject err = new JSONObject();
                err.put("error", String.valueOf(msg));
                return err;
            }
            JSONObject err2 = new JSONObject();
            err2.put("error", "HTTP " + code);
            return err2;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readAll(InputStream is) throws IOException {
        if (is == null) {
            return null;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private static String urlEncode(String s) throws UnsupportedEncodingException {
        if (s == null) {
            return "";
        }
        return URLEncoder.encode(s, "UTF-8");
    }

    private static JSONObject asObject(Object o) {
        if (o instanceof JSONObject) {
            return (JSONObject) o;
        }
        return null;
    }

    private static JSONArray asArray(Object o) {
        if (o instanceof JSONArray) {
            return (JSONArray) o;
        }
        return null;
    }

    private static String asString(Object o) {
        if (o == null) {
            return null;
        }
        return String.valueOf(o);
    }

    private static Double asDouble(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return new Double(((Number) o).doubleValue());
        }
        try {
            return new Double(Double.parseDouble(String.valueOf(o)));
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer asInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return new Integer(((Number) o).intValue());
        }
        try {
            return new Integer(Integer.parseInt(String.valueOf(o)));
        } catch (Exception e) {
            return null;
        }
    }

    private static String capitalize(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.length() == 0) {
            return "";
        }
        char first = t.charAt(0);
        char up = Character.toUpperCase(first);
        if (t.length() == 1) {
            return String.valueOf(up);
        }
        return String.valueOf(up) + t.substring(1);
    }

    private static String format1(Double d) {
        if (d == null) {
            return "";
        }
        double v = d.doubleValue();
        long scaled = Math.round(v * 10.0);
        long whole = scaled / 10;
        long frac = Math.abs(scaled % 10);
        return String.valueOf(whole) + "." + String.valueOf(frac);
    }
}
