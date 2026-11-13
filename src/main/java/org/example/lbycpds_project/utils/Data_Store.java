package org.example.lbycpds_project.utils;

import org.example.lbycpds_project.models.BorrowRequest;
import org.example.lbycpds_project.models.Equipment;

import java.util.*;

/**
 * One shared copy of the inventory, queues and requests.
 * Borrower, Staff and Admin screens should all use Data_Store.getInstance()
 * so they see the same data. (Everything here is in memory for now.)
 */
public final class Data_Store {
    private static final Data_Store INSTANCE = new Data_Store();

    private final Map<String, Equipment> inventory = new HashMap<>();
    private final Queue<BorrowRequest> staffQueue = new LinkedList<>();
    private final Queue<BorrowRequest> reservationQueue = new LinkedList<>();
    private final List<BorrowRequest> allRequests = new ArrayList<>();
    private final Map<String, Deque<String>> recentSearches = new HashMap<>();

    private Data_Store() {
        // starting inventory (same items as the console Main.java)
        inventory.put("E001", new Equipment("E001", "Oscilloscope", "Instrument", "Tektronix", 5, "Shelf A1", "Good"));
        inventory.put("E002", new Equipment("E002", "Digital Multimeter", "Instrument", "Fluke", 10, "Shelf A2", "Good"));
        inventory.put("E003", new Equipment("E003", "Oscilloscope", "Instrument", "Siglent", 4, "Shelf A3", "Good"));
        inventory.put("E004", new Equipment("E004", "Digital Multimeter", "Instrument", "Keysight", 6, "Shelf A4", "Good"));
        inventory.put("C001", new Equipment("C001", "10k Resistor", "Component", "Yageo", 200, "Drawer B1", "New"));
        inventory.put("C002", new Equipment("C002", "Jumper Wires", "Component", "Generic", 3, "Drawer B2", "New"));
        inventory.put("C003", new Equipment("C003", "10k Resistor", "Component", "Vishay", 150, "Drawer B3", "New"));
        inventory.put("C004", new Equipment("C004", "Transistor", "Component", "ON Semiconductor", 50, "Drawer B4", "New"));
        inventory.put("C005", new Equipment("C005", "Transistor", "Component", "Fairchild", 40, "Drawer B5", "New"));
    }

    public static Data_Store getInstance() { return INSTANCE; }

    public Map<String, Equipment> getInventory() { return inventory; }
    public Queue<BorrowRequest> getStaffQueue() { return staffQueue; }
    public Queue<BorrowRequest> getReservationQueue() { return reservationQueue; }
    public List<BorrowRequest> getAllRequests() { return allRequests; }

    /** Newest search first, one history per user. */
    public Deque<String> getRecentSearches(String userId) {
        return recentSearches.computeIfAbsent(userId, k -> new ArrayDeque<>());
    }
}
