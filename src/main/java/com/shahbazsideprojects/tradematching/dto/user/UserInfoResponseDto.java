package com.shahbazsideprojects.tradematching.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponseDto {

    private Long id;
    private String name;
    private String email;

    @JsonProperty("created_at")
    private String createdAt;
}
