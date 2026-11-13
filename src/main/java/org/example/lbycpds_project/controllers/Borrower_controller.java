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

/**
 * JavaFX version of the console BorrowerMenu.
 * Menu 1 -> Search tab, 2 -> Availability tab, 3 -> Request tab,
 * 4 -> My Requests tab, 5 -> Recent Searches tab, 0 -> Logout button.
 */
public class Borrower_controller {

    private final Data_Store store = Data_Store.getInstance();
    private User user;

    // HEADER
    @FXML private Label welcomeLabel;
    @FXML private TabPane mainTabs;
    @FXML private Tab requestTab;

    // SEARCH TAB
    @FXML private ComboBox<String> searchByBox;
    @FXML private TextField keywordField;
    @FXML private CheckBox onlyAvailableBox;
    @FXML private Label searchMessageLabel;
    @FXML private TableView<Equipment> resultsTable;
    @FXML private TableColumn<Equipment, String> colId, colName, colCategory, colBrand,
            colLocation, colAvail, colCondition, colStatus;

    // AVAILABILITY TAB
    @FXML private TextField availNameField;
    @FXML private Label availMessageLabel;
    @FXML private GridPane availDetails;
    @FXML private Label availItemLabel, availTotalLabel, availAvailableLabel,
            availBorrowedLabel, availConditionLabel, availLocationLabel;

    // REQUEST TAB
    @FXML private TextField reqIdField;
    @FXML private TextField reqQtyField;
    @FXML private TextField reqPurposeField;
    @FXML private TextField reqActivityField;
    @FXML private DatePicker returnDatePicker;
    @FXML private Label requestMessageLabel;

    // MY REQUESTS TAB
    @FXML private TableView<BorrowRequest> requestsTable;
    @FXML private TableColumn<BorrowRequest, String> colTxn, colReqItem, colReqQty,
            colReqDate, colReqReturn, colReqStatus, colReqRemarks;

    // RECENT SEARCHES TAB
    @FXML private ListView<String> recentList;


