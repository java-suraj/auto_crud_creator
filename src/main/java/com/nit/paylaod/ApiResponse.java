package com.nit.paylaod;

import java.util.List;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ApiResponse {
	private Boolean success;
	private String message;
	private Integer statusCode;
	private Object data;
	private List<String> errors;

	public ApiResponse(Boolean success, String message, Integer statusCode, Object data, List<String> errors) {
		super();
		this.success = success;
		this.message = message;
		this.statusCode = statusCode;
		this.data = data;
		this.errors = errors;
	}
}