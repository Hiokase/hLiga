# 🔐 Guia de Permissões - hLiga

> **Sistema completo de permissões para controle granular de funcionalidades**  
> LuckPerms • PermissionsEx • GroupManager • Configuração rápida

[![LuckPerms](https://img.shields.io/badge/LuckPerms-Recomendado-brightgreen.svg)](https://luckperms.net/)

O sistema de permissões do hLiga oferece controle preciso sobre quem pode acessar cada funcionalidade, desde comandos básicos até administração completa do sistema.

---

## 🚀 Configuração Rápida

### Para Iniciantes (3 Minutos)
```bash
# LuckPerms - Configuração básica
lp group default permission set hliga.command true
lp group moderador permission set hliga.addpoints true
lp group moderador permission set hliga.removepoints true
lp group admin permission set hliga.admin true
```

### Hierarquia Recomendada
```
👤 Jogadores    → hliga.command
👮 Moderadores  → hliga.command + pontos + temporada.status
🛡️ Administradores → hliga.admin (todas as permissões)
```

---

## 📋 Permissões por Categoria

### 🎮 Básicas - Todos os Jogadores
**`hliga.command`** - Comandos essenciais
- **Padrão**: `true` (liberado para todos)
- **Inclui**: `/hliga menu`, `/hliga top`, `/hliga points`, `/hliga help`

```bash
# LuckPerms
lp group default permission set hliga.command true

# PermissionsEx  
pex group default add hliga.command

# GroupManager
manuadd default hliga.command
```

### 🛡️ Administrativas - Staff Completo
**`hliga.admin`** - Acesso total ao sistema
- **Padrão**: `op` (apenas operadores)
- **Inclui**: Todas as permissões do plugin automaticamente

```bash
# LuckPerms
lp group admin permission set hliga.admin true

# PermissionsEx
pex group admin add hliga.admin

# GroupManager
manuadd admin hliga.admin
```

### 📊 Gerenciamento de Pontos - Moderadores
| Permissão | Descrição | Comando |
|-----------|-----------|---------|
| `hliga.addpoints` | Adicionar pontos aos clãs | `/hliga addpoints <clã> <pontos>` |
| `hliga.removepoints` | Remover pontos dos clãs | `/hliga removepoints <clã> <pontos>` |
| `hliga.setpoints` | Definir pontos específicos | `/hliga setpoints <clã> <pontos>` |

```bash
# LuckPerms - Configuração para moderadores
lp group moderador permission set hliga.addpoints true
lp group moderador permission set hliga.removepoints true
lp group moderador permission set hliga.setpoints true

# PermissionsEx
pex group moderador add hliga.addpoints
pex group moderador add hliga.removepoints
pex group moderador add hliga.setpoints
```

### ⏰ Sistema de Temporadas - Staff Senior
| Permissão | Descrição | Comando |
|-----------|-----------|---------|
| `hliga.temporada` | Comandos básicos de temporada | `/temporada status`, `/temporada historico` |
| `hliga.temporada.iniciar` | Iniciar novas temporadas | `/temporada iniciar <nome> <duração>` |
| `hliga.temporada.fechar` | Encerrar temporadas ativas | `/temporada fechar` |

```bash
# LuckPerms - Staff pode ver, só admin pode gerenciar
lp group staff permission set hliga.temporada true
lp group admin permission set hliga.temporada.iniciar true
lp group admin permission set hliga.temporada.fechar true
```

### 🤖 NPCs do Ranking - Builders
| Permissão | Descrição | Comando |
|-----------|-----------|---------|
| `hliga.topnpc` | Comandos básicos de NPCs | Base para outros comandos |
| `hliga.topnpc.create` | Criar NPCs do ranking | `/hliga topnpc create <id> <posição>` |
| `hliga.topnpc.remove` | Remover NPCs | `/hliga topnpc remove <id>` |
| `hliga.topnpc.update` | Atualizar NPCs | `/hliga topnpc update` |

```bash
# LuckPerms - Builders podem gerenciar NPCs
lp group builder permission set hliga.topnpc.create true
lp group builder permission set hliga.topnpc.remove true
lp group builder permission set hliga.topnpc.update true
```

---

## 🏗️ Templates de Configuração

### Servidor Pequeno (Até 50 Players)
```yaml
# Configuração simples - apenas 2 grupos
groups:
  default:
    - hliga.command
  admin:
    - hliga.admin
```

### Servidor Médio (50-200 Players)
```yaml
# Configuração com hierarquia
groups:
  default:
    - hliga.command
  helper:
    - hliga.command
    - hliga.temporada
  moderador:
    - hliga.command
    - hliga.temporada
    - hliga.addpoints
    - hliga.removepoints
  admin:
    - hliga.admin
```

### Servidor Grande (200+ Players)
```yaml
# Configuração completa com especialização
groups:
  default:
    - hliga.command
  
  builder:
    - hliga.command
    - hliga.topnpc.create
    - hliga.topnpc.remove
    - hliga.topnpc.update
    
  helper:
    - hliga.command
    - hliga.temporada
    
  moderador:
    - hliga.command
    - hliga.temporada
    - hliga.addpoints
    - hliga.removepoints
    
  coordenador:
    - hliga.command
    - hliga.temporada.*
    - hliga.addpoints
    - hliga.removepoints
    - hliga.setpoints
    
  admin:
    - hliga.admin
```

---

## 🔧 Comandos de Configuração

### LuckPerms (Recomendado)
```bash
# Configuração básica
lp group default permission set hliga.command true
lp group moderador permission set hliga.addpoints true
lp group moderador permission set hliga.removepoints true
lp group admin permission set hliga.admin true

# Configuração avançada
lp group builder permission set hliga.topnpc.create true
lp group coordenador permission set hliga.temporada.iniciar true
lp group coordenador permission set hliga.temporada.fechar true

# Verificar permissões
lp user [jogador] permission info
lp group [grupo] permission info
```

### PermissionsEx
```bash
# Configuração básica
pex group default add hliga.command
pex group moderador add hliga.addpoints
pex group moderador add hliga.removepoints
pex group admin add hliga.admin

# Configuração avançada
pex group builder add hliga.topnpc.create
pex group coordenador add hliga.temporada.iniciar
pex group coordenador add hliga.temporada.fechar

# Verificar permissões
pex user [jogador] check hliga.command
pex group [grupo] list
```

### GroupManager
```bash
# Configuração básica
manuadd default hliga.command
manuadd moderador hliga.addpoints
manuadd moderador hliga.removepoints
manuadd admin hliga.admin

# Configuração avançada
manuadd builder hliga.topnpc.create
manuadd coordenador hliga.temporada.iniciar
manuadd coordenador hliga.temporada.fechar

# Verificar permissões
manucheckp [jogador] hliga.command
```

---

## 🚨 Troubleshooting

### Problemas Comuns

**❌ Jogador não consegue usar comandos básicos**
```bash
# Verificar se tem a permissão básica
lp user [jogador] permission set hliga.command true
```

**❌ Moderador não consegue adicionar pontos**
```bash
# Verificar permissões de pontos
lp user [jogador] permission set hliga.addpoints true
```

**❌ Admin não tem acesso total**
```bash
# Dar permissão administrativa completa
lp user [jogador] permission set hliga.admin true
```

**❌ NPCs não podem ser criados**
```bash
# Verificar permissões de NPC
lp user [jogador] permission set hliga.topnpc.create true
```

### Comandos de Debug
```bash
# Verificar permissões de um jogador
lp user [jogador] permission info

# Verificar permissões de um grupo
lp group [grupo] permission info

# Testar permissão específica
lp user [jogador] permission check hliga.command
```

---

## 📋 Lista Completa de Permissões

### Básicas
- `hliga.command` - Comandos básicos (padrão: true)
- `hliga.admin` - Acesso administrativo completo (padrão: op)

### Pontuação
- `hliga.addpoints` - Adicionar pontos aos clãs
- `hliga.removepoints` - Remover pontos dos clãs
- `hliga.setpoints` - Definir pontos específicos

### Temporadas
- `hliga.temporada` - Comandos básicos de temporada
- `hliga.temporada.iniciar` - Iniciar temporadas
- `hliga.temporada.fechar` - Encerrar temporadas

### NPCs
- `hliga.topnpc` - Comandos básicos de NPCs
- `hliga.topnpc.create` - Criar NPCs do ranking
- `hliga.topnpc.remove` - Remover NPCs
- `hliga.topnpc.update` - Atualizar NPCs

### Utilitários
- `hliga.reload` - Recarregar configurações
- `hliga.sync` - Sincronizar clãs

---

<div align="center">

**hLiga v1.0.0** - Sistema de Permissões Completo  
*Controle total sobre quem pode fazer o quê*

[![LuckPerms](https://img.shields.io/badge/LuckPerms-Recommended-brightgreen.svg)](https://luckperms.net/)

</div>

## Permissões de Temporadas

### hliga.temporada
- **Descrição**: Permite usar comandos básicos de temporada
- **Padrão**: `op`
- **Comandos**:
  - `/temporada status`
  - `/temporada historico [página]`

### hliga.temporada.iniciar
- **Descrição**: Permite iniciar novas temporadas
- **Padrão**: `op`
- **Comando**: `/temporada iniciar <nome> <duração|data>`

### hliga.temporada.fechar
- **Descrição**: Permite encerrar temporadas ativas
- **Padrão**: `op`
- **Comando**: `/temporada fechar`

**Exemplo para diferentes níveis de acesso**:
```yaml
# LuckPerms - Staff pode ver status, mas só admin pode gerenciar
lp group staff permission set hliga.temporada true
lp group admin permission set hliga.temporada.iniciar true
lp group admin permission set hliga.temporada.fechar true
```

## Permissões de NPCs

### hliga.topnpc
- **Descrição**: Permite usar comandos básicos de NPCs
- **Padrão**: `op`
- **Nota**: Requer Citizens2 instalado

### hliga.topnpc.create
- **Descrição**: Permite criar NPCs do ranking
- **Padrão**: `op`
- **Comando**: `/hliga topnpc create <id> <posição>`

### hliga.topnpc.remove
- **Descrição**: Permite remover NPCs do ranking
- **Padrão**: `op`
- **Comando**: `/hliga topnpc remove <id>`

### hliga.topnpc.update
- **Descrição**: Permite atualizar NPCs do ranking
- **Padrão**: `op`
- **Comando**: `/hliga topnpc update`

**Exemplo para builders**:
```yaml
# LuckPerms - Builders podem gerenciar NPCs
lp group builder permission set hliga.topnpc.create true
lp group builder permission set hliga.topnpc.remove true
lp group builder permission set hliga.topnpc.update true
```

## Hierarquia de Permissões

### Nível 1: Jogadores (Básico)
```yaml
# Permissões mínimas para jogadores
permissions:
  - hliga.command
```
**Podem fazer**:
- Ver rankings
- Abrir menus
- Consultar pontos de clãs
- Ver ajuda

### Nível 2: Moderadores
```yaml
# Permissões para moderadores
permissions:
  - hliga.command
  - hliga.addpoints
  - hliga.removepoints
  - hliga.temporada
```
**Podem fazer**:
- Tudo do Nível 1
- Adicionar/remover pontos
- Ver status de temporadas
- Ver histórico

### Nível 3: Administradores
```yaml
# Permissões para administradores
permissions:
  - hliga.admin  # Inclui todas as permissões
```
**Podem fazer**:
- Tudo dos níveis anteriores
- Gerenciar temporadas
- Criar/remover NPCs
- Configurar sistema
- Recarregar plugin

## Configurações por Plugin de Permissões

### LuckPerms (Recomendado)

#### Configuração Básica
```bash
# Criar grupos
lp creategroup jogador
lp creategroup moderador
lp creategroup admin

# Permissões para jogadores
lp group jogador permission set hliga.command true

# Permissões para moderadores
lp group moderador parent add jogador
lp group moderador permission set hliga.addpoints true
lp group moderador permission set hliga.removepoints true
lp group moderador permission set hliga.temporada true

# Permissões para administradores
lp group admin parent add moderador
lp group admin permission set hliga.admin true
```

#### Permissões Temporárias
```bash
# Dar permissão temporária (1 hora)
lp user PlayerName permission settemp hliga.temporada.iniciar true 1h

# Dar permissão em mundo específico
lp user PlayerName permission set hliga.addpoints true world=world_pvp
```

### PermissionsEx

#### Configuração Básica
```yaml
# permissions.yml
groups:
  jogador:
    permissions:
      - hliga.command
  
  moderador:
    inheritance:
      - jogador
    permissions:
      - hliga.addpoints
      - hliga.removepoints
      - hliga.temporada
  
  admin:
    inheritance:
      - moderador
    permissions:
      - hliga.admin
```

### GroupManager

#### Configuração Básica
```yaml
# groups.yml
groups:
  default:
    permissions:
      - hliga.command
  
  moderator:
    inherits:
      - default
    permissions:
      - hliga.addpoints
      - hliga.removepoints
      - hliga.temporada
  
  admin:
    inherits:
      - moderator
    permissions:
      - hliga.admin
```

## Permissões Especiais

### Bypass de Limites
Algumas permissões permitem contornar limitações do sistema:

```yaml
# Futuras permissões de bypass (planejadas)
hliga.bypass.pointlimit    # Ignorar limite máximo de pontos
hliga.bypass.seasonlimit   # Múltiplas temporadas simultâneas
hliga.bypass.cooldown      # Ignorar cooldowns de comandos
```

### Permissões por Mundo
```bash
# LuckPerms - Permissões específicas por mundo
lp user PlayerName permission set hliga.addpoints true world=world_events
lp user PlayerName permission set hliga.addpoints false world=world_creative
```

### Permissões Temporárias
```bash
# LuckPerms - Permissão temporária para evento
lp user PlayerName permission settemp hliga.temporada.iniciar true 2h

# Remover permissão após tempo
lp user PlayerName permission unsettemp hliga.temporada.iniciar
```

## Exemplos de Configuração

### Servidor PvP com Eventos
```yaml
# Configuração para servidor focado em PvP
grupos:
  jogador:
    - hliga.command
  
  helper:
    - hliga.command
    - hliga.temporada  # Podem ver status
  
  moderador:
    - hliga.command
    - hliga.addpoints
    - hliga.removepoints
    - hliga.temporada
  
  admin:
    - hliga.admin
```

### Servidor Survival com Clãs
```yaml
# Configuração para servidor survival
grupos:
  membro:
    - hliga.command
  
  vip:
    - hliga.command
    # Mesmas permissões, sem vantagens P2W
  
  moderador:
    - hliga.command
    - hliga.addpoints
    - hliga.removepoints
    - hliga.temporada
    - hliga.topnpc.update
  
  admin:
    - hliga.admin
```

### Servidor com Sub-mundos
```bash
# LuckPerms - Permissões diferentes por mundo
# Mundo principal - acesso completo
lp group moderador permission set hliga.addpoints true world=world

# Mundo criativo - sem permissões de pontos
lp group moderador permission set hliga.addpoints false world=creative

# Mundo de eventos - permissões especiais
lp group event-staff permission set hliga.temporada.iniciar true world=events
```

## Troubleshooting de Permissões

### Problemas Comuns

#### Jogador não pode usar comandos básicos
```bash
# Verificar se tem permissão básica
lp user PlayerName permission check hliga.command

# Adicionar se necessário
lp user PlayerName permission set hliga.command true
```

#### Moderador não pode adicionar pontos
```bash
# Verificar permissões específicas
lp user PlayerName permission check hliga.addpoints

# Verificar herança de grupos
lp user PlayerName info
```

#### Comandos de temporada não funcionam
```bash
# Verificar permissões de temporada
lp user PlayerName permission check hliga.temporada.iniciar
lp user PlayerName permission check hliga.temporada.fechar
```

### Debug de Permissões

#### Verificar Permissões Ativas
```bash
# LuckPerms - Ver todas as permissões de um usuário
lp user PlayerName permission info

# Ver permissões de um grupo
lp group GroupName permission info

# Verificar herança
lp user PlayerName parent info
```

#### Logs de Permissões
```yaml
# config.yml do hLiga - Ativar logs de permissões
sistema:
  debug: true
  log_level: 3
```

### Comandos Úteis

#### LuckPerms
```bash
# Backup de permissões
lp export backup.json

# Importar permissões
lp import backup.json

# Verificar permissão específica
lp user PlayerName permission check hliga.admin

# Ver informações completas
lp user PlayerName info
```

#### Verificação In-Game
```java
// Para desenvolvedores - verificar permissões no código
if (player.hasPermission("hliga.admin")) {
    // Jogador é admin
} else if (player.hasPermission("hliga.addpoints")) {
    // Jogador pode adicionar pontos
}
```

## Melhores Práticas

### Segurança
1. **Princípio do Menor Privilégio**: Dê apenas as permissões necessárias
2. **Herança de Grupos**: Use herança para evitar duplicação
3. **Permissões Temporárias**: Para eventos especiais
4. **Auditoria Regular**: Revise permissões periodicamente

### Organização
1. **Nomes Descritivos**: Use nomes claros para grupos
2. **Documentação**: Mantenha registro das mudanças
3. **Testes**: Teste permissões antes de aplicar em produção
4. **Backup**: Sempre faça backup antes de mudanças grandes

### Performance
1. **Evite Negações**: Use `false` apenas quando necessário
2. **Cache**: Plugins modernos fazem cache automaticamente
3. **Grupos Pequenos**: Evite muitos níveis de herança
4. **Verificação Local**: Cache permissões importantes no plugin

## Lista Completa de Permissões

```yaml
# Todas as permissões do hLiga v1.0.0
permissions:
  # Básicas
  hliga.command:
    description: Comandos básicos do hLiga
    default: true
  
  hliga.admin:
    description: Acesso administrativo completo
    default: op
  
  # Pontuação
  hliga.addpoints:
    description: Adicionar pontos a clãs
    default: op
  
  hliga.removepoints:
    description: Remover pontos de clãs
    default: op
  
  hliga.setpoints:
    description: Definir pontos específicos
    default: op
  
  # Temporadas
  hliga.temporada:
    description: Comandos básicos de temporada
    default: op
  
  hliga.temporada.iniciar:
    description: Iniciar temporadas
    default: op
  
  hliga.temporada.fechar:
    description: Encerrar temporadas
    default: op
  
  # NPCs
  hliga.topnpc:
    description: Comandos básicos de NPCs
    default: op
  
  hliga.topnpc.create:
    description: Criar NPCs
    default: op
  
  hliga.topnpc.remove:
    description: Remover NPCs
    default: op
  
  hliga.topnpc.update:
    description: Atualizar NPCs
    default: op
```

---

Este guia cobre todo o sistema de permissões do hLiga. Para casos específicos ou dúvidas sobre configuração, consulte a documentação do seu plugin de permissões ou entre em contato com o suporte.