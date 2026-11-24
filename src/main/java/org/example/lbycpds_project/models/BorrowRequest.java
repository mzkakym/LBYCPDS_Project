package org.example.lbycpds_project.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BorrowRequest {
    public enum Status { PENDING_STAFF_REVIEW, FILED, RETURNED_TO_STUDENT, APPROVED, REJECTED, ISSUED, RETURNED, RESERVED }

    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
    private static int counter = 1000;

    private final String txnNo, requestId, borrowerId, itemId, purpose, activity, section;
    private final int qty;
    private final LocalDate requestDate, expectedReturn;
    private final LocalDateTime requestDateTime;
    private Status status;
    private String remarks = "-";
    private String handledByStaff = "Unassigned";

    // 1. Existing constructor (defaults section to "N/A")
    public BorrowRequest(String borrowerId, String itemId, int qty, String purpose,
                         String activity, LocalDate expectedReturn, Status status) {
        this("TXN-" + (++counter), "TXN-" + counter, borrowerId, itemId, qty, purpose,
                activity, "N/A", LocalDate.now(), LocalDateTime.now(), expectedReturn, status, "-", "Unassigned");
    }

    // 2. Constructor with requestId AND section (Used by BorrowerPortal)
    public BorrowRequest(String requestId, String borrowerId, String itemId, int qty,
                         String purpose, String activity, String section,
                         LocalDate expectedReturn, Status status) {
        this("TXN-" + (++counter), requestId, borrowerId, itemId, qty, purpose,
                activity, section, LocalDate.now(), LocalDateTime.now(), expectedReturn, status, "-", "Unassigned");
    }

    // 2b. Overload without section (backwards compatible)
    public BorrowRequest(String requestId, String borrowerId, String itemId, int qty,
                         String purpose, String activity, LocalDate expectedReturn, Status status) {
        this(requestId, borrowerId, itemId, qty, purpose, activity, "N/A", expectedReturn, status);
    }

    // 3. Existing 10-arg constructor for loading from file
    public BorrowRequest(String txnNo, String borrowerId, String itemId, int qty,
                         String purpose, String activity, LocalDate requestDate,
                         LocalDate expectedReturn, Status status, String remarks) {
        this(txnNo, txnNo, borrowerId, itemId, qty, purpose, activity, "N/A", requestDate,
                requestDate.atStartOfDay(), expectedReturn, status, remarks, "Unassigned");
    }

    // 4. Existing 11-arg constructor for loading from file (legacy rows without section)
    public BorrowRequest(String txnNo, String requestId, String borrowerId, String itemId, int qty,
                         String purpose, String activity, LocalDate requestDate,
                         LocalDate expectedReturn, Status status, String remarks) {
        this(txnNo, requestId, borrowerId, itemId, qty, purpose, activity, "N/A", requestDate,
                requestDate.atStartOfDay(), expectedReturn, status, remarks, "Unassigned");
    }

    // 4b. 12-arg constructor for loading from file WITH section
    public BorrowRequest(String txnNo, String requestId, String borrowerId, String itemId, int qty,
                         String purpose, String activity, String section, LocalDate requestDate,
                         LocalDate expectedReturn, Status status, String remarks) {
        this(txnNo, requestId, borrowerId, itemId, qty, purpose, activity, section, requestDate,
                requestDate.atStartOfDay(), expectedReturn, status, remarks, "Unassigned");
    }

    // 5. Full constructor
    public BorrowRequest(String txnNo, String requestId, String borrowerId, String itemId, int qty,
                         String purpose, String activity, String section, LocalDate requestDate,
                         LocalDateTime requestDateTime, LocalDate expectedReturn,
                         Status status, String remarks, String handledByStaff) {
        this.txnNo = txnNo;
        this.requestId = requestId;
        this.borrowerId = borrowerId;
        this.itemId = itemId;
        this.qty = qty;
        this.purpose = purpose;
        this.activity = activity;
        this.section = (section != null && !section.trim().isEmpty()) ? section.trim().toUpperCase() : "N/A";
        this.requestDate = requestDate;
        this.requestDateTime = (requestDateTime != null) ? requestDateTime : requestDate.atStartOfDay();
        this.expectedReturn = expectedReturn;
        this.status = status;
        this.remarks = remarks;
        this.handledByStaff = (handledByStaff != null && !handledByStaff.isEmpty()) ? handledByStaff : "Unassigned";
        updateCounter(txnNo);
    }

    private static void updateCounter(String txnNo) {
        if (txnNo.startsWith("TXN-")) {
            try { counter = Math.max(counter, Integer.parseInt(txnNo.substring(4))); }
            catch (NumberFormatException ignored) { }
        }
    }

    public String getTxnNo() { return txnNo; }
    public String getRequestId() { return requestId; }
    public String getBorrowerId() { return borrowerId; }
    public String getItemId() { return itemId; }
    public String getPurpose() { return purpose; }
    public String getActivity() { return activity; }
    public String getSection() { return section; }
    public int getQty() { return qty; }
    public LocalDate getRequestDate() { return requestDate; }
    public LocalDateTime getRequestDateTime() { return requestDateTime; }
    public String getFormattedRequestDateTime() { return requestDateTime.format(TIME_FMT); }
    public LocalDate getExpectedReturn() { return expectedReturn; }
    public Status getStatus() { return status; }
    public String getRemarks() { return remarks; }
    public String getHandledByStaff() { return handledByStaff; }

    public void setStatus(Status s, String remarks) {
        this.status = s;
        this.remarks = remarks;
    }

    public void setStatus(Status s, String remarks, String staffInfo) {
        this.status = s;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
        this.remarks = remarks + " [" + timestamp + "]";
        this.handledByStaff = staffInfo;
    }

    @Override
    public String toString() {
        return String.format("%-9s item=%-7s qty=%-3d section=%-12s requested=%s return by=%s | %s | staff: %s | remarks: %s",
                txnNo, itemId, qty, section, getFormattedRequestDateTime(), expectedReturn, status, handledByStaff, remarks);
    }
}