package hplugins.hliga.models;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de temporada
 */
@Getter
public class Season {
    
    /**
     * ID da temporada
     * -- GETTER --
     *  Obtém o ID da temporada
     *
     * @return ID da temporada

     */
    public int id;
    
    /**
     * Nome da temporada
     * -- GETTER --
     *  Obtém o nome da temporada
     *
     * @return Nome da temporada

     */
    public String name;
    
    /**
     * Data de início (timestamp em milissegundos)
     * -- GETTER --
     *  Obtém a data de início da temporada
     *
     * @return Data de início (timestamp em milissegundos)

     */
    public long startDate;
    
    /**
     * Data de término (timestamp em milissegundos)
     * -- GETTER --
     *  Obtém a data de término da temporada
     *
     * @return Data de término (timestamp em milissegundos)

     */
    public long endDate;
    
    /**
     * Indica se a temporada está ativa
     * -- GETTER --
     *  Verifica se a temporada está ativa
     *
     * @return true se a temporada estiver ativa, false caso contrário

     */
    public boolean active;
    
    /**
     * Tag do clã vencedor (se a temporada já terminou)
     * -- GETTER --
     *  Obtém a tag do clã vencedor
     *
     * @return Tag do clã vencedor

     */
    public String winnerClan;
    
    /**
     * Pontos do clã vencedor
     * -- GETTER --
     *  Obtém a pontuação do clã vencedor
     *
     * @return Pontuação do clã vencedor

     */
    public int winnerPoints;
    
    /**
     * Lista dos top clãs da temporada
     * -- GETTER --
     *  Obtém a lista dos top clãs da temporada
     *
     * @return Lista dos top clãs

     */
    public List<ClanPoints> topClans = new ArrayList<>();
    
    /**
     * Construtor padrão
     */
    public Season() {
    }
    
    /**
     * Construtor com parâmetros
     * 
     * @param id ID da temporada
     * @param name Nome da temporada
     * @param startDate Data de início
     * @param endDate Data de término
     * @param active Se está ativa
     */
    public Season(int id, String name, long startDate, long endDate, boolean active) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
    }

    /**
     * Define o ID da temporada
     * 
     * @param id ID da temporada
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Define o nome da temporada
     * 
     * @param name Nome da temporada
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Define a data de início da temporada
     * 
     * @param startDate Data de início (timestamp em milissegundos)
     */
    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    /**
     * Define a data de término da temporada
     * 
     * @param endDate Data de término (timestamp em milissegundos)
     */
    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    /**
     * Define se a temporada está ativa
     * 
     * @param active true se a temporada estiver ativa, false caso contrário
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Define a tag do clã vencedor
     * 
     * @param winnerClan Tag do clã vencedor
     */
    public void setWinnerClan(String winnerClan) {
        this.winnerClan = winnerClan;
    }

    /**
     * Define a pontuação do clã vencedor
     * 
     * @param winnerPoints Pontuação do clã vencedor
     */
    public void setWinnerPoints(int winnerPoints) {
        this.winnerPoints = winnerPoints;
    }

    /**
     * Define a lista dos top clãs da temporada
     * 
     * @param topClans Lista dos top clãs
     */
    public void setTopClans(List<ClanPoints> topClans) {
        this.topClans = topClans;
    }
    
    /**
     * Verifica se a temporada já terminou
     * 
     * @return true se a temporada já terminou, false caso contrário
     */
    public boolean isEnded() {
        return !active && endDate <= System.currentTimeMillis();
    }
    
    /**
     * Verifica se a temporada está em andamento
     * 
     * @return true se a temporada estiver em andamento, false caso contrário
     */
    public boolean isRunning() {
        return active && endDate > System.currentTimeMillis();
    }
    
    /**
     * Obtém a duração da temporada em dias
     * 
     * @return Duração em dias
     */
    public int getDurationDays() {
        long durationMillis = endDate - startDate;
        return (int) (durationMillis / (1000 * 60 * 60 * 24));
    }
    
    /**
     * Obtém o tempo restante da temporada em milissegundos
     * 
     * @return Tempo restante em milissegundos
     */
    public long getRemainingTime() {
        if (!active) {
            return 0;
        }
        
        long now = System.currentTimeMillis();
        if (now > endDate) {
            return 0;
        }
        
        return endDate - now;
    }
    
    /**
     * Alias para getRemainingTime() para compatibilidade
     * 
     * @return Tempo restante em milissegundos
     */
    public long getTimeLeft() {
        return getRemainingTime();
    }
    
    /**
     * Obtém o vencedor da temporada
     * 
     * @return Tag do clã vencedor
     */
    public String getWinner() {
        return getWinnerClan();
    }
    
    /**
     * Verifica se a temporada está finalizada
     * 
     * @return true se finalizada
     */
    public boolean isFinished() {
        return !active;
    }
}
