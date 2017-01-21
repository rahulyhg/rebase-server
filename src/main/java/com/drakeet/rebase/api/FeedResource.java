package com.drakeet.rebase.api;

import com.drakeet.rebase.api.tool.Authorizations;
import com.drakeet.rebase.api.tool.Log;
import com.drakeet.rebase.api.tool.Resource;
import com.drakeet.rebase.api.type.Feed;
import com.drakeet.rebase.api.type.Result;
import com.drakeet.rebase.api.type.User;
import com.mongodb.client.FindIterable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.bson.Document;

import static com.drakeet.rebase.api.tool.ObjectIds.objectId;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Sorts.descending;

/**
 * @author drakeet
 */
@Path("categories/{username}/{category}/feeds") public class FeedResource extends Resource {

    public FeedResource() {
        super("feed");
    }


    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response readList(
        @PathParam("username") String username,
        @PathParam("category") String category,
        @QueryParam("last_id") String lastId,
        @QueryParam("size") int size) {

        List<Feed> feeds = new ArrayList<>();
        FindIterable<Document> iterable = collection.find();
        if (lastId != null) {
            iterable.filter(lt("_id", objectId(lastId)));
        }
        iterable.sort(descending("_id"))
            .filter(eq("category", category))
            .filter(eq("owner", username))
            .limit(size)
            .forEach((Consumer<Document>) document -> {
                final Feed feed = new Feed();
                feed._id = document.getObjectId("_id");
                feed.title = document.getString("title");
                feed.content = document.getString("content");
                feed.url = document.getString("url");
                feed.category = document.getString("category");
                feed.owner = document.getString("author");
                feed.cover = document.getString("cover");
                feed.publishedAt = document.getDate("published_at");
                feeds.add(feed);
            });
        return Response.ok(feeds).build();
    }


    @POST @Consumes(MediaType.APPLICATION_JSON)
    public Response newFeed(Feed feed, @HeaderParam("Authorization") String auth) {
        Log.i("[newFeed]", feed.toJson() + " Authorization: " + auth);
        final User user = Authorizations.verify(null, auth);
        if (user != null) {
            // TODO: 2017/1/19 avoid passing all fields.
            Document document = Document.parse(feed.toJson());
            document.put("published_at", new Date());
            collection.insertOne(document);
            return Response.ok(document).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Result.failure("Unauthorized"))
                .build();
        }
    }
}
