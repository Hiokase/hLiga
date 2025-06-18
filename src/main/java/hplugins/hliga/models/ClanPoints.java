package hplugins.hliga.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Modelo de pontos de clã
 */
@Setter
@Getter
@NoArgsConstructor
public class ClanPoints {
    
    /**
     * Tag do clã
     * -- SETTER --
     *  Define a tag do clã
     *
     *
     * -- GETTER --
     *  Obtém a tag do clã
     *
     @param clanTag Tag do clã
      * @return Tag do clã

     */
    public String clanTag;
    
    /**
     * Nome do clã
     * -- SETTER --
     *  Define o nome do clã
     *
     * @param clanName Nome do clã

     */
    public String clanName;
    
    /**
     * Pontos do clã
     * -- SETTER --
     *  Define os pontos do clã
     *
     *
     * -- GETTER --
     *  Obtém os pontos do clã
     *
     @param points Pontos do clã
      * @return Pontos do clã

     */
    public int points;
    
    /**
     * Nome do líder do clã
     * -- SETTER --
     *  Define o nome do líder do clã
     *
     * @param leaderName Nome do líder

     */
    public String leaderName;
    
    /**
     * Construtor com tag e pontos
     * 
     * @param clanTag Tag do clã
     * @param points Pontos do clã
     */
    public ClanPoints(String clanTag, int points) {
        this.clanTag = clanTag;
        this.clanName = clanTag; 
        this.points = points;
    }
    
    /**
     * Construtor completo
     * 
     * @param clanTag Tag do clã
     * @param clanName Nome do clã
     * @param points Pontos do clã
     */
    public ClanPoints(String clanTag, String clanName, int points) {
        this.clanTag = clanTag;
        this.clanName = clanName;
        this.points = points;
    }

    /**
     * Obtém o nome do clã
     * 
     * @return Nome do clã
     */
    public String getClanName() {
        return clanName != null ? clanName : clanTag;
    }

    /**
     * Obtém o nome do líder do clã
     * 
     * @return Nome do líder do clã
     */
    public String getLeaderName() {
        return leaderName != null ? leaderName : "";
    }

}