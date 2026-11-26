-- O GoogleGeocoder passou a recusar os resultados com partial_match cuja rua não é a digitada: o
-- Google devolvia um lugar parecido, às vezes em outra rua, com precisão de telhado. Coordenadas
-- aceitas antes da correção podem estar erradas e ficariam no cache por até 29 dias. O cache pode
-- ser apagado a qualquer momento (docs/delivery.md): o próximo pedido de cada endereço consulta o
-- Google de novo.
delete from geocoded_address;
