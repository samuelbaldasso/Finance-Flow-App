# ADR 003: Type-Safe Navigation Compose 2.8+ com Rotas Serializáveis

## Status
Aceito

## Contexto
Versões anteriores do Navigation Compose dependiam de rotas baseadas em strings (`"transactions/{id}"`), interpolações de argumentos manuais e deserializações frágeis propensas a `NullPointerException` e falhas em runtime. No MVP inicial do FinanceFlow, utilizou-se um seletor manual via `when(AppDestination)`, o que impossibilitava o gerenciamento natural do back stack, deep linking e navegação aninhada.

## Decisão
1. Adotar o **Navigation Compose 2.8+** com o suporte nativo a rotas fortemente tipadas (`Type-Safe Routes`) via `kotlinx.serialization`.
2. Modelar todos os destinos como `data object` (sem parâmetros) ou `data class` (com argumentos tipados) anotados com `@Serializable`:
   - `AccountsRoute`, `TransactionsRoute`, `BudgetsRoute`, `GoalsRoute`, `CardsRoute`, `ReportsRoute`, `SettingsRoute`, `NewTransactionRoute(accountId, type)`.
3. Determinar o item selecionado na `NavigationBar` utilizando `currentDestination?.hierarchy?.any { it.hasRoute(topDest.route::class) }`.
4. Utilizar `popUpTo(findStartDestination().id) { saveState = true }` e `restoreState = true` para garantir que o estado de rolagem e preenchimento de cada aba seja preservado ao alternar destinos.

## Consequências

### Positivas
- **Segurança em Tempo de Compilação:** Erros de digitação em rotas ou argumentos incompatíveis impedem a compilação do app.
- **Back Stack Padrão Android:** Suporte nativo ao gesto de retorno preditivo (`Predictive Back`) e múltiplos back stacks por aba.
- **Legibilidade Superior:** Eliminação de parsing manual de `Bundle` e chaves de strings mágicas.

### Negativas
- Dependência adicional do plugin `kotlinx.serialization` e tamanho marginalmente maior nos metadados de classe.
