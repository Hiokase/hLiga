package hplugins.hliga.inventory.menus;

import hplugins.hliga.Main;
import hplugins.hliga.inventory.gui.BaseGui;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu de top clãs (pódio + ranking)
 * Compatível com Minecraft 1.8 - 1.21+
 *
 * @author hLiga Plugin Team
 * @version 2.0.0
 */
public class TopClansMenu extends BaseGui {

    /**
     * Construtor do menu de top clãs
     *
     * @param plugin Instância do plugin
     * @param player Jogador
     */
    public TopClansMenu(Main plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void open(int page) {
        buildInventory();
        player.openInventory(inventory);
        plugin.getInventoryManager().registerGui(player, this);
    }

    @Override
    protected void buildInventory() {
        ConfigurationSection config = plugin.getConfigManager().getMenusConfig().getConfigurationSection("menu_top_clans");

        if (config == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfigManager().getMessages().getMessage("menu_top_clans.erro_config")));
            return;
        }

        String title = ensureCompatibleTitle(config.getString("titulo", "&8Top Clãs"));
        int size = config.getInt("tamanho", 45);

        this.inventory = Bukkit.createInventory(null, size, title);
        this.clickActions.clear();

        createBorder(config);

        List<ClanPoints> allTopClans = plugin.getPointsManager().getTopClans(10);
        List<ClanPoints> validTopClans = new ArrayList<>();
        for (ClanPoints clanPoints : allTopClans) {
            if (clanPoints.getPoints() > 0) {
                validTopClans.add(clanPoints);
            }
        }

        addPodiumItems(config, validTopClans);

        addRankingItems(config, validTopClans);

        addNavigationItems(config);

