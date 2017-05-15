package server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import model.ClientModel;
import model.ServerModel;
import tool.Common;
import tool.Config;
import tool.Log;

public class ServerSocketSSLThread implements Runnable {
	private int sport;
	private ServerModel selfModel;
	
	
	
	public ServerSocketSSLThread(int sport, ServerModel selfModel) {
		this.sport = sport;
		this.selfModel = selfModel;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		ExecutorService pool = Executors.newCachedThreadPool();
		pool.execute(new HeartbeatThread(selfModel));
		System.setProperty("javax.net.ssl.keyStore","serverKeystore/server.jks");
		System.setProperty("javax.net.ssl.keyStorePassword","ezshare");
		try {
			//System.out.println("start to conneting through secure socket");
			Log.log(Common.getMethodName(), "INFO", "started");
			
			SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory
					.getDefault();
			SSLServerSocket sslserversocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(sport);
			//SSLSocket sslSocket = (SSLSocket)sslserversocket.accept();
			
			boolean bool = true;
			while (bool) {
				ClientModel client = new ClientModel();
				// Log.log(Common.getMethodName(), "INFO", "started");
				client.sslsocket = (SSLSocket)sslserversocket.accept();
				client.ip = client.sslsocket.getRemoteSocketAddress().toString().split(":")[0];
				long timestamp = Common.getCurrentSecTimestamp();
				if (selfModel.ipTimeList == null) {
					selfModel.ipTimeList = new HashMap<String, Long>();
				} else if (timestamp - selfModel.getLastClientTime(client.ip) < Config.CONNECTION_LIMIT_INTERVAL) {
					System.out.println("Less than connection limit interval");
					client.sslsocket.close();
					continue;
				}
				selfModel.ipTimeList.put(client.ip, timestamp);
				// client.timestamp = timestamp;

				// Timeout
				client.sslsocket.setSoTimeout(Config.CONNECTION_TIMEOUT);
				// System.out.println(client.socket.getSoTimeout());
				//System.out.println("add clients");
				selfModel.clientList.add(client);
				// TODO: Output client connection log
				// System.out.println("Connected");
				Log.log(Common.getMethodName(), "INFO",
						"New Connection:" + client.sslsocket.getRemoteSocketAddress().toString().split(":")[0] + ":"
								+ client.sslsocket.getPort());
				System.out.println("doing the operation thread");
				pool.execute(new SecureServerThread(client, selfModel));
			}
			sslserversocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			Log.log(Common.getMethodName(), "INFO", "Server fail to start: Port already in used(" + sport + ")");
			System.exit(0);
		}
	}
	
}
