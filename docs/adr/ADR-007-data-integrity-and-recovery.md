# ADR 007: Integridade, evolução do banco e recuperação de exclusão

## Status
Aceito

## Contexto
Persistir uma operação financeira e gravar sua auditoria separadamente permite que uma falha produza registros sem histórico correspondente. Atualizações do aplicativo também devem preservar dados existentes. Room e DataStore não participam de uma transação comum.

## Decisão

- Criação, alteração, exclusão e conciliação de transações executam leituras, validação, gravação e auditoria no mesmo `RoomDatabase.withTransaction`. Mutações de contas e categorias seguem a mesma regra.
- Transferências preservam as duas pernas, propagam valor, datas e status, e auditam cada perna. Contas distintas devem usar a mesma moeda; uma conversão exige um fluxo explícito futuro.
- O saldo disponível inclui somente lançamentos efetivados ou conciliados. Pendências continuam armazenadas e visíveis no extrato.
- A versão 2 do banco adiciona o índice `account_id,status`. A migração 1 → 2 é explícita e testada com registros da versão anterior. Não há migração destrutiva automática.
- A exportação usa um snapshot transacional do banco e inclui toda a auditoria, faturas e campos dos modelos. As preferências públicas são lidas separadamente; PIN e arquivos de anexos ficam fora do JSON.
- Antes da exclusão, uma intenção durável é gravada no DataStore. As tabelas são apagadas, categorias padrão são recriadas e, somente ao final, as preferências e a intenção são limpas. Na inicialização, uma intenção pendente é retomada antes de liberar as telas. Uma falha nessa recuperação exibe uma opção de tentar novamente.

## Consequências

Falhas de auditoria revertem a operação inteira. Evoluções sem migração falham explicitamente em vez de apagar o banco. A exclusão é recuperável e idempotente, mas não constitui uma transação atômica entre Room e DataStore. A exportação em memória ainda exige avaliação de recursos para históricos muito grandes.

## Validação

Testes com Room verificam rollback após a segunda gravação de auditoria, saldo sem pendências, propagação das pernas e recusa de transferência entre moedas. Outros testes verificam migração preservando conta, transação e auditoria; exportação com mais de mil eventos; e retomada após falha de limpeza das preferências.
