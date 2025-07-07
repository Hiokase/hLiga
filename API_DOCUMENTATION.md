# 🔌 API do hLiga - Documentação Completa

> **Interface completa para desenvolvedores integrarem com o sistema de ligas**  
> 60+ métodos • 10 eventos • Operações síncronas e assíncronas • Singleton pattern

[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://java.com/)
[![Bukkit](https://img.shields.io/badge/Bukkit-API-blue.svg)](https://bukkit.org/)
[![Spigot](https://img.shields.io/badge/Spigot-Compatible-brightgreen.svg)](https://spigotmc.org/)
[![Paper](https://img.shields.io/badge/Paper-Optimized-purple.svg)](https://papermc.io/)

A API do hLiga oferece controle total sobre o sistema de ligas, permitindo que outros plugins interajam com temporadas, pontos, clãs e rankings de forma programática e eficiente.

---

## 🚀 Começando Rapidamente

### Configuração Inicial (2 Minutos)
```java
// 1. Adicionar dependência no plugin.yml
depend: [hLiga]

// 2. Obter instância da API
HLigaAPI api = HLigaAPI.getInstance();

// 3. Verificar se está disponível
if (api != null) {
// Pronto para usar!
int pontos = api.getClanPoints("MinhaGuild");
}
```

### Dependência Maven
```xml
<dependency>
    <groupId>hplugins</groupId>
    <artifactId>hliga</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

### Template Básico
```java
import hplugins.hliga.api.HLigaAPI;

public class MeuPlugin extends JavaPlugin {
    private HLigaAPI hligaAPI;

    @Override
    public void onEnable() {
        // Inicializar API
        this.hligaAPI = HLigaAPI.getInstance();

        if (hligaAPI != null) {
            getLogger().info("Integração com hLiga ativada!");
        } else {
            getLogger().warning("hLiga não encontrado!");
        }
    }
}
```

---

## 📊 Gerenciamento de Pontos

### Métodos Principais
| Método | Descrição | Exemplo |
|--------|-----------|---------|
| `getClanPoints(String)` | Obter pontos do clã | `api.getClanPoints("MinhaGuild")` |
| `addClanPoints(String, int)` | Adicionar pontos | `api.addClanPoints("MinhaGuild", 100)` |
| `removeClanPoints(String, int)` | Remover pontos | `api.removeClanPoints("MinhaGuild", 50)` |
| `setClanPoints(String, int)` | Definir pontos | `api.setClanPoints("MinhaGuild", 500)` |

### Exemplos Práticos
```java
// Sistema de recompensas por kill
@EventHandler
public void onPlayerKill(EntityDeathEvent event) {
    if (event.getEntity().getKiller() instanceof Player) {
        Player killer = event.getEntity().getKiller();
        String clanTag = getClanTag(killer); // Seu método

        if (clanTag != null) {
            // Adicionar pontos pelo kill
            hligaAPI.addClanPointsAsync(clanTag, 10, "Kill em PvP");
        }
    }
}

// Sistema de pontos por evento
public void recompensarEvento(String clanVencedor) {
    // Recompensar vencedor
    hligaAPI.addClanPointsAsync(clanVencedor, 500, "Vitória em evento");

    // Notificar no Discord (automático)
    // Atualizar NPCs (automático)
}
```

### Operações Assíncronas
```java
// Operações não bloqueantes - recomendado
hligaAPI.addClanPointsAsync("MinhaGuild", 100, "Motivo", new Callback<Boolean>() {
    @Override
    public void onResult(Boolean success) {
        if (success) {
            // Pontos adicionados com sucesso
        } else {
            // Erro ao adicionar pontos
        }
    }
});

// Com CompletableFuture (Java 8+)
CompletableFuture<Boolean> future = hligaAPI.addClanPointsAsync("MinhaGuild", 100);
future.thenAccept(success -> {
        if (success) {
getLogger().info("Pontos adicionados!");
    }
            });
```

---

## 🏆 Sistema de Rankings

### Métodos de Ranking
| Método | Descrição | Retorno |
|--------|-----------|---------|
| `getTopClans(int)` | Top N clãs | `List<ClanPoints>` |
| `getClanPosition(String)` | Posição do clã | `int` |
| `getTotalClans()` | Total de clãs | `int` |
| `getTopClanTag()` | Nome do 1º lugar | `String` |

### Exemplos de Ranking
```java
// Top 10 clãs para exibição
List<ClanPoints> top10 = hligaAPI.getTopClans(10);

for (int i = 0; i < top10.size(); i++) {
ClanPoints clan = top10.get(i);
String posicao = String.valueOf(i + 1);
    
    player.sendMessage(String.format(
                               "§e%s. §f%s §7- §a%s pontos",
                       posicao, clan.getClanTag(), clan.getPoints()
    ));
            }

// Verificar posição específica
int posicao = hligaAPI.getClanPosition("MinhaGuild");
if (posicao > 0) {
        player.sendMessage("§7Seu clã está em §e#" + posicao);
} else {
        player.sendMessage("§cSeu clã não está no ranking");
}
```

### Placeholders Dinâmicos
```java
// Criar sistema de placeholders personalizado
public String replacePlaceholders(String text, String clanTag) {
    return text
            .replace("{clan_points}", String.valueOf(hligaAPI.getClanPoints(clanTag)))
            .replace("{clan_position}", String.valueOf(hligaAPI.getClanPosition(clanTag)))
            .replace("{total_clans}", String.valueOf(hligaAPI.getTotalClans()))
            .replace("{top_clan}", hligaAPI.getTopClanTag());
}
```

---

## ⏰ Gerenciamento de Temporadas

### Métodos de Temporada
| Método | Descrição | Retorno |
|--------|-----------|---------|
| `isSeasonActive()` | Temporada ativa? | `boolean` |
| `getCurrentSeason()` | Temporada atual | `Season` |
| `startSeason(String, int)` | Iniciar temporada | `boolean` |
| `endCurrentSeason()` | Encerrar temporada | `boolean` |
| `getSeasonTimeLeft()` | Tempo restante | `long` |

### Controle de Temporadas
```java
// Verificar se temporada está ativa
if (hligaAPI.isSeasonActive()) {
Season atual = hligaAPI.getCurrentSeason();

String nome = atual.getNome();
long tempoRestante = hligaAPI.getSeasonTimeLeft();
    
    player.sendMessage(String.format(
                               "§6Temporada: §e%s §7(§c%d dias restantes§7)",
                       nome, tempoRestante / (24 * 60 * 60 * 1000)
    ));
            }

// Iniciar temporada programaticamente
public void iniciarNovaTemporada() {
    boolean sucesso = hligaAPI.startSeason("Liga Automática", 30);

    if (sucesso) {
        getLogger().info("Nova temporada iniciada!");
    } else {
        getLogger().warning("Falha ao iniciar temporada");
    }
}
```

### Histórico de Temporadas
```java
// Obter histórico completo
List<Season> historico = hligaAPI.getSeasonHistory();

for (Season temporada : historico) {
String nome = temporada.getNome();
Date inicio = temporada.getDataInicio();
Date fim = temporada.getDataFim();

// Processar dados históricos
}
```

---

## 🏷️ Sistema de Tags

### Métodos de Tags
| Método | Descrição | Parâmetros |
|--------|-----------|------------|
| `addPlayerTag(Player, String)` | Adicionar tag | `Player`, `TagType` |
| `removePlayerTag(Player, String)` | Remover tag | `Player`, `TagType` |
| `getPlayerTags(Player)` | Obter tags | `Player` |
| `hasPlayerTag(Player, String)` | Verificar tag | `Player`, `TagType` |

### Gerenciamento de Tags
```java
// Adicionar tag de campeão
Player jogador = event.getPlayer();
String clanTag = getClanTag(jogador);

if (clanTag.equals(hligaAPI.getTopClanTag())) {
        hligaAPI.addPlayerTag(jogador, "CAMPEAO");
    
    jogador.sendMessage("§6Você recebeu a tag de CAMPEÃO!");
}

// Verificar tags do jogador
List<String> tags = hligaAPI.getPlayerTags(jogador);
if (tags.contains("CAMPEAO")) {
        // Jogador tem tag de campeão
        // Dar benefícios especiais
        }
```

---

## 🎯 Sistema de Eventos

### Eventos Disponíveis
| Evento | Quando Dispara | Cancelável |
|--------|---------------|------------|
| `HLigaClanPointsChangeEvent` | Mudança de pontos | ✅ |
| `HLigaSeasonStartEvent` | Início de temporada | ✅ |
| `HLigaSeasonEndEvent` | Fim de temporada | ✅ |
| `HLigaPlayerTagAddEvent` | Adição de tag | ✅ |
| `HLigaPlayerTagRemoveEvent` | Remoção de tag | ✅ |

### Exemplos de Uso
```java
// Interceptar mudanças de pontos
@EventHandler
public void onPointsChange(HLigaClanPointsChangeEvent event) {
    String clanTag = event.getClanTag();
    int pontosAntigos = event.getOldPoints();
    int pontosNovos = event.getNewPoints();
    String motivo = event.getReason();

    // Cancelar se negativo
    if (pontosNovos < 0) {
        event.setCancelled(true);
        event.setCancelReason("Pontos não podem ser negativos");
        return;
    }

    // Log personalizado
    getLogger().info(String.format(
            "Clã %s: %d → %d pontos (%s)",
            clanTag, pontosAntigos, pontosNovos, motivo
    ));
}

// Detectar início de temporada
@EventHandler
public void onSeasonStart(HLigaSeasonStartEvent event) {
    Season temporada = event.getSeason();
    String nome = temporada.getNome();

    // Executar ações personalizadas
    prepararEvento(nome);
    anunciarTemporada(nome);
}
```

### Eventos Before/After
```java
// Antes da mudança (cancelável)
@EventHandler
public void onBeforePointsChange(HLigaClanPointsBeforeChangeEvent event) {
    String clanTag = event.getClanTag();
    int novosPontos = event.getNewPoints();

    // Validação customizada
    if (novosPontos > 1000000) {
        event.setCancelled(true);
        event.setCancelReason("Limite de pontos excedido");
    }
}

// Depois da mudança (não cancelável)
@EventHandler
public void onAfterPointsChange(HLigaClanPointsAfterChangeEvent event) {
    String clanTag = event.getClanTag();
    int pontos = event.getNewPoints();

    // Ações pós-mudança
    atualizarHologramas(clanTag, pontos);
    notificarMembros(clanTag, pontos);
}
```

---

## 🏗️ Criando Providers de Clãs Personalizados

### Sistema de Providers do hLiga
O hLiga utiliza um sistema modular de providers para suportar diferentes plugins de clãs. Você pode criar seu próprio provider para integrar qualquer plugin de clãs com o hLiga.

### Interface BaseClanProvider
```java
import hplugins.hliga.hooks.providers.BaseClanProvider;
import hplugins.hliga.models.GenericClan;

public class MeuClanProvider extends BaseClanProvider {
    
    private MeuPluginDeClans plugin;
    
    public MeuClanProvider() {
        this.plugin = (MeuPluginDeClans) Bukkit.getPluginManager().getPlugin("MeuPluginDeClans");
    }
    
    @Override
    public String getProviderName() {
        return "MeuPluginDeClans";
    }
    
    @Override
    public boolean isAvailable() {
        return plugin != null && plugin.isEnabled();
    }
    
    @Override
    public List<GenericClan> getAllClans() {
        List<GenericClan> clans = new ArrayList<>();
        
        // Obter clãs do seu plugin
        for (MeuClan clan : plugin.getAllClans()) {
            GenericClan genericClan = new GenericClan(
                clan.getTag(),           // Tag/nome do clã
                clan.getDisplayName(),   // Nome de exibição
                clan.getMemberCount(),   // Quantidade de membros
                clan.getLeader(),        // Líder do clã
                clan.getCreationDate()   // Data de criação
            );
            clans.add(genericClan);
        }
        
        return clans;
    }
    
    @Override
    public GenericClan getClan(String clanTag) {
        MeuClan clan = plugin.getClan(clanTag);
        
        if (clan == null) {
            return null;
        }
        
        return new GenericClan(
            clan.getTag(),
            clan.getDisplayName(),
            clan.getMemberCount(),
            clan.getLeader(),
            clan.getCreationDate()
        );
    }
    
    @Override
    public String getPlayerClan(Player player) {
        MeuClan clan = plugin.getPlayerClan(player);
        return clan != null ? clan.getTag() : null;
    }
    
    @Override
    public List<String> getClanMembers(String clanTag) {
        MeuClan clan = plugin.getClan(clanTag);
        
        if (clan == null) {
            return new ArrayList<>();
        }
        
        return clan.getMembers().stream()
            .map(Member::getName)
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean clanExists(String clanTag) {
        return plugin.getClan(clanTag) != null;
    }
}
```

### Registrando Seu Provider
```java
import hplugins.hliga.hooks.ClansManager;

public class MeuPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // Aguardar o hLiga carregar
        Bukkit.getScheduler().runTaskLater(this, () -> {
            registrarProvider();
        }, 20L); // 1 segundo de delay
    }
    
    private void registrarProvider() {
        ClansManager clansManager = ClansManager.getInstance();
        
        if (clansManager != null) {
            // Registrar seu provider
            MeuClanProvider provider = new MeuClanProvider();
            clansManager.registerProvider(provider);
            
            getLogger().info("Provider de clãs registrado com sucesso!");
        } else {
            getLogger().warning("hLiga não encontrado!");
        }
    }
}
```

### Provider Avançado com Eventos
```java
public class MeuClanProviderAvancado extends BaseClanProvider implements Listener {
    
    public MeuClanProviderAvancado() {
        // Registrar eventos para detectar mudanças
        Bukkit.getPluginManager().registerEvents(this, MeuPlugin.getInstance());
    }
    
    // Detectar criação de clã
    @EventHandler
    public void onClanCreate(MeuClanCreateEvent event) {
        String clanTag = event.getClan().getTag();
        
        // Notificar o hLiga sobre novo clã
        ClansManager.getInstance().onClanCreated(clanTag);
        
        // Sincronizar automaticamente
        HLigaAPI.getInstance().syncClans();
    }
    
    // Detectar dissolução de clã
    @EventHandler
    public void onClanDisband(MeuClanDisbandEvent event) {
        String clanTag = event.getClan().getTag();
        
        // Notificar o hLiga sobre clã removido
        ClansManager.getInstance().onClanRemoved(clanTag);
    }
    
    // Detectar mudanças de membros
    @EventHandler
    public void onPlayerJoinClan(MeuPlayerJoinClanEvent event) {
        // Atualizar dados do clã no hLiga
        String clanTag = event.getClan().getTag();
        ClansManager.getInstance().onClanUpdated(clanTag);
    }
    
    @Override
    public boolean supportsRealTimeSync() {
        return true; // Indica que suporta sincronização em tempo real
    }
    
    @Override
    public void enableRealTimeSync() {
        // Ativar sincronização automática
        getLogger().info("Sincronização em tempo real ativada para " + getProviderName());
    }
}
```

### Provider para Plugins com API Externa
```java
public class ExternalAPIClanProvider extends BaseClanProvider {
    
    private ExternalClanAPI api;
    
    public ExternalAPIClanProvider() {
        // Conectar com API externa
        this.api = new ExternalClanAPI("seu-token-aqui");
    }
    
    @Override
    public List<GenericClan> getAllClans() {
        try {
            // Buscar clãs via API REST
            List<ClanData> apiClans = api.getAllClans();
            
            return apiClans.stream()
                .map(this::convertToClan)
                .collect(Collectors.toList());
                
        } catch (APIException e) {
            getLogger().warning("Erro ao buscar clãs da API: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private GenericClan convertToClan(ClanData apiClan) {
        return new GenericClan(
            apiClan.getId(),
            apiClan.getName(),
            apiClan.getMembers().size(),
            apiClan.getLeader().getName(),
            new Date(apiClan.getCreatedAt())
        );
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // Testar conexão com API
            return api.isConnected() && api.testConnection();
        } catch (Exception e) {
            return false;
        }
    }
    
    // Cache para melhorar performance
    private final Map<String, GenericClan> clanCache = new HashMap<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5 minutos
    
    @Override
    public GenericClan getClan(String clanTag) {
        // Verificar cache
        if (System.currentTimeMillis() - lastCacheUpdate > CACHE_TTL) {
            refreshCache();
        }
        
        return clanCache.get(clanTag);
    }
    
    private void refreshCache() {
        clanCache.clear();
        getAllClans().forEach(clan -> clanCache.put(clan.getTag(), clan));
        lastCacheUpdate = System.currentTimeMillis();
    }
}
```

### Provider para Banco de Dados Personalizado
```java
public class DatabaseClanProvider extends BaseClanProvider {
    
    private DataSource dataSource;
    
    public DatabaseClanProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public List<GenericClan> getAllClans() {
        List<GenericClan> clans = new ArrayList<>();
        
        String sql = "SELECT tag, name, leader, created_at, " +
                     "(SELECT COUNT(*) FROM clan_members WHERE clan_tag = c.tag) as member_count " +
                     "FROM clans c WHERE active = 1";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                GenericClan clan = new GenericClan(
                    rs.getString("tag"),
                    rs.getString("name"),
                    rs.getInt("member_count"),
                    rs.getString("leader"),
                    rs.getTimestamp("created_at")
                );
                clans.add(clan);
            }
            
        } catch (SQLException e) {
            getLogger().severe("Erro ao buscar clãs do banco: " + e.getMessage());
        }
        
        return clans;
    }
    
    @Override
    public String getPlayerClan(Player player) {
        String sql = "SELECT clan_tag FROM clan_members WHERE player_name = ? AND active = 1";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, player.getName());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("clan_tag");
                }
            }
            
        } catch (SQLException e) {
            getLogger().severe("Erro ao buscar clã do jogador: " + e.getMessage());
        }
        
        return null;
    }
    
    @Override
    public List<String> getClanMembers(String clanTag) {
        List<String> members = new ArrayList<>();
        String sql = "SELECT player_name FROM clan_members WHERE clan_tag = ? AND active = 1";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, clanTag);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    members.add(rs.getString("player_name"));
                }
            }
            
        } catch (SQLException e) {
            getLogger().severe("Erro ao buscar membros do clã: " + e.getMessage());
        }
        
        return members;
    }
}
```

### Configuração no config.yml
```yaml
# Configurar qual provider usar
clan_plugin: "MeuPluginDeClans"

