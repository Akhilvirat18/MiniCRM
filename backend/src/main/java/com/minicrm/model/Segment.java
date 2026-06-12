package com.minicrm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "segments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Segment {
    @Id
    private String id;
    private String name;
    private String description;
    private Map<String, Object> filterRules; // JSON filter tree representation
    private long previewCount;
    private LocalDateTime createdAt;
}
