package org.example.lbycpds_project.models;

import org.example.lbycpds_project.models.BorrowRequest;
import org.example.lbycpds_project.models.Equipment;

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

/**
 * Borrower (Student / Faculty) portal in JavaFX, styled after the DLSU student portal.
 * Use it as the root of a Scene:  new Scene(new BorrowerPortal(...), 1200, 720)
 *
 * Data structures: HashMap (inventory by ID), Stack (recent searches),
 *                  Queue (staffQueue for pending requests, reservationQueue).
 */
public class BorrowerPortal extends BorderPane {
    // ---- theme ----
    private static final String GREEN = "#006738", SELECT = "#D0E6D9", BG = "#F8F9FC",
            GRAY = "#6E6E6E", LINE = "#E1E1E1";

    // ---- page names ----
    private static final String DASH = "Student Dashboard", SEARCH = "Search Items",
            AVAIL = "Check Availability", REQUEST = "Request for Item",
            STATUS = "Status of Requests", RECENT = "Recent Searches";

    // ---- shared data ----
    private final String userId, userName;
    private final Map<String, Equipment> inventory;
    private final Queue<BorrowRequest> staffQueue, reservationQueue;
    private final List<BorrowRequest> allRequests;
    private final Deque<String> recentSearches = new ArrayDeque<>();   // used as a Stack
    private final Runnable onLogout;

    // ---- UI plumbing ----
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final Map<String, Node> pages = new HashMap<>();
    private final Map<String, Runnable> refreshers = new HashMap<>();
    private final TextField reqIdField = new TextField();
    private final Label statPending = new Label("0"), statReserved = new Label("0");

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
        pages.put(RECENT, buildRecent());

