package com.sideprojects.tradematching.mapper;

import com.sideprojects.tradematching.dto.auth.SignupResponseDto;
import com.sideprojects.tradematching.dto.user.UserInfoResponseDto;
import com.sideprojects.tradematching.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    SignupResponseDto toSignupResponseDto(User user);

    @Mapping(target = "createdAt", expression = "java(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)")
    UserInfoResponseDto toUserInfoResponseDto(User user);
}
