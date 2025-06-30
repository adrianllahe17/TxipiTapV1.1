package com.example.txipitapv11;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Actividad principal del juego clicker "TxipitaPV11"
 *
 * Esta clase gestiona toda la lógica del juego, incluyendo:
 * - Sistema de clics para generar puntos (tinta)
 * - Sistema de mejoras para aumentar la producción
 * - Sistema de prestigio para reiniciar con multiplicadores
 * - Guardado y carga del estado del juego
 * - Interfaz de usuario y animaciones
 */
public class MainActivity extends AppCompatActivity {

    // Elementos de la interfaz de usuario
    private ImageView octopusImage;         // Imagen del pulpo que se puede clicar
    private TextView pointsText;            // Texto que muestra los puntos actuales
    private TextView prestigeText;          // Texto que muestra el nivel de prestigio
    private TextView generationInfoText;    // Texto que muestra información de generación de puntos
    private Button prestigeButton;          // Botón para activar el prestigio
    private ImageView infoButton;           // Botón de información
    private RecyclerView upgradesRecyclerView; // Lista de mejoras disponibles
    private UpgradeAdapter upgradeAdapter;  // Adaptador para la lista de mejoras
    private ConstraintLayout mainLayout;    // Layout principal para animaciones

    // Variables del estado del juego
    private double points = 0;              // Puntos actuales (tinta)
    private double pointsPerClick = 1;      // Puntos ganados por cada clic
    private double pointsPerSecond = 0;     // Puntos ganados automáticamente por segundo
    private int prestigeLevel = 0;          // Nivel de prestigio actual
    private double prestigeMultiplier = 1.0; // Multiplicador de puntos por prestigio

    private double prestigeRequirement = 5000; // Puntos necesarios para prestigiar

    // Sistema de mejoras
    private List<Upgrade> upgrades;         // Lista de mejoras disponibles

    // Sistema de auto-clicker
    private Handler autoClickHandler = new Handler(Looper.getMainLooper());
    private Runnable autoClickRunnable;     // Tarea que se ejecuta periódicamente

    // Constantes para el guardado de datos
    private static final String PREFS_NAME = "ClickerGamePrefs";
    private static final String KEY_POINTS = "points";
    private static final String KEY_POINTS_PER_CLICK = "pointsPerClick";
    private static final String KEY_POINTS_PER_SECOND = "pointsPerSecond";
    private static final String KEY_PRESTIGE_LEVEL = "prestigeLevel";
    private static final String KEY_PRESTIGE_MULTIPLIER = "prestigeMultiplier";
    private static final String KEY_PRESTIGE_REQUIREMENT = "prestigeRequirement";
    private static final String KEY_UPGRADES = "upgrades";

    // Variable estática para acceder a los puntos desde otras clases
    private static double currentPoints = 0;

    /**
     * Método para obtener los puntos actuales desde otras clases
     * @return Cantidad actual de puntos (tinta)
     */
    public static double getCurrentPoints() {
        return currentPoints;
    }

