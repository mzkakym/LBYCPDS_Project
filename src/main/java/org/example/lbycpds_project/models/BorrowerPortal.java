package org.example.lbycpds_project.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BorrowerPortal extends BorderPane {
    // theme
    private static final String GREEN = "#006738", SELECT = "#D0E6D9", BG = "#F8F9FC",
            GRAY = "#6E6E6E", LINE = "#E1E1E1";

    // page names
    private static final String DASH = "Student Dashboard", SEARCH = "Search Items",
            AVAIL = "Check Availability", REQUEST = "Request for Item",
            STATUS = "Status of Requests";

    // shared data
    private final String userId, userName;
    private final Map<String, Equipment> inventory;
    private final Queue<BorrowRequest> staffQueue, reservationQueue;
    private final List<BorrowRequest> allRequests;
    private final Deque<String> recentSearches = new ArrayDeque<>();   // stack
    private final Runnable onLogout;

    // UI setup
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final Map<String, Node> pages = new HashMap<>();
    private final Map<String, Runnable> refreshers = new HashMap<>();
    private final Label statPending = new Label("0"), statReserved = new Label("0");
    private final List<RequestItem> requestItems = new ArrayList<>();
    private VBox requestItemsBox;

    public BorrowerPortal(String userId, String userName, Map<String, Equipment> inventory,
                          Queue<BorrowRequest> staffQueue, Queue<BorrowRequest> reservationQueue,
                          List<BorrowRequest> allRequests, Runnable onLogout) {
        this.userId = userId; this.userName = userName; this.inventory = inventory;
        this.staffQueue = staffQueue; this.reservationQueue = reservationQueue;
        this.allRequests = allRequests; this.onLogout = onLogout;

        setStyle("-fx-background-color:" + BG + ";");
        setTop(buildTopBar());
        setLeft(buildSidebar());

        pages.put(DASH, buildDashboard());
        pages.put(SEARCH, buildSearch());
        pages.put(AVAIL, buildAvailability());
        pages.put(REQUEST, buildRequest());
        pages.put(STATUS, buildStatus());

        showPage(DASH);
    }

    // frame
    private Node buildTopBar() {
        Label dlsu = new Label("DLSU");
        dlsu.setStyle("-fx-text-fill:" + GREEN + ";-fx-font-weight:bold;-fx-font-size:16;");
        Label sub = new Label("Electronics Inventory System");
        sub.setStyle("-fx-text-fill:" + GRAY + ";");
        HBox left = new HBox(10, dlsu, sub);
        left.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(userName.toUpperCase());
        name.setStyle("-fx-font-weight:bold;-fx-font-size:12;");
        Label id = new Label(userId);
        id.setStyle("-fx-font-size:12;");
        VBox right = new VBox(2, name, id);
        right.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(left, spacer, right);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(8, 20, 8, 20));
        bar.setStyle("-fx-background-color:white;-fx-border-color:" + LINE + ";-fx-border-width:0 0 1 0;");
        return bar;
    }

    private Node buildSidebar() {
        VBox nav = new VBox();
        nav.setPadding(new Insets(10, 0, 0, 0));
        for (String name : new String[]{DASH, SEARCH, AVAIL, REQUEST, STATUS}) {
            Button b = new Button(name);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setStyle(navStyle(false));
            b.setOnAction(e -> showPage(name));
            navButtons.put(name, b);
            nav.getChildren().add(b);
        }

        Label last = new Label("Last Login : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a")));
        last.setStyle("-fx-font-size:11;");
        last.setWrapText(true);
        Button out = new Button("Logout");
        out.setStyle("-fx-background-color:white;-fx-text-fill:#DC3545;-fx-font-weight:bold;-fx-cursor:hand;");
        out.setOnAction(e -> { if (onLogout != null) onLogout.run(); });
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bottom = new HBox(last, sp, out);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(10, 12, 10, 14));
        bottom.setStyle("-fx-border-color:" + LINE + ";-fx-border-width:1 0 0 0;");

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        VBox side = new VBox(nav, grow, bottom);
        side.setPrefWidth(230);
        side.setMinWidth(230);
        side.setStyle("-fx-background-color:white;-fx-border-color:" + LINE + ";-fx-border-width:0 1 0 0;");
        return side;
    }

    private String navStyle(boolean selected) {
        return "-fx-background-color:" + (selected ? SELECT : "white") + ";-fx-text-fill:#1E1E1E;"
                + "-fx-font-size:13;-fx-alignment:CENTER_LEFT;-fx-padding:11 18 11 18;"
                + "-fx-background-radius:0;-fx-cursor:hand;";
    }

    private void showPage(String name) {
        Runnable r = refreshers.get(name);
        if (r != null) r.run();
        setCenter(pages.get(name));
        navButtons.forEach((n, b) -> b.setStyle(navStyle(n.equals(name))));
    }

    // pages
    private Node buildDashboard() {
        Label small = new Label("Welcome, " + userName.toUpperCase());
        small.setStyle("-fx-text-fill:" + GRAY + ";");
        Label greet = new Label();
        greet.setStyle("-fx-font-size:22;");
        VBox welcome = new VBox(2, small, greet);
        welcome.setPadding(new Insets(14, 18, 14, 18));
        welcome.setStyle("-fx-background-color:white;");

        HBox stats = new HBox(12,
                statCard(statPending, "My Pending Requests", "#00A8FF"),
                statCard(statReserved, "My Reservations", "#FF8C00"));

        TableView<List<String>> t = table("Transaction No.", "Equipment", "Qty", "Return By", "Status");
        VBox recent = card("My Recent Requests", t);
        VBox.setVgrow(recent, Priority.ALWAYS);

        refreshers.put(DASH, () -> {
            int h = LocalTime.now().getHour();
            greet.setText(h < 12 ? "Good Morning" : h < 18 ? "Good Afternoon" : "Good Evening");
            List<BorrowRequest> mine = myRequests();
            statPending.setText(String.valueOf(mine.stream().filter(r ->
                    r.getStatus() == BorrowRequest.Status.PENDING_STAFF_REVIEW
                            || r.getStatus() == BorrowRequest.Status.FILED).count()));
            statReserved.setText(String.valueOf(mine.stream().filter(r ->
                    r.getStatus() == BorrowRequest.Status.RESERVED).count()));
            List<List<String>> rows = new ArrayList<>();
            for (int i = mine.size() - 1, n = 0; i >= 0 && n < 5; i--, n++) {
                BorrowRequest r = mine.get(i);
                rows.add(Arrays.asList(r.getTxnNo(), itemName(r.getItemId()), String.valueOf(r.getQty()),
                        r.getExpectedReturn().toString(), pretty(r.getStatus())));
            }
            t.getItems().setAll(rows);
        });
        return page(welcome, stats, recent);
    }

    private Node buildSearch() {
        ComboBox<String> by = new ComboBox<>(FXCollections.observableArrayList(
                "Name", "ID", "Category", "Brand", "Location"));
        by.setValue("Name");
        TextField kw = new TextField();
        kw.setPromptText("e.g. oscilloscope");
        kw.setPrefWidth(200);
        CheckBox onlyAvail = new CheckBox("Available only");
        Button go = primary("Search");
        Button recent = new Button("Recent Searches");
        recent.setOnAction(e -> showRecentSearches());

        HBox row = new HBox(10, new Label("Search by:"), by, new Label("Keyword:"), kw, onlyAvail, go, recent);
        row.setAlignment(Pos.CENTER_LEFT);

        TableView<List<String>> t = table("ID", "Name", "Category", "Brand", "Location",
                "Available", "Total", "Condition", "Status");
        Button req = primary("Request Selected Item");
        HBox south = new HBox(req);
        south.setAlignment(Pos.CENTER_RIGHT);
        VBox results = card("Results", t, south);
        VBox.setVgrow(results, Priority.ALWAYS);

        Runnable doSearch = () -> {
            String k = kw.getText().trim().toLowerCase();
            String mode = by.getValue();
            List<Equipment> res = new ArrayList<>();
            if (mode.equals("ID") && !k.isEmpty()) {
                Equipment eq = inventory.get(k.toUpperCase());
                if (eq != null) res.add(eq);
            } else {
                for (Equipment eq : inventory.values()) {
                    String f;
                    switch (mode) {
                        case "Name": f = eq.getName(); break;
                        case "Category": f = eq.getCategory(); break;
                        case "Brand": f = eq.getBrand(); break;
                        case "Location": f = eq.getLocation(); break;
                        default: f = eq.getId();
                    }
                    if (k.isEmpty() || f.toLowerCase().contains(k)) res.add(eq);
                }
            }
            res.removeIf(eq -> onlyAvail.isSelected() && eq.getAvailableQty() == 0);
            res.sort(Comparator.comparing(Equipment::getName));

            if (!k.isEmpty() && !k.equals(recentSearches.peek())) recentSearches.push(k);

            List<List<String>> rows = new ArrayList<>();
            for (Equipment eq : res)
                rows.add(Arrays.asList(eq.getId(), eq.getName(), eq.getCategory(), eq.getBrand(), eq.getLocation(),
                        String.valueOf(eq.getAvailableQty()), String.valueOf(eq.getTotalQty()),
                        eq.getCondition(), eq.getStatus()));
            t.getItems().setAll(rows);
            if (res.isEmpty()) info("No matching items found.");
        };
        go.setOnAction(e -> doSearch.run());
        kw.setOnAction(e -> doSearch.run());
        req.setOnAction(e -> {
            List<String> sel = t.getSelectionModel().getSelectedItem();
            if (sel == null) { info("Select an item from the table first."); return; }
            requestItems.get(0).idField.setText(sel.get(0));
            showPage(REQUEST);
        });
        refreshers.put(SEARCH, () -> { if (t.getItems().isEmpty()) doSearch.run(); });

        return page(card("Search Equipment", row), results);
    }


    private Node buildAvailability() {

        TextField name = new TextField();
        name.setPromptText("e.g. Multimeter");

        Button check = primary("Check");

        Label msg = new Label();
        msg.setStyle("-fx-text-fill:#DC3545;");

        HBox row = new HBox(10,
                new Label("Equipment name:"), name, check, msg);
        row.setAlignment(Pos.CENTER_LEFT);

        // SUMMARY STATISTICS
        Label total = new Label("-");
        Label avail = new Label("-");
        Label borrowed = new Label("-");

        HBox stats = new HBox(12,
                statCard(total, "Total Quantity", GREEN),
                statCard(avail, "Quantity Available", "#6E9F2F"),
                statCard(borrowed, "Currently Borrowed", "#FF8C00")
        );

        // EQUIPMENT DETAILS TABLE
        GridPane details = new GridPane();
        details.setHgap(18);
        details.setVgap(12);

        VBox detailCard = card("Equipment Details", details);
        VBox.setVgrow(detailCard, Priority.ALWAYS);

        // SEARCH FUNCTION
        Runnable doCheck = () -> {

            details.getChildren().clear();

            String query = name.getText().trim();

            List<Equipment> matches = new ArrayList<>();

            // Find all exact name matches first
            if (!query.isEmpty()) {

                for (Equipment item : inventory.values()) {

                    if (item.getName().equalsIgnoreCase(query)) {
                        matches.add(item);
                    }
                }

                // If no exact match, search using partial names
                if (matches.isEmpty()) {

                    for (Equipment item : inventory.values()) {

                        if (item.getName().toLowerCase(Locale.ROOT)
                                .contains(query.toLowerCase(Locale.ROOT))) {
                            matches.add(item);
                        }
                    }
                }
            }

            // No matching equipment
            if (matches.isEmpty()) {

                msg.setText("Equipment name not found.");

                total.setText("-");
                avail.setText("-");
                borrowed.setText("-");

                details.add(
                        new Label("No matching equipment found."),
                        0, 0
                );

                return;
            }

            // Sort by brand
            matches.sort(
                    Comparator.comparing(Equipment::getBrand,
                            String.CASE_INSENSITIVE_ORDER)
            );

            msg.setStyle("-fx-text-fill:#006738;");
            msg.setText(matches.size() + " brand(s) found.");

            // Calculate combined quantities
            int totalQty = 0;
            int availableQty = 0;
            int borrowedQty = 0;

            for (Equipment item : matches) {

                totalQty += item.getTotalQty();
                availableQty += item.getAvailableQty();
                borrowedQty += item.getBorrowedQty();
            }

            total.setText(String.valueOf(totalQty));
            avail.setText(String.valueOf(availableQty));
            borrowed.setText(String.valueOf(borrowedQty));

            // TABLE HEADERS
            String[] headers = {
                    "ID", "Brand / Model", "Total", "Available",
                    "Borrowed", "Condition", "Location", "Status"
            };

            for (int col = 0; col < headers.length; col++) {

                Label header = new Label(headers[col]);
                header.setStyle("-fx-font-weight:bold;");

                details.add(header, col, 0);
            }

            // DISPLAY EVERY MATCHING BRAND
            int tableRow = 1;

            for (Equipment item : matches) {

                details.add(new Label(item.getId()), 0, tableRow);
                details.add(new Label(item.getBrand()), 1, tableRow);
                details.add(new Label(String.valueOf(item.getTotalQty())), 2, tableRow);
                details.add(new Label(String.valueOf(item.getAvailableQty())), 3, tableRow);
                details.add(new Label(String.valueOf(item.getBorrowedQty())), 4, tableRow);
                details.add(new Label(item.getCondition()), 5, tableRow);
                details.add(new Label(item.getLocation()), 6, tableRow);
                details.add(new Label(item.getStatus()), 7, tableRow);

                tableRow++;
            }
        };

        check.setOnAction(e -> doCheck.run());
        name.setOnAction(e -> doCheck.run());

        return page(
                card("Check Availability", row),
                stats,
                detailCard
        );
    }


    private Node buildRequest() {
        TextField purpose = new TextField();
        TextField activity = new TextField();
        DatePicker ret = new DatePicker(LocalDate.now().plusDays(3));
        ret.setEditable(false);
        ret.setDayCellFactory(p -> new DateCell() {                   // block past dates
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(empty || !d.isAfter(LocalDate.now()));
            }
        });

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);
        form.setMaxWidth(620);
        ColumnConstraints c1 = new ColumnConstraints();
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(c1, c2);
        requestItemsBox = new VBox(8);
        addRequestItem();
        Button addItem = new Button("Add equipment");
        addItem.setOnAction(e -> addRequestItem());
        form.add(new Label("Equipment items"), 0, 0);
        form.add(requestItemsBox, 1, 0);
        form.add(addItem, 1, 1);
        addRow(form, 2, "Purpose", purpose);
        addRow(form, 3, "Subject / Lab Activity", activity);
        addRow(form, 4, "Expected Return Date", ret);

        Button submit = primary("Submit Request");
        Button clear = new Button("Clear");
        HBox btns = new HBox(8, submit, clear);
        form.add(btns, 1, 5);

        Runnable clearForm = () -> {
            requestItems.clear();
            requestItemsBox.getChildren().clear();
            addRequestItem();
            purpose.clear(); activity.clear();
            ret.setValue(LocalDate.now().plusDays(3));
        };
        clear.setOnAction(e -> clearForm.run());

        submit.setOnAction(e -> {
            List<RequestItem> items = new ArrayList<>();
            for (RequestItem item : requestItems) {
                Equipment eq = inventory.get(item.idField.getText().trim().toUpperCase());
                if (eq == null) { warn("Equipment ID not found."); return; }
                int q;
                try { q = Integer.parseInt(item.qtyField.getText().trim()); }
                catch (NumberFormatException ex) { q = 0; }
                if (q <= 0) { warn("Each quantity must be at least 1."); return; }
                String st = eq.getStatus();
                if (st.equals("Damaged") || st.equals("Missing") || st.equals("Under Maintenance")) {
                    warn("This item is currently " + st + " and cannot be requested."); return;
                }
                items.add(new RequestItem(item.idField, item.qtyField, eq, q));
            }
            String p = purpose.getText().trim(), a = activity.getText().trim();
            if (p.isEmpty() || a.isEmpty()) { warn("Purpose and subject/activity are required."); return; }
            LocalDate d = ret.getValue();
            if (d == null) { warn("Please choose an expected return date."); return; }
            if (!d.isAfter(LocalDate.now())) { warn("Return date must be after today."); return; }

            List<RequestItem> shortItems = items.stream()
                    .filter(item -> item.qty > item.equipment.getAvailableQty())
                    .toList();
            boolean reserveShortItems = false;
            if (!shortItems.isEmpty()) {
                Alert ask = new Alert(Alert.AlertType.CONFIRMATION,
                        "One or more items exceed available stock.\n"
                                + "Join the reservation queue for those items?", ButtonType.YES, ButtonType.NO);
                ask.setHeaderText("Not enough stock");
                if (ask.showAndWait().filter(b -> b == ButtonType.YES).isPresent()) {
                    reserveShortItems = true;
                } else {
                    return;
                }
            }
            String requestId = "REQ-" + System.currentTimeMillis();
            for (RequestItem item : items) {
                boolean reserved = reserveShortItems && shortItems.contains(item);
                BorrowRequest r = new BorrowRequest(requestId, userId, item.equipment.getId(), item.qty, p, a, d,
                        reserved ? BorrowRequest.Status.RESERVED : BorrowRequest.Status.PENDING_STAFF_REVIEW);
                if (reserved) reservationQueue.offer(r); else staffQueue.offer(r);
                allRequests.add(r);
            }
            saveRequests();
            info("Request submitted for " + items.size() + " equipment item(s).");
            clearForm.run();
            showPage(STATUS);
        });

        return page(card("Request for Item", form));
    }

    private void addRequestItem() {
        TextField idField = new TextField();
        idField.setPromptText("Equipment ID");
        TextField qtyField = new TextField();
        qtyField.setPromptText("Quantity");
        Button remove = new Button("Remove");
        RequestItem item = new RequestItem(idField, qtyField, null, 0);
        remove.setOnAction(e -> {
            if (requestItems.size() == 1) return;
            requestItems.remove(item);
            requestItemsBox.getChildren().remove(remove.getParent());
        });
        requestItems.add(item);
        requestItemsBox.getChildren().add(new HBox(8, idField, qtyField, remove));
    }

    private static final class RequestItem {
        private final TextField idField, qtyField;
        private final Equipment equipment;
        private final int qty;

        private RequestItem(TextField idField, TextField qtyField, Equipment equipment, int qty) {
            this.idField = idField;
            this.qtyField = qtyField;
            this.equipment = equipment;
            this.qty = qty;
        }

    }

    private void saveRequests() {
        org.example.lbycpds_project.utils.Data_Store.getInstance().saveRequests();
    }

    private Node buildStatus() {
        TableView<RequestSummary> t = new TableView<>();
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.getColumns().addAll(summaryColumn("Borrower", RequestSummary::borrower),
                summaryColumn("ID", RequestSummary::borrowerId),
                summaryColumn("Date Requested", RequestSummary::date),
                summaryColumn("Status", RequestSummary::status));
        t.setPlaceholder(new Label("No records to display."));
        t.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && t.getSelectionModel().getSelectedItem() != null)
                showRequestDetails(t.getSelectionModel().getSelectedItem());
        });
        VBox c = card("Status of Requests", t);
        VBox.setVgrow(c, Priority.ALWAYS);
        refreshers.put(STATUS, () -> {
            Map<String, List<BorrowRequest>> grouped = new LinkedHashMap<>();
            for (BorrowRequest r : myRequests())
                grouped.computeIfAbsent(r.getRequestId(), key -> new ArrayList<>()).add(r);
            List<RequestSummary> summaries = new ArrayList<>();
            grouped.forEach((id, requests) -> summaries.add(new RequestSummary(userName, userId, requests)));
            t.getItems().setAll(summaries);
        });
        return page(c);
    }

    private TableColumn<RequestSummary, String> summaryColumn(String title,
                                                               java.util.function.Function<RequestSummary, String> value) {
        TableColumn<RequestSummary, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void showRequestDetails(RequestSummary summary) {
        TableView<List<String>> items = table("Item", "Quantity", "Return Date", "Status");
        for (BorrowRequest r : summary.requests)
            items.getItems().add(Arrays.asList(itemName(r.getItemId()), String.valueOf(r.getQty()),
                    r.getExpectedReturn().toString(), pretty(r.getStatus())));
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Request Details");
        dialog.setHeaderText(summary.borrower + " (" + summary.borrowerId + ") - "
                + summary.date + " - " + summary.status);
        dialog.getDialogPane().setContent(items);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(620, 360);
        dialog.showAndWait();
    }

    private static final class RequestSummary {
        private final String borrower, borrowerId, date, status;
        private final List<BorrowRequest> requests;

        private RequestSummary(String borrower, String borrowerId, List<BorrowRequest> requests) {
            this.borrower = borrower;
            this.borrowerId = borrowerId;
            this.requests = requests;
            this.date = requests.get(0).getRequestDate().toString();
            this.status = pretty(requests.get(0).getStatus());
        }

        private String borrower() { return borrower; }
        private String borrowerId() { return borrowerId; }
        private String date() { return date; }
        private String status() { return status; }
    }

    private void showRecentSearches() {
        ListView<String> list = new ListView<>();
        list.setPlaceholder(new Label("No recent searches."));
        Button clear = new Button("Clear History");
        clear.setOnAction(e -> { recentSearches.clear(); list.getItems().clear(); });
        HBox south = new HBox(clear);
        south.setAlignment(Pos.CENTER_RIGHT);
        list.getItems().setAll(recentSearches);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Recent Searches");
        dialog.setHeaderText("Recent Searches (most recent first)");
        dialog.getDialogPane().setContent(new VBox(10, list, south));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(420, 360);
        dialog.showAndWait();
    }

    // helpers
    private List<BorrowRequest> myRequests() {
        List<BorrowRequest> mine = new ArrayList<>();
        for (BorrowRequest r : allRequests) if (r.getBorrowerId().equals(userId)) mine.add(r);
        return mine;
    }

    private String itemName(String id) {
        Equipment e = inventory.get(id);
        return e == null ? id : e.getName() + " (" + id + ")";
    }

    private static String pretty(BorrowRequest.Status s) {
        StringBuilder sb = new StringBuilder();
        for (String w : s.name().toLowerCase().split("_"))
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return sb.toString().trim();
    }

    private void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg);
        a.setHeaderText("Cannot submit request");
        a.showAndWait();
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private VBox page(Node... kids) {
        VBox p = new VBox(12, kids);
        p.setPadding(new Insets(16));
        p.setStyle("-fx-background-color:" + BG + ";");
        return p;
    }

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

    private VBox statCard(Label value, String caption, String color) {
        value.setStyle("-fx-font-weight:bold;-fx-font-size:22;");
        Label cap = new Label(caption);
        cap.setStyle("-fx-text-fill:" + GRAY + ";-fx-font-size:12;");
        VBox c = new VBox(2, value, cap);
        c.setPadding(new Insets(14, 16, 14, 16));
        c.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(c, Priority.ALWAYS);
        c.setStyle("-fx-background-color:white;-fx-border-color:" + color + " transparent transparent transparent;"
                + "-fx-border-width:3 0 0 0;");
        return c;
    }

    private TableView<List<String>> table(String... cols) {
        TableView<List<String>> t = new TableView<>();
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setPlaceholder(new Label("No records to display."));
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<List<String>, String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().get(idx)));
            t.getColumns().add(c);
        }
        return t;
    }

    private Button primary(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + GREEN + ";-fx-text-fill:white;-fx-font-weight:bold;"
                + "-fx-padding:8 18 8 18;-fx-background-radius:3;-fx-cursor:hand;");
        return b;
    }

    private void addRow(GridPane g, int row, String label, Control field) {
        Label l = new Label(label);
        field.setMaxWidth(Double.MAX_VALUE);
        g.add(l, 0, row);
        g.add(field, 1, row);
    }
}