        fillEmptySlots(config);
    }

    /**
     * Adiciona os itens do pódio (1º, 2º e 3º lugares)
     *
     * @param config Configuração do menu
     * @param topClans Lista dos top clãs
     */
    private void addPodiumItems(ConfigurationSection config, List<ClanPoints> topClans) {
        ConfigurationSection podiumConfig = config.getConfigurationSection("formato_podio");
        if (podiumConfig == null) return;

        if (!topClans.isEmpty()) {
            addPodiumItem(podiumConfig.getConfigurationSection("primeiro"), topClans.get(0), 1);
        }

        if (topClans.size() >= 2) {
            addPodiumItem(podiumConfig.getConfigurationSection("segundo"), topClans.get(1), 2);
        }

        if (topClans.size() >= 3) {
            addPodiumItem(podiumConfig.getConfigurationSection("terceiro"), topClans.get(2), 3);
        }
    }

    /**
     * Adiciona um item do pódio
     *
     * @param itemConfig Configuração do item
     * @param clanPoints Dados do clã
     * @param position Posição no ranking
     */
    private void addPodiumItem(ConfigurationSection itemConfig, ClanPoints clanPoints, int position) {
        if (itemConfig == null || clanPoints == null) return;

        int slot = itemConfig.getInt("slot", 13);
        Map<String, String> placeholders = createClanPlaceholders(clanPoints, position);

        ItemStack item = createConfigItem(itemConfig, placeholders);
        if (item != null) {
            inventory.setItem(slot, item);

            addClickAction(slot, event -> {
                String coloredTag = plugin.getClansManager().getColoredClanTag(clanPoints.getClanTag());
                String leaderName = plugin.getClansManager().getClanLeaderName(clanPoints.getClanTag());

                String mensagemEspecial;
                if (position == 1) {
                    mensagemEspecial = "&6&l★ CAMPEÃO DA TEMPORADA! ★";
                } else if (position == 2) {
                    mensagemEspecial = "&f&l◆ VICE-CAMPEÃO! ◆";
                } else if (position == 3) {
                    mensagemEspecial = "&c&l▲ TERCEIRO LUGAR! ▲";
                } else {
                    mensagemEspecial = "&7Excelente posição no ranking!";
                }

                List<String> clanInfo = plugin.getConfigManager().getMessages().getStringList("clan_info");
                for (String linha : clanInfo) {
                    String linhaFormatada = linha
                            .replace("{posicao}", String.valueOf(position))
                            .replace("{nome}", clanPoints.getClanName())
                            .replace("{pontos}", plugin.getPointsManager().formatPoints(clanPoints.getPoints()))
                            .replace("{membros}", String.valueOf(getMemberCount(clanPoints.getClanTag())))
                            .replace("{lider}", leaderName != null ? leaderName : "N/A")
                            .replace("{tag}", clanPoints.getClanTag())
                            .replace("{mensagem_especial}", mensagemEspecial);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', linhaFormatada));
                }
            });
        }
    }

    /**
     * Adiciona os itens do ranking (4º ao 10º lugar)
     *
     * @param config Configuração do menu
     * @param topClans Lista dos top clãs
     */
    private void addRankingItems(ConfigurationSection config, List<ClanPoints> topClans) {
        List<Integer> otherSlots = config.getIntegerList("outros_slots");
        ConfigurationSection formatConfig = config.getConfigurationSection("formato_outros");

        if (otherSlots.isEmpty() || formatConfig == null) return;

        int slotIndex = 0;
        for (int i = 3; i < topClans.size() && slotIndex < otherSlots.size(); i++, slotIndex++) {
            ClanPoints clanPoints = topClans.get(i);
            int position = i + 1;
            int slot = otherSlots.get(slotIndex);

            Map<String, String> placeholders = createClanPlaceholders(clanPoints, position);
            ItemStack item = createConfigItem(formatConfig, placeholders);

            if (item != null) {
                inventory.setItem(slot, item);

                final int finalPosition = position;
                addClickAction(slot, event -> {
                    String coloredTag = plugin.getClansManager().getColoredClanTag(clanPoints.getClanTag());
                    String leaderName = plugin.getClansManager().getClanLeaderName(clanPoints.getClanTag());

                    String mensagemEspecial;
                    if (finalPosition <= 5) {
                        mensagemEspecial = "&a&l✦ TOP 5 DO RANKING! ✦";
                    } else if (finalPosition <= 10) {
                        mensagemEspecial = "&b&l◈ TOP 10 DO RANKING! ◈";
                    } else {
                        mensagemEspecial = "&7Boa posição no ranking!";
                    }

                    List<String> clanInfo = plugin.getConfigManager().getMessages().getStringList("clan_info");
                    for (String linha : clanInfo) {
                        String linhaFormatada = linha
                                .replace("{posicao}", String.valueOf(finalPosition))
                                .replace("{nome}", clanPoints.getClanName())
                                .replace("{pontos}", plugin.getPointsManager().formatPoints(clanPoints.getPoints()))
                                .replace("{membros}", String.valueOf(getMemberCount(clanPoints.getClanTag())))
                                .replace("{lider}", leaderName != null ? leaderName : "N/A")
                                .replace("{tag}", clanPoints.getClanTag())
                                .replace("{mensagem_especial}", mensagemEspecial);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', linhaFormatada));
                    }
                });
            }
        }
    }

    /**
     * Adiciona itens de navegação
     *
     * @param config Configuração do menu
     */
    protected void addNavigationItems(ConfigurationSection config) {
        ConfigurationSection navConfig = config.getConfigurationSection("navegacao");
        if (navConfig == null) return;

        ConfigurationSection backConfig = navConfig.getConfigurationSection("voltar");
        if (backConfig != null) {
            int slot = backConfig.getInt("slot", 40);
            ItemStack backItem = createConfigItem(backConfig, null);

            if (backItem != null) {
                inventory.setItem(slot, backItem);
                addClickAction(slot, event -> {
                    new MainMenu(plugin, player).open(1);
                });
            }
        }
    }

    /**
     * Cria placeholders para um clã
     *
     * @param clanPoints Dados do clã
     * @param position Posição no ranking
     * @return Map com placeholders
     */
    private Map<String, String> createClanPlaceholders(ClanPoints clanPoints, int position) {
        Map<String, String> placeholders = new HashMap<>();
        String clanTag = clanPoints.getClanTag();

        placeholders.put("{tag}", clanTag);
        placeholders.put("{nome}", clanPoints.getClanName());
        placeholders.put("{pontos}", NumberFormatter.format(clanPoints.getPoints()));
        placeholders.put("{posicao}", String.valueOf(position));
        placeholders.put("{membros}", String.valueOf(getMemberCount(clanTag)));

        String coloredTag = plugin.getClansManager().getColoredClanTag(clanTag);
        placeholders.put("{tag_colorida}", coloredTag);

        return placeholders;
    }

    /**
     * Obtém o número de membros de um clã
     *
     * @param clanTag Tag do clã
     * @return Número de membros
     */
    private int getMemberCount(String clanTag) {
        try {
            if (plugin.getClansManager() != null) {
                return plugin.getClansManager().getClanMemberCount(clanTag);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao obter número de membros do clã " + clanTag + ": " + e.getMessage());
        }
        return 0;
    }
}