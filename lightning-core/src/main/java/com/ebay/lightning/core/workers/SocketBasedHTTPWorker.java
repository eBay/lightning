package com.ebay.lightning.core.workers;

import static com.ebay.lightning.core.constants.LightningCoreConstants.HTTP_REQUEST_REQUEST_TEMPLATE;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;

import com.ebay.lightning.core.beans.BatchReport;
import com.ebay.lightning.core.beans.Task;
import com.ebay.lightning.core.beans.URLTask;
import com.ebay.lightning.core.config.RequestConfig;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.constants.LightningCoreConstants;
import com.ebay.lightning.core.constants.LightningCoreConstants.HttpMethod;
import com.ebay.lightning.core.constants.LightningCoreConstants.TaskStatus;
import com.ebay.lightning.core.constants.LightningCoreConstants.WorkStatus;
import com.ebay.lightning.core.constants.LightningCoreConstants.WorkerState;
import com.ebay.lightning.core.store.ExecutionDataStore;
import com.ebay.lightning.core.store.LightningRequestReport;
import com.ebay.lightning.core.utils.ChainedCheckTaskExecutionUtil;
import com.ebay.lightning.core.utils.ExecutorUtil;
import com.ebay.lightning.core.utils.InetSocketAddressCache;

/**
 * {@code SocketBasedHTTPWorker} is a java nio based implementation of {@code Worker} for URL execution.
 * The class rely on the single {@link Selector} to read from multiple channels to execute parallel URL
 * tasks asynchronously at high speed. 
 * 
 * This {@code Worker} uses the {@link InetSocketAddressCache} to speed up the connection phase. The read 
 * process only reads the data required instead of the full response there by saving considerable execution
 * time.
 * 
 * The list of URL tasks are executed in batches and the metrics are aggregated batch wise and for the full
 * task list. The result is stored in {@link ExecutionDataStore} for audit purpose.
 * 
 * The class also has retry mechanism to execute failed URL tasks again with higher timeout values. 
 * 
 * @author shashukla
 * @see InetSocketAddressCache
 * @see ExecutionDataStore
 *
 */
public class SocketBasedHTTPWorker implements Worker {

	private static final Logger log = Logger.getLogger(SocketBasedHTTPWorker.class);

	private WorkerState currentState = WorkerState.NEVER_STARTED;
	private ByteBuffer getDataBuffer = ByteBuffer.allocateDirect(32 * 1024);

	private RequestConfig requestConfig;
	private int batchId = 0;
	private int batchSize;
	private int initialBatchSize;
	private int connCount;
	private int readCount;
	private int successCount;
	private int connectFailureCount;
	private int readWriteFailureCount;

	private int connectTimeoutMillis;
	private int readWriteTimeoutMillis;
	private int slowUrlsConnectTimeoutMillis;
	private int slowUrlsReadWriteTimeoutMillis;
	private int readAccuracyPercent;
	private int connectAccuracyPercent;
	
	private Selector selector;
	private ByteBuffer responseBuffer;

	private long connectTimeInMillis;
	private long readWriteTimeInMillis;

	private InetSocketAddressCache inetCache;
	private static ExecutorUtil executorUtil = new ExecutorUtil(SystemConfig.DEFAULT_THREAD_POOL_SIZE);
	private static final byte[] HTTP_HEADER_END_PATTERN = new byte[]{13,10,13,10};
	private ExecutionDataStore executionStore;

	private List<String> inetCacheToInvalidate = new ArrayList<>();

	/**
	 * Initialized the {@code SocketBasedHTTPWorker} with the required parameters.
	 * @param inetCache the {@link InetSocketAddressCache} to speed the connection phase
	 * @param store to store the results of the execution
	 * @param systemConfig configuration related to cache, retention policy, audit etc
	 */
	public SocketBasedHTTPWorker(InetSocketAddressCache inetCache, ExecutionDataStore store, SystemConfig systemConfig) {
		this(inetCache, store, systemConfig, new RequestConfig());
	}

