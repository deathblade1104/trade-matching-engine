package com.shahbazsideprojects.tradematching.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessOrderPayload {

    private Long orderId;
    private Integer reprocessCount;
}
