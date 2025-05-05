package org.maya.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.jboss.logging.Logger;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.maya.dto.OrderRequest;
import org.maya.service.OrderService;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderController {

    @Inject
    OrderService orderService;

    @Inject
    Logger logger;

    @GET
    public Response getAllOrders() {
        logger.info("Received GET request for /api/orders");
        return orderService.getAll();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        logger.infof("Received GET request for order ID: %d", id);
        return orderService.getById(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(OrderRequest request) {
        logger.infof("Received POST request to create order for address: %s", request != null ? request.address : "null");
        return orderService.create(request);
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") Long id, OrderRequest request) {
        logger.infof("Received PUT request to update order ID: %d", id);
        return orderService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        logger.infof("Received DELETE request for order ID: %d", id);
        return orderService.delete(id);
    }
}