	/**
	 * Initialized the {@code SocketBasedHTTPWorker} with the required parameters.
	 * @param inetCache the {@link InetSocketAddressCache} to speed the connection phase
	 * @param store to store the results of the execution
	 * @param systemConfig configuration related to cache, retention policy, audit etc
	 * @param requestConfig configuration for URL task like timeout, proxy host etc
	 */
	public SocketBasedHTTPWorker(InetSocketAddressCache inetCache, ExecutionDataStore store, SystemConfig systemConfig, RequestConfig requestConfig) {
		setRequestConfig(requestConfig);
		this.responseBuffer = ByteBuffer.allocateDirect(15);
		this.inetCache = inetCache;
		this.executionStore = store;
		this.requestConfig = requestConfig;
		this.connectTimeoutMillis = requestConfig.getConnectTimeoutMillis();
		this.readWriteTimeoutMillis = requestConfig.getReadWriteTimeoutMillis();
		this.slowUrlsConnectTimeoutMillis = requestConfig.getSlowUrlsConnectTimeoutMillis();
		this.slowUrlsReadWriteTimeoutMillis = requestConfig.getSlowUrlsReadWriteTimeoutMillis();
		this.readAccuracyPercent = requestConfig.getReadAccuracyPercent();
		this.connectAccuracyPercent = requestConfig.getConnectAccuracyPercent();
		this.batchSize = systemConfig.getWorkerBatchSize();
		this.initialBatchSize = this.batchSize;
	}

	/**
	 * Get the connection timeout for failed URL tasks.
	 * @return the connection timeout for failed URL tasks
	 */
	public int getSlowUrlsConnectTimeoutMillis() {
		return slowUrlsConnectTimeoutMillis;
	}

	/**
	 * Set the connection timeout for failed URL tasks.
	 * @param slowUrlsConnectTimeoutMillis the connection timeout for failed URL tasks
	 */
	public void setSlowUrlsConnectTimeoutMillis(int slowUrlsConnectTimeoutMillis) {
		this.slowUrlsConnectTimeoutMillis = slowUrlsConnectTimeoutMillis;
	}

	/**
	 * Get the read/write timeout for failed URL tasks.
	 * @return the read/write timeout for failed URL tasks
	 */
	public int getSlowUrlsReadWriteTimeoutMillis() {
		return slowUrlsReadWriteTimeoutMillis;
	}

	/**
	 * Set the read/write timeout for failed URL tasks.
	 * @param slowUrlsReadWriteTimeoutMillis the read/write timeout for failed URL tasks
	 */
	public void setSlowUrlsReadWriteTimeoutMillis(int slowUrlsReadWriteTimeoutMillis) {
		this.slowUrlsReadWriteTimeoutMillis = slowUrlsReadWriteTimeoutMillis;
	}

	/**
	 * Get the minimum threshold for successful read to connect percentage
	 * @return successful read to connect percentage minimum threshold
	 */
	public int getReadAccuracyPercent() {
		return readAccuracyPercent;
	}

	/**
	 * Set the minimum threshold for successful read to connect percentage
	 * @param readAccuracyPercent successful read to connect percentage minimum threshold
	 */
	public void setReadAccuracyPercent(int readAccuracyPercent) {
		this.readAccuracyPercent = readAccuracyPercent;
	}

	/**
	 * Get the minimum threshold for successful connect to total request percent.
	 * @return the minimum threshold for successful connect to total request percent
	 */
	public int getConnectAccuracyPercent() {
		return connectAccuracyPercent;
	}

	/**
	 * Set the minimum threshold for successful connect to total request percent.
	 * @param connectAccuracyPercent the minimum threshold for successful connect to total request percent
	 */
	public void setConnectAccuracyPercent(int connectAccuracyPercent) {
		this.connectAccuracyPercent = connectAccuracyPercent;
	}

