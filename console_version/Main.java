import java.util.*;

public class Main {
    public static void main(String[] args) {
        Map<String, Equipment> inventory = new HashMap<>();
        inventory.put("E001", new Equipment("E001", "Oscilloscope", "Instrument", "Tektronix", 5, "Shelf A1", "Good"));
        inventory.put("E002", new Equipment("E002", "Digital Multimeter", "Instrument", "Fluke", 10, "Shelf A2", "Good"));
        inventory.put("C001", new Equipment("C001", "10k Resistor", "Component", "Yageo", 200, "Drawer B1", "New"));
        inventory.put("C002", new Equipment("C002", "Jumper Wires", "Component", "Generic", 3, "Drawer B2", "New"));

        Queue<BorrowRequest> staffQueue = new LinkedList<>();
        Queue<BorrowRequest> reservationQueue = new LinkedList<>();
        List<BorrowRequest> allRequests = new ArrayList<>();

        new BorrowerMenu("2023-12345", inventory, staffQueue, reservationQueue, allRequests,
                new Scanner(System.in)).run();
    }
}