# Configurações específicas do provider
providers:
  MeuPluginDeClans:
    enabled: true
    auto_sync: true
    sync_interval: 300  # 5 minutos
    
  ExternalAPI:
    enabled: false
    api_token: "seu-token-aqui"
    api_url: "https://api.meuservidor.com"
    cache_ttl: 300
    
  Database:
    enabled: false
    connection_string: "jdbc:mysql://localhost:3306/clans"
    username: "user"
    password: "pass"
```

### Testando Seu Provider
```java
public class ProviderTester {
    
    public static void testProvider(BaseClanProvider provider) {
        System.out.println("=== Testando Provider: " + provider.getProviderName() + " ===");
        
        // Teste 1: Disponibilidade
        System.out.println("Disponível: " + provider.isAvailable());
        
        if (!provider.isAvailable()) {
            System.out.println("Provider não está disponível!");
            return;
        }
        
        // Teste 2: Listar clãs
        List<GenericClan> clans = provider.getAllClans();
        System.out.println("Total de clãs: " + clans.size());
        
        for (GenericClan clan : clans.subList(0, Math.min(5, clans.size()))) {
            System.out.println("- " + clan.getTag() + " (" + clan.getMemberCount() + " membros)");
        }
        
        // Teste 3: Buscar clã específico
        if (!clans.isEmpty()) {
            String testTag = clans.get(0).getTag();
            GenericClan testClan = provider.getClan(testTag);
            System.out.println("Teste getClan(" + testTag + "): " + (testClan != null ? "OK" : "ERRO"));
        }
        
        // Teste 4: Verificar jogador online
        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        if (players.length > 0) {
            String playerClan = provider.getPlayerClan(players[0]);
            System.out.println("Clã do jogador " + players[0].getName() + ": " + playerClan);
        }
        
        System.out.println("=== Teste concluído ===");
    }
}
```

### Exemplo Completo: Provider para Factions
```java
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;

