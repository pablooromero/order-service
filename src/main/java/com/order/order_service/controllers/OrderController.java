package com.order.order_service.controllers;

import com.order.order_service.dtos.*;
import com.order.order_service.exceptions.OrderException;
import com.order.order_service.exceptions.OrderItemException;
import com.order.order_service.exceptions.OrderNotFoundException;
import com.order.order_service.services.OrderItemService;
import com.order.order_service.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

    @Operation(summary = "Get all orders", description = "Retrieve a list of all orders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful retrieval of orders",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrderDTO.class),
                            examples = @ExampleObject(value = "[{\"id\": 1, \"userId\": \"1\", \"status\": \"PENDING\"}]")
                    )
            )
    })
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return orderService.getAllOrders();
    }


    @Operation(summary = "Create a new order", description = "Create a new order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation errors",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "Order status cannot be null or empty")
                    )
            )
    })

    @PostMapping
    public ResponseEntity<OrderCreateWrapperRecord> createOrder(@RequestBody CreateOrderRecord newOrder) throws OrderException {
        OrderCreateWrapperRecord createdOrder = orderService.createOrder(newOrder);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }


    @Operation(summary = "Delete a order", description = "Delete an existing order by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Order deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "Order not found with ID: 1")
                    )
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteOrder(@PathVariable Long id) throws OrderNotFoundException {
        return orderService.deleteOrder(id);
    }


    @Operation(summary = "Create a new order item", description = "Create a new order item and associate it with a order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order item created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderItemDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation errors",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "Order item orderId cannot be null or empty")
                    )
            )
    })
    @PostMapping("/{orderId}")
    public ResponseEntity<OrderItemDTO> addOrderItem(@PathVariable Long orderId,@RequestBody ProductQuantityRecord newOrderItem) throws OrderException, OrderItemException {
        OrderItemDTO orderItemRecord = orderItemService.addOrderItem(orderId, newOrderItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderItemRecord);
    }


    @Operation(summary = "Update a order item", description = "Update an existing order item by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order item updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderItemDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation errors",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "Order item productId cannot empty or null")
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Order item not found",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = "Order item not found with ID: 1")
                            }
                    )
            )
    })
    @PutMapping("/item/{orderItemId}")
    public ResponseEntity<OrderItemDTO> updateOrderItem(@PathVariable Long orderItemId, @RequestBody OrderItemUpdateRecord orderItemUpdateRecord) throws OrderItemException, OrderException {
        OrderItemDTO orderItems = orderItemService.updateOrderItemQuantity(orderItemId, orderItemUpdateRecord.quantity());
        return ResponseEntity.ok(orderItems);
    }


    @Operation(summary = "Delete a order item", description = "Delete an existing order item by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Order item deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Order item not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "Order item not found with ID: 1")
                    )
            )
    })
    @DeleteMapping("delete-item/{id}")
    public ResponseEntity<String> deleteOrderItem(@PathVariable Long id) throws OrderNotFoundException {
        return orderItemService.deleteOrderItem(id);
    }

    @PutMapping("/change-status/{orderId}")
    public ResponseEntity<OrderDTO> changeStatus(@PathVariable Long orderId, @RequestBody UpdateOrderRecord updateOrderRecord) throws OrderException {
        OrderDTO orderDTO = orderService.changeStatus(orderId, updateOrderRecord.orderStatus());
        return new ResponseEntity<>(orderDTO, HttpStatus.CREATED);
    }
}