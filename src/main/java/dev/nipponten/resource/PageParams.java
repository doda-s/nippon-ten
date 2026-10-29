package dev.nipponten.resource;

// Normaliza page/size vindos de query param: tamanho padrão quando ausente/inválido, teto para
// não deixar o cliente pedir a tabela inteira de uma vez.
public record PageParams(int page, int size) {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public static PageParams of(int page, int size) {
        int normalizedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new PageParams(Math.max(page, 0), normalizedSize);
    }
}
