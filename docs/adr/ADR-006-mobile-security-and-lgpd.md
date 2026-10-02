# ADR 006: Segurança em Camadas (Biometria, PIN com Salt SHA-256, FLAG_SECURE, Auditoria e LGPD)

## Status
Aceito

## Contexto
Dados financeiros são classificados como dados pessoais altamente sensíveis. O vazamento de informações patrimoniais, hábitos de consumo ou transações bancárias pode acarretar prejuízos irreparáveis aos usuários e violar diretamente a legislação de privacidade (LGPD - Lei nº 13.709/2018).
Aplicações financeiras de alta criticidade necessitam de salvaguardas rigorosas contra acesso físico não autorizado, espionagem visual no alternador de tarefas e rastreamento indevido.

## Decisão
1. **Camada de Bloqueio & Biometria:**
   - Implementar `AppLockManager` controlando estados de travamento após períodos parametrizáveis de inatividade em background (1 min, 5 min, 15 min ou imediato).
   - Suporte nativo a autenticação biométrica via `AndroidX BiometricPrompt` (impressão digital e reconhecimento facial) com fallback para PIN numérico.
2. **Armazenamento Seguro de Credenciais:**
   - Proibir persistência de senhas ou PINs em texto plano. O PIN do usuário é armazenado como hash `SHA-256` salgado (`FinanceFlow_Security_Salt_v1_`) no DataStore de preferências.
3. **Proteção de Superfície (FLAG_SECURE):**
   - Suporte dinâmico a `FLAG_SECURE` na janela (`window.setFlags(WindowManager.LayoutParams.FLAG_SECURE)`), bloqueando capturas de tela e ocultando dados sensíveis no snapshot do alternador de apps do sistema operacional.
4. **Trilha de Auditoria Imutável:**
   - Registrar cada inserção, alteração ou exclusão financeira na tabela `audit_logs` (com timestamp UTC, tipo de entidade, ação e payload serializado).
5. **Conformidade com a LGPD (Art. 18):**
   - **Portabilidade:** `ExportAllUserDataUseCase` exporta todos os dados do usuário (contas, transações, orçamentos, metas e auditoria) em formato JSON aberto, incluindo faturas e metadados. Credenciais e arquivos de anexos não são exportados; detalhes no ADR-007.
   - **Direito ao Esquecimento:** `WipeAllUserDataUseCase` executa a exclusão dos registros e configurações com intenção persistida para recuperação após falhas. Categorias padrão são recriadas; detalhes no ADR-007.
   - **Minimização de Dados:** Nenhum dado pessoal identificável (PII) é transmitido ou coletado sem consentimento explícito prévio.

## Consequências

### Positivas
- **Controles locais:** Bloqueio, proteção de tela e controle de dados; não há certificação de segurança ou conformidade jurídica.
- **Transparência e Confiança:** O usuário tem controle soberano sobre seus dados e privacidade.
- **Proteção Contra Ataques Físicos e Espionagem:** Sessões bloqueadas automaticamente ao alternar de aplicativo.

### Negativas
- A proteção `FLAG_SECURE` requer toggle explícito pelo usuário quando capturas de tela legítimas forem necessárias (ex: suporte ao cliente).
