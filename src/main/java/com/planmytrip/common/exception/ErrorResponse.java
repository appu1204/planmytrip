package com.planmytrip.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private Map<String, String> fieldErrors;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
