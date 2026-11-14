package org.example.lbycpds_project.utils;

import org.example.lbycpds_project.models.BorrowRequest;
import org.example.lbycpds_project.models.Equipment;

import java.util.*;
import java.io.*;
import java.time.LocalDate;

public final class Data_Store {
    private static final Data_Store INSTANCE = new Data_Store();
    private static final String RECORDS_DIR = "src/main/resources/records";
    private static final String EQUIPMENT_FILE = RECORDS_DIR + "/equipment.txt";
    private static final String REQUESTS_FILE = RECORDS_DIR + "/requests.txt";

    private final Map<String, Equipment> inventory = new HashMap<>();
    private final Queue<BorrowRequest> staffQueue = new LinkedList<>();
    private final Queue<BorrowRequest> reservationQueue = new LinkedList<>();
    private final List<BorrowRequest> allRequests = new ArrayList<>();
    private final Map<String, Deque<String>> recentSearches = new HashMap<>();

    private Data_Store() {
        loadEquipment();
        loadRequests();
    }

    public static Data_Store getInstance() { return INSTANCE; }

    public Map<String, Equipment> getInventory() { return inventory; }
    public Queue<BorrowRequest> getStaffQueue() { return staffQueue; }
    public Queue<BorrowRequest> getReservationQueue() { return reservationQueue; }
    public List<BorrowRequest> getAllRequests() { return allRequests; }

    public void addRequest(BorrowRequest request) {
        allRequests.add(request);
        if (request.getStatus() == BorrowRequest.Status.RESERVED) reservationQueue.offer(request);
        else if (request.getStatus() == BorrowRequest.Status.PENDING_STAFF_REVIEW) staffQueue.offer(request);
        saveRequests();
    }

    public void saveEquipment() {
        writeEquipment();
    }

    public void saveRequests() {
        File file = new File(REQUESTS_FILE);
        try {
            ensureRecordsDirectory();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("txnNo|requestId|borrowerId|itemId|qty|purpose|activity|requestDate|expectedReturn|status|remarks");
                writer.newLine();
                for (BorrowRequest r : allRequests) {
                    writer.write(String.join("|", csv(r.getTxnNo()), csv(r.getRequestId()),
                            csv(r.getBorrowerId()), csv(r.getItemId()), String.valueOf(r.getQty()),
                            csv(r.getPurpose()), csv(r.getActivity()), r.getRequestDate().toString(),
                            r.getExpectedReturn().toString(), r.getStatus().name(), csv(r.getRemarks())));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Error saving requests.csv: " + e.getMessage());
        }
    }

    private void loadEquipment() {
        File file = new File(EQUIPMENT_FILE);
        if (!file.exists()) {
            inventory.put("E001", new Equipment("E001", "Oscilloscope", "Instrument", "Tektronix", 5, "Shelf A1", "Good"));
            inventory.put("E002", new Equipment("E002", "Digital Multimeter", "Instrument", "Fluke", 10, "Shelf A2", "Good"));
            inventory.put("E003", new Equipment("E003", "Oscilloscope", "Instrument", "Siglent", 4, "Shelf A3", "Good"));
            inventory.put("E004", new Equipment("E004", "Digital Multimeter", "Instrument", "Keysight", 6, "Shelf A4", "Good"));
            inventory.put("C001", new Equipment("C001", "10k Resistor", "Component", "Yageo", 200, "Drawer B1", "New"));
            inventory.put("C002", new Equipment("C002", "Jumper Wires", "Component", "Generic", 3, "Drawer B2", "New"));
            inventory.put("C003", new Equipment("C003", "10k Resistor", "Component", "Vishay", 150, "Drawer B3", "New"));
            inventory.put("C004", new Equipment("C004", "Transistor", "Component", "ON Semiconductor", 50, "Drawer B4", "New"));
            inventory.put("C005", new Equipment("C005", "Transistor", "Component", "Fairchild", 40, "Drawer B5", "New"));
            writeEquipment();
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] d = line.split("\\|", -1);
                if (d.length != 9 || d[0].equalsIgnoreCase("id")) continue;
                Equipment e = new Equipment(d[0], d[1], d[2], d[3], Integer.parseInt(d[4]), d[5], d[6]);
                e.setAvailableQty(Integer.parseInt(d[7]));
                e.setStatus(d[8]);
                inventory.put(e.getId(), e);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Error reading equipment.txt: " + e.getMessage());
        }
    }

    private void loadRequests() {
        File file = new File(REQUESTS_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] d = line.split("\\|", -1);
                if (d[0].equalsIgnoreCase("txnNo")) continue;
                BorrowRequest r;
                if (d.length == 11) {
                    r = new BorrowRequest(d[0], d[1], d[2], d[3], Integer.parseInt(d[4]),
                            d[5], d[6], LocalDate.parse(d[7]), LocalDate.parse(d[8]),
                            BorrowRequest.Status.valueOf(d[9]), d[10]);
                } else if (d.length == 10) {
                    r = new BorrowRequest(d[0], d[1], d[2], Integer.parseInt(d[3]),
                            d[4], d[5], LocalDate.parse(d[6]), LocalDate.parse(d[7]),
                            BorrowRequest.Status.valueOf(d[8]), d[9]);
                } else continue;
                allRequests.add(r);
                if (r.getStatus() == BorrowRequest.Status.RESERVED) reservationQueue.offer(r);
                else if (r.getStatus() == BorrowRequest.Status.PENDING_STAFF_REVIEW) staffQueue.offer(r);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Error reading requests.txt: " + e.getMessage());
        }
    }

    private void writeEquipment() {
        try {
            ensureRecordsDirectory();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(EQUIPMENT_FILE))) {
                writer.write("id|name|category|brand|totalQty|location|condition|availableQty|status");
                writer.newLine();
                for (Equipment e : inventory.values()) {
                    writer.write(String.join("|", csv(e.getId()), csv(e.getName()), csv(e.getCategory()),
                            csv(e.getBrand()), String.valueOf(e.getTotalQty()), csv(e.getLocation()),
                            csv(e.getCondition()), String.valueOf(e.getAvailableQty()), csv(e.getStatus())));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Error saving equipment.txt: " + e.getMessage());
        }
    }

    private static String csv(String value) {
        return value == null ? "" : value.replace("|", "/").replace("\r", " ").replace("\n", " ");
    }

    private static void ensureRecordsDirectory() {
        File directory = new File(RECORDS_DIR);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Unable to create records directory");
        }
    }

    /** Newest search first, one history per user. */
    public Deque<String> getRecentSearches(String userId) {
        return recentSearches.computeIfAbsent(userId, k -> new ArrayDeque<>());
    }
}
