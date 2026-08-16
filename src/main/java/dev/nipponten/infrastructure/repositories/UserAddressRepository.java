package dev.nipponten.infrastructure.repositories;

import dev.nipponten.domain.models.UserAddress;
import java.util.List;

public interface UserAddressRepository {
    UserAddress save(UserAddress model);
    void remove(UserAddress model);
    UserAddress getById(Long id);
    List<UserAddress> getAll();
    java.util.List<UserAddress> getByClient(Long clientId);
}
