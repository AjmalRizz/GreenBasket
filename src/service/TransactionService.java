package service;

import model.StockTransaction;
import util.FileHandler;
import util.SecurityUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps the audit trail: every stock change is recorded here and never edited.
 */
public class TransactionService {

    public static final String DEFAULT_FILE = "transactions.dat";

    private final String fileName;
    private final List<StockTransaction> transactions;

    public TransactionService() {
        this(DEFAULT_FILE);
    }

    /** @param fileName where transactions are stored (tests pass a temp file) */
    public TransactionService(String fileName) {
        this.fileName = fileName;
        this.transactions = FileHandler.loadFromFile(fileName);
    }

    public synchronized void recordTransaction(String productId, String productName,
                                               StockTransaction.Type type, int quantityChanged,
                                               int resultingQuantity, BigDecimal unitPrice,
                                               String performedBy, String notes) {
        // Transactions are never deleted, so "count + 1" is always a new, unique ID.
        String txId = String.format("TX-%05d", transactions.size() + 1);
        transactions.add(new StockTransaction(txId, productId, productName, type,
                quantityChanged, resultingQuantity, unitPrice, performedBy, notes));
        FileHandler.saveToFile(transactions, fileName);
    }

    /** Returns a copy, newest first. */
    public synchronized List<StockTransaction> getAllTransactions() {
        List<StockTransaction> copy = new ArrayList<>(transactions);
        Collections.reverse(copy);
        return copy;
    }

    public synchronized int getTransactionCount() {
        return transactions.size();
    }

    public synchronized boolean exportToCSV(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("TransactionID,Date,ProductCode,ProductName,Type,QtyChanged,ResultingQty,UnitPrice,Total,Operator,Notes\n");
            for (StockTransaction tx : transactions) {
                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,%s,%s,\"%s\",\"%s\"%n",
                        SecurityUtil.sanitizeForCSV(tx.getTransactionId()),
                        SecurityUtil.sanitizeForCSV(tx.getFormattedDate()),
                        SecurityUtil.sanitizeForCSV(tx.getProductId()),
                        SecurityUtil.sanitizeForCSV(tx.getProductName()),
                        tx.getType(),
                        tx.getQuantityChanged(),
                        tx.getResultingQuantity(),
                        tx.getUnitPrice().toPlainString(),
                        tx.getTotalAmount().toPlainString(),
                        SecurityUtil.sanitizeForCSV(tx.getPerformedBy()),
                        SecurityUtil.sanitizeForCSV(tx.getNotes())));
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
