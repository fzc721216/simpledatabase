package simpledb.execution;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.storage.DbFileIterator;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.storage.TupleDesc.TDItem;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

/**
 * SeqScan is an implementation of a sequential scan access method that reads
 * each tuple of a table in no particular order (e.g., as they are laid out on
 * disk).
 */
public class SeqScan implements OpIterator {

    private static final long serialVersionUID = 1L;
    private TransactionId transactionId;
    private int tableId;
    private String tableAlias;
    private DbFileIterator iterator;
    /**
     * Creates a sequential scan over the specified table as a part of the
     * specified transaction.
     *
     * @param tid        The transaction this scan is running as a part of.
     * @param tableid    the table to scan.
     * @param tableAlias the alias of this table (needed by the parser); the returned
     *                   tupleDesc should have fields with name tableAlias.fieldName
     *                   (note: this class is not responsible for handling a case where
     *                   tableAlias or fieldName are null. It shouldn't crash if they
     *                   are, but the resulting name can be null.fieldName,
     *                   tableAlias.null, or null.null).
     */
    public SeqScan(TransactionId tid, int tableid, String tableAlias) {
        // TODO: some code goes here
    	this.transactionId = tid;
    	this.tableId = tableid;
    	this.tableAlias = tableAlias;
    	iterator = Database.getCatalog().getDatabaseFile(tableId).iterator(transactionId);
    }

    /**
     * @return return the table name of the table the operator scans. This should
     *         be the actual name of the table in the catalog of the database
     */
    public String getTableName() {
    	return Database.getCatalog().getTableName(tableId);
    }

    /**
     * @return Return the alias of the table this operator scans.
     */
    public String getAlias() {
        // TODO: some code goes here
        return tableAlias;
    }

    /**
     * Reset the tableid, and tableAlias of this operator.
     *
     * @param tableid    the table to scan.
     * @param tableAlias the alias of this table (needed by the parser); the returned
     *                   tupleDesc should have fields with name tableAlias.fieldName
     *                   (note: this class is not responsible for handling a case where
     *                   tableAlias or fieldName are null. It shouldn't crash if they
     *                   are, but the resulting name can be null.fieldName,
     *                   tableAlias.null, or null.null).
     */
    public void reset(int tableid, String tableAlias) {
        // TODO: some code goes here
    	this.tableId = tableid;
    	this.tableAlias = tableAlias;
    }
    
    //没有别名用默认名字
    public SeqScan(TransactionId tid, int tableId) {
        this(tid, tableId, Database.getCatalog().getTableName(tableId));
    }

    public void open() throws DbException, TransactionAbortedException {
        // TODO: some code goes here
        iterator.open();
    }

    /**
     * Returns the TupleDesc with field names from the underlying HeapFile,
     * prefixed with the tableAlias string from the constructor. This prefix
     * becomes useful when joining tables containing a field(s) with the same
     * name.  The alias and name should be separated with a "." character
     * (e.g., "alias.fieldName").
     *
     * @return the TupleDesc with field names from the underlying HeapFile,
     *         prefixed with the tableAlias string from the constructor.
     */
    public TupleDesc getTupleDesc() {
        // TODO: some code goes here
    	TupleDesc ntd = Database.getCatalog().getTupleDesc(tableId);
    	Iterator<TDItem> iterator = ntd.iterator();
    	ArrayList<TDItem> arrayList = new ArrayList<>();
    	while(iterator.hasNext()) {
    		TDItem tdItem = iterator.next();
    		String name = tdItem.fieldName;
    		String newName = (tableAlias == null ? "null" : tableAlias) + "." +
    		(name == null ? "null" : name);
    		arrayList.add(new TDItem(tdItem.fieldType, newName));
    	}
    	return new TupleDesc(arrayList);
    }

    public boolean hasNext() throws TransactionAbortedException, DbException {
        // TODO: some code goes here
    	return iterator.hasNext();
    }

    public Tuple next() throws NoSuchElementException,
            TransactionAbortedException, DbException {
        // TODO: some code goes here
    	if(hasNext()) return iterator.next();
        throw new NoSuchElementException();
    }

    public void close() {
        // TODO: some code goes here
    	iterator.close();
    }

    public void rewind() throws DbException, NoSuchElementException,
            TransactionAbortedException { 
        // TODO: some code goes here
    	iterator.rewind();
    }
}
