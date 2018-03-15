package algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;

public class GeoRecommendation {
	public List<Item> recommendItems(String userId, double lat, double lon) {
		DBConnection conn = DBConnectionFactory.getDBConnection() ;
		
		Set<String> favoriteItems = conn.getFavoriteItemIds(userId) ;
		
		Set<String> allCategories = new HashSet<String>() ;
		for(String item : favoriteItems) {
			allCategories.addAll(conn.getCategories(item)) ;
		}
		
		allCategories.remove("Undefined") ;
		if(allCategories.isEmpty()) {
			allCategories.add("") ;
		}
		
		Set<Item> recommendItems = new HashSet<Item>() ;
		for(String category : allCategories) {
			List<Item> items = conn.searchItems(userId, lat, lon, category) ;
			recommendItems.addAll(items) ;
		}
		
		List<Item> filterItems = new ArrayList<Item>() ;
		for(Item item : recommendItems) {
			if( !favoriteItems.contains(item.getItemId()) ) {
				filterItems.add(item) ;
			}
		}
		
		Collections.sort(filterItems, new Comparator<Item>() {

			@Override
			public int compare(Item item1, Item item2) {
				double distance1 = getDistance(item1.getLatitude(), item1.getLongitude(), lat, lon) ;
				double distance2 = getDistance(item2.getLatitude(), item2.getLongitude(), lat, lon) ;
				if(distance1 == distance2) return 0 ;
				return distance1 < distance2 ? -1 : 1 ;
			}
			
		});
		
		return filterItems ;
	}
	
	private static double getDistance(double lat1, double lon1, double lat2, double lon2) {
		double dlon = lon2 - lon1 ;
		double dlat = lat2 - lat1 ;
		double a = Math.sin(dlat / 2 / 180 * Math.PI) * Math.sin(dlat / 2 / 180 * Math.PI)
		        + Math.cos(lat1 / 180 * Math.PI) * Math.cos(lat2 / 180 * Math.PI)
	            * Math.sin(dlon / 2 / 180 * Math.PI) * Math.sin(dlon / 2 / 180 * Math.PI) ;
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) ;
		double R = 3961 ;
		return R * c ;
	}
}
