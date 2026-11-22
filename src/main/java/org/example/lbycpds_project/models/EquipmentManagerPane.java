package org.example.lbycpds_project.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.lbycpds_project.utils.Data_Store;

import java.util.*;

/**
 * Reusable Inventory Management panel (Add, Edit, Delete, Search).
 * Styled to match BorrowerPortal and connected directly to Data_Store.
 * Can be embedded inside AdminPortal, StaffPortal, or a standalone Scene.
 */
public class EquipmentManagerPane extends VBox {
    // ---- theme (matches BorrowerPortal) ----
    private static final String GREEN = "#006738", BG = "#F8F9FC";

    private final Map<String, Equipment> inventory;
    private final List<BorrowRequest> allRequests;

    // ---- Search controls ----
    private final ComboBox<String> searchBy = new ComboBox<>(
            FXCollections.observableArrayList("Name", "ID", "Category", "Brand", "Location"));
    private final TextField keywordField = new TextField();
    private final CheckBox onlyAvailCheck = new CheckBox("Available only");
    private final TableView<List<String>> table = buildTable(
            "ID", "Name", "Category", "Brand", "Location", "Available", "Total", "Condition", "Status");

    // ---- Form controls (Add / Edit) ----
    private final TextField idField = new TextField();
    private final TextField nameField = new TextField();
    private final ComboBox<String> categoryBox = new ComboBox<>(
            FXCollections.observableArrayList("Consumable", "Instrument"));
    private final TextField brandField = new TextField();
    private final Spinner<Integer> totalQtySpinner = new Spinner<>(1, 100000, 1);
    private final TextField locationField = new TextField();
    private final ComboBox<String> conditionBox = new ComboBox<>(
            FXCollections.observableArrayList("New", "Good", "Fair", "Needs Repair"));
    private final ComboBox<String> statusBox = new ComboBox<>(
            FXCollections.observableArrayList("Available", "Under Maintenance", "Damaged", "Missing"));

    private boolean editMode = false;
    private final Button saveBtn = primaryButton("Add Item");
    private final Label formHeader = new Label("Add New Equipment");

    public EquipmentManagerPane() {
        this(Data_Store.getInstance().getInventory(), Data_Store.getInstance().getAllRequests());
    }

    public EquipmentManagerPane(Map<String, Equipment> inventory, List<BorrowRequest> allRequests) {
        this.inventory = inventory;
        this.allRequests = allRequests;

        setSpacing(12);
        setPadding(new Insets(16));
        setStyle("-fx-background-color:" + BG + ";");

        getChildren().addAll(buildSearchCard(), buildTableCard(), buildFormCard());
        refreshTable();
    }

