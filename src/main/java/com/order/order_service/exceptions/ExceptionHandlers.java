package com.order.order_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionHandlers {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<String> orderNotFoundExceptionHandler(OrderNotFoundException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OrderItemNotFoundException.class)
    public ResponseEntity<String> orderItemNotFoundException(OrderNotFoundException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalAttributeException.class)
    public ResponseEntity<String> illegalAttributeException(IllegalAttributeException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<String> orderExceptionHandler(OrderException orderException){
        if (orderException.getHttpStatus()!=null)
            return new ResponseEntity<>(orderException.getMessage(), orderException.getHttpStatus());
        else
            return new ResponseEntity<>(orderException.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OrderItemException.class)
    public ResponseEntity<String> orderItemExceptionHandler(OrderItemException orderItemException){
        if (orderItemException.getHttpStatus()!=null)
            return new ResponseEntity<>(orderItemException.getMessage(), orderItemException.getHttpStatus());
        else
            return new ResponseEntity<>(orderItemException.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
