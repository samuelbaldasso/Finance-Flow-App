# ADR 002: Arquitetura Offline-First com Room Database como Fonte Única da Verdade

## Status
Aceito

## Contexto
Usuários de aplicativos financeiros esperam disponibilidade imediata, responsividade sem latência de rede e funcionalidade ininterrupta mesmo em ambientes sem conectividade (metrô, aviões, conexões instáveis). A perda de lançamentos durante períodos sem sinal causaria frustração severa e inconsistência contábil.

## Decisão
1. Adotar a arquitetura **Offline-First**, estabelecendo o **Room Database local** como a única fonte da verdade (Single Source of Truth - SSOT).
2. Os registros são persistidos no banco local. Mutações de contas, categorias e transações incluem a auditoria na mesma transação, conforme ADR-007.
3. Repositórios expõem fluxos contínuos (`Flow<T>`) observáveis a partir das queries do Room, assegurando que a UI reaja instantaneamente a qualquer alteração de persistência (Padrão UDF).
4. Transferências executam as duas pernas e auditoria em `withTransaction`. A criação de grupos de parcelas ainda exige evolução para garantir atomicidade do grupo inteiro.

## Consequências

### Positivas
- **Latência Zero na UI:** O usuário interage instantaneamente sem spinners bloqueantes de rede.
- **Confiabilidade:** Nenhuma transação financeira é perdida por queda de sinal de internet.
- **Previsibilidade:** A UI simplesmente reflete o estado do banco Room; testes unitários em memória executam de forma ultrarrápida e determinística.

### Negativas
- Exige gerenciamento cuidadoso de migrações de banco (`Migration` testadas) conforme o schema evolui.