    // =====================================================================
    //  Search Section
    // =====================================================================
    private Node buildSearchCard() {
        searchBy.setValue("Name");
        keywordField.setPromptText("Enter keyword or ID...");
        keywordField.setPrefWidth(220);

        Button searchBtn = primaryButton("Search");
        Button resetBtn = new Button("Reset");

        searchBtn.setOnAction(e -> refreshTable());
        keywordField.setOnAction(e -> refreshTable());
        onlyAvailCheck.setOnAction(e -> refreshTable());
        resetBtn.setOnAction(e -> {
            searchBy.setValue("Name");
            keywordField.clear();
            onlyAvailCheck.setSelected(false);
            refreshTable();
        });

        HBox row = new HBox(10, new Label("Search by:"), searchBy,
                new Label("Keyword:"), keywordField, onlyAvailCheck, searchBtn, resetBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return card("Search Equipment", row);
    }

    // =====================================================================
    //  Inventory Table + Edit/Delete Action Bar
    // =====================================================================
    private Node buildTableCard() {
        Button editBtn = primaryButton("Edit Selected");
        Button deleteBtn = dangerButton("Delete Selected");
        Button newBtn = new Button("+ New Item Mode");

        editBtn.setOnAction(e -> loadSelectedForEdit());
        deleteBtn.setOnAction(e -> deleteSelected());
        newBtn.setOnAction(e -> switchToAddMode());

        // Double-click a row to edit immediately
        table.setRowFactory(tv -> {
            TableRow<List<String>> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    loadSelectedForEdit();
                }
            });
            return row;
        });

        HBox actions = new HBox(10, newBtn, editBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox box = card("Inventory List", table, actions);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    // =====================================================================
    //  Add / Edit Form Section
    // =====================================================================
    private Node buildFormCard() {
        idField.setPromptText("-");
        idField.setEditable(false);
        nameField.setPromptText("e.g. Function Generator");
        categoryBox.setPromptText("Select Category");
        categoryBox.setMaxWidth(Double.MAX_VALUE);
        brandField.setPromptText("e.g. Rigol");
        locationField.setPromptText("e.g. Shelf A5");
        totalQtySpinner.setEditable(true);
        conditionBox.setValue("Good");
        statusBox.setValue("Available");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);

        ColumnConstraints labelCol = new ColumnConstraints();
        ColumnConstraints fieldCol1 = new ColumnConstraints();
        fieldCol1.setHgrow(Priority.ALWAYS);
        ColumnConstraints fieldCol2 = new ColumnConstraints();
        fieldCol2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol1, labelCol, fieldCol2);

        addField(grid, 0, 0, "Equipment ID:", idField);
        addField(grid, 2, 0, "Name:", nameField);
        addField(grid, 0, 1, "Category:", categoryBox);
        addField(grid, 2, 1, "Brand:", brandField);
        addField(grid, 0, 2, "Total Quantity:", totalQtySpinner);
        addField(grid, 2, 2, "Location:", locationField);
        addField(grid, 0, 3, "Condition:", conditionBox);
        addField(grid, 2, 3, "Status:", statusBox);

        Button clearBtn = new Button("Clear / Cancel");
        clearBtn.setOnAction(e -> switchToAddMode());
        saveBtn.setOnAction(e -> handleSave());

        HBox buttons = new HBox(10, saveBtn, clearBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        formHeader.setStyle("-fx-text-fill:" + GREEN + ";-fx-font-weight:bold;-fx-font-size:16;");
        VBox box = new VBox(10, formHeader, grid, buttons);
        box.setPadding(new Insets(12, 16, 14, 16));
        box.setStyle("-fx-background-color:white;-fx-border-color:" + GREEN + " transparent transparent transparent;"
                + "-fx-border-width:3 0 0 0;");
        return box;
    }

    // =====================================================================
    //  CRUD Operations
    // =====================================================================
    public void refreshTable() {
        String kw = keywordField.getText().trim().toLowerCase();
        String mode = searchBy.getValue();
        List<Equipment> matches = new ArrayList<>();

        if (mode.equals("ID") && !kw.isEmpty()) {
            Equipment exact = inventory.get(kw.toUpperCase()); // O(1) HashMap lookup
            if (exact != null) matches.add(exact);
        } else {
            for (Equipment eq : inventory.values()) {
                String target;
                switch (mode) {
                    case "Name": target = eq.getName(); break;
                    case "Category": target = eq.getCategory(); break;
                    case "Brand": target = eq.getBrand(); break;
                    case "Location": target = eq.getLocation(); break;
                    default: target = eq.getId();
                }
                if (kw.isEmpty() || target.toLowerCase().contains(kw)) {
                    matches.add(eq);
                }
            }
        }

        matches.removeIf(eq -> onlyAvailCheck.isSelected() && eq.getAvailableQty() == 0);
        matches.sort(Comparator.comparing(Equipment::getId));

        List<List<String>> rows = new ArrayList<>();
        for (Equipment eq : matches) {
            rows.add(Arrays.asList(
                    eq.getId(), eq.getName(), eq.getCategory(), eq.getBrand(), eq.getLocation(),
                    String.valueOf(eq.getAvailableQty()), String.valueOf(eq.getTotalQty()),
                    eq.getCondition(), eq.getStatus()
            ));
        }
        table.getItems().setAll(rows);
    }

    private void loadSelectedForEdit() {
        List<String> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert(Alert.AlertType.WARNING, "No Selection", "Please select an equipment row from the table first.");
            return;
        }
        Equipment eq = inventory.get(selected.get(0));
        if (eq == null) return;

        editMode = true;
        formHeader.setText("Edit Equipment (" + eq.getId() + ")");
        saveBtn.setText("Save Changes");

        idField.setText(eq.getId());
        idField.setDisable(true); // ID is the HashMap key; keep immutable while editing
        nameField.setText(eq.getName());
        categoryBox.setValue(eq.getCategory());
        brandField.setText(eq.getBrand());
        totalQtySpinner.getValueFactory().setValue(eq.getTotalQty());
        locationField.setText(eq.getLocation());
        conditionBox.setValue(eq.getCondition());
        statusBox.setValue(eq.getStatus());
    }

    private void handleSave() {
        String id;
        String name = nameField.getText().trim();
        String category = categoryBox.getValue();
        String brand = brandField.getText().trim();
        String location = locationField.getText().trim();
        String condition = conditionBox.getValue();
        String status = statusBox.getValue();
        try {
            totalQtySpinner.increment(0);
            } catch (Exception ignored) {
        }
        int totalQty = totalQtySpinner.getValue() == null ? 0 : totalQtySpinner.getValue();

        // VALIDATE REQUIRED FIELDS
        if (name.isEmpty() || category == null || brand.isEmpty() || location.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Missing Fields", "Please fill in Name, Category, Brand, and Location.");
            return;
        }
        if (totalQty <= 0) {
            alert(Alert.AlertType.WARNING, "Invalid Quantity", "Total quantity must be at least 1.");
            return;
        }

        // GENERATE ID ONLY WHEN ADDING A NEW ITEM
        if (!editMode) {
            try {
                id = Data_Store.getInstance().generateEquipmentId(category);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                alert(Alert.AlertType.ERROR, "ID Generation Error", ex.getMessage());
                return;
            }
            Equipment created = new Equipment(id, name, category, brand, totalQty, location, condition);
            created.setStatus(status);
            inventory.put(id, created);
            Data_Store.getInstance().saveEquipment();
            alert(Alert.AlertType.INFORMATION, "Item Added", name + " (" + id + ") has been added to inventory.");
        } else {
            // EDIT EXISTING ITEM - KEEP ORIGINAL ID
            id = idField.getText().trim();
            Equipment existing = inventory.get(id);
            if (existing == null) {
                return;
            }
            try {
                existing.setTotalQty(totalQty);
            } catch (IllegalArgumentException ex) {
                alert(Alert.AlertType.WARNING, "Invalid Quantity", ex.getMessage());
                return;
            }
            existing.setName(name);
            existing.setCategory(category);
            existing.setBrand(brand);
            existing.setLocation(location);
            existing.setCondition(condition);
            existing.setStatus(status);
            Data_Store.getInstance().saveEquipment();
            alert(Alert.AlertType.INFORMATION, "Item Updated", name + " (" + id + ") has been updated.");
        }
        switchToAddMode();
        refreshTable();
    }

    private void deleteSelected() {
        List<String> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert(Alert.AlertType.WARNING, "No Selection", "Please select an equipment row to delete.");
            return;
        }
        String id = selected.get(0);
        Equipment eq = inventory.get(id);
        if (eq == null) return;

        if (eq.getBorrowedQty() > 0) {
            alert(Alert.AlertType.WARNING, "Cannot Delete",
                    "Cannot delete '" + eq.getName() + "' because " + eq.getBorrowedQty() + " unit(s) are currently borrowed.");
            return;
        }

        boolean hasActiveRequest = allRequests.stream().anyMatch(r ->
                r.getItemId().equalsIgnoreCase(id) &&
                        (r.getStatus() == BorrowRequest.Status.PENDING_STAFF_REVIEW ||
                                r.getStatus() == BorrowRequest.Status.FILED ||
                                r.getStatus() == BorrowRequest.Status.APPROVED ||
                                r.getStatus() == BorrowRequest.Status.ISSUED ||
                                r.getStatus() == BorrowRequest.Status.RESERVED));

        if (hasActiveRequest) {
            alert(Alert.AlertType.WARNING, "Cannot Delete",
                    "Cannot delete '" + eq.getName() + "' because it has active/pending borrow requests.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete " + eq.getName() + " (" + id + ")?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Deletion");
        if (confirm.showAndWait().filter(b -> b == ButtonType.YES).isPresent()) {
            inventory.remove(id);
            Data_Store.getInstance().saveEquipment();
            if (editMode && idField.getText().equalsIgnoreCase(id)) {
                switchToAddMode();
            }
            refreshTable();
        }
    }

    private void switchToAddMode() {
        editMode = false;
        formHeader.setText("Add New Equipment");
        saveBtn.setText("Add Item");
        idField.setDisable(false);
        idField.setEditable(false);
        idField.clear();
        nameField.clear();
        categoryBox.setValue(null);
        brandField.clear();
        locationField.clear();
        totalQtySpinner.getValueFactory().setValue(1);
        conditionBox.setValue("Good");
        statusBox.setValue("Available");
    }

    // =====================================================================
    //  UI Helpers (matches BorrowerPortal styling)
    // =====================================================================
    private VBox card(String title, Node... body) {
        Label t = new Label(title);
        t.setStyle("-fx-text-fill:" + GREEN + ";-fx-font-weight:bold;-fx-font-size:16;");
        VBox c = new VBox(10, t);
        for (Node n : body) {
            VBox.setVgrow(n, Priority.ALWAYS);
            c.getChildren().add(n);
        }
        c.setPadding(new Insets(12, 16, 14, 16));
        c.setStyle("-fx-background-color:white;-fx-border-color:" + GREEN + " transparent transparent transparent;"
                + "-fx-border-width:3 0 0 0;");
        return c;
    }

    private TableView<List<String>> buildTable(String... cols) {
        TableView<List<String>> t = new TableView<>();
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setPlaceholder(new Label("No equipment found."));
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<List<String>, String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().get(idx)));
            t.getColumns().add(c);
        }
        return t;
    }

    private Button primaryButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + GREEN + ";-fx-text-fill:white;-fx-font-weight:bold;"
                + "-fx-padding:8 16 8 16;-fx-background-radius:3;-fx-cursor:hand;");
        return b;
    }

    private Button dangerButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:#DC3545;-fx-text-fill:white;-fx-font-weight:bold;"
                + "-fx-padding:8 16 8 16;-fx-background-radius:3;-fx-cursor:hand;");
        return b;
    }

    private void addField(GridPane g, int col, int row, String label, Control field) {
        field.setMaxWidth(Double.MAX_VALUE);
        g.add(new Label(label), col, row);
        g.add(field, col + 1, row);
    }

    private void alert(Alert.AlertType type, String header, String msg) {
        Alert a = new Alert(type, msg);
        a.setHeaderText(header);
        a.showAndWait();
    }
}