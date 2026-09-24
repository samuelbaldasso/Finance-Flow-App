# ADR 004: Injeção de Dependências com Dagger Hilt

## Status
Aceito

## Contexto
O MVP foi inicialmente estruturado com um Service Locator manual (`AppContainer`). Embora essa abordagem seja excelente para bootstrap ágil e zero dependências de frameworks no primeiro dia, ela não escala para uma arquitetura industrial:
- O grafo de objetos precisa ser amarrado manualmente na `Application`.
- Injetar dependências em ViewModels exige factories manuais ou blocos `viewModel { ... }` verbosos nas chamadas de Composable.
- O ciclo de vida dos componentes fica suscetível a retenções acidentais de referências (`memory leaks`).

## Decisão
1. Adotar **Dagger Hilt** (versão 2.60+ com KSP) como a ferramenta oficial de injeção de dependências do FinanceFlow.
2. Anotar o ponto de entrada da aplicação com `@HiltAndroidApp` e a atividade principal com `@AndroidEntryPoint`.
3. Modularizar o provimento em componentes dedicados:
   - `DatabaseModule`: Room Database e todos os DAOs em escopo `@Singleton`.
   - `DataStoreModule`: Preferências de segurança e `AppLockManager` em escopo `@Singleton`.
   - `RepositoryModule`: Associação com `@Binds` de interfaces de domínio para suas respectivas implementações `@Singleton`.
4. Anotar todos os ViewModels com `@HiltViewModel` e construtores `@Inject`, permitindo a instanciação simplificada via `hiltViewModel()` nos nós de navegação.

## Consequências

### Positivas
- **Inversão de Controle Completa:** ViewModels e UseCases recebem suas dependências declarativamente sem boilerplate manual de fábrica.
- **Validação em Compilação:** Dagger valida todo o grafo de dependências antes da execução; se uma dependência estiver faltando, o build falha imediatamente.
- **Testabilidade Facilitada:** Permite a substituição seletiva de módulos de teste em instrumentação e uso direto de construtores em testes unitários puros.

### Negativas
- Tempo de compilação marginalmente acrescido pelo processamento de anotações via KSP e transformações de bytecode via plugin Gradle.
