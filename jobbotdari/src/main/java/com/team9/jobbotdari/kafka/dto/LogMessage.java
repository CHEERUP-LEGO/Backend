package com.team9.jobbotdari.kafka.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogMessage {
    private Long userId;
    private String action;
    private String description;
}