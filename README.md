# 🏆 hLiga - Sistema Avançado de Liga para Clãs

> **Sistema completo de competições entre clãs para Minecraft**  
> Temporadas automáticas • Rankings dinâmicos • NPCs visuais • Integração Discord

[![Minecraft](https://img.shields.io/badge/Minecraft-1.8--1.21.5-brightgreen.svg)](https://github.com/Hiokase/hLiga)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://github.com/Hiokase/hLiga)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://github.com/Hiokase/hLiga)
[![Version](https://img.shields.io/badge/Version-1.0.0-purple.svg)](https://github.com/Hiokase/hLiga)

O hLiga transforma seu servidor Minecraft em uma arena competitiva onde clãs batalham por supremacia. Com funcionalidades avançadas de automação e uma interface intuitiva, gerenciar competições nunca foi tão simples.

---

## ✨ Funcionalidades Principais

### 🎯 Sistema de Temporadas Inteligente
- **Criação Flexível**: Configure por duração (`30 dias`) ou data específica (`31/12/2024`)
- **Finalização Automática**: Distribuição de recompensas e tags sem intervenção manual
- **Histórico Completo**: Preserve dados de todas as temporadas para consulta
- **Avisos Programados**: Notificações automáticas antes do término

### 📊 Rankings e Pontuação Avançados
- **Sistema de Pontos Flexível**: Multiplicadores configuráveis e limites personalizáveis
- **Rankings Dinâmicos**: Atualizações em tempo real conforme clãs ganham pontos
- **Validação Inteligente**: Prevenção de pontos negativos e valores inválidos
- **Formatação Numérica**: Apresentação clara de grandes números (1.234.567)

### 🎮 Interface Gráfica Moderna
- **5 Menus Interativos**: Principal, Top Clãs, Lista, Histórico e Configurações
- **Navegação Intuitiva**: Páginas automáticas para grandes listas
- **Personalização Total**: Customize ícones, cores e textos via arquivos YAML
- **Ações Clicáveis**: Interface responsiva com sons e feedback visual

### 🤖 NPCs Dinâmicos (Citizens2)
- **Ranking Visual**: NPCs representando as posições dos clãs
- **Skins Personalizadas**: Diferentes aparências baseadas na colocação
- **Atualização Automática**: Sincronização periódica com o ranking atual
- **Proteção Avançada**: Imunidade a danos e manipulação de jogadores

### 💬 Integração Discord Completa
- **Webhooks Duplos**: Canal principal (temporadas) + Staff (pontos)
- **Embeds Personalizáveis**: Templates configuráveis em `discord.json`
- **Notificações Automáticas**: Eventos de temporadas e mudanças de pontos
- **Design Moderno**: Embeds com cores vibrantes e formatação markdown

### 🏷️ Sistema de Tags Automático
- **Tags de Temporada**: Aplicação automática aos vencedores
- **Múltiplos Tipos**: Campeão, Vice-campeão, 3º lugar e tags customizadas
- **Placeholders Integrados**: Sistema completo de variáveis dinâmicas
- **Persistência**: Tags mantidas entre sessões e reinicializações

---

## 🚀 Configuração Rápida

### Passo 1: Instalação
1. **Baixe** o arquivo `hLiga-1.0.0.jar`
2. **Coloque** na pasta `plugins/` do seu servidor
3. **Instale** SimpleClans ou LeafGuilds
4. **Reinicie** o servidor

### Passo 2: Configuração Essencial

#### Discord Webhook (Recomendado)
```yaml
# config.yml
discord:
  ativado: true
  webhook_url: "https://discord.com/api/webhooks/SEU_ID/SEU_TOKEN"
  staff_webhook_url: "https://discord.com/api/webhooks/STAFF_ID/STAFF_TOKEN"
```

**Como criar webhook:**
1. Discord → Configurações do Canal → Integrações → Webhooks
2. Criar Webhook → Copiar URL → Colar no config.yml

#### Sistema de Pontos
```yaml
pontos:
  iniciais: 0
  maximo: 999999999
  multiplicador: 1.0
  nome: "ponto"
  nome_plural: "pontos"
```

#### Banco de Dados
```yaml
# SQLite (Padrão)
database:
  type: SQLITE
  sqlite:
    file: "database.db"

# MySQL (Servidores Grandes)
database:
  type: MYSQL
  mysql:
    host: localhost
    port: 3306
    database: hliga
    username: usuario
    password: senha
```

### Passo 3: Primeiros Comandos
```bash
/temporada iniciar "Liga Teste" 7        # Temporada de 7 dias
/hliga addpoints MinhaGuild 100          # Adicionar pontos
/hliga menu                              # Abrir interface
```

---

## 🎯 Comandos Essenciais

### Comando Principal: `/hliga`
| Comando | Descrição | Exemplo |
|---------|-----------|---------|
| `menu` | Abre interface gráfica principal | `/hliga menu` |
| `top [página]` | Exibe ranking paginado | `/hliga top 2` |
| `addpoints <clã> <pontos> [-d motivo]` | Adiciona pontos | `/hliga addpoints MinhaGuilda 100 -d "Vitória PvP"` |
| `removepoints <clã> <pontos>` | Remove pontos | `/hliga removepoints MinhaGuilda 50` |
| `points <clã>` | Consulta pontos do clã | `/hliga points MinhaGuilda` |
| `sync` | Sincroniza clãs com BD | `/hliga sync` |
| `reload` | Recarrega configurações | `/hliga reload` |

### Comando de Temporadas: `/temporada`
| Comando | Descrição | Exemplo |
|---------|-----------|---------|
| `iniciar <nome> <duração>` | Inicia por dias | `/temporada iniciar "Liga Verão" 30` |
| `iniciar <nome> <data>` | Inicia por data | `/temporada iniciar "Liga Inverno" 31/12/2024` |
| `fechar` | Encerra temporada atual | `/temporada fechar` |
| `status` | Status da temporada ativa | `/temporada status` |
| `historico [página]` | Histórico paginado | `/temporada historico 1` |

### NPCs do Ranking: `/hliga topnpc`
```bash
/hliga topnpc create top1 1    # NPC para 1º lugar
/hliga topnpc create top2 2    # NPC para 2º lugar  
/hliga topnpc update           # Atualizar todos os NPCs
```

---

## 🏷️ Sistema de Tags e Placeholders

### Tags Automáticas
Configure as tags dos vencedores em `tags.yml`:

```yaml
tags:
  sistema_ativo: true
  temporada:
    primeiro_lugar: "&6[CAMPEÃO]"
    segundo_lugar: "&e[VICE-CAMPEÃO]"  
    terceiro_lugar: "&c[3º LUGAR]"
    
  duracao_tags: 90  # Dias para expirar
  remover_antigas: true
```

### 📝 Placeholders Disponíveis

#### PlaceholderAPI Integration
```
%hliga_clan_points%              # Pontos do clã do jogador
%hliga_clan_position%            # Posição no ranking atual
%hliga_clan_tag%                 # Tag/nome do clã
%hliga_season_name%              # Nome da temporada ativa
%hliga_season_active%            # Se temporada está ativa (true/false)
%hliga_season_days_left%         # Dias restantes da temporada
%hliga_season_time_left%         # Tempo formatado restante
%hliga_player_tags%              # Tags do jogador (separadas por vírgula)
%hliga_top_clan_1%               # Nome do 1º colocado
%hliga_top_clan_2%               # Nome do 2º colocado  
%hliga_top_clan_3%               # Nome do 3º colocado
%hliga_top_points_1%             # Pontos do 1º colocado
%hliga_total_clans%              # Total de clãs participantes
```

#### Placeholders Internos (messages.yml, discord.json)
```
{clan_tag}                       # Nome/tag do clã
{pontos}                        # Quantidade de pontos
{pontos_formatados}             # Pontos com formatação (1.234.567)
{posicao}                       # Posição no ranking
{nome_temporada}                # Nome da temporada
{duracao_dias}                  # Duração em dias
{data_inicio}                   # Data de início
{data_fim}                      # Data de término
{primeiro_lugar}                # Clã em 1º lugar
{segundo_lugar}                 # Clã em 2º lugar
{terceiro_lugar}               # Clã em 3º lugar
{total_participantes}          # Total de clãs com pontos
{motivo}                       # Motivo da mudança de pontos
{jogador}                      # Nome do jogador
{comando_usado}                # Comando executado
{tempo_restante}               # Tempo restante formatado
```

### Exemplos de Personalização
```yaml
# messages.yml
pontos:
  adicionados: "&a[+] &f{pontos_formatados} pontos adicionados ao clã &e{clan_tag}&f!"
  ranking_atual: "&7Posição atual: &e#{posicao} &7com &a{pontos_formatados} pontos"

temporadas:
  iniciada: "&6Nova temporada &e{nome_temporada} &6iniciada!"
  status: "&7Temporada: &e{nome_temporada} &7| Tempo restante: &c{tempo_restante}"
```

---

## 🔧 Configurações Avançadas

### NPCs Visuais (Citizens2)
Para NPCs do ranking:
```yaml
# config.yml
npcs:
  ativado: true
  intervalo_atualizacao: 5  # minutos
  
# Comandos de criação
/hliga topnpc create top1 1     # NPC do 1º lugar
/hliga topnpc create top2 2     # NPC do 2º lugar
/hliga topnpc create top3 3     # NPC do 3º lugar
```

### Performance e Cache
```yaml
configuracoes:
  intervalo_atualizacao: 5      # Minutos entre atualizações
  
database:
  mysql:
    poolSize: 10               # Pool de conexões
    connectionTimeout: 30000
    
cache:
  redis:
    ativado: true
    host: localhost
    porta: 6379
    ttl: 300                   # 5 minutos
```

### Personalização de Menus
```yaml
# menus.yml
menu_principal:
  titulo: "&6&lhLiga - Menu Principal"
  tamanho: 27
  
  items:
    ranking:
      slot: 11
      material: "DIAMOND_SWORD"
      nome: "&e&lRanking de Clãs"
      lore:
        - "&7Veja o ranking atual"
        - "&7dos clãs em competição"
```

---

## 🛠️ Troubleshooting

### Problemas Comuns

#### NPCs não aparecem
1. Instalar Citizens2
2. Verificar permissões `hliga.topnpc.create`
3. Confirmar localização válida com `/hliga topnpc create <id> <posição>`

#### Discord não envia mensagens
1. Validar URL do webhook no config.yml
2. Verificar `discord.ativado: true`
3. Testar webhook manualmente no Discord

#### Performance lenta
1. Migrar SQLite → MySQL para servidores grandes
2. Ativar cache Redis
3. Aumentar `intervalo_atualizacao` nos NPCs

#### Clãs não sincronizam
1. Verificar se SimpleClans/LeafGuilds está ativo
2. Usar `/hliga sync` para sincronização manual
3. Confirmar `clan_plugin` correto no config.yml

### Comandos de Debug
```bash
/hliga reload                    # Recarregar todas as configurações
/hliga sync                      # Sincronizar clãs com banco de dados
/hliga topnpc update            # Atualizar NPCs manualmente
```

---

## 🔌 API para Desenvolvedores

- **API**: [Documentação Completa](API_DOCUMENTATION.md)


## 📊 Performance e Compatibilidade

### Otimizações Incluídas
- **Cache Redis**: Rankings frequentes mantidos em memória
- **Pool HikariCP**: Conexões MySQL otimizadas com pool inteligente
- **Operações Assíncronas**: Webhooks e queries não bloqueam o servidor
- **Lazy Loading**: Dependências carregadas apenas quando necessárias

### Versões Suportadas
- **Minecraft**: 1.8-1.21.5 (compatibilidade total)
- **Java**: 8, 11, 17, 21+ (otimizado para Java 8)
- **Servidores**: Spigot, Paper, Folia

### Plugins Suportados
- **Obrigatórios**: SimpleClans ou LeafGuilds
- **Opcionais**: Citizens2, PlaceholderAPI, HolographicDisplays

---

## 📚 Documentação Adicional

- **[📖 Documentação Completa](DOCUMENTACAO_COMPLETA.md)**: Guia técnico detalhado
- **[🔌 API Reference](API_DOCUMENTATION.md)**: Documentação para desenvolvedores
- **[🔐 Guia de Permissões](GUIA_PERMISSOES.md)**: Sistema de permissões detalhado

---

## 📋 Requisitos e Instalação

### Requisitos Mínimos
- ✅ **Minecraft**: 1.8 ou superior
- ✅ **Java**: Versão 8 ou superior
- ✅ **Servidor**: Spigot, Paper ou Folia
- ✅ **Plugin de Clãs**: SimpleClans ou LeafGuilds
- ✅ **RAM**: 512MB disponível (recomendado 1GB+)

### Plugins Opcionais
- **Citizens2**: Para NPCs do ranking
- **PlaceholderAPI**: Para placeholders em outros plugins
- **HolographicDisplays**: Para hologramas informativos

### Links Úteis
- **Download**: [Releases GitHub](https://github.com/Hiokase/hLiga/releases)
- **Suporte**: [Discord hPlugins](https://discord.gg/xNpCTaY)
- **Permissões**: [Documentação Completa](GUIA_PERMISSOES.md)
- **API**: [Documentação Completa](API_DOCUMENTATION.md)

---

<div align="center">

**hLiga v1.0.0** - Sistema completo de ligas para Minecraft  
*Desenvolvido para comunidades que buscam excelência competitiva*

[![Download](https://img.shields.io/badge/Download-Latest%20Release-brightgreen.svg)](https://github.com/Hiokase/hLiga)
[![Documentation](https://img.shields.io/badge/Docs-Complete%20Guide-blue.svg)](Dhttps://github.com/Hiokase/hLiga)
[![Support](https://img.shields.io/badge/Support-Community-orange.svg)](https://discord.gg/xNpCTaY)

---

**Licença MIT** • **Compatibilidade Universal** • **Suporte Completo**

</div>