        showPage(DASH);
    }

    // =====================================================================
    //  Frame pieces
    // =====================================================================
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
        for (String name : new String[]{DASH, SEARCH, AVAIL, REQUEST, STATUS, RECENT}) {
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

    // =====================================================================
    //  Pages
    // =====================================================================
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
            for (int i = mine.size() - 1, n = 0; i >= 0 && n < 5; i--, n++) {   // newest 5
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

        HBox row = new HBox(10, new Label("Search by:"), by, new Label("Keyword:"), kw, onlyAvail, go);
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
            if (mode.equals("ID") && !k.isEmpty()) {                 // HashMap lookup, O(1) average
                Equipment eq = inventory.get(k.toUpperCase());
                if (eq != null) res.add(eq);
            } else {                                                 // linear search, O(n)
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
            res.sort(Comparator.comparing(Equipment::getName));      // replace with your merge sort later

            if (!k.isEmpty() && !k.equals(recentSearches.peek())) recentSearches.push(k);  // Stack push

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
            reqIdField.setText(sel.get(0));
            showPage(REQUEST);
        });
        refreshers.put(SEARCH, () -> { if (t.getItems().isEmpty()) doSearch.run(); });  // list everything at first

        return page(card("Search Equipment", row), results);
    }

    private Node buildAvailability() {
        TextField name = new TextField();
        name.setPromptText("e.g. Multimeter");
        Button check = primary("Check");
        Label msg = new Label();
        msg.setStyle("-fx-text-fill:#DC3545;");
        HBox row = new HBox(10, new Label("Equipment name:"), name, check, msg);
        row.setAlignment(Pos.CENTER_LEFT);

        Label total = new Label("-"), avail = new Label("-"), borrowed = new Label("-");
        HBox stats = new HBox(12,
                statCard(total, "Total Quantity", GREEN),
                statCard(avail, "Quantity Available", "#6E9F2F"),
                statCard(borrowed, "Currently Borrowed", "#FF8C00"));

        GridPane details = new GridPane();
        details.setHgap(24);
        details.setVgap(8);
        details.add(new Label("Enter an equipment name to see its details."), 0, 0);
        VBox detailCard = card("Equipment Details", details);
        VBox.setVgrow(detailCard, Priority.ALWAYS);

        Runnable doCheck = () -> {
            details.getChildren().clear();
            String query = name.getText().trim();
            Equipment eq = query.isEmpty() ? null : inventory.values().stream()
                    .filter(item -> item.getName().equalsIgnoreCase(query))
                    .findFirst()
                    .orElseGet(() -> inventory.values().stream()
                            .filter(item -> item.getName().toLowerCase(Locale.ROOT)
                                    .contains(query.toLowerCase(Locale.ROOT)))
                            .findFirst()
                            .orElse(null));
            if (eq == null) {
                msg.setText("Equipment name not found.");
                total.setText("-"); avail.setText("-"); borrowed.setText("-");
                return;
            }
            msg.setText("");
            total.setText(String.valueOf(eq.getTotalQty()));
            avail.setText(String.valueOf(eq.getAvailableQty()));
            borrowed.setText(String.valueOf(eq.getBorrowedQty()));
            String[][] rows = {{"Name", eq.getName()}, {"Brand / Model", eq.getBrand()},
                    {"Category", eq.getCategory()},
                    {"Storage Location", eq.getLocation()}, {"Condition", eq.getCondition()},
                    {"Status", eq.getStatus()}};
            for (int i = 0; i < rows.length; i++) {
                Label key = new Label(rows[i][0]);
                key.setStyle("-fx-font-weight:bold;");
                details.add(key, 0, i);
                details.add(new Label(rows[i][1]), 1, i);
            }
        };
        check.setOnAction(e -> doCheck.run());
        name.setOnAction(e -> doCheck.run());

        return page(card("Check Availability", row), stats, detailCard);
    }

    private Node buildRequest() {
        Spinner<Integer> qty = new Spinner<>(1, 10000, 1);
        qty.setEditable(true);
        TextField purpose = new TextField();
        TextField activity = new TextField();
        DatePicker ret = new DatePicker(LocalDate.now().plusDays(3));
        ret.setEditable(false);
        ret.setDayCellFactory(p -> new DateCell() {                   // block today and past dates
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
        addRow(form, 0, "Equipment ID", reqIdField);
        addRow(form, 1, "Quantity", qty);
        addRow(form, 2, "Purpose", purpose);
        addRow(form, 3, "Subject / Lab Activity", activity);
        addRow(form, 4, "Expected Return Date", ret);

        Button submit = primary("Submit Request");
        Button clear = new Button("Clear");
        HBox btns = new HBox(8, submit, clear);
        form.add(btns, 1, 5);

        Runnable clearForm = () -> {
            reqIdField.clear(); purpose.clear(); activity.clear();
            qty.getValueFactory().setValue(1);
            ret.setValue(LocalDate.now().plusDays(3));
        };
        clear.setOnAction(e -> clearForm.run());

        submit.setOnAction(e -> {
            Equipment eq = inventory.get(reqIdField.getText().trim().toUpperCase());
            if (eq == null) { warn("Equipment ID not found."); return; }
            String st = eq.getStatus();
            if (st.equals("Damaged") || st.equals("Missing") || st.equals("Under Maintenance")) {
                warn("This item is currently " + st + " and cannot be requested."); return;
            }
            try { qty.increment(0); } catch (Exception ignored) { }   // commits typed text
            int q = qty.getValue() == null ? 0 : qty.getValue();
            if (q <= 0) { warn("Quantity must be at least 1."); return; }
            String p = purpose.getText().trim(), a = activity.getText().trim();
            if (p.isEmpty() || a.isEmpty()) { warn("Purpose and subject/activity are required."); return; }
            LocalDate d = ret.getValue();
            if (d == null) { warn("Please choose an expected return date."); return; }
            if (!d.isAfter(LocalDate.now())) { warn("Return date must be after today."); return; }

            if (q > eq.getAvailableQty()) {
                Alert ask = new Alert(Alert.AlertType.CONFIRMATION,
                        "Requested quantity (" + q + ") exceeds available (" + eq.getAvailableQty() + ").\n"
                                + "Join the reservation queue instead?", ButtonType.YES, ButtonType.NO);
                ask.setHeaderText("Not enough stock");
                if (ask.showAndWait().filter(b -> b == ButtonType.YES).isPresent()) {
                    BorrowRequest r = new BorrowRequest(userId, eq.getId(), q, p, a, d, BorrowRequest.Status.RESERVED);
                    reservationQueue.offer(r);                       // Queue enqueue, O(1)
                    allRequests.add(r);
                    info("Reserved!\nTransaction no.: " + r.getTxnNo()
                            + "\nQueue position: " + reservationQueue.size());
                    clearForm.run();
                    showPage(STATUS);
                }
                return;
            }
            BorrowRequest r = new BorrowRequest(userId, eq.getId(), q, p, a, d,
                    BorrowRequest.Status.PENDING_STAFF_REVIEW);
            staffQueue.offer(r);                                     // Queue enqueue, O(1)
            allRequests.add(r);
            info("Request submitted!\nTransaction no.: " + r.getTxnNo() + "\nStatus: Pending Staff Review");
            clearForm.run();
            showPage(STATUS);
        });

        return page(card("Request for Item", form));
    }

    private Node buildStatus() {
        TableView<List<String>> t = table("Transaction No.", "Equipment", "Qty", "Requested",
                "Return By", "Status", "Remarks");
        VBox c = card("Status of Requests", t);
        VBox.setVgrow(c, Priority.ALWAYS);
        refreshers.put(STATUS, () -> {
            List<List<String>> rows = new ArrayList<>();
            for (BorrowRequest r : myRequests())
                rows.add(Arrays.asList(r.getTxnNo(), itemName(r.getItemId()), String.valueOf(r.getQty()),
                        r.getRequestDate().toString(), r.getExpectedReturn().toString(),
                        pretty(r.getStatus()), r.getRemarks()));
            t.getItems().setAll(rows);
        });
        return page(c);
    }

    private Node buildRecent() {
        ListView<String> list = new ListView<>();
        list.setPlaceholder(new Label("No recent searches."));
        Button clear = new Button("Clear History");
        clear.setOnAction(e -> { recentSearches.clear(); list.getItems().clear(); });
        HBox south = new HBox(clear);
        south.setAlignment(Pos.CENTER_RIGHT);
        VBox c = card("Recent Searches (most recent first)", list, south);
        VBox.setVgrow(c, Priority.ALWAYS);
        refreshers.put(RECENT, () -> list.getItems().setAll(recentSearches));  // iterates top of stack -> bottom
        return page(c);
    }

    // =====================================================================
    //  Helpers
    // =====================================================================
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