package hplugins.hliga.commands;

import hplugins.hliga.Main;
import hplugins.hliga.config.Messages;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.PlayerTag;
import hplugins.hliga.models.TagType;
import hplugins.hliga.utils.LogUtils;
import hplugins.hliga.utils.VersionUtils;
import hplugins.hliga.utils.ClickableTextUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

public class HLigaCommand implements CommandExecutor, TabCompleter {
    
    private final Main plugin;
    private final Messages messages;
    
    public HLigaCommand(Main plugin) {
        this.plugin = plugin;
        this.messages = plugin.getConfigManager().getMessages();
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                new hplugins.hliga.inventory.menus.MainMenu(plugin, (Player) sender).open(1);
            } else {
                showHelp(sender);
            }
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "addpoints":
                return handleAddPoints(sender, args);
            case "removepoints":
                return handleRemovePoints(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "top":
                return handleTop(sender, args);
            case "reload":
                return handleReload(sender);
            case "menu":
                return handleMenu(sender);
            case "sync":
                return handleDatabaseSync(sender, args);
            case "topnpc":
                return handleTopNPC(sender, args);
            case "tags":
                return handleTags(sender, args);
            case "tag":
                return handleTag(sender, args);
            case "help":
                return handleHelp(sender, args);
            default:
                showHelp(sender);
                return true;
        }
    }
    
    private boolean handleAddPoints(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hliga.addpoints")) {
            sender.sendMessage(messages.getMessage("geral.sem_permissao"));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(messages.getMessage("geral.formato_invalido", "{formato}", "/hliga addpoints <jogador> <quantidade> [-d descrição]"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(messages.getMessage("geral.jogador_nao_encontrado"));
            return true;
        }
        
        int points;
        try {
            points = Integer.parseInt(args[2]);
            if (points <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(messages.getMessage("geral.valor_invalido"));
            return true;
        }
        
        
        String description = null;
        for (int i = 3; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-d") && i + 1 < args.length) {
                
                StringBuilder sb = new StringBuilder();
                for (int j = i + 1; j < args.length; j++) {
                    sb.append(args[j]).append(" ");
                }
                description = sb.toString().trim();
                break;
            }
        }
        
        
        String clanTag = plugin.getClansManager().getPlayerClanTag(target);
        if (clanTag == null) {
            sender.sendMessage(messages.getMessage("geral.clan_nao_encontrado"));
            return true;
        }
        
        boolean success = plugin.getPointsManager().addPoints(clanTag, points, description);
        if (success) {
            String pointsName = points == 1 ? 
                    plugin.getConfigManager().getConfig().getString("pontos.nome", "ponto") : 
                    plugin.getConfigManager().getConfig().getString("pontos.nome_plural", "pontos");
            
            
            if (description != null && !description.isEmpty()) {
                sender.sendMessage(messages.getMessage("pontos.adicionado_admin_desc", 
                        "{pontos}", plugin.getPointsManager().formatPoints(points),
                        "{pontos_nome}", pointsName,
                        "{clan}", clanTag,
                        "{descricao}", description));
            } else {
                sender.sendMessage(messages.getMessage("pontos.adicionado_admin", 
                        "{pontos}", plugin.getPointsManager().formatPoints(points),
                        "{pontos_nome}", pointsName,
                        "{clan}", clanTag));
            }
            
            
            
            String coloredTag = plugin.getClansManager().getColoredClanTag(clanTag);
            
            
            LogUtils.debugHigh("Enviando notificação para membros do clã: " + clanTag);
            
            
            List<Player> onlineMembers = plugin.getClansManager().getOnlineClanMembers(clanTag);
            LogUtils.debugMedium("Quantidade de membros online do clã " + clanTag + ": " + onlineMembers.size());
            
            
            String message;
            if (description != null && !description.isEmpty()) {
                message = messages.getMessage("pontos.adicionado_clan_desc", 
                        "{pontos}", plugin.getPointsManager().formatPoints(points),
                        "{pontos_nome}", pointsName,
                        "{clan}", coloredTag,
                        "{descricao}", description);
            } else {
                message = messages.getMessage("pontos.adicionado_clan", 
                        "{pontos}", plugin.getPointsManager().formatPoints(points),
                        "{pontos_nome}", pointsName,
                        "{clan}", coloredTag);
            }
            
            
            for (Player member : onlineMembers) {
                LogUtils.debug("Enviando notificação de pontos para: " + member.getName());
                member.sendMessage(message);
            }
        } else {
            sender.sendMessage(messages.getMessage("geral.erro_interno"));
        }
        
        return true;
    }
    
    private boolean handleRemovePoints(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hliga.removepoints")) {
            sender.sendMessage(messages.getMessage("geral.sem_permissao"));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(messages.getMessage("geral.formato_invalido", "{formato}", "/hliga removepoints <jogador> <quantidade> [-d descrição]"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(messages.getMessage("geral.jogador_nao_encontrado"));
            return true;
        }
        
        int points;
        try {
            points = Integer.parseInt(args[2]);
            if (points <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(messages.getMessage("geral.valor_invalido"));
            return true;
        }
        
        
        String description = null;
        for (int i = 3; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-d") && i + 1 < args.length) {
                
                StringBuilder sb = new StringBuilder();
                for (int j = i + 1; j < args.length; j++) {
                    sb.append(args[j]).append(" ");
                }
                description = sb.toString().trim();
                break;
            }
        }
        
        
        String clanTag = plugin.getClansManager().getPlayerClanTag(target);
        if (clanTag == null) {
            sender.sendMessage(messages.getMessage("geral.clan_nao_encontrado"));
            return true;
        }
        
        boolean success = plugin.getPointsManager().removePoints(clanTag, points, description);
        if (success) {
            String pointsName = points == 1 ? 
                    plugin.getConfigManager().getConfig().getString("pontos.nome", "ponto") : 
                    plugin.getConfigManager().getConfig().getString("pontos.nome_plural", "pontos");
            
            
            if (description != null && !description.isEmpty()) {
                sender.sendMessage(messages.getMessage("pontos.removido_admin_desc", 
                        "{pontos}", plugin.getPointsManager().formatPoints(points),
                        "{pontos_nome}", pointsName,
                        "{clan}", clanTag,
                        "{descricao}", description));
            } else {
                sender.sendMessage(messages.getMessage("pontos.removido_admin", 
                        "{pontos}", plugin.getPointsManager().formatPoints(points),
                        "{pontos_nome}", pointsName,
                        "{clan}", clanTag));
            }
            
            
            
            String coloredTag = plugin.getClansManager().getColoredClanTag(clanTag);
            
            
            LogUtils.debugHigh("Enviando notificação de remoção de pontos para membros do clã: " + clanTag);
            
            
            List<Player> onlineMembers = plugin.getClansManager().getOnlineClanMembers(clanTag);
            LogUtils.debugMedium("Quantidade de membros online do clã " + clanTag + " para remoção: " + onlineMembers.size());
            
            
            String message;
            if (description != null && !description.isEmpty()) {
                message = messages.getMessage("pontos.removido_clan_desc", 
                        "{pontos}", plugin.getPointsManager().formatPoints(points),
                        "{pontos_nome}", pointsName,
                        "{clan}", coloredTag,
                        "{descricao}", description);
            } else {
                message = messages.getMessage("pontos.removido_clan", 
                        "{pontos}", plugin.getPointsManager().formatPoints(points),
                        "{pontos_nome}", pointsName,
                        "{clan}", coloredTag);
            }
            
            
            for (Player member : onlineMembers) {
                LogUtils.debug("Enviando notificação de remoção de pontos para: " + member.getName());
                member.sendMessage(message);
            }
        } else {
            sender.sendMessage(messages.getMessage("geral.erro_interno"));
        }
        
        return true;
    }
    
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(messages.getMessage("geral.formato_invalido", "{formato}", "/hliga info <clan>"));
            return true;
        }
        
        String clanTag = args[1];
        
        
        if (!plugin.getClansManager().clanExists(clanTag)) {
            sender.sendMessage(messages.getMessage("geral.clan_nao_encontrado"));
            return true;
        }
        
        int points = plugin.getPointsManager().getClanPoints(clanTag);
        String pointsName = points == 1 ? 
                plugin.getConfigManager().getConfig().getString("pontos.nome", "ponto") : 
                plugin.getConfigManager().getConfig().getString("pontos.nome_plural", "pontos");
        
        
        String coloredTag = plugin.getClansManager().getColoredClanTag(clanTag);
        
        sender.sendMessage(messages.getMessage("pontos.info", 
                "{clan}", coloredTag,
                "{pontos}", plugin.getPointsManager().formatPoints(points),
                "{pontos_nome}", pointsName));
        
        return true;
    }
    
    private boolean handleTop(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
                if (page <= 0) {
                    sender.sendMessage(messages.getMessage("clans.top_pagina_invalida"));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(messages.getMessage("geral.valor_invalido"));
                return true;
            }
        }
        
        
        List<ClanPoints> allClans = plugin.getPointsManager().getTopClans(1000); 
        
        if (allClans.isEmpty()) {
            sender.sendMessage(messages.getMessage("clans.top_vazio"));
            return true;
        }
        
        
        int clansPorPagina = 10;
        int totalPaginas = (int) Math.ceil((double) allClans.size() / clansPorPagina);
        
        if (page > totalPaginas) {
            sender.sendMessage(messages.getMessage("clans.top_pagina_invalida"));
            return true;
        }
        
        int inicioIndex = (page - 1) * clansPorPagina;
        int fimIndex = Math.min(inicioIndex + clansPorPagina, allClans.size());
        
        
        
        
        List<String> header = messages.getStringList("clans.top_header");
        for (String linha : header) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', linha));
        }
        
        
        if (totalPaginas > 1) {
            String infoPagina = messages.getMessage("clans.top_paginacao.info")
                    .replace("{pagina_atual}", String.valueOf(page))
                    .replace("{total_paginas}", String.valueOf(totalPaginas));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', infoPagina));
        }
        
        
        for (int i = inicioIndex; i < fimIndex; i++) {
            ClanPoints clanPoints = allClans.get(i);
            String clanTag = clanPoints.getClanTag();
            
            
            String coloredTag = plugin.getClansManager().getColoredClanTag(clanTag);
            
            
            String pointsName = clanPoints.getPoints() == 1 ? 
                    plugin.getConfigManager().getConfig().getString("pontos.nome", "ponto") : 
                    plugin.getConfigManager().getConfig().getString("pontos.nome_plural", "pontos");
            
            
            String posicao = String.valueOf(i + 1);
            String medalha = i == 0 ? "&6🥇" : i == 1 ? "&7🥈" : i == 2 ? "&c🥉" : "&e" + posicao + ".";
            
            String linhaFormatada = "&f" + medalha + " &f" + coloredTag + " &7- &a" + 
                    plugin.getPointsManager().formatPoints(clanPoints.getPoints()) + " " + pointsName;
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', linhaFormatada));
        }
        
        
        if (totalPaginas > 1) {
            String anteriorText = messages.getMessage("clans.top_paginacao.anterior");
            String proximaText = messages.getMessage("clans.top_paginacao.proxima");
            String infoText = messages.getMessage("clans.top_paginacao.info");
            
            ClickableTextUtils.sendPaginationLine(sender, page, totalPaginas, "/liga top", 
                                                anteriorText, proximaText, infoText);
        }
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("hliga.admin")) {
            sender.sendMessage(messages.getMessage("geral.sem_permissao"));
            return true;
        }
        
        plugin.reload();
        sender.sendMessage(messages.getMessage("geral.reload_completo"));
        return true;
    }
    
    /**
     * Gerencia a sincronização e transferência entre diferentes tipos de banco de dados
     * 
     * @param sender Remetente do comando
     * @param args Argumentos do comando
     * @return true sempre
     */
    private boolean handleDatabaseSync(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hliga.admin")) {
            sender.sendMessage(messages.getMessage("geral.sem_permissao"));
            return true;
        }
        
        if (args.length < 2) {
            List<String> ajudaSync = messages.getStringList("database_sync.ajuda");
            for (String linha : ajudaSync) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', linha));
            }
            return true;
        }
        
        String source = args.length > 1 ? args[1].toLowerCase() : "";
        String target = args.length > 2 ? args[2].toLowerCase() : "";
        
        
        if (source.equals("diagnose") || source.equals("mostrartodos") || 
                source.equals("sincronizar") || source.equals("showall")) {
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&cComando &f/hliga " + source + " &cfoi removido."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&7Use &f/hliga sync &7para transferência de dados entre bancos de dados."));
            return true;
        }
        
        
        if (args.length < 3) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&cÉ necessário especificar a origem E o destino."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&7Exemplo: &f/hliga sync sqlite mysql"));
            return true;
        }
        
        
        if (!isValidDatabaseType(source) || !isValidDatabaseType(target)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&cTipos de banco de dados inválidos. Use: sqlite, mysql ou redis"));
            return true;
        }
        
        if (source.equals(target)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&cA origem e o destino não podem ser do mesmo tipo!"));
            return true;
        }
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&aIniciando transferência de dados de &f" + source + " &apara &f" + target + "&a..."));
        
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                
                long startTime = System.currentTimeMillis();
                
                
                boolean success = plugin.getDatabaseManager().transferData(source, target);
                
                
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                "&aTransferência concluída com sucesso em &f" + duration + "ms&a!"));
                    } else {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                "&cErro ao transferir dados entre os bancos."));
                    }
                });
            } catch (Exception e) {
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            "&cErro ao transferir dados: &f" + e.getMessage()));
                    LogUtils.severe("Erro ao transferir dados: " + e.getMessage(), e);
                });
            }
        });
        
        return true;
    }
    
    /**
     * Verifica se o tipo de banco de dados é válido
     * 
     * @param type Tipo de banco de dados
     * @return true se for válido
     */
    private boolean isValidDatabaseType(String type) {
        return type.equals("sqlite") || type.equals("mysql") || type.equals("redis");
    }
    
    private boolean handleMenu(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getMessage("geral.somente_jogador"));
            return true;
        }
        
        new hplugins.hliga.inventory.menus.MainMenu(plugin, (Player) sender).open(1);
        return true;
    }
    
    /**
     * Verifica se as dependências necessárias para NPCs estão disponíveis
     * 
     * @param sender Remetente do comando para enviar mensagens de erro
     * @return true se todas as dependências estão disponíveis
     */
    private boolean checkNPCDependencies(CommandSender sender) {
        
        if (!plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&c❌ Sistema de NPCs indisponível!"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&7O plugin &eCitizens2 &7é necessário para usar NPCs de ranking."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&7Instale o Citizens2 para usar este recurso."));
            return false;
        }
        
        
        boolean hasHologramPlugin = plugin.getServer().getPluginManager().isPluginEnabled("HolographicDisplays") ||
                                   plugin.getServer().getPluginManager().isPluginEnabled("DecentHolograms");
        
        if (!hasHologramPlugin) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&c❌ Sistema de hologramas indisponível!"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&7É necessário &eHolographicDisplays &7ou &eDecentHolograms &7para NPCs funcionarem."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&7Instale um dos plugins de holograma para usar este recurso."));
            return false;
        }
        
        return true;
    }
    
    /**
     * Manipula o comando para criar NPCs de ranking
     * 
     * @param sender Remetente do comando
     * @param args Argumentos
     * @return true se o comando foi processado
     */
    private boolean handleTopNPC(CommandSender sender, String[] args) {
        
        if (!checkNPCDependencies(sender)) {
            return true;
        }
        
        
        if (!sender.hasPermission("hliga.topnpc.create") && !sender.hasPermission("hliga.topnpc.remove") && 
            !sender.hasPermission("hliga.topnpc.list") && !sender.hasPermission("hliga.topnpc.update")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    messages.getMessage("geral.sem_permissao")));
            return true;
        }
        
        
        if (!(sender instanceof Player) && args.length > 1 && !args[1].equalsIgnoreCase("list") && !args[1].equalsIgnoreCase("update")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    messages.getMessage("geral.somente_jogador")));
            return true;
        }
        
        
        if (args.length < 2) {
            showNPCHelp(sender);
            return true;
        }
        
        Player player = (sender instanceof Player) ? (Player) sender : null;
        
        
        switch (args[1].toLowerCase()) {
            case "create":
                if (!sender.hasPermission("hliga.topnpc.create")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("geral.sem_permissao")));
                    return true;
                }
                
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("geral.formato_invalido").replace("{formato}", "/hliga topnpc create <id> <posição>")));
                    return true;
                }
                
                String standId = args[2];
                int position;
                
                try {
                    position = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("geral.valor_invalido")));
                    return true;
                }
                
                if (position < 1 || position > 100) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("npcs.posicao_invalida")));
                    return true;
                }
                
                if (plugin.getNpcManager().npcExists(standId)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("npcs.ja_existe").replace("{id}", standId)));
                    return true;
                }
                
                boolean created = plugin.getNpcManager().createNPCFromCommand(standId, position, player.getLocation());
                
                if (created) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("npcs.criado")
                                    .replace("NPC", "NPC")
                                    .replace("{id}", standId)
                                    .replace("{posicao}", String.valueOf(position))));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("geral.erro_interno")));
                }
                
                return true;
                
            case "remove":
                if (!sender.hasPermission("hliga.topnpc.remove")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("geral.sem_permissao")));
                    return true;
                }
                
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("geral.formato_invalido").replace("{formato}", "/hliga topnpc remove <id>")));
                    return true;
                }
                
                String removeId = args[2];
                
                if (!plugin.getNpcManager().npcExists(removeId)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("npcs.nao_encontrado")
                                    .replace("{id}", removeId)));
                    return true;
                }
                
                boolean removed = plugin.getNpcManager().removeNPC(removeId);
                
                if (removed) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("npcs.removido")
                                    .replace("NPC", "NPC")
                                    .replace("{id}", removeId)));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("geral.erro_interno")));
                }
                
                return true;
                
            case "list":
                if (!sender.hasPermission("hliga.topnpc.list")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("geral.sem_permissao")));
                    return true;
                }
                
                Set<String> npcIds = plugin.getNpcManager().getAllNPCIds();
                
                if (npcIds.isEmpty()) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("npcs.lista_vazia")));
                    return true;
                }
                
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        messages.getMessage("npcs.lista_header")));
                
                for (String id : npcIds) {
                    
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            "&7- &e" + id + " &7(NPC do Citizens2)"));
                }
                
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        messages.getMessage("npcs.lista_footer")));
                
                return true;
                
            case "update":
                if (!sender.hasPermission("hliga.topnpc.update")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("geral.sem_permissao")));
                    return true;
                }
                
                if (args.length < 3) {
                    
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            "&eAtualizando NPCs... Aguarde alguns segundos."));
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        
                        plugin.getSeasonManager().forceRefreshRankings();
                        
                        int atualizados = plugin.getNpcManager().updateAllNPCs();
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                messages.getMessage("npcs.todos_atualizados")));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                "&7Total de " + atualizados + " NPCs atualizados com dados do banco."));
                    }, 40L); 
                    return true;
                }
                
                String updateId = args[2];
                
                if (!plugin.getNpcManager().npcExists(updateId)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            messages.getMessage("npcs.nao_encontrado").replace("{id}", updateId)));
                    return true;
                }
                
                
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        "&eAtualizando NPC " + updateId + "... Aguarde alguns segundos."));
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    
                    plugin.getSeasonManager().forceRefreshRankings();
                    
                    boolean updated = plugin.getNpcManager().updateNPC(updateId);
                    if (updated) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                messages.getMessage("npcs.atualizado").replace("{id}", updateId)));
                    } else {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                messages.getMessage("npcs.erro_atualizar").replace("{id}", updateId)));
                    }
                }, 40L); 
                
                return true;
                
            case "help":
                return handleNPCHelp(sender, args);
            default:
                showNPCHelp(sender);
                return true;
        }
    }
    
    /**
     * Mostra a ajuda do comando de NPC de ranking
     * 
     * @param sender Remetente do comando
     */
    private void showNPCHelp(CommandSender sender) {
        showNPCHelp(sender, 1);
    }
    
    private void showNPCHelp(CommandSender sender, int page) {
        
        List<String> allMessages = messages.getStringList("npcs.ajuda");
        
        
        List<String> filteredMessages = new ArrayList<>();
        
        for (String message : allMessages) {
            String lowerMessage = message.toLowerCase();
            
            
            if (message.contains("COMANDOS DE NPC") || message.contains("----")) {
                filteredMessages.add(message);
                continue;
            }
            
            
            boolean shouldInclude = false;
            
            if (lowerMessage.contains("create") && sender.hasPermission("hliga.topnpc.create")) {
                shouldInclude = true;
            } else if (lowerMessage.contains("remove") && sender.hasPermission("hliga.topnpc.remove")) {
                shouldInclude = true;
            } else if (lowerMessage.contains("list") && sender.hasPermission("hliga.topnpc.list")) {
                shouldInclude = true;
            } else if (lowerMessage.contains("update") && sender.hasPermission("hliga.topnpc.update")) {
                shouldInclude = true;
            }
            
            if (shouldInclude) {
                filteredMessages.add(message);
            }
        }
        
        
        int linesPerPage = messages.getConfig().getInt("ajuda_paginacao.linhas_por_pagina", 10);
        int totalPages = (int) Math.ceil((double) filteredMessages.size() / linesPerPage);
        
        if (page < 1 || page > totalPages) {
            page = 1;
        }
        
        int startIndex = (page - 1) * linesPerPage;
        int endIndex = Math.min(startIndex + linesPerPage, filteredMessages.size());
        
        
        if (totalPages > 1) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&8| &7Página &f" + page + "&7/&f" + totalPages + " &8|"));
        }
        
        
        for (int i = startIndex; i < endIndex; i++) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', filteredMessages.get(i)));
        }
        
        
        if (totalPages > 1) {
            String anteriorText = "&a◀ Anterior";
            String proximaText = "&aPróxima ▶";
            String infoText = "&8[&7Página &f{pagina_atual}&7/&f{total_paginas}&8]";
            
            ClickableTextUtils.sendPaginationLine(sender, page, totalPages, "/liga topnpc help", 
                                                anteriorText, proximaText, infoText);
        }
    }
    
    private boolean handleNPCHelp(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length >= 3) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                page = 1;
            }
        }
        showNPCHelp(sender, page);
        return true;
    }
    

    
    
    
    
    
    
    
    
    
    
    
    
    

    
    private boolean handleTags(CommandSender sender, String[] args) {
        if (!plugin.getTagManager().isSystemEnabled()) {
            sender.sendMessage(ChatColor.RED + "Sistema de tags está desabilitado.");
            return true;
        }
        
        if (args.length == 1) {
            
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Apenas jogadores podem ver suas próprias tags.");
                return true;
            }
            
            Player player = (Player) sender;
            List<PlayerTag> tags = plugin.getTagManager().getPlayerTags(player.getUniqueId());
            
            if (tags.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "Você não possui tags.");
                return true;
            }
            
            sender.sendMessage(ChatColor.GOLD + "=== Suas Tags ===");
            for (PlayerTag tag : tags) {
                String tagDisplay = ChatColor.translateAlternateColorCodes('&', tag.getFormattedTag());
                String type = tag.getTagType() == TagType.RANKING ? "Ranking" : "Temporada";
                sender.sendMessage(ChatColor.GRAY + "• " + tagDisplay + ChatColor.GRAY + " (" + type + ")");
            }
            
            return true;
        }
        
        if (args.length == 2) {
            String subCommand = args[1].toLowerCase();
            
            if (subCommand.equals("reload")) {
                if (!sender.hasPermission("hliga.admin")) {
                    sender.sendMessage(messages.getMessage("geral.sem_permissao"));
                    return true;
                }
                
                plugin.getTagManager().loadConfig();
                sender.sendMessage(ChatColor.GREEN + "Configuração de tags recarregada!");
                return true;
            }
            
            if (subCommand.equals("update")) {
                if (!sender.hasPermission("hliga.admin")) {
                    sender.sendMessage(messages.getMessage("geral.sem_permissao"));
                    return true;
                }
                
                plugin.getTagManager().forceUpdate();
                List<String> mensagem = messages.getStringList("sistema.tags_atualizadas");
                for (String linha : mensagem) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', linha));
                }
                return true;
            }
            
            
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                List<String> mensagem = messages.getStringList("sistema.jogador_nao_encontrado_comando");
                for (String linha : mensagem) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', linha));
                }
                return true;
            }
            
            List<PlayerTag> tags = plugin.getTagManager().getPlayerTags(target.getUniqueId());
            
            if (tags.isEmpty()) {
                List<String> mensagem = messages.getStringList("sistema.sem_tags");
                for (String linha : mensagem) {
                    String linhaFormatada = linha.replace("{jogador}", target.getName());
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', linhaFormatada));
                }
                return true;
            }
            
            
            StringBuilder listaTags = new StringBuilder();
            for (PlayerTag tag : tags) {
                String tagDisplay = ChatColor.translateAlternateColorCodes('&', tag.getFormattedTag());
                String type = tag.getTagType() == TagType.RANKING ? "Ranking" : "Temporada";
                listaTags.append("&7• ").append(tagDisplay).append(" &7(").append(type).append(")\n");
            }
            
            
            List<String> mensagemTags = messages.getStringList("sistema.tags_de_jogador");
            for (String linha : mensagemTags) {
                String linhaFormatada = linha
                        .replace("{jogador}", target.getName())
                        .replace("{lista_tags}", listaTags.toString().trim());
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', linhaFormatada));
            }
            
            return true;
        }
        
        
        sender.sendMessage(ChatColor.GOLD + "=== Comandos de Tags ===");
        sender.sendMessage(ChatColor.YELLOW + "/liga tags" + ChatColor.GRAY + " - Ver suas tags");
        sender.sendMessage(ChatColor.YELLOW + "/liga tags <jogador>" + ChatColor.GRAY + " - Ver tags de outro jogador");
        
        if (sender.hasPermission("hliga.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/liga tags reload" + ChatColor.GRAY + " - Recarregar configuração");
            sender.sendMessage(ChatColor.YELLOW + "/liga tags update" + ChatColor.GRAY + " - Atualizar tags de ranking");
        }
        
        return true;
    }
    
    private boolean handleTag(CommandSender sender, String[] args) {
        if (!plugin.getTagManager().isSystemEnabled()) {
            sender.sendMessage(ChatColor.RED + "Sistema de tags está desabilitado.");
            return true;
        }
        
        if (args.length == 1) {
            
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Use /liga tag <subcomando> para comandos administrativos.");
                showTagHelp(sender);
                return true;
            }
            
            Player player = (Player) sender;
            showPlayerTags(sender, player);
            return true;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleTagReload(sender);
            case "update":
                return handleTagUpdate(sender);
            case "clear":
                return handleTagClear(sender);
            case "ativar":
                return handleTagEnable(sender);
            case "desativar":
                return handleTagDisable(sender);
            case "player":
                return handleTagPlayer(sender, args);
            case "clan":
                return handleTagClan(sender, args);
            default:
                
                Player target = Bukkit.getPlayer(subCommand);
                if (target != null) {
                    showPlayerTags(sender, target);
                    return true;
                }
                
                showTagHelp(sender);
                return true;
        }
    }
    
    private boolean handleTagReload(CommandSender sender) {
        if (!sender.hasPermission("hliga.admin")) {
            sender.sendMessage(messages.getMessage("geral.sem_permissao"));
            return true;
        }
        
        plugin.getTagManager().loadConfig();
        sender.sendMessage(ChatColor.GREEN + "Configuração de tags recarregada com sucesso!");
        return true;
    }
    
    private boolean handleTagUpdate(CommandSender sender) {
        if (!sender.hasPermission("hliga.admin")) {
            sender.sendMessage(messages.getMessage("geral.sem_permissao"));
            return true;
        }
        
        plugin.getTagManager().updateRankingTags();
        sender.sendMessage(ChatColor.GREEN + "Tags de ranking atualizadas com sucesso!");
        return true;
    }
    
    private boolean handleTagClear(CommandSender sender) {
        if (!sender.hasPermission("hliga.admin")) {
            sender.sendMessage(messages.getMessage("geral.sem_permissao"));
            return true;
        }
        
        plugin.getTagManager().clearRankingTags();
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updateAllNametags();
        }
        
        
        List<String> mensagem = messages.getStringList("tags.sistema.limpo");
        for (String linha : mensagem) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', linha));
        }
        return true;
    }
    
    private boolean handleTagEnable(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getMessage("geral.somente_jogador"));
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerUuid = player.getUniqueId();
        
        if (plugin.getTagManager().isTagsEnabledForPlayer(playerUuid)) {
            sender.sendMessage(messages.getMessage("tags.sistema.ja_ativado"));
            return true;
        }
        
        plugin.getTagManager().enableTagsForPlayer(playerUuid);
        
        
        List<String> mensagem = messages.getStringList("tags.sistema.ativado");
        for (String linha : mensagem) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', linha));
        }
        
        return true;
    }
    
    private boolean handleTagDisable(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getMessage("geral.somente_jogador"));
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerUuid = player.getUniqueId();
        
        if (!plugin.getTagManager().isTagsEnabledForPlayer(playerUuid)) {
            sender.sendMessage(messages.getMessage("tags.sistema.ja_desativado"));
            return true;
        }
        
        plugin.getTagManager().disableTagsForPlayer(playerUuid);
        
        
        List<String> mensagem = messages.getStringList("tags.sistema.desativado");
        for (String linha : mensagem) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', linha));
        }
        
        return true;
    }
    
    private boolean handleTagPlayer(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Use: /liga tag player <jogador>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado ou offline.");
            return true;
        }
        
        showPlayerTags(sender, target);
        return true;
    }
    
    private boolean handleTagClan(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Use: /liga tag clan <tag_do_clan>");
            return true;
        }
        
        String clanTag = args[2];
        List<ClanPoints> topClans = plugin.getPointsManager().getTopClans(10);
        
        
        int position = -1;
        for (int i = 0; i < topClans.size(); i++) {
            if (topClans.get(i).getClanTag().equalsIgnoreCase(clanTag)) {
                position = i + 1;
                break;
            }
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== Informações do Clã " + clanTag + " ===");
        
        if (position != -1 && position <= 5) {
            String tagFormat = plugin.getTagManager().getTagsConfig().getString("tags_ranking." + position);
            if (tagFormat != null) {
                String displayTag = ChatColor.translateAlternateColorCodes('&', tagFormat);
                sender.sendMessage(ChatColor.GREEN + "Posição no ranking: " + ChatColor.YELLOW + position + "º");
                sender.sendMessage(ChatColor.GREEN + "Tag atual: " + displayTag);
            }
        } else {
            sender.sendMessage(ChatColor.GRAY + "Clã não está no top 5 do ranking.");
        }
        
        
        int points = plugin.getPointsManager().getClanPoints(clanTag);
        sender.sendMessage(ChatColor.GREEN + "Pontos: " + ChatColor.YELLOW + plugin.getPointsManager().formatPoints(points));
        
        return true;
    }
    
    private void showPlayerTags(CommandSender sender, Player target) {
        List<PlayerTag> tags = plugin.getTagManager().getPlayerTags(target.getUniqueId());
        
        sender.sendMessage(ChatColor.GOLD + "=== Tags de " + target.getName() + " ===");
        
        if (tags.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Nenhuma tag encontrada.");
            return;
        }
        
        
        List<PlayerTag> seasonTags = new ArrayList<>();
        List<PlayerTag> rankingTags = new ArrayList<>();
        
        for (PlayerTag tag : tags) {
            if (tag.getTagType() == TagType.SEASON) {
                seasonTags.add(tag);
            } else {
                rankingTags.add(tag);
            }
        }
        
        
        if (!seasonTags.isEmpty()) {
            sender.sendMessage(ChatColor.AQUA + "Tags Permanentes:");
            for (PlayerTag tag : seasonTags) {
                String displayTag = ChatColor.translateAlternateColorCodes('&', tag.getFormattedTag());
                sender.sendMessage(ChatColor.GRAY + "  • " + displayTag);
            }
        }
        
        
        if (!rankingTags.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Tags Temporárias:");
            for (PlayerTag tag : rankingTags) {
                String displayTag = ChatColor.translateAlternateColorCodes('&', tag.getFormattedTag());
                sender.sendMessage(ChatColor.GRAY + "  • " + displayTag);
            }
        }
        
        
        String activeTag = plugin.getTagManager().getPlayerActiveTag(target.getUniqueId());
        if (activeTag != null && !activeTag.isEmpty()) {
            String displayActiveTag = ChatColor.translateAlternateColorCodes('&', activeTag);
            sender.sendMessage(ChatColor.GREEN + "Tag Ativa: " + displayActiveTag);
        }
    }
    
    private void showTagHelp(CommandSender sender) {
        List<String> ajuda = messages.getStringList("tags.ajuda");
        for (String linha : ajuda) {
            
            if (!sender.hasPermission("hliga.admin") && linha.contains("COMANDOS ADMINISTRATIVOS")) {
                break;
            }
            if (!sender.hasPermission("hliga.admin") && linha.startsWith("&f• &c/liga tag")) {
                continue;
            }
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', linha));
        }
    }
    
    private void showHelp(CommandSender sender) {
        showHelp(sender, 1);
    }
    
    private void showHelp(CommandSender sender, int page) {
        
        List<String> ajudaMensagens = messages.getStringList("ajuda.principal");
        List<String> todasMensagens = new ArrayList<>();
        
        
        for (String linha : ajudaMensagens) {
            if (!sender.hasPermission("hliga.admin") && linha.contains("COMANDOS ADMINISTRATIVOS")) {
                break; 
            }
            if (!sender.hasPermission("hliga.admin") && linha.contains("/liga") && linha.contains("&c")) {
                continue; 
            }
            todasMensagens.add(linha);
        }
        
        
        int linhasPorPagina = messages.getConfig().getInt("ajuda_paginacao.linhas_por_pagina", 10);
        int totalPaginas = (int) Math.ceil((double) todasMensagens.size() / linhasPorPagina);
        
        if (page < 1 || page > totalPaginas) {
            page = 1;
        }
        
        int inicioIndex = (page - 1) * linhasPorPagina;
        int fimIndex = Math.min(inicioIndex + linhasPorPagina, todasMensagens.size());
        
        
        
        
        if (totalPaginas > 1) {
            String infoPagina = messages.getMessage("ajuda_paginacao.info")
                    .replace("{pagina_atual}", String.valueOf(page))
                    .replace("{total_paginas}", String.valueOf(totalPaginas));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', infoPagina));
        }
        
        
        for (int i = inicioIndex; i < fimIndex; i++) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', todasMensagens.get(i)));
        }
        
        
        if (totalPaginas > 1) {
            String anteriorText = messages.getMessage("ajuda_paginacao.anterior");
            String proximaText = messages.getMessage("ajuda_paginacao.proxima");
            String infoText = messages.getMessage("ajuda_paginacao.info");
            
            ClickableTextUtils.sendPaginationLine(sender, page, totalPaginas, "/liga help", 
                                                anteriorText, proximaText, infoText);
        }
    }
    
    private boolean handleHelp(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                page = 1;
            }
        }
        showHelp(sender, page);
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("info");
            completions.add("top");
            completions.add("help");
            
            if (sender instanceof Player) {
                completions.add("menu");
            }
            
            if (sender.hasPermission("hliga.addpoints")) {
                completions.add("addpoints");
            }
            
            if (sender.hasPermission("hliga.removepoints")) {
                completions.add("removepoints");
            }
            
            if (sender.hasPermission("hliga.admin")) {
                completions.add("reload");
                completions.add("sync");
            }
            
            
            if (sender.hasPermission("hliga.topnpc.create") || 
                sender.hasPermission("hliga.topnpc.remove") || 
                sender.hasPermission("hliga.topnpc.list") || 
                sender.hasPermission("hliga.topnpc.update")) {
                completions.add("topnpc");
            }
            
            
            completions.add("tags");
            completions.add("tag");
            
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("addpoints") || args[0].equalsIgnoreCase("removepoints")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("info")) {
                return plugin.getClansManager().getAllClanTags().stream()
                        .filter(tag -> tag.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("sync")) {
                List<String> dbTypes = Arrays.asList("sqlite", "mysql", "redis");
                return dbTypes.stream()
                        .filter(type -> type.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("tag")) {
                List<String> tagCommands = new ArrayList<>();
                
                
                tagCommands.add("ativar");
                tagCommands.add("desativar");
                
                
                if (sender.hasPermission("hliga.admin")) {
                    tagCommands.add("reload");
                    tagCommands.add("update");
                    tagCommands.add("clear");
                    tagCommands.add("player");
                    tagCommands.add("clan");
                }
                
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    tagCommands.add(p.getName());
                }
                
                return tagCommands.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("sync")) {
                List<String> dbTypes = Arrays.asList("sqlite", "mysql", "redis");
                return dbTypes.stream()
                        .filter(type -> !type.equals(args[1].toLowerCase())) 
                        .filter(type -> type.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        
        if (args[0].equalsIgnoreCase("topnpc")) {
            List<String> subCommands = new ArrayList<>();
            
            if (sender.hasPermission("hliga.topnpc.create")) {
                subCommands.add("create");
            }
            
            if (sender.hasPermission("hliga.topnpc.remove")) {
                subCommands.add("remove");
            }
            
            if (sender.hasPermission("hliga.topnpc.list")) {
                subCommands.add("list");
            }
            
            if (sender.hasPermission("hliga.topnpc.update")) {
                subCommands.add("update");
            }
            
            return subCommands.stream()
                    .filter(s -> args.length < 2 || s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return Arrays.asList();
    }
    

}
