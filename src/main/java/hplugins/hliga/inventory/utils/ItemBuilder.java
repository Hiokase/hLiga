package hplugins.hliga.inventory.utils;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Construtor de itens otimizado para compatibilidade
 * Compatível com Minecraft 1.8 - 1.21+
 * 
 * @author hPlugins and Hokase
 * @version 2.0.0
 */
public class ItemBuilder {
    
    private ItemStack itemStack;
    private ItemMeta itemMeta;
    
    /**
     * Construtor com XMaterial (compatível com todas as versões)
     * 
     * @param material XMaterial do item
     */
    public ItemBuilder(XMaterial material) {
        this.itemStack = material.parseItem();
        if (this.itemStack == null) {
            this.itemStack = XMaterial.STONE.parseItem();
        }
        this.itemMeta = itemStack.getItemMeta();
    }
    
    /**
     * Construtor com XMaterial e quantidade
     * 
     * @param material XMaterial do item
     * @param amount Quantidade
     */
    public ItemBuilder(XMaterial material, int amount) {
        this.itemStack = material.parseItem();
        if (this.itemStack == null) {
            this.itemStack = XMaterial.STONE.parseItem();
        }
        this.itemStack.setAmount(amount);
        this.itemMeta = itemStack.getItemMeta();
    }
    
    /**
     * Construtor com ItemStack existente
     * 
     * @param itemStack ItemStack base
     */
    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
    }
    
    /**
     * Define o nome do item
     * 
     * @param name Nome do item (com cores)
     * @return ItemBuilder para chain
     */
    public ItemBuilder name(String name) {
        if (itemMeta != null && name != null) {
            itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }
        return this;
    }
    
    /**
     * Define a lore do item
     * 
     * @param lore Linhas da lore
     * @return ItemBuilder para chain
     */
    public ItemBuilder lore(String... lore) {
        if (itemMeta != null && lore != null) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            itemMeta.setLore(loreList);
        }
        return this;
    }
    
    /**
     * Define a lore do item a partir de uma lista
     * 
     * @param lore Lista com as linhas da lore
     * @return ItemBuilder para chain
     */
    public ItemBuilder lore(List<String> lore) {
        if (itemMeta != null && lore != null) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            itemMeta.setLore(coloredLore);
        }
        return this;
    }
    
    /**
     * Adiciona linhas à lore existente
     * 
     * @param lines Linhas a serem adicionadas
     * @return ItemBuilder para chain
     */
    public ItemBuilder addLore(String... lines) {
        if (itemMeta != null && lines != null) {
            List<String> currentLore = itemMeta.getLore();
            if (currentLore == null) {
                currentLore = new ArrayList<>();
            }
            
            for (String line : lines) {
                currentLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            
            itemMeta.setLore(currentLore);
        }
        return this;
    }
    
    /**
     * Define a quantidade do item
     * 
     * @param amount Quantidade
     * @return ItemBuilder para chain
     */
    public ItemBuilder amount(int amount) {
        itemStack.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }
    
    /**
     * Adiciona um encantamento
     * 
     * @param enchantment Encantamento
     * @param level Nível do encantamento
     * @return ItemBuilder para chain
     */
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        if (itemMeta != null && enchantment != null) {
            itemMeta.addEnchant(enchantment, level, true);
        }
        return this;
    }
    
    /**
     * Adiciona efeito de brilho sem encantamento visível
     * 
     * @return ItemBuilder para chain
     */
    public ItemBuilder glow() {
        if (itemMeta != null) {
            try {
                
                Enchantment glowEnchant = getGlowEnchantment();
                if (glowEnchant != null) {
                    itemMeta.addEnchant(glowEnchant, 1, true);
                    
                    try {
                        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    } catch (Exception ignored) {
                        
                    }
                }
            } catch (Exception e) {
                
            }
        }
        return this;
    }
    
    /**
     * Obtém um encantamento compatível para efeito de brilho
     * 
     * @return Encantamento compatível ou null
     */
    private Enchantment getGlowEnchantment() {
        
        try {
            
            return Enchantment.getByName("UNBREAKING");
        } catch (Exception e1) {
            try {
                
                return Enchantment.getByName("DURABILITY");
            } catch (Exception e2) {
                try {
                    
                    return Enchantment.getByName("ARROW_DAMAGE");
                } catch (Exception e3) {
                    return null;
                }
            }
        }
    }
    
    /**
     * Define o dono da cabeça (para PLAYER_HEAD)
     * 
     * @param owner Nome do jogador
     * @return ItemBuilder para chain
     */
    public ItemBuilder skullOwner(String owner) {
        if (itemMeta instanceof SkullMeta && owner != null && !owner.isEmpty()) {
            SkullMeta skullMeta = (SkullMeta) itemMeta;
            try {
                skullMeta.setOwner(owner);
            } catch (Exception e) {
                
            }
        }
        return this;
    }
    
    /**
     * Remove atributos do item
     * 
     * @return ItemBuilder para chain
     */
    public ItemBuilder hideAttributes() {
        if (itemMeta != null) {
            try {
                itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            } catch (Exception e) {
                
            }
        }
        return this;
    }
    
    /**
     * Esconde encantamentos
     * 
     * @return ItemBuilder para chain
     */
    public ItemBuilder hideEnchants() {
        if (itemMeta != null) {
            try {
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } catch (Exception e) {
                
            }
        }
        return this;
    }
    
    /**
     * Define se o item é inquebrável
     * 
     * @param unbreakable Se deve ser inquebrável
     * @return ItemBuilder para chain
     */
    public ItemBuilder unbreakable(boolean unbreakable) {
        if (itemMeta != null) {
            try {
                itemMeta.setUnbreakable(unbreakable);
                if (unbreakable) {
                    itemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                }
            } catch (Exception e) {
                
            }
        }
        return this;
    }
    
    /**
     * Constrói o ItemStack final
     * 
     * @return ItemStack construído
     */
    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }
    
    /**
     * Cria rapidamente um item de vidro colorido para decoração
     * 
     * @param color Cor do vidro (ex: "RED", "BLUE", "BLACK")
     * @return ItemStack de vidro colorido
     */
    public static ItemStack createGlassPane(String color) {
        String materialName = color.toUpperCase() + "_STAINED_GLASS_PANE";
        XMaterial material = XMaterial.matchXMaterial(materialName).orElse(XMaterial.BLACK_STAINED_GLASS_PANE);
        
        return new ItemBuilder(material)
                .name(" ")
                .hideAttributes()
                .build();
    }
    
    /**
     * Cria um item de cabeça de jogador
     * 
     * @param playerName Nome do jogador
     * @param displayName Nome de exibição
     * @param lore Lore do item
     * @return ItemStack da cabeça
     */
    public static ItemStack createPlayerHead(String playerName, String displayName, String... lore) {
        return new ItemBuilder(XMaterial.PLAYER_HEAD)
                .name(displayName)
                .lore(lore)
                .skullOwner(playerName)
                .build();
    }
}