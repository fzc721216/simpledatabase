package simpledb.optimizer;
 
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import simpledb.common.Database;
import simpledb.common.Type;
import simpledb.execution.Predicate;
import simpledb.execution.SeqScan;
import simpledb.storage.DbFile;
import simpledb.storage.Field;
import simpledb.storage.HeapFile;
import simpledb.storage.IntField;
import simpledb.storage.StringField;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionId;
 
/**
 * TableStats represents statistics (e.g., histograms) about base tables in a
 * query. 
 * 
 * This class is not needed in implementing lab1 and lab2.
 */
public class TableStats {
 
    private static final ConcurrentHashMap<String, TableStats> statsMap = new ConcurrentHashMap<String, TableStats>();
 
    static final int IOCOSTPERPAGE = 1000;
 
    public static TableStats getTableStats(String tablename) {
        return statsMap.get(tablename);
    }
 
    public static void setTableStats(String tablename, TableStats stats) {
        statsMap.put(tablename, stats);
    }
    
    public static void setStatsMap(HashMap<String,TableStats> s)
    {
        try {
            java.lang.reflect.Field statsMapF = TableStats.class.getDeclaredField("statsMap");
            statsMapF.setAccessible(true);
            statsMapF.set(null, s);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
 
    }
 
    public static Map<String, TableStats> getStatsMap() {
        return statsMap;
    }
 
    public static void computeStatistics() {
        Iterator<Integer> tableIt = Database.getCatalog().tableIdIterator();
 
        System.out.println("Computing table stats.");
        while (tableIt.hasNext()) {
            int tableid = tableIt.next();
            TableStats s = new TableStats(tableid, IOCOSTPERPAGE);
            setTableStats(Database.getCatalog().getTableName(tableid), s);
        }
        System.out.println("Done.");
    }
 
    /**
     * Number of bins for the histogram. Feel free to increase this value over
     * 100, though our tests assume that you have at least 100 bins in your
     * histograms.
     */
    static final int NUM_HIST_BINS = 100;
 
    private int ioCostPerPage;
    private DbFile dbFile;
    private int tableid;
    private int numFields;
    private int numTuples;
    private int numPages;
    private HashMap<Integer,IntHistogram> intHistogramHashMap;
    private HashMap<Integer,StringHistogram> stringHistogramHashMap;
 
    /**
     * Create a new TableStats object, that keeps track of statistics on each
     * column of a table
     * 
     * @param tableid
     *            The table over which to compute statistics
     * @param ioCostPerPage
     *            The cost per page of IO. This doesn't differentiate between
     *            sequential-scan IO and disk seeks.
     */
    public TableStats(int tableid, int ioCostPerPage) {
        // For this function, you'll have to get the
        // DbFile for the table in question,
        // then scan through its tuples and calculate
        // the values that you need.
        // You should try to do this reasonably efficiently, but you don't
        // necessarily have to (for example) do everything
        // in a single scan of the table.
        // some code goes here
        numTuples = 0;
        this.tableid = tableid;
        this.ioCostPerPage = ioCostPerPage;
        intHistogramHashMap = new HashMap<Integer, IntHistogram>();
        stringHistogramHashMap = new HashMap<Integer, StringHistogram>();
 
        dbFile = Database.getCatalog().getDatabaseFile(tableid);
        numPages = ((HeapFile)dbFile).numPages();
        TupleDesc td = dbFile.getTupleDesc();
 
        numFields = td.numFields();
        Type types[] = getTypes(td);
 
        int[] mins = new int[numFields];
        int[] maxs = new int[numFields];
 
        TransactionId tid = new TransactionId();
        SeqScan scan = new SeqScan(tid,tableid,"");
        try{
            scan.open();
            for(int i=0;i<numFields;++i){
                if(types[i] == Type.STRING_TYPE)
                    continue;
 
                int min = Integer.MAX_VALUE;
                int max = Integer.MIN_VALUE;
 
                while(scan.hasNext()){
                    if(i == 0) numTuples++;
                    Tuple tuple = scan.next();
                    IntField field = (IntField)tuple.getField(i);
                    int val = field.getValue();
                    if(val > max) max = val;
                    if(val < min) min = val;
                }
                scan.rewind();
                mins[i] = min;
                maxs[i] = max;
            }
            scan.close();
        }catch (Exception e){
            e.printStackTrace();
        }
 
        for(int i=0;i < numFields;++i){
            Type type = types[i];
            if(type == Type.INT_TYPE){
                IntHistogram intHistogram = new IntHistogram(NUM_HIST_BINS,mins[i],maxs[i]);
                intHistogramHashMap.put(i,intHistogram);
            }else{
                StringHistogram stringHistogram = new StringHistogram(NUM_HIST_BINS);
                stringHistogramHashMap.put(i,stringHistogram);
            }
        }
 
        addValueToHist();
    }
 
    private Type[] getTypes(TupleDesc td){
        int numFields = td.numFields();
        Type[] types = new Type[numFields];
 
        for(int i=0;i<numFields;++i){
            Type t = td.getFieldType(i);
            types[i] = t;
        }
        return types;
    }
 
    private void addValueToHist(){
        TransactionId tid = new TransactionId();
        SeqScan scan = new SeqScan(tid,tableid,"");
        try{
            scan.open();
            while(scan.hasNext()){
                Tuple tuple = scan.next();
 
                for(int i=0;i<numFields;++i){
                    Field field = tuple.getField(i);
 
                    if(field.getType() == Type.INT_TYPE){
                        int val = ((IntField)field).getValue();
                        intHistogramHashMap.get(i).addValue(val);
                    }else{
                        String val = ((StringField)field).getValue();
                        stringHistogramHashMap.get(i).addValue(val);
                    }
                }
            }
            scan.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
 
    /**
     * Estimates the cost of sequentially scanning the file, given that the cost
     * to read a page is costPerPageIO. You can assume that there are no seeks
     * and that no pages are in the buffer pool.
     * 
     * Also, assume that your hard drive can only read entire pages at once, so
     * if the last page of the table only has one tuple on it, it's just as
     * expensive to read as a full page. (Most real hard drives can't
     * efficiently address regions smaller than a page at a time.)
     * 
     * @return The estimated cost of scanning the table.
     */
    public double estimateScanCost() {
        // some code goes here
        HeapFile heapFile = (HeapFile)dbFile;
        return heapFile.numPages() * ioCostPerPage;
    }
 
    /**
     * This method returns the number of tuples in the relation, given that a
     * predicate with selectivity selectivityFactor is applied.
     * 
     * @param selectivityFactor
     *            The selectivity of any predicates over the table
     * @return The estimated cardinality of the scan with the specified
     *         selectivityFactor
     */
    public int estimateTableCardinality(double selectivityFactor) {
        // some code goes here
        double cardinality = numTuples * selectivityFactor;
        return (int) cardinality;
    }
 
    /**
     * The average selectivity of the field under op.
     * @param field
     *        the index of the field
     * @param op
     *        the operator in the predicate
     * The semantic of the method is that, given the table, and then given a
     * tuple, of which we do not know the value of the field, return the
     * expected selectivity. You may estimate this value from the histograms.
     * */
    public double avgSelectivity(int field, Predicate.Op op) {
        // some code goes here
        return 1.0;
    }
 
    /**
     * Estimate the selectivity of predicate <tt>field op constant</tt> on the
     * table.
     * 
     * @param field
     *            The field over which the predicate ranges
     * @param op
     *            The logical operation in the predicate
     * @param constant
     *            The value against which the field is compared
     * @return The estimated selectivity (fraction of tuples that satisfy) the
     *         predicate
     */
    public double estimateSelectivity(int field, Predicate.Op op, Field constant) {
        // some code goes here
        double selectivity;
        if(constant.getType() == Type.INT_TYPE){
            IntField intField = (IntField) constant;
            selectivity = intHistogramHashMap.get(field).estimateSelectivity(op,intField.getValue());
        }else{
            StringField stringField = (StringField) constant;
            selectivity = stringHistogramHashMap.get(field).estimateSelectivity(op,stringField.getValue());
        }
        return selectivity;
    }
 
    /**
     * return the total number of tuples in this table
     * */
    public int totalTuples() {
        // some code goes here
        return numTuples;
    }
 
}