    /**
     * Método que se ejecuta al crear la actividad
     * Inicializa la interfaz y carga el estado del juego
     * @param savedInstanceState Estado guardado de la actividad
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de elementos de la interfaz
        octopusImage = findViewById(R.id.octopusImage);
        pointsText = findViewById(R.id.pointsText);
        prestigeText = findViewById(R.id.prestigeText);
        generationInfoText = findViewById(R.id.generationInfoText);
        prestigeButton = findViewById(R.id.prestigeButton);
        infoButton = findViewById(R.id.infoButton);
        upgradesRecyclerView = findViewById(R.id.upgradesRecyclerView);
        mainLayout = findViewById(R.id.mainLayout);

        // Configuración del RecyclerView con un layout de cuadrícula de 2 columnas
        upgradesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Cargar estado guardado (incluye mejoras)
        loadGameState();

        // Si no hay mejoras guardadas, inicializar por primera vez
        if (upgrades == null || upgrades.isEmpty()) {
            initUpgrades();
        }

        // Configuración del adaptador con listener para comprar mejoras
        upgradeAdapter = new UpgradeAdapter(upgrades, new UpgradeAdapter.OnUpgradeClickListener() {
            @Override
            public void onUpgradeClick(Upgrade upgrade) {
                buyUpgrade(upgrade);
            }
        });
        upgradesRecyclerView.setAdapter(upgradeAdapter);

        // Configuración del listener de clic en el pulpo
        octopusImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Añadir puntos y crear animación de clic
                addPoints(pointsPerClick);
                ClickAnimationHelper.createClickAnimation(MainActivity.this, mainLayout,
                        v.getX() + v.getWidth() / 2,
                        v.getY() + v.getHeight() / 2,
                        formatNumber(pointsPerClick));
            }
        });

        // Configuración del botón de prestigio
        prestigeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prestige();
            }
        });

        // Configuración de mantener pulsado para ver información de prestigio
        prestigeButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Mostrar información del prestigio actual
                Toast.makeText(MainActivity.this,
                        "Multiplicador actual: x" + prestigeMultiplier +
                                "\nRequisito para prestigio: " + formatNumber(prestigeRequirement) + " Tinta",
                        Toast.LENGTH_LONG).show();
                return true;
            }
        });

        // Configuración del botón de información
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInfoDialog();
            }
        });

        // Iniciar el sistema de auto-clicker
        startAutoClicker();

        // Actualizar la interfaz inicial
        updateUI();
    }

    /**
     * Inicializa la lista de mejoras disponibles en el juego
     * Cada mejora tiene un nombre, coste, factor de crecimiento, tipo y descripción
     */
    private void initUpgrades() {
        upgrades = new ArrayList<>();

        // Mejoras de tipo CLICK (aumentan los puntos por clic)
        upgrades.add(new Upgrade("Tinta Mejorada", 10, 1.2, Upgrade.Type.CLICK, "Aumenta el poder de clic en 1"));
        upgrades.add(new Upgrade("Super Tinta Mejorada", 100, 1.5, Upgrade.Type.CLICK, "Aumenta mucho el poder de clic en 5"));

        // Mejoras de tipo AUTO (generan puntos automáticamente)
        upgrades.add(new Upgrade("Tinta Automática", 50, 1.3, Upgrade.Type.AUTO, "Añade 1 clic por segundo"));
        upgrades.add(new Upgrade("M4-Tintosa Automatica", 200, 1.4, Upgrade.Type.AUTO, "Añade 5 clics por segundo"));
        upgrades.add(new Upgrade("Fábrica de Tinta", 1000, 1.6, Upgrade.Type.AUTO, "Añade 10 clics por segundo"));

        // Mejoras de tipo MULTIPLIER (multiplican todos los puntos)
        upgrades.add(new Upgrade("Tinta Multiplicadora", 500, 2.0, Upgrade.Type.MULTIPLIER, "Multiplica todos tus puntos por 1.5"));
    }

    /**
     * Carga el estado del juego desde las preferencias compartidas
     * Incluye puntos, mejoras, nivel de prestigio y otros valores
     */
    private void loadGameState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Cargar puntos con manejo de errores
        String savedPoints = prefs.getString(KEY_POINTS, "0.0");
        try {
            points = Double.parseDouble(savedPoints);
        } catch (NumberFormatException e) {
            points = 0.0;
        }

        // Cargar resto de valores numéricos
        pointsPerClick = prefs.getFloat(KEY_POINTS_PER_CLICK, 1.0f);
        pointsPerSecond = prefs.getFloat(KEY_POINTS_PER_SECOND, 0.0f);
        prestigeLevel = prefs.getInt(KEY_PRESTIGE_LEVEL, 0);
        prestigeMultiplier = prefs.getFloat(KEY_PRESTIGE_MULTIPLIER, 1.0f);
        prestigeRequirement = prefs.getFloat(KEY_PRESTIGE_REQUIREMENT, 5000.0f);

        // Cargar lista de mejoras usando Gson para deserializar JSON
        Gson gson = new Gson();
        String json = prefs.getString(KEY_UPGRADES, null);
        Type type = new TypeToken<ArrayList<Upgrade>>() {}.getType();
        upgrades = gson.fromJson(json, type);

        // Asegurar que upgrades nunca sea null después de cargar
        if (upgrades == null) {
            upgrades = new ArrayList<>();
        }

