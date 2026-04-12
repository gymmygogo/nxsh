package com.mmy.nxsh.common;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(200);
        resp.setMessage("success");
        resp.setData(data);
        return resp;
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(code);
        resp.setMessage(message);
        return resp;
    }
}

