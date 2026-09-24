# ADR 001: Representação Monetária Imutável com Value Class e Menor Unidade (Centavos)

## Status
Aceito

## Contexto
Aplicações financeiras exigem precisão matemática absoluta. O uso de tipos de ponto flutuante primitivos como `Float` ou `Double` introduz erros clássicos de arredondamento IEEE 754 (ex: `0.1 + 0.2 = 0.30000000000000004`), inaceitáveis em balanços bancários, faturas de cartão e conciliação contábil.
Por outro lado, o uso irrestrito de `BigDecimal` em todo o pipeline de UI e persistência no Android gera alocações excessivas no heap, impactando a Garbage Collection em renderizações de listas longas (`LazyColumn`).

## Decisão
1. Representar todo e qualquer montante monetário como um valor inteiro na menor unidade da moeda (centavos, tipo `Long`), encapsulado no Kotlin `@JvmInline value class Money(val amountMinor: Long)`.
2. Proibir categoricamente operações implícitas entre moedas distintas (`BRL` e `USD`). Conversões exigem uma entidade explícita `ExchangeRate` com data e provedor.
3. Centralizar todas as rotinas de arredondamento em modo bancário (`RoundingMode.HALF_EVEN`), garantindo conformidade com normas contábeis internacionais.
4. Delegar formatação visual exclusivamente para a camada de apresentação através de `NumberFormat.getCurrencyInstance(locale)`.

## Consequências

### Positivas
- **Precisão Cirúrgica:** Zero centavos perdidos por erros de conversão binária.
- **Alta Performance:** Por ser um `value class`, o Kotlin desempacota `Money` diretamente como primitivo `long` na JVM em tempo de execução, gerando zero alocação de objetos em loops de cálculo.
- **Segurança de Domínio:** O compilador impede a passagem acidental de um `Long` puro representando quantidade ou ID onde um valor monetário é esperado.

### Negativas
- Exige atenção constante dos desenvolvedores ao persistir valores ou receber inputs numéricos, sempre operando em centavos (ex: R$ 10,50 = `1050L`).
