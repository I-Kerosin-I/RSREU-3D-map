package org.rsreu.map.schedule;

/**
 * One lesson of a classroom schedule: a time slot on a specific day with the
 * class type (Упр./Лек./Лаб.), the subject and the remaining details
 * (groups, lecturer).
 */
public record ScheduleLesson(
    String date,
    String weekday,
    String time,
    String type,
    String subject,
    String details
) {
}