public class FactionsProvider extends BaseClanProvider {
    
    @Override
    public String getProviderName() {
        return "Factions";
    }
    
    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("Factions") != null;
    }
    
    @Override
    public List<GenericClan> getAllClans() {
        List<GenericClan> clans = new ArrayList<>();
        
        for (Faction faction : Factions.getInstance().getAllFactions()) {
            // Ignorar wilderness e safezone
            if (faction.isWilderness() || faction.isSafeZone() || faction.isWarZone()) {
                continue;
            }
            
            GenericClan clan = new GenericClan(
                faction.getTag(),
                faction.getDescription(),
                faction.getFPlayers().size(),
                faction.getFPlayerAdmin() != null ? faction.getFPlayerAdmin().getName() : "Nenhum",
                new Date(faction.getFoundedDate())
            );
            
            clans.add(clan);
        }
        
        return clans;
    }
    
    @Override
    public GenericClan getClan(String clanTag) {
        Faction faction = Factions.getInstance().getByTag(clanTag);
        
        if (faction == null || faction.isWilderness()) {
            return null;
        }
        
        return new GenericClan(
            faction.getTag(),
            faction.getDescription(),
            faction.getFPlayers().size(),
            faction.getFPlayerAdmin() != null ? faction.getFPlayerAdmin().getName() : "Nenhum",
            new Date(faction.getFoundedDate())
        );
    }
    
    @Override
    public String getPlayerClan(Player player) {
        FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
        
        if (fplayer.hasFaction()) {
            return fplayer.getFaction().getTag();
        }
        
        return null;
    }
    
    @Override
    public List<String> getClanMembers(String clanTag) {
        Faction faction = Factions.getInstance().getByTag(clanTag);
        
        if (faction == null) {
            return new ArrayList<>();
        }
        
        return faction.getFPlayers().stream()
            .map(FPlayer::getName)
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean clanExists(String clanTag) {
        Faction faction = Factions.getInstance().getByTag(clanTag);
        return faction != null && !faction.isWilderness();
    }
}
```

---

## 🔗 Integração com Outros Plugins

### PlaceholderAPI
```java
// Registrar placeholders customizados
public class MeuPlaceholderExpansion extends PlaceholderExpansion {

