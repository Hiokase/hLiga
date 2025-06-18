package hplugins.hliga.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Modelo de recompensa
 */
@Setter
@Getter
@NoArgsConstructor
public class Reward {
    
    /**
     * Posição no ranking (1 = primeiro lugar, 2 = segundo lugar, etc.)
     */
    public int position;
    
    /**
     * Lista de comandos para executar
     */
    public List<String> commands;
    
    /**
     * Construtor com parâmetros
     */
    public Reward(int position, List<String> commands) {
        this.position = position;
        this.commands = commands;
    }
    
    /**
     * Verifica se a recompensa é válida
     * 
     * @return true se a recompensa for válida, false caso contrário
     */
    public boolean isValid() {
        return position > 0 && commands != null && !commands.isEmpty();
    }
}
