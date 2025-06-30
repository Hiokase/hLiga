package hplugins.hliga.managers;

import hplugins.hliga.Main;
import hplugins.hliga.models.ClanPoints;
import hplugins.hliga.models.GenericClan;
import hplugins.hliga.utils.LogUtils;
import hplugins.hliga.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PointsManager {

    private final Main plugin;

    public PointsManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Formata um número de pontos para exibição com separadores
     *
     * @param points Número de pontos
     * @return String formatada com separadores de milhar
     */
    public String formatPoints(int points) {
        return NumberFormatter.format(points);
    }

    /**
     * Obtém os pontos de um clã
     *
     * @param clanTag Tag do clã
     * @return Pontuação do clã
     */
    public int getClanPoints(String clanTag) {
        return plugin.getDatabaseManager().getAdapter().getClanPoints(clanTag);
    }

    /**
     * Verifica se um clã tem pontos registrados
     *
     * @param clanTag Tag do clã
     * @return true se o clã tiver pontos, false caso contrário
     */
    public boolean hasClanPoints(String clanTag) {
        return getClanPoints(clanTag) > 0;
    }

    /**
     * Define os pontos de um clã
     *
     * @param clanTag Tag do clã
     * @param points Pontos a serem definidos
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    public boolean setClanPoints(String clanTag, int points) {
        FileConfiguration config = plugin.getConfig();
        int maxPoints = config.getInt("pontos.maximo", 0);

        if (maxPoints > 0 && points > maxPoints) {
            points = maxPoints;
        }

        return plugin.getDatabaseManager().getAdapter().setClanPoints(clanTag, points);
    }

    /**
     * Adiciona pontos a um clã
     *
     * @param clanTag Tag do clã
     * @param points Pontos a serem adicionados
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    public boolean addPoints(String clanTag, int points) {
        return addPoints(clanTag, points, null);
    }

    /**
     * Adiciona pontos a um clã com uma descrição opcional
     *
     * @param clanTag Tag do clã
     * @param points Pontos a serem adicionados
     * @param description Descrição opcional da operação
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    public boolean addPoints(String clanTag, int points, String description) {
        if (points <= 0) {
            return false;
        }

        FileConfiguration config = plugin.getConfig();

        double multiplier = config.getDouble("pontos.multiplicador", 1.0);
        if (multiplier != 1.0) {
            points = (int) Math.round(points * multiplier);
        }

        int maxPoints = config.getInt("pontos.maximo", 0);
        int currentPoints = getClanPoints(clanTag);

        if (maxPoints > 0 && (currentPoints + points) > maxPoints) {
            points = maxPoints - currentPoints;

            if (points <= 0) {
                return false; // Já atingiu o limite máximo
            }
        }

        boolean success = plugin.getDatabaseManager().getAdapter().addClanPoints(clanTag, points);

        if (success) {
            int newTotal = getClanPoints(clanTag);

            if (description != null && !description.isEmpty()) {
                LogUtils.debugHigh("Pontos adicionados ao clã " + clanTag + ": +" + points + " (Total: " + newTotal + "). Motivo: " + description);
            } else {
                LogUtils.debugHigh("Pontos adicionados ao clã " + clanTag + ": +" + points + " (Total: " + newTotal + ")");
            }

            plugin.getLigaManager().sendDiscordPointsNotification(clanTag, points, newTotal, description);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (plugin.getTagManager() != null) {
                    plugin.getTagManager().updateRankingTags();
                }
            });

            int intervaloMinutos = plugin.getConfig().getInt("configuracoes.intervalo_atualizacao", 5);
            long intervalTicks = intervaloMinutos * 60 * 20L; // Converter minutos para ticks (1 minuto = 1200 ticks)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getNpcManager() != null) {
                    plugin.getNpcManager().updateAllNPCs();
                }
            }, intervalTicks);
        }

        return success;
    }

    /**
     * Remove pontos de um clã
     *
     * @param clanTag Tag do clã
     * @param points Pontos a serem removidos
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    public boolean removePoints(String clanTag, int points) {
        return removePoints(clanTag, points, null);
    }

    /**
     * Remove pontos de um clã com uma descrição opcional
     *
     * @param clanTag Tag do clã
     * @param points Pontos a serem removidos
     * @param description Descrição opcional da operação
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    public boolean removePoints(String clanTag, int points, String description) {
        if (points <= 0) {
            LogUtils.debug("Tentativa de remover quantidade inválida de pontos: " + points);
            return false;
        }

        int currentPoints = getClanPoints(clanTag);
        if (currentPoints < points) {
            LogUtils.debug("Tentativa de remover mais pontos (" + points + ") do que o clã possui (" + currentPoints + ") - Clã: " + clanTag);
            return false;
        }

        boolean success = plugin.getDatabaseManager().getAdapter().removeClanPoints(clanTag, points);
        if (success) {
            int newTotal = getClanPoints(clanTag);

            if (description != null && !description.isEmpty()) {
                LogUtils.debugHigh("Pontos removidos do clã " + clanTag + ": -" + points + " (Total: " + newTotal + "). Motivo: " + description);
            } else {
                LogUtils.debugHigh("Pontos removidos do clã " + clanTag + ": -" + points + " (Total: " + newTotal + ")");
            }

            plugin.getLigaManager().sendDiscordPointsNotification(clanTag, -points, newTotal, description);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (plugin.getTagManager() != null) {
                    plugin.getTagManager().updateRankingTags();
                }
            });

            int intervaloMinutos = plugin.getConfig().getInt("configuracoes.intervalo_atualizacao", 5);
            long intervalTicks = intervaloMinutos * 60 * 20L; // Converter minutos para ticks (1 minuto = 1200 ticks)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getNpcManager() != null) {
                    plugin.getNpcManager().updateAllNPCs();
                }
            }, intervalTicks);
        }
        return success;
    }

    /**
     * Remove todos os pontos de um clã
     *
     * @param clanTag Tag do clã
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    public boolean removeClanPoints(String clanTag) {
        int currentPoints = getClanPoints(clanTag);
        if (currentPoints <= 0) {
            return true; // Não há pontos para remover
        }

        return removePoints(clanTag, currentPoints);
    }

    /**
     * Reseta os pontos de todos os clãs
     *
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    public boolean resetAllPoints() {
        LogUtils.debug("Reiniciando pontos de todos os clãs...");
        boolean success = plugin.getDatabaseManager().getAdapter().resetAllPoints();
        if (success) {
            LogUtils.debug("Todos os pontos dos clãs foram zerados com sucesso");
        } else {
            LogUtils.warning("Falha ao zerar pontos de todos os clãs");
        }
        return success;
    }

    /**
     * Obtém os clãs com maior pontuação
     *
     * @param limit Limite de resultados
     * @return Lista de clãs ordenados por pontuação
     */
    public List<ClanPoints> getTopClans(int limit) {
        List<ClanPoints> clans = plugin.getDatabaseManager().getAdapter().getTopClans(limit);

        if (!plugin.getConfig().getBoolean("visual.mostrar_todos_clans", true)) {
            clans.removeIf(clan -> clan.getPoints() <= 0);
        }

        return clans;
    }

    /**
     * Obtém um clã específico do ranking com base na posição
     * Método otimizado para quando apenas um clã específico é necessário
     *
     * @param position Posição no ranking (começando em 1)
     * @param count Quantidade de clãs a retornar a partir da posição
     * @return Lista com o clã na posição especificada ou lista vazia se não existir
     */
    public List<ClanPoints> getTopClans(int position, int count) {
        if (position <= 0) {
            return new ArrayList<>();
        }

        List<ClanPoints> allClans = getTopClans(position + count - 1);

        if (allClans.size() < position) {
            return new ArrayList<>();
        }

        List<ClanPoints> result = new ArrayList<>(count);
        for (int i = position - 1; i < Math.min(position - 1 + count, allClans.size()); i++) {
            result.add(allClans.get(i));
        }

        return result;
    }

    /**
     * Obtém a posição de um clã no ranking
     *
     * @param clanTag Tag do clã
     * @return Posição do clã (começando em 1) ou -1 se não estiver no ranking
     */
    public int getClanPosition(String clanTag) {
        List<ClanPoints> topClans = getTopClans(Integer.MAX_VALUE);

        for (int i = 0; i < topClans.size(); i++) {
            if (topClans.get(i).getClanTag().equals(clanTag)) {
                return i + 1;
            }
        }

        return -1;
    }

    /**
     * Obtém todos os clãs com pontos registrados
     *
     * @return Lista de clãs com seus pontos
     */
    public List<ClanPoints> getAllClanPoints() {
        List<ClanPoints> clans = plugin.getDatabaseManager().getAdapter().getTopClans(Integer.MAX_VALUE);

        if (!plugin.getConfig().getBoolean("visual.mostrar_todos_clans", true)) {
            clans.removeIf(clan -> clan.getPoints() <= 0);
        }

        return clans;
    }

    /**
     * Sincroniza todos os clãs do provedor ativo com o banco de dados do plugin.
     * Isso garante que clãs sem pontos também apareçam no sistema.
     *
     * Note que a exibição de clãs com zero pontos depende da configuração "visual.mostrar_todos_clans".
     * Se essa configuração for falsa, os clãs com zero pontos não serão exibidos nas listas e menus,
     * mesmo que eles estejam sincronizados no banco de dados.
     */
    public void syncClansWithDatabase() {
        if (plugin.getClansManager() != null) {
            plugin.getClansManager().syncClansWithDatabase();
        } else {
            syncClansWithDatabaseLegacy();
        }
    }

    /**
     * Método para sincronizar clãs com a base de dados
     * Atualizado para usar o sistema de provedores de clãs
     */
    private void syncClansWithDatabaseLegacy() {
        if (!plugin.getClansManager().hasAvailableProvider()) {
            LogUtils.warning("Nenhum provedor de clãs está disponível para sincronização.");
            return;
        }

        List<GenericClan> allClans = plugin.getClansManager().getAllClans();
        if (allClans.isEmpty()) {
            LogUtils.debug("Nenhum clã encontrado para sincronizar.");
            return;
        }

        LogUtils.debugHigh("Iniciando sincronização de " + allClans.size() + " clãs...");

        int syncCount = 0;
        for (GenericClan clan : allClans) {
            String clanTag = clan.getTag();

            if (getClanPoints(clanTag) <= 0) {
                setClanPoints(clanTag, 0);
                syncCount++;
                LogUtils.debug("Clã sincronizado: " + clanTag);
            }
        }

        LogUtils.debug("Sincronização de clãs concluída: " + syncCount + " novos clãs adicionados ao sistema.");
        LogUtils.debugMedium("Total de clãs no sistema após sincronização: " + getAllClanPoints().size());
    }

    /**
     * Adiciona pontos a um clã a partir de um jogador
     *
     * @param player Jogador
     * @param points Pontos a serem adicionados
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    public boolean addPointsToPlayerClan(Player player, int points) {
        if (player == null || points <= 0) {
            LogUtils.debug("Tentativa de adicionar pontos inválidos: jogador=" +
                    (player == null ? "null" : player.getName()) + ", pontos=" + points);
            return false;
        }

        String clanTag = null;
        if (plugin.getClansManager() != null) {
            clanTag = plugin.getClansManager().getPlayerClanTag(player);
        } else {
            try {
                clanTag = plugin.getSimpleClansHook().getPlayerClan(player).getTag();
            } catch (Exception e) {
                LogUtils.debug("Erro ao obter clã do jogador " + player.getName() + ": " + e.getMessage());
                return false;
            }
        }

        if (clanTag == null) {
            LogUtils.debugMedium("Jogador " + player.getName() + " não pertence a nenhum clã");
            return false;
        }

        LogUtils.debugMedium("Adicionando " + points + " pontos ao clã " + clanTag + " pelo jogador " + player.getName());
        return addPoints(clanTag, points);
    }

    /**
     * Remove pontos de um clã a partir de um jogador
     *
     * @param player Jogador
     * @param points Pontos a serem removidos
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    public boolean removePointsFromPlayerClan(Player player, int points) {
        if (player == null || points <= 0) {
            LogUtils.debug("Tentativa de remover pontos inválidos: jogador=" +
                    (player == null ? "null" : player.getName()) + ", pontos=" + points);
            return false;
        }

        String clanTag = null;
        if (plugin.getClansManager() != null) {
            clanTag = plugin.getClansManager().getPlayerClanTag(player);
        } else {
            try {
                clanTag = plugin.getSimpleClansHook().getPlayerClan(player).getTag();
            } catch (Exception e) {
                LogUtils.debug("Erro ao obter clã do jogador " + player.getName() + ": " + e.getMessage());
                return false;
            }
        }

        if (clanTag == null) {
            LogUtils.debugMedium("Jogador " + player.getName() + " não pertence a nenhum clã");
            return false;
        }

        LogUtils.debugMedium("Removendo " + points + " pontos do clã " + clanTag + " pelo jogador " + player.getName());
        return removePoints(clanTag, points);
    }
}
