package hearthway.user.mapper;

import hearthway.user.application.dto.ReadUserDTO;
import hearthway.user.domain.Authority;
import hearthway.user.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    ReadUserDTO readUserDTOToUser(User user);

    default String mapAuthoritiesToString(Authority authority) {
        return authority.getName();
    }

}
