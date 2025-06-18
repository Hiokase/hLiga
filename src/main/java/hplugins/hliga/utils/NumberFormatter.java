package hplugins.hliga.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utilitário para formatação de números
 */
public class NumberFormatter {

    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("#,###.##");
    private static final NumberFormat NUMBER_FORMATTER = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
    
    /**
     * Formata um número com separadores de milhar
     * Ex: 1000 -> 1.000
     *
     * @param number Número a ser formatado
     * @return String com o número formatado
     */
    public static String format(int number) {
        return NUMBER_FORMATTER.format(number);
    }
    
    /**
     * Formata um número com separadores de milhar
     * Ex: 1000.5 -> 1.000,50
     *
     * @param number Número a ser formatado
     * @return String com o número formatado
     */
    public static String format(double number) {
        return DECIMAL_FORMATTER.format(number);
    }
    
    /**
     * Formata um número com separadores de milhar e sufixos de abreviação
     * Ex: 1000 -> 1k, 1000000 -> 1M
     *
     * @param number Número a ser formatado
     * @return String com o número formatado e abreviado
     */
    public static String formatCompact(int number) {
        if (number < 1000) {
            return String.valueOf(number);
        } else if (number < 1000000) {
            return format(number / 1000) + "k";
        } else {
            return format(number / 1000000) + "M";
        }
    }
    
    /**
     * Formata uma String numérica com separadores de milhar
     *
     * @param numberString String numérica a ser formatada
     * @return String com o número formatado
     */
    public static String format(String numberString) {
        try {
            
            return format(Integer.parseInt(numberString));
        } catch (NumberFormatException e) {
            try {
                
                return format(Double.parseDouble(numberString));
            } catch (NumberFormatException e2) {
                
                return numberString;
            }
        }
    }
}