package hplugins.hliga.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Utilitário para criar textos clicáveis que funcionam em todas as versões do Minecraft
 */
public class ClickableTextUtils {
    
    /**
     * Envia uma mensagem com texto clicável para o jogador
     * 
     * @param sender O destinatário da mensagem
     * @param text O texto a ser exibido
     * @param command O comando a ser executado quando clicado
     * @param hoverText O texto que aparece ao passar o mouse
     */
    public static void sendClickableText(CommandSender sender, String text, String command, String hoverText) {
        if (!(sender instanceof Player)) {
            
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', text));
            return;
        }
        
        Player player = (Player) sender;
        
        try {
            
            TextComponent component = new TextComponent(ChatColor.translateAlternateColorCodes('&', text));
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
            
            if (hoverText != null && !hoverText.isEmpty()) {
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', hoverText)).create()));
            }
            
            player.spigot().sendMessage(component);
        } catch (Exception e) {
            
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', text));
        }
    }
    
    /**
     * Cria uma linha de paginação clicável
     * 
     * @param sender O destinatário da mensagem
     * @param currentPage Página atual
     * @param totalPages Total de páginas
     * @param baseCommand Comando base (ex: "/liga help")
     * @param previousText Texto do botão anterior
     * @param nextText Texto do botão próximo
     * @param infoText Texto de informação da página
     */
    public static void sendPaginationLine(CommandSender sender, int currentPage, int totalPages, 
                                        String baseCommand, String previousText, String nextText, String infoText) {
        if (!(sender instanceof Player)) {
            
            String info = infoText.replace("{pagina_atual}", String.valueOf(currentPage))
                                 .replace("{total_paginas}", String.valueOf(totalPages));
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', info));
            return;
        }
        
        Player player = (Player) sender;
        
        try {
            TextComponent message = new TextComponent("");
            
            
            if (currentPage > 1) {
                TextComponent previous = new TextComponent(ChatColor.translateAlternateColorCodes('&', previousText));
                previous.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                    baseCommand + " " + (currentPage - 1)));
                previous.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder("§7Clique para ir à página anterior").create()));
                message.addExtra(previous);
            }
            
            
            message.addExtra(new TextComponent("  "));
            
            
            String info = infoText.replace("{pagina_atual}", String.valueOf(currentPage))
                                 .replace("{total_paginas}", String.valueOf(totalPages));
            message.addExtra(new TextComponent(ChatColor.translateAlternateColorCodes('&', info)));
            
            
            message.addExtra(new TextComponent("  "));
            
            
            if (currentPage < totalPages) {
                TextComponent next = new TextComponent(ChatColor.translateAlternateColorCodes('&', nextText));
                next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                    baseCommand + " " + (currentPage + 1)));
                next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder("§7Clique para ir à próxima página").create()));
                message.addExtra(next);
            }
            
            player.spigot().sendMessage(message);
        } catch (Exception e) {
            
            String info = infoText.replace("{pagina_atual}", String.valueOf(currentPage))
                                 .replace("{total_paginas}", String.valueOf(totalPages));
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', info));
        }
    }
}