    // SETUP
    @FXML
    public void initialize() {
        searchByBox.setItems(FXCollections.observableArrayList("ID", "Name", "Category", "Brand", "Location"));
        searchByBox.getSelectionModel().select("Name");

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
        resultsTable.setPlaceholder(new Label("Run a search to see equipment here."));

        // my requests table
        colTxn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getTxnNo()));
        colReqItem.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getItemId()));
        colReqQty.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().getQty())));
        colReqDate.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getRequestDate().toString()));
        colReqReturn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getExpectedReturn().toString()));
        colReqStatus.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStatus().toString()));
        colReqRemarks.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getRemarks()));
        requestsTable.setPlaceholder(new Label("You have no requests yet."));

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

        availDetails.setVisible(false);
        availDetails.setManaged(false);
    }

    /** Called by Login_controller right after a successful login. */
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
        results.sort(Comparator.comparing(Equipment::getName));

        resultsTable.setItems(FXCollections.observableArrayList(results));
        if (results.isEmpty()) {
            setMsg(searchMessageLabel, "No matching equipment found.", false);
        } else {
            setMsg(searchMessageLabel, results.size() + " item(s) found.", true);
        }
        refreshRecentSearches();
    }

    /** Convenience: pick a row in the results and jump to the Request tab with its ID filled in. */
    @FXML
    private void handleRequestSelected() {
        Equipment selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setMsg(searchMessageLabel, "Select an item in the table first.", false);
            return;
        }
        reqIdField.setText(selected.getId());
        mainTabs.getSelectionModel().select(requestTab);
    }


    // 2. CHECK AVAILABILITY
    @FXML
    private void handleCheckAvailability() {
        String query = availNameField.getText().trim();
        Equipment e = query.isEmpty() ? null : store.getInventory().values().stream()
                .filter(item -> item.getName().equalsIgnoreCase(query))
                .findFirst()
                .orElseGet(() -> store.getInventory().values().stream()
                        .filter(item -> item.getName().toLowerCase().contains(query.toLowerCase()))
                        .findFirst()
                        .orElse(null));
        if (e == null) {
            availDetails.setVisible(false);
            availDetails.setManaged(false);
            setMsg(availMessageLabel, "Equipment name not found.", false);
            return;
        }

        availMessageLabel.setText("");
        availItemLabel.setText(e.getName() + " (" + e.getBrand() + ")");
        availTotalLabel.setText(String.valueOf(e.getTotalQty()));
        availAvailableLabel.setText(String.valueOf(e.getAvailableQty()));
        availBorrowedLabel.setText(String.valueOf(e.getBorrowedQty()));
        availConditionLabel.setText(e.getCondition() + "  |  Status: " + e.getStatus());
        availLocationLabel.setText(e.getLocation());
        availDetails.setVisible(true);
        availDetails.setManaged(true);
    }


    // 3. REQUEST FOR ITEM
    @FXML
    private void handleSubmitRequest() {
        Equipment e = store.getInventory().get(reqIdField.getText().trim().toUpperCase());
        if (e == null) {
            setMsg(requestMessageLabel, "Equipment ID not found.", false);
            return;
        }
        if (e.getStatus().equals("Damaged") || e.getStatus().equals("Missing")
                || e.getStatus().equals("Under Maintenance")) {
            setMsg(requestMessageLabel, "This item is currently " + e.getStatus() + " and cannot be requested.", false);
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(reqQtyField.getText().trim());
        } catch (NumberFormatException ex) {
            qty = -1;
        }
        if (qty <= 0) {
            setMsg(requestMessageLabel, "Quantity must be at least 1.", false);
            return;
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

        // not enough stock -> offer the reservation queue
        if (qty > e.getAvailableQty()) {
            Alert ask = new Alert(Alert.AlertType.CONFIRMATION,
                    "Requested quantity (" + qty + ") exceeds available (" + e.getAvailableQty() + ").\n"
                            + "Join the reservation queue instead?",
                    ButtonType.YES, ButtonType.NO);
            ask.setHeaderText("Not enough stock");
            ask.initOwner(mainTabs.getScene().getWindow());
            Optional<ButtonType> answer = ask.showAndWait();

            if (answer.isPresent() && answer.get() == ButtonType.YES) {
                BorrowRequest r = new BorrowRequest(user.getUserID(), e.getId(), qty, purpose, activity,
                        ret, BorrowRequest.Status.RESERVED);
                store.getReservationQueue().offer(r);
                store.getAllRequests().add(r);
                setMsg(requestMessageLabel, "Reserved. Transaction no.: " + r.getTxnNo()
                        + " (position " + store.getReservationQueue().size() + " in queue)", true);
                clearRequestForm();
                refreshMyRequests();
            } else {
                setMsg(requestMessageLabel, "Request cancelled.", false);
            }
            return;
        }

        BorrowRequest r = new BorrowRequest(user.getUserID(), e.getId(), qty, purpose, activity, ret,
                BorrowRequest.Status.PENDING_STAFF_REVIEW);
        store.getStaffQueue().offer(r);
        store.getAllRequests().add(r);
        setMsg(requestMessageLabel, "Request submitted! Transaction no.: " + r.getTxnNo()
                + " (Pending Staff Review)", true);
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
    private void clearRequestForm() {
        reqIdField.clear();
        reqQtyField.clear();
        reqPurposeField.clear();
        reqActivityField.clear();
        returnDatePicker.setValue(null);
    }

    /** 4. Status of requests: only this borrower's requests. */
    private void refreshMyRequests() {
        if (user == null) return;
        ObservableList<BorrowRequest> mine = FXCollections.observableArrayList();
        for (BorrowRequest r : store.getAllRequests()) {
            if (r.getBorrowerId().equals(user.getUserID())) mine.add(r);
        }
        requestsTable.setItems(mine);
    }

    /** 5. Recent searches (newest first). */
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
