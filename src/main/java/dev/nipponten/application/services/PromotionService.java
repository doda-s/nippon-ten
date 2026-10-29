package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.InvalidRequestException;
import dev.nipponten.application.exceptions.PromotionNotFoundException;
import dev.nipponten.domain.models.Combo;
import dev.nipponten.domain.models.PageResult;
import dev.nipponten.domain.models.Promotion;
import dev.nipponten.domain.models.PromotionPrice;
import dev.nipponten.domain.models.PromotionPricing;
import dev.nipponten.domain.models.PromotionType;
import dev.nipponten.domain.repositories.PromotionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class PromotionService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Inject PromotionRepository repository;

    @Inject ProductService productService;

    @Inject ComboService comboService;

    @Inject ProductSizeService productSizeService;

    @Inject PromotionTypeService promotionTypeService;

    public Promotion create(Promotion model) {
        requireValidWindow(model);
        requireItems(model);
        requireReferences(model);
        requireNoOverlap(model, null);
        return repository.save(model);
    }

    public Promotion getById(Long id) {
        Promotion model = repository.getById(id);
        if (model == null) throw new PromotionNotFoundException(id);
        return model;
    }

    public List<Promotion> getAll() {
        return repository.getAll();
    }

    public PageResult<Promotion> getPage(int page, int size) {
        return repository.getPage(page, size);
    }

    public Promotion update(Long id, Promotion model) {
        getById(id);
        requireValidWindow(model);
        requireItems(model);
        requireReferences(model);
        requireNoOverlap(model, id);
        return repository.save(model);
    }

    public void delete(Long id) {
        Promotion model = getById(id);
        repository.remove(model);
    }

    // Disponível no catálogo: ativa e dentro da janela.
    public List<Promotion> getAvailable(LocalDateTime now) {
        return repository.getAvailable(now);
    }

    public PageResult<Promotion> getAvailablePage(LocalDateTime now, int page, int size) {
        return repository.getAvailablePage(now, page, size);
    }

    public List<Promotion> getByProduct(Long productId) {
        return repository.getByProduct(productId);
    }

    public List<Promotion> getByPromotionType(Long promotionTypeId) {
        return repository.getByPromotionType(promotionTypeId);
    }

    public List<Promotion> getByCombo(Long comboId) {
        return repository.getByCombo(comboId);
    }

    public PromotionPricing getPrices(Promotion promotion) {
        PromotionType promotionType = promotionTypeService.getById(promotion.promotionTypeId());
        return new PromotionPricing(
                promotion.productIds().stream()
                        .map(productId -> productPricing(productId, promotionType))
                        .toList(),
                promotion.comboIds().stream()
                        .map(comboId -> comboPricing(comboId, promotionType))
                        .toList());
    }

    // O preço do produto vive no tamanho, então o desconto é calculado para cada um.
    private PromotionPricing.ProductPricing productPricing(
            Long productId, PromotionType promotionType) {
        return new PromotionPricing.ProductPricing(
                productId,
                productSizeService.getByProduct(productId).stream()
                        .map(
                                size ->
                                        new PromotionPrice(
                                                size.id(),
                                                size.price(),
                                                applyDiscount(size.price(), promotionType)))
                        .toList());
    }

    private PromotionPricing.ComboPricing comboPricing(Long comboId, PromotionType promotionType) {
        Combo combo = comboService.getById(comboId);
        return new PromotionPricing.ComboPricing(
                combo.id(), combo.price(), applyDiscount(combo.price(), promotionType));
    }

    private BigDecimal applyDiscount(BigDecimal price, PromotionType promotionType) {
        BigDecimal discounted =
                switch (promotionType.type()) {
                    case PERCENTAGE_DISCOUNT ->
                            price.multiply(ONE_HUNDRED.subtract(promotionType.value()))
                                    .divide(ONE_HUNDRED);
                    case FIXED_DISCOUNT -> price.subtract(promotionType.value());
                };
        return discounted.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    // Uma promoção vale para um ou mais produtos e/ou combos, cada um informado uma vez só.
    private void requireItems(Promotion model) {
        if (model.productIds().isEmpty() && model.comboIds().isEmpty()) {
            throw new InvalidRequestException(
                    "Promotion must apply to at least one product or combo");
        }
        requireNoDuplicates(model.productIds(), "product");
        requireNoDuplicates(model.comboIds(), "combo");
    }

    private static void requireNoDuplicates(List<Long> ids, String label) {
        var seen = new HashSet<Long>();
        ids.stream()
                .filter(id -> !seen.add(id))
                .findFirst()
                .ifPresent(
                        id -> {
                            throw new InvalidRequestException(
                                    "Duplicated " + label + " in promotion: " + id);
                        });
    }

    private void requireReferences(Promotion model) {
        model.productIds().forEach(productService::getById);
        model.comboIds().forEach(comboService::getById);
        promotionTypeService.getById(model.promotionTypeId());
    }

    // Um item não pode estar em duas promoções com períodos sobrepostos: o preço final seria
    // ambíguo. Promoções em sequência (agendar a próxima) continuam permitidas.
    private void requireNoOverlap(Promotion model, Long promotionId) {
        model.productIds()
                .forEach(
                        productId ->
                                repository
                                        .getOverlappingByProduct(
                                                productId, model.startDate(), model.endDate())
                                        .stream()
                                        .filter(
                                                existing ->
                                                        !Objects.equals(existing.id(), promotionId))
                                        .findFirst()
                                        .ifPresent(
                                                existing -> {
                                                    throw overlap("Product", productId, existing);
                                                }));
        model.comboIds()
                .forEach(
                        comboId ->
                                repository
                                        .getOverlappingByCombo(
                                                comboId, model.startDate(), model.endDate())
                                        .stream()
                                        .filter(
                                                existing ->
                                                        !Objects.equals(existing.id(), promotionId))
                                        .findFirst()
                                        .ifPresent(
                                                existing -> {
                                                    throw overlap("Combo", comboId, existing);
                                                }));
    }

    private static InvalidRequestException overlap(String label, Long id, Promotion existing) {
        return new InvalidRequestException(
                label
                        + " "
                        + id
                        + " already has promotion "
                        + existing.id()
                        + " in an overlapping period");
    }

    private void requireValidWindow(Promotion model) {
        if (!model.endDate().isAfter(model.startDate())) {
            throw new InvalidRequestException(
                    "Promotion endDate must be after startDate: "
                            + model.startDate()
                            + " -> "
                            + model.endDate());
        }
    }
}
