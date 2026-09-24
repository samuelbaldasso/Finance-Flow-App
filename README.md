# FinanceFlow 💳📊

<p align="center">
  <strong>Aplicativo de Gestão Financeira Pessoal e PJ para Android Nativo</strong><br>
  <em>Construído com padrões de engenharia de nível Staff/Senior, Kotlin idiomático, Jetpack Compose, Clean Architecture e Offline-First.</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.02-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Hilt-2.60.1-brightgreen" alt="Hilt" />
  <img src="https://img.shields.io/badge/Room-2.7.2-orange?logo=sqlite&logoColor=white" alt="Room" />
  <img src="https://img.shields.io/badge/Paging%203-3.3.6-blue" alt="Paging 3" />
  <img src="https://img.shields.io/badge/MinSdk-26-informational" alt="MinSdk" />
  <img src="https://img.shields.io/badge/TargetSdk-36-success" alt="TargetSdk" />
  <img src="https://img.shields.io/badge/Tests-100%25%20Passing-brightgreen" alt="Tests" />
</p>

---

## 📱 Visão Geral & Demonstração Visual

O **FinanceFlow** é um aplicativo financeiro de alta criticidade desenvolvido para garantir máxima precisão monetária, segurança de dados e experiência fluida no ecossistema Android.

### Galeria de Telas

<p align="center">
  <img src="docs/screenshots/01_accounts_screen.png" width="30%" alt="Patrimônio e Contas" />
  <img src="docs/screenshots/07_create_account_dialog.png" width="30%" alt="Modal Nova Conta" />
  <img src="docs/screenshots/02_transactions_screen.png" width="30%" alt="Extrato com Paging 3" />
</p>
<p align="center">
  <img src="docs/screenshots/03_budgets_screen.png" width="30%" alt="Orçamentos" />
  <img src="docs/screenshots/04_goals_screen.png" width="30%" alt="Metas de Economia" />
  <img src="docs/screenshots/06_settings_screen.png" width="30%" alt="Segurança e LGPD" />
</p>

---

## 🏛️ Princípios de Arquitetura & Engenharia

O projeto foi concebido sob princípios rigorosos de **Clean Architecture**, **DDD (Domain-Driven Design)** e **UDF (Unidirectional Data Flow)**:

```
┌────────────────────────────────────────────────────────┐
│                   Camada de UI (Compose)               │
│  - Stateless Composables, State Hoisting, Material 3   │
│  - Navigation Compose 2.8+ com Type-Safe Routes        │
│  - Paging 3 (LazyPagingItems) para Extrato Escalável   │
└───────────────────────────┬────────────────────────────┘
                            │ Dispara UiEvent / Observa StateFlow<UiState>
┌───────────────────────────▼────────────────────────────┐
│                  Camada de Apresentação                │
│  - ViewModels (@HiltViewModel)                         │
│  - Channels para efeitos únicos (UiEffect)             │
└───────────────────────────┬────────────────────────────┘
                            │ Executa
┌───────────────────────────▼────────────────────────────┐
│                    Camada de Domínio                   │
│  - UseCases puros com Single Responsibility Principle  │
│  - Modelos de Domínio Imutáveis (sem dependência de SO)│
│  - Value Class Money (cálculos exatos em centavos)    │
│  - Regras de negócio de Cartão, Recorrência e Orçamento│
└───────────────────────────┬────────────────────────────┘
                            │ Solicita dados
┌───────────────────────────▼────────────────────────────┐
│                    Camada de Dados                     │
│  - Repositórios (@Singleton via Hilt)                  │
│  - Room Database (Offline-First / Single Source of Truth)│
│  - Preferences DataStore (Credenciais e Configurações) │
│  - Trilha de Auditoria Imutável (audit_logs)          │
└────────────────────────────────────────────────────────┘
```

---

## ⚖️ Regras de Negócio & Integridade Financeira (Fonte da Verdade)

1. **Precisão Monetária Absoluta (§3.1):**
   - **Zero floats/doubles:** Todos os valores financeiros são expressos em centavos inteiros (`Long`) através da `@JvmInline value class Money(val amountMinor: Long)`.
   - Arredondamento bancário centralizado (`RoundingMode.HALF_EVEN`).
   - Operações multi-moeda não convertem silenciosamente: exigem conversor explícito com taxa e data.

2. **Contas com Saldo Derivado (§3.2):**
   - O saldo de uma conta nunca é editado diretamente; ele é estritamente derivado da soma do saldo inicial com suas transações efetivadas.
   - Contas com transações não podem ser excluídas: apenas **arquivadas** (soft-delete). Contas arquivadas não aceitam novos lançamentos.

