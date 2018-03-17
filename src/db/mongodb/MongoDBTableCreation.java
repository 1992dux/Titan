package db.mongodb;

import java.text.ParseException;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;

/*
 * Create table for MongoDB (for all pipelines)
 */
public class MongoDBTableCreation {
	/*
	 * run as application to create MongoDB table with index
	 */
	public static void main(String[] args) throws ParseException{
		MongoClient mongoClient = new MongoClient();
		MongoDatabase db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);
		
		// remove old table
		db.getCollection("users").drop();
		db.getCollection("items").drop();
		
		// create tables, populate data and create index
		db.getCollection("users").insertOne(new Document().append("first_name", "John").append("last_name", "Smith")
				.append("password", "3229c1097c00d497a0fd282d586be050").append("user_id", "1111"));
		
		// user_id is unique
		IndexOptions indexOptions = new IndexOptions().unique(true);
		
		// 1 for ascending, -1 for descending index
		// different from MySQL, MongoDB has history info
		db.getCollection("users").createIndex(new Document("user_id", 1), indexOptions);
		
		// item_id is unique
		// different from MySQL, MongoDB has Category info
		db.getCollection("items").createIndex(new Document("item_id", 1), indexOptions);
		
		mongoClient.close();
		System.out.println("Import is done succesfully");
	}
}
