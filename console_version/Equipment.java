public class Equipment {

    private final String id;
    private String name, category, brand, location, condition, status;
    private int totalQty, availableQty;

    public Equipment(String id, String name, String category, String brand,
                     int totalQty, String location, String condition) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.totalQty = totalQty;
        this.availableQty = totalQty;
        this.location = location;
        this.condition = condition;
        this.status = "Available";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getBrand() { return brand; }
    public String getLocation() { return location; }
    public String getCondition() { return condition; }
    public String getStatus() { return status; }
    public int getTotalQty() { return totalQty; }
    public int getAvailableQty() { return availableQty; }
    public int getBorrowedQty() { return totalQty - availableQty; }

    public void setAvailableQty(int q) {
        if (q < 0 || q > totalQty) {
            throw new IllegalArgumentException("Invalid quantity");
        }

        availableQty = q;

        if (!status.equals("Damaged") &&
                !status.equals("Missing") &&
                !status.equals("Under Maintenance")) {
            status = (q == 0) ? "Borrowed" : "Available";
        }
    }

    @Override
    public String toString() {
        return String.format("%-8s %-22s %-12s %-10s %3d/%-3d %-10s %s",
                id, name, category, location, availableQty, totalQty,
                condition, status);
    }
}