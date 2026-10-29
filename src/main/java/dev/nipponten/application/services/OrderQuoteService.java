package dev.nipponten.application.services;

import dev.nipponten.application.exceptions.UnavailableItemsException;
import dev.nipponten.application.exceptions.UnavailableItemsException.Problem;
import dev.nipponten.domain.models.AdditionalIngredient;
import dev.nipponten.domain.models.Combo;
import dev.nipponten.domain.models.Ingredient;
import dev.nipponten.domain.models.OrderItem;
import dev.nipponten.domain.models.OrderItemAdditional;
import dev.nipponten.domain.models.OrderItemComboComponent;
import dev.nipponten.domain.models.OrderItemRemovedIngredient;
import dev.nipponten.domain.models.OrderQuote;
import dev.nipponten.domain.models.Product;
import dev.nipponten.domain.models.ProductIngredient;
import dev.nipponten.domain.models.ProductSize;
import dev.nipponten.domain.models.Promotion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Precifica e confere a disponibilidade dos itens de um pedido, sem gravar nada. Todo valor sai do
 * catálogo no momento da chamada: o front nunca define preço.
 */
@ApplicationScoped
public class OrderQuoteService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    @Inject BusinessTime businessTime;

    @Inject ProductService productService;

    @Inject ProductSizeService productSizeService;

    @Inject SizeService sizeService;

    @Inject IngredientService ingredientService;

    @Inject ProductIngredientService productIngredientService;

    @Inject AdditionalIngredientService additionalIngredientService;

    @Inject PromotionService promotionService;

    @Inject ComboService comboService;

    @Inject ComboProductService comboProductService;

    // Id inexistente é 404 na hora; indisponibilidade é acumulada e devolvida de uma vez.
    public OrderQuote quote(List<OrderItem> items) {
        LocalDateTime now = businessTime.now();
        List<Problem> problems = new ArrayList<>();
        List<OrderItem> priced = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            String field = "items[" + i + "]";
            OrderItem item = items.get(i);
            priced.add(
                    switch (item.type()) {
                        case PRODUCT -> priceProduct(field, item, now, problems);
                        case COMBO -> priceCombo(field, item, now, problems);
                    });
        }
        if (!problems.isEmpty()) {
            throw new UnavailableItemsException(problems);
        }
        BigDecimal subtotal = priced.stream().map(OrderItem::total).reduce(ZERO, BigDecimal::add);
        // Cupons, pontos e taxa de entrega ficam fora da primeira versão.
        return new OrderQuote(priced, subtotal, ZERO, ZERO, subtotal);
    }

    private OrderItem priceProduct(
            String field, OrderItem item, LocalDateTime now, List<Problem> problems) {
        if (item.productSizeId() == null || item.comboId() != null) {
            problems.add(new Problem(field, "A PRODUCT item needs productSizeId and no comboId"));
            return null;
        }
        ProductSize productSize = productSizeService.getById(item.productSizeId());
        Product product = productService.getById(productSize.productId());
        String sizeName = sizeService.getById(productSize.sizeId()).name();
        if (product.status() != Product.Status.ACTIVE) {
            problems.add(new Problem(field, "Product " + product.name() + " is unavailable"));
        }
        if (productSize.status() != ProductSize.Status.ACTIVE) {
            problems.add(
                    new Problem(
                            field,
                            "Size " + sizeName + " of " + product.name() + " is unavailable"));
        }
        List<OrderItemRemovedIngredient> removed = resolveRemoved(field, product, item, problems);
        requireCompositionInStock(field, product, removed, problems);
        List<OrderItemAdditional> additionals = resolveAdditionals(field, product, item, problems);

        BigDecimal originalPrice = productSize.price();
        Optional<Promotion> promotion = promotionService.getActiveForProduct(product.id(), now);
        BigDecimal unitPrice =
                promotion
                        .map(active -> promotionService.getPromotionalPrice(active, originalPrice))
                        .orElse(originalPrice);
        BigDecimal additionalsPerUnit =
                additionals.stream()
                        .map(a -> a.unitPrice().multiply(BigDecimal.valueOf(a.quantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total =
                unitPrice.add(additionalsPerUnit).multiply(BigDecimal.valueOf(item.quantity()));

        return new OrderItem(
                null,
                null,
                OrderItem.Type.PRODUCT,
                productSize.id(),
                null,
                promotion.map(Promotion::id).orElse(null),
                product.name(),
                sizeName,
                item.quantity(),
                money(originalPrice),
                money(unitPrice),
                money(total),
                item.notes(),
                additionals,
                removed,
                List.of());
    }

    // Só se remove o que faz parte da composição; remover não muda o preço.
    private List<OrderItemRemovedIngredient> resolveRemoved(
            String field, Product product, OrderItem item, List<Problem> problems) {
        Set<Long> composition =
                productIngredientService.getByProduct(product.id()).stream()
                        .map(ProductIngredient::ingredientId)
                        .collect(Collectors.toSet());
        Set<Long> seen = new HashSet<>();
        List<OrderItemRemovedIngredient> removed = new ArrayList<>();
        for (int j = 0; j < item.removedIngredients().size(); j++) {
            String removedField = field + ".removedIngredientIds[" + j + "]";
            Long ingredientId = item.removedIngredients().get(j).ingredientId();
            if (!seen.add(ingredientId)) {
                problems.add(new Problem(removedField, "Ingredient " + ingredientId + " repeated"));
            } else if (!composition.contains(ingredientId)) {
                problems.add(
                        new Problem(
                                removedField,
                                "Ingredient "
                                        + ingredientId
                                        + " is not part of "
                                        + product.name()));
            } else {
                Ingredient ingredient = ingredientService.getById(ingredientId);
                removed.add(new OrderItemRemovedIngredient(null, ingredientId, ingredient.name()));
            }
        }
        return removed;
    }

    // Um ingrediente esgotado só bloqueia o item se o cliente não o tirou do pastel.
    private void requireCompositionInStock(
            String field,
            Product product,
            List<OrderItemRemovedIngredient> removed,
            List<Problem> problems) {
        Set<Long> removedIds =
                removed.stream()
                        .map(OrderItemRemovedIngredient::ingredientId)
                        .collect(Collectors.toSet());
        productIngredientService.getByProduct(product.id()).stream()
                .map(ProductIngredient::ingredientId)
                .filter(ingredientId -> !removedIds.contains(ingredientId))
                .map(ingredientService::getById)
                .filter(ingredient -> ingredient.status() == Ingredient.Status.OUT_OF_STOCK)
                .forEach(
                        ingredient ->
                                problems.add(
                                        new Problem(
                                                field,
                                                "Ingredient "
                                                        + ingredient.name()
                                                        + " of "
                                                        + product.name()
                                                        + " is out of stock")));
    }

    private List<OrderItemAdditional> resolveAdditionals(
            String field, Product product, OrderItem item, List<Problem> problems) {
        Map<Long, AdditionalIngredient> offered =
                additionalIngredientService.getByProduct(product.id()).stream()
                        .collect(
                                Collectors.toMap(
                                        AdditionalIngredient::ingredientId, Function.identity()));
        Set<Long> seen = new HashSet<>();
        List<OrderItemAdditional> additionals = new ArrayList<>();
        for (int j = 0; j < item.additionals().size(); j++) {
            String additionalField = field + ".additionals[" + j + "]";
            OrderItemAdditional requested = item.additionals().get(j);
            Ingredient ingredient = ingredientService.getById(requested.ingredientId());
            AdditionalIngredient config = offered.get(ingredient.id());
            if (!seen.add(ingredient.id())) {
                problems.add(
                        new Problem(
                                additionalField,
                                "Additional "
                                        + ingredient.name()
                                        + " repeated: use quantity instead"));
            } else if (config == null) {
                problems.add(
                        new Problem(
                                additionalField,
                                ingredient.name()
                                        + " is not offered as an additional for "
                                        + product.name()));
            } else if (config.status() != AdditionalIngredient.Status.ACTIVE) {
                problems.add(
                        new Problem(
                                additionalField,
                                "Additional " + ingredient.name() + " is unavailable"));
            } else if (requested.quantity() > config.maximumQuantity()) {
                problems.add(
                        new Problem(
                                additionalField,
                                "Additional "
                                        + ingredient.name()
                                        + " allows at most "
                                        + config.maximumQuantity()));
            } else if (ingredient.status() == Ingredient.Status.OUT_OF_STOCK) {
                problems.add(
                        new Problem(
                                additionalField,
                                "Ingredient " + ingredient.name() + " is out of stock"));
            } else {
                additionals.add(
                        new OrderItemAdditional(
                                null,
                                ingredient.id(),
                                ingredient.name(),
                                requested.quantity(),
                                money(ingredient.price())));
            }
        }
        return additionals;
    }

    // O combo tem preço próprio: tamanhos, promoções e adicionais não entram na conta.
    private OrderItem priceCombo(
            String field, OrderItem item, LocalDateTime now, List<Problem> problems) {
        if (item.comboId() == null
                || item.productSizeId() != null
                || !item.additionals().isEmpty()
                || !item.removedIngredients().isEmpty()) {
            problems.add(
                    new Problem(
                            field,
                            "A COMBO item needs comboId only: no productSizeId, additionals or"
                                    + " removed ingredients"));
            return null;
        }
        Combo combo = comboService.getById(item.comboId());
        if (!comboService.isAvailable(combo, now)) {
            problems.add(new Problem(field, "Combo " + combo.name() + " is unavailable"));
        }
        List<OrderItemComboComponent> components =
                comboProductService.getByCombo(combo.id()).stream()
                        .map(
                                component ->
                                        new OrderItemComboComponent(
                                                null,
                                                component.productId(),
                                                component.productSizeId(),
                                                productService
                                                        .getById(component.productId())
                                                        .name(),
                                                component.productSizeId() == null
                                                        ? null
                                                        : sizeNameOf(component.productSizeId()),
                                                component.quantity()))
                        .toList();
        BigDecimal total = combo.price().multiply(BigDecimal.valueOf(item.quantity()));
        return new OrderItem(
                null,
                null,
                OrderItem.Type.COMBO,
                null,
                combo.id(),
                null,
                combo.name(),
                null,
                item.quantity(),
                money(combo.price()),
                money(combo.price()),
                money(total),
                item.notes(),
                List.of(),
                List.of(),
                components);
    }

    private String sizeNameOf(Long productSizeId) {
        return sizeService.getById(productSizeService.getById(productSizeId).sizeId()).name();
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
