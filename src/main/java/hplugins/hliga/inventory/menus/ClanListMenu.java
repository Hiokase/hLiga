package hplugins.hliga.inventory.menus;

import com.cryptomorin.xseries.XMaterial;
import hplugins.hliga.Main;
import hplugins.hliga.inventory.gui.PaginatedGui;
import hplugins.hliga.inventory.utils.ItemBuilder;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.utils.NumberFormatter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu de lista de clãs com paginação
 * Compatível com Minecraft 1.8 - 1.21+
 *
 * @author hLiga Plugin Team
 * @version 2.0.0
 */
public class ClanListMenu extends PaginatedGui {

    /**
     * Construtor do menu de lista de clãs
     *
     * @param plugin Instância do plugin
     * @param player Jogador
     */
    public ClanListMenu(Main plugin, Player player) {
        super(plugin, player, "menu_clans");
    }

    @Override
    protected List<ItemStack> getContent() {
        List<ItemStack> items = new ArrayList<>();

        try {
            if (plugin.getPointsManager() == null) {
                return items;
            }

            List<ClanPoints> allClanPointsList = plugin.getPointsManager().getTopClans(Integer.MAX_VALUE);

            List<ClanPoints> validClansList = new ArrayList<>();
            for (ClanPoints clanPoints : allClanPointsList) {
                if (clanPoints.getPoints() > 0) {
                    validClansList.add(clanPoints);
                }
            }

            ConfigurationSection formatConfig = menuConfig != null ? menuConfig.getConfigurationSection("formato_clan") : null;

            if (formatConfig == null) {
                return createDefaultClanItems(validClansList);
            }

            int position = 1;
            for (ClanPoints clanPoints : validClansList) {
                ItemStack clanItem = createClanItem(clanPoints, position, formatConfig);
                if (clanItem != null) {
                    items.add(clanItem);
                }
                position++;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao carregar lista de clãs: " + e.getMessage());
        }

        return items;
    }

    /**
     * Cria itens padrão para clãs quando não há configuração
     */
    private List<ItemStack> createDefaultClanItems(List<ClanPoints> clanPointsList) {
        List<ItemStack> items = new ArrayList<>();

        int position = 1;
        for (ClanPoints clanPoints : clanPointsList) {
            String coloredTag = plugin.getClansManager().getColoredClanTag(clanPoints.getClanTag());

            ItemStack clanItem = new ItemBuilder(XMaterial.PLAYER_HEAD)
                    .name("&6Clã: " + coloredTag)
                    .lore(
                            "&7Nome: &f" + clanPoints.getClanName(),
                            "&7Pontos: &f" + plugin.getPointsManager().formatPoints(clanPoints.getPoints()),
                            "&7Posição: &f#" + position,
                            "",
                            "&8➥ &7Clique para mais informações"
                    )
                    .build();
            items.add(clanItem);
            position++;
        }

        return items;
    }

    /**
     * Cria um item representando um clã
     *
     * @param clanPoints Dados do clã
     * @param position Posição no ranking
     * @param formatConfig Configuração de formato
     * @return ItemStack do clã
     */
    private ItemStack createClanItem(ClanPoints clanPoints, int position, ConfigurationSection formatConfig) {
        Map<String, String> placeholders = new HashMap<>();
        String clanTag = clanPoints.getClanTag();

        placeholders.put("{tag}", clanTag);
        placeholders.put("{nome}", clanPoints.getClanName());
        placeholders.put("{pontos}", NumberFormatter.format(clanPoints.getPoints()));
        placeholders.put("{posicao}", String.valueOf(position));

        String coloredTag = plugin.getClansManager().getColoredClanTag(clanTag);
        placeholders.put("{tag_colorida}", coloredTag);

        int memberCount = getMemberCount(clanTag);
        placeholders.put("{membros}", String.valueOf(memberCount));

        return createConfigItem(formatConfig, placeholders);
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

    @Override
    protected void addContentClickAction(int slot, int contentIndex, ItemStack item) {
        addClickAction(slot, event -> {
            List<ClanPoints> allClans = plugin.getPointsManager().getTopClans(Integer.MAX_VALUE);

            List<ClanPoints> validClans = new ArrayList<>();
            for (ClanPoints cp : allClans) {
                if (cp.getPoints() > 0) {
                    validClans.add(cp);
                }
            }

            if (contentIndex < validClans.size()) {
                ClanPoints clanPoints = validClans.get(contentIndex);
                String coloredTag = plugin.getClansManager().getColoredClanTag(clanPoints.getClanTag());

                List<String> menuClans = plugin.getConfigManager().getMessages().getStringList("menu_clans");
                for (String linha : menuClans) {
                    String linhaFormatada = linha
                            .replace("{nome}", clanPoints.getClanName())
                            .replace("{tag}", coloredTag)
                            .replace("{pontos}", plugin.getPointsManager().formatPoints(clanPoints.getPoints()))
                            .replace("{membros}", String.valueOf(getMemberCount(clanPoints.getClanTag())))
                            .replace("{posicao}", String.valueOf(contentIndex + 1))
                            .replace("{tag_clan}", clanPoints.getClanTag());
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', linhaFormatada));
                }
            }
        });
    }
}