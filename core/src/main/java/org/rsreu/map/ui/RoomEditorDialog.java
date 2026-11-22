package org.rsreu.map.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

import org.rsreu.map.domain.campus.CategoryDef;
import org.rsreu.map.domain.campus.CategoryTaxonomy;
import org.rsreu.map.domain.campus.Space;
import org.rsreu.map.domain.campus.SpaceType;
import org.rsreu.map.domain.model.FloorId;
import org.rsreu.map.domain.model.RoomId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The modal dialog of the room editor: edits the name, description, type,
 * category/subcategory, seat count, equipment (preset checkboxes + free text)
 * and arbitrary key/value attributes of a room, then hands the assembled
 * {@link Space} to the save handler. Also creates rooms that are not in the
 * manifest yet, prefilled from the hitbox node name.
 */
public final class RoomEditorDialog {

    /** Receives the assembled space together with its floor id. Throwing keeps
     *  the dialog open with the error shown instead of closing silently. */
    public interface SaveHandler {

        void onSave(Space space, String floorId, String newSubcategoryName) throws Exception;
    }

    private static final String UNSET_ITEM = "— не задана —";
    /** The last subcategory list item: switching to it reveals the free-text
     *  field for a custom department (see {@link #newSubcategoryField}). */
    private static final String ADD_NEW_ITEM = "Добавить кафедру…";

    private static final float FIELD_WIDTH = 300f;

    private final UiTheme theme;
    private final SaveHandler saveHandler;

    private final Window window;
    private final Label errorLabel;
    private final TextField idField;
    private final TextField nameField;
    private final TextArea descriptionArea;
    private final SelectBox<String> typeSelect;
    private final SelectBox<String> categorySelect;
    private final SelectBox<String> subcategorySelect;
    /** Shown while the "add a custom department" list item is selected. */
    private final TextField newSubcategoryField;
    private final TextField seatsField;
    private final Map<String, CheckBox> equipmentChecks = new LinkedHashMap<>();
    private final TextField otherEquipmentField;
    private final Table attributeRows;
    private final List<TextField[]> attributeInputs = new ArrayList<>();

    private CategoryTaxonomy taxonomy;
    private Space original;
    private String hitboxNodeName;
    private String floorId;
    private Actor openedOn;

    /** The space types offered by the type selector, in display order. */
    private static final SpaceType[] TYPES = SpaceType.values();