    private HLigaAPI hligaAPI;

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params.equals("clan_points")) {
            String clanTag = getClanTag(player);
            return String.valueOf(hligaAPI.getClanPoints(clanTag));
        }

        if (params.equals("clan_position")) {
            String clanTag = getClanTag(player);
            return String.valueOf(hligaAPI.getClanPosition(clanTag));
        }

        return null;
    }
}
```

### Essentials/EssentialsX
```java
// Integrar com sistema de economia
public void recompensarPorPosicao() {
    List<ClanPoints> top3 = hligaAPI.getTopClans(3);

    for (int i = 0; i < top3.size(); i++) {
        ClanPoints clan = top3.get(i);
        double premio = (i == 0) ? 10000 : (i == 1) ? 5000 : 2500;

        // Dar dinheiro para membros do clã
        List<Player> membros = getClanMembers(clan.getClanTag());
        for (Player membro : membros) {
            EssentialsAPI.getUser(membro).giveMoney(BigDecimal.valueOf(premio));
        }
    }
}
```

### Citizens/NPCs
```java
// Sincronizar com NPCs customizados
public void atualizarNPCsCustomizados() {
    List<ClanPoints> top3 = hligaAPI.getTopClans(3);

    for (int i = 0; i < top3.size(); i++) {
        ClanPoints clan = top3.get(i);
        NPC npc = getMeuNPC(i + 1); // Seu método

        // Atualizar nome do NPC
        npc.setName(String.format("§e%dº §f%s §7(%s pts)",
                i + 1, clan.getClanTag(), clan.getPoints()));
    }
}
```

---

## 🔧 Utilitários e Helpers

### Formatação de Números
```java
// Formatação automática de pontos
String pontosFormatados = hligaAPI.formatPoints(1234567);
// Resultado: "1.234.567"