	/**
	 * Initialize the worker.
	 */
	@Deprecated
	@PostConstruct
	public void startWorker() {
		prepareSelector();
		this.currentState = WorkerState.IDLE;
	}

	/**
	 * Create a selector for the current batch of tasks.
	 */
	private void prepareSelector() {
		try {
			this.selector = SelectorProvider.provider().openSelector();
		} catch (IOException e) {
			this.currentState = WorkerState.BAD_STATE;
		}
		this.currentState = WorkerState.IDLE;
	}

	/**
	 * Execute the list of tasks for the request.
	 * @param sessionId the session Id corresponding to the request
	 */
	@Override
	public WorkerState execute(String sessionId) {
		LightningRequestReport report = executionStore.getReport(sessionId);
		if (report == null) {
			this.currentState = WorkerState.IDLE;
			return this.currentState;
		}
		try {
			List<Task> tasks = report.getRequest().getTasks();
			if (tasks != null && sessionId != null) {
				if (ChainedCheckTaskExecutionUtil.areChainedCheckTasks(tasks)) {
					ChainedCheckTaskExecutionUtil util = new ChainedCheckTaskExecutionUtil(tasks);
					while (util.hasMoreSubTasks()) {
						List<Task> subTasks = util.getSubNextTasks();
						executeInBatch(report, subTasks);
					}
					updateIncompleteTasksStatus(tasks);
				} else {
					executeInBatch(report, tasks);
					List<Task> tasksToRetry = updateIncompleteTasksStatus(tasks);
					
					if (requestConfig.isRetryFailedTasks() && !tasksToRetry.isEmpty()) {
						configureWorkerForRetry();
						executeInBatch(report, tasksToRetry);
						updateIncompleteTasksStatus(tasksToRetry);
					}
				}
				report.setStatus(WorkStatus.DONE);
			} else {
				report.setStatus(WorkStatus.STOPPED);
			}
		} catch (Exception e) {
			report.setStatus(WorkStatus.STOPPED);
			log.error("Error Executing request Id : " + sessionId, e);
		}finally{
			clearSelector();
		}

		this.currentState = WorkerState.IDLE;
		return this.currentState;
	}

	/**
	 * Get the list of failed tasks after execution.
	 * @param tasks the list of tasks executed
	 * @return the list of tasks that failed
	 */
	private List<Task> updateIncompleteTasksStatus(List<Task> tasks) {
		List<Task> tasksToRetry = new ArrayList<>();
		for (Task task : tasks) {
			if (task != null) {
				URLTask urlTask = (URLTask) task;
				if (TaskStatus.INIT.equals(task.getStatus()) || TaskStatus.CONNECTED.equals(task.getStatus())) {
					task.setErrorMsg("URL Connection Timeout Out with status: " + task.getStatus());
					task.setStatus(TaskStatus.TIMEDOUT);
					task.setUrl(urlTask.getCompleteURL()); // adding respective URL to taskInfo in case of any error
					tasksToRetry.add(task);
				} else if (TaskStatus.WRITTEN.equals(task.getStatus())) {
					readWriteFailureCount++;
					task.setStatus(TaskStatus.TIMEDOUT);
					task.setErrorMsg("URL Request Timeout Out. No Response Rcvd.");
					task.setUrl(urlTask.getCompleteURL()); // adding respective URL to taskInfo in case of any error
					tasksToRetry.add(task);
				}
			}
		}
		return tasksToRetry;
	}

