import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class BorrowerMenu {
    private final String userId;
    private final Map<String, Equipment> inventory;
    private final Queue<BorrowRequest> staffQueue;
    private final Queue<BorrowRequest> reservationQueue;
    private final List<BorrowRequest> allRequests;
    private final Deque<String> recentSearches = new ArrayDeque<>();
    private final Scanner in;

    public BorrowerMenu(String userId, Map<String, Equipment> inventory,
                        Queue<BorrowRequest> staffQueue, Queue<BorrowRequest> reservationQueue,
                        List<BorrowRequest> allRequests, Scanner in) {
        this.userId = userId; this.inventory = inventory; this.staffQueue = staffQueue;
        this.reservationQueue = reservationQueue; this.allRequests = allRequests; this.in = in;
    }

    public void run() {
        boolean running = true;
        while (running) {
            System.out.println("BORROWER MENU");
            System.out.println("1. Search Items");
            System.out.println("2. Check Availability");
            System.out.println("3. Request for Item");
            System.out.println("4. Status of Requests");
            System.out.println("5. Recent Searches");
            System.out.println("0. Logout");
            System.out.print("Choice: ");

            switch (in.nextLine().trim()) {
                case "1": searchItems(); break;
                case "2": checkAvailability(); break;
                case "3": requestForItem(); break;
                case "4": statusOfRequests(); break;
                case "5": showRecentSearches(); break;
                case "0": running = false; System.out.println("Logging out..."); break;
                default: System.out.println("Please enter 0-5.");
            }
        }
    }

    // Searching (linear search + filters)


    private void searchItems() {
        System.out.println("\nSearch Equipment By:");
        System.out.println("1. ID");
        System.out.println("2. Name");
        System.out.println("3. Category");
        System.out.println("4. Brand");
        System.out.println("5. Location");
        System.out.print("Choice: ");

        String mode = in.nextLine().trim();
        String kw = "";
        String searchType = "";

        switch (mode) {
            case "1":
                searchType = "ID";
                System.out.print("Enter Equipment ID: ");
                kw = in.nextLine().trim().toUpperCase();
                break;

            case "2":
                searchType = "Name";
                System.out.print("Enter Equipment Name: ");
                kw = in.nextLine().trim().toLowerCase();
                break;

            case "3":
                searchType = "Category";
                System.out.print("Enter Category: ");
                kw = in.nextLine().trim().toLowerCase();
                break;

            case "4":
                searchType = "Brand";
                System.out.print("Enter Brand: ");
                kw = in.nextLine().trim().toLowerCase();
                break;

            case "5":
                searchType = "Location";
                System.out.print("Enter Location: ");
                kw = in.nextLine().trim().toLowerCase();
                break;

            default:
                System.out.println("Invalid search type.");
                return;
        }

        if (kw.isEmpty()) {
            System.out.println("Search keyword cannot be empty.");
            return;
        }

        System.out.print("\nOnly show available items? (y/n): ");
        String availChoice = in.nextLine().trim();

        if (!availChoice.equalsIgnoreCase("y") &&
                !availChoice.equalsIgnoreCase("n")) {
            System.out.println("Invalid choice.");
            return;
        }

        boolean onlyAvail = availChoice.equalsIgnoreCase("y");

        recentSearches.push(searchType + ": " + kw);

        List<Equipment> results = new ArrayList<>();

        switch (mode) {
            case "1":
                Equipment e = inventory.get(kw);

                if (e != null) {
                    results.add(e);
                }
                break;

            case "2":
            case "3":
            case "4":
            case "5":
                for (Equipment item : inventory.values()) {
                    String field = "";

                    switch (mode) {
                        case "2":
                            field = item.getName();
                            break;

                        case "3":
                            field = item.getCategory();
                            break;

                        case "4":
                            field = item.getBrand();
                            break;

                        case "5":
                            field = item.getLocation();
                            break;
                    }

                    if (field.toLowerCase().contains(kw)) {
                        results.add(item);
                    }
                }
                break;
        }

        results.removeIf(e -> onlyAvail && e.getAvailableQty() == 0);

        results.sort(Comparator.comparing(Equipment::getName));

        if (results.isEmpty()) {
            System.out.println("\nNo matching equipment found.");
            return;
        }

        System.out.println("\nSearch Results:");
        printHeader();
        results.forEach(System.out::println);
    }

    // Checking availability
    private void checkAvailability() {
        System.out.print("Enter equipment ID: ");
        Equipment e = inventory.get(in.nextLine().trim().toUpperCase());
        if (e == null) { System.out.println("Equipment ID not found."); return; }
        System.out.println("Item      : " + e.getName() + " (" + e.getBrand() + ")");
        System.out.println("Total     : " + e.getTotalQty());
        System.out.println("Available : " + e.getAvailableQty());
        System.out.println("Borrowed  : " + e.getBorrowedQty());
        System.out.println("Condition : " + e.getCondition() + " | Status: " + e.getStatus());
        System.out.println("Location  : " + e.getLocation());
    }

    // Requesting an item
    private void requestForItem() {
        System.out.print("Equipment ID: ");
        Equipment e = inventory.get(in.nextLine().trim().toUpperCase());
        if (e == null) { System.out.println("Equipment ID not found."); return; }
        if (e.getStatus().equals("Damaged") || e.getStatus().equals("Missing")
                || e.getStatus().equals("Under Maintenance")) {
            System.out.println("This item is currently " + e.getStatus() + " and cannot be requested."); return;
        }

        int qty = readInt("Quantity: ");
        if (qty <= 0) { System.out.println("Quantity must be at least 1."); return; }

        System.out.print("Purpose: ");
        String purpose = in.nextLine().trim();
        System.out.print("Subject / laboratory activity: ");
        String activity = in.nextLine().trim();
        if (purpose.isEmpty() || activity.isEmpty()) { System.out.println("Purpose and activity are required."); return; }

        System.out.print("Expected return date (YYYY-MM-DD): ");
        LocalDate ret;
        try { ret = LocalDate.parse(in.nextLine().trim()); }
        catch (DateTimeParseException ex) { System.out.println("Invalid date format."); return; }
        if (!ret.isAfter(LocalDate.now())) { System.out.println("Return date must be after today."); return; }

        if (qty > e.getAvailableQty()) {
            System.out.println("Requested quantity (" + qty + ") exceeds available (" + e.getAvailableQty() + ").");
            System.out.print("Join the reservation queue instead? (y/n): ");
            if (in.nextLine().trim().equalsIgnoreCase("y")) {
                BorrowRequest r = new BorrowRequest(userId, e.getId(), qty, purpose, activity, ret, BorrowRequest.Status.RESERVED);
                reservationQueue.offer(r);
                allRequests.add(r);
                System.out.println("Reserved. Transaction no.: " + r.getTxnNo()
                        + " (position " + reservationQueue.size() + " in queue)");
            }
            return;
        }

        BorrowRequest r = new BorrowRequest(userId, e.getId(), qty, purpose, activity, ret,
                BorrowRequest.Status.PENDING_STAFF_REVIEW);
        staffQueue.offer(r);
        allRequests.add(r);
        System.out.println("Request submitted! Transaction no.: " + r.getTxnNo() + " (Pending Staff Review)");
    }

    // Status of the requests
    private void statusOfRequests() {
        boolean any = false;
        for (BorrowRequest r : allRequests) {
            if (r.getBorrowerId().equals(userId)) { System.out.println(r); any = true; }
        }
        if (!any) System.out.println("You have no requests yet.");
    }

    // Recent searches (search history??)

    private void showRecentSearches() {
        if (recentSearches.isEmpty()) {
            System.out.println("No recent searches.");
            return;
        }

        System.out.println("\nRecent Searches:");

        int i = 1;
        for (String search : recentSearches) {
            System.out.println(i++ + ". " + search);
        }
    }


    // helpers
    private int readInt(String prompt) {
        System.out.print(prompt);
        try { return Integer.parseInt(in.nextLine().trim()); }
        catch (NumberFormatException ex) { return -1; }
    }

    private void printHeader() {
        System.out.println(String.format("%-8s %-22s %-12s %-10s %-7s %-10s %-6s %s",
                "ID", "Name", "Category", "Location", "Avail", "Condition", "Type", "Status"));
    }
}
