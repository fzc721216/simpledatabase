package simpledb.execution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import simpledb.common.Type;
import simpledb.storage.Field;
import simpledb.storage.IntField;
import simpledb.storage.StringField;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.storage.TupleIterator;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;
    private Map<Field, Integer> aggResult;
    private int gbfield;
    private int afield;
    private Type gbfieldType;
    private Op what;
    /**
     * Aggregate constructor
     *
     * @param gbfield     the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield      the 0-based index of the aggregate field in the tuple
     * @param what        aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // TODO: some code goes here
    	this.aggResult = new HashMap<>();
    	this.what = what;
    	this.afield = afield;
    	this.gbfield = gbfield;
    	this.gbfieldType = gbfieldtype;
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     *
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // TODO: some code goes here
    	if(what != Op.COUNT) {
    		throw new IllegalArgumentException();
    	}
    	Field field = gbfield == NO_GROUPING ? null : tup.getField(gbfield);
    	aggResult.put(field, aggResult.getOrDefault(field, 0) + 1);
    	
    }
    public String getName() {
    	return "StringAggregator";
    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal,
     *         aggregateVal) if using group, or a single (aggregateVal) if no
     *         grouping. The aggregateVal is determined by the type of
     *         aggregate specified in the constructor.
     */
    public OpIterator iterator() {
        Type[] types;
        String[] names;
        TupleDesc tupleDesc;
        List<Tuple> tuples = new ArrayList<>();
        if(gbfield == NO_GROUPING){
            types = new Type[]{Type.INT_TYPE};
            names = new String[]{"aggregateVal"};
            tupleDesc = new TupleDesc(types, names);
            IntField resultField = new IntField(aggResult.get(null));
            Tuple tuple = new Tuple(tupleDesc);
            tuple.setField(0, resultField);
            tuples.add(tuple);
        }
        else{
            types = new Type[]{gbfieldType, Type.INT_TYPE};
            names = new String[]{"groupVal", "aggregateVal"};
            tupleDesc = new TupleDesc(types, names);
            for(Field field: aggResult.keySet()){
                Tuple tuple = new Tuple(tupleDesc);
                if(gbfieldType == Type.INT_TYPE){
                    IntField intField = (IntField) field;
                    tuple.setField(0, intField);
                }
                else{
                    StringField stringField = (StringField) field;
                    tuple.setField(0, stringField);
                }

                IntField resultField = new IntField(aggResult.get(field));
                tuple.setField(1, resultField);
                tuples.add(tuple);
            }
        }
        return new TupleIterator(tupleDesc ,tuples);
    }
    

}
