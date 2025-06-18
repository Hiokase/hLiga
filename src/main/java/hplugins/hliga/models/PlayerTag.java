package hplugins.hliga.models;

import lombok.*;

import java.util.UUID;

/**
 * Modelo de dados para tags de jogadores
 */
@Setter
@Getter
public class PlayerTag {
    
    /**
     * Construtor vazio para uso geral
     */
    public PlayerTag() {
        this.obtainedDate = System.currentTimeMillis();
        this.active = true;
    }
    
    /**
     * UUID do jogador
     */
    private UUID playerUuid;
    
    /**
     * Tipo da tag (RANKING ou SEASON)
     */
    private TagType tagType;
    
    /**
     * Posição da tag (1, 2, 3, etc.)
     */
    private int position;
    
    /**
     * Número da temporada (para tags SEASON)
     */
    private int seasonNumber;
    
    /**
     * Timestamp quando a tag foi obtida
     */
    private long obtainedDate;
    
    /**
     * Se a tag está ativa
     */
    private boolean active;
    
    /**
     * Texto formatado da tag
     */
    private String formattedTag;
    
    /**
     * Nome da tag para identificação
     */
    private String tagName;
    
    /**
     * Construtor para tags de ranking
     */
    public PlayerTag(UUID playerUuid, int position, String formattedTag, String tagName) {
        this.playerUuid = playerUuid;
        this.tagType = TagType.RANKING;
        this.position = position;
        this.seasonNumber = 0;
        this.obtainedDate = System.currentTimeMillis();
        this.active = true;
        this.formattedTag = formattedTag;
        this.tagName = tagName;
    }
    
    /**
     * Construtor para tags de temporada
     */
    public PlayerTag(UUID playerUuid, int position, int seasonNumber, String formattedTag, String tagName) {
        this.playerUuid = playerUuid;
        this.tagType = TagType.SEASON;
        this.position = position;
        this.seasonNumber = seasonNumber;
        this.obtainedDate = System.currentTimeMillis();
        this.active = true;
        this.formattedTag = formattedTag;
        this.tagName = tagName;
    }
    
    /**
     * Construtor completo para recuperação do banco de dados
     */
    public PlayerTag(UUID playerUuid, TagType tagType, int position, int seasonNumber, String formattedTag, String tagName) {
        this.playerUuid = playerUuid;
        this.tagType = tagType;
        this.position = position;
        this.seasonNumber = seasonNumber;
        this.obtainedDate = System.currentTimeMillis();
        this.active = true;
        this.formattedTag = formattedTag;
        this.tagName = tagName;
    }
    
    /**
     * Verifica se é uma tag de ranking
     */
    public boolean isRankingTag() {
        return tagType == TagType.RANKING;
    }
    
    /**
     * Verifica se é uma tag de temporada
     */
    public boolean isSeasonTag() {
        return tagType == TagType.SEASON;
    }
    
    /**
     * Obtém a chave única da tag para comparação
     */
    public String getUniqueKey() {
        if (isRankingTag()) {
            return "RANKING_" + position;
        } else {
            return "SEASON_" + seasonNumber + "_" + position;
        }
    }
    
    /**
     * Verifica se a tag é válida
     */
    public boolean isValid() {
        return playerUuid != null && 
               tagType != null && 
               position > 0 && 
               formattedTag != null && 
               !formattedTag.isEmpty();
    }


}