	/**
	 * Execute the list of tasks in batches and store the result.
	 * @param report object to store the execution result data
	 * @param tasks list of tasks to be executed
	 */
	private void executeInBatch(LightningRequestReport report, List<Task> tasks) {
		currentState = WorkerState.RUNNING;
		report.setStatus(WorkStatus.RUNNING);
		int counter = 0;
		int localBatchId = 0;
		if (tasks.size() < batchSize) {
			batchSize = tasks.size();
		}
		if(tasks.size() > 0)
			prepareSelector();

		long processStartTime = System.currentTimeMillis();
		cacheInetSocketAddress(tasks);

		List<SocketChannel> channels = new ArrayList<SocketChannel>();
		for (Task task : tasks) {
			if (task instanceof URLTask) {
				counter++;
				URLTask urlTask = (URLTask) task;
				
				try {
					//InetSocketAddress inetAddress = inetCache.getInetSocketAddress(urlTask.getHost(), urlTask.getPort());
					InetSocketAddress inetAddress = null;
					if (urlTask.isUseProxyServer()) {
						urlTask.setHostIPAddress(inetCache.getInetSocketAddress(urlTask.getHost(), urlTask.getPort()).getAddress().getHostAddress());
						inetAddress = inetCache.getInetSocketAddress(requestConfig.getProxyServerHost(), requestConfig.getProxyServerPort());
					} else {
						inetAddress = inetCache.getInetSocketAddress(urlTask.getHost(), urlTask.getPort());
					}

					if (inetAddress == null) {
						connectFailureCount++;
						urlTask.setStatus(TaskStatus.CONNECT_FAILED);
						task.setErrorMsg("Inet Socket Address is null.");
						task.setUrl(urlTask.getCompleteURL()); // adding respective URL to taskInfo in case of any error
					} else {
						channels.add(createSocketChannel(inetAddress, this.selector, urlTask));
					}
				} catch (Exception e) {
					connectFailureCount++;
					urlTask.setStatus(TaskStatus.CONNECT_FAILED);
					task.setErrorMsg("Connect failed : " + e.getMessage());
					task.setUrl(urlTask.getCompleteURL()); // adding respective URL to taskInfo in case of any error
				}
				if (counter == batchSize || (counter + (localBatchId * batchSize)) == tasks.size()) {
					int completedTasks = counter + (localBatchId * batchSize);
					if (completedTasks == tasks.size()) {
						batchSize = counter;
					}
					BatchReport batchReport = new BatchReport(batchSize);
					report.getBatchReport().put(batchId, batchReport);
					batchReport.setBatchId(batchId);
					batchReport.setInetSocketAddressCreateTimeInMillis(System.currentTimeMillis() - processStartTime);
					processStartTime = System.currentTimeMillis();
					initializeCounters();
					processConnect();
					batchReport.setConnectFailureCount(connectFailureCount);
					batchReport.setConnectTimeInMillis(connectTimeInMillis);
					processReadWrite();
					batchReport.setExecutionTime(System.currentTimeMillis() - processStartTime);
					batchReport.setCurrentInetCacheSize((int) inetCache.getInetCache().size());
					batchReport.setSuccessCount(successCount);
					batchReport.setReadWriteFailureCount(readWriteFailureCount);
					batchReport.setConnectFailureCount(connectFailureCount);
					batchReport.setConnectTimeInMillis(connectTimeInMillis);
					batchReport.setReadWriteTimeInMillis(readWriteTimeInMillis);
					processStartTime = System.currentTimeMillis();
					clearSelector();
					closeChannels(channels);
					channels.clear();
					if(completedTasks < tasks.size())  // create selector only if there are more tasks to be processed.
						prepareSelector();
					counter = 0;
					batchId++;
					localBatchId++;
					batchReport.setCleanupTimeInMillis(System.currentTimeMillis() - processStartTime);
					processStartTime = System.currentTimeMillis();
				}
			}
		}
	}

