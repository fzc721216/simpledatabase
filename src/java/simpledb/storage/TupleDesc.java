package simpledb.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import simpledb.common.Type;

/**
 * TupleDesc describes the schema of a tuple.
 */
public class TupleDesc implements Serializable {

    /**
     * A help class to facilitate organizing the information of each field
     */
	private final TDItem[] tDItems;
	
    public static class TDItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The type of the field
         */
        public final Type fieldType;

        /**
         * The name of the field
         */
        public final String fieldName;

        public TDItem(Type t, String n) {
            this.fieldName = n;
            this.fieldType = t;
        }

        public String toString() {
            return fieldName + "(" + fieldType + ")";
        }
    }

    /**
     * @return An iterator which iterates over all the field TDItems
     *         that are included in this TupleDesc
     */
    public Iterator<TDItem> iterator() {
        // TODO: some code goes here
        return (Iterator<TDItem>)Arrays.asList(tDItems).iterator();
    }

    private static final long serialVersionUID = 1L;

    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     *
     * @param typeAr  array specifying the number of and types of fields in this
     *                TupleDesc. It must contain at least one entry.
     * @param fieldAr array specifying the names of the fields. Note that names may
     *                be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
        // TODO: some code goes here
    	tDItems = new TDItem[typeAr.length];
    	for(int i = 0; i < typeAr.length; i++) {
    		tDItems[i] = new TDItem(typeAr[i], fieldAr[i]);
    	}
    }

    /**
     * Constructor. Create a new tuple desc with typeAr.length fields with
     * fields of the specified types, with anonymous (unnamed) fields.
     *
     * @param types array specifying the number of and types of fields in this
     *               TupleDesc. It must contain at least one entry.
     */
    public TupleDesc(Type[] types) {
        // TODO: some code goes here
    	tDItems = new TDItem[types.length];
    	for(int i = 0; i < types.length; i++) {
    		tDItems[i] = new TDItem(types[i], "");
    	}
    }
    public TupleDesc(ArrayList<TDItem> arrayList) {
        // TODO: some code goes here
    	tDItems = new TDItem[arrayList.size()];
    	for(int i = 0; i < arrayList.size(); i++) {
    		tDItems[i] = arrayList.get(i);
    	}
    }
    /**
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {
        // TODO: some code goes here
        return tDItems.length;
    }

    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     *
     * @param i index of the field name to return. It must be a valid index.
     * @return the name of the ith field
     * @throws NoSuchElementException if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {
        // TODO: some code goes here
        return tDItems[i].fieldName;
    }

    /**
     * Gets the type of the ith field of this TupleDesc.
     *
     * @param i The index of the field to get the type of. It must be a valid
     *          index.
     * @return the type of the ith field
     * @throws NoSuchElementException if i is not a valid field reference.
     */
    public Type getFieldType(int i) throws NoSuchElementException {
        // TODO: some code goes here
        return tDItems[i].fieldType;
    }

    /**
     * Find the index of the field with a given name.
     *
     * @param name name of the field.
     * @return the index of the field that is first to have the given name.
     * @throws NoSuchElementException if no field with a matching name is found.
     */
    public int indexForFieldName(String name) throws NoSuchElementException {
        // TODO: some code goes here
    	int ret = -1;
    	for(int i = 0; i < tDItems.length; i++) {
    		if(tDItems[i].fieldName.equals(name)) {
    			ret = i;
    			return ret;
    		}
    	}
        throw new NoSuchElementException();
    }

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     *         Note that tuples from a given TupleDesc are of a fixed size.
     */
    public int getSize() {
        // TODO: some code goes here
    	int size = 0;
    	for(int i = 0; i < tDItems.length; i++) {
    		size += tDItems[i].fieldType.getLen();
    	}
        return size;
    }

    /**
     * Merge two TupleDescs into one, with td1.numFields + td2.numFields fields,
     * with the first td1.numFields coming from td1 and the remaining from td2.
     *
     * @param td1 The TupleDesc with the first fields of the new TupleDesc
     * @param td2 The TupleDesc with the last fields of the TupleDesc
     * @return the new TupleDesc
     */
    public static TupleDesc merge(TupleDesc td1, TupleDesc td2) {
        // TODO: some code goes here
    	int td1Len = td1.tDItems.length;
    	int td2Len = td2.tDItems.length;
    	int sum = td1Len + td2Len;
    	Type[] newTypes = new Type[sum];
    	String[] newFields = new String[sum];
    	
    	for(int i = 0; i < td1Len; i++) {
    		newTypes[i] = td1.tDItems[i].fieldType;
    		newFields[i] = td1.tDItems[i].fieldName;
    	}
    	for(int i = td1Len; i < sum; i++) {
    		newTypes[i] = td2.tDItems[i - td1Len].fieldType;
    		newFields[i] = td2.tDItems[i - td1Len].fieldName;
    	}
    	
        return new TupleDesc(newTypes, newFields);
    }

    /**
     * Compares the specified object with this TupleDesc for equality. Two
     * TupleDescs are considered equal if they have the same number of items
     * and if the i-th type in this TupleDesc is equal to the i-th type in o
     * for every i.
     *
     * @param o the Object to be compared for equality with this TupleDesc.
     * @return true if the object is equal to this TupleDesc.
     */

    public boolean equals(Object o) {
        // TODO: some code goes here
    	TupleDesc other = null;
    	if(this.getClass().isInstance(o)) {
    		other = (TupleDesc)o;
    		if(this.numFields() != other.numFields()) return false;
    	}else {
    		return false;
    	}
    	for(int i = 0; i < this.numFields(); i++) {
    		if(this.tDItems[i].fieldType != other.tDItems[i].fieldType) return false;
    	}
        return true;
    }

    public int hashCode() {
        // If you want to use TupleDesc as keys for HashMap, implement this so
        // that equal objects have equals hashCode() results
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldType[0](fieldName[0]), ..., fieldType[M](fieldName[M])", although
     * the exact format does not matter.
     *
     * @return String describing this descriptor.
     */
    public String toString() {
        // TODO: some code goes here
    	StringBuffer sb = new StringBuffer();
    	for(int i = 0; i < this.numFields(); i++) {
    		sb.append(tDItems[i].fieldName)
    		.append("(")
    		.append(tDItems[i].fieldType)
    		.append(")")
    		.append(",");
    	}
    	sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
