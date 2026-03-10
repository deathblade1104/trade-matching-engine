package com.shahbazsideprojects.tradematching.mapper;

import com.shahbazsideprojects.tradematching.dto.auth.SignupResponseDto;
import com.shahbazsideprojects.tradematching.dto.user.UserInfoResponseDto;
import com.shahbazsideprojects.tradematching.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    SignupResponseDto toSignupResponseDto(User user);

    @Mapping(target = "createdAt", expression = "java(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)")
    UserInfoResponseDto toUserInfoResponseDto(User user);
}
