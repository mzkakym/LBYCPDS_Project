package org.example.lbycpds_project.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.lbycpds_project.models.*;
import org.example.lbycpds_project.utils.Data_Store;
import org.example.lbycpds_project.utils.User_File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Admin_controller {
    private static final String GREEN = "#006738";
    private static final String SELECT = "#D0E6D9";
    private static final String BG = "#F8F9FC";
    private static final String LINE = "#E1E1E1";

    @FXML private Label adminNameLabel;
    @FXML private Label adminIdLabel;
    @FXML private VBox navigationBox;
    @FXML private StackPane contentPane;

    private User currentUser;
    private Runnable onLogout;
    private final Data_Store store = Data_Store.getInstance();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final String[] menuItems = {
            "Admin Dashboard",
            "User Account Verification",
            "Borrowing Backlogs",
            "Staff Activity Logs",
            "Equipment Records",
            "Return & Incident Records",
            "Reservations & Repairs",
            "Reports & Analytics"
    };

    // INITIALIZATION
    public void setUser(User user, Runnable logoutAction) {

        currentUser = user;
        onLogout = logoutAction;

        adminNameLabel.setText(user.getFullName().toUpperCase());
        adminIdLabel.setText(user.getUserID());

        buildNavigation();
        showPage("Admin Dashboard");
    }

    private void alert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Admin");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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

        Label lastLogin = new Label(
                "Last Login : " +
                        LocalDateTime.now().format(
                                DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a")
                        )
        );

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
        bottom.setStyle(
                "-fx-border-color:" + LINE + ";" +
                        "-fx-border-width:1 0 0 0;"
        );

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

            case "Admin Dashboard":
                content = buildDashboard();
                break;

            case "User Account Verification":
                content = buildAccountVerification();
                break;

            case "Borrowing Backlogs":
                content = buildBorrowingBacklogs();
                break;

            case "Staff Activity Logs":
                content = buildActivityLogs();
                break;

            case "Equipment Records":
                content = buildEquipmentRecords();
                break;

            case "Return & Incident Records":
                content = buildReturnRecords();
                break;

            case "Reservations & Repairs":
                content = buildReservationsRepairs();
                break;

            case "Reports & Analytics":
                content = buildReports();
                break;

            default:
                content = buildDashboard();
        }

        contentPane.getChildren().setAll(content);

        navButtons.forEach((name, button) ->
                button.setStyle(navStyle(name.equals(page)))
        );
    }

    // ADMIN DASHBOARD
    private Node buildDashboard() {

        long pending = countStatus(
                BorrowRequest.Status.PENDING_STAFF_REVIEW
        );

        long approved = countStatus(
                BorrowRequest.Status.APPROVED
        );

        long rejected = countStatus(
                BorrowRequest.Status.REJECTED
        );

        long issued = countStatus(
                BorrowRequest.Status.ISSUED
        );

        long returned = countStatus(
                BorrowRequest.Status.RETURNED
        );

        HBox stats = new HBox(12,
                statCard(String.valueOf(pending),
                        "Pending Requests", "#00A8FF"),

                statCard(String.valueOf(approved),
                        "Approved Requests", GREEN),

                statCard(String.valueOf(issued),
                        "Currently Borrowed", "#FF8C00"),

                statCard(String.valueOf(rejected),
                        "Rejected Requests", "#DC3545")
        );

        TableView<List<String>> table = buildTable(
                "Transaction No.",
                "Borrower ID",
                "Equipment ID",
                "Quantity",
                "Status",
                "Remarks"
        );

        List<List<String>> rows = new ArrayList<>();

        List<BorrowRequest> requests = store.getAllRequests();

        for (int i = requests.size() - 1, count = 0;
             i >= 0 && count < 10; i--, count++) {

            BorrowRequest r = requests.get(i);

            rows.add(Arrays.asList(
                    r.getTxnNo(),
                    r.getBorrowerId(),
                    r.getItemId(),
                    String.valueOf(r.getQty()),
                    r.getStatus().name(),
                    r.getRemarks()
            ));
        }

        table.getItems().setAll(rows);

        return page(
                card("Administrator Dashboard",
                        new Label("Welcome, " +
                                currentUser.getFullName().toUpperCase())),
                stats,
                card("Recent Borrowing Transactions", table),
                card("System Activity",
                        new Label("Total Staff activities recorded: " +
                                store.getAuditLogs().size()),
                        new Label("Total returned transactions: " + returned))
        );
    }


    // USER ACCOUNT VERIFICATION
    private Node buildAccountVerification() {

        VBox content = new VBox(14);
        content.setPadding(new Insets(22));

        Label title = new Label("User Account Verification");

        title.setStyle(
                "-fx-font-size:20;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:" + GREEN + ";"
        );

        Label description = new Label(
                "Review and verify newly registered Borrower and Staff accounts."
        );

        ComboBox<String> filter = new ComboBox<>();

        filter.getItems().addAll(
                "PENDING",
                "VERIFIED",
                "REJECTED",
                "ALL"
        );

        filter.setValue("PENDING");

        TableView<User> table = new TableView<>();

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        TableColumn<User, String> idColumn =
                new TableColumn<>("User ID");

        idColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUserID())
        );

        TableColumn<User, String> nameColumn =
                new TableColumn<>("Full Name");

        nameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFullName())
        );

        TableColumn<User, String> roleColumn =
                new TableColumn<>("Role");

        roleColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRole())
        );

        TableColumn<User, String> departmentColumn =
                new TableColumn<>("Department");

        departmentColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDepartment())
        );

        TableColumn<User, String> contactColumn =
                new TableColumn<>("Contact");

        contactColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getContact())
        );

        TableColumn<User, String> statusColumn =
                new TableColumn<>("Status");

        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus())
        );

        table.getColumns().addAll(
                idColumn,
                nameColumn,
                roleColumn,
                departmentColumn,
                contactColumn,
                statusColumn
        );

        Label selectedLabel = new Label("No account selected.");

        Button verifyButton = primary("Verify Account");
        Button rejectButton = new Button("Reject Account");
        Button refreshButton = new Button("Refresh");

        rejectButton.setStyle(
                "-fx-background-color:#DC3545;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-weight:bold;" +
                        "-fx-padding:8 16 8 16;"
        );

        // REFRESH TABLE
        Runnable refresh = () -> {

            List<User> users = User_File.getAllUsers();

            List<User> displayed = new ArrayList<>();

            for (User user : users) {

                // Do not allow the Admin to modify their own account
                if (user.getRole().equalsIgnoreCase("ADMIN")) {
                    continue;
                }

                if (filter.getValue().equals("ALL") ||
                        user.getStatus().equalsIgnoreCase(filter.getValue())) {

                    displayed.add(user);
                }
            }

            table.getItems().setAll(displayed);
            selectedLabel.setText("No account selected.");
        };

        filter.setOnAction(e -> refresh.run());
        refreshButton.setOnAction(e -> refresh.run());

        // DISPLAY SELECTED ACCOUNT
        table.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldUser, selectedUser) -> {

                    if (selectedUser != null) {

                        selectedLabel.setText(
                                "Selected Account: " +
                                        selectedUser.getFullName() +
                                        " | " + selectedUser.getUserID() +
                                        " | " + selectedUser.getRole() +
                                        " | " + selectedUser.getStatus()
                        );
                    }
                });

        // VERIFY ACCOUNT
        verifyButton.setOnAction(e -> {

            User selected = table.getSelectionModel()
                    .getSelectedItem();

            if (selected == null) {
                alert("Please select an account first.");
                return;
            }

            if (!selected.getStatus().equalsIgnoreCase("PENDING")) {
                alert("Only pending accounts can be verified.");
                return;
            }

            Alert confirmation = new Alert(
                    Alert.AlertType.CONFIRMATION
            );

            confirmation.setTitle("Verify Account");
            confirmation.setHeaderText("Verify this account?");
            confirmation.setContentText(
                    "User: " + selected.getFullName() +
                            "\nUser ID: " + selected.getUserID() +
                            "\nRole: " + selected.getRole()
            );

            Optional<ButtonType> result = confirmation.showAndWait();

            if (result.isPresent() &&
                    result.get() == ButtonType.OK) {

                boolean updated = User_File.updateStatus(
                        selected.getUserID(),
                        "VERIFIED"
                );

                if (updated) {
                    alert("Account successfully verified.");
                    refresh.run();
                } else {
                    alert("Unable to update account.");
                }
            }
        });

        // REJECT ACCOUNT
        rejectButton.setOnAction(e -> {

            User selected = table.getSelectionModel()
                    .getSelectedItem();

            if (selected == null) {
                alert("Please select an account first.");
                return;
            }

            if (!selected.getStatus().equalsIgnoreCase("PENDING")) {
                alert("Only pending accounts can be rejected.");
                return;
            }

            Alert confirmation = new Alert(
                    Alert.AlertType.CONFIRMATION
            );

            confirmation.setTitle("Reject Account");
            confirmation.setHeaderText("Reject this account?");
            confirmation.setContentText(
                    "User: " + selected.getFullName() +
                            "\nUser ID: " + selected.getUserID()
            );

            Optional<ButtonType> result = confirmation.showAndWait();

            if (result.isPresent() &&
                    result.get() == ButtonType.OK) {

                boolean updated = User_File.updateStatus(
                        selected.getUserID(),
                        "REJECTED"
                );

                if (updated) {
                    alert("Account successfully rejected.");
                    refresh.run();
                } else {
                    alert("Unable to update account.");
                }
            }
        });

        HBox controls = new HBox(
                10,
                new Label("Account Status:"),
                filter,
                refreshButton
        );

        HBox actions = new HBox(
                10,
                verifyButton,
                rejectButton
        );

        VBox.setVgrow(table, Priority.ALWAYS);

        content.getChildren().addAll(
                title,
                description,
                controls,
                table,
                selectedLabel,
                actions
        );

        refresh.run();

        return page(content);
    }

    // BORROWING BACKLOGS
    private Node buildBorrowingBacklogs() {

        ComboBox<String> filter = new ComboBox<>();

        filter.getItems().addAll(
                "All Requests",
                "PENDING_STAFF_REVIEW",
                "APPROVED",
                "REJECTED",
                "ISSUED",
                "RETURNED",
                "RESERVED",
                "FILED",
                "RETURNED_TO_STUDENT"
        );

        filter.setValue("All Requests");

        TableView<List<String>> table = buildTable(
                "Transaction No.",
                "Borrower ID",
                "Equipment ID",
                "Quantity",
                "Purpose",
                "Request Date",
                "Expected Return",
                "Status",
                "Remarks"
        );

        Runnable refresh = () -> {

            List<List<String>> rows = new ArrayList<>();

            for (BorrowRequest r : store.getAllRequests()) {

                boolean include =
                        filter.getValue().equals("All Requests") ||
                                r.getStatus().name().equals(filter.getValue());

                if (include) {

                    rows.add(Arrays.asList(
                            r.getTxnNo(),
                            r.getBorrowerId(),
                            r.getItemId(),
                            String.valueOf(r.getQty()),
                            r.getPurpose(),
                            r.getRequestDate().toString(),
                            r.getExpectedReturn().toString(),
                            r.getStatus().name(),
                            r.getRemarks()
                    ));
                }
            }

            table.getItems().setAll(rows);
        };

        filter.setOnAction(e -> refresh.run());

        refresh.run();

        return page(
                card("Borrowing Backlogs (Read Only)",
                        new HBox(10,
                                new Label("Filter by status:"),
                                filter),
                        table)
        );
    }

    // =====================================================
    // STAFF ACTIVITY LOGS
    // =====================================================

    private Node buildActivityLogs() {

        TableView<List<String>> table = buildTable(
                "Date & Time",
                "Staff ID",
                "Action",
                "Transaction No.",
                "Details"
        );

        TextField searchField = new TextField();
        searchField.setPromptText(
                "Search by Staff ID, action, or transaction..."
        );

        Button searchButton = primary("Search");
        Button refreshButton = new Button("Refresh");

        Runnable refresh = () -> {

            List<List<String>> rows = new ArrayList<>();

            String keyword = searchField.getText()
                    .trim().toLowerCase();

            for (String log : store.getAuditLogs()) {

                String[] data = log.split("\\|", -1);

                if (data.length < 5) {
                    continue;
                }

                String combined = String.join(" ", data)
                        .toLowerCase();

                if (keyword.isEmpty() ||
                        combined.contains(keyword)) {

                    rows.add(Arrays.asList(
                            data[0],
                            data[1],
                            data[2],
                            data[3],
                            data[4]
                    ));
                }
            }

            Collections.reverse(rows);
            table.getItems().setAll(rows);
        };

        searchButton.setOnAction(e -> refresh.run());
        searchField.setOnAction(e -> refresh.run());
        refreshButton.setOnAction(e -> refresh.run());

        return page(
                card("Staff Activity Logs",
                        new HBox(10, searchField,
                                searchButton, refreshButton),
                        table)
        );
    }

    // =====================================================
    // EQUIPMENT RECORDS
    // =====================================================

    private Node buildEquipmentRecords() {

        TableView<List<String>> table = buildTable(
                "Equipment ID",
                "Name",
                "Category",
                "Brand",
                "Location",
                "Total Quantity",
                "Available",
                "Borrowed",
                "Condition",
                "Status"
        );

        List<List<String>> rows = new ArrayList<>();

        for (Equipment e : store.getInventory().values()) {

            rows.add(Arrays.asList(
                    e.getId(),
                    e.getName(),
                    e.getCategory(),
                    e.getBrand(),
                    e.getLocation(),
                    String.valueOf(e.getTotalQty()),
                    String.valueOf(e.getAvailableQty()),
                    String.valueOf(e.getBorrowedQty()),
                    e.getCondition(),
                    e.getStatus()
            ));
        }

        table.getItems().setAll(rows);

        return page(
                card("Equipment Inventory Records (Read Only)", table)
        );
    }

    // =====================================================
    // RETURN AND INCIDENT RECORDS
    // =====================================================

    private Node buildReturnRecords() {

        TableView<List<String>> table = buildTable(
                "Transaction No.",
                "Borrower ID",
                "Equipment ID",
                "Quantity",
                "Expected Return",
                "Status",
                "Remarks"
        );

        List<List<String>> rows = new ArrayList<>();

        for (BorrowRequest r : store.getAllRequests()) {

            if (r.getStatus() == BorrowRequest.Status.RETURNED ||
                    r.getStatus() == BorrowRequest.Status.ISSUED) {

                rows.add(Arrays.asList(
                        r.getTxnNo(),
                        r.getBorrowerId(),
                        r.getItemId(),
                        String.valueOf(r.getQty()),
                        r.getExpectedReturn().toString(),
                        r.getStatus().name(),
                        r.getRemarks()
                ));
            }
        }

        table.getItems().setAll(rows);

        return page(
                card("Return and Incident Records (Read Only)", table)
        );
    }

    // =====================================================
    // RESERVATIONS AND REPAIRS
    // =====================================================

    private Node buildReservationsRepairs() {

        TableView<List<String>> reservations = buildTable(
                "Transaction No.",
                "Borrower ID",
                "Equipment ID",
                "Quantity",
                "Expected Return",
                "Status"
        );

        List<List<String>> reservationRows = new ArrayList<>();

        for (BorrowRequest r : store.getAllRequests()) {

            if (r.getStatus() == BorrowRequest.Status.RESERVED) {

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

        TableView<List<String>> repairs = buildTable(
                "Equipment ID",
                "Name",
                "Brand",
                "Location",
                "Condition",
                "Status"
        );

        List<List<String>> repairRows = new ArrayList<>();

        for (Equipment e : store.getInventory().values()) {

            if (e.getStatus().equalsIgnoreCase("Damaged") ||
                    e.getStatus().equalsIgnoreCase("Missing") ||
                    e.getStatus().equalsIgnoreCase("Under Maintenance") ||
                    e.getCondition().equalsIgnoreCase("Needs Repair")) {

                repairRows.add(Arrays.asList(
                        e.getId(),
                        e.getName(),
                        e.getBrand(),
                        e.getLocation(),
                        e.getCondition(),
                        e.getStatus()
                ));
            }
        }

        repairs.getItems().setAll(repairRows);

        return page(
                card("Reservation Records", reservations),
                card("Equipment Repair and Maintenance Records", repairs)
        );
    }

    // =====================================================
    // REPORTS AND ANALYTICS
    // =====================================================

    private Node buildReports() {

        int total = store.getInventory().values().stream()
                .mapToInt(Equipment::getTotalQty)
                .sum();

        int available = store.getInventory().values().stream()
                .mapToInt(Equipment::getAvailableQty)
                .sum();

        int borrowed = store.getInventory().values().stream()
                .mapToInt(Equipment::getBorrowedQty)
                .sum();

        VBox report = new VBox(12);

        report.getChildren().addAll(
                new Label("Total Equipment Quantity: " + total),
                new Label("Available Equipment Quantity: " + available),
                new Label("Currently Borrowed Quantity: " + borrowed),
                new Label("Total Borrowing Requests: " +
                        store.getAllRequests().size()),
                new Label("Pending Requests: " +
                        countStatus(BorrowRequest.Status.PENDING_STAFF_REVIEW)),
                new Label("Approved Requests: " +
                        countStatus(BorrowRequest.Status.APPROVED)),
                new Label("Rejected Requests: " +
                        countStatus(BorrowRequest.Status.REJECTED)),
                new Label("Issued Transactions: " +
                        countStatus(BorrowRequest.Status.ISSUED)),
                new Label("Returned Transactions: " +
                        countStatus(BorrowRequest.Status.RETURNED)),
                new Label("Staff Activities Recorded: " +
                        store.getAuditLogs().size())
        );

        return page(card("System Reports and Analytics", report));
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    private long countStatus(BorrowRequest.Status status) {

        return store.getAllRequests().stream()
                .filter(r -> r.getStatus() == status)
                .count();
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
                        "-fx-border-color:" + GREEN +
                        " transparent transparent transparent;" +
                        "-fx-border-width:3 0 0 0;"
        );

        return box;
    }

    private HBox statCard(String value, String title, String color) {

        Label number = new Label(value);
        number.setStyle(
                "-fx-font-size:26;" +
                        "-fx-font-weight:bold;"
        );

        Label description = new Label(title);
        description.setStyle("-fx-text-fill:#6E6E6E;");

        VBox box = new VBox(5, number, description);
        box.setPadding(new Insets(16));
        box.setMaxWidth(Double.MAX_VALUE);

        box.setStyle(
                "-fx-background-color:white;" +
                        "-fx-border-color:" + color +
                        " transparent transparent transparent;" +
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
                        "-fx-background-radius:3;"
        );

        return button;
    }
}

