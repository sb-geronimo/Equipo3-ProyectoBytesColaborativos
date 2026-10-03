package es.bytescolab.msauth.mapper;


import es.bytescolab.msauth.dto.request.RegisterRequest;
import es.bytescolab.msauth.dto.response.RegisterResponse;
import es.bytescolab.msauth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", constant = "MANAGER")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterRequest request);

    RegisterResponse toRegisterResponse(User entity);
}
