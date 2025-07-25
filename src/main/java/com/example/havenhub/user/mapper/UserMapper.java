package com.example.havenhub.user.mapper;

import com.example.havenhub.user.application.dto.ReadUserDTO;
import com.example.havenhub.user.domain.Authority;
import com.example.havenhub.user.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    ReadUserDTO readUserDTOToUser(User user);

    default String mapAuthoritiesToString(Authority authority) {
        return authority.getName();
    }

}
