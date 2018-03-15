package external;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;

public class TicketMasterAPI implements ExternalAPI{
	private static final String API_HOST = "app.ticketmaster.com" ;
	private static final String SEARCH_PATH = "/discovery/v2/events.json" ;
	private static final String DEFAULT_TERM = "" ;
	private static final String API_KEY = "XgSD7KZ8Lh33Z8VUES6c4OWN6IAMfvwJ" ;
	
	@Override 
	public List<Item> search(double lat, double lon, String term) {
		
		String url = "https://" + API_HOST + SEARCH_PATH ;
		String geoHash = GeoHash.encodeGeohash(lat, lon, 4) ;
		
		if(term == null) term = DEFAULT_TERM ;
		term = URLEncodeHelper(term) ;
		
		String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=100", API_KEY, geoHash, term) ;
		
		try {
			HttpURLConnection connect = (HttpURLConnection) new URL(url + "?" + query).openConnection() ;
			connect.setRequestMethod("GET");
			
			int responseCode = connect.getResponseCode() ;
			System.out.println("Sending GET request to URL" + url + "?" + query);
			System.out.println("Response Code is " + responseCode);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(connect.getInputStream())) ;
			String inputLine ;
			StringBuffer response = new StringBuffer() ;
			while((inputLine = in.readLine()) != null) {
				response.append(inputLine) ;
			}
			in.close() ;
			
			JSONObject responseJSON = new JSONObject(response.toString()) ;
			JSONObject embedded = responseJSON.getJSONObject("_embedded") ;
			JSONArray events = embedded.getJSONArray("events") ;
			return getItemList(events) ;
		} catch (Exception e) {
			e.printStackTrace() ;
		}
		return null ;
	}
	
	private List<Item> getItemList(JSONArray events) throws JSONException {
		List<Item> itemList = new ArrayList<Item>() ;
		
		for(int i = 0 ; i < events.length() ; i++) {
			JSONObject event = events.getJSONObject(i) ;
			ItemBuilder builder = new ItemBuilder() ;
			builder.setItemId(getStringFieldOrNull(event, "id")) ;
			builder.setName(getStringFieldOrNull(event, "name")) ;
			builder.setDescription(getDescription(event)) ;
			builder.setCategories(getCategories(event)) ;
			builder.setImageUrl(getImageURL(event)) ;
			builder.setUrl(getStringFieldOrNull(event, "url")) ;
			JSONObject venue = getVenue(event) ;
			
			if(venue != null) {
				if( !venue.isNull("address") ) {
					JSONObject address = venue.getJSONObject("address") ;
					StringBuilder sb = new StringBuilder() ;
					if( !address.isNull("line1") ) {
						sb.append(address.getString("line1")) ;
					}
					if( !address.isNull("line2") ) {
						sb.append(address.getString("line2")) ;
					}
					if( !address.isNull("line3") ) {
						sb.append(address.getString("line3")) ;
					}
					builder.setAddress(sb.toString()) ;
				}
				if( !venue.isNull("city") ) {
					JSONObject city = venue.getJSONObject("city") ;
					builder.setCity(getStringFieldOrNull(city, "name")) ;
				}
				if( !venue.isNull("country") ) {
					JSONObject country = venue.getJSONObject("country") ;
					builder.setCountry(getStringFieldOrNull(country, "name")) ;
				}
				if( !venue.isNull("state") ) {
					JSONObject state = venue.getJSONObject("state") ;
					builder.setState(getStringFieldOrNull(state, "name")) ;
				}
				builder.setZipcode(getStringFieldOrNull(venue, "postalCode")) ;
				if( !venue.isNull("location") ) {
					JSONObject location = venue.getJSONObject("location") ;
					builder.setLatitude(getNumericFieldOrNull(location, "latitude")) ;
					builder.setLongitude(getNumericFieldOrNull(location, "longitude")) ;
				}
			}
			
			Item item = builder.build() ;
			itemList.add(item) ;
		}
		return itemList ;
	}
	
	private String getStringFieldOrNull(JSONObject event, String field) throws JSONException {
		return event.isNull(field) ? null : event.getString(field) ;
	}
	
	private double getNumericFieldOrNull(JSONObject event, String field) throws JSONException{
		return event.isNull(field) ? 0.0 : event.getDouble(field) ;
	}
	
	private String getDescription(JSONObject event) throws JSONException {
		if( !event.isNull("description") ) {
			return event.getString("description") ;
		}
		else if( !event.isNull("additionalInfo") ) {
			return event.getString("additionalInfo") ;
		}
		else if( !event.isNull("info") ) {
			return event.getString("info") ;
		}
		else if( !event.isNull("pleaseNote") ) {
			return event.getString("pleaseNote") ;
		}
		return null ;
	}

	private Set<String> getCategories(JSONObject event) throws JSONException{
		if( !event.isNull("classifications") ) {
			Set<String> categories = new HashSet<String>() ;
			JSONArray classifications = event.getJSONArray("classifications") ;
			for(int i = 0 ; i < classifications.length() ; i++) {
				JSONObject classification = classifications.getJSONObject(i) ;
				JSONObject segment = classification.getJSONObject("segment") ;
				categories.add(segment.getString("name")) ;
			}
			return categories ;
		}
		return null ;
	}
	
	private String getImageURL(JSONObject event) throws JSONException{
		if( !event.isNull("images") ) {
			JSONArray imagesArray = event.getJSONArray("images") ;
			if(imagesArray.length() >= 1) {
				return getStringFieldOrNull(imagesArray.getJSONObject(0), "url") ;
			}
		}
		return null ;
	}
	
	private JSONObject getVenue(JSONObject event) throws JSONException {
		if( !event.isNull("_embedded") ) {
			JSONObject embedded = event.getJSONObject("_embedded") ;
			if( !embedded.isNull("venues") ) {
				JSONArray venues = embedded.getJSONArray("venues") ;
				if(venues.length() >= 1) {
					return venues.getJSONObject(0) ;
				}
			}
		}
		return null ;
	}
	
	private String URLEncodeHelper(String term) {
		try {
			term = java.net.URLEncoder.encode(term, "UTF-8") ;
		} catch (Exception e) {
			e.printStackTrace() ;
		}
		return term ;
	}
	
	private void queryAPI(double lat, double lon) {
		List<Item> itemList = search(lat, lon, null) ;
		try {
			for(Item item : itemList) {
				JSONObject obj = item.toJSONObject() ;
				System.out.println(obj) ;
			}
		} catch (Exception e) {
			e.printStackTrace() ;
		}
	}
	
	public static void main(String args[]) {
		TicketMasterAPI test = new TicketMasterAPI() ;
		test.queryAPI(40.45, -79.95);
	}
}
