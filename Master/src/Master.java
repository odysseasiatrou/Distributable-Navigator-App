import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Master {

	private static Scanner input;
	private static String ip_addr_worker1="192.168.43.244", workerPort1="4321";
	private static String ip_addr_worker2="192.168.43.144", workerPort2="4321";
	private static String ip_addr_worker3="192.168.43.210", workerPort3="4321";
	private static String ip_addr_worker="192.168.43.210" , workerPort="4320";
	private static String[] workerHashes;
	private static int[] indexingOfWorkerHashes;    
    private static List<DirectionsObject> cache;
    private static String[] dst_src_array;
	private static ServerSocket  server_socket;
	private static String src_lat, src_lon, dst_lat, dst_lon;
    
	public static void main(String[] args) throws NoSuchAlgorithmException, UnsupportedEncodingException, UnknownHostException{
		
		cache = new ArrayList<DirectionsObject>();
		indexingOfWorkerHashes = new int[3];
		String workerHash1 = getSHA1(ip_addr_worker1, workerPort1);
		
		String workerHash2 = getSHA1(ip_addr_worker2, workerPort2);
		String workerHash3 = getSHA1(ip_addr_worker3, workerPort3);
		workerHashes = new String[] {workerHash1, workerHash2, workerHash3};
		Arrays.sort(workerHashes, (p1, p2) -> p1.compareTo(p2));
		List list = (List) Arrays.asList(workerHashes);
		indexingOfWorkerHashes[0] = list.indexOf(workerHash1);
		indexingOfWorkerHashes[1] = list.indexOf(workerHash2);
		indexingOfWorkerHashes[2] = list.indexOf(workerHash3);
		Map<String,DirectionsObject> directionsList = null;
		
		server_socket = null;
		Socket connection = null;
		
				
		try{
			server_socket = new ServerSocket(4377);
			String message = "";
			
			while(true){
							
				System.out.println("waiting for connection ...");
				connection = server_socket.accept();

				ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
		
				message = "Connection succesfull !";
				out.writeObject(message);
				out.flush();
				
				String request_type = (String) in.readObject();
				message = (String) in.readObject();
				String[] srcDstCoordArray = message.split(",");
				
				src_lat = srcDstCoordArray[0];
				src_lon = srcDstCoordArray[1];
				dst_lat = srcDstCoordArray[2];
				dst_lon = srcDstCoordArray[3];
				String data= "";
				
				DirectionsObject directionsObject = new DirectionsObject();
				directionsObject.setEndLatLon(dst_lat+","+dst_lon);
				directionsObject.setStartLatLon(src_lat+","+src_lon);
				dst_src_array = new String[] {directionsObject.getStartLatLon(), directionsObject.getEndLatLon()};
				
				DirectionsObject directionItem = existsInCache(directionsObject);
				if(directionItem != null) {
					out.writeObject(directionItem.getJson_string());
					out.flush();
				}
				else if(request_type.equals("FILE"))
					directionsList = distribute_to_workers(src_lat, src_lon, dst_lat, dst_lon);
				else if(request_type.equals("API")){
					directionsObject = query_to_googleAPI(srcDstCoordArray[0], srcDstCoordArray[1], srcDstCoordArray[2], srcDstCoordArray[3]);
					cache.add(directionsObject);
					out.writeObject(directionsObject.getJson_string());
					out.flush();
				}
				
				if(directionItem == null && directionsList!=null && request_type.equals("FILE")){
					Map<String, DirectionsObject> map = directionsList;
					List<DirectionsObject> final_list = map.entrySet().stream().map(p -> p.getValue()).collect(Collectors.toList());
					EuclideanMetric metric = new EuclideanMetric();
					final String direction = directionsObject.getStartLatLon();
					Collections.sort(final_list, (DirectionsObject p1, DirectionsObject p2) ->
						Double.compare(metric.euclidean_metric(p1.getStartLatLon(),direction),
								metric.euclidean_metric(p2.getStartLatLon(),direction)));
					cache.add(final_list.get(0));
					out.writeObject(final_list.get(0).getJson_string());
					out.flush();
				}
				else if(directionItem == null && directionsList==null && request_type.equals("FILE")){
					out.writeObject("");
					out.flush();
				}
							
			}
				
		}catch(IOException ioException){
			ioException.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}finally{
			try{
				server_socket.close();
			}catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
					
	}
	
	private static Map<String,DirectionsObject> distribute_to_workers(String srcLat, String srcLon, String dstLat, String dstLon){
		int counter = 0;
		List<Map<String,DirectionsObject>> directionsList = new ArrayList<Map<String,DirectionsObject>>();
		Map<String,DirectionsObject> map;
		
		map = socketImplementation(ip_addr_worker1, workerPort1);
		if(map.isEmpty())
			counter++;
		else
			directionsList.add(map);

		map = socketImplementation(ip_addr_worker2, workerPort2);
		if(map.isEmpty())
			counter++;
		else
			directionsList.add(map);

		map = socketImplementation(ip_addr_worker3, workerPort3);
		if(map.isEmpty())
			counter++;
		else
			directionsList.add(map);

		if(counter!=3)
			return sendToReducer(ip_addr_worker, directionsList);
		return null;
	}
	
	private static Map<String,DirectionsObject> sendToReducer(String ip, List<Map<String,DirectionsObject>> dir){
		Socket requestSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		String message = "";
		Map<String,DirectionsObject> dirList1 = null;
		
		
		try{
			requestSocket = new Socket(InetAddress.getByName(ip_addr_worker), Integer.parseInt(workerPort));
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			in = new ObjectInputStream(requestSocket.getInputStream());
			
			if((message = (String) in.readObject()).equals("Connection succesfull !")){
				message = "wait for List<Map<String,DirectionsObject>> list";
				out.writeObject(message);
				out.flush();
				message = (String) in.readObject();
				if(message.equals("waiting for List<Map<String,DirectionsObject>> list")){
					out.writeObject(dir);
					out.flush();
					dirList1 = (Map<String,DirectionsObject>) in.readObject();
				}
				
			}	else {
				System.out.print("Error with sockets connection");
				return null;
			}
			
		}catch(UnknownHostException unknownHost){
				System.err.println("You are trying to connect to an unknown host!");
		}catch(IOException ioException){
				ioException.printStackTrace();
		}
		catch(ClassNotFoundException e){
			e.printStackTrace();
		}finally{
			try{
				in.close();
				out.close();
				requestSocket.close();
			}catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	   return dirList1;
	   
	}
	
	private static Map<String,DirectionsObject> socketImplementation(String ip, String port){
		Socket requestSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		String message = "";
		Map<String,DirectionsObject> directionsList = null;
		
		try{
			requestSocket = new Socket(InetAddress.getByName(ip), Integer.parseInt(port));
			
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			in = new ObjectInputStream(requestSocket.getInputStream());
			
			out.writeObject("want some new work from you");
			out.flush();
			if((message = (String) in.readObject()).equals("Connection succesfull !")){
				message = "search to your local file";
				out.writeObject(message);
				out.flush();
				
				out.writeObject(dst_src_array[0]);
				out.flush();
				
				out.writeObject(dst_src_array[1]);
				out.flush();
				directionsList = (HashMap<String,DirectionsObject>) in.readObject();
				
			}
			else {
				System.out.print("Error with sockets connection");
				return null;
			}
			
		}catch(UnknownHostException unknownHost){
				System.err.println("You are trying to connect to an unknown host!");
		}catch(IOException ioException){
				ioException.printStackTrace();
		}
		catch(ClassNotFoundException e){
			e.printStackTrace();
		}finally{
			try{
				in.close();
				out.close();
				requestSocket.close();
			}catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
		return directionsList;
	}
	
	private static DirectionsObject socketImplementation(int index){
		
		Socket requestSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		String message = "";
		String ip = null;
		String port = null;
		DirectionsObject directionsObject = null;
		
		switch(index){
		case 0:
			ip = ip_addr_worker1;
			port = workerPort1;
			break;
		case 1:
			ip = ip_addr_worker2;
			port = workerPort2;
			break;
		case 2:
			ip = ip_addr_worker3;
			port = workerPort3;
			break;
		default :
			return null;
		}
		
		try{
			requestSocket = new Socket(InetAddress.getByName(ip),Integer.parseInt(port));
			
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			in = new ObjectInputStream(requestSocket.getInputStream());
			
			out.writeObject("want some new work from you");
			out.flush();
			if((message = (String) in.readObject()).equals("Connection succesfull !")){
				message = "make a query to Google Directions API";
				out.writeObject(message);
				out.flush();
				out.writeObject(dst_src_array[0]);
				out.flush();
				
				out.writeObject(dst_src_array[1]);
				out.flush();
				directionsObject = (DirectionsObject) in.readObject();
				if(directionsObject==null)
					System.out.println(true);
				return directionsObject;
			}
			else {
				System.out.print("Error with sockets connection");
				return directionsObject;
			}
			
		}catch(UnknownHostException unknownHost){
				System.err.println("You are trying to connect to an unknown host!");
		}catch(IOException ioException){
				ioException.printStackTrace();
		}
		catch(ClassNotFoundException e){
			e.printStackTrace();
		}finally{
			try{
				in.close();
				out.close();
				requestSocket.close();
			}catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
		return directionsObject;
	}
	
	private static DirectionsObject query_to_googleAPI(String srcLat, String srcLon, String dstLat, String dstLon){
		
		int counter = 0;
		String queryHash;
		try{
			String src_lat_lon = srcLat+srcLon;
			String dst_lat_lon = dstLat+dstLon;
			queryHash = getSHA1(src_lat_lon, dst_lat_lon);
			
			counter = 0;
			for(int i = 0; i < workerHashes.length; i++){
				if(queryHash.compareTo(workerHashes[i])<0){
					counter = i;
					break;
				}
			}
		}catch(NoSuchAlgorithmException e){
			e.printStackTrace();
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
		}

		List<Integer> list = Arrays.stream(indexingOfWorkerHashes).boxed().collect(Collectors.<Integer>toList());
		counter = list.indexOf(counter);
		switch(counter){
		case 0:
			return socketImplementation(counter);
		case 1:
			return socketImplementation(counter);
		case 2:
			return socketImplementation(counter);
		default:
			break;
		}
	 	return null;
	}
	
	private static String getSHA1(String str1, String str2) throws NoSuchAlgorithmException, UnsupportedEncodingException{
		String string_to_hash = str1+str2;
		MessageDigest encryption = MessageDigest.getInstance("SHA-1");
		encryption.reset();
		encryption.update(string_to_hash.getBytes("UTF-8"));
		String sha1 = byteToHex(encryption.digest());
		return sha1;
	}

	private static String byteToHex(byte[] digest) {
		Formatter formatter = new Formatter();
		for(byte b : digest){
			formatter.format("%02x", b);
		}
		String result = formatter.toString();
		formatter.close();
		return result;
	}
	
	private static DirectionsObject existsInCache(DirectionsObject direct){
		try{
			return cache.stream().filter(p -> p.getEndLatLon().equals(direct.getEndLatLon())).
				filter(p -> p.getStartLatLon().equals(direct.getStartLatLon())).findFirst().get();
		}
		catch(NoSuchElementException e){
			return null;
		}
			
	}

}
