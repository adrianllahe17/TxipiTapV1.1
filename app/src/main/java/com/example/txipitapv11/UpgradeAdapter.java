package com.example.txipitapv11;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adaptador para mostrar una lista de mejoras en un RecyclerView.
 */
public class UpgradeAdapter extends RecyclerView.Adapter<UpgradeAdapter.UpgradeViewHolder> {

    private List<Upgrade> upgrades;
    private OnUpgradeClickListener listener;

    /**
     * Interfaz para manejar clics en mejoras.
     */
    public interface OnUpgradeClickListener {
        void onUpgradeClick(Upgrade upgrade);
    }

    public UpgradeAdapter(List<Upgrade> upgrades, OnUpgradeClickListener listener) {
        this.upgrades = upgrades;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UpgradeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.upgrade_item, parent, false);
        return new UpgradeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UpgradeViewHolder holder, int position) {
        Upgrade upgrade = upgrades.get(position);
        holder.bind(upgrade, listener);
    }

    @Override
    public int getItemCount() {
        return upgrades.size();
    }

    /**
     * ViewHolder que representa una mejora individual.
     */
    static class UpgradeViewHolder extends RecyclerView.ViewHolder {
        TextView upgradeName;
        TextView upgradeCost;
        TextView upgradeLevel;
        Button buyButton;

        public UpgradeViewHolder(@NonNull View itemView) {
            super(itemView);
            upgradeName = itemView.findViewById(R.id.upgradeName);
            upgradeCost = itemView.findViewById(R.id.upgradeCost);
            upgradeLevel = itemView.findViewById(R.id.upgradeLevel);
            buyButton = itemView.findViewById(R.id.buyButton);
        }

        /**
         * Asocia los datos de una mejora con la vista.
         */
        public void bind(final Upgrade upgrade, final OnUpgradeClickListener listener) {
            upgradeName.setText(upgrade.getName());
            upgradeCost.setText(formatNumber(upgrade.getCurrentCost()) + " Tintas");
            upgradeLevel.setText("Nivel: " + upgrade.getLevel());

            // Descripción hablada para accesibilidad
            itemView.setContentDescription(
                    upgrade.getName() + ", costo: " + formatNumber(upgrade.getCurrentCost()) +
                            ", nivel actual: " + upgrade.getLevel()
            );

            // Mostrar la descripción al mantener pulsado
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(v.getContext(), upgrade.getDescription(), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

            boolean canAfford = MainActivity.getCurrentPoints() >= upgrade.getCurrentCost();

            updateButtonText(upgrade, canAfford);
            buyButton.setEnabled(canAfford);

            // Manejo de clic en botón de compra
            buyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (canAfford) {
                        listener.onUpgradeClick(upgrade);
                    }
                }
            });
        }

        /**
         * Actualiza el texto y la opacidad del botón según si el jugador puede comprar la mejora.
         */
        private void updateButtonText(Upgrade upgrade, boolean canAfford) {
            if (!canAfford) {
                buyButton.setAlpha(0.5f);
                buyButton.setText("Tinta Insuficiente");
            } else {
                buyButton.setAlpha(1.0f);
                buyButton.setText("Mejorar");
            }
        }

        /**
         * Formatea grandes cantidades con sufijos K, M o B.
         */
        private String formatNumber(double number) {
            if (number < 1000) {
                return String.format("%.1f", number);
            } else if (number < 1000000) {
                return String.format("%.1fK", number / 1000);
            } else if (number < 1000000000) {
                return String.format("%.1fM", number / 1000000);
            } else {
                return String.format("%.1fB", number / 1000000000);
            }
        }
    }

}
