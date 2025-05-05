package org.maya.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.jboss.logging.Logger;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.maya.model.Item;
import org.maya.service.ItemService;

@Path("/api/items")
@Produces(MediaType.APPLICATION_JSON)
public class ItemController {

    @Inject
    ItemService itemService;

    @Inject
    Logger logger;

    @GET
    public Response getAll() {
        logger.info("Received GET request for /api/items");
        return itemService.getAll();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(Item item) {
        logger.infof("Received POST request to create item: %s", item != null ? item.name : "null");
        return itemService.create(item);
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") Long id, Item item) {
        logger.infof("Received PUT request to update item ID: %d", id);
        return itemService.update(id, item);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        logger.infof("Received DELETE request for item ID: %d", id);
        return itemService.delete(id);
    }
}
