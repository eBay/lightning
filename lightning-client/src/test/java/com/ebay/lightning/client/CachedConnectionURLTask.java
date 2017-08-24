package com.ebay.lightning.client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CachedConnectionURLTask {

	private static final int MAX_ADDRESS = 5000;
	private static final int BATCH_SIZE = 5000;
	private static final int CONNECT_TIMEOUT = 2000;
	private static final int READ_WRITE_TIMEOUT = 2000;
	private static final int MAX_ITERATIONS = 1;
	static int iteration = 0;
	int connCount = 0;
	int writeCount = 0;
	int readCount = 0;
	int successCount = 0;
	int failureCount = 0;
	static double overallReadTime = 0;
	static double overalCacheTime = 0;

	String requestFormat = "HEAD /admin/v3console/ValidateInternals2?component=hostinformation&forceXml=true HTTP/1.1\nHost: %s\nConnection: keep-alive\n\n";

	private final Charset charset = Charset.forName("ISO-8859-1");
	private ByteBuffer responseBuffer = ByteBuffer.allocateDirect(15);
	private Selector selector;
	private List<InetSocketAddress> urls;
	private static Map<String, SocketChannel> cachedChannel = new HashMap<String, SocketChannel>();

	enum State {
		init, connected, written, read
	}

	public CachedConnectionURLTask(List<InetSocketAddress> url) throws IOException {
		this.urls = url;
		selector = SelectorProvider.provider().openSelector();
	}

	public void incrementIteration() throws IOException {
		iteration++;
		for (Entry<String, SocketChannel> entry : cachedChannel.entrySet()) {
			try {
				closeChannel(entry.getValue());
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}

		selector = SelectorProvider.provider().openSelector();
		cachedChannel = new HashMap<String, SocketChannel>();
		this.connCount = 0;
		this.writeCount = 0;
		this.readCount = 0;
		this.successCount = 0;
		this.failureCount = 0;
	}

	private void closeChannel(SocketChannel channel) throws IOException {
		channel.close();
	}

	public SocketChannel createSocketChannel(InetSocketAddress url, Selector selector) throws Exception {
		SocketChannel server = SocketChannel.open();
		server.setOption(StandardSocketOptions.TCP_NODELAY, true);
		server.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
		server.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		// server.bind(new InetSocketAddress(InetAddress.getLocalHost(), 8888
		// +iteration));
		server.configureBlocking(false);
		server.connect(url);
		server.register(selector, SelectionKey.OP_CONNECT);
		// String key = buildKey(server.socket().getLocalPort(), url);
		cachedChannel.put(cachedChannel.size() + url.getHostName(), server);
		return server;
	}

	private void cacheChannels() {
		long processStartTime = System.currentTimeMillis();
		for (int iter = iteration * BATCH_SIZE; iter < urls.size(); iter++) {
			try {
				if (cachedChannel.size() >= BATCH_SIZE) {
					break;
				}
				createSocketChannel(urls.get(iter), selector);
			} catch (Exception e) {
				// failureCount++;
			}
		}

		while (true) {
			try {
				processSelector(selector, true);
			} catch (Exception e) {
				failureCount++;
				System.out.println("Connection count/Cache size: " + connCount + "/" + cachedChannel.size());
			}
			if (connCount >= (cachedChannel.size() - failureCount) || connCount > 900 || (System.currentTimeMillis() - processStartTime) > CONNECT_TIMEOUT) {
				break;
			}
		}
		System.out.println("Failure count after connect: " + failureCount);
		System.out.println("Connection count/Cache size: " + connCount + "/" + cachedChannel.size());
	}

	public void executeURLs() throws IOException {
		for (Entry<String, SocketChannel> entry : cachedChannel.entrySet()) {
			try {
				registerForWriting(entry.getValue(), selector);
			} catch (Exception e) {
				failureCount++;
			}
		}
		System.out.println("Unable to register count: " + failureCount);
		long processStartTime = System.currentTimeMillis();
		readCount = 0;
		while (selector.keys().size() > 0) {
			processSelector(selector, false);
			if (readCount >= (connCount - failureCount) || (System.currentTimeMillis() - processStartTime) > READ_WRITE_TIMEOUT) {
				break;
			}
		}
		System.out.println("Read count: " + readCount + "/" + connCount);
		System.out.println("Success count: " + successCount);
	}

	private void registerForWriting(SocketChannel socketChannel, Selector selector) throws ClosedChannelException {
		if (socketChannel.isConnected()) {
			socketChannel.register(selector, SelectionKey.OP_WRITE, State.connected);
		} else {
			failureCount++;
		}
	}

	private void processSelector(final Selector selector, boolean onlyConnect) throws IOException {
		int selectCount = selector.selectNow();
		if (selectCount > 0) {
			for (Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext();) {
				SelectionKey selKey = iterator.next();
				iterator.remove();
				try {
					if (onlyConnect) {
						processConnect(selKey);
					} else {
						processReadWrite(selKey);
					}
				} catch (Exception e) {
					failureCount++;
				}
			}
			selector.selectedKeys().clear();
		}
	}

	public boolean processReadWrite(SelectionKey selKey) throws IOException {
		SocketChannel sChannel = (SocketChannel) selKey.channel();
		try {
			if (selKey.isReadable() && State.written.equals(selKey.attachment())) {
				readCount++;
				readFromChannel(sChannel);
				selKey.cancel();
				return true;
			} else if (selKey.isWritable() && State.connected.equals(selKey.attachment())) {
				writeCount++;
				writeToChannel(sChannel);
				selKey.attach(State.written);
				sChannel.register(selector, SelectionKey.OP_READ, State.written);
				// selKey.cancel();
			}
		} catch (Exception e) {
			failureCount++;
			// e.printStackTrace();
			// System.out.println("Error hitting " +
			// sChannel.getRemoteAddress().toString().split("/")[0]
			// +
			// "/admin/v3console/ValidateInternals2?component=hostinformation&forceXml=true");
		}
		return false;
	}

	public boolean processConnect(SelectionKey selKey) throws IOException {
		boolean success = false;
		SocketChannel sChannel = (SocketChannel) selKey.channel();
		if (selKey.isConnectable()) {
			sChannel.finishConnect();
			connCount++;
			success = true;
		} else {
			failureCount++;
		}
		return success;
	}

	private void readFromChannel(SocketChannel sChannel) throws IOException {
		// Get channel with bytes to read
		if (sChannel.read(responseBuffer) > 0) {
			int responseCode = 100 * (responseBuffer.get(9) - '0') + 10 * (responseBuffer.get(10) - '0') + 1 * (responseBuffer.get(11) - '0');
			if (responseCode >= 200 && responseCode <= 320) {
				successCount++;
			}

			responseBuffer.clear();
		}
	}

	private void writeToChannel(SocketChannel sChannel) throws IOException {
		CharBuffer requestChars = CharBuffer.wrap(String.format(requestFormat, ((InetSocketAddress) sChannel.getRemoteAddress()).getHostName()));
		ByteBuffer requestBytes = charset.encode(requestChars);
		sChannel.write(requestBytes);
		while (requestBytes.remaining() > 0) {
			sChannel.write(requestBytes);
		}
		requestBytes.clear();
	}

//	private String buildKey(int i, final InetSocketAddress url) {
//		return i + ":" + url.getHostName() + ":" + url.getPort();
//	}

	public static void main(String[] args) throws IOException, InterruptedException {
		long startTime = System.currentTimeMillis();
		System.setSecurityManager(null);
		BufferedReader reader = null;
		final List<InetSocketAddress> urls = new ArrayList<>();
		try {
			reader = new BufferedReader(new FileReader(ClassLoader.getSystemResource("500sandbox-success-urls.txt").getPath()));
			String line = null;
			System.out.println("Started Reading file");
			while ((line = reader.readLine()) != null && urls.size() < MAX_ADDRESS) {
				try {
					URI uri = new URI(line);
					urls.add(new InetSocketAddress(uri.getHost(), uri.getPort()));
				} catch (URISyntaxException e) {
					// dont add
				}
			}
			System.out.println("Done Reading file: url count " + urls.size());
			System.out.println("Total urls executed: " + BATCH_SIZE * MAX_ITERATIONS);
			System.out.println("Time taked to add socket addresses: " + (System.currentTimeMillis() - startTime) / 1000.0 + "s\n");
		} finally {
			if (reader != null) {
				reader.close();
			}
		}

		CachedConnectionURLTask task = new CachedConnectionURLTask(urls);
		for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
			if (iter != 0) {
				task.incrementIteration();
			}
			startTime = System.currentTimeMillis();
			System.out.println("------------------   Iteration " + iteration + "   ------------------------");
			task.cacheChannels();
			overalCacheTime = overalCacheTime + ((System.currentTimeMillis() - startTime) / 1000.0);
			System.out.println("Time taked to cache channels: " + (System.currentTimeMillis() - startTime) / 1000.0 + "s.\n\n");

			startTime = System.currentTimeMillis();
			task.executeURLs();
			overallReadTime = overallReadTime + ((System.currentTimeMillis() - startTime) / 1000.0);
			System.out.println("Time taken to read/write: " + (System.currentTimeMillis() - startTime) / 1000.0 + "s.\n\n");
		}

		System.out.println("Over all time taken for creating cache: " + overalCacheTime + "s.");
		System.out.println("Over all time taken for read write: " + overallReadTime + "s.");

		System.exit(0);
	}

}