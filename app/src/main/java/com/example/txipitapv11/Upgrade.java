package com.example.txipitapv11;

/**
 * Clase que representa una mejora en el juego
 *
 * Cada mejora tiene un coste base que aumenta con cada nivel,
 * un tipo que determina su efecto, y una descripción para el usuario.
 * Las mejoras pueden ser de tipo CLICK, AUTO o MULTIPLIER.
 */
public class Upgrade {

    // Atributos de la mejora
    private String name;            // Nombre de la mejora
    private double baseCost;        // Coste base inicial
    private double costMultiplier;  // Multiplicador de coste por nivel
    private Type type;              // Tipo de mejora (CLICK, AUTO, MULTIPLIER)
    private int level;              // Nivel actual de la mejora
    private String description;     // Descripción para mostrar al usuario

    /**
     * Enumeración de los tipos de mejora disponibles
     */
    public enum Type {
        CLICK,
        AUTO,
        MULTIPLIER
    }

    /**
     * Constructor de la mejora
    */
    public Upgrade(String name, double baseCost, double costMultiplier, Type type, String description) {
        this.name = name;
        this.baseCost = baseCost;
        this.costMultiplier = costMultiplier;
        this.type = type;
        this.level = 0;
        this.description = description;
    }

    /**
     * Obtiene el nombre de la mejora
     * @return Nombre de la mejora
     */
    public String getName() {
        return name;
    }

    /**
     * Obtiene el coste base de la mejora
     * @return Coste base inicial
     */
    public double getBaseCost() {
        return baseCost;
    }

    /**
     * Calcula el coste actual de la mejora según su nivel
     * El coste aumenta exponencialmente con cada nivel
     * @return Coste actual para comprar el siguiente nivel
     */
    public double getCurrentCost() {
        return baseCost * Math.pow(costMultiplier, level);
    }

    /**
     * Obtiene el multiplicador de coste por nivel
     * @return Factor de incremento del coste
     */
    public double getCostMultiplier() {
        return costMultiplier;
    }

    /**
     * Obtiene el tipo de mejora
     * @return Tipo de mejora (CLICK, AUTO, MULTIPLIER)
     */
    public Type getType() {
        return type;
    }

    /**
     * Obtiene el nivel actual de la mejora
     * @return Nivel actual
     */
    public int getLevel() {
        return level;
    }

    /**
     * Aumenta el nivel de la mejora en 1
     * Esto incrementará su coste para la próxima compra
     */
    public void levelUp() {
        level++;
    }

    /**
     * Reinicia el nivel de la mejora a 0
     * Útil cuando se realiza un prestigio
     */
    public void resetLevel() {
        level = 0;
    }

    /**
     * Obtiene la descripción de la mejora
     * @return Texto descriptivo de la mejora
     */
    public String getDescription() {
        return description;
    }
}