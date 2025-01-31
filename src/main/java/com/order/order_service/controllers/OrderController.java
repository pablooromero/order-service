package com.order.order_service.controllers;

import com.order.order_service.config.JwtUtils;
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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private JwtUtils jwtUtils;

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
    @GetMapping("/admin")
    public ResponseEntity<Set<OrderDTO>> getAllOrders() {
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

    @GetMapping("/user")
    public ResponseEntity<Set<OrderDTO>> getAllOrdersByUserId(HttpServletRequest request) {
        Long id = jwtUtils.getIdFromToken(request.getHeader("Authorization"));

        return orderService.getAllOrdersByUserId(id);
    }


    @GetMapping("/admin/{userId}")
    public ResponseEntity<Set<OrderDTO>> getAllOrdersByUserId(@PathVariable Long userId) {
        return orderService.getAllOrdersByUserId(userId);
    }

    @GetMapping("/user/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId, HttpServletRequest request) throws OrderException {
        Long userId = jwtUtils.getIdFromToken(request.getHeader("Authorization"));
        return orderService.getOrderByUserId(userId, orderId);
    }

    @GetMapping("/admin/{orderId}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId) throws OrderException {
        return orderService.getOrderById(orderId);
    }


    @PostMapping("/user")
    public ResponseEntity<OrderCreatedRecord> createOrder(@RequestBody NewOrderRecord newOrder, HttpServletRequest request) throws OrderException {
        String email = jwtUtils.getEmailFromToken(request.getHeader("Authorization"));
        return orderService.createOrder(email, newOrder);
    }

    @PutMapping("/user/{orderId}")
    public ResponseEntity<OrderDTO> changeStatus(@PathVariable Long orderId, @RequestBody UpdateOrderRecord updateOrderRecord, HttpServletRequest request) throws OrderException {
        Long userId = jwtUtils.getIdFromToken(request.getHeader("Authorization"));
        String email = jwtUtils.getEmailFromToken(request.getHeader("Authorization"));
        return orderService.changeStatus(userId, email, orderId, updateOrderRecord.orderStatus());
    }

    @DeleteMapping("/user/{orderId}")
    public ResponseEntity<String> deleteOrder(@PathVariable Long orderId, HttpServletRequest request) throws OrderNotFoundException, OrderException {
        Long userId = jwtUtils.extractId(request.getHeader("Authorization"));

        return orderService.deleteOrderUser(userId, orderId);
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
    @DeleteMapping("/admin/{orderId}")
    public ResponseEntity<String> deleteOrder(@PathVariable Long orderId) throws OrderNotFoundException {
        return orderService.deleteOrder(orderId);
    }




    /**
     * ORDER ITEMS
     * --------------------------------------------------------------------------------------------------------------------------------
     * */

    @GetMapping("/user/item/{orderId}")
    public ResponseEntity<Set<OrderItemRecord>> getAllOrderItemsByOrderId(@PathVariable Long orderId, HttpServletRequest request) throws OrderException {
        Long userId = jwtUtils.getIdFromToken(request.getHeader("Authorization"));

        return orderItemService.getAllOrderItemsByOrderId(userId, orderId);
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
    @PostMapping("/user/item/{orderId}")
    public ResponseEntity<OrderItemRecord> addOrderItem(@PathVariable Long orderId, @RequestBody ProductQuantityRecord newOrderItem, HttpServletRequest request) throws OrderException, OrderItemException {
        Long userId = jwtUtils.getIdFromToken(request.getHeader("Authorization"));
        return orderItemService.addOrderItem(userId, orderId, newOrderItem);
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
    @PutMapping("/user/item/{orderItemId}")
    public ResponseEntity<OrderItemRecord> updateOrderItem(@PathVariable Long orderItemId, @RequestBody OrderItemUpdateRecord orderItemUpdateRecord, HttpServletRequest request) throws OrderItemException, OrderException {
        Long userId = jwtUtils.getIdFromToken(request.getHeader("Authorization"));
        return orderItemService.updateOrderItemQuantity(userId, orderItemId, orderItemUpdateRecord.quantity());
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
    @DeleteMapping("user/item/{orderItemId}")
    public ResponseEntity<String> deleteOrderItem(@PathVariable Long orderItemId, HttpServletRequest request) throws OrderNotFoundException, OrderItemException, OrderException {
        Long userId = jwtUtils.getIdFromToken(request.getHeader("Authorization"));
        return orderItemService.deleteOrderItem(userId, orderItemId);
    }

}