// Formatação customizada
public String formatarPontos(int pontos) {
    return NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"))
            .format(pontos);
}
```

### Validação de Clãs
```java
// Verificar se clã existe
if (hligaAPI.clanExists("MinhaGuild")) {
// Clã existe no sistema
int pontos = hligaAPI.getClanPoints("MinhaGuild");
} else {
        // Clã não encontrado
        player.sendMessage("§cClã não encontrado!");
}

// Sincronizar clãs manualmente
        hligaAPI.syncClans(); // Busca novos clãs do plugin de clãs
```

### Cache e Performance
```java
// Cache local para evitar queries excessivas
private final Map<String, Integer> cachePoints = new HashMap<>();
private long lastUpdate = 0;

public int getClanPointsCached(String clanTag) {
    // Atualizar cache a cada 30 segundos
    if (System.currentTimeMillis() - lastUpdate > 30000) {
        cachePoints.clear();
        lastUpdate = System.currentTimeMillis();
    }

    return cachePoints.computeIfAbsent(clanTag,
            tag -> hligaAPI.getClanPoints(tag));
}
```

---

## 📋 Referência Completa de Métodos

### Gerenciamento de Pontos
```java
// Síncronos
int getClanPoints(String clanTag)
boolean addClanPoints(String clanTag, int points)
boolean addClanPoints(String clanTag, int points, String reason)
boolean removeClanPoints(String clanTag, int points)
boolean setClanPoints(String clanTag, int points)

