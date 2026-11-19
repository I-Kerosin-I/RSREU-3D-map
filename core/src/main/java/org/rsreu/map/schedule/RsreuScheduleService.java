package org.rsreu.map.schedule;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fetches classroom schedules from rasp.rsreu.ru.
 * <p>
 * The site renders a week grid server-side for
 * {@code /schedule-frame/classroom?building=X&classroom=<label>}, where the
 * classroom is passed by its display label ("210 C") — the internal option ids
 * do not work as URL parameters. The label is resolved by matching the room
 * number against the classroom list embedded in the building page.
 * <p>
 * Fetches run on a daemon thread; results are delivered on the libGDX render
 * thread through {@link Gdx#app postRunnable}.
 */
public final class RsreuScheduleService {

    private static final String BASE_URL = "https://rasp.rsreu.ru/schedule-frame/classroom";
    private static final int TIMEOUT_MS = 10000;
    /** Building letter of the main campus on rasp.rsreu.ru. */
    public static final String MAIN_BUILDING = "C";

    /** One parsed entry of the classroom autocomplete list. */
    private record ClassroomOption(String label) {
    }

    /**
     * Starts an asynchronous fetch of the classroom schedule and delivers the
     * result on the render thread.
     *
     * @param building   the site building letter ("C" for the main campus)
     * @param roomNumber the room number as it appears on the site ("210", "304a")
     * @param callback   the consumer of the result, invoked on the render thread
     */
    public void fetchAsync(String building, String roomNumber, Consumer<ScheduleResult> callback) {
        Thread thread = new Thread(() -> {
            ScheduleResult result;
            try {
                result = fetch(building, roomNumber);
            } catch (Exception e) {
                result = ScheduleResult.fail("Ошибка сети: " + e.getMessage(), browserUrl(building, roomNumber));
            }
            ScheduleResult delivered = result;
            Gdx.app.postRunnable(() -> callback.accept(delivered));
        }, "rsreu-schedule-fetch");
        thread.setDaemon(true);
        thread.start();
    }

    /** URL the user can open in a system browser for the same classroom. */
    public String browserUrl(String building, String roomNumber) {
        return BASE_URL + "?building=" + building + "&classroom="
            + urlEncode(roomNumber + " " + building);
    }

    private ScheduleResult fetch(String building, String roomNumber) throws IOException {
        List<ClassroomOption> options = fetchClassroomOptions(building);
        String label = resolveLabel(options, roomNumber);
        if (label == null) {
            return ScheduleResult.fail("Аудитория «" + roomNumber + "» не найдена на rasp.rsreu.ru",
                browserUrl(building, roomNumber));
        }

        String html = get(BASE_URL + "?building=" + building + "&classroom=" + urlEncode(label));
        List<ScheduleLesson> lessons = parseWeekTable(html);
        if (lessons.isEmpty()) {
            return ScheduleResult.fail("Занятия в расписании не найдены", browserUrl(building, roomNumber));
        }
        return ScheduleResult.ok(lessons, browserUrl(building, roomNumber));
    }

    private List<ClassroomOption> fetchClassroomOptions(String building) throws IOException {
        String html = get(BASE_URL + "?building=" + building);
        JsonValue options = extractOptionsJson(html);
        if (options == null) {
            throw new IOException("Структура страницы расписания изменилась");
        }
        List<ClassroomOption> result = new ArrayList<>();
        for (JsonValue option = options.child(); option != null; option = option.next()) {
            result.add(new ClassroomOption(option.getString("label")));
        }
        return result;
    }

    /** Extracts the HTML-escaped options JSON of the classroom autocomplete. */
    private JsonValue extractOptionsJson(String html) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile(":options=\"([^\"]*)\"").matcher(html);
        if (!matcher.find()) {
            return null;
        }
        String json = matcher.group(1)
            .replace("&quot;", "\"")
            .replace("&amp;", "&");
        try {
            return new JsonReader().parse(json);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Matches the room number against the site labels: both sides are lowered,
     * stripped of spaces and transliterated from Cyrillic lookalikes.
     */
    private String resolveLabel(List<ClassroomOption> options, String roomNumber) {
        String target = normalize(roomNumber);
        if (target.isEmpty()) {
            return null;
        }
        for (ClassroomOption option : options) {
            String label = option.label();
            String stripped = label;
            // Labels end with the building letter ("210 C").
            int spaceIndex = stripped.lastIndexOf(' ');
            if (spaceIndex > 0) {
                stripped = stripped.substring(0, spaceIndex);
            }
            if (normalize(stripped).equals(target)) {
                return label;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        StringBuilder result = new StringBuilder();
        for (char c : value.toLowerCase().toCharArray()) {
            if (Character.isWhitespace(c)) {
                continue;
            }
            result.append(transliterate(c));
        }
        return result.toString();
    }

    private static char transliterate(char c) {
        return switch (c) {
            case 'а' -> 'a';
            case 'б' -> 'b';
            case 'в' -> 'v';
            case 'г' -> 'g';
            case 'д' -> 'd';
            case 'е', 'ё' -> 'e';
            case 'ж' -> 'z';
            case 'з' -> 'z';
            case 'и' -> 'i';
            case 'й' -> 'y';
            case 'к' -> 'k';
            case 'л' -> 'l';
            case 'м' -> 'm';
            case 'н' -> 'n';
            case 'о' -> 'o';
            case 'п' -> 'p';
            case 'р' -> 'p';
            case 'с' -> 'c';
            case 'т' -> 't';
            case 'у' -> 'y';
            case 'ф' -> 'f';
            case 'х' -> 'h';
            case 'ц' -> 'c';
            case 'ч' -> 'c';
            case 'ш' -> 's';
            case 'щ' -> 's';
            case 'ы' -> 'i';
            case 'ь' -> 'i';
            case 'э' -> 'e';
            case 'ю' -> 'u';
            case 'я' -> 'a';
            default -> c;
        };
    }

    /**
     * Parses the week grid: the first table row holds the days, the following
     * rows hold time slots whose cells carry one or more lesson divs.
     */
    private List<ScheduleLesson> parseWeekTable(String html) throws IOException {
        String table = findLessonTable(html);
        if (table == null) {
            throw new IOException("Структура страницы расписания изменилась");
        }

        java.util.regex.Matcher rowMatcher = java.util.regex.Pattern
            .compile("<tr[^>]*>(.*?)</tr>", java.util.regex.Pattern.DOTALL).matcher(table);
        List<String> rows = new ArrayList<>();
        while (rowMatcher.find()) {
            rows.add(rowMatcher.group(1));
        }
        if (rows.isEmpty()) {
            throw new IOException("Не удалось прочитать сетку расписания");
        }

        List<String> dates = new ArrayList<>();
        List<String> weekdays = new ArrayList<>();
        java.util.regex.Matcher headerCells = java.util.regex.Pattern
            .compile("<t[dh][^>]*>(.*?)</t[dh]>", java.util.regex.Pattern.DOTALL).matcher(rows.get(0));
        while (headerCells.find()) {
            String cell = headerCells.group(1);
            java.util.regex.Matcher dateMatch = java.util.regex.Pattern
                .compile("<span[^>]*>(.*?)</span>").matcher(cell);
            dates.add(dateMatch.find() ? cleanText(dateMatch.group(1)) : "");
            weekdays.add(cleanText(stripSpan(cell)));
        }

        List<ScheduleLesson> lessons = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            java.util.regex.Matcher cells = java.util.regex.Pattern
                .compile("<td[^>]*>(.*?)</td>", java.util.regex.Pattern.DOTALL).matcher(rows.get(rowIndex));
            List<String> rowCells = new ArrayList<>();
            while (cells.find()) {
                rowCells.add(cells.group(1));
            }
            if (rowCells.isEmpty()) {
                continue;
            }

            String time = extractTime(rowCells.get(0));
            for (int dayIndex = 1; dayIndex < rowCells.size() && dayIndex - 1 < weekdays.size(); dayIndex++) {
                List<String[]> cellLessons = parseLessonDivs(rowCells.get(dayIndex));
                String date = dayIndex - 1 < dates.size() ? dates.get(dayIndex - 1) : "";
                String weekday = dayIndex - 1 < weekdays.size() ? weekdays.get(dayIndex - 1) : "";
                for (String[] lesson : cellLessons) {
                    lessons.add(new ScheduleLesson(date, weekday, time, lesson[0], lesson[1], lesson[2]));
                }
            }
        }

        lessons.sort(Comparator.comparing(ScheduleLesson::weekday).thenComparing(ScheduleLesson::time));
        return lessons;
    }

    /** Finds the schedule table: the first one that carries lesson badges. */
    private String findLessonTable(String html) {
        java.util.regex.Matcher tables = java.util.regex.Pattern
            .compile("<table[^>]*>.*?</table>", java.util.regex.Pattern.DOTALL).matcher(html);
        while (tables.find()) {
            String candidate = tables.group();
            if (candidate.contains("schedule-lesson-type-badge")) {
                return candidate;
            }
        }
        if (html.contains("Выберите аудиторию")) {
            return null; // the site state: no classroom selected
        }
        tables.reset();
        return tables.find() ? tables.group() : null;
    }

    /**
     * Splits a table cell into lessons: each lesson div yields
     * {@code {type, subject, details}}.
     */
    private List<String[]> parseLessonDivs(String cellHtml) {
        List<String[]> lessons = new ArrayList<>();
        java.util.regex.Matcher divs = java.util.regex.Pattern
            .compile("<div[^>]*>(.*?)</div>", java.util.regex.Pattern.DOTALL).matcher(cellHtml);
        while (divs.find()) {
            String div = divs.group(1);
            java.util.regex.Matcher badge = java.util.regex.Pattern
                .compile("<span class=\"schedule-lesson-type-badge\">(.*?)</span>", java.util.regex.Pattern.DOTALL)
                .matcher(div);
            String type = badge.find() ? cleanText(stripSpan(badge.group(1))) : "";
            String rest = badge.find() ? div.substring(badge.end()) : div;
            String text = cleanText(rest);
            if (text.isEmpty()) {
                continue;
            }
            int separator = text.indexOf(", гр.");
            String subject = separator >= 0 ? text.substring(0, separator) : text;
            String details = separator >= 0 ? text.substring(separator + 2) : "";
            lessons.add(new String[]{type, subject, details});
        }
        return lessons;
    }

    private String extractTime(String timeCell) {
        java.util.regex.Matcher times = java.util.regex.Pattern
            .compile("<div[^>]*>(.*?)</div>", java.util.regex.Pattern.DOTALL).matcher(timeCell);
        List<String> parts = new ArrayList<>();
        while (times.find()) {
            parts.add(cleanText(times.group(1)));
        }
        if (parts.size() >= 2) {
            return parts.get(0) + "–" + parts.get(1);
        }
        return parts.isEmpty() ? "" : parts.get(0);
    }

    private String stripSpan(String html) {
        return html.replaceAll("<span[^>]*>", "").replaceAll("</span>", "");
    }

    private static String cleanText(String html) {
        return html
            .replaceAll("<br\\s*/?>", " ")
            .replaceAll("<[^>]+>", "")
            .replace("&amp;", "&")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private static String get(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)
            new java.net.URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (RSREU 3D Map)");
        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            throw new IOException("HTTP " + status);
        }
        try (InputStream stream = connection.getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }
    }
}
