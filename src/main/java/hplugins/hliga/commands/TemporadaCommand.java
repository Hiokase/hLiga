package hplugins.hliga.commands;

import hplugins.hliga.Main;
import hplugins.hliga.config.Messages;
import hplugins.hliga.models.Season;
import hplugins.hliga.utils.LogUtils;
import hplugins.hliga.utils.NumberFormatter;
import hplugins.hliga.utils.TimeUtils;
import hplugins.hliga.utils.NotificationUtils;
import hplugins.hliga.utils.ClickableTextUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class TemporadaCommand implements CommandExecutor, TabCompleter {
    
    private final Main plugin;
    private final Messages messages;
    
    public TemporadaCommand(Main plugin) {
        this.plugin = plugin;
        this.messages = plugin.getConfigManager().getMessages();
    }
    
    /**
     * Resultado do parsing flexível de data
     */
    private static class DateParseResult {
        boolean isSpecificDate;
        int days;
        LocalDate targetDate;
        
        DateParseResult(int days) {
            this.isSpecificDate = false;
            this.days = days;
        }
        
        DateParseResult(LocalDate targetDate) {
            this.isSpecificDate = true;
            this.targetDate = targetDate;
            this.days = 0; 
        }
    }
    
    /**
     * Faz o parsing flexível de data/duração que aceita tanto dias (números) quanto datas específicas
     * 
     * @param input String de entrada que pode ser um número (dias) ou uma data (DD/MM/AAAA)
     * @return Resultado do parsing com informação sobre tipo
     * @throws IllegalArgumentException se o formato for inválido
     */
    private DateParseResult parseFlexibleDate(String input) {
        
        try {
            int days = Integer.parseInt(input);
            if (days <= 0) {
                throw new IllegalArgumentException("Número de dias deve ser positivo");
            }
            return new DateParseResult(days);
        } catch (NumberFormatException e) {
            
        }
        
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        try {
            LocalDate targetDate = LocalDate.parse(input, dateFormatter);
            LocalDate today = LocalDate.now();
            
            
            if (targetDate.isBefore(today)) {
                throw new IllegalArgumentException("A data não pode ser no passado. Use " + today.format(dateFormatter) + " ou uma data futura");
            }
            
            LogUtils.debug("Data parseada: " + input + " -> " + targetDate.format(dateFormatter));
            return new DateParseResult(targetDate);
            
        } catch (DateTimeParseException e) {
            LogUtils.debug("Erro ao parsear data: " + input + " - " + e.getMessage());
            throw new IllegalArgumentException("Formato inválido. Use um número de dias ou uma data no formato DD/MM/AAAA (exemplo: 04/06/2025)");
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("hliga.temporada")) {
            sender.sendMessage(messages.getMessage("geral.sem_permissao"));
            return true;
        }
        
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "iniciar":
                return handleIniciar(sender, args);
            case "fechar":
                return handleFechar(sender);
            case "stop":
            case "parar":
                return handleStop(sender);
            case "info":
                return handleInfo(sender);
            case "historico":
                return handleHistorico(sender);
            case "help":
                return handleHelp(sender, args);
            default:
                showHelp(sender);
                return true;
        }
    }
    
    private boolean handleIniciar(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hliga.temporada.iniciar")) {
            sender.sendMessage(messages.getMessage("geral.sem_permissao"));
            return true;
        }
        
        if (plugin.getSeasonManager().isSeasonActive()) {
            sender.sendMessage(messages.getMessage("temporada.ja_ativa"));
            return true;
        }
        
        String name = "Temporada " + (plugin.getSeasonManager().getSeasonHistory().size() + 1);
        int days = plugin.getConfigManager().getConfig().getInt("temporada.duracao_padrao", 30);
        DateParseResult dateResult = null;
        int hour = -1;
        int minute = -1;
        
        
        
        int daysParamIndex = -1;
        int timeParamIndex = -1;
        
        
        for (int i = 1; i < args.length; i++) {
            if ((args[i].matches("\\d+") || args[i].matches("\\d{1,2}/\\d{1,2}/\\d{4}")) && daysParamIndex == -1) {
                
                daysParamIndex = i;
            } else if (args[i].matches("\\d{1,2}:\\d{2}") && timeParamIndex == -1) {
                
                timeParamIndex = i;
            }
        }
        
        
        if (daysParamIndex > 1 || timeParamIndex > 1) {
            
            int nameEndIndex = (daysParamIndex != -1 && timeParamIndex != -1) 
                ? Math.min(daysParamIndex, timeParamIndex) 
                : (daysParamIndex != -1 ? daysParamIndex : timeParamIndex);
            
            
            StringBuilder nameBuilder = new StringBuilder();
            for (int i = 1; i < nameEndIndex; i++) {
                nameBuilder.append(args[i]);
                if (i < nameEndIndex - 1) {
                    nameBuilder.append(" ");
                }
            }
            name = nameBuilder.toString();
            
            
            if (daysParamIndex != -1) {
                try {
                    dateResult = parseFlexibleDate(args[daysParamIndex]);
                    if (!dateResult.isSpecificDate) {
                        days = dateResult.days;
                    }
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            "&c[X] &f" + e.getMessage()));
                    return true;
                }
            }
        } else if (args.length > 1) {
            
            StringBuilder nameBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                nameBuilder.append(args[i]);
                if (i < args.length - 1) {
                    nameBuilder.append(" ");
                }
            }
            name = nameBuilder.toString();
        } else if (args.length > 2) {

            try {
                dateResult = parseFlexibleDate(args[2]);
                if (!dateResult.isSpecificDate) {
                    days = dateResult.days;
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&c[X] &f" + e.getMessage()));
                return true;
            }
        }


        if (timeParamIndex != -1) {
            String timeFormat = args[timeParamIndex];
            if (timeFormat.matches("\\d{1,2}:\\d{2}")) {
                try {
                    String[] timeParts = timeFormat.split(":");
                    hour = Integer.parseInt(timeParts[0]);
                    minute = Integer.parseInt(timeParts[1]);
                    
                    
                    if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                        sender.sendMessage(messages.getMessage("geral.hora_invalida"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(messages.getMessage("geral.hora_invalida"));
                    return true;
                }
            } else {
                sender.sendMessage(messages.getMessage("geral.formato_hora_invalido"));
                return true;
            }
        } 
        
        else if (args.length > 3) {
            String timeFormat = args[3];
            if (timeFormat.matches("\\d{1,2}:\\d{2}")) {
                try {
                    String[] timeParts = timeFormat.split(":");
                    hour = Integer.parseInt(timeParts[0]);
                    minute = Integer.parseInt(timeParts[1]);
                    
                    
                    if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                        sender.sendMessage(messages.getMessage("geral.hora_invalida"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(messages.getMessage("geral.hora_invalida"));
                    return true;
                }
            } else {
                sender.sendMessage(messages.getMessage("geral.formato_hora_invalido"));
                return true;
            }
        }
        
        boolean success;
        if (dateResult != null && dateResult.isSpecificDate) {
            
            success = plugin.getSeasonManager().startSeasonWithSpecificDate(name, dateResult.targetDate, hour, minute);
        } else if (hour >= 0 && minute >= 0) {
            
            success = plugin.getSeasonManager().startSeason(name, days, hour, minute);
        } else {
            
            success = plugin.getSeasonManager().startSeason(name, days);
        }
        
        if (success) {
            Season season = plugin.getSeasonManager().getActiveSeason().orElse(null);
            if (season != null) {
                
                
                
                if (sender instanceof Player) {
                    sender.sendMessage(messages.getMessage("temporada.iniciada", 
                            "{nome}", season.name,
                            "{termino}", TimeUtils.formatDate(season.endDate)));
                } else {
                    
                    LogUtils.debugHigh("Temporada " + season.name + " iniciada com sucesso via comando de console");
                }
                
                
                if (plugin.getConfigManager().getConfig().getBoolean("discord.ativado", true) && 
                        plugin.getConfigManager().getConfig().getBoolean("discord.anunciar_temporadas", true)) {
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                        plugin.getLigaManager().sendDiscordSeasonStart(season);
                    });
                }
            }
        } else {
            sender.sendMessage(messages.getMessage("geral.erro_interno"));
        }
        
        return true;
    }
    
    private boolean handleFechar(CommandSender sender) {
        if (!sender.hasPermission("hliga.temporada.fechar")) {
            sender.sendMessage(messages.getMessage("geral.sem_permissao"));
            return true;
        }
        
        
        if (!plugin.getSeasonManager().isSeasonActive()) {
            sender.sendMessage(messages.getMessage("temporada.nao_ativa"));
            return true;
        }
        
        
        Optional<Season> optionalSeason = plugin.getSeasonManager().getActiveSeason();
        if (!optionalSeason.isPresent()) {
            sender.sendMessage(messages.getMessage("temporada.nao_ativa"));
            return true;
        }
        
        Season season = optionalSeason.get();
        
        
        if (!season.active) {
            sender.sendMessage(messages.getMessage("temporada.nao_ativa"));
            return true;
        }
        
        
        
        Season freshSeason = plugin.getDatabaseManager().getAdapter().getActiveSeason().orElse(null);
        if (freshSeason == null || !freshSeason.active) {
            sender.sendMessage(messages.getMessage("temporada.nao_ativa"));
            return true;
        }
        
        boolean success = plugin.getSeasonManager().endSeason();
        
        if (success) {
            
            sender.sendMessage(messages.getMessage("temporada.fechada", 
                    "{nome}", season.name));
            
            
            sender.sendMessage(messages.getMessage("temporada.recompensas_distribuidas"));
        } else {
            
            sender.sendMessage(messages.getMessage("temporada.nao_ativa"));
        }
        
        return true;
    }
    
    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("hliga.temporada.stop")) {
            sender.sendMessage(messages.getMessage("geral.sem_permissao"));
            return true;
        }
        
        
        if (!plugin.getSeasonManager().isSeasonActive()) {
            sender.sendMessage(messages.getMessage("temporada.nao_ativa"));
            return true;
        }
        
        
        Optional<Season> optionalSeason = plugin.getSeasonManager().getActiveSeason();
        if (!optionalSeason.isPresent()) {
            sender.sendMessage(messages.getMessage("temporada.nao_ativa"));
            return true;
        }
        
        Season season = optionalSeason.get();
        
        
        if (!season.active) {
            sender.sendMessage(messages.getMessage("temporada.nao_ativa"));
            return true;
        }
        
        
        
        Season freshSeason = plugin.getDatabaseManager().getAdapter().getActiveSeason().orElse(null);
        if (freshSeason == null || !freshSeason.active) {
            sender.sendMessage(messages.getMessage("temporada.nao_ativa"));
            return true;
        }
        
        String seasonName = season.name;
        
        boolean success = plugin.getSeasonManager().endSeason();
        
        if (success) {
            
            
            
            List<hplugins.hliga.models.ClanPoints> topClans = plugin.getDatabaseManager().getAdapter()
                    .getTopClans(plugin.getConfigManager().getTopsConfig().getInt("top.quantidade", 5));
            
            
            String clanVencedor = "Nenhum";
            int pontosVencedor = 0;
            String posicaoVencedor = "1";
            
            if (!topClans.isEmpty() && topClans.get(0).getPoints() > 0) {
                clanVencedor = plugin.getClansManager().getClanName(topClans.get(0).getClanTag());
                pontosVencedor = topClans.get(0).getPoints();
            }
            
            
            List<String> stopMessages = messages.getStringList("temporada.stop");
            for (String linha : stopMessages) {
                String processedLine = linha
                        .replace("{nome}", seasonName)
                        .replace("{duracao}", String.valueOf(season.getDurationDays()))
                        .replace("{posicao}", posicaoVencedor)
                        .replace("{tag}", clanVencedor)
                        .replace("{pontos}", String.valueOf(pontosVencedor));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', processedLine));
            }
            
            
            NotificationUtils.announceSeasonEnd(plugin, season, plugin.getConfigManager().getTopsConfig().getInt("top.quantidade", 5));
            
            
            if (plugin.getConfigManager().getConfig().getBoolean("temporada.resetar_pontos", true)) {
                plugin.getPointsManager().resetAllPoints();
            }
        } else {
            sender.sendMessage(messages.getMessage("geral.erro_interno"));
        }
        
        return true;
    }
    
    private boolean handleInfo(CommandSender sender) {
        if (!plugin.getSeasonManager().isSeasonActive()) {
            sender.sendMessage(messages.getMessage("temporada.nao_ativa"));
            return true;
        }
        
        Optional<Season> optionalSeason = plugin.getSeasonManager().getActiveSeason();
        if (!optionalSeason.isPresent()) {
            sender.sendMessage(messages.getMessage("temporada.nao_ativa"));
            return true;
        }
        
        Season season = optionalSeason.get();
        
        sender.sendMessage(messages.getMessage("temporada.info", 
                "{nome}", season.name,
                "{inicio}", TimeUtils.formatDate(season.startDate),
                "{termino}", TimeUtils.formatDate(season.endDate),
                "{restante}", TimeUtils.formatTimeLeft(season.endDate - System.currentTimeMillis())));
        
        return true;
    }
    
    private boolean handleHistorico(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            new hplugins.hliga.inventory.menus.SeasonHistoryMenu(plugin, player).open(1);
            return true;
        } else {
            List<Season> seasons = plugin.getSeasonManager().getSeasonHistory();
            
            if (seasons.isEmpty()) {
                sender.sendMessage(messages.getMessage("temporada.sem_historico"));
                return true;
            }
            
            
            StringBuilder listaTemporadas = new StringBuilder();
            for (int i = 0; i < seasons.size(); i++) {
                Season season = seasons.get(i);
                String vencedor = season.winnerClan != null ? season.winnerClan : "Sem vencedor";
                String duracao = TimeUtils.formatDateRange(season.startDate, season.endDate);
                listaTemporadas.append("&e").append(i + 1).append(". &f").append(season.name)
                        .append(" &7(").append(vencedor).append(") &8- ").append(duracao).append("\n");
            }
            
            
            List<String> mensagemHistorico = messages.getStringList("historico_temporadas");
            for (String linha : mensagemHistorico) {
                String linhaFormatada = linha.replace("{lista_temporadas}", listaTemporadas.toString().trim());
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', linhaFormatada));
            }
            return true;
        }
    }
    
    private void showHelp(CommandSender sender) {
        showHelp(sender, 1);
    }
    
    private void showHelp(CommandSender sender, int page) {
        
        List<String> allMessages = messages.getStringList("ajuda.temporada");
        
        
        if (allMessages == null || allMessages.isEmpty()) {
            sender.sendMessage("§c✗ §fErro: Mensagens de ajuda não encontradas na configuração.");
            return;
        }
        
        
        List<String> filteredMessages = new ArrayList<>(allMessages);
        
        
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
            
            ClickableTextUtils.sendPaginationLine(sender, page, totalPages, "/temporada help", 
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
            
            if (sender.hasPermission("hliga.temporada.iniciar")) {
                completions.add("iniciar");
            }
            
            if (sender.hasPermission("hliga.temporada.fechar")) {
                completions.add("fechar");
            }
            
            if (sender.hasPermission("hliga.temporada.stop")) {
                completions.add("stop");
                completions.add("parar");
            }
            
            completions.add("info");
            completions.add("historico");
            completions.add("help");
            
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("iniciar")) {
            
            return Arrays.asList("7", "14", "30", "60", "90");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("iniciar")) {
            
            return Arrays.asList("00:00", "08:00", "12:00", "15:00", "18:00", "20:00", "22:00");
        }
        
        return Collections.emptyList();
    }
}
