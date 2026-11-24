package org.example.lbycpds_project.controllers;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.lbycpds_project.models.BorrowRequest;
import org.example.lbycpds_project.models.Equipment;
import org.example.lbycpds_project.models.User;
import org.example.lbycpds_project.utils.Data_Store;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class Borrower_controller {

    private final Data_Store store = Data_Store.getInstance();
    private User user;

    // HEADER
    @FXML private Label welcomeLabel;
    @FXML private TabPane mainTabs;
    @FXML private Tab requestTab;

    // SEARCH TAB
    @FXML private ComboBox<String> searchByBox;
    @FXML private ComboBox<String> sortByBox;
    @FXML private Button sortOrderButton;
    @FXML private TextField keywordField;
    @FXML private CheckBox onlyAvailableBox;
    @FXML private Label searchMessageLabel;
    @FXML private TableView<Equipment> resultsTable;
    @FXML private TableColumn<Equipment, String> colId, colName, colCategory, colBrand,
            colLocation, colAvail, colCondition, colStatus;

    // AVAILABILITY TAB
    @FXML private TextField availNameField;
    @FXML private Label availMessageLabel;
    @FXML private TableView<Equipment> availabilityTable;
    @FXML private TableColumn<Equipment, String> availColId;
    @FXML private TableColumn<Equipment, String> availColName;
    @FXML private TableColumn<Equipment, String> availColBrand;
    @FXML private TableColumn<Equipment, String> availColTotal;
    @FXML private TableColumn<Equipment, String> availColAvailable;
    @FXML private TableColumn<Equipment, String> availColBorrowed;
    @FXML private TableColumn<Equipment, String> availColCondition;
    @FXML private TableColumn<Equipment, String> availColLocation;
    @FXML private TableColumn<Equipment, String> availColStatus;

    // REQUEST TAB
    @FXML private VBox requestItemsBox;
    @FXML private TextField reqPurposeField;
    @FXML private TextField reqActivityField;
    @FXML private DatePicker returnDatePicker;
    @FXML private Label requestMessageLabel;

    // MY REQUESTS TAB
    @FXML private TableView<RequestSummary> requestsTable;
    @FXML private TableColumn<RequestSummary, String> colTxn, colReqItem, colReqQty,
            colReqDate, colReqReturn, colReqStatus, colReqRemarks;

    // RECENT SEARCHES
    @FXML private ListView<String> recentList;


    // SETUP
    @FXML
    public void initialize() {
        searchByBox.setItems(FXCollections.observableArrayList("ID", "Name", "Category", "Brand", "Location"));
        searchByBox.getSelectionModel().select("Name");
        sortByBox.setItems(FXCollections.observableArrayList(
                "Name", "ID", "Category", "Brand", "Location",
                "Available", "Condition", "Status"));
        sortByBox.getSelectionModel().select("Name");
        sortByBox.setOnAction(event -> handleSortResults());
        sortOrderButton.setText("Ascending");
        addRequestItem(null);

        // search results table
        colId.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getId()));
        colName.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getName()));
        colCategory.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCategory()));
        colBrand.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getBrand()));
        colLocation.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getLocation()));
        colAvail.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getAvailableQty() + " / " + c.getValue().getTotalQty()));
        colCondition.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCondition()));
        colStatus.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStatus()));
        colId.setComparator(Borrower_controller::compareNatural);
        colName.setComparator(String.CASE_INSENSITIVE_ORDER);
        colCategory.setComparator(String.CASE_INSENSITIVE_ORDER);
        colBrand.setComparator(String.CASE_INSENSITIVE_ORDER);
        colLocation.setComparator(String.CASE_INSENSITIVE_ORDER);
        colAvail.setComparator((a, b) -> Integer.compare(parseAvailableQty(a), parseAvailableQty(b)));
        resultsTable.setPlaceholder(new Label("Run a search to see equipment here."));

        // availability table
        availColId.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getId()));

        availColName.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getName()));

        availColBrand.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getBrand()));

        availColTotal.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.valueOf(c.getValue().getTotalQty())));

        availColAvailable.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.valueOf(c.getValue().getAvailableQty())));

        availColBorrowed.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(String.valueOf(c.getValue().getBorrowedQty())));

        availColCondition.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getCondition()));

        availColLocation.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getLocation()));

        availColStatus.setCellValueFactory(c ->
                new ReadOnlyStringWrapper(c.getValue().getStatus()));

        availabilityTable.setPlaceholder(
                new Label("Search for an equipment name to check availability.")
        );

        // my requests table
        colTxn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().borrower));
        colReqItem.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().borrowerId));
        colReqQty.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().date));
        colReqDate.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().status));
        colReqReturn.setVisible(false);
        colReqRemarks.setVisible(false);
        requestsTable.setPlaceholder(new Label("You have no requests yet."));
        requestsTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && requestsTable.getSelectionModel().getSelectedItem() != null)
                showRequestDetails(requestsTable.getSelectionModel().getSelectedItem());
        });

        if (recentList == null) recentList = new ListView<>();
        recentList.setPlaceholder(new Label("No recent searches."));

        // return date must be after today (same rule as the console version)
        returnDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date == null || !date.isAfter(LocalDate.now()));
            }
        });

        // refresh the lists whenever a tab is opened
        mainTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            refreshMyRequests();
            refreshRecentSearches();
        });
    }

    public void setUser(User user) {
        this.user = user;
        welcomeLabel.setText("Logged in as " + user.getFullName() + " (" + user.getUserID() + ")");
        refreshMyRequests();
        refreshRecentSearches();
    }


    // 1. SEARCH ITEMS (linear search + filters)
    @FXML
    private void handleSearch() {
        String mode = searchByBox.getValue();
        if (mode == null) {
            setMsg(searchMessageLabel, "Choose what to search by.", false);
            return;
        }

        String kw = keywordField.getText().trim();
        if (kw.isEmpty()) {
            setMsg(searchMessageLabel, "Search keyword cannot be empty.", false);
            return;
        }
        kw = mode.equals("ID") ? kw.toUpperCase() : kw.toLowerCase();

        boolean onlyAvail = onlyAvailableBox.isSelected();

        store.getRecentSearches(user.getUserID()).push(mode + ": " + kw);

        List<Equipment> results = new ArrayList<>();
        if (mode.equals("ID")) {
            Equipment e = store.getInventory().get(kw);
            if (e != null) results.add(e);
        } else {
            for (Equipment item : store.getInventory().values()) {
                String field;
                switch (mode) {
                    case "Name":     field = item.getName(); break;
                    case "Category": field = item.getCategory(); break;
                    case "Brand":    field = item.getBrand(); break;
                    default:         field = item.getLocation(); break;
                }
                if (field.toLowerCase().contains(kw)) results.add(item);
            }
        }

        results.removeIf(e -> onlyAvail && e.getAvailableQty() == 0);
        results.sort(comparatorForSortOption(sortByBox.getValue()));
        if ("Descending".equals(sortOrderButton.getText())) {
            results.sort(comparatorForSortOption(sortByBox.getValue()).reversed());
        }

        resultsTable.setItems(FXCollections.observableArrayList(results));
        if (results.isEmpty()) {
            setMsg(searchMessageLabel, "No matching equipment found.", false);
        } else {
            setMsg(searchMessageLabel, results.size() + " item(s) found.", true);
        }
        refreshRecentSearches();
    }

    @FXML
    private void handleRecentSearches() {
        refreshRecentSearches();
        Button clear = new Button("Clear History");
        clear.setOnAction(event -> {
            store.getRecentSearches(user.getUserID()).clear();
            refreshRecentSearches();
        });
        VBox content = new VBox(10, recentList, clear);
        VBox.setVgrow(recentList, Priority.ALWAYS);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Recent Searches");
        dialog.setHeaderText("Recent searches (newest first)");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(420, 360);
        dialog.showAndWait();
    }

    @FXML
    private void handleRequestSelected() {
        Equipment selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setMsg(searchMessageLabel, "Select an item in the table first.", false);
            return;
        }
        if (requestItems.isEmpty()) addRequestItem(null);
        requestItems.get(0).idField.setText(selected.getId());
        mainTabs.getSelectionModel().select(requestTab);
    }


    // 2. CHECK AVAILABILITY
    @FXML
    private void handleCheckAvailability() {

        String query = availNameField.getText().trim();

        if (query.isEmpty()) {
            availabilityTable.getItems().clear();
            setMsg(availMessageLabel, "Please enter an equipment name.", false);
            return;
        }

        List<Equipment> results = new ArrayList<>();

        // Search for all matching equipment names
        for (Equipment item : store.getInventory().values()) {

            if (item.getName().toLowerCase().contains(query.toLowerCase())) {
                results.add(item);
            }
        }

        // Sort by name, then brand
        results.sort(
                Comparator.comparing(Equipment::getName)
                        .thenComparing(Equipment::getBrand)
        );

        // Display all matching records
        availabilityTable.setItems(
                FXCollections.observableArrayList(results)
        );

        if (results.isEmpty()) {
            setMsg(availMessageLabel, "Equipment name not found.", false);
        } else {
            setMsg(availMessageLabel,
                    results.size() + " matching equipment record(s) found.",
                    true);
        }
    }

    // 3. REQUEST FOR ITEM
    @FXML
    private void handleSubmitRequest() {
        List<RequestItem> items = new ArrayList<>();
        for (RequestItem item : requestItems) {
            String id = item.idField.getText().trim().toUpperCase();
            Equipment e = store.getInventory().get(id);
            if (e == null) {
                setMsg(requestMessageLabel, "Equipment ID not found: " + id, false);
                return;
            }
            int qty;
            try {
                qty = Integer.parseInt(item.qtyField.getText().trim());
            } catch (NumberFormatException ex) {
                qty = -1;
            }
            if (qty <= 0) {
                setMsg(requestMessageLabel, "Each quantity must be at least 1.", false);
                return;
            }
            if (e.getStatus().equals("Damaged") || e.getStatus().equals("Missing")
                    || e.getStatus().equals("Under Maintenance")) {
                setMsg(requestMessageLabel, "This item is currently " + e.getStatus()
                        + " and cannot be requested: " + id, false);
                return;
            }
            items.add(new RequestItem(item.idField, item.qtyField, e, qty));
        }

        String purpose = reqPurposeField.getText().trim();
        String activity = reqActivityField.getText().trim();
        if (purpose.isEmpty() || activity.isEmpty()) {
            setMsg(requestMessageLabel, "Purpose and activity are required.", false);
            return;
        }

        LocalDate ret = returnDatePicker.getValue();
        if (ret == null) {
            setMsg(requestMessageLabel, "Please pick an expected return date.", false);
            return;
        }
        if (!ret.isAfter(LocalDate.now())) {
            setMsg(requestMessageLabel, "Return date must be after today.", false);
            return;
        }

        List<RequestItem> shortItems = items.stream()
                .filter(item -> item.qty > item.equipment.getAvailableQty())
                .toList();
        boolean reserveShortItems = false;
        if (!shortItems.isEmpty()) {
            StringBuilder message = new StringBuilder("These items exceed available stock:\n");
            for (RequestItem item : shortItems) {
                message.append(item.equipment.getId()).append(" (requested ")
                        .append(item.qty).append(", available ")
                        .append(item.equipment.getAvailableQty()).append(")\n");
            }
            message.append("\nJoin the reservation queue for these items?");
            Alert ask = new Alert(Alert.AlertType.CONFIRMATION,
                    message.toString(),
                    ButtonType.YES, ButtonType.NO);
            ask.setHeaderText("Not enough stock");
            ask.initOwner(mainTabs.getScene().getWindow());
            Optional<ButtonType> answer = ask.showAndWait();
            if (answer.isEmpty() || answer.get() != ButtonType.YES) {
                setMsg(requestMessageLabel, "Request cancelled.", false);
                return;
            }
            reserveShortItems = true;
        }

        String requestId = "REQ-" + System.currentTimeMillis();
        int submitted = 0;
        for (RequestItem item : items) {
            boolean reserved = reserveShortItems && shortItems.contains(item);
            BorrowRequest r = new BorrowRequest(requestId, user.getUserID(), item.equipment.getId(), item.qty,
                    purpose, activity, ret, reserved
                    ? BorrowRequest.Status.RESERVED
                    : BorrowRequest.Status.PENDING_STAFF_REVIEW);
            store.addRequest(r);
            submitted++;
        }
        setMsg(requestMessageLabel, submitted + " request(s) submitted.", true);
        clearRequestForm();
        refreshMyRequests();
    }

    @FXML
    private void handleClearRequestForm() {
        clearRequestForm();
        requestMessageLabel.setText("");
    }

    // LOGOUT (menu option 0)
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/login/Login_View.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Electronics Inventory System");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // HELPERS
    @FXML
    private void handleSortOrder() {
        sortOrderButton.setText("Ascending".equals(sortOrderButton.getText())
                ? "Descending"
                : "Ascending");
        handleSortResults();
    }

    private void handleSortResults() {
        if (sortByBox == null || sortByBox.getValue() == null
                || resultsTable == null || resultsTable.getItems().isEmpty()) return;

        List<Equipment> sorted = new ArrayList<>(resultsTable.getItems());
        Comparator<Equipment> comparator = comparatorForSortOption(sortByBox.getValue());
        if ("Descending".equals(sortOrderButton.getText())) {
            comparator = comparator.reversed();
        }
        sorted.sort(comparator);
        resultsTable.setItems(FXCollections.observableArrayList(sorted));
    }

    private static Comparator<Equipment> comparatorForSortOption(String sortBy) {
        Comparator<Equipment> comparator;
        switch (sortBy) {
            case "ID":
                comparator = Comparator.comparing(Equipment::getId, Borrower_controller::compareNatural);
                break;
            case "Category":
                comparator = Comparator.comparing(Equipment::getCategory, String.CASE_INSENSITIVE_ORDER);
                break;
            case "Brand":
                comparator = Comparator.comparing(Equipment::getBrand, String.CASE_INSENSITIVE_ORDER);
                break;
            case "Location":
                comparator = Comparator.comparing(Equipment::getLocation, String.CASE_INSENSITIVE_ORDER);
                break;
            case "Available":
                comparator = Comparator.comparingInt(Equipment::getAvailableQty);
                break;
            case "Condition":
                comparator = Comparator.comparing(Equipment::getCondition, String.CASE_INSENSITIVE_ORDER);
                break;
            case "Status":
                comparator = Comparator.comparing(Equipment::getStatus, String.CASE_INSENSITIVE_ORDER);
                break;
            default:
                comparator = Comparator.comparing(Equipment::getName, String.CASE_INSENSITIVE_ORDER);
                break;
        }
        return comparator
                .thenComparing(Equipment::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Equipment::getId, Borrower_controller::compareNatural);
    }

    private static int compareNatural(String left, String right) {
        String leftPrefix = left.replaceAll("\\d+$", "");
        String rightPrefix = right.replaceAll("\\d+$", "");
        int prefixComparison = leftPrefix.compareToIgnoreCase(rightPrefix);
        if (prefixComparison != 0) return prefixComparison;

        java.util.regex.Matcher leftMatch =
                java.util.regex.Pattern.compile("(\\d+$)").matcher(left);
        java.util.regex.Matcher rightMatch =
                java.util.regex.Pattern.compile("(\\d+$)").matcher(right);
        int leftNumber = leftMatch.find() ? Integer.parseInt(leftMatch.group(1)) : 0;
        int rightNumber = rightMatch.find() ? Integer.parseInt(rightMatch.group(1)) : 0;
        int numberComparison = Integer.compare(leftNumber, rightNumber);
        return numberComparison != 0 ? numberComparison : left.compareToIgnoreCase(right);
    }

    private static int parseAvailableQty(String value) {
        try {
            return Integer.parseInt(value.substring(0, value.indexOf('/')).trim());
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private void clearRequestForm() {
        requestItems.clear();
        requestItemsBox.getChildren().clear();
        addRequestItem(null);
        reqPurposeField.clear();
        reqActivityField.clear();
        returnDatePicker.setValue(null);
    }

    @FXML
    private void handleAddRequestItem() {
        addRequestItem(null);
    }

    private final List<RequestItem> requestItems = new ArrayList<>();

    private void addRequestItem(String equipmentId) {
        TextField idField = new TextField(equipmentId == null ? "" : equipmentId);
        idField.setPromptText("Equipment ID");
        TextField qtyField = new TextField();
        qtyField.setPromptText("Quantity");
        Button remove = new Button("Remove");
        RequestItem item = new RequestItem(idField, qtyField, null, 0);
        remove.setOnAction(event -> {
            if (requestItems.size() == 1) return;
            requestItems.remove(item);
            requestItemsBox.getChildren().remove(remove.getParent());
        });
        HBox row = new HBox(8, idField, qtyField, remove);
        requestItems.add(item);
        requestItemsBox.getChildren().add(row);
    }

    private static final class RequestItem {
        private final TextField idField;
        private final TextField qtyField;
        private final Equipment equipment;
        private final int qty;

        private RequestItem(TextField idField, TextField qtyField, Equipment equipment, int qty) {
            this.idField = idField;
            this.qtyField = qtyField;
            this.equipment = equipment;
            this.qty = qty;
        }
    }

    // 4. Status of requests: only this borrower's requests.
    private void refreshMyRequests() {
        if (user == null) return;
        ObservableList<RequestSummary> mine = FXCollections.observableArrayList();
        java.util.Map<String, List<BorrowRequest>> grouped = new java.util.LinkedHashMap<>();
        for (BorrowRequest r : store.getAllRequests()) {
            if (r.getBorrowerId().equals(user.getUserID()))
                grouped.computeIfAbsent(r.getRequestId(), key -> new ArrayList<>()).add(r);
        }
        grouped.values().forEach(requests -> mine.add(new RequestSummary(user, requests)));
        requestsTable.setItems(mine);
    }

    private void showRequestDetails(RequestSummary summary) {
        TableView<BorrowRequest> details = new TableView<>(FXCollections.observableArrayList(summary.requests));
        TableColumn<BorrowRequest, String> item = new TableColumn<>("Item");
        item.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getItemId()));
        TableColumn<BorrowRequest, String> qty = new TableColumn<>("Quantity");
        qty.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().getQty())));
        TableColumn<BorrowRequest, String> ret = new TableColumn<>("Return Date");
        ret.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getExpectedReturn().toString()));
        TableColumn<BorrowRequest, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStatus().toString()));
        details.getColumns().addAll(item, qty, ret, status);
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Request Details");
        dialog.setHeaderText(summary.borrower + " (" + summary.borrowerId + ") - "
                + summary.date + " - " + summary.status);
        dialog.getDialogPane().setContent(details);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(560, 340);
        dialog.showAndWait();
    }

    private static final class RequestSummary {
        private final String borrower, borrowerId, date, status;
        private final List<BorrowRequest> requests;

        private RequestSummary(User user, List<BorrowRequest> requests) {
            this.borrower = user.getFullName();
            this.borrowerId = user.getUserID();
            this.date = requests.get(0).getRequestDate().toString();
            this.status = requests.get(0).getStatus().toString();
            this.requests = requests;
        }
    }

    // 5. Recent searches
    private void refreshRecentSearches() {
        if (user == null) return;
        recentList.setItems(FXCollections.observableArrayList(store.getRecentSearches(user.getUserID())));
    }

    private void setMsg(Label label, String text, boolean ok) {
        label.getStyleClass().removeAll("msg-ok", "msg-error");
        label.getStyleClass().add(ok ? "msg-ok" : "msg-error");
        label.setText(text);
    }
}
