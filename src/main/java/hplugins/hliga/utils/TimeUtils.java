package hplugins.hliga.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Utilitários para manipulação e formatação de datas e tempo
 */
public class TimeUtils {
    
    /**
     * Formata uma data em milissegundos para uma string legível
     * 
     * @param timestamp Data em milissegundos
     * @return Data formatada (dd/MM/yyyy HH:mm)
     */
    public static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(new Date(timestamp));
    }
    
    /**
     * Formata uma data em milissegundos para uma string legível com formato customizado
     * 
     * @param timestamp Data em milissegundos
     * @param pattern Padrão de formatação
     * @return Data formatada segundo o padrão
     */
    public static String formatDate(long timestamp, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(new Date(timestamp));
    }
    
    /**
     * Formata um intervalo de datas
     * 
     * @param startTimestamp Data de início em milissegundos
     * @param endTimestamp Data de término em milissegundos
     * @return Intervalo formatado (dd/MM/yyyy - dd/MM/yyyy)
     */
    public static String formatDateRange(long startTimestamp, long endTimestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(new Date(startTimestamp)) + " - " + sdf.format(new Date(endTimestamp));
    }
    
    /**
     * Formata um tempo restante em milissegundos para uma string legível
     * 
     * @param millisLeft Tempo restante em milissegundos
     * @return Tempo formatado (Xd Xh Xm Xs)
     */
    public static String formatTimeLeft(long millisLeft) {
        if (millisLeft <= 0) {
            return "0s";
        }
        
        long days = TimeUnit.MILLISECONDS.toDays(millisLeft);
        millisLeft -= TimeUnit.DAYS.toMillis(days);
        
        long hours = TimeUnit.MILLISECONDS.toHours(millisLeft);
        millisLeft -= TimeUnit.HOURS.toMillis(hours);
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millisLeft);
        millisLeft -= TimeUnit.MINUTES.toMillis(minutes);
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millisLeft);
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append("d ");
        }
        
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m ");
        }
        
        sb.append(seconds).append("s");
        
        return sb.toString().trim();
    }
    
    /**
     * Formata uma duração em milissegundos para uma string legível
     * 
     * @param durationMillis Duração em milissegundos
     * @return Duração formatada (X dias, X horas, X minutos)
     */
    public static String formatDuration(long durationMillis) {
        if (durationMillis <= 0) {
            return "0 segundos";
        }
        
        long days = TimeUnit.MILLISECONDS.toDays(durationMillis);
        durationMillis -= TimeUnit.DAYS.toMillis(days);
        
        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
        durationMillis -= TimeUnit.HOURS.toMillis(hours);
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis);
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append(days == 1 ? " dia" : " dias");
            
            if (hours > 0 && minutes > 0) {
                sb.append(", ");
            } else if (hours > 0 || minutes > 0) {
                sb.append(" e ");
            }
        }
        
        if (hours > 0) {
            sb.append(hours).append(hours == 1 ? " hora" : " horas");
            
            if (minutes > 0) {
                sb.append(" e ");
            }
        }
        
        if (minutes > 0 || (days == 0 && hours == 0)) {
            sb.append(minutes).append(minutes == 1 ? " minuto" : " minutos");
        }
        
        return sb.toString();
    }
    
    /**
     * Calcula a data de término com base na duração em dias e fuso horário
     * 
     * @param durationDays Duração em dias
     * @param timezone Fuso horário (formato Java TimeZone ID)
     * @return Data de término em milissegundos
     */
    public static long calculateEndDate(int durationDays, String timezone) {
        Calendar calendar = Calendar.getInstance();
        
        
        if (timezone != null && !timezone.isEmpty()) {
            try {
                TimeZone tz = TimeZone.getTimeZone(timezone);
                calendar.setTimeZone(tz);
            } catch (Exception e) {
                
            }
        }
        
        
        
        
        
        calendar.add(Calendar.DAY_OF_MONTH, durationDays);
        
        
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        
        return calendar.getTimeInMillis();
    }
    
    /**
     * Calcula a data de término com base na duração em dias e hora/minuto específicos
     * 
     * @param durationDays Duração em dias
     * @param hour Hora específica para encerramento (0-23)
     * @param minute Minuto específico para encerramento (0-59)
     * @param timezone Fuso horário (formato Java TimeZone ID)
     * @return Data de término em milissegundos
     */
    public static long calculateEndDateWithTime(int durationDays, int hour, int minute, String timezone) {
        Calendar calendar = Calendar.getInstance();
        
        
        if (timezone != null && !timezone.isEmpty()) {
            try {
                TimeZone tz = TimeZone.getTimeZone(timezone);
                calendar.setTimeZone(tz);
            } catch (Exception e) {
                
            }
        }
        
        
        
        
        
        calendar.add(Calendar.DAY_OF_MONTH, durationDays);
        
        
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        return calendar.getTimeInMillis();
    }
    
    /**
     * Calcula a data de término para uma data específica com hora/minuto
     * 
     * @param targetDate Data específica no formato LocalDate
     * @param hour Hora específica (0-23), -1 para usar 23:59
     * @param minute Minuto específico (0-59), -1 para usar 59
     * @param timezone Fuso horário
     * @return Data de término em milissegundos
     */
    public static long calculateEndDateForSpecificDate(java.time.LocalDate targetDate, int hour, int minute, String timezone) {
        Calendar calendar = Calendar.getInstance();
        
        
        if (timezone != null && !timezone.isEmpty()) {
            try {
                TimeZone tz = TimeZone.getTimeZone(timezone);
                calendar.setTimeZone(tz);
            } catch (Exception e) {
                
            }
        }
        
        
        calendar.set(Calendar.YEAR, targetDate.getYear());
        calendar.set(Calendar.MONTH, targetDate.getMonthValue() - 1); 
        calendar.set(Calendar.DAY_OF_MONTH, targetDate.getDayOfMonth());
        
        
        if (hour >= 0 && minute >= 0) {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        } else {
            
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
        }
        
        return calendar.getTimeInMillis();
    }
    
    /**
     * Verifica se uma data já passou
     * 
     * @param timestamp Data em milissegundos
     * @return true se a data já passou, false caso contrário
     */
    public static boolean hasDatePassed(long timestamp) {
        return System.currentTimeMillis() > timestamp;
    }
    
    /**
     * Calcula a diferença em dias entre duas datas
     * 
     * @param startTimestamp Data de início em milissegundos
     * @param endTimestamp Data de término em milissegundos
     * @return Diferença em dias
     */
    public static int getDaysBetween(long startTimestamp, long endTimestamp) {
        long diffMillis = endTimestamp - startTimestamp;
        return (int) TimeUnit.MILLISECONDS.toDays(diffMillis);
    }
    
    /**
     * Converte uma string de data para timestamp
     * 
     * @param dateStr String de data no formato dd/MM/yyyy HH:mm
     * @return Timestamp em milissegundos ou -1 em caso de erro
     */
    public static long parseDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            return sdf.parse(dateStr).getTime();
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Formata um timestamp para mostrar tempo relativo (ex: "há 5 minutos", "em 3 horas")
     * 
     * @param timestamp Timestamp a ser formatado
     * @return String com o tempo relativo
     */
    public static String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = timestamp - now;
        
        boolean future = diff > 0;
        diff = Math.abs(diff);
        
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
        
        StringBuilder sb = new StringBuilder();
        
        if (future) {
            sb.append("em ");
        } else {
            sb.append("há ");
        }
        
        if (days > 0) {
            sb.append(days).append(days == 1 ? " dia" : " dias");
        } else if (hours > 0) {
            sb.append(hours).append(hours == 1 ? " hora" : " horas");
        } else if (minutes > 0) {
            sb.append(minutes).append(minutes == 1 ? " minuto" : " minutos");
        } else {
            sb.append("poucos segundos");
        }
        
        return sb.toString();
    }
}
