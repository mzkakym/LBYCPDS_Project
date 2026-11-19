package org.example.lbycpds_project.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.beans.property.SimpleStringProperty;

import org.example.lbycpds_project.models.*;
import org.example.lbycpds_project.utils.Data_Store;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Staff_controller {
    private static final String GREEN = "#006738";
    private static final String SELECT = "#D0E6D9";
    private static final String BG = "#F8F9FC";
    private static final String LINE = "#E1E1E1";

    @FXML private Label staffNameLabel;
    @FXML private Label staffIdLabel;
    @FXML private VBox navigationBox;
    @FXML private StackPane contentPane;

    private User currentUser;
    private Runnable onLogout;

    private final Data_Store store = Data_Store.getInstance();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final String[] menuItems = {
            "Staff Dashboard",
            "Manage Equipment",
            "Borrowing Requests",
            "Release Equipment",
            "Process Returns",
            "Overdue & Missing Items",
            "Reservations & Repairs",
            "Duty Schedule"
    };

    public void setUser(User user, Runnable logoutAction) {
        currentUser = user;
        onLogout = logoutAction;
        staffNameLabel.setText(user.getFullName().toUpperCase());
        staffIdLabel.setText(user.getUserID());
        buildNavigation();
        showPage("Staff Dashboard");
    }

    // NAVIGATION
    private void buildNavigation() {
        navigationBox.getChildren().clear();
        navButtons.clear();
        for (String item : menuItems) {
            Button button = new Button(item);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.setStyle(navStyle(false));
            button.setOnAction(e -> showPage(item));
            navButtons.put(item, button);
            navigationBox.getChildren().add(button);
        }
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label lastLogin = new Label("Last Login : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a")));
        lastLogin.setWrapText(true);
        lastLogin.setStyle("-fx-font-size:11;");

        Button logoutButton = new Button("⏻");
        logoutButton.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #f83d3d;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-family: 'Arial Unicode MS';" +
                        "-fx-cursor: hand;"
        );

        logoutButton.setOnAction(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });
        HBox bottom = new HBox(8, lastLogin, logoutButton);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(10, 12, 10, 14));
        bottom.setStyle("-fx-border-color:" + LINE + ";" + "-fx-border-width:1 0 0 0;");
        navigationBox.getChildren().addAll(spacer, bottom);
    }

    private String navStyle(boolean selected) {
        return "-fx-background-color:" +
                (selected ? SELECT : "white") + ";" +
                "-fx-text-fill:#1E1E1E;" +
                "-fx-font-size:13;" +
                "-fx-alignment:CENTER_LEFT;" +
                "-fx-padding:11 18 11 18;" +
                "-fx-background-radius:0;" +
                "-fx-cursor:hand;";
    }

    private void showPage(String page) {
        Node content;
        switch (page) {
            case "Staff Dashboard":
                content = buildDashboard();
                break;

            case "Manage Equipment":
                content = new EquipmentManagerPane(store.getInventory(), store.getAllRequests());
                break;

            case "Borrowing Requests":
                content = buildRequests();
                break;

            case "Release Equipment":
                content = buildRelease();
                break;

            case "Process Returns":
                content = buildReturns();
                break;

            case "Overdue & Missing Items":
                content = buildOverdue();
                break;

            case "Reservations & Repairs":
                content = buildReservationsRepairs();
                break;

            case "Duty Schedule":
                content = buildDutySchedule();
                break;

            default:
                content = buildDashboard();
        }
        contentPane.getChildren().setAll(content);
        navButtons.forEach((name, button) -> button.setStyle(navStyle(name.equals(page)))
        );
    }

    // DASHBOARD
    private Node buildDashboard() {
        long pending = store.getAllRequests().stream().filter(r -> r.getStatus() == BorrowRequest.Status.PENDING_STAFF_REVIEW).count();
        long filed = store.getAllRequests().stream().filter(r -> r.getStatus() == BorrowRequest.Status.FILED).count();
        long overdue = store.getAllRequests().stream().filter(r -> r.getStatus() == BorrowRequest.Status.ISSUED && r.getExpectedReturn().isBefore(LocalDate.now())).count();
        long available = store.getInventory().values().stream().mapToLong(Equipment::getAvailableQty).sum();

        Label pendingValue = new Label(String.valueOf(pending));
        Label filedValue = new Label(String.valueOf(filed));
        Label overdueValue = new Label(String.valueOf(overdue));
        Label availableValue = new Label(String.valueOf(available));

        HBox stats = new HBox(12,
                statCard(pendingValue, "Pending Staff Review", "#00A8FF"),
                statCard(filedValue, "Filed for Admin", GREEN),
                statCard(overdueValue, "Overdue Items", "#FF8C00"),
                statCard(availableValue, "Available Quantity", "#6E9F2F")
        );

        TableView<List<String>> table = buildTable(
                "Transaction No.",
                "Borrower ID",
                "Equipment ID",
                "Quantity",
                "Expected Return",
                "Status"
        );

        List<List<String>> rows = new ArrayList<>();
        for (BorrowRequest request : store.getAllRequests()) {
            if (request.getStatus() == BorrowRequest.Status.PENDING_STAFF_REVIEW || request.getStatus() == BorrowRequest.Status.FILED) {
                rows.add(Arrays.asList(
                        request.getTxnNo(),
                        request.getBorrowerId(),
                        request.getItemId(),
                        String.valueOf(request.getQty()),
                        request.getExpectedReturn().toString(),
                        request.getStatus().name()
                ));
            }
        }

        table.getItems().setAll(rows);
        VBox recent = card("Pending Borrowing Activities", table);
        VBox.setVgrow(recent, Priority.ALWAYS);
        return page(card("Staff Dashboard", new Label("Welcome, " + currentUser.getFullName().toUpperCase())), stats, recent);
    }

    // BORROWING REQUESTS
    private Node buildRequests() {
        TableView<List<String>> table = buildTable(
                "Transaction No.",
                "Borrower ID",
                "Equipment ID",
                "Quantity",
                "Purpose",
                "Expected Return",
                "Status",
                "Remarks"
        );

        TextField remarks = new TextField();
        remarks.setPromptText("Enter remarks or rejection reason");

        Button approveButton = primary("Approve Request");
        Button rejectButton = danger("Reject Request");
        Button refreshButton = new Button("Refresh");

        Runnable refresh = () -> {

            List<List<String>> rows = new ArrayList<>();

            for (BorrowRequest r : store.getAllRequests()) {

                if (r.getStatus() ==
                        BorrowRequest.Status.PENDING_STAFF_REVIEW ||
                        r.getStatus() ==
                                BorrowRequest.Status.RETURNED_TO_STUDENT) {

                    rows.add(Arrays.asList(
                            r.getTxnNo(),
                            r.getBorrowerId(),
                            r.getItemId(),
                            String.valueOf(r.getQty()),
                            r.getPurpose(),
                            r.getExpectedReturn().toString(),
                            r.getStatus().name(),
                            r.getRemarks()
                    ));
                }
            }

            table.getItems().setAll(rows);
        };

        // APPROVE REQUEST
        approveButton.setOnAction(e -> {

            List<String> selected =
                    table.getSelectionModel().getSelectedItem();

            if (selected == null) {
                alert("Select a borrowing request first.");
                return;
            }

            BorrowRequest request = findRequest(selected.get(0));

            if (request == null ||
                    (request.getStatus() !=
                            BorrowRequest.Status.PENDING_STAFF_REVIEW &&
                            request.getStatus() !=
                                    BorrowRequest.Status.RETURNED_TO_STUDENT)) {

                alert("This request is no longer pending.");
                return;
            }

            Equipment equipment =
                    store.getInventory().get(request.getItemId());

            if (equipment == null) {
                alert("Equipment record not found.");
                return;
            }

            if (equipment.getAvailableQty() < request.getQty()) {
                alert("Insufficient available equipment quantity.");
                return;
            }

            String staffRemarks = remarks.getText().trim();

            if (staffRemarks.isEmpty()) {
                staffRemarks = "Approved by Laboratory Staff";
            }

            request.setStatus(
                    BorrowRequest.Status.APPROVED,
                    staffRemarks
            );

            store.saveRequests();

            store.recordActivity(
                    currentUser.getUserID(),
                    "APPROVED REQUEST",
                    request.getTxnNo(),
                    "Approved " + request.getQty() +
                            " unit(s) of equipment " + request.getItemId()
            );

            remarks.clear();
            refresh.run();

            alert("Request approved. It is now ready for equipment release.");
        });

        // REJECT REQUEST
        rejectButton.setOnAction(e -> {

            List<String> selected =
                    table.getSelectionModel().getSelectedItem();

            if (selected == null) {
                alert("Select a borrowing request first.");
                return;
            }

            if (remarks.getText().trim().isEmpty()) {
                alert("Please provide a reason for rejecting the request.");
                return;
            }

            BorrowRequest request = findRequest(selected.get(0));

            if (request == null ||
                    (request.getStatus() !=
                            BorrowRequest.Status.PENDING_STAFF_REVIEW &&
                            request.getStatus() !=
                                    BorrowRequest.Status.RETURNED_TO_STUDENT)) {

                alert("This request is no longer pending.");
                return;
            }

            String reason = remarks.getText().trim();

            request.setStatus(
                    BorrowRequest.Status.REJECTED,
                    reason
            );

            store.saveRequests();

            store.recordActivity(
                    currentUser.getUserID(),
                    "REJECTED REQUEST",
                    request.getTxnNo(),
                    reason
            );

            remarks.clear();
            refresh.run();

            alert("Borrowing request rejected.");
        });

        refreshButton.setOnAction(e -> refresh.run());

        HBox actions = new HBox(
                10,
                approveButton,
                rejectButton,
                refreshButton
        );

        VBox content = card(
                "Manage Borrowing Requests",
                table,
                new Label("Staff Remarks:"),
                remarks,
                actions
        );

        VBox.setVgrow(content, Priority.ALWAYS);

        refresh.run();

        return page(content);
    }


    // =========================================================
    // RELEASE EQUIPMENT
    // =========================================================

    private Node buildRelease() {

        TableView<List<String>> table = buildTable(
                "Transaction No.",
                "Borrower ID",
                "Equipment ID",
                "Quantity",
                "Expected Return",
                "Status"
        );

        Button releaseButton = primary("Release Selected Equipment");
        Button refreshButton = new Button("Refresh");

        Runnable refresh = () -> {

            List<List<String>> rows = new ArrayList<>();

            for (BorrowRequest r : store.getAllRequests()) {

                if (r.getStatus() ==
                        BorrowRequest.Status.APPROVED) {

                    rows.add(Arrays.asList(
                            r.getTxnNo(),
                            r.getBorrowerId(),
                            r.getItemId(),
                            String.valueOf(r.getQty()),
                            r.getExpectedReturn().toString(),
                            r.getStatus().name()
                    ));
                }
            }

            table.getItems().setAll(rows);
        };

        releaseButton.setOnAction(e -> {

            List<String> selected =
                    table.getSelectionModel().getSelectedItem();

            if (selected == null) {
                alert("Select an approved request first.");
                return;
            }

            BorrowRequest request = findRequest(selected.get(0));

            if (request == null ||
                    request.getStatus() !=
                            BorrowRequest.Status.APPROVED) {

                alert("This request is no longer approved.");
                return;
            }

            Equipment equipment =
                    store.getInventory().get(request.getItemId());

            if (equipment == null) {
                alert("Equipment record not found.");
                return;
            }

            if (equipment.getAvailableQty() < request.getQty()) {
                alert("Not enough available equipment quantity.");
                return;
            }

            equipment.setAvailableQty(
                    equipment.getAvailableQty() - request.getQty()
            );

            request.setStatus(
                    BorrowRequest.Status.ISSUED,
                    "Equipment released by Laboratory Staff"
            );

            store.saveEquipment();
            store.saveRequests();

            store.recordActivity(
                    currentUser.getUserID(),
                    "RELEASED EQUIPMENT",
                    request.getTxnNo(),
                    "Released " + request.getQty() +
                            " unit(s) of equipment " + request.getItemId()
            );

            refresh.run();

            alert("Equipment successfully released.");
        });

        refreshButton.setOnAction(e -> refresh.run());

        VBox content = card(
                "Release Approved Equipment",
                table,
                new HBox(10, releaseButton, refreshButton)
        );

        VBox.setVgrow(content, Priority.ALWAYS);

        refresh.run();

        return page(content);
    }

    // =========================================================
    // PROCESS RETURNS
    // =========================================================

    private Node buildReturns() {

        TableView<List<String>> table = buildTable(
                "Transaction No.",
                "Borrower ID",
                "Equipment ID",
                "Quantity",
                "Expected Return",
                "Status"
        );

        ComboBox<String> condition = new ComboBox<>(
                FXCollections.observableArrayList(
                        "Good", "Fair", "Needs Repair", "Damaged"
                )
        );

        condition.setValue("Good");

        Button returnButton = primary("Process Return");
        Button refreshButton = new Button("Refresh");

        Runnable refresh = () -> {

            List<List<String>> rows = new ArrayList<>();

            for (BorrowRequest r : store.getAllRequests()) {

                if (r.getStatus() ==
                        BorrowRequest.Status.ISSUED) {

                    rows.add(Arrays.asList(
                            r.getTxnNo(),
                            r.getBorrowerId(),
                            r.getItemId(),
                            String.valueOf(r.getQty()),
                            r.getExpectedReturn().toString(),
                            r.getStatus().name()
                    ));
                }
            }

            table.getItems().setAll(rows);
        };

        returnButton.setOnAction(e -> {

            List<String> selected =
                    table.getSelectionModel().getSelectedItem();

            if (selected == null) {
                alert("Select an issued transaction first.");
                return;
            }

            BorrowRequest request = findRequest(selected.get(0));

            if (request == null ||
                    request.getStatus() !=
                            BorrowRequest.Status.ISSUED) {

                alert("This transaction is no longer active.");
                return;
            }

            Equipment equipment =
                    store.getInventory().get(request.getItemId());

            if (equipment == null) {
                alert("Equipment record not found.");
                return;
            }

            String selectedCondition = condition.getValue();

            int newAvailable =
                    equipment.getAvailableQty() + request.getQty();

            if (newAvailable > equipment.getTotalQty()) {
                alert("Return quantity exceeds the inventory total.");
                return;
            }

            equipment.setAvailableQty(newAvailable);

            if (selectedCondition.equals("Needs Repair") ||
                    selectedCondition.equals("Damaged")) {

                equipment.setCondition(selectedCondition);
                equipment.setStatus(
                        selectedCondition.equals("Damaged")
                                ? "Damaged"
                                : "Under Maintenance"
                );
            }

            request.setStatus(
                    BorrowRequest.Status.RETURNED,
                    "Returned. Condition: " + selectedCondition
            );

            store.saveEquipment();
            store.saveRequests();

            store.recordActivity(
                    currentUser.getUserID(),
                    "PROCESSED RETURN",
                    request.getTxnNo(),
                    "Returned " + request.getQty() +
                            " unit(s) of equipment " + request.getItemId() +
                            ". Condition: " + selectedCondition
            );

            refresh.run();

            alert("Equipment return successfully recorded.");
        });

        refreshButton.setOnAction(e -> refresh.run());

        VBox content = card(
                "Process Equipment Returns",
                table,
                new Label("Returned equipment condition:"),
                condition,
                new HBox(10, returnButton, refreshButton)
        );

        VBox.setVgrow(content, Priority.ALWAYS);

        refresh.run();

        return page(content);
    }

    // =========================================================
    // OVERDUE & MISSING ITEMS
    // =========================================================

    private Node buildOverdue() {

        TableView<List<String>> table = buildTable(
                "Transaction No.",
                "Borrower ID",
                "Equipment ID",
                "Quantity",
                "Expected Return",
                "Days Overdue",
                "Status"
        );

        List<List<String>> rows = new ArrayList<>();

        for (BorrowRequest r : store.getAllRequests()) {

            if (r.getStatus() ==
                    BorrowRequest.Status.ISSUED &&
                    r.getExpectedReturn().isBefore(LocalDate.now())) {

                long days = java.time.temporal.ChronoUnit.DAYS.between(
                        r.getExpectedReturn(), LocalDate.now()
                );

                rows.add(Arrays.asList(
                        r.getTxnNo(),
                        r.getBorrowerId(),
                        r.getItemId(),
                        String.valueOf(r.getQty()),
                        r.getExpectedReturn().toString(),
                        String.valueOf(days),
                        "OVERDUE"
                ));
            }
        }

        table.getItems().setAll(rows);

        TableView<List<String>> missingTable =
                buildTable("Equipment ID", "Name", "Brand", "Location", "Status");

        List<List<String>> missingRows = new ArrayList<>();

        for (Equipment equipment : store.getInventory().values()) {

            if (equipment.getStatus().equalsIgnoreCase("Missing") ||
                    equipment.getStatus().equalsIgnoreCase("Damaged")) {

                missingRows.add(Arrays.asList(
                        equipment.getId(),
                        equipment.getName(),
                        equipment.getBrand(),
                        equipment.getLocation(),
                        equipment.getStatus()
                ));
            }
        }

        missingTable.getItems().setAll(missingRows);

        VBox overdueCard = card("Overdue Transactions", table);
        VBox missingCard = card("Damaged / Missing Equipment", missingTable);

        VBox.setVgrow(overdueCard, Priority.ALWAYS);
        VBox.setVgrow(missingCard, Priority.ALWAYS);

        return page(overdueCard, missingCard);
    }

    // =========================================================
    // RESERVATIONS & REPAIRS
    // =========================================================

    private Node buildReservationsRepairs() {

        TableView<List<String>> reservations =
                buildTable(
                        "Transaction No.",
                        "Borrower ID",
                        "Equipment ID",
                        "Quantity",
                        "Expected Return",
                        "Status"
                );

        List<List<String>> reservationRows = new ArrayList<>();

        for (BorrowRequest r : store.getAllRequests()) {

            if (r.getStatus() ==
                    BorrowRequest.Status.RESERVED) {

                reservationRows.add(Arrays.asList(
                        r.getTxnNo(),
                        r.getBorrowerId(),
                        r.getItemId(),
                        String.valueOf(r.getQty()),
                        r.getExpectedReturn().toString(),
                        r.getStatus().name()
                ));
            }
        }

        reservations.getItems().setAll(reservationRows);

        TableView<List<String>> repairs =
                buildTable(
                        "Equipment ID",
                        "Equipment Name",
                        "Brand",
                        "Condition",
                        "Location",
                        "Status"
                );

        List<List<String>> repairRows = new ArrayList<>();

        for (Equipment equipment : store.getInventory().values()) {

            if (equipment.getStatus().equalsIgnoreCase("Under Maintenance") ||
                    equipment.getStatus().equalsIgnoreCase("Damaged") ||
                    equipment.getCondition().equalsIgnoreCase("Needs Repair")) {

                repairRows.add(Arrays.asList(
                        equipment.getId(),
                        equipment.getName(),
                        equipment.getBrand(),
                        equipment.getCondition(),
                        equipment.getLocation(),
                        equipment.getStatus()
                ));
            }
        }

        repairs.getItems().setAll(repairRows);

        VBox reservationCard =
                card("Equipment Reservations", reservations);

        VBox repairCard =
                card("Equipment for Repair / Maintenance", repairs);

        VBox.setVgrow(reservationCard, Priority.ALWAYS);
        VBox.setVgrow(repairCard, Priority.ALWAYS);

        return page(reservationCard, repairCard);
    }

    // =========================================================
    // DUTY SCHEDULE
    // =========================================================

    private Node buildDutySchedule() {

        Label message = new Label(
                "The duty schedule interface will be connected to the " +
                        "technician Circular Linked List implementation."
        );

        message.setWrapText(true);
        message.setStyle("-fx-font-size:14;");

        return page(
                card("Technician Duty Schedule", message)
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private BorrowRequest findRequest(String txnNo) {

        for (BorrowRequest request : store.getAllRequests()) {

            if (request.getTxnNo().equalsIgnoreCase(txnNo)) {
                return request;
            }
        }

        return null;
    }

    private VBox page(Node... nodes) {

        VBox root = new VBox(14, nodes);
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color:" + BG + ";");

        return root;
    }

    private VBox card(String title, Node... nodes) {

        Label heading = new Label(title);

        heading.setStyle(
                "-fx-text-fill:" + GREEN + ";" +
                        "-fx-font-weight:bold;" +
                        "-fx-font-size:16;"
        );

        VBox box = new VBox(12);
        box.getChildren().add(heading);
        box.getChildren().addAll(nodes);

        box.setPadding(new Insets(14, 18, 16, 18));

        box.setStyle(
                "-fx-background-color:white;" +
                        "-fx-border-color:" + GREEN + " transparent transparent transparent;" +
                        "-fx-border-width:3 0 0 0;"
        );

        return box;
    }

    private HBox statCard(Label value, String title, String color) {

        value.setStyle(
                "-fx-font-size:26;" +
                        "-fx-font-weight:bold;"
        );

        Label description = new Label(title);
        description.setStyle("-fx-text-fill:#6E6E6E;");

        VBox box = new VBox(5, value, description);

        box.setPadding(new Insets(16));
        box.setMaxWidth(Double.MAX_VALUE);

        box.setStyle(
                "-fx-background-color:white;" +
                        "-fx-border-color:" + color + " transparent transparent transparent;" +
                        "-fx-border-width:3 0 0 0;"
        );

        HBox.setHgrow(box, Priority.ALWAYS);

        return new HBox(box);
    }

    private TableView<List<String>> buildTable(String... columns) {

        TableView<List<String>> table = new TableView<>();

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        table.setPlaceholder(new Label("No records found."));

        for (int i = 0; i < columns.length; i++) {

            final int index = i;

            TableColumn<List<String>, String> column =
                    new TableColumn<>(columns[i]);

            column.setCellValueFactory(data ->
                    new SimpleStringProperty(
                            data.getValue().get(index)
                    )
            );

            table.getColumns().add(column);
        }

        return table;
    }

    private Button primary(String text) {

        Button button = new Button(text);

        button.setStyle(
                "-fx-background-color:" + GREEN + ";" +
                        "-fx-text-fill:white;" +
                        "-fx-font-weight:bold;" +
                        "-fx-padding:8 16 8 16;" +
                        "-fx-background-radius:3;" +
                        "-fx-cursor:hand;"
        );

        return button;
    }

    private Button danger(String text) {

        Button button = new Button(text);

        button.setStyle(
                "-fx-background-color:#DC3545;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-weight:bold;" +
                        "-fx-padding:8 16 8 16;" +
                        "-fx-background-radius:3;"
        );

        return button;
    }

    private void alert(String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
