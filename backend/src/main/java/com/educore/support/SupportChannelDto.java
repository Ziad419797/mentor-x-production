package com.educore.support;

import lombok.Data;

@Data
public class SupportChannelDto {
    private Long id;
    private String groupName;
    private String grade;
    private String label;
    private String type;   // WHATSAPP | TELEGRAM | LINK
    private String value;
    private Integer displayOrder;
}
