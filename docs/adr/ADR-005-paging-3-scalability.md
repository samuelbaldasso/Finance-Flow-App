# ADR 005: Paginação Reativa com AndroidX Paging 3 para Escalabilidade de Extratos

## Status
Aceito

## Contexto
Extratos de finanças pessoais acumulam milhares de registros ao longo do tempo (10.000+ transações em históricos de múltiplos anos).
Carregar o histórico completo em memória com `Flow<List<Transaction>>`:
- Consome dezenas de megabytes de heap Android desnecessariamente.
- Provoca engasgos e jank severo por pressão contínua sobre a Garbage Collection (GC) ao rolar listas no Jetpack Compose.
- Degrada o tempo de resposta e o consumo de bateria do usuário.

## Decisão
1. Integrar a biblioteca **AndroidX Paging 3** (`paging-runtime-ktx`, `paging-compose`, `room-paging`).
2. Configurar o `TransactionDao` para expor consultas paginadas retornando `PagingSource<Int, TransactionEntity>`.
3. Configurar o repositório com `Pager(PagingConfig(pageSize = 20, enablePlaceholders = false))`.
4. Na camada de UI, consumir os itens paginados utilizando `collectAsLazyPagingItems()`, delegando a renderização no `LazyColumn` com chaves estáveis (`pagedTransactions.itemKey { it.id }`) e tipos de conteúdo explícitos (`pagedTransactions.itemContentType`).

## Consequências

### Positivas
- **Pegada de Memória Mínima:** Apenas as páginas visíveis e adjacentes residem em memória, mantendo o app leve mesmo com 100.000+ lançamentos.
- **Scroll a 120 FPS:** Renderização fluida e contínua sem engasgos de GC.
- **Carregamento sob Demanda:** Consultas SQL com `LIMIT`/`OFFSET` geradas automaticamente pelo Room de forma otimizada.

### Negativas
- Curva de teste ligeiramente mais complexa, exigindo `PagingData.from(list)` em fakes e utilitários específicos para testes de paginação.