// Assíncronos
CompletableFuture<Integer> getClanPointsAsync(String clanTag)
CompletableFuture<Boolean> addClanPointsAsync(String clanTag, int points)
CompletableFuture<Boolean> addClanPointsAsync(String clanTag, int points, String reason)
CompletableFuture<Boolean> removeClanPointsAsync(String clanTag, int points)
CompletableFuture<Boolean> setClanPointsAsync(String clanTag, int points)
```

### Rankings
```java
List<ClanPoints> getTopClans(int limit)
int getClanPosition(String clanTag)
int getTotalClans()
String getTopClanTag()
int getTopClanPoints()
```

### Temporadas
```java
boolean isSeasonActive()
Season getCurrentSeason()
List<Season> getSeasonHistory()
boolean startSeason(String name, int days)
boolean startSeason(String name, Date endDate)
boolean endCurrentSeason()
long getSeasonTimeLeft()
String getSeasonTimeLeftFormatted()
```

### Tags
```java
boolean addPlayerTag(Player player, String tagType)
boolean removePlayerTag(Player player, String tagType)
List<String> getPlayerTags(Player player)
boolean hasPlayerTag(Player player, String tagType)
```

### Utilitários
```java
boolean clanExists(String clanTag)
void syncClans()
String formatPoints(int points)
boolean reload()
```

---

## 🚨 Tratamento de Erros

### Exceções Comuns
```java
try {
int pontos = hligaAPI.getClanPoints("ClanInexistente");
} catch (ClanNotFoundException e) {
        // Clã não encontrado
        player.sendMessage("§cClã não existe!");
} catch (DatabaseException e) {
// Erro de banco de dados
getLogger().severe("Erro de BD: " + e.getMessage());
        } catch (APIException e) {
// Erro geral da API
getLogger().warning("Erro na API: " + e.getMessage());
        }