    public RoomEditorDialog(UiTheme theme, CategoryTaxonomy taxonomy, SaveHandler saveHandler) {
        this.theme = theme;
        this.taxonomy = taxonomy;
        this.saveHandler = saveHandler;

        window = new Window("Редактор аудитории", theme.skin());
        window.setModal(true);
        window.setMovable(true);
        window.setResizable(false);

        errorLabel = new Label("", theme.skin(), "error");
        errorLabel.setWrap(true);

        idField = new TextField("", theme.skin());
        nameField = new TextField("", theme.skin());
        descriptionArea = new TextArea("", theme.skin());
        descriptionArea.setPrefRows(4);

        typeSelect = new SelectBox<>(theme.skin());
        String[] typeNames = new String[TYPES.length];
        for (int i = 0; i < TYPES.length; i++) {
            typeNames[i] = TYPES[i].name();
        }
        typeSelect.setItems(typeNames);

        categorySelect = new SelectBox<>(theme.skin());
        categorySelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                repopulateSubcategories();
            }
        });
        rebuildCategoryItems(null);

        subcategorySelect = new SelectBox<>(theme.skin());
        newSubcategoryField = new TextField("", theme.skin());
        newSubcategoryField.setMessageText("Название кафедры");
        newSubcategoryField.setVisible(false);
        subcategorySelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateNewSubcategoryVisibility();
            }
        });

        seatsField = new TextField("", theme.skin());
        seatsField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());

        otherEquipmentField = new TextField("", theme.skin());
        otherEquipmentField.setMessageText("другое — через запятую");

        Table fields = new Table();
        addRow(fields, "ID", idField);
        addRow(fields, "Название", nameField);
        addRow(fields, "Описание", descriptionArea);
        addRow(fields, "Тип", typeSelect);
        addRow(fields, "Категория", categorySelect);
        addRow(fields, "Подкатегория", subcategorySelect);
        addRow(fields, "", newSubcategoryField);
        addRow(fields, "Число мест", seatsField);

        Table equipment = new Table();
        Label equipmentTitle = new Label("Оборудование", theme.skin(), "hint");
        equipment.add(equipmentTitle).left().colspan(2);
        equipment.row();
        for (String preset : UiTheme.EQUIPMENT_PRESETS) {
            CheckBox check = new CheckBox(preset, theme.skin());
            equipmentChecks.put(preset, check);
            equipment.add(check).left().colspan(2);
            equipment.row();
        }
        Label otherEquipmentTitle = new Label("Другое", theme.skin(), "hint");
        equipment.add(otherEquipmentTitle).left().padRight(8f);
        equipment.add(otherEquipmentField).growX();
        equipment.row();
        fields.add(sectionTitle("Оборудование")).left().colspan(2);
        fields.row();
        fields.add(equipment).left().growX().colspan(2);
        fields.row();

        attributeRows = new Table();
        Label attributesTitle = sectionTitle("Произвольные атрибуты");
        TextButton addAttribute = new TextButton("+ атрибут", theme.skin());
        addAttribute.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                addAttributeRow("", "");
                window.pack();
            }
        });
        fields.add(attributesTitle).left();
        fields.add(addAttribute).right();
        fields.row();
        fields.add(attributeRows).growX().colspan(2);
        fields.row();

        Table buttons = new Table();
        TextButton save = new TextButton("Сохранить", theme.skin());
        save.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                save();
            }
        });
        TextButton cancel = new TextButton("Отмена", theme.skin());
        cancel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
            }
        });
        buttons.add(save).padRight(8f);
        buttons.add(cancel);

        window.add(fields).growX().pad(8, 8, 8, 8);
        window.row();
        window.add(errorLabel).left().pad(0, 8, 4, 8).growX();
        window.row();
        window.add(buttons).right().pad(4, 8, 8, 8);
        window.row();
        window.setVisible(false);
    }

    private Label sectionTitle(String text) {
        return new Label(text, theme.skin(), "accent");
    }

    private void addRow(Table fields, String caption, Actor field) {
        fields.add(new Label(caption, theme.skin(), "hint")).left().padRight(8f).padTop(4f);
        fields.add(field).growX().padTop(4f);
        fields.row();
    }

    private Label label(String text) {
        return new Label(text, theme.skin(), "hint");
    }

    private Label caption(String text) {
        return new Label(text, theme.skin(), "hint");
    }

    private void rebuildCategoryItems(String selectedCategoryName) {
        List<String> items = new ArrayList<>();
        items.add(UNSET_ITEM);
        for (var category : taxonomy.categories()) {
            items.add(category.name());
        }
        categorySelect.setItems(items.toArray(new String[0]));
        if (selectedCategoryName != null) {
            categorySelect.setSelected(selectedCategoryName);
        }
        repopulateSubcategories();
    }

    /** Fills the subcategory selector from the chosen category, ending with the
     *  "add a custom one" item that reveals the free-text department field. */
    private void repopulateSubcategories() {
        if (subcategorySelect == null || newSubcategoryField == null) {
            return; // called from the constructor before the fields are created
        }
        String categoryName = categorySelect.getSelected();
        List<String> items = new ArrayList<>();
        items.add(UNSET_ITEM);
        taxonomy.categories().stream()
            .filter(category -> category.name().equals(categoryName))
            .findFirst()
            .ifPresent(category -> {
                for (var subcategory : category.subcategories()) {
                    items.add(subcategory.name());
                }
            });
        items.add(ADD_NEW_ITEM);
        subcategorySelect.setItems(items.toArray(new String[0]));
        subcategorySelect.setSelectedIndex(0);
        updateNewSubcategoryVisibility();
    }

    private void updateNewSubcategoryVisibility() {
        boolean adding = ADD_NEW_ITEM.equals(subcategorySelect.getSelected());
        newSubcategoryField.setVisible(adding);
        if (!adding) {
            newSubcategoryField.setText("");
        }
    }

    /**
     * Opens the dialog: with an existing space to edit, or with {@code null}
     * to create a room prefilled from the hitbox node name.
     */
    public void show(Actor openedOn, Space space, CategoryTaxonomy taxonomyState,
                     String hitboxNodeName, String floorId) {
        this.taxonomy = taxonomyState;
        this.original = space;
        this.hitboxNodeName = hitboxNodeName;
        this.floorId = floorId;
        this.openedOn = openedOn;

        errorLabel.setText("");

        if (space != null) {
            window.getTitleLabel().setText("Редактор аудитории — " + space.displayName());
            idField.setText(space.id().value());
            idField.setTouchable(Touchable.disabled);
            nameField.setText(space.displayName());
            descriptionArea.setText(space.description() != null ? space.description() : "");
            typeSelect.setSelected(space.type().name());
            categorySelect.setSelected(categoryNameOf(space.category()));
            repopulateSubcategories();
            subcategorySelect.setSelected(subcategoryNameOf(space.subcategory()));
            seatsField.setText(space.seats() != null ? String.valueOf(space.seats()) : "");
            applyEquipment(space.equipment());
            rebuildAttributeRows(space.attributes());
        } else {
            window.getTitleLabel().setText("Новая аудитория");
            String derivedId = hitboxNodeName != null && hitboxNodeName.endsWith("_hitbox")
                ? hitboxNodeName.substring(0, hitboxNodeName.length() - "_hitbox".length())
                : "";
            idField.setText(derivedId);
            idField.setTouchable(Touchable.enabled);
            nameField.setText("а." + derivedId.replaceFirst("^main_room_", ""));
            descriptionArea.setText("");
            typeSelect.setSelected(SpaceType.ROOM.name());
            categorySelect.setSelectedIndex(0);
            repopulateSubcategories();
            subcategorySelect.setSelectedIndex(0);
            seatsField.setText("");
            applyEquipment(List.of());
            rebuildAttributeRows(Map.of());
        }

        window.pack();
        centerOn(openedOn);
        if (window.getStage() == null && openedOn != null && openedOn.getStage() != null) {
            openedOn.getStage().addActor(window);
        }
        window.toFront();
        window.setVisible(true);
    }

    private void applyEquipment(List<String> equipment) {
        List<String> present = equipment != null ? equipment : List.of();
        for (Map.Entry<String, CheckBox> entry : equipmentChecks.entrySet()) {
            entry.getValue().setChecked(present.contains(entry.getKey()));
        }
        List<String> other = new ArrayList<>(present);
        other.removeAll(UiTheme.EQUIPMENT_PRESETS);
        otherEquipmentField.setText(String.join(", ", other));
    }

    private void rebuildAttributeRows(Map<String, String> attributes) {
        attributeRows.clearChildren();
        attributeInputs.clear();
        if (attributes.isEmpty()) {
            addAttributeRow("", "");
        } else {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                addAttributeRow(entry.getKey(), entry.getValue());
            }
        }
        attributeRows.pack();
    }

    private void addAttributeRow(String key, String value) {
        TextField keyField = new TextField(key, theme.skin());
        keyField.setMessageText("ключ");
        TextField valueField = new TextField(value, theme.skin());
        valueField.setMessageText("значение");
        TextButton remove = new TextButton("×", theme.skin());
        remove.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                attributeInputs.removeIf(pair -> pair[0] == keyField && pair[1] == valueField);
                rebuildAttributeRowsFromInputs();
                window.pack();
            }
        });
        attributeRows.add(keyField).width(110f);
        attributeRows.add(valueField).width(150f);
        attributeRows.add(remove).width(28f);
        attributeRows.row();
        attributeInputs.add(new TextField[]{keyField, valueField});
    }

    private void rebuildAttributeRowsFromInputs() {
        List<String[]> rows = new ArrayList<>();
        for (TextField[] pair : attributeInputs) {
            rows.add(new String[]{pair[0].getText(), pair[1].getText()});
        }
        attributeRows.clearChildren();
        attributeInputs.clear();
        for (String[] row : rows) {
            TextField keyField = new TextField(row[0], theme.skin());
            keyField.setMessageText("ключ");
            TextField valueField = new TextField(row[1], theme.skin());
            valueField.setMessageText("значение");
            TextButton remove = new TextButton("×", theme.skin());
            remove.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    attributeInputs.removeIf(pair -> pair[0] == keyField && pair[1] == valueField);
                    rebuildAttributeRowsFromInputs();
                    window.pack();
                }
            });
            attributeRows.add(keyField).width(110f);
            attributeRows.add(valueField).width(150f);
            attributeRows.add(remove).width(28f);
            attributeRows.row();
            attributeInputs.add(new TextField[]{keyField, valueField});
        }
    }

    private void save() {
        try {
            String newSubcategoryName = null;
            if (ADD_NEW_ITEM.equals(subcategorySelect.getSelected())) {
                newSubcategoryName = newSubcategoryField.getText().trim();
                if (newSubcategoryName.isEmpty()) {
                    errorLabel.setText("Введите название новой кафедры");
                    return;
                }
            }
            saveHandler.onSave(buildSpace(newSubcategoryName), floorId, newSubcategoryName);
            hide();
        } catch (Exception e) {
            // Persistence problems must not close the dialog silently: the
            // error stays visible next to the fields.
            errorLabel.setText(e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private Space buildSpace(String newSubcategoryName) {
        String id = idField.getText().trim().replace(' ', '_');
        if (id.isEmpty()) {
            throw new IllegalArgumentException("ID не может быть пустым");
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Название не может быть пустым");
        }
        String modelNode = hitboxNodeName != null ? hitboxNodeName
            : (original != null ? original.modelNodeName() : id + "_hitbox");

        SpaceType type = SpaceType.OTHER;
        for (SpaceType candidate : TYPES) {
            if (candidate.name().equals(typeSelect.getSelected())) {
                type = candidate;
                break;
            }
        }

        String categoryName = categorySelect.getSelected();
        String category = null;
        String subcategory = null;
        if (categoryName != null && !categoryName.startsWith("—")) {
            for (var def : taxonomy.categories()) {
                if (def.name().equals(categoryName)) {
                    category = def.id();
                    String subcategoryName = subcategorySelect.getSelected();
                    if (subcategoryName != null && !subcategoryName.startsWith("—")) {
                        if (ADD_NEW_ITEM.equals(subcategoryName)) {
                            // A custom department typed by hand: its id equals
                            // its name, which keeps the lookup simple.
                            subcategory = newSubcategoryName;
                        } else {
                            for (var sub : def.subcategories()) {
                                if (sub.name().equals(subcategoryName)) {
                                    subcategory = sub.id();
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }

        Integer seats = null;
        String seatsText = seatsField.getText().trim();
        if (!seatsText.isEmpty()) {
            seats = Integer.parseInt(seatsText);
            if (seats < 0) {
                throw new IllegalArgumentException("Число мест не может быть отрицательным");
            }
        }

        List<String> equipment = new ArrayList<>();
        for (Map.Entry<String, CheckBox> entry : equipmentChecks.entrySet()) {
            if (entry.getValue().isChecked()) {
                equipment.add(entry.getKey());
            }
        }
        for (String extra : otherEquipmentField.getText().split(",")) {
            String trimmed = extra.trim();
            if (!trimmed.isEmpty()) {
                equipment.add(trimmed);
            }
        }

        Map<String, String> attributes = new LinkedHashMap<>();
        for (TextField[] pair : attributeInputs) {
            String key = pair[0].getText().trim();
            String value = pair[1].getText().trim();
            if (!key.isEmpty()) {
                attributes.put(key, value);
            }
        }

        String description = descriptionArea.getText().trim();
        // The edited flag is stamped by the manifest store on every save; the
        // dialog always builds a pre-save copy of the room.
        return new Space(new RoomId(id), name, new FloorId(floorId), type, modelNode,
            description.isEmpty() ? null : description,
            category, subcategory, seats,
            equipment.isEmpty() ? null : equipment,
            attributes.isEmpty() ? null : attributes,
            false);
    }

    private String categoryNameOf(String categoryId) {
        if (categoryId == null) {
            return "— не задана —";
        }
        return taxonomy.find(categoryId)
            .map(CategoryDef::name)
            .orElse("— не задана —");
    }

    private String subcategoryNameOf(String subcategoryId) {
        if (subcategoryId == null) {
            return "— не задана —";
        }
        for (var category : taxonomy.categories()) {
            for (var subcategory : category.subcategories()) {
                if (subcategory.id().equals(subcategoryId)) {
                    return subcategory.name();
                }
            }
        }
        return "— не задана —";
    }

    private void centerOn(Actor reference) {
        window.pack();
        if (reference != null && reference.getStage() != null) {
            var stage = reference.getStage();
            window.setPosition((int) ((stage.getWidth() - window.getWidth()) / 2),
                (int) ((stage.getHeight() - window.getHeight()) / 2));
        } else {
            window.setPosition(200, 150);
        }
    }

    public void hide() {
        window.remove();
        openedOn = null;
    }
}
