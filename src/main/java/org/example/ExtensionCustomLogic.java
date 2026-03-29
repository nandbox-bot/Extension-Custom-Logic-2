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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class ExtensionCustomLogic extends CallbackAdapter {
    private Nandbox.Api api;

    private static final String API_BASE_URL = "http://api.weatherapi.com/v1/";
    private static final String API_KEY = "4e57492e251f4907834122714252304";

    private static final int HTTP_CONNECT_TIMEOUT_MS = 10000;
    private static final int HTTP_READ_TIMEOUT_MS = 15000;

    private static final String HELP_TEXT = "Your instant weather guide!\n\n" +
            "Commands:\n" +
            "- weather <city>\n" +
            "- weather <lat>,<lon>\n" +
            "- forecast <city> [days(1-10)]\n" +
            "- help\n\n" +
            "Examples:\n" +
            "weather London\n" +
            "weather 51.5072,-0.1276\n" +
            "forecast Cairo 3";

    public static void main(String[] args) throws Exception {
        String TOKEN = "90091783773743996:0:ujgAHdqSoVlBT6rowqQPGtuo7U4VbU";
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

        String chatId = incomingMsg.getChat() != null ? incomingMsg.getChat().getId() : null;
        String text = incomingMsg.getText();
        String reference = Utils.getUniqueId();
        String userId = incomingMsg.getFrom() != null ? incomingMsg.getFrom().getId() : null;
        String appId = incomingMsg.getAppId();
        Integer chatSettings = incomingMsg.getChatSettings();

        if (chatId == null || userId == null || appId == null) {
            return;
        }

        String trimmed = text == null ? "" : text.trim();
        if (trimmed.length() == 0) {
            sendTextSafe(chatId, "Type 'help' to see how to use the weather bot.", reference, userId, chatSettings, appId);
            return;
        }

        String lower = trimmed.toLowerCase();
        if (lower.equals("help") || lower.equals("/help") || lower.equals("start") || lower.equals("/start")) {
            sendTextSafe(chatId, HELP_TEXT, reference, userId, chatSettings, appId);
            return;
        }

        if (startsWithCommand(lower, "weather")) {
            String arg = extractArgs(trimmed, "weather");
            if (arg.length() == 0) {
                sendTextSafe(chatId, "Usage: weather <city> OR weather <lat>,<lon>", reference, userId, chatSettings, appId);
                return;
            }
            try {
                String reply = handleCurrentWeather(arg);
                sendTextSafe(chatId, reply, reference, userId, chatSettings, appId);
            } catch (Exception e) {
                sendTextSafe(chatId, "Sorry, I couldn't fetch the weather right now.", reference, userId, chatSettings, appId);
            }
            return;
        }

        if (startsWithCommand(lower, "forecast")) {
            String arg = extractArgs(trimmed, "forecast");
            if (arg.length() == 0) {
                sendTextSafe(chatId, "Usage: forecast <city> [days(1-10)]", reference, userId, chatSettings, appId);
                return;
            }
            try {
                String replyF = handleForecast(arg);
                sendTextSafe(chatId, replyF, reference, userId, chatSettings, appId);
            } catch (Exception e2) {
                sendTextSafe(chatId, "Sorry, I couldn't fetch the forecast right now.", reference, userId, chatSettings, appId);
            }
            return;
        }

        if (looksLikeLocationQuery(trimmed)) {
            try {
                String reply2 = handleCurrentWeather(trimmed);
                sendTextSafe(chatId, reply2, reference, userId, chatSettings, appId);
            } catch (Exception e3) {
                sendTextSafe(chatId, "Sorry, I couldn't fetch the weather right now.", reference, userId, chatSettings, appId);
            }
            return;
        }

        if (trimmed.length() >= 2) {
            try {
                String reply3 = handleCurrentWeather(trimmed);
                sendTextSafe(chatId, reply3, reference, userId, chatSettings, appId);
            } catch (Exception e4) {
                sendTextSafe(chatId, "Type 'help' for usage. Example: weather London", reference, userId, chatSettings, appId);
            }
            return;
        }

        sendTextSafe(chatId, "Type 'help' for usage.", reference, userId, chatSettings, appId);
    }

    @Override
    public void onReceive(JSONObject obj) {
        if (obj == null) {
            return;
        }
        Object t = obj.get("type");
        if (t != null) {
            String type = String.valueOf(t);
            if (type.toLowerCase().indexOf("message") >= 0 || type.toLowerCase().indexOf("incoming") >= 0) {
                return;
            }
        }
        if (obj.containsKey("message") || obj.containsKey("chat") || obj.containsKey("from")) {
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

    private void sendTextSafe(String chatId, String msg, String reference, String userId, Integer chatSettings, String appId) {
        try {
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
        } catch (Exception ignore) {
        }
    }

    private boolean startsWithCommand(String lowerTrimmed, String cmd) {
        if (lowerTrimmed == null) {
            return false;
        }
        if (lowerTrimmed.equals(cmd)) {
            return true;
        }
        return lowerTrimmed.startsWith(cmd + " ") || lowerTrimmed.startsWith("/" + cmd + " ") || lowerTrimmed.equals("/" + cmd);
    }

    private String extractArgs(String originalTrimmed, String cmd) {
        String s = originalTrimmed;
        if (s == null) {
            return "";
        }
        s = s.trim();
        if (s.startsWith("/")) {
            s = s.substring(1);
        }
        if (s.length() < cmd.length()) {
            return "";
        }
        if (!s.toLowerCase().startsWith(cmd)) {
            return "";
        }
        String rest = s.substring(cmd.length()).trim();
        return rest;
    }

    private boolean looksLikeLocationQuery(String s) {
        if (s == null) {
            return false;
        }
        String t = s.trim();
        int idx = t.indexOf(',');
        if (idx <= 0 || idx >= t.length() - 1) {
            return false;
        }
        String a = t.substring(0, idx).trim();
        String b = t.substring(idx + 1).trim();
        return isDecimal(a) && isDecimal(b);
    }

    private boolean isDecimal(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        try {
            Double.parseDouble(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String handleCurrentWeather(String query) throws Exception {
        String q = normalizeQuery(query);
        Map params = new HashMap();
        params.put("key", API_KEY);
        params.put("q", q);
        params.put("aqi", "no");

        String url = API_BASE_URL + "current.json" + "?" + toQueryString(params);
        JSONObject json = httpGetJson(url);

        JSONObject location = asObject(json.get("location"));
        JSONObject current = asObject(json.get("current"));

        String name = stringOrEmpty(location.get("name"));
        String region = stringOrEmpty(location.get("region"));
        String country = stringOrEmpty(location.get("country"));
        String localtime = stringOrEmpty(location.get("localtime"));

        Double tempC = asDouble(current.get("temp_c"));
        Double feelsC = asDouble(current.get("feelslike_c"));
        Integer humidity = asInt(current.get("humidity"));
        Double windKph = asDouble(current.get("wind_kph"));
        String windDir = stringOrEmpty(current.get("wind_dir"));
        Double precipMm = asDouble(current.get("precip_mm"));
        Integer isDay = asInt(current.get("is_day"));

        JSONObject condition = asObject(current.get("condition"));
        String condText = condition != null ? stringOrEmpty(condition.get("text")) : "";

        String where = joinNonEmpty(new String[] { name, region, country }, ", ");
        if (where.length() == 0) {
            where = q;
        }

        String dayNight = "";
        if (isDay != null) {
            dayNight = isDay.intValue() == 1 ? "Day" : "Night";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Current weather for ").append(where);
        if (localtime.length() > 0) {
            sb.append("\nLocal time: ").append(localtime);
        }
        if (condText.length() > 0) {
            sb.append("\nCondition: ").append(condText);
        }
        if (dayNight.length() > 0) {
            sb.append(" (").append(dayNight).append(")");
        }
        if (tempC != null) {
            sb.append("\nTemperature: ").append(format1(tempC)).append("°C");
        }
        if (feelsC != null) {
            sb.append(" (feels like ").append(format1(feelsC)).append("°C)");
        }
        if (humidity != null) {
            sb.append("\nHumidity: ").append(humidity).append("%");
        }
        if (windKph != null) {
            sb.append("\nWind: ").append(format1(windKph)).append(" kph");
            if (windDir.length() > 0) {
                sb.append(" ").append(windDir);
            }
        }
        if (precipMm != null) {
            sb.append("\nPrecipitation: ").append(format1(precipMm)).append(" mm");
        }
        return sb.toString();
    }

    private String handleForecast(String args) throws Exception {
        String a = args == null ? "" : args.trim();
        if (a.length() == 0) {
            throw new Exception("empty args");
        }

        int days = 3;
        String query = a;

        String[] parts = splitByWhitespace(a);
        if (parts.length >= 2) {
            String last = parts[parts.length - 1];
            if (isInteger(last)) {
                days = Integer.parseInt(last);
                if (days < 1) {
                    days = 1;
                }
                if (days > 10) {
                    days = 10;
                }
                query = a.substring(0, a.lastIndexOf(last)).trim();
            }
        }

        query = normalizeQuery(query);
        Map params = new HashMap();
        params.put("key", API_KEY);
        params.put("q", query);
        params.put("days", String.valueOf(days));
        params.put("aqi", "no");
        params.put("alerts", "no");

        String url = API_BASE_URL + "forecast.json" + "?" + toQueryString(params);
        JSONObject json = httpGetJson(url);

        JSONObject location = asObject(json.get("location"));
        JSONObject forecast = asObject(json.get("forecast"));
        JSONArray forecastday = forecast != null ? asArray(forecast.get("forecastday")) : null;

        String name = location != null ? stringOrEmpty(location.get("name")) : "";
        String region = location != null ? stringOrEmpty(location.get("region")) : "";
        String country = location != null ? stringOrEmpty(location.get("country")) : "";
        String where = joinNonEmpty(new String[] { name, region, country }, ", ");
        if (where.length() == 0) {
            where = query;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Forecast for ").append(where).append(" (next ").append(days).append(" day");
        if (days != 1) {
            sb.append("s");
        }
        sb.append(")");

        if (forecastday == null || forecastday.size() == 0) {
            sb.append("\nNo forecast data available.");
            return sb.toString();
        }

        for (int i = 0; i < forecastday.size(); i++) {
            JSONObject fd = asObject(forecastday.get(i));
            if (fd == null) {
                continue;
            }
            String date = stringOrEmpty(fd.get("date"));
            JSONObject day = asObject(fd.get("day"));
            Double maxt = day != null ? asDouble(day.get("maxtemp_c")) : null;
            Double mint = day != null ? asDouble(day.get("mintemp_c")) : null;
            Double avgt = day != null ? asDouble(day.get("avgtemp_c")) : null;
            Double maxwind = day != null ? asDouble(day.get("maxwind_kph")) : null;
            Double totalprecip = day != null ? asDouble(day.get("totalprecip_mm")) : null;
            Integer avghumidity = day != null ? asInt(day.get("avghumidity")) : null;
            JSONObject cond = day != null ? asObject(day.get("condition")) : null;
            String condText = cond != null ? stringOrEmpty(cond.get("text")) : "";

            sb.append("\n\n");
            if (date.length() > 0) {
                sb.append(date);
            } else {
                sb.append("Day ").append(i + 1);
            }
            if (condText.length() > 0) {
                sb.append(" - ").append(condText);
            }
            if (mint != null || maxt != null) {
                sb.append("\nTemp: ");
                if (mint != null) {
                    sb.append(format1(mint)).append("°C");
                } else {
                    sb.append("?");
                }
                sb.append(" to ");
                if (maxt != null) {
                    sb.append(format1(maxt)).append("°C");
                } else {
                    sb.append("?");
                }
                if (avgt != null) {
                    sb.append(" (avg ").append(format1(avgt)).append("°C)");
                }
            }
            if (maxwind != null) {
                sb.append("\nMax wind: ").append(format1(maxwind)).append(" kph");
            }
            if (totalprecip != null) {
                sb.append("\nTotal precip: ").append(format1(totalprecip)).append(" mm");
            }
            if (avghumidity != null) {
                sb.append("\nAvg humidity: ").append(avghumidity).append("%");
            }
        }
        return sb.toString();
    }

    private String normalizeQuery(String q) {
        String t = q == null ? "" : q.trim();
        if (t.length() == 0) {
            return t;
        }
        if (looksLikeLocationQuery(t)) {
            String[] ll = splitByComma(t);
            if (ll.length == 2) {
                return ll[0].trim() + "," + ll[1].trim();
            }
        }
        return t;
    }

    private String[] splitByWhitespace(String s) {
        if (s == null) {
            return new String[0];
        }
        s = s.trim();
        if (s.length() == 0) {
            return new String[0];
        }
        return s.split("\\s+");
    }

    private String[] splitByComma(String s) {
        if (s == null) {
            return new String[0];
        }
        int idx = s.indexOf(',');
        if (idx < 0) {
            return new String[] { s };
        }
        return new String[] { s.substring(0, idx), s.substring(idx + 1) };
    }

    private boolean isInteger(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i == 0 && (c == '-' || c == '+')) {
                continue;
            }
            if (c < '0' || c > '9') {
                return false;
            }
        }
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String toQueryString(Map params) throws Exception {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object kObj : params.keySet()) {
            String k = String.valueOf(kObj);
            String v = params.get(kObj) == null ? "" : String.valueOf(params.get(kObj));
            if (!first) {
                sb.append("&");
            }
            first = false;
            sb.append(URLEncoder.encode(k, "UTF-8"));
            sb.append("=");
            sb.append(URLEncoder.encode(v, "UTF-8"));
        }
        return sb.toString();
    }

    private JSONObject httpGetJson(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(HTTP_READ_TIMEOUT_MS);
            conn.setUseCaches(false);
            conn.setDoInput(true);

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
                String err = readAll(is);
                String msg = "HTTP " + code;
                try {
                    JSONObject eJson = parseJsonObject(err);
                    String eMsg = extractWeatherApiErrorMessage(eJson);
                    if (eMsg.length() > 0) {
                        msg = eMsg;
                    }
                } catch (Exception ignore) {
                }
                throw new Exception(msg);
            }
            String body = readAll(is);
            JSONObject obj = parseJsonObject(body);
            return obj;
        } finally {
            if (is != null) {
                try { is.close(); } catch (Exception ignore2) {}
            }
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignore3) {}
            }
        }
    }

    private String extractWeatherApiErrorMessage(JSONObject json) {
        if (json == null) {
            return "";
        }
        JSONObject err = asObject(json.get("error"));
        if (err == null) {
            return "";
        }
        String msg = stringOrEmpty(err.get("message"));
        return msg;
    }

    private JSONObject parseJsonObject(String body) throws Exception {
        if (body == null) {
            throw new Exception("Empty response");
        }
        JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
        Object parsed = parser.parse(body);
        if (parsed instanceof JSONObject) {
            return (JSONObject) parsed;
        }
        throw new Exception("Invalid JSON response");
    }

    private String readAll(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private JSONObject asObject(Object o) {
        if (o instanceof JSONObject) {
            return (JSONObject) o;
        }
        return null;
    }

    private JSONArray asArray(Object o) {
        if (o instanceof JSONArray) {
            return (JSONArray) o;
        }
        return null;
    }

    private String stringOrEmpty(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private Double asDouble(Object o) {
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

    private Integer asInt(Object o) {
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

    private String format1(Double d) {
        if (d == null) {
            return "";
        }
        double v = d.doubleValue();
        boolean neg = v < 0;
        if (neg) {
            v = -v;
        }
        long scaled = Math.round(v * 10.0);
        long whole = scaled / 10;
        long frac = scaled % 10;
        String s = String.valueOf(whole) + "." + String.valueOf(frac);
        return neg ? ("-" + s) : s;
    }

    private String joinNonEmpty(String[] parts, String sep) {
        if (parts == null || parts.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i] == null ? "" : parts[i].trim();
            if (p.length() == 0) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(p);
        }
        return sb.toString();
    }
}
