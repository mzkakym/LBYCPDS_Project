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
        this.txnNo = "TXN-" + (++counter);
        this.borrowerId = borrowerId; this.itemId = itemId; this.qty = qty;
        this.purpose = purpose; this.activity = activity;
        this.requestDate = LocalDate.now(); this.expectedReturn = expectedReturn;
        this.status = status;
    }

    public String getTxnNo() { return txnNo; }
    public String getBorrowerId() { return borrowerId; }
    public String getItemId() { return itemId; }
    public int getQty() { return qty; }
    public LocalDate getExpectedReturn() { return expectedReturn; }
    public Status getStatus() { return status; }
    public String getRemarks() { return remarks; }
    public void setStatus(Status s, String remarks) { this.status = s; this.remarks = remarks; }

    @Override public String toString() {
        return String.format("%-9s item=%-7s qty=%-3d requested=%s return by=%s | %s | remarks: %s",
                txnNo, itemId, qty, requestDate, expectedReturn, status, remarks);
    }
}
