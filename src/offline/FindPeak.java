package offline;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.MapReduceIterable;
import com.mongodb.client.MongoDatabase;

import db.mongodb.MongoDBUtil;

public class FindPeak {
	
	private static final String COLLECTION_NAME = "logs";
	private static final String TIME = "time";
	private static final String URL_PREFIX = "/Titan";
	private static List<LocalTime> buckets = initBuckets();
	
	public static void main(String[] args) {
		MongoClient mongoClient = new MongoClient();
		MongoDatabase db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);
		
		// contstruct mapper function
		// function() {
		//   if (this.url.startsWith("/Titan")) {
		//     emit(this.time.substring(0, 5), 1);
		//   }
		// }
		
		StringBuilder sb = new StringBuilder();
		sb.append("function() {");
		sb.append("if (this.url.startsWith(\"");
		sb.append(URL_PREFIX);
		sb.append("\")) { emit(this.");
		sb.append(TIME);
		sb.append(".substring(0, 5), 1); }");
		sb.append("}");
		String map = sb.toString();
		
		// Construct reduce function
		//function(key, values) {
		//	return Arrays.sum(values)
		//}
		String reduce = "function(key, values) {return Array.sum(values);}";
		
		// MapReduce
		MapReduceIterable<Document> results = db.getCollection(COLLECTION_NAME).mapReduce(map, reduce);
		
		//Save total count to each bucket
		Map<String, Double> timeMap = new HashMap<>();
		results.forEach(new Block<Document>() {

			@Override
			public void apply(final Document document) {
				String time = findBucket(document.getString("_id"));
				Double count = document.getDouble("value");
				if (timeMap.containsKey(time)) {
					timeMap.put(time, timeMap.get(time) + count);
				} else {
					timeMap.put(time, count);
				}
			}
			
		});
		
		// Sort the list
		List<Map.Entry<String, Double>> timeList = new ArrayList<>(timeMap.entrySet());
		Collections.sort(timeList, new Comparator<Map.Entry<String, Double>>() {

			@Override
			public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
				if (o1.getValue().compareTo(o2.getValue()) > 0) {
					return 1;
				} else {
					return -1;
				}
			}
			
		});
		
		printList(timeList);
		mongoClient.close();
	}

	private static void printList(List<Map.Entry<String, Double>> timeList) {
		for (Map.Entry<String, Double> entry : timeList) {
			System.out.println("time : " + entry.getKey() + " count : " + entry.getValue());
		}
	}
	
	private static List<LocalTime> initBuckets() {
		List<LocalTime> buckets = new ArrayList<>();
		LocalTime time = LocalTime.parse("00:00");
		
		// expand the granularity, set every 15 minute as time zone
		for (int i = 0; i < 96; i++) {
			buckets.add(time);
			time = time.plusMinutes(15);
		}
		return buckets;
	}
	
	private static String findBucket(String currentTime) {
		String[] time = currentTime.split(":");
		int timeNum = Integer.parseInt(time[0]) * 60 + Integer.parseInt(time[1]);
		int index = timeNum / 15;
		return buckets.get(index).toString();
//		LocalTime curr = LocalTime.parse(currentTime);
//		int left = 0, right = buckets.size() - 1;
//		while (left < right - 1) {
//			int mid = (left + right) / 2;
//			if (buckets.get(mid).isAfter(curr)) {
//				right = mid - 1;
//			} else {
//				left = mid;
//			}
//		}
//		if (buckets.get(right).isAfter(curr)) {
//			return buckets.get(left).toString();
//		}
//		return buckets.get(right).toString();
	}
}
