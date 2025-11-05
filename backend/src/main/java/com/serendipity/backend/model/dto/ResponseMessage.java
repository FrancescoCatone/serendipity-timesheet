package com.serendipity.backend.model.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ResponseMessage {

    private int status;
    private String message;
    private LocalDateTime timestamp;
    private Object data;
    private Map<String, Object> meta;

    public ResponseMessage(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public ResponseMessage(int status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.data = data;
    }

    public ResponseMessage(int status, String message, Object data, Map<String, Object> meta) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.data = data;
        this.meta = meta;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }
}
