package org.maya.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.maya.dto.ItemResponseDTO;
import org.maya.model.Item;
import org.maya.model.OrderItem;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ItemService {

    @Inject
    Logger logger;

    public Response getAll() {
        logger.debug("Attempting to fetch all items");
        try {
            List<Item> items = Item.listAll();
            List<ItemResponseDTO> dtos = items.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
            logger.infof("Found %d items", dtos.size());
            return Response.ok(dtos).build();
        } catch (Exception e) {
            logger.error("Failed to fetch items", e);
            return Response.serverError().entity("{\"error\": \"Failed to fetch items from database.\"}").build();
        }
    }

    @Transactional
    public Response create(Item item) {
        logger.debug("Attempting to create a new item");
        try {
            if (item == null) {
                logger.warn("Received null item for creation");
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Request body cannot be empty\"}").build();
            }

            if (item.name == null || item.name.trim().isEmpty()) {
                logger.warnf("Item creation failed: name is required. Received name: '%s'", item.name);
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Item name is required\"}").build();
            }
            item.name = item.name.trim();

            if (Item.find("name", item.name).count() > 0) {
                logger.warnf("Item creation failed: item with name '%s' already exists", item.name);
                return Response.status(Response.Status.CONFLICT) // 409 Conflict более подходит
                        .entity("{\"error\": \"Товар с таким именем уже существует\"}")
                        .build();
            }

            if (item.quantity < 0) {
                logger.warnf("Item creation failed: quantity cannot be negative. Received quantity: %d", item.quantity);
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Quantity cannot be negative\"}").build();
            }

            if (item.price < 0) {
                logger.warnf("Item creation failed: price cannot be negative. Received price: %.2f", item.price);
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Price cannot be negative\"}").build();
            }

            item.persist();
            logger.infof("Successfully created item with ID: %d, Name: '%s'", item.id, item.name);
            ItemResponseDTO dto = mapToDto(item);
            return Response.status(Response.Status.CREATED).entity(dto).build();

        } catch (Exception e) {
            logger.error("Unexpected error during item creation", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Internal server error during item creation.\", \"details\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @Transactional
    public Response update(Long id, Item updatedItem) {
        logger.debugf("Attempting to update item with ID: %d", id);
        try {
            if (updatedItem == null) {
                logger.warnf("Update item failed for ID %d: received null request body", id);
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Request body cannot be empty\"}").build();
            }

            Item item = Item.findById(id);
            if (item == null) {
                logger.warnf("Update item failed: item with ID %d not found", id);
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\": \"Item not found\"}").build();
            }

            logger.debugf("Found item %d for update. Current state: Name='%s', Quantity=%d, Price=%.2f",
                    id, item.name, (Object) item.quantity, (Object)item.price);

            boolean updated = false;

            if (updatedItem.name != null && !updatedItem.name.trim().isEmpty()) {
                String newName = updatedItem.name.trim();
                if (!item.name.equals(newName)) {
                    if (Item.find("name = ?1 and id != ?2", newName, id).count() > 0) {
                        logger.warnf("Update item failed for ID %d: item name '%s' already exists for another item", id, newName);
                        return Response.status(Response.Status.CONFLICT) // 409 Conflict
                                .entity("{\"error\": \"Товар с таким именем уже существует\"}")
                                .build();
                    }
                    logger.debugf("Updating item %d: name from '%s' to '%s'", id, item.name, newName);
                    item.name = newName;
                    updated = true;
                }
            } else {
                logger.warnf("Update item failed for ID %d: name cannot be empty", id);
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Item name is required and cannot be empty\"}").build();
            }

            if (updatedItem.quantity >= 0) {
                if (item.quantity != updatedItem.quantity) {
                    logger.debugf("Updating item %d: quantity from %d to %d", id, (Object) item.quantity, (Object) updatedItem.quantity);
                    item.quantity = updatedItem.quantity;
                    updated = true;
                }
            } else {
                logger.warnf("Update item failed for ID %d: quantity must be >= 0. Received: %d", id, (Object) updatedItem.quantity);
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Quantity must be non-negative\"}").build();
            }

            if (updatedItem.price >= 0) {
                if (item.price != updatedItem.price) {
                    logger.debugf("Updating item %d: price from %.2f to %.2f", id, (Object) item.price, (Object) updatedItem.price);
                    item.price = updatedItem.price;
                    updated = true;
                }
            } else {
                logger.warnf("Update item failed for ID %d: price must be >= 0. Received: %.2f", id, (Object) updatedItem.price);
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\": \"Price must be non-negative\"}").build();
            }

            if (updated) {
                logger.infof("Successfully updated item with ID: %d. New state: Name='%s', Quantity=%d, Price=%.2f",
                        id, item.name, (Object) item.quantity, (Object) item.price);
            } else {
                logger.info("Item " + id + " was not updated as no changes were provided or data matched existing values.");
            }
            ItemResponseDTO dto = mapToDto(item);
            return Response.ok(dto).build();

        } catch (Exception e) {
            logger.error("Unexpected error during item update for ID: " + id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Internal server error during item update.\", \"details\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @Transactional
    public Response delete(Long id) {
        logger.debugf("Attempting to delete item with ID: %d", id);
        try {
            Item item = Item.findById(id);
            if (item == null) {

                logger.warnf("Delete item failed: item with ID %d not found", id);

                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Item not found\"}")
                        .build();
            }

            long orderItemCount = OrderItem.count("item", item);
            if (orderItemCount > 0) {
                logger.warnf("Attempt to delete item ID %d failed: it is associated with %d order item(s).", id, (Object) orderItemCount);
                return Response.status(Response.Status.CONFLICT) // 409 Conflict - более подходящий статус
                        .entity("{\"error\":\"Cannot delete item: it is part of existing orders.\"}")
                        .build();
            }

            String itemName = item.name;
            item.delete();

            logger.infof("Successfully deleted item with ID: %d, Name: '%s'", id, itemName);
            return Response.noContent().build();

        } catch (Exception e) {
            logger.error("Unexpected error during item deletion for ID: " + id, e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Internal server error during item deletion.\"}")
                    .build();
        }
    }
    private ItemResponseDTO mapToDto(Item item) {
        if (item == null) {
            return null;
        }
        ItemResponseDTO dto = new ItemResponseDTO();
        dto.id = item.id;
        dto.name = item.name;
        dto.quantity = item.quantity;
        dto.price = item.price;
        return dto;
    }
}