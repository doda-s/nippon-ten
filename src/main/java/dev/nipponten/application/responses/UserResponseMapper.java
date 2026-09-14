package dev.nipponten.application.responses;

import dev.nipponten.domain.models.User;
import dev.nipponten.domain.models.UserProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserResponseMapper {

    @Inject ClientResponseMapper clientMapper;

    @Inject UserAddressResponseMapper userAddressMapper;

    public UserResponse toResponse(User user) {
        return new UserResponse(user.id(), user.email(), user.createdAt());
    }

    public UserDetailResponse toDetailResponse(UserProfile profile) {
        User user = profile.user();
        return new UserDetailResponse(
                user.id(),
                user.email(),
                user.createdAt(),
                profile.client() == null ? null : clientMapper.toResponse(profile.client()),
                profile.addresses().stream().map(userAddressMapper::toResponse).toList());
    }
}