```

### Validação de Entrada
```java
public boolean adicionarPontosSafe(String clanTag, int pontos) {
    // Validar parâmetros
    if (clanTag == null || clanTag.trim().isEmpty()) {
        return false;
    }

    if (pontos <= 0) {
        return false;
    }

    // Verificar se clã existe
    if (!hligaAPI.clanExists(clanTag)) {
        return false;
    }

    // Executar operação
    return hligaAPI.addClanPoints(clanTag, pontos);
}
```

---

## 🎯 Exemplos Práticos

### Sistema de Quests
```java
public class QuestSystem {

    @EventHandler
    public void onQuestComplete(QuestCompleteEvent event) {
        Player player = event.getPlayer();
        String clanTag = getClanTag(player);

        if (clanTag != null) {
            // Recompensar clã por quest
            int pontos = calculateQuestPoints(event.getQuest());
            hligaAPI.addClanPointsAsync(clanTag, pontos, "Quest: " + event.getQuest().getName());

            // Notificar jogador
            player.sendMessage(String.format(
                    "§a+%d pontos para o clã %s!", pontos, clanTag
            ));
        }
    }
}
```

### Sistema de Guerra
```java
public class WarSystem {

    public void finalizarGuerra(String clanVencedor, String clanPerdedor) {
        // Recompensar vencedor
        hligaAPI.addClanPointsAsync(clanVencedor, 1000, "Vitória na guerra");

        // Penalizar perdedor
        hligaAPI.removeClanPointsAsync(clanPerdedor, 500, "Derrota na guerra");

        // Anunciar resultado
        Bukkit.broadcastMessage(String.format(
                "§6Guerra: §e%s §avenceu §e%s§a!",
                clanVencedor, clanPerdedor
        ));
    }
}
```

### Sistema de Eventos
```java
public class EventManager {

