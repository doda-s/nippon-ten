package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.ComboNotFoundException;
import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.domain.models.Combo;
import dev.nipponten.domain.models.ComboProduct;
import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.models.ProductSize;
import dev.nipponten.domain.repositories.ComboProductRepository;
import dev.nipponten.domain.repositories.ComboRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@ApplicationScoped
public class ComboService {

    public static final int MINIMUM_PRODUCTS = 2;

    @Inject ComboRepository repository;

    @Inject ComboProductRepository comboProductRepository;

    @Inject ProductService productService;

    @Inject ProductSizeService productSizeService;

    // Combo + produtos são criados juntos: a relação é feita durante o fluxo de criação e o
    // combo precisa de no mínimo dois produtos, então a operação inteira é atômica.
    @Transactional
    public Combo register(Combo model, List<ComboProduct> products) {
        requireValidWindow(model);
        products.forEach(this::requireValidProduct);
        requireNoRepeatedProducts(products);
        requireMinimumProducts(products.stream().mapToInt(ComboProduct::quantity).sum());
        Combo saved = repository.save(model);
        products.forEach(
                product ->
                        comboProductRepository.save(
                                new ComboProduct(
                                        null,
                                        saved.id(),
                                        product.productId(),
                                        product.productSizeId(),
                                        product.quantity())));
        return saved;
    }

    // A cozinha precisa saber o tamanho: ele só pode faltar se o produto não tem nenhum.
    public void requireValidProduct(ComboProduct product) {
        List<ProductSize> sizes = productSizeService.getByProduct(product.productId());
        if (product.productSizeId() == null) {
            if (!sizes.isEmpty()) {
                throw new InvalidRequestException(
                        "Product " + product.productId() + " has sizes: productSizeId is required");
            }
            return;
        }
        productSizeService.requireByProduct(product.productId(), product.productSizeId());
    }

    // O mínimo conta a soma das quantidades: 2x o mesmo pastel já é um combo.
    public void requireMinimumProducts(int totalQuantity) {
        if (totalQuantity < MINIMUM_PRODUCTS) {
            throw new InvalidRequestException(
                    "Combo must have at least "
                            + MINIMUM_PRODUCTS
                            + " products (sum of quantities), got "
                            + totalQuantity);
        }
    }

    // A mesma combinação produto+tamanho aparece uma vez; a repetição é a quantidade.
    private void requireNoRepeatedProducts(List<ComboProduct> products) {
        // Arrays.asList aceita o tamanho nulo como parte da chave.
        var seen = new HashSet<List<Long>>();
        for (ComboProduct product : products) {
            if (!seen.add(Arrays.asList(product.productId(), product.productSizeId()))) {
                throw new InvalidRequestException(
                        "Product "
                                + product.productId()
                                + " with size "
                                + product.productSizeId()
                                + " is repeated: use quantity instead");
            }
        }
    }

    public Combo getById(Long id) {
        Combo model = repository.getById(id);
        if (model == null) throw new ComboNotFoundException(id);
        return model;
    }

    public List<Combo> getAll() {
        return repository.getAll();
    }

    public PageResult<Combo> getPage(int page, int size) {
        return repository.getPage(page, size);
    }

    public Combo update(Long id, Combo model) {
        getById(id);
        requireValidWindow(model);
        return repository.save(model);
    }

    // Disponível no catálogo: ativo e dentro da janela. Sem datas, vale sempre.
    public List<Combo> getAvailable(LocalDateTime now) {
        return repository.getAvailable(now);
    }

    public PageResult<Combo> getAvailablePage(LocalDateTime now, int page, int size) {
        return repository.getAvailablePage(now, page, size);
    }

    @Transactional
    public void delete(Long id) {
        Combo model = getById(id);
        comboProductRepository.getByCombo(id).forEach(comboProductRepository::remove);
        repository.remove(model);
    }

    // O período de duração é opcional; quando informado, precisa fazer sentido.
    private void requireValidWindow(Combo model) {
        if (model.startDate() != null
                && model.endDate() != null
                && !model.endDate().isAfter(model.startDate())) {
            throw new InvalidRequestException(
                    "Combo endDate must be after startDate: "
                            + model.startDate()
                            + " -> "
                            + model.endDate());
        }
    }
}
