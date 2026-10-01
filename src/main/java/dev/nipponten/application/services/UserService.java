package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.application.exceptions.UserNotFoundException;
import dev.nipponten.application.requests.ClientRequest;
import dev.nipponten.application.requests.ClientRequestMapper;
import dev.nipponten.application.requests.InternalRegistrationRequest;
import dev.nipponten.application.requests.InternalRequestMapper;
import dev.nipponten.application.requests.UserAddressRequest;
import dev.nipponten.application.requests.UserAddressRequestMapper;
import dev.nipponten.application.requests.UserRegistrationRequest;
import dev.nipponten.application.requests.UserRequest;
import dev.nipponten.application.requests.UserRequestMapper;
import dev.nipponten.application.responses.ClientResponse;
import dev.nipponten.application.responses.ClientResponseMapper;
import dev.nipponten.application.responses.InternalResponse;
import dev.nipponten.application.responses.InternalResponseMapper;
import dev.nipponten.application.responses.UserAddressResponse;
import dev.nipponten.application.responses.UserAddressResponseMapper;
import dev.nipponten.application.responses.UserDetailResponse;
import dev.nipponten.application.responses.UserResponse;
import dev.nipponten.application.responses.UserResponseMapper;
import dev.nipponten.domain.models.Client;
import dev.nipponten.domain.models.Internal;
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

    @Inject InternalService internalService;

    @Inject UserResponseMapper userMapper;

    @Inject ClientResponseMapper clientMapper;

    @Inject UserAddressResponseMapper userAddressMapper;

    @Inject InternalResponseMapper internalMapper;

    @Inject UserRequestMapper userRequestMapper;

    @Inject ClientRequestMapper clientRequestMapper;

    @Inject UserAddressRequestMapper userAddressRequestMapper;

    @Inject InternalRequestMapper internalRequestMapper;

    @Transactional
    public UserDetailResponse register(UserRegistrationRequest request) {
        requireUniqueEmail(request.user().email(), null);
        User savedUser = repository.save(userRequestMapper.toModel(null, request.user()));
        Client savedClient =
                clientService.create(
                        clientRequestMapper.toModel(null, savedUser.id(), request.client()));
        List<UserAddress> addresses =
                request.address() == null
                        ? List.of()
                        : List.of(
                                userAddressService.create(
                                        userAddressRequestMapper.toModel(
                                                null, savedClient.id(), request.address())));
        return userMapper.toDetailResponse(new UserProfile(savedUser, savedClient, addresses));
    }

    @Transactional
    public InternalResponse registerInternal(InternalRegistrationRequest request) {
        requireUniqueEmail(request.user().email(), null);
        User savedUser = repository.save(userRequestMapper.toModel(null, request.user()));
        Internal savedInternal =
                internalService.create(
                        internalRequestMapper.toModel(null, savedUser.id(), request.internal()));
        return internalMapper.toResponse(savedInternal);
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
        User current = getById(id);
        requireUniqueEmail(request.email(), id);
        User model = userRequestMapper.toModel(id, request);
        return userMapper.toResponse(
                repository.save(
                        new User(
                                id,
                                model.email(),
                                model.password(),
                                current.createdAt(),
                                current.active())));
    }

    public UserResponse setActive(Long id, boolean active) {
        User current = getById(id);
        return userMapper.toResponse(
                repository.save(
                        new User(
                                id,
                                current.email(),
                                current.password(),
                                current.createdAt(),
                                active)));
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
        internalService.getByUser(id).stream().map(Internal::id).forEach(internalService::delete);
        repository.remove(model);
    }

    @Transactional
    public void deleteInternal(Long internalId) {
        User user = getById(internalService.getById(internalId).userId());
        if (user.active()) {
            throw new InvalidRequestException(
                    "Internal user "
                            + internalId
                            + " must be deactivated before deletion (user "
                            + user.id()
                            + ")");
        }
        delete(user.id());
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

    private void requireUniqueEmail(String email, Long userId) {
        User existing = repository.getByEmail(email);
        if (existing != null && !existing.id().equals(userId)) {
            throw new InvalidRequestException("Email already registered: " + email);
        }
    }

    private Client requireClientOf(Long userId) {
        getById(userId);
        return clientService.getByUser(userId);
    }
}
