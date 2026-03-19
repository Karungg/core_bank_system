package com.miftah.core_bank_system.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebResponse<T> {

    private Integer code;

    private String message;

    private T data;

    private Object errors;

    public static <T> WebResponse<T> success(Integer code, String message, T data) {
        return WebResponse.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> WebResponse<T> error(Integer code, String message, Object errors) {
        return WebResponse.<T>builder()
                .code(code)
                .message(message)
                .errors(errors)
                .build();
    }
}