        // Actualizar variable estática
        currentPoints = points;
    }

    /**
     * Guarda el estado actual del juego en las preferencias compartidas
     * Incluye todos los valores importantes para restaurar el juego
     */
    private void saveGameState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Guardar todos los valores del estado del juego
        editor.putString(KEY_POINTS, String.valueOf(points));
        editor.putFloat(KEY_POINTS_PER_CLICK, (float) pointsPerClick);
        editor.putFloat(KEY_POINTS_PER_SECOND, (float) pointsPerSecond);
        editor.putInt(KEY_PRESTIGE_LEVEL, prestigeLevel);
        editor.putFloat(KEY_PRESTIGE_MULTIPLIER, (float) prestigeMultiplier);
        editor.putFloat(KEY_PRESTIGE_REQUIREMENT, (float) prestigeRequirement);

        // Guardar lista de mejoras usando Gson para serializar a JSON
        if (upgrades != null) {
            Gson gson = new Gson();
            String json = gson.toJson(upgrades);
            editor.putString(KEY_UPGRADES, json);
        }

        // Aplicar cambios
        editor.commit();
    }

    /**
     * Compra una mejora si el jugador tiene suficientes puntos
     * @param upgrade La mejora que se quiere comprar
     */
    private void buyUpgrade(Upgrade upgrade) {
        if (points >= upgrade.getCurrentCost()) {
            // Restar el coste y subir de nivel la mejora
            points -= upgrade.getCurrentCost();
            upgrade.levelUp();

            // Aplicar los efectos de la mejora
            applyUpgradeEffects(upgrade);

            // Actualizar la interfaz y el adaptador
            updateUI();
            upgradeAdapter.notifyDataSetChanged();

            // Guardar inmediatamente después de la compra
            saveGameState();
        } else {
            // Mostrar mensaje si no hay suficientes puntos
            Toast.makeText(this, "¡No tienes suficientes Tinta!", Toast.LENGTH_SHORT).show();
        }
        saveGameState();
    }

    /**
     * Aplica los efectos de una mejora según su tipo
     * @param upgrade La mejora cuyos efectos se van a aplicar
     */
    private void applyUpgradeEffects(Upgrade upgrade) {
        switch (upgrade.getType()) {
            case CLICK:
                // Mejoras que aumentan los puntos por clic
                if (upgrade.getName().equals("Tinta Mejorada")) {
                    pointsPerClick += 1 * prestigeMultiplier;
                } else if (upgrade.getName().equals("Super Tinta Mejorada")) {
                    pointsPerClick += 5 * prestigeMultiplier;
                }
                break;
            case AUTO:
                // Mejoras que aumentan los puntos por segundo
                if (upgrade.getName().equals("Tinta Automática")) {
                    pointsPerSecond += 1 * prestigeMultiplier;
                } else if (upgrade.getName().equals("M4-Tintosa Automatica")) {
                    pointsPerSecond += 5 * prestigeMultiplier;
                } else if (upgrade.getName().equals("Fábrica de Tinta")) {
                    pointsPerSecond += 10 * prestigeMultiplier;
                }
                break;
            case MULTIPLIER:
                // Mejoras que multiplican todos los puntos
                if (upgrade.getName().equals("Tinta Multiplicadora")) {
                    pointsPerClick *= 1.5;
                    pointsPerSecond *= 1.5;
                }
                break;
        }
        saveGameState();
    }

    /**
     * Realiza un prestigio si se cumplen los requisitos
     * Reinicia el progreso pero aumenta el multiplicador
     */
    private void prestige() {
        if (points >= prestigeRequirement) {
            // Aumentar nivel de prestigio y calcular nuevo multiplicador
            prestigeLevel++;
            prestigeMultiplier = 1.0 + (prestigeLevel * 0.5);

            // Aumentar requisito para el próximo prestigio
            prestigeRequirement *= 1.5;

            // Reiniciar progreso con el nuevo multiplicador
            points = 0;
            pointsPerClick = 1 * prestigeMultiplier;
            pointsPerSecond = 0;

            // Reiniciar mejoras
            initUpgrades();

            // Actualizar adaptador de mejoras
            upgradeAdapter = new UpgradeAdapter(upgrades, new UpgradeAdapter.OnUpgradeClickListener() {
                @Override
                public void onUpgradeClick(Upgrade upgrade) {
                    buyUpgrade(upgrade);
                }
            });
            upgradesRecyclerView.setAdapter(upgradeAdapter);

            // Actualizar textos de la interfaz
            pointsText.setText(formatNumber(points) + " Tinta");
            prestigeText.setText("Prestigio: " + prestigeLevel);
            generationInfoText.setText(formatNumber(pointsPerClick) + " tinta/clic | " +  formatNumber(pointsPerSecond) + " tinta/s");

            // Ocultar botón de prestigio hasta alcanzar el nuevo requisito
            prestigeButton.setVisibility(View.INVISIBLE);

            // Guardar inmediatamente después del prestigio
            saveGameState();

            // Mostrar mensaje de éxito
            Toast.makeText(this, "¡Prestigio conseguido! Multiplicador: x" + prestigeMultiplier +
                            "\nPróximo prestigio: " + formatNumber(prestigeRequirement) + " Tinta",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Detiene el auto-clicker eliminando los callbacks pendientes
     */
    private void stopAutoClicker() {
        if (autoClickRunnable != null) {
            autoClickHandler.removeCallbacks(autoClickRunnable);
        }
    }

    /**
     * Muestra un diálogo de información con opciones
     * Incluye la posibilidad de reiniciar las estadísticas
     */
    private void showInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_info, null);
        builder.setView(dialogView);

        // Obtener referencias a los elementos del diálogo
        TextView versionText = dialogView.findViewById(R.id.versionText);
        Button resetButton = dialogView.findViewById(R.id.resetButton);
        Button closeButton = dialogView.findViewById(R.id.closeButton);

        final AlertDialog dialog = builder.create();

        // Configurar botón de reinicio
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Mostrar diálogo de confirmación
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Reiniciar Estadísticas")
                        .setMessage("¿Estás seguro de que quieres reiniciar todas las estadísticas? Esta acción no se puede deshacer.")
                        .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                resetGameStats();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        // Configurar botón de cierre
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /**
     * Reinicia todas las estadísticas del juego a sus valores iniciales
     */
    private void resetGameStats() {
        // Reiniciar todas las variables a sus valores iniciales
        points = 0;
        pointsPerClick = 1;
        pointsPerSecond = 0;
        prestigeLevel = 0;
        prestigeMultiplier = 1.0;
        prestigeRequirement = 5000;

        // Reiniciar mejoras
        initUpgrades();

        // Actualizar interfaz
        updateUI();

        // Actualizar adaptador de mejoras
        upgradeAdapter = new UpgradeAdapter(upgrades, new UpgradeAdapter.OnUpgradeClickListener() {
            @Override
            public void onUpgradeClick(Upgrade upgrade) {
                buyUpgrade(upgrade);
            }
        });
        upgradesRecyclerView.setAdapter(upgradeAdapter);

        // Guardar inmediatamente después del reinicio
        saveGameState();

        // Mostrar mensaje de confirmación
        Toast.makeText(this, "¡Estadísticas reiniciadas!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Método llamado cuando la actividad entra en pausa
     * Guarda el estado y detiene el auto-clicker
     */
    @Override
    protected void onPause() {
        super.onPause();
        saveGameState();
        stopAutoClicker();
    }

    /**
     * Método llamado cuando la actividad se detiene
     * Guarda el estado para asegurar la persistencia
     */
    @Override
    protected void onStop() {
        super.onStop();
        // Guardar de nuevo cuando la app se detiene para asegurar la persistencia
        saveGameState();
    }

    /**
     * Método llamado cuando la actividad se destruye
     * Realiza un guardado final del estado
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Guardado final cuando la app se destruye
        saveGameState();
    }

    /**
     * Método llamado cuando la actividad se reanuda
     * Carga el estado, reinicia el auto-clicker y actualiza la interfaz
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadGameState();
        startAutoClicker();
        updateUI();
        saveGameState();
    }

    /**
     * Añade puntos al contador y actualiza la interfaz
     * @param amount Cantidad de puntos a añadir
     */
    private void addPoints(double amount) {
        // Añadir puntos con el multiplicador de prestigio
        points += amount * prestigeMultiplier;
        pointsText.setText(formatNumber(points) + " Tinta");
        updateUI();

        // No guardar en cada clic para evitar problemas de rendimiento
        // saveGameState() será llamado periódicamente por el auto-clicker
        saveGameState();
    }

    /**
     * Inicia el sistema de auto-clicker que genera puntos automáticamente
     */
    private void startAutoClicker() {
        autoClickRunnable = new Runnable() {
            @Override
            public void run() {
                // Añadir puntos automáticos cada segundo
                points += pointsPerSecond * prestigeMultiplier;
                pointsText.setText(formatNumber(points) + " Tinta");

                // Guardar cada 5 segundos en lugar de cada segundo para reducir operaciones I/O
                if (Math.random() < 0.2) {
                    saveGameState();
                }

                // Actualizar color del texto y la interfaz
                pointsText.setTextColor(Color.WHITE);
                updateUI();

                // Programar la próxima ejecución en 1 segundo
                autoClickHandler.postDelayed(this, 1000);
            }
        };
        autoClickHandler.postDelayed(autoClickRunnable, 1000);
    }

    /**
     * Actualiza todos los elementos de la interfaz de usuario
     * con los valores actuales del juego
     */
    private void updateUI() {
        // Actualizar variable estática
        currentPoints = points;

        // Actualizar texto de puntos
        pointsText.setText(formatNumber(points) + " Tinta");

        // Actualizar texto de prestigio
        prestigeText.setText("Prestigio: " + prestigeLevel);

        // Actualizar texto de información de generación
        String infoText = formatNumber(pointsPerClick) + " tinta/clic | " +
                formatNumber(pointsPerSecond) + " tinta/s";
        generationInfoText.setText(infoText);

        // Mostrar u ocultar botón de prestigio según corresponda
        if (points >= prestigeRequirement) {
            prestigeButton.setVisibility(View.VISIBLE);
        } else {
            prestigeButton.setVisibility(View.INVISIBLE);
        }

        // Actualizar adaptador de mejoras si existe
        if (upgradeAdapter != null) {
            upgradeAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Formatea un número para mostrarlo de forma legible
     * @param number El número a formatear
     * @return Cadena formateada con separadores de miles
     */
    private String formatNumber(double number) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        numberFormat.setGroupingUsed(true);
        numberFormat.setMaximumFractionDigits(0); // Elimina los decimales
        return numberFormat.format(number);
    }
}