3. **Transferências Atômicas (§3.3):**
   - Transferências entre contas criam um par atômico (débito na origem + crédito no destino vinculados pelo mesmo `transferId`) persistido em bloco `@Transaction`. Alterar ou deletar uma perna propaga atomicamente para a outra.

4. **Ciclo de Cartão de Crédito e Parcelamento (§3.7):**
   - Suporte a dia de fechamento e dia de vencimento. Compras após o fechamento caem na fatura subsequente.
   - Compras parceladas geram N parcelas vinculadas (`installmentGroupId`) distribuídas sem perda de centavos residuais na divisão.

5. **Auditoria Contínua (§3.3):**
   - Qualquer mutação financeira é gravada na tabela imutável `audit_logs` com timestamp UTC e payload de auditoria.

6. **Privacidade e LGPD (§3.11):**
   - Conformidade com o Artigo 18 da LGPD:
     - **Portabilidade:** Exportação completa de todos os dados do usuário em JSON legível.
     - **Direito ao Esquecimento:** Wipe total de dados com limpeza atômica de tabelas e preferências.
     - **FLAG_SECURE:** Bloqueio de capturas de tela e ocultação dos dados no alternador de apps.
     - **AppLock & Biometria:** Bloqueio automático por tempo de background com suporte a Biometria (`BiometricPrompt`) e PIN com Hash `SHA-256` salgado.

---

## 📑 Registros de Decisões de Arquitetura (ADRs)

Todas as decisões arquiteturais foram formalizadas em ADRs concisos:

| ADR | Título | Status |
|:---:|:---|:---:|
| [ADR-001](docs/adr/ADR-001-money-representation.md) | Representação Monetária em Centavos (`Long`) e Value Class `Money` | Aceito |
| [ADR-002](docs/adr/ADR-002-offline-first-room.md) | Arquitetura Offline-First com Room Database como Fonte Única da Verdade | Aceito |
| [ADR-003](docs/adr/ADR-003-navigation-compose-type-safe.md) | Type-Safe Navigation Compose 2.8+ com Rotas Serializáveis | Aceito |
| [ADR-004](docs/adr/ADR-004-dependency-injection-hilt.md) | Injeção de Dependências com Dagger Hilt | Aceito |
| [ADR-005](docs/adr/ADR-005-paging-3-scalability.md) | Paginação Reativa com AndroidX Paging 3 para Escalabilidade de Extratos | Aceito |
| [ADR-006](docs/adr/ADR-006-mobile-security-and-lgpd.md) | Segurança em Camadas (Biometria, PIN Salgado, FLAG_SECURE e LGPD) | Aceito |

---

## 🛠️ Stack Tecnológica

- **Linguagem:** Kotlin 2.2.10
- **UI:** Jetpack Compose (BOM 2026.02.01), Material 3, Material Icons Extended
- **Navegação:** AndroidX Navigation Compose 2.8.7 (Type-Safe Routes via `kotlinx.serialization`)
- **Injeção de Dependências:** Dagger Hilt 2.60.1 + KSP
- **Banco de Dados Local:** Room 2.7.2 + Room Paging
- **Paginação:** AndroidX Paging 3 (Paging Compose 3.3.6)
- **Preferências:** AndroidX DataStore Preferences 1.1.2
- **Segurança & Biometria:** AndroidX Biometric 1.2.0-alpha05, SHA-256 + Salt
- **Splash Screen:** AndroidX Core SplashScreen 1.0.1
- **Assincronia:** Coroutines 1.9.0 + StateFlow / SharedFlow / Channels
- **Testes:** JUnit 4, Kotlinx Coroutines Test, Robolectric 4.14.1, CashApp Turbine 1.2.1

---

## 🧪 Suíte de Testes

O projeto conta com ampla cobertura de testes unitários de regras de domínio, viewmodels e conciliação bancária:

```bash
# Executar todos os testes unitários
./gradlew test

# Gerar APK de debug
./gradlew assembleDebug
```

---

## 🚀 Como Executar o Projeto

1. Clone o repositório:
   ```bash
   git clone https://github.com/samuelbaldasso/Finance-Flow-App.git
   cd Finance-Flow-App
   ```
2. Abra o projeto no **Android Studio** (Ladybug / Meerkat ou superior com suporte a JDK 21).
3. Aguarde o Gradle Sync.
4. Execute no emulador ou dispositivo físico (`minSdk = 26`, `targetSdk = 36`).

---

## 👤 Autor

**Samuel Baldasso**  
- GitHub: [@samuelbaldasso](https://github.com/samuelbaldasso)
- E-mail: baldassosamuel93@gmail.com
