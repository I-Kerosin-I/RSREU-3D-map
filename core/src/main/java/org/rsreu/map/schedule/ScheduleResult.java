package org.rsreu.map.schedule;

import java.util.List;

/**
 * The outcome of a schedule fetch: the lessons of a classroom for the current
 * week, or an error description (network failure, room not found on the site)
 * together with the browser URL the user can open instead.
 */
public record ScheduleResult(
    List<ScheduleLesson> lessons,
    String error,
    String url
) {

    public static ScheduleResult ok(List<ScheduleLesson> lessons, String url) {
        return new ScheduleResult(List.copyOf(lessons), null, url);
    }

    public static ScheduleResult fail(String error, String url) {
        return new ScheduleResult(List.of(), error, url);
    }

    public boolean isSuccess() {
        return error == null;
    }
}