	/**
	 * Caches the {@link InetSocketAddress} of all the hosts in the task list.
	 * @param tasks the list of tasks to be cached
	 */
	private void cacheInetSocketAddress(List<Task> tasks) {

		ArrayList<Future> fl = new ArrayList<Future>();
		for (final Task task : tasks) {
			fl.add(executorUtil.submit(new Callable<String>() {
				@Override
				public String call() throws Exception {
					inetCache.getInetSocketAddress(((URLTask) task).getHost(), ((URLTask) task).getPort());
					return null;
				}
			}));
		}

		for (Future f : fl) {
			try {
				f.get(10, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				//ignore
			}
		}
	}

	/**
	 * Closes the channels opened for task execution.
	 * @param channels the list of channels opened
	 */
	private void closeChannels(List<SocketChannel> channels) {
		for (SocketChannel socketChannel : channels) {
			try {
				if (socketChannel.isOpen()) {
					socketChannel.close();
				}
			} catch (Exception e) {
				// Ignore close failures and continue
			}
		}
	}

	/**
	 * Close the selector.
	 */
	private void clearSelector() {
		if (this.selector != null) {
			try {
				this.selector.close();
			} catch (IOException e) {
				// Ignore close failures
			}
			this.currentState = WorkerState.IDLE;
		}
	}

	/**
	 * Creates a socket channel in the connect operation.
	 * @param inetAddress the {@link InetSocketAddress} corresponding to the host and port
	 * @param selector the selector for the current batch
	 * @param task the task to be executed
	 * @return the socket channel
	 * @throws IOException when the connect operation fails
	 */
	private SocketChannel createSocketChannel(InetSocketAddress inetAddress, Selector selector, URLTask task) throws IOException {
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
		socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
		socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		socketChannel.configureBlocking(false);
		try {
			socketChannel.connect(inetAddress);
			task.setStatus(TaskStatus.INIT);
			socketChannel.register(selector, SelectionKey.OP_CONNECT, task);
		} catch (UnresolvedAddressException ex) {
			// This prevents any hosts that are missing in DNS
			connectFailureCount++;
			socketChannel.close();
			task.setStatus(TaskStatus.CONNECT_FAILED);
			task.setErrorMsg("Hostname can't be resolved : " + ex.getMessage());
			task.setUrl(task.getCompleteURL()); // adding respective URL to taskInfo in case of any error
			inetCacheToInvalidate.add(task.getHost() + ":" + task.getPort());//TODO invalidate this AFTER all the batches are done 
		}
		return socketChannel;
	}

	/**
	 * Gather metrics for the connect operation.
	 */
	private void processConnect() {
		long processStartTime = System.currentTimeMillis();
		while (true) {
			try {
				processSelector(this.selector, true);
			} catch (ClosedSelectorException cse) {
				break;
			} catch (Exception e) {
				// Eat any exception and let the loop retry run for defined ConnectTimeoutMillis
			}
			long processTime = System.currentTimeMillis() - processStartTime;
			if (connCount >= (batchSize - connectFailureCount)) {
				break;
			} else if (((connCount * 100) / (batchSize - connectFailureCount) >= connectAccuracyPercent) && processTime >= connectTimeoutMillis) {
				break;
			} else if (((connCount * 100) / (batchSize - connectFailureCount) >= connectAccuracyPercent) || processTime >= slowUrlsConnectTimeoutMillis) {
				break;
			}
		}

		//		log.info("Connect failure connect: " + connectFailureCount);
		//		log.info("Connection count/batch Size: " + connCount + "/" + batchSize);
		connectFailureCount = batchSize - connCount;
		connectTimeInMillis = (System.currentTimeMillis() - processStartTime);
		//		log.info("Time taken to finish connect channels: " + connectTimeInMillis / 1000.0 + "s.\n\n");
	}

	/**
	 * Register the channels for connect or read/write
	 * @param selector the {@link Selector} for the current batch
	 * @param onlyConnect true for connect and false for read/write
	 * @throws IOException when the operation fails
	 */
	private void processSelector(final Selector selector, boolean onlyConnect) throws IOException {
		int selectCount = selector.selectNow();
		if (selectCount > 0) {
			for (Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext();) {
				SelectionKey selKey = iterator.next();
				iterator.remove();
				if (onlyConnect) {
					try {
						finishConnect(selKey);
						registerForWriting(selKey);
					} catch (IOException e) {
						connectFailureCount++;
						URLTask task = (URLTask) selKey.attachment();
						task.setStatus(TaskStatus.CONNECT_FAILED);
						task.setErrorMsg("Connect failure : " + e.getMessage());
						task.setUrl(task.getCompleteURL()); // adding respective URL to taskInfo in case of any error
					}
				} else {
					processReadWrite(selKey);
				}
			}
			selector.selectedKeys().clear();
		}
	}

	/**
	 * Register the channel as complete for connect operation.
	 * @param selKey the channel
	 * @return true if successful
	 * @throws IOException when the operation fails
	 */
	private boolean finishConnect(SelectionKey selKey) throws IOException {
		boolean success = false;
		SocketChannel sChannel = (SocketChannel) selKey.channel();
		if (selKey.isConnectable()) {
			sChannel.finishConnect();
			connCount++;
			success = true;
		}
		return success;
	}

	/**
	 * Register the channel for write operation.
	 * @param selKey the channel
	 */
	private void registerForWriting(SelectionKey selKey) {
		SocketChannel sChannel = (SocketChannel) selKey.channel();
		URLTask attachement = (URLTask) selKey.attachment();
		if (sChannel.isConnected()) {
			selKey.interestOps(SelectionKey.OP_WRITE);
			attachement.setStatus(TaskStatus.CONNECTED);
		} else {
			connectFailureCount++;
			attachement.setStatus(TaskStatus.CONNECT_FAILED);
			attachement.setErrorMsg("Channel not connected & failed writing.");
		}
	}

	/**
	 * Gather the metrics for read/write operation.
	 */
	private void processReadWrite() {
		long processStartTime = System.currentTimeMillis();
		while (true) {
			try {
				processSelector(this.selector, false);
			} catch (ClosedSelectorException cse) {
				// Preventing runtime exception
				break;
			} catch (Exception e) {
				// Eat any exception and let the loop retry run for defined ConnectTimeoutMillis
			}
			long processTime = System.currentTimeMillis() - processStartTime;
			if (readCount >= connCount) {
				break;
			} else if (((readCount * 100) / connCount >= readAccuracyPercent) && processTime >= readWriteTimeoutMillis) {
				break;
			} else if (((readCount * 100) / connCount >= readAccuracyPercent) || processTime >= slowUrlsReadWriteTimeoutMillis) {
				break;
			}
		}
		//		log.info("Read count: " + readCount + "/" + connCount);
		//		log.info("Success count: " + successCount);
		//		log.info("Read Write failure count: " + readWriteFailureCount);
		readWriteTimeInMillis = (System.currentTimeMillis() - processStartTime);
		//		log.info("Time taken to read/write: " + readWriteTimeInMillis / 1000.0 + "s.\n\n");
	}

	/**
	 * Prepares the channel for read/write.
	 * @param selKey the channel
	 * @return true if the read/write operation is successful.
	 */
	private boolean processReadWrite(SelectionKey selKey) {
		SocketChannel sChannel = (SocketChannel) selKey.channel();
		URLTask attachement = (URLTask) selKey.attachment();
		try {
			if (selKey.isReadable() && TaskStatus.WRITTEN.equals(attachement.getStatus())) {
				readFromChannel(sChannel, attachement);
				readCount++;
				selKey.cancel();
				return true;
			} else if (selKey.isWritable() && TaskStatus.CONNECTED.equals(attachement.getStatus())) {
				writeToChannel(sChannel, attachement);
				attachement.setStatus(TaskStatus.WRITTEN);
				sChannel.register(this.selector, SelectionKey.OP_READ, attachement);
			} else if (sChannel.isConnected()) {
				selKey.interestOps(SelectionKey.OP_WRITE);
			}
		} catch (Exception e) {
			readWriteFailureCount++;
			attachement.setStatus(TaskStatus.READ_WRITE_FAILED);
			attachement.setErrorMsg("Read-Write Failure: " + e.getMessage());
		}
		return false;
	}

	/**
	 * Read the data from the channel.
	 * @param sChannel the channel
	 * @param task the task corresponding to the channel
	 * @throws IOException if the read operation fails
	 */
	private void readFromChannel(SocketChannel sChannel, URLTask task) throws IOException {
		if (HttpMethod.HEAD.equals(requestConfig.getMethod())) {
			readHeadResponse(sChannel, task);
		} else if (HttpMethod.GET.equals(requestConfig.getMethod())) {
			readHeadResponse(sChannel, task); //readGetResponse(sChannel, task);
		} else {
			readWriteFailureCount++;
			task.setStatus(TaskStatus.FAILED);
			task.setErrorMsg("Unsupported HTTP Method" + requestConfig.getMethod());
		}
	}

	/**
	 * Read the response for HTTP HEAD operation.
	 * @param sChannel the channel
	 * @param task the task associated with the channel
	 * @throws IOException when the read operation fails
	 */
	private void readHeadResponse(SocketChannel sChannel, URLTask task) throws IOException {
		// Get channel with bytes to read
		if (sChannel.read(this.responseBuffer) > 0) {
			readHttpStatusCode(task, responseBuffer);
			responseBuffer.clear();
		}
	}

	/**
	 * Read the HTTP status code from the response.
	 * @param task the task
	 * @param buffer the response buffer
	 * @return {@code true} for HTTP status code 200; {@code false} otherwise
	 */
	private boolean readHttpStatusCode(URLTask task, ByteBuffer buffer) {
		int responseCode = 100 * (buffer.get(9) - '0') + 10 * (buffer.get(10) - '0') + 1 * (buffer.get(11) - '0');
		task.setStatusCode(responseCode);
		if (responseCode >= 200 && responseCode <= 320) {
			successCount++;
			task.setStatus(TaskStatus.SUCCESS);
			return true;
		} else if (responseCode >= 100 && responseCode <= 511) {
			readWriteFailureCount++;
			task.setStatus(TaskStatus.FAILED);
			task.setErrorMsg("HTTP " + responseCode);
			task.setUrl(task.getCompleteURL()); // adding respective URL to taskInfo in case of any error
		} else {
			readWriteFailureCount++;
			task.setStatus(TaskStatus.FAILED);
			task.setErrorMsg("Bad HTTP URL");
			task.setUrl(task.getCompleteURL()); // adding respective URL to taskInfo in case of any error
		}
		return false;
	}

	/*
	private void readGetResponse(SocketChannel sChannel, URLTask task) throws IOException {
		try {
			// Have we discarded the HTTP response headers yet?
			boolean skippedHeaders = false;
			// The code sent by the server
			int responseCode = -1;

			// Now loop, reading data from the server channel and writing it
			// to the destination channel until the server indicates that it
			// has no more data.
			while (sChannel.read(getDataBuffer) > 0) { // Read data, and check for end
				getDataBuffer.flip(); // Prepare to extract data from buffer

				// All HTTP responses begin with a set of HTTP headers, which
				// we need to discard. The headers end with the string
				// "\r\n\r\n", or the bytes 13,10,13,10. If we haven't already
				// skipped them then do so now.
				if (!skippedHeaders) {
					// First, though, read the HTTP response code.
					// Assume that we get the complete first line of the
					// response when the first read() call returns. Assume also
					// that the first 9 bytes are the ASCII characters
					// "HTTP/1.1 ", and that the response code is the ASCII
					// characters in the following three bytes.
					if (responseCode == -1) {
						if (!readHttpStatusCode(task, getDataBuffer)) {
							return;
						}
					}

					// Now skip the rest of the headers.
					try {
						for (;;) {
							boolean matchFailed = false;
							for(byte pattern : HTTP_HEADER_END_PATTERN){
								if(getDataBuffer.get() != pattern){
									matchFailed = true;
									break;
								}
							}
							if(!matchFailed){
								skippedHeaders = true;
								break;
							}
						}
					} catch (BufferUnderflowException e) {
						// If we arrive here, it means we reached the end of
						// the buffer and didn't find the end of the headers.
						// There is a chance that the last 1, 2, or 3 bytes in
						// the buffer were the beginning of the \r\n\r\n
						// sequence, so back up a bit.
						getDataBuffer.position(getDataBuffer.position() - 3);
						// Now discard the headers we have read
						getDataBuffer.compact();
						// And go read more data from the server.
						continue;
					}
				}

				// Write the data out; drain the buffer fully.
				StringBuilder responseBody = new StringBuilder("");
				while (getDataBuffer.hasRemaining()) {
					byte[] bytes;
					if (getDataBuffer.hasArray()) {
						bytes = getDataBuffer.array();
					} else {
						bytes = new byte[getDataBuffer.remaining()];
						getDataBuffer.get(bytes);
					}
					responseBody.append(new String(bytes, "UTF-8"));
				}
				task.setBody(responseBody.toString());
			}
			successCount++;
		} finally {
			// Now that the buffer is drained, put it into fill mode
			// in preparation for reading more data into it.
			if (getDataBuffer != null) {
				getDataBuffer.clear();
			}
		}

	}*/

	/**
	 * Write the data to the channel.
	 * @param sChannel the channel to write data
	 * @param attachement the task
	 * @throws IOException when the write operation fails
	 */
	private void writeToChannel(SocketChannel sChannel, URLTask attachement) throws IOException {
		String path = attachement.getPath();
		if (attachement.isUseProxyServer()) {
			path = attachement.getCompleteURL();
		}
		String requestString = String.format(HTTP_REQUEST_REQUEST_TEMPLATE, requestConfig.getMethod().toString(), path, attachement.getHost());
		CharBuffer requestChars = CharBuffer.wrap(requestString);
		ByteBuffer requestBytes = LightningCoreConstants.DEFAULT_CHARSET.encode(requestChars);
		sChannel.write(requestBytes);
		while (requestBytes.remaining() > 0) {
			sChannel.write(requestBytes);
		}
		requestBytes.clear();
	}

	/**
	 * Initialize the counters before execution.
	 */
	private void initializeCounters() {
		this.connCount = 0;
		this.readCount = 0;
		this.successCount = 0;
		this.readWriteFailureCount = 0;
		this.connectFailureCount = 0;
	}

	/**
	 * Get the state of the {@link Worker}.
	 * @return the current state of {@code Worker}
	 */
	public WorkerState getCurrentState() {
		return currentState;
	}

	/**
	 * Get the configuration for the current tasks.
	 * @return the task configuration
	 */
	public RequestConfig getTaskConfig() {
		return requestConfig;
	}

	/**
	 * Set the configuration for the current tasks.
	 * @param taskConfig the task configuration
	 */
	public void setRequestConfig(RequestConfig taskConfig) {

		this.requestConfig = taskConfig;
	}

	/**
	 * Initialize the counter for retrying failed tasks.
	 */
	private void configureWorkerForRetry() {
		this.connectAccuracyPercent = requestConfig.getRetryConnectAccuracyPercent();
		this.readAccuracyPercent = requestConfig.getRetryReadAccuracyPercent();
		this.slowUrlsConnectTimeoutMillis = requestConfig.getRetrySlowUrlsConnectTimeoutMillis();
		this.slowUrlsReadWriteTimeoutMillis = requestConfig.getRetrySlowUrlsReadWriteTimeoutMillis();
		this.batchSize = this.initialBatchSize;
	}
	

}