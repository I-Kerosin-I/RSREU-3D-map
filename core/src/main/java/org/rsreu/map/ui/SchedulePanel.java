package org.rsreu.map.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
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
 * The docked right panel (the inspector): the selected room's summary card and
 * its classroom schedule from rasp.rsreu.ru. Lessons are grouped by day, each
 * lesson is a compact card with the time and type on the first line, the
 * subject as the main text and the details (group, lecturer) as secondary
 * lines. Falls back to an error state and a browser link when the site is
 * unreachable or the room is not found.
 */
public final class SchedulePanel {

    private final UiTheme theme;
    private final RsreuScheduleService service;

    private final Table table;
    private final Table header;
    private final Label roomLabel;
    private final Table infoCard;
    private final Label infoDescription;
    private final Label infoMeta;
    private final Label statusLabel;
    private final Table lessons;
    private final ScrollPane lessonsScroll;
    private final TextButton browserButton;
    private Cell<Actor> collapseCell;

    /** The room whose schedule was requested; guards stale async results. */
    private Space requestedRoom;
    private String lastUrl;

    public SchedulePanel(UiTheme theme, RsreuScheduleService service) {
        this.theme = theme;
        this.service = service;

        table = new Table();
        table.setBackground(theme.rightPanelBackground());
        table.pad(UiTheme.SPACE_M, UiTheme.SPACE_S, UiTheme.SPACE_M, UiTheme.SPACE_S);

        TextButton refreshButton = new TextButton("Обновить", theme.skin(), "compact");
        refreshButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                refresh();
            }
        });

        header = new Table();
        header.add(new Label("Расписание", theme.skin(), "title")).left().growX();
        header.add(refreshButton).right();

        roomLabel = new Label("", theme.skin(), "hint");
        roomLabel.setEllipsis(true);

        infoDescription = new Label("", theme.skin(), "secondary");
        infoDescription.setWrap(true);
        infoMeta = new Label("", theme.skin(), "hint");
        infoMeta.setWrap(true);
        infoCard = new Table();
        infoCard.setBackground(theme.cardBackground());
        infoCard.pad(UiTheme.SPACE_S, UiTheme.SPACE_M, UiTheme.SPACE_S, UiTheme.SPACE_M);
        infoCard.add(infoDescription).left().growX();
        infoCard.row();
        infoCard.add(infoMeta).left().growX().padTop(UiTheme.SPACE_XS);
        infoCard.setVisible(false);

        statusLabel = new Label("", theme.skin(), "hint");
        statusLabel.setWrap(true);

        lessons = new Table().left().top();

        browserButton = new TextButton("В браузере", theme.skin(), "link");
        browserButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                String url = lastUrl != null ? lastUrl : service.browserUrl(RsreuScheduleService.MAIN_BUILDING);
                Gdx.app.getNet().openURI(url);
            }
        });

        lessonsScroll = new ContentScrollPane(lessons, theme.skin(), false);
        lessonsScroll.setFadeScrollBars(false);
        lessonsScroll.setScrollingDisabled(true, false);

        // The docked panel is opaque UI: presses on it must never drag the
        // camera behind it.
        table.addListener(new ClickListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                     float x, float y, int pointer, int button) {
                return true;
            }
        });

        table.add(header).growX();
        table.row();
        table.add(roomLabel).left().growX().padTop(2f);
        table.row();
        table.add(infoCard).left().growX().padTop(UiTheme.SPACE_S);
        table.row();
        table.add(statusLabel).left().growX().padTop(UiTheme.SPACE_S);
        table.row();
        table.add(lessonsScroll).grow().padTop(UiTheme.SPACE_S);
        table.row();
        table.add(browserButton).left().padTop(UiTheme.SPACE_XS);
    }

    public Actor getActor() {
        return table;
    }

    /** Places the panel collapse toggle into the header, right of the refresh action. */
    public void setCollapseButton(Actor button) {
        if (collapseCell == null) {
            collapseCell = header.add(button).size(28f, 36f).fill().padLeft(UiTheme.SPACE_S);
        } else {
            collapseCell.setActor(button);
        }
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
        roomLabel.setText("Выберите аудиторию, чтобы увидеть расписание");
        fillInfoCard(null);
        lessons.clearChildren();
        setStatus("", UiTheme.TEXT_SECONDARY);
        requestedRoom = space;
        lastUrl = null;

        if (space == null) {
            return;
        }
        roomLabel.setText(space.displayName());
        fillInfoCard(space);
        setStatus("Загрузка…", UiTheme.TEXT_SECONDARY);
        service.fetchAsync(RsreuScheduleService.MAIN_BUILDING, roomNumberOf(space), result -> {
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

    /** The room summary: description and the compact meta line. */
    private void fillInfoCard(Space space) {
        if (space == null) {
            infoCard.setVisible(false);
            return;
        }
        String description = space.description();
        boolean hasDescription = description != null && !description.isBlank();
        infoDescription.setText(hasDescription ? description : "");
        infoDescription.setVisible(hasDescription);

        StringBuilder meta = new StringBuilder("Этаж " + space.floorId().value());
        if (space.seats() != null) {
            meta.append(" · мест: ").append(space.seats());
        }
        if (space.equipment() != null && !space.equipment().isEmpty()) {
            meta.append(" · ").append(String.join(", ", space.equipment()));
        }
        infoMeta.setText(meta.toString());
        infoCard.setVisible(true);
    }

    private void setStatus(String text, com.badlogic.gdx.graphics.Color color) {
        statusLabel.setText(text);
        statusLabel.setColor(color);
        statusLabel.setVisible(!text.isEmpty());
    }

    private void render(Space space, ScheduleResult result) {
        lastUrl = result.url();

        lessons.clearChildren();
        if (!result.isSuccess()) {
            setStatus(result.error(), UiTheme.ERROR);
            return;
        }

        List<ScheduleLesson> lessonsList = result.lessons();
        if (lessonsList.isEmpty()) {
            setStatus("Занятия не найдены", UiTheme.TEXT_SECONDARY);
            return;
        }
        setStatus("", UiTheme.TEXT_SECONDARY);

        String lastDay = null;
        for (ScheduleLesson lesson : lessonsList) {
            String day = dayTitle(lesson);
            if (!day.equals(lastDay)) {
                Label dayLabel = new Label(day, theme.skin(), "hint");
                lessons.add(dayLabel).left().growX()
                    .padTop(lastDay == null ? 0 : UiTheme.SPACE_M)
                    .padBottom(UiTheme.SPACE_XS);
                lessons.row();
                lastDay = day;
            }
            lessons.add(lessonCard(lesson)).left().growX().padBottom(UiTheme.SPACE_XS);
            lessons.row();
        }
    }

    /** «Понедельник · 16.11» — the site provides the weekday and the date,
     *  sometimes both in one cell. */
    private static String dayTitle(ScheduleLesson lesson) {
        String weekday = lesson.weekday() == null ? "" : lesson.weekday().trim();
        String date = lesson.date() == null ? "" : lesson.date().trim();
        if (!date.isEmpty() && weekday.contains(date)) {
            // The weekday cell carries the date itself: split them apart and
            // rejoin in a consistent order.
            String day = weekday.replace(date, " ").trim();
            return day.isEmpty() ? date : day + " · " + date;
        }
        if (weekday.isEmpty()) {
            return date;
        }
        return date.isEmpty() ? weekday : weekday + " · " + date;
    }

    /**
     * One lesson card: time and the class type badge on the first line, the
     * subject as the main text, the details (groups, lecturer) as the
     * secondary line.
     */
    private Table lessonCard(ScheduleLesson lesson) {
        Table card = new Table();
        card.setBackground(theme.cardBackground());
        card.pad(UiTheme.SPACE_S, UiTheme.SPACE_M, UiTheme.SPACE_S, UiTheme.SPACE_M);
        card.left().top();

        card.add(new Label(lesson.time(), theme.skin(), "default")).left();
        if (!lesson.type().isEmpty()) {
            card.add(new Label(lesson.type(), theme.skin(), "caption")).right().growX();
        } else {
            card.add().growX();
        }
        card.row();

        Label subject = new Label(lesson.subject(), theme.skin(), "default");
        subject.setWrap(true);
        card.add(subject).left().growX().padTop(2f);
        card.row();

        if (!lesson.details().isEmpty()) {
            Label details = new Label(lesson.details(), theme.skin(), "secondary");
            details.setWrap(true);
            card.add(details).left().growX().padTop(2f);
            card.row();
        }
        return card;
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
