package simpledb.execution;

import java.io.IOException;
import java.util.NoSuchElementException;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.storage.BufferPool;
import simpledb.storage.IntField;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

/**
 * The delete operator. Delete reads tuples from its child operator and removes
 * them from the table they belong to.
 */
public class Delete extends Operator {
	
    private static final long serialVersionUID = 1L;
    private final TransactionId tId;
    private OpIterator child;
    private TupleDesc td;
    /**
     * Constructor specifying the transaction that this delete belongs to as
     * well as the child to read from.
     *
     * @param t     The transaction this delete runs in
     * @param child The child operator from which to read tuples for deletion
     */
    public Delete(TransactionId t, OpIterator child) {
        this.child = child;
        this.tId = t;
        td = new TupleDesc(new Type[] {Type.INT_TYPE}, new String[] {"number"});
    }

    public TupleDesc getTupleDesc() {
        return td;
    }

    public void open() throws DbException, TransactionAbortedException {
        child.open();
    }

    public void close() {
        child.close();
    }
    public void rewind() throws DbException, TransactionAbortedException {
        child.rewind();
    }
 
    /**
     * Deletes tuples as they are read from the child operator. Deletes are
     * processed via the buffer pool (which can be accessed via the
     * Database.getBufferPool() method.
     *
     * @return A 1-field tuple containing the number of deleted records.
     * @see Database#getBufferPool
     * @see BufferPool#deleteTuple 
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
    	TupleDesc td = new TupleDesc(new Type[] {Type.INT_TYPE}, new String[] {"number"});
    	Tuple tuple = new Tuple(td);
    	int count = 0;
    	try {
			while(child.hasNext()){
				Database.getBufferPool().deleteTuple(tId, child.next());
				count++;
			}
		} catch (NoSuchElementException | DbException | IOException | TransactionAbortedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	tuple.setField(0, new IntField(count));
    	return tuple;
    }

    @Override
    public OpIterator[] getChildren() {
        return new OpIterator[] {this.child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
    	if(children != null && children.length != 0) {
    		this.child = children[0];
    	}
    }

}
