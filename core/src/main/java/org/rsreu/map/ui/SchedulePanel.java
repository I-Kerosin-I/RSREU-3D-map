package org.rsreu.map.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import org.rsreu.map.domain.campus.Space;
import org.rsreu.map.schedule.RsreuScheduleService;
import org.rsreu.map.schedule.ScheduleLesson;
import org.rsreu.map.schedule.ScheduleResult;

import java.util.List;

/**
 * The collapsible right-side panel with the classroom schedule from
 * rasp.rsreu.ru. Shows the week of the currently selected room as a flat
 * lesson list grouped by day; falls back to an error state and a browser link
 * when the site is unreachable or the room is not found.
 */
public final class SchedulePanel {

    private final UiTheme theme;
    private final RsreuScheduleService service;

    private final Table table;
    private final Label roomLabel;
    private final Label statusLabel;
    private final Table lessons;
    private final ScrollPane lessonsScroll;
    private final TextButton browserButton;

    /** The room whose schedule was requested; guards stale async results. */
    private Space requestedRoom;
    private String lastUrl;

    public SchedulePanel(UiTheme theme, RsreuScheduleService service) {
        this.theme = theme;
        this.service = service;

        table = new Table();
        table.setBackground(theme.panelBackground());
        table.pad(8f);

        roomLabel = new Label("Расписание", theme.skin(), "title");
        statusLabel = new Label("", theme.skin(), "hint");
        statusLabel.setWrap(true);
        lessons = new Table().left().top();

        TextButton refreshButton = new TextButton("Обновить", theme.skin());
        refreshButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                refresh();
            }
        });

        browserButton = new TextButton("В браузере", theme.skin());
        browserButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                String url = lastUrl != null ? lastUrl : service.browserUrl(RsreuScheduleService.MAIN_BUILDING);
                Gdx.app.getNet().openURI(url);
            }
        });

        lessonsScroll = new ScrollPane(lessons, theme.skin());
        lessonsScroll.setFadeScrollBars(false);
        lessonsScroll.setScrollingDisabled(true, false);

        Table header = new Table();
        header.add(roomLabel).left().growX();
        header.add(refreshButton).right();
        header.row();
        header.add(statusLabel).left().growX().colspan(2);

        table.add(header).growX().padBottom(6f);
        table.row();
        table.add(lessonsScroll).grow();
        table.row();
        table.add(browserButton).left().padTop(6f);
    }

    public Actor getActor() {
        return table;
    }

    public boolean isVisible() {
        return table.isVisible();
    }

    public void setVisible(boolean visible) {
        table.setVisible(visible);
        table.setTouchable(visible
            ? Touchable.enabled
            : Touchable.disabled);
    }

    /** Shows the schedule of the given room; a null room clears the panel. */
    public void showForRoom(Space space) {
        roomLabel.setText("Расписание");
        lessons.clearChildren();
        statusLabel.setText("");
        statusLabel.setColor(UiTheme.TEXT_DIM);
        requestedRoom = space;
        lastUrl = null;

        if (space == null) {
            statusLabel.setText("Выберите аудиторию, чтобы увидеть расписание");
            return;
        }
        String roomNumber = roomNumberOf(space);
        roomLabel.setText("Расписание — " + space.displayName());
        statusLabel.setText("Загрузка…");
        service.fetchAsync(RsreuScheduleService.MAIN_BUILDING, roomNumber, result -> {
            // Drop stale results: the user may have selected another room meanwhile.
            if (requestedRoom != space) {
                return;
            }
            render(space, result);
        });
    }

    /** Re-requests the schedule of the room that is currently displayed. */
    public void refresh() {
        showForRoom(requestedRoom);
    }

    private void render(Space space, ScheduleResult result) {
        lastUrl = result.url();
        roomLabel.setText("Расписание — " + space.displayName());

        lessons.clearChildren();
        if (!result.isSuccess()) {
            statusLabel.setText(result.error());
            statusLabel.setColor(Color.SCARLET);
            return;
        }

        statusLabel.setText("");
        statusLabel.setColor(UiTheme.TEXT_DIM);
        List<ScheduleLesson> lessonsList = result.lessons();
        if (lessonsList.isEmpty()) {
            statusLabel.setText("Занятия не найдены");
            return;
        }

        String lastDay = null;
        for (ScheduleLesson lesson : lessonsList) {
            String day = lesson.weekday() + (lesson.date().isEmpty() ? "" : ", " + lesson.date());
            if (!day.equals(lastDay)) {
                Label dayLabel = new Label(day, theme.skin(), "accent");
                lessons.add(dayLabel).left().growX().padTop(6f);
                lessons.row();
                lastDay = day;
            }
            Label timeLabel = new Label(lesson.time() + (lesson.type().isEmpty() ? "" : "  " + lesson.type()),
                theme.skin(), "hint");
            Label subjectLabel = new Label(lesson.subject(), theme.skin(), "default");
            subjectLabel.setWrap(true);
            lessons.add(timeLabel).left().growX();
            lessons.row();
            lessons.add(subjectLabel).left().growX().pad(0, 12, 0, 0);
            lessons.row();
            if (!lesson.details().isEmpty()) {
                Label detailsLabel = new Label(lesson.details(), theme.skin(), "hint");
                detailsLabel.setWrap(true);
                lessons.add(detailsLabel).left().growX().pad(0, 12, 0, 0);
                lessons.row();
            }
        }
    }

    /** The room number as rasp.rsreu.ru knows it, e.g. "210" from "а.210". */
    private static String roomNumberOf(Space space) {
        String name = space.displayName().trim();
        if (name.startsWith("а.")) {
            return name.substring(2).trim();
        }
        return name;
    }
}
