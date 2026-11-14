package org.example.lbycpds_project.models;

import java.time.LocalDate;

public class BorrowRequest {
    public enum Status { PENDING_STAFF_REVIEW, FILED, RETURNED_TO_STUDENT, APPROVED, REJECTED, ISSUED, RETURNED, RESERVED }

    private static int counter = 1000;

    private final String txnNo, borrowerId, itemId, purpose, activity;
    private final int qty;
    private final LocalDate requestDate, expectedReturn;
    private Status status;
    private String remarks = "-";

    public BorrowRequest(String borrowerId, String itemId, int qty, String purpose,
                         String activity, LocalDate expectedReturn, Status status) {
        this("TXN-" + (++counter), borrowerId, itemId, qty, purpose, activity,
                LocalDate.now(), expectedReturn, status, "-");
    }

    public BorrowRequest(String txnNo, String borrowerId, String itemId, int qty,
                         String purpose, String activity, LocalDate requestDate,
                         LocalDate expectedReturn, Status status, String remarks) {
        this.txnNo = txnNo;
        this.borrowerId = borrowerId; this.itemId = itemId; this.qty = qty;
        this.purpose = purpose; this.activity = activity;
        this.requestDate = requestDate; this.expectedReturn = expectedReturn;
        this.status = status;
        this.remarks = remarks;
        updateCounter(txnNo);
    }

    private static void updateCounter(String txnNo) {
        if (txnNo.startsWith("TXN-")) {
            try { counter = Math.max(counter, Integer.parseInt(txnNo.substring(4))); }
            catch (NumberFormatException ignored) { }
        }
    }

    public String getTxnNo() { return txnNo; }
    public String getBorrowerId() { return borrowerId; }
    public String getItemId() { return itemId; }
    public String getPurpose() { return purpose; }
    public String getActivity() { return activity; }
    public int getQty() { return qty; }
    public LocalDate getRequestDate() { return requestDate; }
    public LocalDate getExpectedReturn() { return expectedReturn; }
    public Status getStatus() { return status; }
    public String getRemarks() { return remarks; }
    public void setStatus(Status s, String remarks) { this.status = s; this.remarks = remarks; }

    @Override public String toString() {
        return String.format("%-9s item=%-7s qty=%-3d requested=%s return by=%s | %s | remarks: %s",
                txnNo, itemId, qty, requestDate, expectedReturn, status, remarks);
    }
}
