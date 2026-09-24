# ADR 002: Arquitetura Offline-First com Room Database como Fonte Única da Verdade

## Status
Aceito

## Contexto
Usuários de aplicativos financeiros esperam disponibilidade imediata, responsividade sem latência de rede e funcionalidade ininterrupta mesmo em ambientes sem conectividade (metrô, aviões, conexões instáveis). A perda de lançamentos durante períodos sem sinal causaria frustração severa e inconsistência contábil.

## Decisão
1. Adotar a arquitetura **Offline-First**, estabelecendo o **Room Database local** como a única fonte da verdade (Single Source of Truth - SSOT).
2. Toda e qualquer mutação de estado (criação de conta, registro de despesa, conciliação, rolagem de orçamento) é gravada atomicamente no banco local antes de qualquer eventual sincronização remota.
3. Repositórios expõem fluxos contínuos (`Flow<T>`) observáveis a partir das queries do Room, assegurando que a UI reaja instantaneamente a qualquer alteração de persistência (Padrão UDF).
4. Operações complexas que envolvem múltiplas tabelas (ex: transferências atômicas debitando origem e creditando destino, ou parcelamentos com N faturas) são executadas em bloco `@Transaction` com isolamento ACID.

## Consequências

### Positivas
- **Latência Zero na UI:** O usuário interage instantaneamente sem spinners bloqueantes de rede.
- **Confiabilidade:** Nenhuma transação financeira é perdida por queda de sinal de internet.
- **Previsibilidade:** A UI simplesmente reflete o estado do banco Room; testes unitários em memória executam de forma ultrarrápida e determinística.

### Negativas
- Exige gerenciamento cuidadoso de migrações de banco (`Migration` testadas) conforme o schema evolui.