    public void criarEventoAutomatico() {
        // Aguardar fim de temporada
        if (hligaAPI.isSeasonActive()) {
            long tempoRestante = hligaAPI.getSeasonTimeLeft();

            // Criar evento se restam menos de 3 dias
            if (tempoRestante < 3 * 24 * 60 * 60 * 1000) {
                criarEventoFinalTemporada();
            }
        }
    }

    private void criarEventoFinalTemporada() {
        // Dobrar pontos por 24 horas
        List<ClanPoints> top10 = hligaAPI.getTopClans(10);

        Bukkit.broadcastMessage("§6Evento Final da Temporada ativado!");
        Bukkit.broadcastMessage("§ePontos em dobro pelas próximas 24 horas!");
    }
}
```

---

<div align="center">

**hLiga API v1.0.0** - Interface Completa para Desenvolvedores  
*Integre seu plugin com o sistema de ligas mais avançado*

[![Javadoc](https://img.shields.io/badge/Javadoc-Available-brightgreen.svg)](https://github.com/)
[![Examples](https://img.shields.io/badge/Examples-Repository-blue.svg)](https://github.com/)
[![Support](https://img.shields.io/badge/Support-Discord-purple.svg)](https://discord.com/)

---

**60+ Métodos** • **10 Eventos** • **Thread-Safe** • **Async Ready**

</div>