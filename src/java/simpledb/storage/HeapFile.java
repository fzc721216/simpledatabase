package simpledb.storage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Permissions;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @author Sam Madden
 * @see HeapPage#HeapPage
 */
public class HeapFile implements DbFile {

	private File file;
	private TupleDesc td;

	/**
	 * Constructs a heap file backed by the specified file.
	 *
	 * @param f the file that stores the on-disk backing store for this heap file.
	 */
	public HeapFile(File f, TupleDesc td) {
		// TODO: some code goes here
		this.file = f;
		this.td = td;
	}

	/**
	 * Returns the File backing this HeapFile on disk.
	 *
	 * @return the File backing this HeapFile on disk.
	 */
	public File getFile() {
		// TODO: some code goes here
		return file;
	}

	/**
	 * Returns an ID uniquely identifying this HeapFile. Implementation note: you
	 * will need to generate this tableid somewhere to ensure that each HeapFile has
	 * a "unique id," and that you always return the same value for a particular
	 * HeapFile. We suggest hashing the absolute file name of the file underlying
	 * the heapfile, i.e. f.getAbsoluteFile().hashCode().
	 *
	 * @return an ID uniquely identifying this HeapFile.
	 */
	public int getId() {
		// TODO: some code goes here
		return file.getAbsoluteFile().hashCode();
	}

	/**
	 * Returns the TupleDesc of the table stored in this DbFile.
	 *
	 * @return TupleDesc of this DbFile.
	 */
	public TupleDesc getTupleDesc() {
		// TODO: some code goes here
		return td;
	}

	// see DbFile.java for javadocs
	public Page readPage(PageId pid) {
		// TODO: some code goes here
		int tableId = pid.getTableId();
		int pgNo = pid.getPageNumber();

		RandomAccessFile f = null;
		try {
			f = new RandomAccessFile(file, "r");
			if ((pgNo + 1) * BufferPool.getPageSize() > f.length()) {
				f.close();
				throw new IllegalArgumentException(String.format("table %d page %d is invalid", tableId, pgNo));
			}
			byte[] bytes = new byte[BufferPool.getPageSize()];
			f.seek(pgNo * BufferPool.getPageSize());
			// big end
			int read = f.read(bytes, 0, BufferPool.getPageSize());
			if (read != BufferPool.getPageSize()) {
				throw new IllegalArgumentException(
						String.format("table %d page %d read %d bytes", tableId, pgNo, read));
			}
			HeapPageId id = new HeapPageId(tableId, pgNo);
			return new HeapPage(id, bytes);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				f.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		throw new IllegalArgumentException();
	}

	public void writePage(Page page) throws IOException {
		// TODO: some code goes here
		// not necessary for lab1
		int pageId = page.getId().getPageNumber();
		if (pageId > numPages())
			throw new IOException();
		RandomAccessFile f = new RandomAccessFile(file, "rw");
		f.seek(pageId * BufferPool.getPageSize());
		byte[] data = page.getPageData();
		f.write(data);
		f.close();
	}
	

	/**
	 * Returns the number of pages in this HeapFile.
	 */
	public int numPages() {
		// TODO: some code goes here
		return (int) file.length() / BufferPool.getPageSize();
	}

	// see DbFile.java for javadocs
	public List<Page> insertTuple(TransactionId tid, Tuple t)
			throws DbException, IOException, TransactionAbortedException {
		ArrayList<Page> list = new ArrayList<>();
		for (int pageNo = 0; pageNo < numPages(); pageNo++) {
			HeapPageId pageId = new HeapPageId(getId(), pageNo);
			HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pageId, Permissions.READ_WRITE);
			if (page.getNumUnusedSlots() != 0) {
				page.insertTuple(t);
				list.add(page);
				return list;
			}
		}
		BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
		byte[] emptyPage = HeapPage.createEmptyPageData();
		output.write(emptyPage);
		output.close();
		HeapPageId pageId = new HeapPageId(getId(), numPages() - 1);
		HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pageId, Permissions.READ_WRITE);
		page.insertTuple(t);
		list.add(page);
		return list;
	}

	// see DbFile.java for javadocs
	public List<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException, TransactionAbortedException {
		ArrayList<Page> list = new ArrayList<>();
		HeapPageId pageId = new HeapPageId(getId(), numPages() - 1);
		HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pageId, Permissions.READ_WRITE);
		page.deleteTuple(t);
		list.add(page);
		return list;
	}

	// see DbFile.java for javadocs
	public DbFileIterator iterator(TransactionId tid) {
		// TODO: some code goes here
		return new HeapFileIterator(this, tid);
	}

	private static final class HeapFileIterator implements DbFileIterator {
		private final HeapFile heapFile;
		private final TransactionId tid;
		private Iterator<Tuple> it;
		private int whichPage;

		public HeapFileIterator(HeapFile file, TransactionId tid) {
			this.heapFile = file;
			this.tid = tid;
		}

		@Override
		public void open() throws DbException, TransactionAbortedException {
			// TODO Auto-generated method stub
			whichPage = 0;
			it = getPageTuples(whichPage);
		}

		private Iterator<Tuple> getPageTuples(int pageNumber) throws TransactionAbortedException, DbException {
			if (pageNumber >= 0 && pageNumber < heapFile.numPages()) {
				HeapPageId pid = new HeapPageId(heapFile.getId(), pageNumber);
				HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY);
				return page.iterator();
			} else {
				throw new DbException(
						String.format("heapfile %d does not contain page %d!", pageNumber, heapFile.getId()));
			}
		}

		@Override
	        public boolean hasNext() throws DbException, TransactionAbortedException {
	            if(it == null){
	                return false;
	            }
	            if(!it.hasNext()){
	                while(whichPage < (heapFile.numPages() - 1)){
	                    whichPage++;
	                    it = getPageTuples(whichPage);
	                    if(it.hasNext()){
	                        return it.hasNext();
	                    }
	                }
	                return false;
	            }
	            return true;
	        }

		@Override
		public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
			// TODO Auto-generated method stub
			if (it == null || !it.hasNext()) {
				throw new NoSuchElementException();
			}
			return it.next();
		}

		@Override
		public void rewind() throws DbException, TransactionAbortedException {
			// TODO Auto-generated method stub
			close();
			open();
		}

		@Override
		public void close() {
			// TODO Auto-generated method stub
			it = null;
		}
	}

}