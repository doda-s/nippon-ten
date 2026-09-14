package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.UserNotFoundException;
import dev.nipponten.application.requests.ClientRequest;
import dev.nipponten.application.requests.ClientRequestMapper;
import dev.nipponten.application.requests.UserAddressRequest;
import dev.nipponten.application.requests.UserAddressRequestMapper;
import dev.nipponten.application.requests.UserRegistrationRequest;
import dev.nipponten.application.requests.UserRequest;
import dev.nipponten.application.requests.UserRequestMapper;
import dev.nipponten.application.responses.ClientResponse;
import dev.nipponten.application.responses.ClientResponseMapper;
import dev.nipponten.application.responses.UserAddressResponse;
import dev.nipponten.application.responses.UserAddressResponseMapper;
import dev.nipponten.application.responses.UserDetailResponse;
import dev.nipponten.application.responses.UserResponse;
import dev.nipponten.application.responses.UserResponseMapper;
import dev.nipponten.domain.models.Client;
import dev.nipponten.domain.models.User;
import dev.nipponten.domain.models.UserAddress;
import dev.nipponten.domain.models.UserProfile;
import dev.nipponten.domain.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class UserService {

    @Inject UserRepository repository;

    @Inject ClientService clientService;

    @Inject UserAddressService userAddressService;

    @Inject UserResponseMapper userMapper;

    @Inject ClientResponseMapper clientMapper;

    @Inject UserAddressResponseMapper userAddressMapper;

    @Inject UserRequestMapper userRequestMapper;

    @Inject ClientRequestMapper clientRequestMapper;

    @Inject UserAddressRequestMapper userAddressRequestMapper;

    @Transactional
    public UserDetailResponse register(UserRegistrationRequest request) {
        User savedUser = repository.save(userRequestMapper.toModel(null, request.user()));
        Client savedClient =
                clientService.create(
                        clientRequestMapper.toModel(null, savedUser.id(), request.client()));
        return userMapper.toDetailResponse(new UserProfile(savedUser, savedClient, List.of()));
    }

    public UserDetailResponse getProfile(Long id) {
        User user = getById(id);
        Client client = clientService.findByUser(id).orElse(null);
        List<UserAddress> addresses =
                client == null ? List.of() : userAddressService.getByClient(client.id());
        return userMapper.toDetailResponse(new UserProfile(user, client, addresses));
    }

    public List<UserResponse> getAll() {
        return repository.getAll().stream().map(userMapper::toResponse).toList();
    }

    public UserResponse update(Long id, UserRequest request) {
        getById(id);
        return userMapper.toResponse(repository.save(userRequestMapper.toModel(id, request)));
    }

    @Transactional
    public void delete(Long id) {
        User model = getById(id);
        clientService
                .findByUser(id)
                .ifPresent(
                        client -> {
                            userAddressService.getByClient(client.id()).stream()
                                    .map(UserAddress::id)
                                    .forEach(
                                            addressId ->
                                                    userAddressService.delete(
                                                            client.id(), addressId));
                            clientService.delete(client.id());
                        });
        repository.remove(model);
    }

    public ClientResponse getClient(Long userId) {
        return clientMapper.toResponse(requireClientOf(userId));
    }

    public ClientResponse updateClient(Long userId, ClientRequest request) {
        Client current = requireClientOf(userId);
        return clientMapper.toResponse(
                clientService.update(
                        current.id(), clientRequestMapper.toModel(current.id(), userId, request)));
    }

    public List<UserAddressResponse> getAddresses(Long userId) {
        return userAddressService.getByClient(requireClientOf(userId).id()).stream()
                .map(userAddressMapper::toResponse)
                .toList();
    }

    public UserAddressResponse addAddress(Long userId, UserAddressRequest request) {
        Client client = requireClientOf(userId);
        return userAddressMapper.toResponse(
                userAddressService.create(
                        userAddressRequestMapper.toModel(null, client.id(), request)));
    }

    public UserAddressResponse updateAddress(
            Long userId, Long addressId, UserAddressRequest request) {
        Client client = requireClientOf(userId);
        return userAddressMapper.toResponse(
                userAddressService.update(
                        client.id(),
                        addressId,
                        userAddressRequestMapper.toModel(addressId, client.id(), request)));
    }

    public void removeAddress(Long userId, Long addressId) {
        Client client = requireClientOf(userId);
        userAddressService.delete(client.id(), addressId);
    }

    private User getById(Long id) {
        User model = repository.getById(id);
        if (model == null) throw new UserNotFoundException(id);
        return model;
    }

    private Client requireClientOf(Long userId) {
        getById(userId);
        return clientService.getByUser(userId);
    }
}
