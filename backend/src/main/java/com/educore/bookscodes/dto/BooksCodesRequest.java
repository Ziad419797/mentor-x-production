package com.educore.bookscodes.dto;

import lombok.Data;

@Data
public class BooksCodesRequest {
    private String name;
    private String type;
    private String address;
    private String phone;
    private boolean sellsBooks;
    private boolean sellsCodes;
    private String notes;
}
