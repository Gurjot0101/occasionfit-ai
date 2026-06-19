package com.occasionfit.backend.dto.res;


import lombok.Data;

@Data
public class UserResponse {
    private String name;
    private String email;
    private String image;
}