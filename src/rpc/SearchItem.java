package rpc;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;

/**
 * Servlet implementation class SearchItem
 */
@WebServlet("/search" )
public class SearchItem extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SearchItem() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String userId = request.getParameter("user_id") ;
		double lat = Double.parseDouble(request.getParameter("lat")) ;
		double lon = Double.parseDouble(request.getParameter("lon")) ;
		String term = request.getParameter("term") ;
		
		DBConnection conn = DBConnectionFactory.getDBConnection() ;
		List<Item> items = conn.searchItems(userId, lat, lon, term) ;
		/**
		 * db operation
		 */
		
		List<JSONObject> list = new ArrayList<JSONObject>() ;
		Set<Item> favorite = conn.getFavoriteItems(userId) ;
		
		try {
			for(Item item : items) {
				JSONObject obj = item.toJSONObject() ;
				if(favorite != null) {
					obj.put( "favorite", favorite.contains(item.getItemId()) ) ;
				}
				list.add(obj) ;
			}
		} catch (Exception e) {
			e.printStackTrace() ;
		}
		JSONArray array = new JSONArray(list) ;
		RpcHelper.writeJsonArray(response, array) ;
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
	
	/*
	 * base on cross-origin-access, need to handle request on Option method
	 */
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		response.setHeader("Access-Control-Max-Age", "3600");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "content-type"); 
		response.setHeader("Access-Control-Allow-Methods", "GET"); 
		response.setHeader("Access-Control-Allow-Credentials" , "true");

		PrintWriter out = response.getWriter();
		out.flush();
	}
}
